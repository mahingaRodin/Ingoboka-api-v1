# USSD channel — Africa's Taking sandbox (*477#)

**Status:** DEMO / SANDBOX  
Ingoboka is a distribution platform. This USSD flow does **not** sell commercial insurance unless a licensed insurer has approved the product.

## What was built

Thin adapter:

```text
Africa's Talking  →  POST /api/v1/ussd/callback  →  UssdOrchestrator  →  existing services
```

No insurance rules live in the controller.

## Main menu (Kinyarwanda + English labels)

```text
CON Ingoboka *477#
1. Serivise z'ubwishingizi     (Available insurance services)
2. Ubwishingizi bwange         (My policies)
3. Ishyura ubwishingizi        (Pay premium)
4. Kwiyandikisha               (Register family / business)
5. Saba ubufasha               (Help)
```

## No login

- Identity = **MSISDN** from the network (`phoneNumber` from Africa's Taking).
- Register family or business once → SMS confirmation with reference.
- Re-register attempt → `Already registered` + reference.
- Creates a citizen user + profile + DATA_PROCESSING consent under the hood so policies/payments work.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/v1/ussd/callback` | Africa's Taking form callback (`sessionId`, `phoneNumber`, `serviceCode`, `text`) |
| POST | `/api/v1/ussd/simulate` | Same logic for Postman / local demo |

Optional header: `X-Ussd-Api-Key` (required if `USSD_GATEWAY_API_KEY` is set).

Response is plain text starting with `CON` (continue) or `END` (close).

## Africa's Talking setup

1. Create an [Africa's Talking](https://account.africastalking.com/) sandbox app.
2. Create a USSD channel / service code (sandbox may assign a code; map product shortcode story to `*477#` in your demo narrative).
3. Set callback URL to:

```text
https://YOUR_PUBLIC_HOST:8085/api/v1/ussd/callback
```

4. Ensure the server is reachable from the internet (your Kamatera host is fine).
5. Add to `/opt/ingoboka/deploy/.env`:

```env
USSD_ENABLED=true
USSD_SERVICE_CODE=*477#
USSD_GATEWAY_API_KEY=optional-shared-secret
USSD_SESSION_STORAGE=redis
```

6. Redeploy API so Flyway runs `V21__ussd_registrations.sql`.

## Local / Postman simulator

```bash
# Open session (main menu)
curl -s -X POST "http://localhost:8085/api/v1/ussd/simulate" \
  -d "sessionId=demo1&phoneNumber=+250780000099&text="

# Register family
curl -s -X POST "http://localhost:8085/api/v1/ussd/simulate" \
  -d "sessionId=demo1&phoneNumber=+250780000099&text=4"
curl -s -X POST "http://localhost:8085/api/v1/ussd/simulate" \
  -d "sessionId=demo1&phoneNumber=+250780000099&text=4*1"
curl -s -X POST "http://localhost:8085/api/v1/ussd/simulate" \
  -d "sessionId=demo1&phoneNumber=+250780000099&text=4*1*Aline%20Uwase"
curl -s -X POST "http://localhost:8085/api/v1/ussd/simulate" \
  -d "sessionId=demo1&phoneNumber=+250780000099&text=4*1*Aline%20Uwase*Gasabo"
```

### SMS to the handset (not log-only)

By default SMS is **logged only** (`SMS [+250...]: ...` in API logs) because no SMS provider is enabled.

To deliver registration SMS to the phone via **Africa's Talking** (same account as USSD):

1. In AT dashboard → **SMS** → enable sandbox (or live app).
2. **Sandbox only:** whitelist the test MSISDN under SMS → Sandbox → Phone Numbers.
3. On the server `deploy/.env`:

```env
AFRICASTALKING_SMS_ENABLED=true
AFRICASTALKING_USERNAME=sandbox
AFRICASTALKING_API_KEY=atsk_...
AFRICASTALKING_SMS_API_URL=https://api.sandbox.africastalking.com/version1/messaging
MTN_BULK_SMS_ENABLED=false
```

4. Redeploy (`docker compose up -d --force-recreate api`) and register again — logs should show `Africa's Talking SMS queued to +250...`.

Live traffic needs a live AT app username, live API key, and
`AFRICASTALKING_SMS_API_URL=https://api.africastalking.com/version1/messaging`
(plus any approved sender ID in `AFRICASTALKING_SMS_FROM`).

Alternative: `MTN_BULK_SMS_ENABLED=true` with MTN bulk credentials (keep AT SMS disabled).

### Language (Kinyarwanda / English)

Menus are a **curated bilingual catalog** (`UssdMessages`) — option **6. Choose language / Hitamo ururimi**.
Google Translate is intentionally not called per USSD request (latency, cost, character limits, and flaky networks on feature phones). Edit the catalog when copy changes.

## Demo product

Fresh `DemoDataSeeder` also creates **Hospital Cash Protection** (`HC-DEMO`) marked DEMO/SANDBOX: 500 RWF/week, 2,000 RWF/day, max 30 days (described on plan/benefit text). Existing DBs that already have `DEMO_INSURER` will not re-seed — add the product via admin UI or reset demo volume if needed.

## Honest gaps (master audit)

Done in this slice: USSD adapter, no-login register + SMS, menus, sandbox pay initiate, Hospital Cash demo product description.

Still open for later phases: structured benefit calculation engine, claim disbursement/outbox, product versioning tables, full NFIR dashboards, live AT/MTN production shortcodes.
