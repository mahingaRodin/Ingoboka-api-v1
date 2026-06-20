# Rodin Backend Brief — Frontend 100% Implementation

**Repo:** `backend/` (canonical; mirrors `Ingoboka-api-v1/`)  
**Audience:** Rodin Mahinga — backend lead  
**Prepared by:** Arnold / platform team  
**Date:** 20 June 2026  
**Status:** Gap analysis for Next.js frontend at ~93% UI completion  
**Supersedes / merges:** `docs/RODIN_BACKEND_WORK_BRIEF.md` (18 June) — use this doc for remaining work

---

## 1. Executive summary

The Next.js frontend (`frontend/`) is **~93% built against design**. Most screens, routes, and API clients exist. What blocks **100%** is not missing pages — it is **backend data shape and demo readiness**:

| Blocker category | Impact |
|------------------|--------|
| **No reproducible demo dataset** | Empty catalogs, manual partner onboarding, hackathon demos fail |
| **Product content is flat** | UI tabs (Cover, Exclusions, FAQ, Documents) use **hardcoded defaults**; API only returns `description` |
| **Needs assessment is score-only** | Frontend redirects to generic product list; no product recommendations |
| **Policy / claim enrichment** | Wallet shows wrong coverage amounts; claim lists lack names; no citizen claim timeline API |
| **Reporting gaps** | Insurer charts show zeros; `claims-breakdown` endpoint missing |
| **Ops / auth polish** | Remote SMTP for email OTP; platform admin seed missing `phoneVerified`; agent endpoint errors on wrong role |

**Already done (do not rebuild):** Auth aliases (`/auth/register`, phone login, verify-otp → tokens), enrollment MVP shortcut (`POST /applications`, submit, sandbox auto-approve), payment aliases, policy card/verify aliases, admin claims aliases, `FrontendCompatController` customer/payment/policy/agent routes, needs-assessment endpoint (basic), plan-level benefits/exclusions in DB.

**Frontend fallback pattern to eliminate:** When the API is unreachable, clients in `frontend/src/lib/api/*.ts` return `mockData` via `isNetworkError()`. When the API is reachable but fields are missing, pages fall back to **hardcoded constants** (see §9). Both must become unnecessary.

---

## 2. Priority table

