# Ingoboka Backend Work Brief for Rodin

**Repo:** `Ingoboka-api-v1` (to become platform `backend/`)  
**Audience:** Rodin Mahinga — backend lead  
**Prepared by:** Arnold / platform team  
**Date:** 18 June 2026  
**Status:** Action list for MVP integration with Next.js frontend

---

## 1. Context

We are standardizing on **your backend** as the single Ingoboka API. The platform frontend (`frontend/`), the **Enterprise System Concept v1** PDF, and the **Technical System Blueprint** PDF all target:

- Base URL: `http://localhost:8080/api/v1`
- Response wrapper: `{ "success": true, "message": "...", "data": { ... } }`
- Spring Boot modular monolith, PostgreSQL, Flyway, JWT, RBAC

Your latest code (V1–V14 migrations, 21 controllers) already covers most domains. This brief lists what to **build**, **edit**, and **align** so the hackathon MVP demo works end-to-end with our frontend.

**Reference documents (in platform repo):**

- `Ingoboka_Enterprise_System_Concept_v1.pdf` — services S01–S18, user journeys, MVP scope
- `Ingoboka_Technical_System_Blueprint.pdf` — service layer, DB rules, core API contracts (§11), acceptance criteria (§12)
- Frontend API clients: `frontend/src/lib/api/*.ts`

**Definition of done (from blueprint §12):** One complete citizen journey (register → product → application → payment → policy → claim) and one insurer journey (review claim → decision), with persistent data, separate roles, and reproducible local setup.

---

## 2. What you already have (do not rebuild)

These modules are in good shape. Extend them only where noted below.

| Area | Current state |
|------|----------------|
| Identity & JWT | Auth, refresh, OTP service, RBAC, platform admin seed |
| Customer | Profile, dependants, consents |
| Product catalog | Create, publish, plans, benefits, eligibility |
| Enrollment | Quote, application, underwriter review |
| Policy | Issuance, lifecycle, premium schedules, QR verify |
| Billing & payments | Initiate, webhooks, MoMo, idempotency, outbox |
| Billing finance | Bills, receipts, refunds, reconciliation |
| Claims | Full workflow, documents, decisions, appeals |
| Documents | MinIO presigned upload/download |
| Partner | Onboarding, contracts, staff |
| Revenue | Price rules, ledger, invoices |
| Reporting | Overview, policy/claim/payment reports, CSV export |
| Admin dashboard | Basic platform counts |
| Agent dashboard | Counts only (needs expansion) |
| Notifications | List, mark read |
| Audit | Logs, GDPR data-subject requests |
| Integrations | Registry, MoMo sandbox seed (V13/V14) |

---

## 3. Priority overview

| Priority | Focus | Why |
|----------|-------|-----|
| **P0** | Auth, enrollment/payment path, policy wallet, portal aliases | Blocks frontend demo |
| **P1** | KYC submit, agent assisted enrollment, reporting shapes, dev seed | Blueprint MVP gaps |
| **P2** | Complaints, SMS adapter, tests, ops alignment | Polish & definition of done |

---

## 4. P0 — Must fix first (frontend blockers)

### 4.1 Authentication & citizen onboarding (S01)

**Problem:** Frontend is phone-first; your API is email-first. Field names and response shapes differ.

| Action | Detail |
|--------|--------|
| **Add** `POST /api/v1/auth/register` | Alias or replacement for signup. Accept: `{ fullName, phone, nationalId, password }`. Split `fullName` into first/last internally. Email optional for citizens. |
| **Edit** `POST /api/v1/auth/signup` | Keep for staff/email users OR delegate to same service as register. |
| **Edit** `POST /api/v1/auth/login` | Accept **phone OR email** + password (frontend sends one identifier). |
| **Edit** `POST /api/v1/auth/verify-otp` | Accept `{ phone, code }` **or** `{ phoneNumber, otp }` (support both). **Return tokens + user** after verify (auto-login), not void. |
| **Add** `POST /api/v1/auth/logout` | Revoke refresh token; idempotent. |
| **Edit** OTP delivery | Send signup OTP via **SMS channel** in dev (log adapter OK). Today OTP goes to email template — citizens expect phone OTP. |
| **Edit** auth user response | Map to frontend shape: `{ id, fullName, phone, email?, role, verified, consentGiven }`. Single primary `role` string (e.g. `CITIZEN`, `PLATFORM_ADMIN`, `INSURER_CLAIMS_OFFICER`). |

