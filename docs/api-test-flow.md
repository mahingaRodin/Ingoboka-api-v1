# API manual test flow (mock data)

**Purpose:** Step-by-step endpoint testing for user management, onboarding, and the citizen purchase journey.  
**Base URLs:**

| Environment | Base URL |
|-------------|----------|
| Local | `http://localhost:8085/api/v1` |
| Deployed (Kamatera) | `http://185.181.10.165:8085/api/v1` |

Set once in your shell:

```bash
export API=http://localhost:8085/api/v1
# PowerShell:
# $API = "http://localhost:8085/api/v1"
```

**Tools:** curl, Postman, or Swagger UI at `{host}/swagger-ui/index.html`

**OTP tip:** With `OTP_DELIVERY_CHANNEL=email`, check the user inbox (or `docker compose logs api` if `OTP_DELIVERY_CHANNEL=log`).

---

## Mock personas

| Persona | Email / phone | Password (initial) | Role |
|---------|---------------|-------------------|------|
| Platform admin (seeded) | `agressive.one04@gmail.com` | `admin@123` | `PLATFORM_ADMIN` |
| Partner admin | `eric@demo-insurer.rw` | `Ingoboka@2026` (set at onboard) | `PARTNER_ADMIN` |
| Claims officer | `claims@demo-insurer.rw` | emailed temp | `CLAIMS_OFFICER` |
| Citizen | `+250780000001` | self-chosen at register | `CITIZEN` |

---

## Flow A — Platform admin → onboard partner

### A1. Login as platform admin

```bash
curl -s -X POST "$API/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "agressive.one04@gmail.com",
    "password": "admin@123"
  }' | jq .
```

**Expect:** `success: true`, `data.user.role: "PLATFORM_ADMIN"`, `accountActive: true`

Save token:

```bash
export PLATFORM_TOKEN="<accessToken from response>"
```

### A2. Create insurer / partner organization + admin

```bash
curl -s -X POST "$API/partners" \
  -H "Authorization: Bearer $PLATFORM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Demo Insurer Ltd",
    "code": "DEMO_INSURER",
    "type": "INSURER",
    "registrationNumber": "REG-DEMO-001",
    "contactEmail": "contact@demo-insurer.rw",
    "contactPhone": "+250788000099",
    "adminFirstName": "Eric",
    "adminLastName": "Mugisha",
    "adminEmail": "eric@demo-insurer.rw",
    "adminPhone": "+250788000100",
    "adminDefaultPassword": "Ingoboka@2026"
  }' | jq .
```

**Expect:**

- `data.partner.id` — save as `PARTNER_ID`
- `data.partnerAdmin.userId` — partner admin user id
- `data.partnerAdmin.mustChangePassword: true`
- Welcome email sent (if SMTP configured)

```bash
export PARTNER_ID="<partner.id>"
```

---

## Flow B — Partner admin onboarding (forced password + email verify)

### B1. First login (temporary password)

```bash
curl -s -X POST "$API/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "eric@demo-insurer.rw",
    "password": "Ingoboka@2026"
  }' | jq .
```

**Expect:**

- `data.user.mustChangePassword: true`
- `data.user.status: "PENDING_PASSWORD_CHANGE"`
- `data.user.accountActive: false`

```bash
export PARTNER_TOKEN="<accessToken>"
```

### B2. Try protected API before onboarding (should fail)

```bash
curl -s "$API/products/tenant" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .
```

**Expect:** HTTP 403, `code: "MUST_CHANGE_PASSWORD"`

### B3. Change password

```bash
curl -s -X POST "$API/auth/change-password" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Ingoboka@2026",
    "newPassword": "EricSecure@2026"
  }' | jq .
```

**Expect:** New tokens, `requiresEmailVerification: true`, `status: "PENDING_EMAIL_VERIFICATION"`

Update token:

```bash
export PARTNER_TOKEN="<new accessToken>"
```

### B4. Verify email

Check inbox for verification link/token, then:

```bash
curl -s -X POST "$API/auth/verify-email/confirm" \
  -H "Content-Type: application/json" \
  -d '{ "token": "<uuid-from-email>" }' | jq .
```