| Pri | Gap | Frontend needs | Suggested endpoint / schema | Acceptance criteria |
|-----|-----|----------------|-----------------------------|---------------------|
| **P0** | Dev demo seed | Login as citizen, insurer, agent, platform admin; browse products; complete purchase | `V99__dev_demo_seed.sql` (dev profile only) + documented credentials | One-command docker demo: citizen sees ≥1 published product with ≥2 plans; insurer sees claims/applications; no manual partner setup |
| **P0** | Published products with plans | Product catalog, enroll frequency picker (daily/weekly/monthly) | Seed product + plans; **add `DAILY` to `PremiumFrequency`** (DB + enum) | `GET /products` returns items with plans; citizen enroll flow completes through payment |
| **P0** | Email OTP on deployed server | Register → verify → login on Kamatera | Configure `MAIL_*` in deploy `.env`; document in README | Citizen receives OTP email on `:8085`; `GET /auth/otp-delivery-config` returns `EMAIL` |
| **P0** | Platform admin seed `phoneVerified` | Staff flows that check phone verification | Edit `PlatformAdminSeeder`: set `phoneVerified=true`, `emailVerified=true` | Seeded admin login works; no spurious phone-verify blocks |
| **P0** | Agent `/agent/applications` wrong-role handling | Agent dashboard lists/creates applications | Return **403 JSON** (not 500) when role ≠ `AGENT`; validate org context | Citizen or insurer hitting endpoint gets `{ message, code: "ACCESS_DENIED" }` with HTTP 403 |
| **P1** | Structured product cover/exclusions/FAQ | Product detail tabs; insurer wizard step 2 (Coverage) | Extend `ProductResponse` / `GET /products/{id}` or plan detail with `benefits[]`, `exclusions[]`, `faq[]`, `claimSteps[]`, `waitingPeriodDays` | Insurer create sends structured arrays (not markdown blob); citizen detail tabs render API data |
| **P1** | Needs assessment → products | Results screen then filtered catalog | Extend `POST /applications/needs-assessment` → add `recommendedProducts: [{ id, name, category, startingPremium, matchScore }]` | Frontend can highlight recommended cards; no mock `{ score: 70, guidance: '...' }` on network error path needed |
| **P1** | Policy list enrichment | Dashboard hero + policy history | Extend `PolicyResponse` in `GET /policies` (alias): `productName`, `insurerName`, `coverageAmount` (sum of benefits or explicit field), `currency` | Wallet shows real product names and coverage, not premium-as-coverage |
| **P1** | Policy activity feed | Dashboard “Policy History” activity stream | `GET /policies/me/activity?page&size` → `[{ type, label, occurredAt, policyId?, claimId? }]` | Dashboard activity section populated from API (payments, renewals, claim updates) |
| **P1** | Citizen claim status timeline | Claim detail / status page (UI ready via shared `ClaimTimeline`) | `GET /claims/{id}` include `statusHistory[]` **or** `GET /claims/{id}/timeline` | Citizen opens claim → sees Submitted → Under review → Decision steps from `claim_status_history` table |
| **P1** | Claim list field enrichment | Claim cards for citizen + insurer | Extend `ClaimResponse`: `policyNumber`, `claimantName`, `currency` | No mapper fallbacks `'Citizen'`, empty policy numbers |
| **P2** | Product hero images / media | Product detail hero, catalog cards | `heroImageUrl`, optional `media[]` on product; MinIO upload on create | Detail page renders image when present; gradient fallback when absent |
| **P2** | PDF document downloads | Product detail “Documents” tab | `GET /products/{id}/documents` → `[{ id, title, fileName, downloadUrl }]` (presigned) | Download buttons fetch real URLs; insurer attaches PDFs at publish |
| **P2** | Insurer reports / claims breakdown | Reports page chart + stat cards | `GET /admin/reports/claims-breakdown` → `{ resolvedToday, avgResolutionDays, claimsByStatus: [{ status, count }] }` | `claimApi.getInsurerStats()` no longer hardcodes zeros |
| **P2** | Platform overview accuracy | Admin dashboard counts | Fix `GET /admin/platform/overview`: real `openClaims`, `totalApplications` | Admin dashboard matches DB counts |
| **P2** | Policy report aggregates | Insurer reports enrolled citizens | `GET /reports/policies/summary` or top-level fields on overview: `{ activePolicies, citizensEnrolled }` | Reports page stat cards show numbers, not `—` |

---

## 3. P0 — Demo blockers

### 3.1 Dev seed data (`V99__dev_demo_seed.sql`)

**Problem:** No Flyway demo seed exists (only `V3` roles, `V13` MoMo adapter, runtime `PlatformAdminSeeder`). Frontend demos require manual partner onboarding per `api-test-flow.md`.

**Action:** Add migration **`V99__dev_demo_seed.sql`** gated by profile `dev` / `docker` (**never production**).

**Seed contents:**

| Entity | Detail |
|--------|--------|
| Demo insurer org | `Demo Insurer Ltd`, code `DEMO_INSURER` |
| Staff accounts | Partner admin, claims officer, agent (see credentials below) |
| Citizen | Phone `+250780000001`, password `Ingoboka@2026`, verified, consent granted |
| Product | Published personal-accident (or family bundle) with **daily, weekly, monthly** plans |
| Sample policy | One `ACTIVE` policy for demo citizen (optional: one `PENDING_PAYMENT`) |
| Sample claim | One `UNDER_REVIEW` claim on demo policy |

**Suggested credentials** (align with original brief + `api-test-flow.md`):

| Role | Identifier | Password |
|------|------------|----------|
| Citizen | `+250780000001` | `Ingoboka@2026` |
| Partner admin | `eric@demo-insurer.rw` | `Ingoboka@2026` |
| Claims officer | `claims@demo-insurer.rw` | `Ingoboka@2026` |
| Agent | `agent@demo-insurer.rw` / `+250788000099` | `Ingoboka@2026` |
| Platform admin | `admin@ingoboka.rw` (or keep existing seeded email) | `Ingoboka@2026` |

**Acceptance:** Fresh `docker compose up` → citizen logs in → sees products → enroll → pay (sandbox) → policy in wallet — **no curl setup**.

---

