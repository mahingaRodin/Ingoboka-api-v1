-- Priority + entity reference for in-app notifications (urgent claim updates)
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 0;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(32);

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS reference_id UUID;

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id, read_at, created_at DESC)
    WHERE read_at IS NULL;

INSERT INTO notification_templates (code, channel, subject_template, body_template)
SELECT 'CLAIM_STATUS_CHANGE', 'EMAIL',
       'Claim update - {{claimNumber}}',
       'Your claim {{claimNumber}} status is now {{status}}. {{notes}}'
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE code = 'CLAIM_STATUS_CHANGE');

INSERT INTO notification_templates (code, channel, subject_template, body_template)
SELECT 'CLAIM_UPDATED', 'EMAIL',
       'Claim updated - {{claimNumber}}',
       'Your insurer updated claim {{claimNumber}}. {{notes}}'
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE code = 'CLAIM_UPDATED');