**Blueprint reference:** Concept §10.3 — `POST /api/v1/auth/register`, `POST /api/v1/auth/login`.

---

### 4.2 Customer profile & consent (S02, S03)

| Action | Endpoint | Detail |
|--------|----------|--------|
| **Add alias** | `GET /api/v1/customers/me` | Same as `GET /customers/me/profile` for citizens. |
| **Add alias** | `POST /api/v1/customers/consent` | Same as `POST /customers/me/consents`. Accept: `{ dataProcessing, marketing?, termsAccepted }`. |
| **Add alias** | `GET/POST/DELETE /api/v1/customer/dependants` | Same as `/customers/me/dependants` (frontend uses `/customer/dependants`). |
| **Add** | `POST /api/v1/customer/kyc/submit` | Citizen submits KYC for review. Set `kycStatus = SUBMITTED`. Use existing `nationalId` hash on profile. Duplicate detection message if hash exists. |
| **Keep** | `PATCH /api/v1/customers/kyc/review` | Admin/underwriter approve/reject (already exists). |

**Blueprint reference:** S03 KYC sandbox; Technical Blueprint — `citizen_profiles.kyc_status`.

---

### 4.3 Enrollment & quotation (S04, S06)

**Problem:** Frontend uses a simplified flow; your flow requires quote + consentId + underwriter approval before payment.

**Frontend flow today:**

```
POST /applications/quote          { productPlanId }
POST /applications                { productPlanId, beneficiaries: [] }
POST /applications/{id}/submit
POST /payments/initiate           { applicationId }
POST /payments/sandbox/callback   { providerReference, status }
```

**Your flow today:**

```
POST /applications/quote            { productPlanId, answers }
POST /applications                { quoteId, consentId }
PATCH /applications/{id}/review   (underwriter APPROVED)
→ policy PENDING_PAYMENT
POST /payments                    { policyId }
POST /payments/webhooks/{provider}
```

| Action | Detail |
|--------|--------|
| **Add** | `POST /api/v1/applications/needs-assessment` — Input: `{ occupation, incomeRange?, dependents?, primaryRisk? }`. Output: `{ score, guidance, recommendedCategories? }`. |
| **Edit** | `POST /api/v1/applications/quote` — Accept `{ productPlanId }` without full profile where possible; return affordability warning in response. |
| **Add** | `POST /api/v1/applications` shortcut — Accept `{ productPlanId }` (creates quote + draft application) for MVP demo. |
| **Add** | `POST /api/v1/applications/{id}/submit` — Validate completeness and move to SUBMITTED. |
| **Add** | **Sandbox auto-approve** — In `dev`/`sandbox` profile: auto-approve citizen self-service applications → issue policy `PENDING_PAYMENT` without manual underwriter step. Keep manual review for production insurer tenants. |
| **Add alias** | `POST /api/v1/enrollments` — Optional alias: create + submit application in one call (blueprint §10.3). |

**Blueprint reference:** Concept §9.1 citizen journey steps 3–7; Technical Blueprint §11 core contracts.

---

### 4.4 Payments (S07)

| Action | Endpoint | Detail |
|--------|----------|--------|
| **Add alias** | `POST /api/v1/payments/initiate` | Same as `POST /payments`. Accept `{ policyId }` **or** `{ applicationId }` (resolve to policy or auto-issue after approval). |
| **Add alias** | `POST /api/v1/payments/sandbox/callback` | Demo callback for frontend enroll page. Map to existing webhook processor. Body: `{ providerReference, status: "SUCCESS" \| "FAILED" }`. |
| **Add** | `GET /api/v1/payments/{id}/status` | Poll payment status: `{ id, status, paymentReference }`. |
| **Keep** | Idempotency keys on initiate + webhooks | Already partially implemented — document in OpenAPI. |