### 3.2 Products with multi-frequency plans

**Problem:** Frontend product detail and enroll flows expose **daily / weekly / monthly** toggles. Backend `PremiumFrequency` enum and DB check constraint allow `WEEKLY`, `MONTHLY`, `QUARTERLY`, `ANNUAL` only — **no `DAILY`**.

**Action:**

1. Migration: add `DAILY` to `product_plans.premium_frequency` check + Java enum.
2. Seed (or document) three published plans per demo product.
3. Ensure `GET /products/{id}/plans` returns all published plans with `premiumFrequency`.

**Frontend reference:** `frontend/src/app/[locale]/(citizen)/products/[id]/page.tsx` — `planForFrequency()`.

---

### 3.3 Email OTP for production / remote demo

**Problem:** SMS is not enabled (`frontend-auth-without-sms.md`). Deployed API (`185.181.10.165:8085`) must send OTP email reliably.

**Action:**

| Setting | Value |
|---------|-------|
| `OTP_DELIVERY_CHANNEL` | `email` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Production SMTP (Gmail app password or transactional provider) |
| `FRONTEND_*` URLs | Match deployed frontend origin |

**Acceptance:** `POST /auth/register` → email received within 60s; `POST /auth/verify-otp` returns tokens; login with phone works.

**Fallback for local dev:** `OTP_DELIVERY_CHANNEL=log` + Mailhog (`localhost:8025`) — document in backend README.

---

### 3.4 Platform admin seed — `phoneVerified`

**Problem:** `PlatformAdminSeeder` sets `emailVerified=true` but leaves `phoneVerified=false` (default). Flows that gate on phone verification may block the seeded admin.

**Action:** In `PlatformAdminSeeder.java`, after creating admin user:

```java
admin.setEmailVerified(true);
admin.setPhoneVerified(true);  // ADD
```

**Acceptance:** `POST /auth/login` with seeded admin → `user.verified: true`; no OTP loop for platform admin.

---

### 3.5 Agent applications — wrong role → 403 not 500

**Problem:** Frontend agent dashboard calls `GET/POST /agent/applications`. Non-agent roles (or missing org context) reportedly produce **HTTP 500** instead of a clean denial.

**Action:**

1. Ensure `@PreAuthorize("hasRole('AGENT')")` on `FrontendCompatController` agent routes returns standard 403 via global exception handler.
2. In `EnrollmentServiceImpl.listTenantApplications` / `createAgentAssistedApplication`, catch “no organization” and throw `AccessDeniedException` (403), not uncaught NPE/IllegalState.
3. Add integration test: citizen token → `GET /agent/applications` → 403.

**Frontend reference:** `frontend/src/lib/api/admin.ts` — `agentApi.listApplications`, `createAssistedApplication`.

---

## 4. P1 — UI data shape gaps

### 4.1 Product detail tabs (Cover, Exclusions, How to claim, FAQ)

**Current state:**

- Backend **has** `product_benefits`, `product_exclusions`, `eligibility_rules` per plan (`ProductPlanResponse` with `benefits[]`, `exclusions[]` when `includeDetails=true`).
- `GET /products/{id}` returns **only** flat `ProductResponse` (id, name, description, category).
- Insurer frontend wizard **concatenates** coverage into markdown `description` instead of POSTing `benefits` / `exclusions` arrays (`productApi.create` only sends plan code/name/premium).
- Citizen product detail uses **`DEFAULT_COVER`**, **`DEFAULT_EXCLUSIONS`**, **`FAQ_ITEMS`** constants.

**Action:**

| # | Task |
|---|------|
| 1 | Add **`GET /products/{id}/detail`** (or enrich existing GET) returning product + default/published plan + structured content |
| 2 | Add product-level fields: `faq: [{ question, answer, sortOrder }]`, `claimSteps: [{ step, title, description }]`, `waitingPeriodDays` (from plan) |
| 3 | Update insurer `POST /products/{id}/plans` path — frontend will be updated to send `benefits[]`, `exclusions[]`, `eligibility` (backend already accepts these on `CreateProductPlanRequest`) |
| 4 | Migration: `product_faq` table (product_id, question, answer, sort_order) |

**Target response shape:** see §7.1.

