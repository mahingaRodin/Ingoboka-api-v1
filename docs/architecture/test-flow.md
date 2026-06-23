# Ingoboka API — Test Flow

How tests are structured, executed locally, and run in CI/CD.

## Pipeline overview

```mermaid
flowchart TB
    subgraph Dev["Local developer"]
        MVN["./mvnw clean verify"]
        FLAG{"-Dintegration=true?"}
        DOCKER{"Docker available?"}
        UNIT["Unit tests<br/>always run"]
        INT["Integration tests<br/>Testcontainers"]
        SKIP["Integration tests skipped<br/>@EnabledIf"]
    end

    subgraph CI["GitHub Actions (main / PR)"]
        CHECKOUT[checkout]
        JDK["JDK 21 Temurin"]
        VERIFY["mvnw clean verify -B -Dintegration=true"]
        IMG["docker build<br/>(main push only)"]
        DEPLOY["SCP + SSH deploy<br/>(main push only)"]
    end

    MVN --> FLAG
    FLAG -->|no| UNIT
    FLAG -->|yes| DOCKER
    DOCKER -->|yes| INT
    DOCKER -->|no| SKIP
    INT --> UNIT

    CHECKOUT --> JDK --> VERIFY
    VERIFY -->|pass + push main| IMG --> DEPLOY
    VERIFY -->|fail| FAIL[❌ Pipeline stops]
```

## Integration test architecture

```mermaid
flowchart LR
    subgraph Surefire["Maven Surefire"]
        TC["JUnit 5 test classes"]
        EN["@EnabledIf<br/>IntegrationTestSupport#isEnabled"]
    end

    subgraph Spring["Spring Boot test context"]
        MVC[MockMvc]
        APP["@SpringBootTest<br/>profile: test"]
        SEED[PlatformAdminSeeder]
    end

    subgraph TC_Containers["Testcontainers (when enabled)"]
        PG[("postgres:16-alpine")]
        RD[("redis:7-alpine")]
    end

    TC --> EN
    EN -->|integration=true + Docker| APP
    APP --> MVC
    APP --> PG & RD
    APP --> SEED
```

## Test suite map

```mermaid
flowchart TB
    ROOT[V1ApplicationTests<br/>context smoke test]

    subgraph Integration["Integration tests (-Dintegration=true)"]
        APP_IT[V1ApplicationIntegrationTest<br/>context loads with Testcontainers]
        AUTH[AuthControllerIntegrationTest<br/>register · OTP · login · refresh · logout]
        CIT[CitizenEndpointsIntegrationTest<br/>profile · dependants · agent 403]
        PROT[ProtectedEndpointsIntegrationTest<br/>RBAC · admin · partner · reports]
        PART[PlatformPartnerFlowIntegrationTest<br/>onboard · staff · contracts]
        WRITE[WriteOperationsIntegrationTest<br/>validation on write endpoints]
    end

    ROOT --> Integration
```

## Test class responsibilities

| Test class | Order / scope | What it verifies |
|------------|---------------|------------------|
| `V1ApplicationTests` | Unit | Spring application context starts |
| `V1ApplicationIntegrationTest` | Integration | Full stack + Flyway migrations against Testcontainers Postgres |
| `AuthControllerIntegrationTest` | Integration | Public OTP config; citizen register→verify→login; admin login; refresh/logout |
| `CitizenEndpointsIntegrationTest` | Integration | Citizen profile routes; citizen blocked from `/agent/applications` (403) |
| `ProtectedEndpointsIntegrationTest` | Integration | 401 without token; platform admin can reach dashboards, partners, reports, Swagger |
| `PlatformPartnerFlowIntegrationTest` | Ordered | Partner onboard → login → staff → contracts |
| `WriteOperationsIntegrationTest` | Integration | Write endpoints return 4xx (not 5xx) on empty payloads |

## Integration test support

`IntegrationTestSupport` provides:

- **Testcontainers** — PostgreSQL 16 + Redis 7 (when Docker is available)
- **Dynamic properties** — JDBC and Redis URLs wired at runtime
- **MockMvc helpers** — `get`, `post`, `loginPlatformAdmin`, etc.
- **Security isolation** — `SecurityContextHolder.clearContext()` before/after each test
- **Opt-out** — `-Dintegration=true` required; without Docker, tests are skipped (not failed)

### Local commands

```bash
# Unit tests only (no Docker required)
./mvnw clean test

# Full verify — integration tests run when Docker is available
./mvnw clean verify -B -Dintegration=true

# Force integration with local Postgres/Redis instead of Testcontainers
./mvnw clean verify -B -Dintegration=true -Dintegration.local=true
```

### Test profile (`application-test.properties`)

| Setting | Value | Purpose |
|---------|-------|---------|
| OTP storage | `memory` | No Redis dependency for OTP in tests |
| OTP delivery | `log` | OTP printed in logs; no SMTP |
| MinIO | disabled | Document storage not required |
| Platform admin seed | `platform-admin@test.ingoboka` | Predictable CI credentials |
| Demo seed | disabled | Isolated test data |

## CI/CD test gate

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub
    participant Runner as ubuntu-latest runner
    participant TC as Testcontainers
    participant Server as Kamatera VPS

    Dev->>GH: push / PR to main
    GH->>Runner: build-and-test job
    Runner->>TC: Start Postgres + Redis
    Runner->>Runner: ./mvnw clean verify -Dintegration=true
    alt tests fail
        Runner-->>GH: ❌ job failed — deploy blocked
    else tests pass + push main
        Runner->>Runner: docker build
        Runner->>Server: SCP deploy bundle
        Runner->>Server: docker compose up -d --build
    end
```

## Common failure modes (fixed / watch)

| Symptom | Cause | Fix |
|---------|-------|-----|
| `UnsatisfiedDependency: ObjectMapper` | `@Autowired ObjectMapper` in test base | Use `new ObjectMapper()` in `IntegrationTestSupport` |
| Refresh token 400 after login | `login()` was `@Transactional(readOnly=true)` | Persist refresh tokens in read-write transaction |
| 403 instead of 401 on unauth | Leaked `SecurityContext` between MockMvc calls | Clear context in `@BeforeEach` / `@AfterEach` |
| Integration tests skipped locally | No Docker Desktop | Start Docker or rely on CI |

## Pre-push checklist

```bash
chmod +x mvnw   # required on Linux / CI
./mvnw clean verify -B -Dintegration=true
```

All 31 tests should pass (0 failures) on GitHub Actions with Docker available.
