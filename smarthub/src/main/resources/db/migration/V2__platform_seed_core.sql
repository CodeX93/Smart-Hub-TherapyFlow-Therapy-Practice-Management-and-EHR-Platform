-- PLATFORM
-- Core platform seed: organisations, roles, permissions, role_permissions.

-- 1. Roles
INSERT INTO roles (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, description, display_name, is_active, is_system, name)
VALUES
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Full system access', 'Super Admin', true, true, 'SUPER_ADMIN'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Read-only audit access', 'Platform Auditor', true, true, 'AUDITOR'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Support and troubleshooting', 'Platform Support', true, true, 'SUPPORT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Infrastructure and operations', 'Platform DevOps', true, true, 'DEVOPS'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Billing and subscriptions', 'Platform Billing', true, true, 'BILLING_ADMIN'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Organisation administrator', 'Admin', true, true, 'ADMIN'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Clinical therapist', 'Therapist', true, true, 'THERAPIST'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Client/patient', 'Client', true, true, 'CLIENT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Clinical supervisor', 'Supervisor', true, true, 'SUPERVISOR'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'Billing and payments', 'Billing Specialist', true, true, 'BILLING_SPECIALIST'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'System AI assistant', 'System AI', true, true, 'SYSTEM_AI_ASSISTANT');

-- 3. Permissions
INSERT INTO permissions (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, category, description, display_name, is_active, name)
VALUES
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'user_management', 'View users', 'View Users', true, 'USER_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'user_management', 'Create users', 'Create Users', true, 'USER_CREATE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'user_management', 'Edit users', 'Edit Users', true, 'USER_EDIT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'user_management', 'Delete users', 'Delete Users', true, 'USER_DELETE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'user_management', 'Full user management', 'Manage Users', true, 'USER_MANAGE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_management', 'View own clients', 'View Own Clients', true, 'CLIENT_VIEW_OWN'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_management', 'View team clients', 'View Team Clients', true, 'CLIENT_VIEW_TEAM'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_management', 'View all clients', 'View All Clients', true, 'CLIENT_VIEW_ALL'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_management', 'View clients', 'View Clients', true, 'CLIENT_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_management', 'Create clients', 'Create Clients', true, 'CLIENT_CREATE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_management', 'Edit clients', 'Edit Clients', true, 'CLIENT_EDIT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_management', 'Delete clients', 'Delete Clients', true, 'CLIENT_DELETE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_management', 'Export clients', 'Export Clients', true, 'CLIENT_EXPORT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'session_management', 'View sessions', 'View Sessions', true, 'SESSION_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'session_management', 'Create sessions', 'Create Sessions', true, 'SESSION_CREATE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'session_management', 'Edit sessions', 'Edit Sessions', true, 'SESSION_EDIT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'session_management', 'Delete sessions', 'Delete Sessions', true, 'SESSION_DELETE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'assessment', 'View assessments', 'View Assessments', true, 'ASSESSMENT_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'assessment', 'Assign assessments', 'Assign Assessments', true, 'ASSESSMENT_ASSIGN'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'forms', 'Manage form templates', 'Manage Form Templates', true, 'FORM_TEMPLATE_MANAGE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'forms', 'Fill forms', 'Fill Forms', true, 'FORM_FILL'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'forms', 'View forms', 'View Forms', true, 'FORM_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'billing', 'View billing', 'View Billing', true, 'BILLING_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'billing', 'Manage billing', 'Manage Billing', true, 'BILLING_MANAGE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'billing', 'Create billing', 'Create Billing', true, 'BILLING_CREATE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'billing', 'Edit billing', 'Edit Billing', true, 'BILLING_EDIT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'billing', 'Delete billing', 'Delete Billing', true, 'BILLING_DELETE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'billing', 'Export billing', 'Export Billing', true, 'BILLING_EXPORT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'reporting', 'View reports', 'View Reports', true, 'REPORT_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'reporting', 'Export reports', 'Export Reports', true, 'REPORT_EXPORT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'audit', 'View audit log', 'View Audit Log', true, 'AUDIT_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'audit', 'Export audit log', 'Export Audit Log', true, 'AUDIT_EXPORT'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'rooms', 'Manage rooms', 'Manage Rooms', true, 'ROOM_MANAGE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'consent', 'Admin view consents', 'Admin View Consents', true, 'CONSENT_ADMIN_VIEW'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'ai', 'Use AI features', 'Use AI', true, 'AI_USE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'Portal access', 'Client Portal Access', true, 'CLIENT_PORTAL_ACCESS'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'View own profile', 'View Own Profile', true, 'CLIENT_VIEW_OWN_PROFILE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'Edit own profile', 'Edit Own Profile', true, 'CLIENT_EDIT_OWN_PROFILE'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'View own assessments', 'View Own Assessments', true, 'CLIENT_VIEW_OWN_ASSESSMENTS'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'Submit assessments', 'Submit Assessments', true, 'CLIENT_SUBMIT_ASSESSMENTS'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'View own sessions', 'View Own Sessions', true, 'CLIENT_VIEW_OWN_SESSIONS'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'Book appointments', 'Book Appointments', true, 'CLIENT_BOOK_APPOINTMENTS'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'View own documents', 'View Own Documents', true, 'CLIENT_VIEW_OWN_DOCUMENTS'),
  ('2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, 'client_portal', 'View own billing', 'View Own Billing', true, 'CLIENT_VIEW_OWN_BILLING');

-- 4. Role–permission mappings
INSERT INTO role_permissions (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, role_id, permission_id)
SELECT '2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN' AND p.name IN (
  'USER_VIEW','USER_CREATE','USER_EDIT','USER_DELETE','USER_MANAGE',
  'CLIENT_VIEW_OWN','CLIENT_VIEW_TEAM','CLIENT_VIEW_ALL','CLIENT_VIEW','CLIENT_CREATE','CLIENT_EDIT','CLIENT_DELETE','CLIENT_EXPORT',
  'SESSION_VIEW','SESSION_CREATE','SESSION_EDIT','SESSION_DELETE',
  'ASSESSMENT_VIEW','ASSESSMENT_ASSIGN','FORM_TEMPLATE_MANAGE','FORM_FILL','FORM_VIEW',
  'BILLING_VIEW','BILLING_MANAGE','BILLING_CREATE','BILLING_EDIT','BILLING_DELETE','BILLING_EXPORT',
  'REPORT_VIEW','REPORT_EXPORT','AUDIT_VIEW','AUDIT_EXPORT','ROOM_MANAGE','CONSENT_ADMIN_VIEW','AI_USE'
);

INSERT INTO role_permissions (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, role_id, permission_id)
SELECT '2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN' AND p.id IS NOT NULL;

INSERT INTO role_permissions (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, role_id, permission_id)
SELECT '2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'THERAPIST' AND p.name IN (
  'CLIENT_VIEW_OWN','CLIENT_VIEW_TEAM','CLIENT_VIEW','CLIENT_CREATE','CLIENT_EDIT','CLIENT_EXPORT',
  'SESSION_VIEW','SESSION_CREATE','SESSION_EDIT','SESSION_DELETE',
  'ASSESSMENT_VIEW','ASSESSMENT_ASSIGN','FORM_FILL','FORM_VIEW',
  'BILLING_VIEW','BILLING_CREATE','BILLING_EDIT','BILLING_EXPORT','CONSENT_ADMIN_VIEW','AI_USE'
);