**Acceptance:** Remove hardcoded arrays from `products/[id]/page.tsx`; tabs render API payload.

---

### 4.2 Insurer product wizard — Coverage as first-class schema (step 2)

**Problem:** Insurer UI step “Coverage” collects benefits, exclusions, age range, location — then flattens to markdown. Backend supports structured plan data but frontend does not send it yet; **backend must keep accepting and returning it** so FE can wire up.

**Action:**

- No new endpoint required if `POST /products/{productId}/plans` accepts full `CreateProductPlanRequest` (already does).
- Add **`PUT /products/{productId}/plans/{planId}`** for edit (P2 if time-boxed).
- Validate: benefit titles required; exclusions required for publish.

**Acceptance:** Partner admin creates product via API with benefits/exclusions → citizen `GET /products/{id}/detail` shows same items.

---

### 4.3 Needs assessment → product recommendations

**Current state:**

- `POST /applications/needs-assessment` returns `{ score, guidance, recommendedCategories }` (`EnrollmentServiceImpl.assessNeeds`).
- Frontend **ignores** `recommendedCategories`; on success redirects to `/products` after 2s.
- Frontend catch block returns hardcoded `{ score: 70, guidance: 'Accident cover...' }` on network error.

**Action:** Extend response:

```json
{
  "score": 72,
  "guidance": "Based on your profile…",
  "recommendedCategories": ["PERSONAL_ACCIDENT", "HEALTH_MICRO"],
  "recommendedProducts": [
    {
      "id": "uuid",
      "name": "Personal Accident Micro",
      "category": "PERSONAL_ACCIDENT",
      "startingPremium": 500,
      "currency": "RWF",
      "matchScore": 92,
      "reason": "Matches moto rider occupational risk"
    }
  ]
}
```

Implement matching: map occupation/income/dependents → published products in those categories (limit 3, sorted by matchScore).

**Acceptance:** Frontend can pass `?recommended=true` or store session flag; product cards show “Recommended” badge from API data (`ProductCard` already supports `recommended` prop).

---

### 4.4 Policy wallet enrichment + activity feed

**Current state:**

- `GET /policies` (alias) returns `PolicyResponse` without `productName`, `insurerName`, or true `coverageAmount`.
- Frontend mapper sets `coverageAmount = premiumAmount` and hardcodes `'Insurance Product'`, `'Partner Insurer'`.
- Dashboard “Policy History” lists policies only — design expects **activity feed** (payments, status changes).

**Action:**

| Endpoint | Fields / behaviour |
|----------|-------------------|
| Enrich `PolicyResponse` | Join product plan + org: `productName`, `insurerName`, `coverageAmount`, `currency: "RWF"` |
| **`GET /policies/me/activity`** | Paginated events from payments, policy status changes, claim submissions |

**Activity event types:** `POLICY_ACTIVATED`, `PREMIUM_PAID`, `POLICY_RENEWED`, `CLAIM_SUBMITTED`, `CLAIM_DECISION`.

**Acceptance:** Dashboard hero card and list show real names/amounts; activity section has ≥1 event after demo seed journey.

---

### 4.5 Citizen claim status timeline

**Current state:**

- Insurer claim detail builds timeline **client-side** from status (`buildTimeline()` in `insurer/claims/[id]/page.tsx`).
- Backend persists `claim_status_history` on every transition but **does not expose** it in API.
- `ClaimResponse` lacks `policyNumber`, `claimantName` — mapper uses placeholders.
- No citizen `claims/[id]` route yet, but `ClaimTimeline` component is shared and i18n key `claimDetail` exists.

**Action:**

1. Extend `ClaimResponse` OR add `ClaimDetailResponse`:
   - `policyNumber`, `claimantName`, `currency`
   - `statusHistory: [{ status, label?, occurredAt, note? }]`
2. Populate history from `ClaimStatusHistoryRepository.findByClaimIdOrderByCreatedAtAsc`.
3. Optional: `GET /claims/{id}/timeline` alias for clarity.

**Acceptance:** Same payload powers insurer and citizen timeline UIs; history matches DB after decision.

---

## 5. P2 — Polish

### 5.1 Product hero images / media

**Problem:** Product detail hero is CSS gradient only; catalog uses icon placeholder.

**Action:**

