CREATE TABLE premium_schedules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id       UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    due_date        DATE NOT NULL,
    amount          NUMERIC(14, 2) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    paid_at         TIMESTAMPTZ,
    payment_id      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT premium_schedules_status_check CHECK (
        status IN ('PENDING', 'PAID', 'OVERDUE', 'CANCELLED')
    )
);

CREATE INDEX idx_premium_schedules_policy_id ON premium_schedules(policy_id);
CREATE INDEX idx_premium_schedules_status ON premium_schedules(status);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    policy_id           UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    citizen_profile_id  UUID NOT NULL REFERENCES citizen_profiles(id) ON DELETE CASCADE,
    amount              NUMERIC(14, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'RWF',
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    provider            VARCHAR(64) NOT NULL DEFAULT 'SANDBOX',
    provider_reference  VARCHAR(128) NOT NULL UNIQUE,
    idempotency_key     VARCHAR(128) NOT NULL UNIQUE,
    initiated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    CONSTRAINT payments_status_check CHECK (
        status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')
    )
);

CREATE INDEX idx_payments_policy_id ON payments(policy_id);
CREATE INDEX idx_payments_citizen_profile_id ON payments(citizen_profile_id);
CREATE INDEX idx_payments_status ON payments(status);

CREATE TABLE payment_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    event_type          VARCHAR(64) NOT NULL,
    payload             TEXT,
    idempotency_key     VARCHAR(128) NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_events_payment_id ON payment_events(payment_id);

CREATE TABLE refunds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    amount          NUMERIC(14, 2) NOT NULL,
    reason          TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT refunds_status_check CHECK (
        status IN ('PENDING', 'COMPLETED', 'FAILED')
    )
);

CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);

ALTER TABLE premium_schedules
    ADD CONSTRAINT fk_premium_schedules_payment
    FOREIGN KEY (payment_id) REFERENCES payments(id);
