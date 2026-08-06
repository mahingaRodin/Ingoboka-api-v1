UPDATE audit_logs SET outcome = 'FAILED' WHERE outcome = 'FAILURE';

ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_outcome_check;

ALTER TABLE audit_logs
    ADD CONSTRAINT audit_logs_outcome_check
        CHECK (outcome IN ('SUCCESS', 'FAILED', 'PENDING', 'INFO'));
