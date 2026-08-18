# Ingoboka API — k6 load tests

Production-oriented load tests for critical API paths: health, auth login, product/policy/claim lists, and protected endpoint smoke under concurrent load.

## Layout

```
load-tests/
├── README.md
├── .env.example          # Copy to .env (gitignored)
├── package.json
├── smoke.js              # CI gate + quick local check (2 VUs, 1 min)
├── load.js               # sustained load (ramp to 20 VUs)
├── stress.js             # stress profile (ramp to 60 VUs)
├── lib/
│   ├── config.js         # BASE_URL, thresholds, credentials from env
│   └── auth.js           # login helper
├── scenarios/
│   └── critical-paths.js # shared scenario
└── scripts/
    ├── run-smoke.ps1
    ├── run-load.ps1
    └── run-stress.ps1
```

## Prerequisites

- [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) installed locally
- API reachable at `BASE_URL`
- A demo/staging login with access to tenant lists (partner demo user from your staging DB works well)

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:8085/api/v1` | API base including `/api/v1` |
| `EMAIL` | *(required)* | Login identifier (email or phone) |
| `PASSWORD` | *(required)* | Login password — **never commit** |

**Setup:** copy `.env.example` to `.env` and fill in credentials:

```powershell
Copy-Item load-tests\.env.example load-tests\.env
# Edit load-tests\.env — EMAIL/PASSWORD from your local deploy/.env or staging secrets
```

**Local Docker** (`docker compose up`): use the same `PLATFORM_ADMIN_EMAIL` / `PLATFORM_ADMIN_PASSWORD` from `deploy/.env` (see `deploy/.env.example`).

**Remote / staging:** set `BASE_URL` to your staging API URL and use credentials configured for that environment. Do not paste production passwords into this repo.

Login body must be `{ "identifier": "<email-or-phone>", "password": "..." }` (not `{ email, password }`). The k6 helper already sends `identifier`.

## Windows PowerShell — run locally

From repo root `ingoboka-api`:

```powershell
# Smoke (fast gate) — reads load-tests/.env if present
.\load-tests\scripts\run-smoke.ps1

# Override target or credentials via params / env
$env:BASE_URL = "https://staging.example.com/api/v1"
$env:EMAIL = "your-loadtest@example.com"
$env:PASSWORD = "<from-your-secrets-manager>"
.\load-tests\scripts\run-smoke.ps1

# Load / stress (local or staging — not in default CI)
.\load-tests\scripts\run-load.ps1
.\load-tests\scripts\run-stress.ps1
```

Direct `k6` (from `load-tests/`):

```powershell
cd load-tests
$env:BASE_URL = "http://localhost:8085/api/v1"
$env:EMAIL = "your-loadtest@example.com"
$env:PASSWORD = "<from-deploy/.env>"
k6 run smoke.js
k6 run load.js
k6 run stress.js
```

## Thresholds (tuned for demo VM)

| Profile | `http_req_failed` | p95 latency | Notes |
|---------|-------------------|-------------|-------|
| **smoke** | &lt; 1% | &lt; 2.5s overall, health &lt; 1.5s | CI gate on `test-build-push` |
| **load** | &lt; 2% | &lt; 3.5s | Local / staging validation |
| **stress** | &lt; 5% | &lt; 6s | Capacity exploration only |

Checks must pass at &gt; 95% (`checks` threshold).

## Scenarios covered

1. `GET /actuator/health` (derived from `BASE_URL` origin)
2. `GET /auth/otp-delivery-config`
3. `POST /auth/login`
4. `GET /products`, `GET /products/tenant`
5. `GET /policies/tenant`
6. `GET /claims`
7. Protected smoke: `GET /staff/me`, `GET /reports/overview`

## Branch / CI flow

```
feature work → test-build-push → staging → PR → main
```

| Branch | Workflow | What runs |
|--------|----------|-----------|
| **`test-build-push`** | `.github/workflows/test-build-push.yml` | Maven `verify`, Docker build sanity, **k6 smoke** (if `LOAD_TEST_BASE_URL` secret set) |
| **`staging`** | `.github/workflows/staging-integration.yml` | Full Maven integration tests; optional k6 load if secrets set |
| **`main`** | `.github/workflows/ci-cd.yml` | Existing build + deploy to Azure VM |

### GitHub secrets (repo → Settings → Secrets)

| Secret | Description | Used by |
|--------|-------------|---------|
| `LOAD_TEST_BASE_URL` | Staging API URL including `/api/v1` | k6 in CI |
| `LOAD_TEST_EMAIL` | Login identifier for k6 (partner demo user from staging DB) | k6 login |
| `LOAD_TEST_PASSWORD` | Login password for k6 | k6 login |

Configure these in **Settings → Secrets and variables → Actions**. Never commit secret values to the repository.

If `LOAD_TEST_BASE_URL` is **not** set, CI skips k6 with a notice (Maven + Docker still run).

## Merge sequence (manual)

From `ingoboka-api` on Windows PowerShell:

```powershell
# 1) Finish work on test-build-push, push, wait for CI green
git checkout test-build-push
git push origin test-build-push

# 2) Merge into staging for integration tests
git checkout staging
git pull origin staging
git merge test-build-push --no-ff -m "Merge test-build-push: k6 load tests and build pipeline"
git push origin staging

# 3) Open PR staging → main (do not merge until reviewed)
gh pr create --base main --head staging --title "Release: staging → main" --body "$(@'
## Summary
- Integration-tested on staging branch
- k6 smoke passed on test-build-push

## Test plan
- [ ] Staging CI green
- [ ] Smoke k6 against production-like URL
- [ ] Manual smoke on platform frontend
'@)"
```

Production deploy to Azure still happens on push to `main` via `ci-cd.yml`.

## npm scripts (optional)

```powershell
cd load-tests
npm run smoke
npm run load
npm run stress
```
