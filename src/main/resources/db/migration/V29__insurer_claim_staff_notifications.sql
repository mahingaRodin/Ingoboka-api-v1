-- Insurer staff in-app notification templates for claim events

INSERT INTO notification_templates (code, channel, subject_template, body_template)
SELECT 'INSURER_CLAIM_SUBMITTED', 'IN_APP',
       'New claim — {{claimNumber}}',
       'Claim {{claimNumber}} from {{claimantName}} (policy {{policyNumber}}) was submitted for {{claimedAmount}} {{currency}}.'
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE code = 'INSURER_CLAIM_SUBMITTED');

INSERT INTO notification_templates (code, channel, subject_template, body_template)
SELECT 'INSURER_CLAIM_STATUS', 'IN_APP',
       'Claim {{claimNumber}} — {{statusLabel}}',
       'Claim {{claimNumber}} for {{claimantName}} is now {{statusLabel}}. {{reason}}'
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE code = 'INSURER_CLAIM_STATUS');

INSERT INTO notification_templates (code, channel, subject_template, body_template)
SELECT 'INSURER_CLAIM_DECISION', 'IN_APP',
       'Claim {{claimNumber}} — {{decision}}',
       'Claim {{claimNumber}} for {{claimantName}}: {{decision}}. {{reason}}'
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE code = 'INSURER_CLAIM_DECISION');