Or resend:

```bash
curl -s -X POST "$API/auth/verify-email/request" \
  -H "Content-Type: application/json" \
  -d '{ "email": "eric@demo-insurer.rw" }' | jq .
```

**Expect after confirm:** `status: "ACTIVE"`, `accountActive: true`

Re-login if needed:

```bash
curl -s -X POST "$API/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "eric@demo-insurer.rw",
    "password": "EricSecure@2026"
  }' | jq .
```

---

## Flow C — Partner admin creates staff (same onboarding model)

### C1. Staff overview (empty or seeded)

```bash
curl -s "$API/partner/staff/overview" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .
```

### C2. Create claims officer

```bash
curl -s -X POST "$API/partner/staff" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Claire",
    "lastName": "Uwase",
    "email": "claims@demo-insurer.rw",
    "phoneNumber": "+250788000101",
    "roleCode": "CLAIMS_OFFICER",
    "defaultPassword": "StaffTemp@2026"
  }' | jq .
```

**Expect:**

- `mustChangePassword: true`
- Same provisioning as partner admin at `POST /partners`
- Welcome email with temporary password

Save:

```bash
export STAFF_USER_ID="<userId from response>"
```

### C3. Staff first login + change password + verify email

Repeat **Flow B** steps using `claims@demo-insurer.rw` / `StaffTemp@2026` → new password `ClaimsSecure@2026`.

```bash
export STAFF_TOKEN="<claims officer accessToken after full onboarding>"
```

### C4. Partner admin manages staff

```bash
# List
curl -s "$API/partner/staff?page=0&size=20" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .

# Get one
curl -s "$API/partner/staff/$STAFF_USER_ID" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .

# Reset credentials (re-starts temp password flow)
curl -s -X POST "$API/partner/staff/$STAFF_USER_ID/reset-credentials" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

# Disable staff
curl -s -X PATCH "$API/partner/staff/$STAFF_USER_ID/status" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "status": "DISABLED" }' | jq .
```

**Platform admin alternate path** (explicit org id):

```bash
curl -s "$API/partners/$PARTNER_ID/staff" \
  -H "Authorization: Bearer $PLATFORM_TOKEN" | jq .
```

---

## Flow D — Partner admin tenant operations

After partner admin is `ACTIVE`:

### D1. Create and publish product

```bash
curl -s -X POST "$API/products" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "PA-MICRO",
    "name": "Personal Accident Micro",
    "description": "Daily accident cover for demo",
    "category": "PERSONAL_ACCIDENT"
  }' | jq .
```

```bash
export PRODUCT_ID="<id>"
```

Create plan:

```bash
curl -s -X POST "$API/products/$PRODUCT_ID/plans" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "PA-MICRO-BASIC",
    "name": "Basic Plan",
    "description": "Monthly micro accident cover",
    "premiumAmount": 500,
    "premiumFrequency": "MONTHLY"
  }' | jq .
```

```bash
export PLAN_ID="<planId>"
```

Publish:

```bash
curl -s -X POST "$API/products/$PRODUCT_ID/publish" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .

curl -s -X POST "$API/products/$PRODUCT_ID/plans/$PLAN_ID/publish" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .
```

### D2. List tenant applications / claims (partner admin oversight)

```bash
curl -s "$API/applications?status=SUBMITTED" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .

curl -s "$API/claims" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .

curl -s "$API/reports/overview" \
  -H "Authorization: Bearer $PARTNER_TOKEN" | jq .
```

Staff with `CLAIMS_OFFICER` can use the same tenant claim endpoints; partner admin sees everything.

---

## Flow E — Citizen registration and purchase

### E1. Register citizen

```bash
curl -s -X POST "$API/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jean Uwimana",
    "phone": "+250780000001",
    "email": "jean.uwimana@example.com",
    "nationalId": "1199880012345678",
    "password": "JeanPass@2026"
  }' | jq .
```

### E2. Verify OTP

```bash
curl -s -X POST "$API/auth/verify-otp" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+250780000001",
    "code": "123456"
  }' | jq .
```

