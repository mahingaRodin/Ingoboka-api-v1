# Ingoboka Backend Remaining Work — Brief for Rodin

**Date:** 28 June 2026  
**Frontend repo:** [Arn-The-Wolf/ingoboka-platform](https://github.com/Arn-The-Wolf/ingoboka-platform) (deployed: [ingoboka-platform.vercel.app](https://ingoboka-platform.vercel.app))  
**Backend repo:** [mahingaRodin/Ingoboka-api-v1](https://github.com/mahingaRodin/Ingoboka-api-v1)  
**API base:** `/api/v1`  
**Audit source:** Cloned `Ingoboka-api-v1` to `.tmp-api/` (commit at clone time), all `src/lib/api/*.ts`, hooks, portal pages, and PDFs in repo root.

---

## Completion summary

| Layer | % | Notes |
|-------|---|-------|
| **Frontend** | **84%** | All four portals + marketing + enroll/claim flows are built and wired to API clients. Gaps: sandbox-only payment UX (no status polling), `customerApi.getMe()` discards API response, insurer `REQUEST_INFO` decision not mapped to backend, dependants/KYC pages exist but are thin. |
| **Backend** | **68%** | Rodin's repo is a substantial modular monolith (~25 controllers, Flyway V1–V10, `FrontendCompatController`). Most MVP routes exist. Gaps: 5+ frontend path aliases missing, claim document upload contract mismatch (JSON vs multipart), consent idempotency bug in enrollment, no `/admin/platform/settings`, production MoMo/storage/CORS not hardened. |
| **Integration** | **47%** | Vercel → Kamatera path is fragile: remote host (`185.181.10.165:8085`) has had unhealthy actuator/Swagger; CORS must whitelist Vercel; MinIO presigned URLs break `heroImageUrl`/product documents off-server; 11 frontend calls hit wrong path or wrong payload shape. End-to-end demo works locally with Docker Compose + seed data when env vars align. |

**MVP journey (concept doc §15.1) completion: ~52%** — register → consent → needs assessment → enroll → pay → policy → claim → insurer decision → appeal is coded on both sides but several hops fail on the deployed stack without fixes below.

---

## MVP journey map (S01–S09)

Aligned with `Ingoboka_Enterprise_System_Concept_v1.pdf` §9.1–9.3 and §15.1 acceptance journey.

| Step | Journey | Frontend | Backend | Integration status |
|------|---------|----------|---------|-------------------|
| **S01** | Register, OTP verify, login | `auth.ts`, `(auth)/register`, `(auth)/verify`, `(auth)/login` | `AuthController` `/api/v1/auth/*` | **Works** locally; email OTP needs SMTP on Kamatera |
| **S02** | Consent (terms, data processing) | `customerApi.submitConsent`, `(auth)/consent` | `FrontendCompatController` `POST /customers/consent`; `GET /customers/me/consents` | **Partial** — duplicate consent → 500 (see P0) |
| **S03** | Needs assessment | `enrollmentApi.needsAssessment`, `products/needs-assessment` | `POST /applications/needs-assessment` | **Works** |
| **S04** | Product catalog + detail | `productApi.list/getById`, `products/[id]` | `GET /products`, `GET /products/{id}/detail` | **Partial** — `heroImageUrl`, `documents[].downloadUrl` null if MinIO unreachable |
| **S05** | Quote, application, submit | `enrollmentApi.*`, `products/[id]/enroll` | `POST /applications/quote`, `POST /applications`, `POST /applications/{id}/submit` | **Works** with `SANDBOX_AUTO_APPROVE=true` (issues policy before payment) |
| **S06** | Payment → active policy | `paymentApi.initiate`, enroll page sandbox callback | `FrontendCompatController` payments + `BillingService` | **Sandbox only** — frontend simulates callback; no MoMo poll/webhook in UI |
| **S07** | Policy wallet, card, QR verify | `policyApi.*`, `dashboard`, `policies/[id]/card`, `verify/[token]` | `GET /policies`, `/policies/{id}/card`, `/verify/{token}`, `/policies/me/activity` | **Works** when policy issued |
| **S08** | Create claim, upload docs, submit | `claimApi.*`, `claims/new` | `ClaimController` create/submit/documents | **Broken upload** — multipart vs JSON metadata (see P0) |
| **S09** | Insurer review, decision, appeal | insurer `claims/[id]`, citizen `claims/[id]` appeal | `admin/claims/*`, `POST /claims/{id}/appeals` | **Partial** — `REQUEST_INFO` not supported on decision endpoint; appeals OK if claim `REJECTED` |

---

## P0 — Blocks production demo

### 1. Claim document upload — contract mismatch

| | |
|---|---|
| **Frontend** | `POST /claims/{id}/documents` — `multipart/form-data`, field `files` (repeatable). `src/lib/api/claims.ts`, `claims/new/page.tsx` |
| **Backend** | `ClaimController` `POST /api/v1/claims/{claimId}/documents` expects JSON `AttachClaimDocumentRequest`: `{ documentType, objectKey, mimeType, sizeBytes, checksum }` — metadata only, no file bytes |
| **Fix** | Add compat endpoint (or change existing) accepting `MultipartFile[] files`, upload to MinIO via `DocumentStorageService`, then call `claimService.attachDocument` per file. Return uploaded doc list. |
| **Error today** | `415 Unsupported Media Type` or `400` validation failure; frontend catches and redirects with `?uploadPartial=1` |

### 2. Insurer enrollment review — wrong path and payload

| | |
|---|---|
| **Frontend** | `GET /insurer/applications?status=PENDING`, `POST /insurer/applications/{id}/decision` body `{ decision: "APPROVED"\|"REJECTED", reason? }` — `src/lib/api/admin.ts` `insurerApi`, `insurer/applications/page.tsx` |
| **Backend** | `GET /api/v1/applications?status=…` (not under `/insurer`), `PATCH /api/v1/applications/{id}/review` body `{ status: ApplicationStatus, decisionReason? }` where status is `APPROVED`/`REJECTED`/`UNDER_REVIEW` etc. |
| **Fix** | Add to `FrontendCompatController`: alias GET `/api/v1/insurer/applications` → delegate to `enrollmentService.listTenantApplications`; alias POST `/api/v1/insurer/applications/{id}/decision` → map `decision` to `ReviewApplicationRequest.status` and `reason` → `decisionReason`, call `reviewApplication`. |
| **Note** | Frontend filters `PENDING` but backend enum has `SUBMITTED`, `UNDER_REVIEW` — map or add `PENDING` alias in query handling. |

### 3. Consent re-grant → HTTP 500 on duplicate TERMS

| | |
|---|---|
| **Frontend** | `POST /customers/consent` with `{ termsAccepted, dataProcessing, marketing }`; pre-filters via `GET /customers/me/consents` — `src/lib/api/auth.ts` |
| **Backend** | `CustomerProfileServiceImpl.grantConsent` revokes-then-inserts (OK). **`EnrollmentServiceImpl.grantConsentForUser`** inserts without revoke — violates partial unique index `idx_consents_active_user_type` on `(user_id, consent_type) WHERE granted AND revoked_at IS NULL` (`V5__create_customer_and_consent.sql`) |
| **Repro** | Citizen completes consent page, then enrolls (`createQuickApplication` auto-grants `DATA_PROCESSING`) → 500 if active consent already exists |
| **Fix** | Reuse revoke-then-grant logic in `grantConsentForUser`; make `grantFrontendConsent` / `grantConsent` idempotent (return 200 + existing record if already active). |

### 4. Admin audit log — path mismatch

| | |
|---|---|
| **Frontend** | `GET /admin/audit-logs?page&size` — `adminApi.listAuditLog`, `admin/dashboard`, `admin/audit` |
| **Backend** | `GET /api/v1/audit/logs` — `AuditController` |
| **Fix** | Add `FrontendCompatController` alias `GET /api/v1/admin/audit-logs` → `auditComplianceService.listAuditLogs`. Map response fields: frontend expects `action`, `actor`/`actorName`, `resource`/`resourceType`, `occurredAt`/`createdAt`, `details`/`description`. |

### 5. CORS + Vercel production origin

| | |
|---|---|
| **Frontend** | Browser calls `NEXT_PUBLIC_API_BASE_URL` from `ingoboka-platform.vercel.app` |
| **Backend** | `CorsConfig` reads `ingoboka.cors.allowed-origins` (default `*` in dev; docker-compose sets env) |
| **Fix** | On Kamatera set `CORS_ALLOWED_ORIGINS=https://ingoboka-platform.vercel.app,https://www.ingoboka-platform.vercel.app` (and preview URLs if needed). Wildcard + `allowCredentials(true)` can break credentialed requests in some browsers. |

### 6. Remote API health (Kamatera)

Prior audits: `http://185.181.10.165:8085` — Swagger UI loads but `/v3/api-docs` returned 500, `/actuator/health` 503. Frontend shows network/generic errors across all portals until API is healthy.

**Fix checklist:** Postgres + Redis + MinIO up; `JWT_SECRET` set; `MAIL_HEALTH_ENABLED=false` if SMTP not configured; verify `GET /actuator/health/liveness` → 200; expose port 8085 (or reverse proxy TLS).

---

## P1 — MVP completeness

### 7. Admin platform settings — endpoint missing

| | |
|---|---|
| **Frontend** | `GET /admin/platform/settings` — `adminApi.getPlatformSettings`, `admin/settings/page.tsx` |
| **Backend** | No route. Config exists in `application.properties` (`ingoboka.platform.name`, etc.) but not exposed |
| **Fix** | `GET /api/v1/admin/platform/settings` returning `{ platformName, defaultLocale, maintenanceMode, apiBaseUrl, supportEmail }` from `@ConfigurationProperties` |

### 8. Payment — real MoMo + status polling (not sandbox simulate)

| | |
|---|---|
| **Frontend** | Enroll page calls `POST /payments/sandbox/callback` directly after `paymentApi.initiate` — no `paymentApi.getStatus` polling — `products/[id]/enroll/page.tsx` |
| **Backend** | `BillingController` `POST /payments/webhooks/momo`; `MomoSandboxPaymentAdapter`; `GET /payments/{id}/status` exists in compat layer |
| **Fix (backend)** | Document webhook URL for Kamatera; env for MoMo credentials; ensure `initiatePayment` returns `instructions` + `providerReference` for USSD push |
| **Fix (frontend — Arnold)** | Replace sandbox button with poll `GET /payments/{id}/status` every 3s; optional — Rodin only needs stable status endpoint + webhook |

### 9. Claim insurer decision — `REQUEST_INFO` unsupported

| | |
|---|---|
| **Frontend** | Insurer claim detail sends `decision: "REQUEST_INFO"` via `claimApi.decide` → maps to `REQUEST_INFO` string in body — `insurer/claims/[id]/page.tsx` |
| **Backend** | `RecordClaimDecisionRequest.decision` is `ClaimDecisionType`: `APPROVED`, `REJECTED`, `PARTIAL` only. `PATCH /claims/{id}/status` supports `UpdateClaimStatusRequest` with `INFO_REQUESTED` |
| **Fix** | Either extend compat `admin/claims/{id}/decision` to accept `REQUEST_INFO` and call `updateStatus`, or document that frontend must call `PATCH /claims/{id}/status` |

### 10. `GET /customers/me` — frontend ignores response (blocks consent routing)

| | |
|---|---|
| **Frontend** | `customerApi.getMe()` calls API then `return mapAuthUser(null)` — always empty user — `src/lib/api/auth.ts:124-126` |
| **Backend** | `GET /api/v1/customers/me` returns `CitizenProfileResponse` (profile fields, not auth user shape) |
| **Fix (Arnold)** | Map profile + embed role/consent from JWT or extend response with `user` block |
| **Fix (Rodin)** | Optional: include `consentGiven`, `role`, `fullName` on `CitizenProfileResponse` for session refresh |

### 11. Product `heroImageUrl` and documents on remote deploy

| | |
|---|---|
| **Backend** | `ProductCatalogServiceImpl` resolves `heroImageUrl` via `StorageUrlResolver` → MinIO presigned URL. If MinIO down/disabled, returns `null` |
| **Symptom** | Product cards and detail pages show gradient fallback; Documents tab empty on Vercel |
| **Fix** | Ensure MinIO in Kamatera compose; seed demo products with `heroImageKey`; set public CDN or proxy path for read-only assets; document `MINIO_ENABLED`, bucket policy |

### 12. Application status filter `PENDING` vs backend enums

Frontend `insurerApi.listApplications(..., 'PENDING')` — backend `ApplicationStatus` has `SUBMITTED`, `UNDER_REVIEW`, not `PENDING`. Alias in list endpoint or map query param.

### 13. `CreateClaimRequest` missing `incidentDate`

Frontend sends `incidentDate` on claim create; backend `CreateClaimRequest` has no field — silently dropped. Add column + DTO if timeline/SLA needs it.

### 14. Auth refresh on page load

Frontend stores tokens in Zustand but never calls `GET /customers/me` or refresh to hydrate user after reload. Backend refresh token flow exists (`POST /auth/refresh`). Integration gap for long sessions.

---

## P2 — Nice to have

- **Notifications inbox** — backend `GET /notifications/me`; no frontend page yet
- **Data subject requests** — `AuditController` `/audit/data-subject-requests/*`; no frontend
- **Partner staff CRUD** — `/partner/staff/*`, `/partners/{id}/staff/*`; insurer UI doesn't expose
- **Premium schedules / policy renew** — `GET /policies/{id}/premium-schedules`, `POST .../renew`
- **Billing finance** — refunds, reconciliation (`/billing/*`)
- **Integration adapters** — `/integrations/{code}/invoke`
- **CSV exports** — `/reports/export/*` for insurer reports page
- **Real SMS OTP** — `MTN_BULK_SMS_ENABLED`, production phone verify
- **Email verification** for insurer staff (citizens use phone OTP)
- **Beneficiary capture** on enrollment (concept §15.2; DB has `application_beneficiaries`, no frontend step)

---

## Endpoint contract table

Base: `{NEXT_PUBLIC_API_BASE_URL}` = `/api/v1`. Status reflects **frontend expectation vs `.tmp-api` clone**.

| Method | Path | Frontend file | Status | Request/Response notes |
|--------|------|---------------|--------|------------------------|
| GET | `/auth/otp-delivery-config` | `auth.ts` | **exists** | Returns `{ deliveryChannel, requiresEmail, smsAvailable, verifyHint }`; frontend falls back on error |
| POST | `/auth/login` | `auth.ts` | **exists** | Body `{ identifier, password }`; response `{ accessToken, refreshToken, user }` unwrapped by client |
| POST | `/auth/register` | `auth.ts` | **exists** | Body `{ fullName, phone, email?, password, nationalId? }` |
| POST | `/auth/verify-otp` | `auth.ts` | **exists** | Body `{ phone, phoneNumber, code, otp }` (dual keys for compat) |
| POST | `/auth/resend-otp` | `auth.ts` | **exists** | Body `{ phone, phoneNumber }` |
| POST | `/auth/refresh` | `auth.ts` | **exists** | Body `{ refreshToken }` |
| POST | `/auth/logout` | `auth.ts` | **exists** | Body `{ refreshToken? }` |
| GET | `/customers/me/consents` | `auth.ts` | **exists** | Paginated `{ content: [{ consentType, granted, revokedAt }] }` |
| GET | `/customers/me` | `auth.ts` | **broken** | Backend returns profile; frontend ignores body |
| POST | `/customers/consent` | `auth.ts` | **broken** | Body `{ termsAccepted, dataProcessing, marketing }`; 500 on duplicate via enrollment path |
| GET | `/claims/me` | `claims.ts` | **exists** | Citizen list; paginated `ClaimResponse` |
| GET | `/admin/claims` | `claims.ts` | **exists** | Insurer list (compat controller) |
| POST | `/claims` | `claims.ts` | **exists** | `{ policyId, claimType, description, claimedAmount, incidentDate? }` — `incidentDate` not persisted |
| POST | `/claims/{id}/submit` | `claims.ts` | **exists** | No body |
| GET | `/claims/{id}` | `claims.ts` | **exists** | Includes `statusHistory[]` |
| GET | `/admin/claims/{id}` | `claims.ts` | **exists** | Insurer detail |
| POST | `/admin/claims/{id}/decision` | `claims.ts` | **partial** | Body `{ decision: APPROVED\|REJECTED, reason }`; `REQUEST_INFO` fails validation |
| POST | `/claims/{id}/appeals` | `claims.ts` | **exists** | Body `{ reason }` → `CreateClaimAppealRequest` |
| POST | `/claims/{id}/documents` | `claims.ts` | **broken** | Frontend: multipart `files[]`; Backend: JSON metadata only |
| GET | `/admin/reports/overview` | `claims.ts`, `admin.ts` | **exists** | `{ openClaims, activePolicies, pendingApplications?, citizensEnrolled? }` |
| GET | `/admin/reports/claims-breakdown` | `claims.ts` | **exists** | `{ claimsByStatus[], resolvedToday?, avgResolutionDays? }` |
| GET | `/policies` | `policies.ts` | **exists** | Compat alias → citizen policies page |
| GET | `/policies/{id}` | `policies.ts` | **exists** | `PolicyController` |
| GET | `/policies/{id}/card` | `policies.ts` | **exists** | `{ policyNumber, holderName, qrToken, … }` |
| GET | `/verify/{token}` | `policies.ts` | **exists** | Public, no auth |
| GET | `/policies/me/activity` | `policies.ts` | **exists** | Paginated activity feed |
| POST | `/payments/initiate` | `payments.ts` | **exists** | `{ policyId? \| applicationId? }` — requires policy linked to application (sandbox auto-approve creates it) |
| GET | `/payments/{id}/status` | `payments.ts` | **exists** | Not used by enroll UI yet |
| POST | `/payments/sandbox/callback` | `enroll/page.tsx` | **exists** | Public; `{ providerReference, status: "SUCCESS" }` — demo only |
| GET | `/products` | `products.ts` | **exists** | Paginated product list |
| GET | `/products/tenant` | `products.ts` | **exists** | Insurer admin catalog |
| POST | `/products` | `products.ts` | **exists** | Create product shell |
| POST | `/products/{id}/plans` | `products.ts` | **exists** | Plan + benefits/exclusions arrays |
| POST | `/products/{id}/publish` | `products.ts` | **exists** | |
| GET | `/products/{id}/detail` | `products.ts` | **partial** | `{ product, plans, faq?, claimSteps?, documents? }`; URLs depend on MinIO |
| POST | `/applications/quote` | `products.ts` | **exists** | `{ productPlanId }` |
| POST | `/applications` | `products.ts` | **exists** | `{ productPlanId }` quick create |
| POST | `/applications/{id}/submit` | `products.ts` | **exists** | Triggers sandbox auto-approve when enabled |
| POST | `/applications/needs-assessment` | `products.ts` | **exists** | `{ occupation, incomeRange?, dependents?, primaryRisk? }` |
| GET | `/admin/platform/overview` | `admin.ts` | **exists** | Platform admin KPIs |
| GET | `/partners` | `admin.ts` | **exists** | Maps to organizations list |
| GET | `/admin/users` | `admin.ts` | **exists** | Paginated; map `ManagedUserResponse` → frontend `AdminUser` |
| GET | `/admin/audit-logs` | `admin.ts` | **missing** | Backend: `/audit/logs` |
| GET | `/admin/platform/settings` | `admin.ts` | **missing** | |
| GET | `/agent/applications` | `admin.ts` | **exists** | Compat controller |
| POST | `/agent/applications` | `admin.ts` | **exists** | `{ citizenPhone, productPlanId }` |
| GET | `/customer/dependants` | `admin.ts` | **exists** | Compat path (also `/customers/me/dependants` native) |
| POST | `/customer/dependants` | `admin.ts` | **exists** | `{ firstName, lastName, relationship, dateOfBirth? }` |
| DELETE | `/customer/dependants/{id}` | `admin.ts` | **exists** | 204 |
| POST | `/customer/kyc/submit` | `admin.ts` | **exists** | Requires `nationalId` on profile or 400 |
| GET | `/insurer/settings` | `admin.ts` | **exists** | `PartnerSettingsController` |
| PUT | `/insurer/settings` | `admin.ts` | **exists** | Body `{ settingsJson: string }` |
| GET | `/reports/policies` | `admin.ts` | **exists** | Uses `totalElements` for citizen count |
| GET | `/revenue/invoices` | `admin.ts` | **exists** | Partner billing |
| GET | `/partners/me` | `admin.ts` | **exists** | Current partner context |
| GET | `/partners/{id}/contracts` | `admin.ts` | **exists** | |
| GET | `/revenue/ledger` | `admin.ts` | **exists** | |
| GET | `/insurer/applications` | `admin.ts` | **missing** | Use `/applications` natively |
| POST | `/insurer/applications/{id}/decision` | `admin.ts` | **missing** | Use `PATCH /applications/{id}/review` |

---

## FrontendCompatController gaps

File: `.tmp-api/src/main/java/com/ingoboka_api/v1/frontend/controllers/FrontendCompatController.java`

**Already aliased (21 routes):**  
`customers/me`, `customers/consent`, `customer/kyc/submit`, `customer/dependants` CRUD, `payments/initiate`, `payments/sandbox/callback`, `payments/{id}/status`, `policies`, `policies/{id}/card`, `verify/{token}`, `admin/claims` list/detail/decision, `admin/reports/overview`, `admin/reports/claims-breakdown`, `admin/platform/overview`, `agent/applications` list/create.

**Add to FrontendCompatController (P0/P1):**

```text
GET  /api/v1/admin/audit-logs          → auditComplianceService.listAuditLogs
GET  /api/v1/admin/platform/settings   → platform settings DTO
GET  /api/v1/insurer/applications      → enrollmentService.listTenantApplications (map status query)
POST /api/v1/insurer/applications/{id}/decision → map to reviewApplication (APPROVED/REJECTED)
POST /api/v1/claims/{id}/documents     → multipart upload + MinIO (or proxy to new DocumentService method)
```

**Optional aliases:**

```text
GET  /api/v1/policies/{id}             → policyService (frontend uses native route today — works)
PATCH /api/v1/admin/claims/{id}/request-info → wrap updateStatus(INFO_REQUESTED)
```

**Response envelope:** All compat routes must return `ApiResponse<T>` with `{ success, message, data }` — frontend `apiClient` interceptor unwraps `data` only.

---

## Deployment notes for Kamatera / Vercel

### Vercel (frontend)

```env
NEXT_PUBLIC_API_BASE_URL=https://<kamatera-host>:8085/api/v1
```

Redeploy after changing. No server-side proxy — all calls are browser CORS requests.

### Kamatera (backend) — `deploy/docker-compose.yml`

| Service | Port | Required for MVP |
|---------|------|------------------|
| API | 8085→8080 | Yes |
| PostgreSQL | internal | Yes |
| Redis | internal | Yes (OTP when `OTP_STORAGE=redis`) |
| MinIO | 9000/9001 | Yes for claim docs + product images |

**Critical env vars:**

```env
JWT_SECRET=<256-bit-secret>
CORS_ALLOWED_ORIGINS=https://ingoboka-platform.vercel.app
SEED_DEMO_DATA=true
SANDBOX_AUTO_APPROVE=true
MINIO_ENABLED=true
MINIO_ENDPOINT=http://minio:9000
OTP_DELIVERY_CHANNEL=email
MAIL_HOST=... MAIL_USERNAME=... MAIL_PASSWORD=...
```

**Health check:** `GET /actuator/health/liveness` (compose healthcheck uses this).

**Smoke test from laptop:**

```bash
curl -s https://<host>:8085/actuator/health/liveness
curl -s https://<host>:8085/api/v1/products?page=0&size=5
```

### Known remote issues

- Documented demo host `185.181.10.165:8085` — partial failures on OpenAPI + health in prior audits; treat as deploy debt.
- Presigned MinIO URLs use internal endpoint if misconfigured — browser on Vercel cannot fetch them; use external MinIO URL or API proxy download endpoint (`GET /documents/{id}/download-url`).

---

## Suggested Rodin PR order

1. **P0 compat aliases** — audit-logs, insurer applications (+ decision), idempotent consent in `EnrollmentServiceImpl.grantConsentForUser`
2. **Multipart claim documents** — MinIO upload + attach metadata
3. **Platform settings GET** — unblock admin settings page
4. **CORS + Kamatera health** — coordinate with DevOps
5. **MoMo webhook + docs** — when moving off sandbox simulate
6. **Seed data** — demo products with `heroImageKey`, citizen `0780000001`, insurer accounts per README

---

## Reference documents

- `Ingoboka_Enterprise_System_Concept_v1.pdf` — MVP acceptance journey (§15.1), must-have features (§15.2)
- `Ingoboka_Technical_System_Blueprint.pdf` — modular monolith boundaries, storage/CORS/testing standards
- Backend README in `Ingoboka-api-v1` — Docker ports, demo credentials, API overview

---

*Generated by frontend platform audit. Backend snapshot: shallow clone of `mahingaRodin/Ingoboka-api-v1` in `.tmp-api/` (not committed; listed in `.gitignore`).*
