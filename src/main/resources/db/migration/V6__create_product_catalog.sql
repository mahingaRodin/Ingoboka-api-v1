CREATE TABLE insurance_products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(64)  NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT insurance_products_status_check CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')
    ),
    CONSTRAINT insurance_products_org_code_unique UNIQUE (organization_id, code)
);

CREATE INDEX idx_insurance_products_organization_id ON insurance_products(organization_id);
CREATE INDEX idx_insurance_products_status ON insurance_products(status);

CREATE TABLE product_plans (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL REFERENCES insurance_products(id) ON DELETE CASCADE,
    code                VARCHAR(64)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    premium_amount      NUMERIC(14, 2) NOT NULL,
    premium_frequency   VARCHAR(32)  NOT NULL,
    waiting_period_days INTEGER      NOT NULL DEFAULT 0,
    status              VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT product_plans_status_check CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')
    ),
    CONSTRAINT product_plans_frequency_check CHECK (
        premium_frequency IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'ANNUAL')
    ),
    CONSTRAINT product_plans_product_code_unique UNIQUE (product_id, code)
);

CREATE INDEX idx_product_plans_product_id ON product_plans(product_id);
CREATE INDEX idx_product_plans_status ON product_plans(status);

CREATE TABLE product_benefits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id         UUID NOT NULL REFERENCES product_plans(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    coverage_limit  NUMERIC(14, 2),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_benefits_plan_id ON product_benefits(plan_id);

CREATE TABLE product_exclusions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id     UUID NOT NULL REFERENCES product_plans(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_exclusions_plan_id ON product_exclusions(plan_id);

CREATE TABLE eligibility_rules (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id     UUID NOT NULL REFERENCES product_plans(id) ON DELETE CASCADE,
    min_age     INTEGER,
    max_age     INTEGER,
    rule_type   VARCHAR(64) NOT NULL DEFAULT 'AGE_RANGE',
    rule_value  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT eligibility_rules_type_check CHECK (
        rule_type IN ('AGE_RANGE', 'OCCUPATION', 'CUSTOM')
    )
);

CREATE INDEX idx_eligibility_rules_plan_id ON eligibility_rules(plan_id);

CREATE TRIGGER trg_insurance_products_updated_at
    BEFORE UPDATE ON insurance_products
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_product_plans_updated_at
    BEFORE UPDATE ON product_plans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
