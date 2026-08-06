-- Platform-wide key/value settings (admin-configurable, applied immediately via cache).
CREATE TABLE IF NOT EXISTS platform_settings (
    setting_key VARCHAR(128) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_platform_settings_updated_at ON platform_settings (updated_at DESC);

-- Optional outcome for richer audit filtering (SUCCESS / FAILURE / INFO).
ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS outcome VARCHAR(32) NOT NULL DEFAULT 'SUCCESS';

CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_email ON audit_logs (actor_email);
CREATE INDEX IF NOT EXISTS idx_audit_logs_outcome ON audit_logs (outcome);

-- Seed sensible defaults (idempotent).
INSERT INTO platform_settings (setting_key, setting_value)
VALUES
    ('platformName', 'Ingoboka'),
    ('defaultLocale', 'rw'),
    ('maintenanceMode', 'false'),
    ('apiBaseUrl', '/api/v1'),
    ('supportEmail', 'support@ingoboka.rw'),
    ('supportPhone', '+250788000000'),
    ('brandingTagline', 'Digital microinsurance for Rwanda'),
    ('registrationEnabled', 'true'),
    ('selfServiceClaimsEnabled', 'true'),
    ('emailNotificationsEnabled', 'true'),
    ('smsNotificationsEnabled', 'true'),
    ('ussdEnabled', 'true'),
    ('agentAssistedEnabled', 'true'),
    ('requireKycBeforeEnrollment', 'false'),
    ('defaultCurrency', 'RWF'),
    ('defaultPolicyGraceDays', '7'),
    ('maxLoginAttempts', '5'),
    ('apiRateLimitPerMinute', '120')
ON CONFLICT (setting_key) DO NOTHING;