**Blueprint reference:** S07 sandbox mobile-money, webhook handling, idempotency, receipts.

---

### 4.5 Policy wallet & QR verification (S08)

| Action | Endpoint | Detail |
|--------|----------|--------|
| **Add alias** | `GET /api/v1/policies` | For citizens: same as `GET /policies/me`. Return list (not only paginated wrapper if frontend expects array — or document paginated shape). |
| **Add** | `GET /api/v1/policies/{id}/card` | Digital policy card: policy number, holder name, product name, status, premium, coverage dates, **qrToken**, verification URL. |
| **Add alias** | `GET /api/v1/verify/{token}` | Public, no auth. Same as `GET /policies/verify/{token}`. Minimum non-PII verification data. |

**Blueprint reference:** Concept §10.3 — `GET /policies/{id}/card`, `GET /verify/{token}`.

---

### 4.6 Claims — insurer portal paths (S09)

Logic exists; frontend uses `/admin/claims` prefix.

| Action | Endpoint | Detail |
|--------|----------|--------|
| **Add alias** | `GET /api/v1/admin/claims` | Same as tenant `GET /claims` for claims officers. |
| **Add alias** | `GET /api/v1/admin/claims/{id}` | Same as `GET /claims/{id}`. |
| **Add alias** | `POST /api/v1/admin/claims/{id}/decision` | Same as `POST /claims/{id}/decision`. |
| **Add** | `GET /api/v1/admin/reports/overview` | Insurer stats: `{ pendingClaims, approvedClaims, rejectedClaims, openClaims? }`. |
| **Add** | `GET /api/v1/admin/reports/claims-breakdown` | `{ resolvedToday, avgResolutionDays, claimsByStatus: [{ status, count }] }`. |

**Keep:** `POST /claims`, `POST /claims/{id}/submit`, `POST /claims/{id}/documents`, `POST /claims/{id}/appeals`.

---

### 4.7 Admin, agent & insurer portals

#### Platform admin

| Action | Endpoint | Detail |
|--------|----------|--------|
| **Add alias** | `GET /api/v1/admin/platform/overview` | Extend current `/admin/dashboard` with: `{ organizations, activeUsers, activePolicies, openClaims, totalApplications }`. |
| **Add** | `GET /api/v1/admin/platform/organizations` | List all organizations: `{ id, name, slug, organizationType, status, contactEmail? }`. |

#### Agent (field officer)

| Action | Endpoint | Detail |
|--------|----------|--------|
| **Add** | `GET /api/v1/agent/applications` | Paginated list of applications agent may view (tenant-scoped). |
| **Add** | `POST /api/v1/agent/applications` | Assisted enrollment: `{ citizenPhone, productPlanId }`. Create/link citizen, start application. **Audit log required.** |
| **Keep** | `GET /api/v1/agent/dashboard` | Optional summary counts. |

**Blueprint reference:** Concept §10.1 Agent role — assisted sessions, no claim approval.

#### Insurer settings & partner revenue

| Action | Endpoint | Detail |
|--------|----------|--------|
| **Add alias** | `GET /api/v1/admin/organizations/me` | Same as `GET /insurer/settings` for logged-in partner admin. |
| **Add alias** | `PATCH /api/v1/admin/organizations/me` | Same as `PUT /insurer/settings`. |
| **Add alias** | `GET /api/v1/admin/partner/invoices` | Tenant-scoped: same as `GET /revenue/invoices` (no partnerId in path). |
| **Add alias** | `GET /api/v1/admin/partner/contracts` | Tenant-scoped contracts for current partner org. |
| **Add alias** | `GET /api/v1/admin/partner/revenue/ledger` | Tenant-scoped ledger. |
| **Add alias** | `GET/POST /api/v1/admin/products`, `POST .../publish` | Same as `/products/tenant`, `POST /products`, `POST /products/{id}/publish`. |