Use the real code from email/logs.

### E3. Citizen login

```bash
curl -s -X POST "$API/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "+250780000001",
    "password": "JeanPass@2026"
  }' | jq .
```

```bash
export CITIZEN_TOKEN="<accessToken>"
```

### E4. Browse products and apply

```bash
curl -s "$API/products" \
  -H "Authorization: Bearer $CITIZEN_TOKEN" | jq .

curl -s -X POST "$API/applications" \
  -H "Authorization: Bearer $CITIZEN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "productPlanId": "'"$PLAN_ID"'" }' | jq .
```

```bash
export APPLICATION_ID="<application id>"
```

Submit:

```bash
curl -s -X POST "$API/applications/$APPLICATION_ID/submit" \
  -H "Authorization: Bearer $CITIZEN_TOKEN" | jq .
```

### E5. Payment (sandbox)

```bash
curl -s -X POST "$API/payments/initiate" \
  -H "Authorization: Bearer $CITIZEN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "applicationId": "'"$APPLICATION_ID"'" }' | jq .
```

Complete sandbox callback per your payment integration, then:

```bash
curl -s "$API/policies" \
  -H "Authorization: Bearer $CITIZEN_TOKEN" | jq .
```

### E6. Partner admin reviews application

```bash
curl -s -X PATCH "$API/applications/$APPLICATION_ID/review" \
  -H "Authorization: Bearer $PARTNER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "status": "APPROVED", "decisionReason": "Demo approval" }' | jq .
```

---

## Flow F — Platform admin user CRUD (optional)

```bash
# List users
curl -s "$API/admin/users?page=0&size=20" \
  -H "Authorization: Bearer $PLATFORM_TOKEN" | jq .

# Create tenant user directly
curl -s -X POST "$API/admin/users" \
  -H "Authorization: Bearer $PLATFORM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Underwriter",
    "lastName": "Demo",
    "email": "uw@demo-insurer.rw",
    "phoneNumber": "+250788000102",
    "roleCode": "UNDERWRITER",
    "organizationId": "'"$PARTNER_ID"'",
    "defaultPassword": "UwTemp@2026"
  }' | jq .

# Reset password
curl -s -X POST "$API/admin/users/<userId>/reset-password" \
  -H "Authorization: Bearer $PLATFORM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

---

## Quick validation checklist

| # | Test | Expected |
|---|------|----------|
| 1 | Platform admin login | `ACTIVE`, full access |
| 2 | `POST /partners` | Partner + admin with `mustChangePassword` |
| 3 | Partner login before password change | 403 `MUST_CHANGE_PASSWORD` on protected routes |
| 4 | `POST /auth/change-password` | `PENDING_EMAIL_VERIFICATION` |
| 5 | `POST /auth/verify-email/confirm` | `ACTIVE`, `accountActive: true` |
| 6 | `POST /partner/staff` | Staff created, same temp-password flow |
| 7 | `GET /partner/staff/overview` | Counts match staff onboarding states |
| 8 | Staff completes onboarding | Can access role-specific endpoints |
| 9 | Partner admin `GET /applications`, `GET /claims` | Tenant-wide visibility |
| 10 | Citizen register + OTP + purchase | Policy issued after payment |

---

## Troubleshooting

| Symptom | Check |
|---------|--------|
| 403 `MUST_CHANGE_PASSWORD` | Complete change-password flow first |
| 403 `EMAIL_VERIFICATION_REQUIRED` | Confirm email via token |
| No welcome / OTP email | `MAIL_*` in `.env`, or use `OTP_DELIVERY_CHANNEL=log` |
| 401 on login | Wrong identifier; use email for staff, phone for citizens |
| Duplicate partner code | Use a unique `code` per test run (e.g. `DEMO_INSURER_2`) |

---

## Related docs

- [`frontend-user-management-and-onboarding.md`](frontend-user-management-and-onboarding.md) — FE integration
- [`frontend-auth-without-sms.md`](frontend-auth-without-sms.md) — email OTP for citizens
