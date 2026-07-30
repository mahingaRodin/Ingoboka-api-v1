-- USSD channel: Africa's Talking sandbox registrations (*477#)
CREATE TABLE ussd_registrations (
    id                  UUID PRIMARY KEY,
    phone_number        VARCHAR(32)  NOT NULL,
    registration_type   VARCHAR(16)  NOT NULL,
    full_name           VARCHAR(255) NOT NULL,
    business_name       VARCHAR(255),
    district            VARCHAR(120),
    language            VARCHAR(8)   NOT NULL DEFAULT 'rw',
    user_id             UUID REFERENCES users(id) ON DELETE SET NULL,
    reference_code      VARCHAR(32)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ussd_registrations_type_check CHECK (
        registration_type IN ('FAMILY', 'BUSINESS')
    ),
    CONSTRAINT ussd_registrations_phone_unique UNIQUE (phone_number),
    CONSTRAINT ussd_registrations_reference_unique UNIQUE (reference_code)
);

CREATE INDEX idx_ussd_registrations_user_id ON ussd_registrations(user_id);
CREATE INDEX idx_ussd_registrations_created_at ON ussd_registrations(created_at);
