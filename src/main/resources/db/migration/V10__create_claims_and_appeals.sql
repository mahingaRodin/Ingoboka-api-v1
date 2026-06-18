CREATE TABLE claims (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_number        VARCHAR(64) NOT NULL UNIQUE,
    policy_id           UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    citizen_profile_id  UUID NOT NULL REFERENCES citizen_profiles(id) ON DELETE CASCADE,
    claim_type          VARCHAR(64) NOT NULL,
    description         TEXT NOT NULL,
    claimed_amount      NUMERIC(14, 2),
    status              VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT claims_status_check CHECK (
        status IN (
            'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'INFORMATION_REQUIRED',
            'APPROVED', 'REJECTED', 'PAYMENT_PROCESSING', 'PAID'
        )
    )
);

CREATE INDEX idx_claims_policy_id ON claims(policy_id);
CREATE INDEX idx_claims_organization_id ON claims(organization_id);
CREATE INDEX idx_claims_citizen_profile_id ON claims(citizen_profile_id);
CREATE INDEX idx_claims_status ON claims(status);

CREATE TABLE claim_documents (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id                UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    document_type           VARCHAR(64) NOT NULL,
    object_key              VARCHAR(512) NOT NULL,
    mime_type               VARCHAR(128) NOT NULL,
    size_bytes              BIGINT NOT NULL,
    checksum                VARCHAR(128) NOT NULL,
    access_classification   VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT claim_documents_access_check CHECK (
        access_classification IN ('INTERNAL', 'CUSTOMER', 'PUBLIC')
    )
);

CREATE INDEX idx_claim_documents_claim_id ON claim_documents(claim_id);

CREATE TABLE claim_status_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id        UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    from_status     VARCHAR(32),
    to_status       VARCHAR(32) NOT NULL,
    reason          TEXT,
    changed_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_claim_status_history_claim_id ON claim_status_history(claim_id);

CREATE TABLE claim_decisions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id            UUID NOT NULL UNIQUE REFERENCES claims(id) ON DELETE CASCADE,
    decision            VARCHAR(32) NOT NULL,
    approved_amount     NUMERIC(14, 2),
    reason              TEXT NOT NULL,
    decided_by          UUID NOT NULL REFERENCES users(id),
    decided_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT claim_decisions_decision_check CHECK (
        decision IN ('APPROVED', 'REJECTED', 'PARTIAL')
    )
);

CREATE TABLE claim_appeals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id        UUID NOT NULL REFERENCES claims(id) ON DELETE CASCADE,
    reason          TEXT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMPTZ,
    reviewed_by     UUID REFERENCES users(id),
    review_notes    TEXT,
    CONSTRAINT claim_appeals_status_check CHECK (
        status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED')
    )
);

CREATE INDEX idx_claim_appeals_claim_id ON claim_appeals(claim_id);

CREATE TRIGGER trg_claims_updated_at
    BEFORE UPDATE ON claims
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
