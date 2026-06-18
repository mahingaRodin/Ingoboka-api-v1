CREATE TABLE citizen_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    national_id_hash    VARCHAR(128),
    date_of_birth       DATE,
    gender              VARCHAR(16),
    address_line        TEXT,
    district            VARCHAR(120),
    country             VARCHAR(2)  NOT NULL DEFAULT 'RW',
    occupation          VARCHAR(120),
    preferred_language  VARCHAR(8)  NOT NULL DEFAULT 'en',
    kyc_status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT citizen_profiles_kyc_status_check CHECK (
        kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED')
    )
);

CREATE INDEX idx_citizen_profiles_user_id ON citizen_profiles(user_id);
CREATE INDEX idx_citizen_profiles_national_id_hash ON citizen_profiles(national_id_hash);

CREATE TABLE dependants (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    citizen_profile_id  UUID NOT NULL REFERENCES citizen_profiles(id) ON DELETE CASCADE,
    first_name          VARCHAR(120) NOT NULL,
    last_name           VARCHAR(120) NOT NULL,
    relationship        VARCHAR(32)  NOT NULL,
    date_of_birth       DATE,
    national_id_hash    VARCHAR(128),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT dependants_relationship_check CHECK (
        relationship IN ('SPOUSE', 'CHILD', 'PARENT', 'SIBLING', 'OTHER')
    )
);

CREATE INDEX idx_dependants_citizen_profile_id ON dependants(citizen_profile_id);

CREATE TABLE consents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type    VARCHAR(64) NOT NULL,
    version         VARCHAR(32) NOT NULL,
    granted         BOOLEAN     NOT NULL DEFAULT TRUE,
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT consents_type_check CHECK (
        consent_type IN ('TERMS_OF_SERVICE', 'PRIVACY_POLICY', 'DATA_PROCESSING', 'MARKETING')
    )
);

CREATE UNIQUE INDEX idx_consents_active_user_type ON consents(user_id, consent_type)
    WHERE granted = TRUE AND revoked_at IS NULL;

CREATE INDEX idx_consents_user_id ON consents(user_id);

CREATE TRIGGER trg_citizen_profiles_updated_at
    BEFORE UPDATE ON citizen_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_dependants_updated_at
    BEFORE UPDATE ON dependants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
