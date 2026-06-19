ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check;

ALTER TABLE users
    ADD CONSTRAINT users_status_check CHECK (
        status IN (
            'PENDING_EMAIL_VERIFICATION',
            'PENDING_ACTIVATION',
            'PENDING_PASSWORD_CHANGE',
            'ACTIVE',
            'LOCKED',
            'DISABLED'
        )
    );
