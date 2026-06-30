# Ingoboka Backend — Final Handoff for Rodin

**Prepared for:** Rodin (`mahingaRodin/Ingoboka-api-v1`)  
**Prepared by:** Arnold — frontend integration owner  
**Date:** 20 June 2026  
**Frontend repo:** [Arn-The-Wolf/ingoboka-platform](https://github.com/Arn-The-Wolf/ingoboka-platform)  
**Live demo:** [https://ingoboka-platform.vercel.app](https://ingoboka-platform.vercel.app)  
**API base (production):** Browser calls `/api/v1` on Vercel; Next.js rewrites proxy to Kamatera (`API_PROXY_TARGET`)

---

## Executive summary

The **frontend MVP is integration-complete (~99%)**. All four portals (citizen, insurer, admin, agent), marketing site, enrollment, payments (sandbox + poll fallback), claims, dependants, KYC, profile, and **notifications inbox** are built and wired to your API clients.

**Your backend is the remaining blocker for a flawless production demo (~72–78% ready).** The frontend already implements workarounds (path fallbacks, presigned upload flow, public catalog proxy, local SVG image fallbacks), but several routes still return **HTTP 500** or have **contract mismatches** on the live host `185.181.10.165:8085`.

Arnold will integrate immediately after you merge fixes to `Ingoboka-api-v1` `main`. No frontend re-architecture is required — only backend compat routes, idempotency fixes, storage, and deploy hardening.

| Layer | Completion | Owner |
|-------|------------|-------|
| Frontend UI + i18n (EN/RW) | **~99%** | Arnold ✅ |
| Frontend ↔ API integration | **~85%** | Arnold (workarounds in place) |
| Backend API + deploy | **~72%** | **Rodin** ← this document |
| End-to-end MVP demo | **~96%** | Blocked on P0 backend items |

---

## Architecture (how Vercel talks to Kamatera)

```
Browser (HTTPS)
  → ingoboka-platform.vercel.app/api/v1/*
  → Next.js rewrite (next.config.js)
  → http://185.181.10.165:8085/api/v1/*
```

**Vercel env (production):**

```env
NEXT_PUBLIC_API_BASE_URL=/api/v1
API_PROXY_TARGET=http://185.181.10.165:8085
```

This avoids mixed-content (HTTPS page → HTTP API). CORS is still required for any direct browser calls and for preflight from preview deployments.

**Optional (marketing catalog without citizen login):**

```env
CATALOG_SERVICE_EMAIL=citizen.demo@ingoboka.rw
CATALOG_SERVICE_PASSWORD=Ingoboka@2026
```

---

## Demo credentials (live `:8085`)

| Role | Login | Password |
|------|-------|----------|
| Citizen | `0780000001` or `citizen.demo@ingoboka.rw` | `Ingoboka@2026` |
| Platform admin | `agressive.one04@gmail.com` | `admin@123` |
| Insurer (claims) | `claims@demo-insurer.rw` | `Ingoboka@2026` |

---

## MVP journey — current status

| Step | Journey | Frontend | Backend | Live integration |
|------|---------|----------|---------|------------------|
| S01 | Register, OTP, login | ✅ | ✅ | ✅ via proxy |
| S02 | Consent | ✅ | ⚠️ | **500 on re-grant** (P0) |
| S03 | Needs assessment | ✅ | ✅ | ✅ |
| S04 | Product catalog + detail | ✅ | ⚠️ | `heroImageUrl` null; docs empty off MinIO |
| S05 | Quote → application → submit | ✅ | ✅ | ✅ with `SANDBOX_AUTO_APPROVE` |
| S06 | Payment → policy | ✅ poll + sandbox | ✅ | ✅ demo path works |
| S07 | Policy wallet, QR verify | ✅ | ✅ | ✅ when policy issued |
| S08 | Claim + documents | ✅ presigned workaround | ❌ multipart | **500 on raw multipart** (P0) |
| S09 | Insurer decision + appeal | ✅ | ⚠️ | `REQUEST_INFO` not on decision DTO |
| — | Notifications inbox | ✅ `GET /notifications/me` | ✅ route exists | **Needs seed data** to demo |

---

## P0 — Blocks production demo (fix first)

### 1. Claim document upload — multipart contract

| | |
|---|---|
| **Frontend** | `POST /claims/{id}/documents` — `multipart/form-data`, field `files[]`. Also supports presigned flow via `documents.ts` when backend exposes upload URL. |
| **Backend** | `ClaimController` expects JSON `AttachClaimDocumentRequest` (`objectKey`, `mimeType`, …) — no file bytes. |
| **Live** | Multipart → **500**; JSON metadata attach → **201** |
| **Fix** | Accept `MultipartFile[] files`, upload to MinIO, call `claimService.attachDocument` per file. Return list of attached docs. |

**Files:** `ClaimController`, `DocumentStorageService`, optionally `FrontendCompatController`.

---

### 2. Insurer enrollment review — path + payload alias

| | |
|---|---|
| **Frontend** | `GET /insurer/applications?status=PENDING`, `POST /insurer/applications/{id}/decision` body `{ decision: "APPROVED"\|"REJECTED", reason? }` |
| **Backend** | `GET /applications`, `PATCH /applications/{id}/review` body `{ status, decisionReason? }` |
| **Live** | `/insurer/applications` → **500**; `/applications` → **200** |
| **Fix** | Add compat aliases in `FrontendCompatController`. Map `PENDING` query → `SUBMITTED` + `UNDER_REVIEW`. Map `decision` → `status`. |

---

### 3. Consent re-grant → HTTP 500

| | |
|---|---|
| **Frontend** | `POST /customers/consent` after `GET /customers/me/consents` filter |
| **Backend** | `EnrollmentServiceImpl.grantConsentForUser` inserts without revoke → violates unique index `idx_consents_active_user_type` |
| **Live** | Re-consent during enrollment → **500** |
| **Fix** | Revoke-then-grant (same as `CustomerProfileServiceImpl`); return 200 if already active. |

---

### 4. Admin audit log — path alias

| | |
|---|---|
| **Frontend** | `GET /admin/audit-logs?page&size` |
| **Backend** | `GET /audit/logs` |
| **Live** | `/admin/audit-logs` → **500**; `/audit/logs` → **200** |
| **Fix** | `FrontendCompatController` alias; map field names (`action`, `actor`, `resource`, `occurredAt`, `details`). |

---

### 5. Admin platform settings — endpoint missing

| | |
|---|---|
| **Frontend** | `GET /admin/platform/settings` |
| **Backend** | Not exposed (config only in `application.properties`) |
| **Live** | **500** / 404 |
| **Fix** | `GET /api/v1/admin/platform/settings` → `{ platformName, defaultLocale, maintenanceMode, apiBaseUrl, supportEmail }` |

---

### 6. CORS + Kamatera health

| | |
|---|---|
| **Fix** | `CORS_ALLOWED_ORIGINS=https://ingoboka-platform.vercel.app` (+ previews if needed). Ensure Postgres, Redis, MinIO up; `GET /actuator/health/liveness` → 200. |

---

## P1 — MVP completeness (after P0)

### 7. Product `heroImageUrl` + document download URLs

MinIO presigned URLs must use a **browser-reachable host**. If `MINIO_ENDPOINT` is internal Docker hostname, Vercel users get null/ broken images.

**Fix:** External MinIO URL or API download proxy (`GET /documents/{id}/download-url`). Seed products with `heroImageKey`.

### 8. Public product catalog (optional)

Frontend marketing proxy logs in server-side for catalog. Cleaner backend fix: allow `GET /products` for `PUBLISHED` products without JWT (read-only).

### 9. Claim insurer `REQUEST_INFO`

Frontend sends `decision: "REQUEST_INFO"`. Backend `RecordClaimDecisionRequest` only allows `APPROVED`, `REJECTED`, `PARTIAL`.

**Fix:** Extend compat decision endpoint or document `PATCH /claims/{id}/status` with `INFO_REQUESTED`.

### 10. Application status filter `PENDING`

Frontend filters `PENDING`; backend enum uses `SUBMITTED`, `UNDER_REVIEW`. Add query alias.

### 11. `incidentDate` on claim create

Frontend sends `incidentDate`; backend `CreateClaimRequest` ignores it. Add field if SLA/timeline needs it.

### 12. Real MoMo (post-demo)

Frontend enroll flow polls `GET /payments/{id}/status` then falls back to sandbox callback. Document webhook URL and env for production MoMo.

### 13. Notifications seed data

Frontend inbox is live at `/notifications`. Seed `notifications` table for demo citizen so inbox is non-empty in demos.

---

## FrontendCompatController — required additions

**File:** `src/main/java/com/ingoboka_api/v1/frontend/controllers/FrontendCompatController.java`

**Already aliased (~21 routes):** auth-adjacent customer routes, payments, policies, admin claims, reports, platform overview, agent applications, etc.

**Add (P0):**

```text
GET  /api/v1/admin/audit-logs
GET  /api/v1/admin/platform/settings
GET  /api/v1/insurer/applications
POST /api/v1/insurer/applications/{id}/decision
POST /api/v1/claims/{id}/documents          (multipart)
```

**Response envelope:** All routes return `ApiResponse<T>` with `{ success, message, data }`. Frontend axios interceptor unwraps `data`.

---

## Endpoint contract table (frontend expectations)

Base: `/api/v1`. Status on live host `185.181.10.165:8085` as of June 2026.

| Method | Path | Frontend module | Status | Notes |
|--------|------|-----------------|--------|-------|
| POST | `/auth/login` | `auth.ts` | ✅ | |
| GET | `/customers/me` | `auth.ts` | ✅ | Profile mapped on refresh |
| POST | `/customers/consent` | `auth.ts` | ❌ | 500 duplicate |
| GET | `/products` | `products.ts` | ⚠️ | 401 without JWT; marketing uses server proxy |
| GET | `/products/{id}/detail` | `products.ts` | ⚠️ | `heroImageUrl` often null |
| POST | `/applications/needs-assessment` | `products.ts` | ✅ | |
| POST | `/payments/initiate` | `payments.ts` | ✅ | |
| GET | `/payments/{id}/status` | `payments.ts` | ✅ | Used in enroll poll |
| POST | `/claims` | `claims.ts` | ✅ | |
| POST | `/claims/{id}/documents` | `claims.ts` | ❌ | Multipart 500 |
| GET | `/admin/audit-logs` | `admin.ts` | ❌ | Use fallback `/audit/logs` |
| GET | `/admin/platform/settings` | `admin.ts` | ❌ | |
| GET | `/insurer/applications` | `admin.ts` | ❌ | Fallback `/applications` |
| GET | `/notifications/me` | `notifications.ts` | ✅ | **New frontend page** |
| PATCH | `/notifications/{id}/read` | `notifications.ts` | ✅ | |
| GET | `/agent/applications` | `admin.ts` | ✅ | |
| POST | `/agent/applications` | `admin.ts` | ✅ | `{ citizenPhone, productPlanId }` |

Full table in internal doc `docs/RODIN_BACKEND_REMAINING_WORK.md` (local, not in git).

---

## Suggested PR order for Rodin

1. **P0 compat aliases** — audit-logs, insurer applications + decision, platform settings  
2. **Idempotent consent** — `EnrollmentServiceImpl.grantConsentForUser`  
3. **Multipart claim documents** — MinIO upload  
4. **MinIO external URL + product seed images**  
5. **CORS + health on Kamatera**  
6. **Notification seeds + optional public catalog**  
7. **MoMo webhook documentation** (when leaving sandbox)

---

## Smoke tests (run after deploy)

```bash
HOST=http://185.181.10.165:8085

# Health
curl -s "$HOST/actuator/health/liveness"

# Login citizen
TOKEN=$(curl -s -X POST "$HOST/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"identifier":"0780000001","password":"Ingoboka@2026"}' \
  | jq -r '.data.accessToken')

# Products (authenticated)
curl -s "$HOST/api/v1/products?page=0&size=5" -H "Authorization: Bearer $TOKEN"

# Notifications
curl -s "$HOST/api/v1/notifications/me" -H "Authorization: Bearer $TOKEN"

# Audit (should work after compat)
curl -s "$HOST/api/v1/admin/audit-logs?page=0&size=5" -H "Authorization: Bearer $TOKEN"

# Insurer applications (should work after compat)
curl -s "$HOST/api/v1/insurer/applications?status=PENDING" -H "Authorization: Bearer $TOKEN"
```

---

## Kamatera deploy checklist

| Service | Port | MVP |
|---------|------|-----|
| API | 8085→8080 | Yes |
| PostgreSQL | internal | Yes |
| Redis | internal | Yes |
| MinIO | 9000/9001 | Yes (claims + images) |

```env
JWT_SECRET=<256-bit-secret>
CORS_ALLOWED_ORIGINS=https://ingoboka-platform.vercel.app
SEED_DEMO_DATA=true
SANDBOX_AUTO_APPROVE=true
MINIO_ENABLED=true
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_ENDPOINT=https://<external-minio-or-cdn>
OTP_DELIVERY_CHANNEL=email
MAIL_HOST=... MAIL_USERNAME=... MAIL_PASSWORD=...
```

---

## What Arnold completed (this handoff)

- i18n (EN/RW): enroll, product detail tabs/quiz, needs assessment  
- Notifications inbox: `GET /notifications/me`, mark read, sidebar + mobile header link  
- Agent portal: product/plan picker from live `productApi` (no hardcoded UUID)  
- Insurer/admin list pages: spinners → shimmer skeletons  
- Rodin handoff doc (this file) for backend completion  

**After your P0 merge:** Arnold will remove redundant fallbacks, verify E2E on Vercel, and tag a release.

---

## References

- Backend repo: [mahingaRodin/Ingoboka-api-v1](https://github.com/mahingaRodin/Ingoboka-api-v1)  
- `Ingoboka_Enterprise_System_Concept_v1.pdf` — MVP journey §15.1  
- `Ingoboka_Technical_System_Blueprint.pdf` — monolith boundaries, storage, CORS  
- Frontend API clients: `src/lib/api/*.ts`  
- Frontend compat fallbacks: `src/lib/api/integration-helpers.ts`, `admin.ts`

---

*Questions: coordinate via Arnold. Frontend changes are frozen until P0 backend is on `main`.*
