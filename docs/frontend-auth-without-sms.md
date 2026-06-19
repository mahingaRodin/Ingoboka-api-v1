# Frontend guide — authentication without SMS (email OTP)

**Audience:** Next.js frontend team  
**Backend status:** SMS to real Rwanda numbers is **not enabled** (MTN bulk SMS is paid).  
**Current mode:** `OTP_DELIVERY_CHANNEL=email` (default on server and Docker).

Use this guide until paid SMS credentials are configured.

---

## 1. Why not SMS right now?

| Option | Free? | Real Rwanda phones? | Verdict |
|--------|-------|---------------------|---------|
| MTN bulk SMS | No (paid) | Yes | Production only |
| Twilio trial | ~100 SMS / 30 days | Limited by trial country rules | Not reliable for RW demo |
| Africa's Talking sandbox | Free | **No** — sandbox numbers only | Dev API testing only |
| Africala / eSMS Africa | Test credits | Maybe after signup | Still needs vendor account |
| **Email OTP (our default)** | Free (Gmail SMTP) | N/A | **Use this for hackathon** |
| `LOG` mode | Free | N/A | Backend devs read Docker logs only |

**Conclusion:** Keep **phone as the account identifier**, but deliver the **6-digit OTP by email** until SMS is paid for.

---

## 2. Discover server mode (call once on app load)

```http
GET /api/v1/auth/otp-delivery-config
```

No auth required.

**Example response:**

```json
{
  "success": true,
  "message": "OTP delivery configuration",
  "data": {
    "deliveryChannel": "EMAIL",
    "requiresEmail": true,
    "smsAvailable": false,
    "verifyHint": "Enter the 6-digit code sent to your email. Login still uses your phone number."
  }
}
```

**Frontend behaviour:**

- If `requiresEmail === true` → show **email field** on registration (required).
- If `deliveryChannel === "SMS"` → show “code sent to your phone”.
- If `deliveryChannel === "LOG"` → show admin/dev warning only.

---

## 3. Registration flow (citizen)

### Step A — Register

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "fullName": "Jean Uwimana",
  "phone": "+250780000001",
  "email": "jean@example.com",
  "nationalId": "1199880012345678",
  "password": "Ingoboka@2026"
}
```

| Field | Required now? | Notes |
|-------|---------------|-------|
| `fullName` | Yes | Split into first/last on backend |
| `phone` | Yes | Primary login identifier |
| `email` | **Yes** (while SMS off) | Must be a real inbox that receives mail |
| `nationalId` | Yes | Hashed server-side |
| `password` | Yes | Min 8 characters |

**Success message (email mode):**  
`Registration successful. Check your email for the 6-digit verification code.`

**Error if email missing:**  
`Email is required for verification while SMS is unavailable. Provide a real email address.`

### Step B — Verify OTP (unchanged API)

User still verifies with **phone + code** (not email):

```http
POST /api/v1/auth/verify-otp
```

```json
{
  "phone": "+250780000001",
  "code": "123456"
}
```

Aliases still work: `phoneNumber` / `otp`.

**Response:** `{ accessToken, refreshToken, expiresIn, user }` inside `data` — auto-login.

### Step C — Resend code

```http
POST /api/v1/auth/resend-otp
```

```json
{
  "phone": "+250780000001"
}
```

Code is resent to the **email on file**, not SMS.

---

## 4. Login (after verification)

```http
POST /api/v1/auth/login
```

```json
{
  "identifier": "+250780000001",
  "password": "Ingoboka@2026"
}
```

`identifier` can be phone **or** email.

User must have completed `verify-otp` first (`phoneVerified: true`).

---

## 5. UI copy suggestions

| Screen | Suggested text |
|--------|----------------|
| Register | “We’ll send a 6-digit code to your **email**. You’ll sign in with your **phone number**.” |
| Verify OTP | “Enter the code we sent to **{email}**” (mask: `j***@example.com`) |
| Resend | “Resend code to email” (not “Resend SMS”) |

---

## 6. TypeScript client sketch

```typescript
const API = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8085/api/v1";

export async function getOtpConfig() {
  const res = await fetch(`${API}/auth/otp-delivery-config`);
  const json = await res.json();
  return json.data as {
    deliveryChannel: "EMAIL" | "SMS" | "LOG";
    requiresEmail: boolean;
    smsAvailable: boolean;
    verifyHint: string;
  };
}

export async function registerCitizen(payload: {
  fullName: string;
  phone: string;
  email: string;
  nationalId: string;
  password: string;
}) {
  const res = await fetch(`${API}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw await res.json();
  return res.json();
}

export async function verifyOtp(phone: string, code: string) {
  const res = await fetch(`${API}/auth/verify-otp`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phone, code }),
  });
  if (!res.ok) throw await res.json();
  return res.json(); // data = tokens + user
}
```

---

## 7. Staff / email-first users (optional)

Legacy endpoint still works:

```http
POST /api/v1/auth/signup
```

Uses email + phone; OTP follows the same `OTP_DELIVERY_CHANNEL` setting.

---

## 8. When SMS is enabled later

Server `.env`:

```env
OTP_DELIVERY_CHANNEL=sms
MTN_BULK_SMS_ENABLED=true
MTN_BULK_SMS_API_KEY=...
```

Frontend: call `otp-delivery-config` again — `requiresEmail` becomes `false`, hide required email field (email optional again).

---

## 9. Troubleshooting

| Problem | Check |
|---------|--------|
| No email received | Gmail app password in server `.env` (`MAIL_*`), spam folder |
| `401` on login | User not verified — complete `verify-otp` |
| OTP expired | Default 10 minutes — use `resend-otp` |
| Dev without SMTP | Set `OTP_DELIVERY_CHANNEL=log` and read `docker compose logs api` |

---

## 10. Endpoints summary

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/auth/otp-delivery-config` | Know email vs SMS mode |
| POST | `/auth/register` | Citizen signup |
| POST | `/auth/verify-otp` | Phone + 6-digit code → tokens |
| POST | `/auth/resend-otp` | Resend code |
| POST | `/auth/login` | Phone or email + password |
| POST | `/auth/refresh` | Refresh token |
| POST | `/auth/logout` | Revoke refresh token |

All paths are under `/api/v1/auth/*`. Responses use `{ success, message, data }`.

---

*Last updated: June 2026 — backend default `OTP_DELIVERY_CHANNEL=email`.*