---

## 5. P1 — Blueprint MVP completion

### 5.1 Notifications (S12)

| Action | Detail |
|--------|--------|
| **Edit** notification triggers | Fire on: OTP sent, payment success, policy activated, claim submitted, claim status change, claim decision. |
| **Keep** | `GET /notifications/me`, `PATCH /notifications/{id}/read`. |

### 5.2 Appeals & support (S10)

| Action | Detail |
|--------|--------|
| **Keep** | `POST /claims/{id}/appeals` (already exists). |
| **Add** | Basic complaints: `POST /api/v1/support/complaints`, `GET /api/v1/support/complaints/me`. Statuses: OPEN → ASSIGNED → RESOLVED. Table: `support_tickets` (blueprint schema). MVP basic is enough. |

### 5.3 Reporting (S14)

| Action | Detail |
|--------|--------|
| **Keep** | `/reports/overview`, `/reports/policies`, `/reports/claims`, CSV exports. |
| **Add** | Insurer dashboard-specific aggregates (see §4.6). |
| **Add alias** | `GET /api/v1/admin/reports/policies` → same as `/reports/policies` for partner admin. |

### 5.4 Audit & compliance (S15)

| Action | Detail |
|--------|--------|
| **Edit** audit coverage | Log: consent grants, agent assisted actions, claim decisions, payment callbacks, KYC status changes. |
| **Keep** | `/audit/logs`, data-subject request APIs. |

### 5.5 Integration adapters (S16)

| Action | Detail |
|--------|--------|
| **Add** | SMS adapter interface + `log` implementation (like payment sandbox). Wire OTP to SMS in dev. |
| **Keep** | MoMo sandbox adapter, payment webhooks, integration registry. |
| **Document** | OpenAPI tags for each adapter; callback URLs under `/api/v1/payments/webhooks/*`. |

### 5.6 Dev seed data (Technical Blueprint §10)

| Action | Detail |
|--------|--------|
| **Add** | Flyway seed migration **`V99__dev_demo_seed.sql`** (or profile `dev` only — **never auto-run in production**). |

**Suggested demo accounts:**

| Role | Identifier | Password |
|------|------------|----------|
| Citizen | Phone `0780000001` | `Ingoboka@2026` |
| Insurer admin | `eric@demo-insurer.rw` | `Ingoboka@2026` |
| Claims officer | `claims@demo-insurer.rw` | `Ingoboka@2026` |
| Agent | `agent@demo-insurer.rw` / `0780000099` | `Ingoboka@2026` |
| Platform admin | `admin@ingoboka.rw` | `Ingoboka@2026` |

**Also seed:** one published personal-accident product with daily/weekly/monthly plans, one demo partner org.

---

## 6. P2 — Quality, ops & contract stability

### 6.1 API contract rules

- **Lists:** paginated `{ content, page, size, totalElements, totalPages }` everywhere.
- **Auth response:** `{ accessToken, refreshToken, expiresIn, user }` — frontend unwraps `data` from `ApiResponse`.
- **Errors:** `{ message, code?, status? }` on 4xx/5xx.
- **Versioning:** stay on `/api/v1/*`; do not introduce breaking path changes without aliases.

### 6.2 Configuration alignment

Edit `application.properties` / `.env.example` to match platform `docker-compose.yml`:

| Setting | Platform compose | Your default today | Action |
|---------|------------------|---------------------|--------|
| DB password | `ingoboka_dev_secret` | `ingoboka` | Align default or document env var |
| MinIO user | `ingoboka_minio` | `minioadmin` | Align defaults |
| MinIO password | `ingoboka_minio_secret` | `minioadmin` | Align defaults |
| CORS | `http://localhost:3000` | `*` | Restrict in dev example |
| OTP storage | Redis available | Redis required | Document `OTP_STORAGE=memory` fallback |