INSERT INTO role_permissions (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, role_id, permission_id)
SELECT '2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'SUPERVISOR' AND p.name IN (
  'CLIENT_VIEW_OWN','CLIENT_VIEW_TEAM','CLIENT_VIEW','CLIENT_CREATE','CLIENT_EDIT','CLIENT_EXPORT',
  'SESSION_VIEW','SESSION_CREATE','SESSION_EDIT','SESSION_DELETE',
  'ASSESSMENT_VIEW','ASSESSMENT_ASSIGN','FORM_FILL','FORM_VIEW',
  'BILLING_VIEW','BILLING_CREATE','BILLING_EDIT','BILLING_EXPORT','CONSENT_ADMIN_VIEW','AI_USE'
);

INSERT INTO role_permissions (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, role_id, permission_id)
SELECT '2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'BILLING_SPECIALIST' AND p.name IN (
  'CLIENT_VIEW_OWN','CLIENT_VIEW_TEAM','CLIENT_VIEW_ALL','CLIENT_VIEW',
  'SESSION_VIEW','BILLING_VIEW','BILLING_MANAGE','BILLING_CREATE','BILLING_EDIT','BILLING_DELETE','BILLING_EXPORT','REPORT_VIEW','REPORT_EXPORT'
);

INSERT INTO role_permissions (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, role_id, permission_id)
SELECT '2025-01-01 00:00:00+00'::timestamptz, 0, NULL::timestamptz, false, '2025-01-01 00:00:00+00'::timestamptz, 0, 0, r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'CLIENT' AND p.name IN (
  'CLIENT_PORTAL_ACCESS','CLIENT_VIEW_OWN_PROFILE','CLIENT_EDIT_OWN_PROFILE','CLIENT_VIEW_OWN_ASSESSMENTS','CLIENT_SUBMIT_ASSESSMENTS',
  'CLIENT_VIEW_OWN_SESSIONS','CLIENT_BOOK_APPOINTMENTS','CLIENT_VIEW_OWN_DOCUMENTS','CLIENT_VIEW_OWN_BILLING'
);