- Migration: `insurance_products.hero_image_key VARCHAR(512)` (MinIO object key).
- `POST /products/{id}/media` — presigned upload (reuse document service).
- Include `heroImageUrl` (presigned or CDN path) in product list + detail responses.

---

### 5.2 PDF document downloads

**Problem:** Documents tab lists fake filenames (`Policy_Summary.pdf`) with non-functional download icons.

**Action:**

- Table `product_documents` (product_id, title, file_name, object_key, sort_order).
- `GET /products/{id}/documents` → presigned download URLs via existing `DocumentStorageService`.
- Insurer upload during product publish flow.

---

### 5.3 Admin / insurer reports

**Missing today:**

| Frontend call | Backend today | Fix |
|---------------|---------------|-----|
| `GET /admin/reports/overview` | Returns `openClaims`, `activePolicies`, … — OK for open claims | Map `pendingClaims` alias if needed |
| `claimApi.getInsurerStats()` also needs breakdown | **No** `GET /admin/reports/claims-breakdown` | **Implement** — see §7.3 |
| `GET /reports/policies` | Paginated policy rows | Add summary aggregate or separate `GET /reports/policies/summary` with `{ activePolicies, citizensEnrolled }` |
| `GET /admin/platform/overview` | `openClaims: 0`, `totalApplications: 0` hardcoded | Query real counts |

**Frontend reference:** `frontend/src/lib/api/claims.ts` — `getInsurerStats()` hardcodes `resolvedToday: 0`, `avgResolutionDays: 0`, `claimsByStatus: []`.

---

## 6. Endpoint checklist — frontend expects vs backend

Base: `/api/v1`. Wrapper: `{ success, message, data }`.  
✅ = implemented and shape OK · ⚠️ = exists but shape/behaviour gap · ❌ = missing

### Auth (`frontend/src/lib/api/auth.ts`)

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| GET | `/auth/otp-delivery-config` | ✅ | FE defaults if unreachable |
| POST | `/auth/register` | ✅ | Email required when SMS off |
| POST | `/auth/login` | ✅ | `identifier` phone or email |
| POST | `/auth/verify-otp` | ✅ | Returns tokens + user |
| POST | `/auth/resend-otp` | ✅ | |
| POST | `/auth/refresh` | ✅ | |
| POST | `/auth/logout` | ✅ | |
| GET | `/customers/me` | ✅ | Alias in `FrontendCompatController` |
| POST | `/customers/consent` | ✅ | Alias; returns consent state |

### Customer extensions (`admin.ts` → `customerApiExt`)

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| GET | `/customer/dependants` | ✅ | |
| POST | `/customer/dependants` | ✅ | |
| DELETE | `/customer/dependants/{id}` | ✅ | |
| POST | `/customer/kyc/submit` | ✅ | |

### Products & enrollment (`products.ts`)

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| GET | `/products` | ⚠️ | Missing `startingPremium` per item (FE derives from plans client-side) |
| GET | `/products/tenant` | ✅ | Insurer admin list |
| GET | `/products/{id}` | ⚠️ | No structured cover/exclusions/FAQ |
| GET | `/products/{id}/plans` | ⚠️ | Has benefits/exclusions; FE mapper ignores them |
| POST | `/products` | ✅ | |
| POST | `/products/{id}/plans` | ⚠️ | FE doesn't send benefits yet; backend ready |
| POST | `/products/{id}/publish` | ✅ | Plan publish separate: `/plans/{planId}/publish` |
| POST | `/applications/quote` | ⚠️ | FE hardcodes productName/planName in mapper |
| POST | `/applications` | ✅ | MVP shortcut `{ productPlanId }` |
| POST | `/applications/{id}/submit` | ✅ | Sandbox auto-approve |
| POST | `/applications/needs-assessment` | ⚠️ | Missing `recommendedProducts[]` |

### Payments (`payments.ts`)

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| POST | `/payments/initiate` | ✅ | Alias `{ applicationId \| policyId }` |
| GET | `/payments/{id}/status` | ✅ | |
| POST | `/payments/sandbox/callback` | ✅ | Used by enroll page |

