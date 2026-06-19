ALTER TABLE citizen_profiles DROP CONSTRAINT IF EXISTS citizen_profiles_kyc_status_check;

ALTER TABLE citizen_profiles
    ADD CONSTRAINT citizen_profiles_kyc_status_check CHECK (
        kyc_status IN ('PENDING', 'SUBMITTED', 'VERIFIED', 'REJECTED')
    );
