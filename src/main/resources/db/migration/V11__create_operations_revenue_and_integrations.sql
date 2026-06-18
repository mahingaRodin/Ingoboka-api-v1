CREATE TABLE document_registry (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         UUID REFERENCES organizations(id) ON DELETE SET NULL,
    owner_user_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    entity_type             VARCHAR(64) NOT NULL,
    entity_id               UUID NOT NULL,
    document_type           VARCHAR(64) NOT NULL,
    object_key              VARCHAR(512) NOT NULL,
    mime_type               VARCHAR(128) NOT NULL,
    size_bytes              BIGINT NOT NULL,
    checksum                VARCHAR(128) NOT NULL,
    malware_scan_status     VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    access_classification   VARCHAR(32) NOT NULL DEFAULT 'INTERNAL',
    retention_until         DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT document_registry_entity_check CHECK (
        entity_type IN ('POLICY', 'CLAIM', 'APPLICATION', 'PARTNER', 'GENERAL')
    ),
    CONSTRAINT document_registry_malware_check CHECK (
        malware_scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'SKIPPED')
    ),
    CONSTRAINT document_registry_access_check CHECK (
        access_classification IN ('INTERNAL', 'CUSTOMER', 'PUBLIC')
    )
);

CREATE INDEX idx_document_registry_entity ON document_registry(entity_type, entity_id);
CREATE INDEX idx_document_registry_organization_id ON document_registry(organization_id);
CREATE INDEX idx_document_registry_owner_user_id ON document_registry(owner_user_id);

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    channel         VARCHAR(32) NOT NULL,
    template_code   VARCHAR(64) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            TEXT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMPTZ,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notifications_channel_check CHECK (
        channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')
    ),
    CONSTRAINT notifications_status_check CHECK (
        status IN ('PENDING', 'SENT', 'FAILED', 'READ')
    )
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

CREATE TABLE contract_price_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    contract_id         UUID REFERENCES partner_contracts(id) ON DELETE SET NULL,
    rule_type           VARCHAR(64) NOT NULL,
    rate_type           VARCHAR(32) NOT NULL,
    rate_value          NUMERIC(14, 4) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'RWF',
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT contract_price_rules_type_check CHECK (
        rule_type IN ('PLATFORM_FEE', 'COMMISSION', 'PER_POLICY_FEE', 'SUBSCRIPTION')
    ),
    CONSTRAINT contract_price_rules_rate_check CHECK (
        rate_type IN ('PERCENTAGE', 'FIXED')
    )
);

CREATE INDEX idx_contract_price_rules_organization_id ON contract_price_rules(organization_id);

CREATE TABLE revenue_ledger (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    policy_id       UUID REFERENCES policies(id) ON DELETE SET NULL,
    payment_id      UUID REFERENCES payments(id) ON DELETE SET NULL,
    entry_type      VARCHAR(64) NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'RWF',
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reference       VARCHAR(128) NOT NULL UNIQUE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT revenue_ledger_type_check CHECK (
        entry_type IN ('COMMISSION', 'PLATFORM_FEE', 'SUBSCRIPTION', 'POLICY_FEE', 'ADJUSTMENT')
    ),
    CONSTRAINT revenue_ledger_status_check CHECK (
        status IN ('PENDING', 'SETTLED', 'REVERSED')
    )
);

CREATE INDEX idx_revenue_ledger_organization_id ON revenue_ledger(organization_id);
CREATE INDEX idx_revenue_ledger_created_at ON revenue_ledger(created_at DESC);

CREATE TABLE invoices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    invoice_number  VARCHAR(64) NOT NULL UNIQUE,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'RWF',
    status          VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    issued_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT invoices_status_check CHECK (
        status IN ('DRAFT', 'ISSUED', 'PAID', 'VOID')
    )
);

CREATE INDEX idx_invoices_organization_id ON invoices(organization_id);

CREATE TABLE data_subject_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_type    VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
    details         TEXT,
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT data_subject_requests_type_check CHECK (
        request_type IN ('ACCESS', 'RECTIFICATION', 'ERASURE', 'PORTABILITY', 'RESTRICTION')
    ),
    CONSTRAINT data_subject_requests_status_check CHECK (
        status IN ('SUBMITTED', 'IN_REVIEW', 'COMPLETED', 'REJECTED')
    )
);

CREATE INDEX idx_data_subject_requests_user_id ON data_subject_requests(user_id);

CREATE TABLE audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID REFERENCES organizations(id) ON DELETE SET NULL,
    actor_user_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email         VARCHAR(320),
    action              VARCHAR(120) NOT NULL,
    entity_type         VARCHAR(64) NOT NULL,
    entity_id           UUID,
    correlation_id      VARCHAR(64),
    summary             TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_organization_id ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(120) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT outbox_events_status_check CHECK (
        status IN ('PENDING', 'PUBLISHED', 'FAILED')
    )
);

CREATE INDEX idx_outbox_events_status ON outbox_events(status);
CREATE INDEX idx_outbox_events_created_at ON outbox_events(created_at DESC);

CREATE TABLE idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_value       VARCHAR(128) NOT NULL UNIQUE,
    scope           VARCHAR(64) NOT NULL,
    response_hash   VARCHAR(128),
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys(expires_at);

CREATE TABLE integration_adapters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(64) NOT NULL UNIQUE,
    adapter_type    VARCHAR(64) NOT NULL,
    name            VARCHAR(180) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    config_json     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT integration_adapters_type_check CHECK (
        adapter_type IN ('PAYMENT', 'MESSAGING', 'IDENTITY', 'INSURER_CORE')
    )
);

CREATE TRIGGER trg_integration_adapters_updated_at
    BEFORE UPDATE ON integration_adapters
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO integration_adapters (code, adapter_type, name, enabled, config_json) VALUES
    ('SANDBOX_PAYMENT', 'PAYMENT', 'Sandbox Payment Provider', TRUE, '{"mode":"sandbox"}'),
    ('SMTP_EMAIL', 'MESSAGING', 'SMTP Email Adapter', TRUE, '{"channel":"email"}'),
    ('SANDBOX_SMS', 'MESSAGING', 'Sandbox SMS Gateway', TRUE, '{"mode":"sandbox"}'),
    ('SANDBOX_IDV', 'IDENTITY', 'Sandbox Identity Verification', TRUE, '{"mode":"sandbox"}'),
    ('SANDBOX_INSURER_CORE', 'INSURER_CORE', 'Sandbox Insurer Core', TRUE, '{"mode":"sandbox"}');
