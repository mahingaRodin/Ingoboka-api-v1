CREATE TABLE partner_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    registration_number VARCHAR(64),
    contact_email       VARCHAR(320),
    contact_phone       VARCHAR(32),
    address_line        TEXT,
    district            VARCHAR(120),
    country             VARCHAR(2)  NOT NULL DEFAULT 'RW',
    website             VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_partner_profiles_organization_id ON partner_profiles(organization_id);

CREATE TABLE partner_contracts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    contract_reference  VARCHAR(64) NOT NULL UNIQUE,
    status              VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    start_date          DATE,
    end_date            DATE,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT partner_contracts_status_check CHECK (
        status IN ('DRAFT', 'ACTIVE', 'SUSPENDED', 'EXPIRED', 'TERMINATED')
    )
);

CREATE INDEX idx_partner_contracts_organization_id ON partner_contracts(organization_id);
CREATE INDEX idx_partner_contracts_status ON partner_contracts(status);

CREATE TRIGGER trg_partner_profiles_updated_at
    BEFORE UPDATE ON partner_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_partner_contracts_updated_at
    BEFORE UPDATE ON partner_contracts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
