# Rodin — Remaining Backend Work (→ Frontend 100%)

**Date:** 20 June 2026  
**Audience:** Rodin Mahinga  
**Frontend status:** ~98% — all design screens built; API clients wired to your latest endpoints on remote  
**Canonical latest code:** `Ingoboka-api-v1/` (verified on `http://185.181.10.165:8085/api/v1`)  
**Deploy path in platform repo:** `backend/` — **currently stale** vs `Ingoboka-api-v1/` (missing product detail, claims-breakdown, policy enrichment, etc.)

**Full history / context:** `docs/RODIN_BACKEND_FOR_100_PERCENT_FRONTEND.md`

---

## Already shipped (thank you — no rework)

| Area | Notes |
|------|--------|
| Demo seed | `DemoDataSeeder` (`@Profile("docker")`) — citizen, insurer, agent, platform admin |
| `DAILY` premium frequency | Migration + enum |
| Email OTP | Remote returns `EMAIL` mode; SMTP on deploy |
| Platform admin | `phoneVerified=true` in seeder |
| `GET /products/{id}/detail` | Plans, benefits, exclusions, FAQ, claimSteps |
| Needs assessment | `recommendedProducts[]` on response |
| Policies | Enriched list + `GET /policies/me/activity` |
| Claims | `statusHistory[]`, enriched list fields |
| Reports | `GET /admin/reports/claims-breakdown` |
| Agent routes | 403 (not 500) on wrong role |
| Platform overview | Real `openClaims`, `totalApplications` |

**Demo credentials (remote):**

| Role | Login | Password |
|------|-------|----------|
| Citizen | `+250780000001` | `Ingoboka@2026` |
| Partner admin | `eric@demo-insurer.rw` | `Ingoboka@2026` |

---

## Still needed from you (P2 — blocks polish, not core flows)

### 1. Product hero images — `heroImageUrl`

**Why:** Catalog cards and product detail hero currently show a gradient fallback. Frontend already reads `heroImageUrl` when present.

**Where to add:**

- `ProductResponse` (and therefore `GET /products` list + `GET /products/{id}/detail` → `product.heroImageUrl`)
- Optional: accept image upload on product create/publish (MinIO object key → presigned or CDN URL)

**Example:**

```json
{
  "id": "…",
  "name": "Personal Accident Micro",
  "category": "PERSONAL_ACCIDENT",
  "heroImageUrl": "https://minio.example/ingoboka/products/pa-micro-hero.jpg"
}
```

**Acceptance:** Demo product in seeder has a real `heroImageUrl`; citizen catalog + `/products/{id}` show the image.

---

### 2. Product PDF documents

**Why:** Product detail **Documents** tab is wired but empty — no API field or endpoint yet.

**Preferred contract (either is fine — tell Arnold which you pick):**

**Option A — nested in detail (simplest for FE):**

```json
GET /products/{id}/detail
{
  "product": { … },
  "documents": [
    {
      "id": "uuid",
      "title": "Policy wording",
      "fileName": "policy-wording.pdf",
      "downloadUrl": "https://minio…/presigned…"
    }
  ]
}
```

**Option B — separate endpoint:**

```
GET /products/{id}/documents → [{ id, title, fileName, downloadUrl }]
```

**Implementation hints:**

- Reuse existing `DocumentStorageService` + MinIO presigned URLs
- Table e.g. `product_documents` (product_id, title, file_name, object_key, sort_order)
- Seed at least one PDF for the demo product

**Acceptance:** Documents tab shows downloadable links; insurer can attach PDFs at publish (or via admin upload).

---

### 3. Quote labels — `productName` + `planName`

**Why:** Enroll flow quote step shows hardcoded `"Insurance Product"` / `"Selected Plan"` because `POST /applications/quote` does not return names.

**Add to `QuoteResponse`:**

```json
{
  "id": "…",
  "productPlanId": "…",
  "productName": "Personal Accident Micro",
  "planName": "Monthly Plan",
  "premiumAmount": 500,
  "premiumFrequency": "MONTHLY"
}
```

**Acceptance:** Citizen enroll → quote screen shows real product and plan names without frontend guessing.

---

### 4. Policy report — `citizensEnrolled`

**Why:** Insurer **Reports** page stat card for enrolled citizens shows `—` or a workaround (`totalElements` from paginated policies).

**Add either:**

- `GET /reports/policies/summary` → `{ activePolicies, citizensEnrolled }`, **or**
- Top-level fields on existing `GET /admin/reports/overview`

**Acceptance:** Reports page shows distinct citizen count (unique policyholders), not just policy row count.

---

## Repo / deploy (platform team + Rodin)

### 5. Sync `Ingoboka-api-v1/` → `backend/`

**Problem:** Platform repo `backend/` does **not** contain your latest Java (no `/products/{id}/detail`, claims-breakdown, DemoDataSeeder, etc.). CI/docker-compose may still point at `backend/`.

**Ask:** After each backend drop, either:

1. Copy/sync `Ingoboka-api-v1/` into `backend/` in the monorepo, **or**
2. Confirm `Ingoboka-api-v1/` is the only deploy source and we update compose/CI paths

**Acceptance:** One canonical folder; local `docker compose up` and Kamatera deploy use the same code as remote `:8085`.

---

## Optional (nice-to-have, not blocking MVP demo)

| Item | Notes |
|------|--------|
| Flyway `V99__dev_demo_seed.sql` | You use runtime `DemoDataSeeder` today — OK for docker; SQL seed preferred if we need reproducible DB-only demos |
| Product `media[]` gallery | Only if design adds carousel beyond single hero |
| OpenAPI examples | For new fields above |
| Integration test | register → purchase → claim on docker profile |

---

## Frontend will do after you ship

| When you deliver | Arnold removes |
|------------------|----------------|
| Stable remote/local API | `isNetworkError` → `mockData` in `auth.ts`, `policies.ts`, `claims.ts` |
| `heroImageUrl` + documents | Gradient-only hero; empty Documents tab message |
| Quote names | Client-side `productName ?? 'Insurance Product'` fallback in `products.ts` |
| `citizensEnrolled` | `admin.ts` workaround using `policies.totalElements` |

---

## Quick verification checklist (for Rodin)

- [ ] `GET /products` — at least one item with `heroImageUrl`
- [ ] `GET /products/{demoId}/detail` — `documents[]` with working presigned `downloadUrl`
- [ ] `POST /applications/quote` — returns `productName`, `planName`
- [ ] Insurer reports — `citizensEnrolled` populated
- [ ] `backend/` in monorepo matches `Ingoboka-api-v1/` (or deploy docs updated)

---

## Questions

1. **Documents:** Option A (nested in detail) or Option B (separate route)?
2. **Hero images:** Presigned MinIO URLs per request, or stable CDN/base URL?
3. **Sync:** Will you push to `backend/` in the monorepo, or should platform team mirror from `Ingoboka-api-v1/`?

Reply on Slack/issue with ETA — frontend wiring for items 1–4 is ~1 PR once shapes are live on `:8085`.
