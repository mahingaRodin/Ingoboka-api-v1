CREATE TABLE policies (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_number           VARCHAR(64) NOT NULL UNIQUE,
    organization_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    application_id          UUID NOT NULL UNIQUE REFERENCES policy_applications(id),
    citizen_profile_id      UUID NOT NULL REFERENCES citizen_profiles(id) ON DELETE CASCADE,
    product_plan_id         UUID NOT NULL REFERENCES product_plans(id),
    status                  VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
    premium_amount          NUMERIC(14, 2) NOT NULL,
    premium_frequency       VARCHAR(32) NOT NULL,
    start_date              DATE,
    end_date                DATE,
    qr_verification_token   VARCHAR(128) NOT NULL UNIQUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT policies_status_check CHECK (
        status IN ('PENDING_PAYMENT', 'ACTIVE', 'GRACE_PERIOD', 'LAPSED', 'EXPIRED', 'CANCELLED')
    )
);

CREATE INDEX idx_policies_organization_id ON policies(organization_id);
CREATE INDEX idx_policies_citizen_profile_id ON policies(citizen_profile_id);
CREATE INDEX idx_policies_status ON policies(status);
CREATE INDEX idx_policies_qr_token ON policies(qr_verification_token);

CREATE TABLE policy_members (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id           UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    member_type         VARCHAR(32) NOT NULL,
    dependant_id        UUID REFERENCES dependants(id),
    full_name           VARCHAR(255) NOT NULL,
    relationship        VARCHAR(32),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT policy_members_type_check CHECK (
        member_type IN ('POLICYHOLDER', 'DEPENDANT')
    )
);

CREATE INDEX idx_policy_members_policy_id ON policy_members(policy_id);

CREATE TABLE policy_documents (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id               UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    document_type           VARCHAR(64) NOT NULL,
    object_key              VARCHAR(512) NOT NULL,
    mime_type               VARCHAR(128) NOT NULL,
    size_bytes              BIGINT NOT NULL,
    checksum                VARCHAR(128) NOT NULL,
    access_classification   VARCHAR(32) NOT NULL DEFAULT 'CUSTOMER',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT policy_documents_access_check CHECK (
        access_classification IN ('INTERNAL', 'CUSTOMER', 'PUBLIC')
    )
);

CREATE INDEX idx_policy_documents_policy_id ON policy_documents(policy_id);

CREATE TRIGGER trg_policies_updated_at
    BEFORE UPDATE ON policies
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
