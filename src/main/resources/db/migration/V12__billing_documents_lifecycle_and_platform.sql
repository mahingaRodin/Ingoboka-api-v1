ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET phone_verified = email_verified WHERE phone_verified = FALSE;

ALTER TABLE policies
    ADD COLUMN IF NOT EXISTS grace_period_end DATE,
    ADD COLUMN IF NOT EXISTS renewed_from_policy_id UUID REFERENCES policies(id);

CREATE TABLE bills (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    policy_id           UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    premium_schedule_id UUID REFERENCES premium_schedules(id) ON DELETE SET NULL,
    bill_number         VARCHAR(64) NOT NULL UNIQUE,
    amount              NUMERIC(14, 2) NOT NULL,
    amount_paid         NUMERIC(14, 2) NOT NULL DEFAULT 0,
    currency            VARCHAR(3) NOT NULL DEFAULT 'RWF',
    status              VARCHAR(32) NOT NULL DEFAULT 'ISSUED',
    due_date            DATE NOT NULL,
    issued_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT bills_status_check CHECK (
        status IN ('ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED')
    )
);

CREATE INDEX idx_bills_policy_id ON bills(policy_id);
CREATE INDEX idx_bills_status ON bills(status);

CREATE TABLE receipts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    policy_id       UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    payment_id      UUID REFERENCES payments(id) ON DELETE SET NULL,
    refund_id       UUID REFERENCES refunds(id) ON DELETE SET NULL,
    bill_id         UUID REFERENCES bills(id) ON DELETE SET NULL,
    receipt_number  VARCHAR(64) NOT NULL UNIQUE,
    receipt_type    VARCHAR(32) NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'RWF',
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT receipts_type_check CHECK (
        receipt_type IN ('FULL_PAYMENT', 'PARTIAL_PAYMENT', 'REFUND')
    )
);

CREATE INDEX idx_receipts_policy_id ON receipts(policy_id);

CREATE TABLE reconciliation_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    reconciliation_date DATE NOT NULL,
    total_payments      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_refunds       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    net_amount          NUMERIC(14, 2) NOT NULL DEFAULT 0,
    status              VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT reconciliation_status_check CHECK (
        status IN ('DRAFT', 'CONFIRMED')
    )
);

CREATE INDEX idx_reconciliation_organization_id ON reconciliation_records(organization_id);

CREATE TABLE organization_settings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE CASCADE,
    settings_json   TEXT NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_organization_settings_updated_at
    BEFORE UPDATE ON organization_settings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE notification_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(64) NOT NULL UNIQUE,
    channel         VARCHAR(32) NOT NULL,
    subject_template VARCHAR(255) NOT NULL,
    body_template   TEXT NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT notification_templates_channel_check CHECK (
        channel IN ('EMAIL', 'SMS', 'PUSH', 'IN_APP')
    )
);

INSERT INTO notification_templates (code, channel, subject_template, body_template) VALUES
    ('OTP_VERIFICATION', 'EMAIL', 'Your Ingoboka verification code', 'Your verification code is: {{otp}}. It expires in {{minutes}} minutes.'),
    ('PAYMENT_SUCCESS', 'EMAIL', 'Payment received - {{policyNumber}}', 'We received {{amount}} {{currency}} for policy {{policyNumber}}. Receipt: {{receiptNumber}}.'),
    ('PAYMENT_PARTIAL', 'EMAIL', 'Partial payment received - {{policyNumber}}', 'Partial payment of {{amount}} {{currency}} received for policy {{policyNumber}}. Remaining: {{remaining}}.'),
    ('BILL_ISSUED', 'EMAIL', 'Premium bill due - {{policyNumber}}', 'Your premium bill {{billNumber}} of {{amount}} {{currency}} is due on {{dueDate}}.'),
    ('REFUND_PROCESSED', 'EMAIL', 'Refund processed - {{policyNumber}}', 'A refund of {{amount}} {{currency}} has been processed for policy {{policyNumber}}.'),
    ('POLICY_ACTIVE', 'EMAIL', 'Policy activated - {{policyNumber}}', 'Your policy {{policyNumber}} is now active from {{startDate}} to {{endDate}}.'),
    ('POLICY_GRACE', 'EMAIL', 'Policy in grace period - {{policyNumber}}', 'Policy {{policyNumber}} is in grace period until {{graceEndDate}}. Please pay outstanding premium.'),
    ('POLICY_LAPSED', 'EMAIL', 'Policy lapsed - {{policyNumber}}', 'Policy {{policyNumber}} has lapsed. Contact support to renew.'),
    ('CLAIM_SUBMITTED', 'EMAIL', 'Claim submitted - {{claimNumber}}', 'Your claim {{claimNumber}} has been submitted and is under review.'),
    ('CLAIM_DECISION', 'EMAIL', 'Claim decision - {{claimNumber}}', 'Claim {{claimNumber}} decision: {{decision}}. {{notes}}'),
    ('SUPPORT_ACK', 'EMAIL', 'Support request received', 'We received your support request and will respond shortly.');

ALTER TABLE data_subject_requests
    ADD COLUMN IF NOT EXISTS resolution_notes TEXT;
