# Ingoboka API — System Architecture

High-level view of the platform, infrastructure, and deployment topology.

## Context diagram

```mermaid
flowchart TB
    subgraph Clients["Client applications"]
        FE["Next.js frontend<br/>(Citizen · Insurer · Agent · Admin)"]
        MOB["Future mobile / integrations"]
    end

    subgraph Edge["Edge & delivery"]
        CORS["CORS + JWT auth"]
        SWAG["Swagger UI<br/>/swagger-ui/index.html"]
    end

    subgraph API["Ingoboka API v1<br/>Spring Boot 4 · Java 21"]
        direction TB
        SEC["Security layer<br/>JWT · RBAC · Onboarding filters"]
        CTRL["REST controllers<br/>25+ resource groups"]
        SVC["Domain services<br/>Identity · Catalog · Enrollment · Policy · Claims · Billing"]
        SCH["Schedulers<br/>Policy lifecycle"]
    end

    subgraph Data["Data & cache"]
        PG[("PostgreSQL 16<br/>Flyway migrations")]
        RD[("Redis 7<br/>OTP · sessions")]
        MN[("MinIO<br/>Documents · product media")]
    end

    subgraph External["External services"]
        SMTP["SMTP / Gmail<br/>Email OTP · notifications"]
        SMS["MTN Bulk SMS<br/>(optional)"]
        MOMO["MoMo sandbox<br/>Premium payments"]
    end

    subgraph Ops["Operations"]
        GH["GitHub Actions<br/>build · test · deploy"]
        VM["Kamatera VPS<br/>docker compose"]
        ACT["Actuator<br/>/actuator/health"]
    end

    FE -->|HTTPS JSON /api/v1| CORS
    MOB --> CORS
    CORS --> SEC
    SEC --> CTRL
    CTRL --> SVC
    SVC --> PG
    SVC --> RD
    SVC --> MN
    SVC --> SMTP
    SVC --> SMS
    SVC --> MOMO
    SCH --> PG
    SWAG --> CTRL
    GH -->|push main| VM
    VM --> API
    ACT --> API
```

## Application layers

```mermaid
flowchart LR
    subgraph Presentation
        AC[AuthController]
        PC[ProductController]
        EC[EnrollmentController]
        POL[PolicyController]
        CL[ClaimController]
        BC[BillingController]
        FC[FrontendCompatController]
        ADM[Admin / Partner / Agent controllers]
    end

    subgraph Application
        AS[AuthService]
        PCS[ProductCatalogService]
        ES[EnrollmentService]
        PS[PolicyService]
        CS[ClaimService]
        BS[BillingService]
        RS[ReportingService]
    end

    subgraph Domain
        ENT[Entities & enums]
        REPO[JPA repositories]
    end

    subgraph Infrastructure
        FLY[Flyway]
        JWT[JwtService]
        OTP[OtpService]
        DOC[DocumentStorageService]
        MAIL[NotificationService]
    end

    AC --> AS
    PC --> PCS
    EC --> ES
    POL --> PS
    CL --> CS
    BC --> BS
    FC --> ES & PS & CS & BS
    ADM --> RS

    AS & PCS & ES & PS & CS & BS --> REPO
    REPO --> ENT
    REPO --> FLY
    AS --> JWT & OTP & MAIL
    PCS --> DOC
```

## Docker runtime (local / production)

```mermaid
flowchart TB
    subgraph compose["docker compose (deploy/)"]
        API_C["ingoboka-api :8085→8080"]
        PG_C["postgres :5432"]
        RD_C["redis :6379"]
        MN_C["minio :9000 / :9001"]
    end

  Host["Developer / VPS host"] --> API_C
    API_C --> PG_C
    API_C --> RD_C
    API_C --> MN_C
    API_C -->|SMTP 587| Internet["Gmail / mail provider"]
```

## Security model

| Layer | Mechanism |
|-------|-----------|
| Transport | HTTPS in production; CORS configurable per origin |
| Authentication | JWT access token (30 min) + refresh token (7 days, hashed in DB) |
| Authorization | Role-based (`@PreAuthorize`) — `CITIZEN`, `AGENT`, `PARTNER_ADMIN`, `CLAIMS_OFFICER`, `PLATFORM_ADMIN`, … |
| OTP | Email (default), SMS (MTN), or log mode for dev |
| Staff onboarding | Temporary password + email verification gates (`OnboardingAccessFilter`) |
| Documents | MinIO presigned URLs; access classification on policy/claim docs |

## Key technology choices

| Component | Choice |
|-----------|--------|
| Runtime | Java 21, Spring Boot 4.1 |
| API | REST, JSON wrapper `{ success, message, data }` |
| Persistence | PostgreSQL + Hibernate validate + Flyway |
| Cache / OTP | Redis (or in-memory in test profile) |
| Object storage | MinIO (S3-compatible) |
| API docs | springdoc-openapi / Swagger UI |
| Tests | JUnit 5, MockMvc, Testcontainers |
| Packaging | Multi-stage Docker image (Temurin 21) |

## Related docs

- [Services flow](./services-flow.md) — business journeys and service interactions
- [Test flow](./test-flow.md) — CI and integration test pipeline
- [README](../../README.md) — setup and operations
