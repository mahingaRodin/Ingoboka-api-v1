# Ingoboka API — Services Flow

Domain service interactions and end-to-end business journeys.

## Service map

```mermaid
flowchart TB
    subgraph Identity["Identity & access"]
        AUTH[AuthService]
        STAFF[StaffProvisioningService]
        ORG[OrganizationManagementService]
        SEED[PlatformAdminSeeder / DemoDataSeeder]
    end

    subgraph Customer["Customer"]
        CUST[CustomerProfileService]
    end

    subgraph Catalog["Product catalog"]
        CAT[ProductCatalogService]
        STORE[DocumentStorageService]
    end

    subgraph Enrollment["Enrollment"]
        ENR[EnrollmentService]
        POL_ISS[PolicyIssuanceService]
    end

    subgraph Policy["Policy"]
        POL[PolicyService]
        LIFE[PolicyLifecycleScheduler]
    end

    subgraph Claims["Claims"]
        CLM[ClaimService]
    end

    subgraph Billing["Billing & payments"]
        BILL[BillingService]
        FIN[BillingFinanceService]
        MOMO_ADP[MoMo payment adapter]
    end

    subgraph Platform["Reporting & ops"]
        REP[ReportingService]
        AUD[AuditComplianceService]
        NOTIF[NotificationService / UserNotificationService]
    end

    AUTH --> CUST
    ENR --> CAT & CUST & POL_ISS
    POL_ISS --> POL & BILL
    BILL --> MOMO_ADP
    CLM --> POL & CUST
    CAT --> STORE
    REP --> POL & CLM & BILL
    STAFF --> ORG & AUTH
```

## Citizen journey — register to claim

```mermaid
sequenceDiagram
    actor C as Citizen
    participant FE as Frontend
    participant AUTH as AuthService
    participant OTP as OtpService / SMTP
    participant CUST as CustomerProfileService
    participant CAT as ProductCatalogService
    participant ENR as EnrollmentService
    participant BILL as BillingService
    participant POL as PolicyService
    participant CLM as ClaimService

    C->>FE: Register (phone + email)
    FE->>AUTH: POST /auth/register
    AUTH->>OTP: Send signup OTP (email)
    C->>FE: Enter OTP
    FE->>AUTH: POST /auth/verify-otp
    AUTH-->>FE: access + refresh tokens

    C->>FE: Grant consent
    FE->>CUST: POST /customers/consent

    C->>FE: Browse catalog
    FE->>CAT: GET /products, GET /products/{id}/detail

    C->>FE: Needs assessment
    FE->>ENR: POST /applications/needs-assessment
    ENR-->>FE: recommendedProducts[]

    C->>FE: Select plan
    FE->>ENR: POST /applications/quote
    ENR-->>FE: productName, planName, premium

    C->>FE: Enroll
    FE->>ENR: POST /applications → submit
    ENR->>POL: issue policy (sandbox auto-approve)

    C->>FE: Pay premium
    FE->>BILL: POST /payments/initiate
    BILL-->>FE: payment URL / sandbox flow
    FE->>BILL: POST /payments/sandbox/callback
    BILL->>POL: activate policy

    C->>FE: View wallet
    FE->>POL: GET /policies (+ enrichment)

    C->>FE: File claim
    FE->>CLM: POST /claims → submit
    CLM-->>FE: statusHistory timeline
```

## Insurer / partner journey

```mermaid
sequenceDiagram
    actor PA as Platform admin
    actor INS as Partner admin
    participant PART as PartnerService
    participant CAT as ProductCatalogService
    participant ENR as EnrollmentService
    participant CLM as ClaimService
    participant REP as ReportingService

    PA->>PART: POST /partners (onboard insurer)
    PART-->>INS: Staff account + temp password

    INS->>CAT: POST /products, plans, publish
    INS->>CAT: Benefits, exclusions, FAQ, documents

    Note over ENR: Citizens submit applications
    INS->>ENR: GET /applications (tenant)
    INS->>ENR: Review / approve (or sandbox auto-approve)

    INS->>CLM: GET /admin/claims
    INS->>CLM: POST /admin/claims/{id}/decision

    INS->>REP: GET /admin/reports/overview
    INS->>REP: GET /admin/reports/claims-breakdown
    INS->>REP: GET /reports/policies/summary
```

## Agent assisted enrollment

```mermaid
flowchart LR
    A[Agent login] --> B[GET /agent/applications]
    A --> C[POST /agent/applications<br/>citizen phone + plan id]
    C --> D{Citizen exists?}
    D -->|yes| E[EnrollmentService<br/>createAgentAssistedApplication]
    D -->|no| F[400 Business error]
    E --> G[Application created<br/>for citizen profile]
    B --> H{Wrong role?}
    H -->|citizen / insurer| I[403 ACCESS_DENIED]
```

## Payment flow (sandbox)

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT: Policy issued from approved application
    PENDING_PAYMENT --> PAYMENT_INITIATED: POST /payments/initiate
    PAYMENT_INITIATED --> SUCCESS: Sandbox callback / webhook
    SUCCESS --> ACTIVE: BillingService activates policy
    PENDING_PAYMENT --> LAPSED: Lifecycle scheduler (unpaid)
    ACTIVE --> GRACE_PERIOD: Missed renewal
    GRACE_PERIOD --> EXPIRED: Grace period ended
```

## Frontend compatibility layer

The Next.js MVP calls a mix of canonical and alias routes. `FrontendCompatController` maps frontend-expected paths to domain services without duplicating business logic.

| Frontend area | Alias examples | Backing service |
|---------------|----------------|-----------------|
| Customer | `/customers/me`, `/customer/dependants` | CustomerProfileService |
| Enrollment | `/applications` shortcut | EnrollmentService |
| Policies | `/policies`, `/policies/me/activity` | PolicyService |
| Claims | `/claims/me`, `/admin/claims` | ClaimService |
| Payments | `/payments/initiate` | BillingService |
| Reports | `/admin/reports/overview`, `/admin/reports/claims-breakdown` | ReportingService, ClaimService |
| Agent | `/agent/applications` | EnrollmentService |

## Scheduled jobs

| Job | Schedule | Service |
|-----|----------|---------|
| Policy lifecycle | Daily 02:00 UTC (`ingoboka.policy.lifecycle.cron`) | `PolicyLifecycleScheduler` — grace period, lapse, expiry |