### 6.3 Tests (Technical Blueprint §12)

| Action | Detail |
|--------|--------|
| **Add** integration test | Register → OTP verify → login → quote → application → payment webhook → policy ACTIVE → create claim → submit → record decision. |
| **Add** Testcontainers | PostgreSQL (+ Redis optional) in CI. |
| **Fix deploy** | Remote `:8085` — `/v3/api-docs` returned 500; ensure health + Swagger work after deploy. |

### 6.4 Documentation

| Action | Detail |
|--------|--------|
| **Add** `README.md` | Local run: Docker services, env vars, `./mvnw spring-boot:run`, Swagger URL. |
| **Keep OpenAPI accurate** | Every alias endpoint documented with example request/response. |

---

## 7. Endpoint checklist (quick reference)

Use this as a sprint board. Mark ✅ when done.

### Auth
- [x] `POST /auth/register`
- [x] `POST /auth/login` (phone or email)
- [x] `POST /auth/verify-otp` → returns tokens + user
- [x] `POST /auth/resend-otp`
- [x] `POST /auth/refresh`
- [x] `POST /auth/logout`

### Customer
- [x] `GET /customers/me` (alias)
- [x] `POST /customers/consent` (alias)
- [x] `GET/POST/DELETE /customer/dependants` (aliases)
- [x] `POST /customer/kyc/submit`

### Enrollment
- [x] `POST /applications/needs-assessment`
- [x] `POST /applications/quote`
- [x] `POST /applications` (productPlanId shortcut)
- [x] `POST /applications/{id}/submit`
- [x] Sandbox auto-approve → policy PENDING_PAYMENT

### Payments
- [x] `POST /payments/initiate` (alias)
- [x] `POST /payments/sandbox/callback` (alias)
- [x] `GET /payments/{id}/status`

### Policies
- [x] `GET /policies` (alias for /policies/me)
- [x] `GET /policies/{id}/card`
- [x] `GET /verify/{token}` (public alias)

### Claims
- [x] `GET/POST /claims/*` (citizen — exists)
- [x] `GET /admin/claims`, `GET /admin/claims/{id}`
- [x] `POST /admin/claims/{id}/decision`
- [x] `GET /admin/reports/overview`
- [ ] `GET /admin/reports/claims-breakdown`

### Portals
- [x] `GET /admin/platform/overview`
- [ ] `GET /admin/platform/organizations`
- [x] `GET/POST /agent/applications`
- [ ] `GET/PATCH /admin/organizations/me`
- [ ] `GET /admin/partner/invoices`, `/contracts`, `/revenue/ledger`
- [ ] `GET/POST /admin/products`, publish

### Data & ops
- [ ] `V99__dev_demo_seed.sql`
- [x] Env defaults aligned with docker-compose (MTN SMS + sandbox auto-approve)
- [ ] End-to-end integration test
- [ ] README + working Swagger on deploy

---

## 8. Suggested sprint order

1. **Week 1 — Unblock demo:** §4.1 Auth → §4.3 Enrollment/payment → §4.5 Policy card/verify → dev seed
2. **Week 2 — Portals:** §4.6 Claims aliases → §4.7 Admin/agent/insurer aliases → §5.3 Reporting shapes
3. **Week 3 — Hardening:** §5 KYC, notifications, audit → §6 Tests, ops, docs

---

## 9. Out of scope for now (do not build yet)

Per blueprint §12:

- Blockchain, facial recognition, ML underwriting
- Full microservices split
- Custom workflow engine
- Production MTN MoMo / live SMS (sandbox + adapters only)
- Fraud signals (S18) — future

---

## 10. Questions for Rodin

1. Prefer **route aliases** (frontend unchanged) or **frontend retarget** to your existing paths?
2. OK to **auto-approve** sandbox citizen applications for hackathon demo?
3. Timeline for P0 before we merge `Ingoboka-api-v1` into platform `backend/`?

---

*Share this file with Rodin. Update checkboxes in §7 as work completes.*
