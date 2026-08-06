-- Platform-wide and insurer-scoped announcement banners
CREATE TABLE platform_announcements (
    id              UUID PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    body            TEXT NOT NULL,
    source          VARCHAR(32) NOT NULL,
    organization_id UUID REFERENCES organizations(id),
    priority        INT NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    CONSTRAINT platform_announcements_source_check CHECK (
        source IN ('PLATFORM', 'INSURER')
    )
);

CREATE INDEX idx_platform_announcements_active_created
    ON platform_announcements (active, created_at DESC);

-- Needs assessment completion + stored preferences for product recommendations
ALTER TABLE citizen_profiles
    ADD COLUMN IF NOT EXISTS needs_assessment_completed_at TIMESTAMPTZ;

ALTER TABLE citizen_profiles
    ADD COLUMN IF NOT EXISTS needs_assessment_preferences JSONB;

-- Additional citizen notification templates (multi-channel content)
INSERT INTO notification_templates (code, channel, subject_template, body_template)
SELECT 'KYC_APPROVED', 'EMAIL', 'Account verified - Ingoboka', 'Hello {{fullName}}, your Ingoboka account has been approved. You can now enroll in insurance plans.'
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE code = 'KYC_APPROVED');

INSERT INTO notification_templates (code, channel, subject_template, body_template)
SELECT 'PAYOUT_READY', 'EMAIL', 'Payout ready - {{policyNumber}}', 'Your payout of {{amount}} {{currency}} for policy {{policyNumber}} is ready for collection.'
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE code = 'PAYOUT_READY');