### Policies (`policies.ts`)

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| GET | `/policies` | ⚠️ | Missing productName, insurerName, coverageAmount |
| GET | `/policies/{id}` | ⚠️ | Same enrichment needed |
| GET | `/policies/{id}/card` | ⚠️ | `productName` hardcoded `"Insurance Plan"` in service |
| GET | `/verify/{token}` | ✅ | Public alias |
| GET | `/policies/me/activity` | ❌ | Dashboard activity feed |

### Claims (`claims.ts`)

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| GET | `/claims/me` | ⚠️ | Missing policyNumber, claimantName |
| GET | `/claims/{id}` | ⚠️ | Missing statusHistory |
| POST | `/claims` | ✅ | |
| POST | `/claims/{id}/submit` | ✅ | |
| GET | `/admin/claims` | ⚠️ | Same field gaps |
| GET | `/admin/claims/{id}` | ⚠️ | |
| POST | `/admin/claims/{id}/decision` | ✅ | |
| GET | `/admin/reports/overview` | ⚠️ | Partial; FE maps openClaims |
| GET | `/admin/reports/claims-breakdown` | ❌ | FE hardcodes zeros |

### Admin / agent / insurer (`admin.ts`)

| Method | Path | Status | Notes |
|--------|------|--------|-------|
| GET | `/admin/platform/overview` | ⚠️ | openClaims/totalApplications stubbed 0 |
| GET | `/partners` | ✅ | FE uses for org list (expects `/admin/platform/organizations` alias optional) |
| GET | `/agent/applications` | ⚠️ | Wrong role → must be 403 not 500 |
| POST | `/agent/applications` | ⚠️ | Requires existing citizen by phone |
| GET | `/insurer/settings` | ✅ | |
| PUT | `/insurer/settings` | ✅ | |
| GET | `/reports/policies` | ⚠️ | FE expects summary stats; gets paginated rows |
| GET | `/revenue/invoices` | ✅ | |
| GET | `/partners/me` + `/partners/{id}/contracts` | ✅ | |
| GET | `/revenue/ledger` | ✅ | |

**Reference implementation:** `backend/src/main/java/.../frontend/controllers/FrontendCompatController.java`

---

## 7. Sample JSON — key new / changed responses

### 7.1 Product detail (new or enriched GET)

```json
{
  "success": true,
  "message": "Product retrieved",
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "code": "PA-MICRO",
    "name": "Personal Accident Micro",
    "category": "PERSONAL_ACCIDENT",
    "description": "Affordable accident protection for informal workers.",
    "heroImageUrl": "https://minio.example/ingoboka/products/pa-micro-hero.jpg",
    "waitingPeriodDays": 30,
    "currency": "RWF",
    "plans": [
      {
        "id": "plan-daily-uuid",
        "code": "PA-DAILY",
        "name": "Daily Plan",
        "premiumAmount": 150,
        "premiumFrequency": "DAILY",
        "benefits": [
          {
            "title": "Accident Protection",
            "description": "Lump sum benefit for accidental injury or disability.",
            "coverageLimit": 500000
          }
        ],
        "exclusions": [
          {
            "title": "Illegal activities",
            "description": "Injuries resulting from illegal activities."
          }
        ]
      }
    ],
    "faq": [
      {
        "question": "When does my cover start?",
        "answer": "Full cover begins after a 30-day waiting period from your first successful payment."
      }
    ],
    "claimSteps": [
      { "step": 1, "title": "Notify via App", "description": "Report within 72 hours through Claims." },
      { "step": 2, "title": "Upload Documents", "description": "Submit hospital records or certificates." },
      { "step": 3, "title": "Receive Payout", "description": "Approved claims paid to Mobile Money." }
    ],
    "documents": [
      {
        "id": "doc-uuid",
        "title": "Policy Summary",
        "fileName": "Policy_Summary.pdf",
        "downloadUrl": "https://minio.example/presigned..."
      }
    ]
  }
}
```

### 7.2 Needs assessment (extended)

```json
{
  "success": true,
  "message": "Assessment complete",
  "data": {
    "score": 72,
    "guidance": "Based on your profile, personal accident cover fits your daily moto risk.",
    "recommendedCategories": ["PERSONAL_ACCIDENT"],
    "recommendedProducts": [
      {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "name": "Personal Accident Micro",
        "category": "PERSONAL_ACCIDENT",
        "startingPremium": 500,
        "currency": "RWF",
        "matchScore": 92,
        "reason": "Occupation: moto rider"
      }
    ]
  }
}
```

