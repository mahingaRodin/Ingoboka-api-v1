INSERT INTO organizations (id, name, code, type, status)
VALUES ('00000000-0000-4000-8000-000000000001', 'Ingoboka Platform', 'INGOBOKA_PLATFORM', 'PLATFORM', 'ACTIVE');

INSERT INTO roles (code, name, description, scope) VALUES
    ('PLATFORM_ADMIN', 'Platform Administrator', 'Manages tenants, global configuration and security operations', 'PLATFORM'),
    ('PARTNER_ADMIN', 'Partner Administrator', 'Manages staff and settings for one tenant organization', 'TENANT'),
    ('COMPLIANCE_AUDITOR', 'Compliance Auditor', 'Read-only access to audit and compliance evidence', 'TENANT'),
    ('INTEGRATION_CLIENT', 'Integration Client', 'Machine identity for scoped API integrations', 'SYSTEM'),
    ('INSURER_PRODUCT_MANAGER', 'Insurer Product Manager', 'Creates and publishes insurance products', 'TENANT'),
    ('UNDERWRITER', 'Underwriter', 'Reviews and approves policy applications', 'TENANT'),
    ('CLAIMS_OFFICER', 'Claims Officer', 'Reviews claim evidence and updates workflow', 'TENANT'),
    ('CLAIMS_SUPERVISOR', 'Claims Supervisor', 'Approves high-value or exception claim decisions', 'TENANT'),
    ('FINANCE_OFFICER', 'Finance Officer', 'Reviews payments, refunds and reconciliation', 'TENANT'),
    ('CUSTOMER_SUPPORT', 'Customer Support Officer', 'Handles enquiries and support tickets', 'TENANT'),
    ('AGENT', 'Agent / Field Officer', 'Assists citizens with onboarding and applications', 'TENANT'),
    ('CITIZEN', 'Citizen / Policyholder', 'Registers, enrolls, pays and submits claims', 'CUSTOMER'),
    ('BENEFICIARY', 'Beneficiary / Dependant', 'Limited access controlled by policy rules', 'CUSTOMER');

INSERT INTO permissions (code, name, module, description) VALUES
    ('auth:public', 'Public authentication', 'AUTH', 'Unauthenticated auth endpoints'),
    ('identity:user:read', 'Read users', 'IDENTITY', 'View user profiles within authorized scope'),
    ('identity:user:write', 'Manage users', 'IDENTITY', 'Create and update users within authorized scope'),
    ('identity:role:read', 'Read roles', 'IDENTITY', 'View role assignments'),
    ('identity:role:write', 'Manage roles', 'IDENTITY', 'Assign and revoke roles'),
    ('tenant:manage', 'Manage tenants', 'TENANT', 'Create and configure partner organizations'),
    ('tenant:read', 'Read tenant data', 'TENANT', 'View tenant configuration'),
    ('audit:read', 'Read audit logs', 'AUDIT', 'View immutable audit trail'),
    ('product:read', 'Read products', 'PRODUCT', 'Browse insurance product catalog'),
    ('product:write', 'Manage products', 'PRODUCT', 'Create and publish insurance products'),
    ('policy:read', 'Read policies', 'POLICY', 'View policy records'),
    ('policy:write', 'Manage policies', 'POLICY', 'Issue and update policies'),
    ('claim:read', 'Read claims', 'CLAIM', 'View claim records'),
    ('claim:write', 'Manage claims', 'CLAIM', 'Process claim workflow'),
    ('payment:read', 'Read payments', 'PAYMENT', 'View payment records'),
    ('payment:write', 'Manage payments', 'PAYMENT', 'Initiate and reconcile payments');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'PLATFORM_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'identity:user:read', 'identity:user:write', 'identity:role:read', 'identity:role:write',
    'tenant:read', 'product:read', 'product:write', 'policy:read', 'policy:write',
    'claim:read', 'claim:write', 'payment:read', 'payment:write'
) WHERE r.code = 'PARTNER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'audit:read', 'tenant:read', 'policy:read', 'claim:read', 'payment:read'
) WHERE r.code = 'COMPLIANCE_AUDITOR';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'product:read', 'product:write', 'policy:read', 'policy:write'
) WHERE r.code = 'INSURER_PRODUCT_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'policy:read', 'policy:write'
) WHERE r.code = 'UNDERWRITER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'claim:read', 'claim:write'
) WHERE r.code = 'CLAIMS_OFFICER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'claim:read', 'claim:write'
) WHERE r.code = 'CLAIMS_SUPERVISOR';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'payment:read', 'payment:write'
) WHERE r.code = 'FINANCE_OFFICER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'policy:read', 'claim:read'
) WHERE r.code = 'CUSTOMER_SUPPORT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'identity:user:read', 'policy:read', 'policy:write'
) WHERE r.code = 'AGENT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'auth:public', 'product:read', 'policy:read', 'policy:write', 'claim:read', 'claim:write', 'payment:read'
) WHERE r.code = 'CITIZEN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN ('auth:public', 'policy:read')
WHERE r.code = 'BENEFICIARY';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
    'policy:read', 'policy:write', 'payment:read', 'claim:read'
) WHERE r.code = 'INTEGRATION_CLIENT';
