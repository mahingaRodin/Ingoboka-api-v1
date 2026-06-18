CREATE TABLE quotes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quote_reference     VARCHAR(64) NOT NULL UNIQUE,
    citizen_profile_id  UUID NOT NULL REFERENCES citizen_profiles(id) ON DELETE CASCADE,
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    product_plan_id     UUID NOT NULL REFERENCES product_plans(id) ON DELETE CASCADE,
    premium_amount      NUMERIC(14, 2) NOT NULL,
    premium_frequency   VARCHAR(32)  NOT NULL,
    valid_until         TIMESTAMPTZ  NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT quotes_status_check CHECK (
        status IN ('ACTIVE', 'EXPIRED', 'CONVERTED')
    )
);

CREATE INDEX idx_quotes_citizen_profile_id ON quotes(citizen_profile_id);
CREATE INDEX idx_quotes_organization_id ON quotes(organization_id);
CREATE INDEX idx_quotes_status ON quotes(status);

CREATE TABLE quote_answers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quote_id        UUID NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    question_key    VARCHAR(120) NOT NULL,
    answer_value    TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT quote_answers_unique_key UNIQUE (quote_id, question_key)
);

CREATE TABLE policy_applications (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_reference   VARCHAR(64) NOT NULL UNIQUE,
    quote_id                UUID REFERENCES quotes(id),
    citizen_profile_id      UUID NOT NULL REFERENCES citizen_profiles(id) ON DELETE CASCADE,
    organization_id         UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    product_plan_id         UUID NOT NULL REFERENCES product_plans(id) ON DELETE CASCADE,
    consent_id              UUID NOT NULL REFERENCES consents(id),
    premium_amount          NUMERIC(14, 2) NOT NULL,
    premium_frequency       VARCHAR(32)  NOT NULL,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'SUBMITTED',
    decision_reason         TEXT,
    reviewed_by             UUID REFERENCES users(id),
    reviewed_at             TIMESTAMPTZ,
    submitted_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT policy_applications_status_check CHECK (
        status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN')
    )
);

CREATE INDEX idx_policy_applications_citizen_profile_id ON policy_applications(citizen_profile_id);
CREATE INDEX idx_policy_applications_organization_id ON policy_applications(organization_id);
CREATE INDEX idx_policy_applications_status ON policy_applications(status);

CREATE TABLE application_answers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES policy_applications(id) ON DELETE CASCADE,
    question_key    VARCHAR(120) NOT NULL,
    answer_value    TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT application_answers_unique_key UNIQUE (application_id, question_key)
);

CREATE TRIGGER trg_policy_applications_updated_at
    BEFORE UPDATE ON policy_applications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