### 7.3 Claims breakdown (new GET `/admin/reports/claims-breakdown`)

```json
{
  "success": true,
  "message": "Claims breakdown",
  "data": {
    "resolvedToday": 4,
    "avgResolutionDays": 3.2,
    "claimsByStatus": [
      { "status": "SUBMITTED", "count": 5 },
      { "status": "UNDER_REVIEW", "count": 7 },
      { "status": "APPROVED", "count": 18 },
      { "status": "REJECTED", "count": 2 }
    ]
  }
}
```

### 7.4 Policy with enrichment (changed GET `/policies`)

```json
{
  "success": true,
  "message": "Policies retrieved",
  "data": {
    "content": [
      {
        "id": "pol-uuid",
        "policyNumber": "ING-2026-0001",
        "productName": "Personal Accident Micro",
        "insurerName": "Demo Insurer Ltd",
        "status": "ACTIVE",
        "premiumAmount": 500,
        "coverageAmount": 500000,
        "currency": "RWF",
        "startDate": "2026-01-01",
        "endDate": "2026-12-31",
        "qrVerificationToken": "verify-token-abc"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 7.5 Claim with timeline (changed GET `/claims/{id}`)

```json
{
  "success": true,
  "message": "Claim retrieved",
  "data": {
    "id": "claim-uuid",
    "claimNumber": "CLM-2026-0042",
    "policyId": "pol-uuid",
    "policyNumber": "ING-2026-0001",
    "claimantName": "Jean Uwimana",
    "status": "UNDER_REVIEW",
    "claimedAmount": 75000,
    "currency": "RWF",
    "description": "Outpatient medical visit.",
    "createdAt": "2026-06-10T08:30:00Z",
    "updatedAt": "2026-06-12T14:00:00Z",
    "statusHistory": [
      { "status": "SUBMITTED", "occurredAt": "2026-06-10T08:30:00Z", "note": "Claim submitted" },
      { "status": "UNDER_REVIEW", "occurredAt": "2026-06-11T09:00:00Z", "note": "Assigned to officer" }
    ]
  }
}
```

### 7.6 Policy activity feed (new GET `/policies/me/activity`)

```json
{
  "success": true,
  "message": "Activity retrieved",
  "data": {
    "content": [
      {
        "type": "PREMIUM_PAID",
        "label": "Premium paid — Personal Accident Micro",
        "occurredAt": "2026-06-01T10:00:00Z",
        "policyId": "pol-uuid"
      },
      {
        "type": "CLAIM_SUBMITTED",
        "label": "Claim CLM-2026-0042 submitted",
        "occurredAt": "2026-06-10T08:30:00Z",
        "policyId": "pol-uuid",
        "claimId": "claim-uuid"
      }
    ],
    "totalElements": 2
  }
}
```

---

## 8. Definition of done — “frontend 100%”

The frontend is **100% implemented** when all of the following are true **without mock fallbacks or hardcoded product/claim content**:

### 8.1 Citizen journey (end-to-end)

- [ ] Register with phone + email → OTP email received on deployed server → verify → consent
- [ ] Needs assessment returns **recommended products**; catalog highlights them
- [ ] Product detail tabs (Cover, Exclusions, How to claim, Documents, FAQ) render **API data**
- [ ] Enroll: quote → application → submit → payment initiate → sandbox callback → **ACTIVE** policy
- [ ] Dashboard: hero card + policy list show **real productName, coverageAmount, insurerName**
- [ ] Dashboard activity feed shows payment/claim events from **`/policies/me/activity`**
- [ ] File claim against active policy; view claim status timeline from **`statusHistory`**
- [ ] Policy card + public `/verify/{token}` show correct holder, product, dates, QR

### 8.2 Insurer journey

- [ ] Product wizard persists **structured** benefits/exclusions (not markdown-only)
- [ ] Claims queue lists claims with **claimantName, policyNumber**
- [ ] Claim detail timeline matches backend history
- [ ] Reports page: **`/admin/reports/claims-breakdown`** populates chart (no zero stubs)
- [ ] Policy report summary shows **activePolicies** and **citizensEnrolled**

### 8.3 Agent & admin

- [ ] Agent lists/creates assisted applications; wrong role gets **403**
- [ ] Platform admin dashboard shows **real** openClaims and totalApplications
- [ ] Demo seed accounts work out of the box (§3.1 credentials)

### 8.4 Engineering gates

- [ ] No `isNetworkError` mock usage required for demo (API always reachable in compose)
- [ ] OpenAPI documents all new/changed endpoints with examples
- [ ] One integration test: register → purchase → claim → insurer decision
- [ ] Frontend team confirms: grep for `DEFAULT_COVER`, `DEFAULT_EXCLUSIONS`, `FAQ_ITEMS`, `mockData` only in test/dev files

---

## 9. Frontend mock & hardcoded fallback inventory

Grep targets for verification after backend delivery:

| Location | Pattern | Replace with |
|----------|---------|--------------|
| `frontend/src/lib/api/auth.ts` | `isNetworkError` → mock tokens/user | Real API only in demo env |
| `frontend/src/lib/api/policies.ts` | `mockData.policies`, `getPolicyCard` | Enriched `/policies`, `/card` |
| `frontend/src/lib/api/claims.ts` | `mockData.claims`, hardcoded stats in `getInsurerStats` | Full claim fields + `/admin/reports/claims-breakdown` |
| `frontend/src/lib/api/products.ts` | needs-assessment network mock; quote hardcoded names | API product/plan names + recommendations |
| `frontend/src/lib/api/mappers.ts` | `'Insurance Product'`, `'Partner Insurer'`, `coverageAmount: premium` | Backend-enriched policy/claim fields |
| `frontend/src/lib/api/mock-data.ts` | Entire mock dataset | Retain for Storybook/tests only |
| `products/[id]/page.tsx` | `DEFAULT_COVER`, `DEFAULT_EXCLUSIONS`, `FAQ_ITEMS`, fake PDF list | Product detail API |
| `insurer/products/page.tsx` | `DEFAULT_BENEFITS`, `DEFAULT_EXCLUSIONS`, markdown builder | Structured plan POST |
| `products/needs-assessment/page.tsx` | Redirect to `/products` without recommendations | `recommendedProducts` |
| `insurer/reports/page.tsx` | Stats chart with empty `claimsByStatus` | claims-breakdown endpoint |
| `products/[id]/page.tsx` | Hero gradient only | `heroImageUrl` when present |

**Network error message:** `client.ts` — `'Network error — is the API running?'` (status 0) triggers all mocks.

---

## 10. Suggested sprint order for Rodin

1. **Day 1–2:** P0 — `V99` seed, `DAILY` frequency, platform admin `phoneVerified`, agent 403 fix, SMTP on deploy
2. **Day 3–4:** P1 — Product detail enrichment + insurer structured plans; policy/claim field enrichment
3. **Day 5:** P1 — Needs assessment products; claim `statusHistory`; policy activity feed
4. **Day 6:** P2 — claims-breakdown, platform overview fixes, policy report summary
5. **Day 7:** P2 — Product media + PDF documents; integration test; OpenAPI pass

---

## 11. Related docs

| Document | Purpose |
|----------|---------|
| `docs/RODIN_BACKEND_WORK_BRIEF.md` | Original MVP alias checklist (mostly done) |
| `frontend-auth-without-sms.md` | Email OTP contract for citizens |
| `api-test-flow.md` | Manual curl validation flows |
| `docs/FRONTEND_BACKEND_INTEGRATION.md` | Run locally + OTP mode |
| `frontend/src/lib/api/*.ts` | Exact paths and mappers |
| `backend/.../FrontendCompatController.java` | Alias route reference |

---

## 12. Questions for Rodin

1. Prefer **one fat `GET /products/{id}/detail`** vs multiple tab endpoints?
2. OK to add **`DAILY`** premium frequency (schema migration)?
3. **`recommendedProducts`**: rule-based (occupation → category) sufficient for MVP, or store assessment results?
4. Activity feed: **synthetic view** over payments/claims tables vs dedicated `policy_activity` table?
5. Timeline for P0 seed before platform merges remaining FE wiring?

---

*Update this doc as endpoints ship. Frontend team will remove hardcoded fallbacks in the same PRs as backend shape changes.*
