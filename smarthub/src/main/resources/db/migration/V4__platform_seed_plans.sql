-- PLATFORM
-- Seed app_features, subscription_plans, and plan_feature_versions.

INSERT INTO app_features (code, name, description, created_at) VALUES
('BOOKING', 'Appointment booking', 'Client and therapist scheduling', CURRENT_TIMESTAMP),
('FORMS', 'Forms builder', 'Custom forms and assignments', CURRENT_TIMESTAMP),
('REPORTS', 'Reports & analytics', 'Assessment and progress reports', CURRENT_TIMESTAMP),
('ASSESSMENTS', 'Assessments', 'Standardized assessments and scoring', CURRENT_TIMESTAMP),
('THERAPIST_SEATS', 'Therapist seats', 'Number of therapist users', CURRENT_TIMESTAMP),
('CLIENT_LIMIT', 'Client limit', 'Maximum active clients', CURRENT_TIMESTAMP),
('STORAGE_MB', 'Storage (MB)', 'Document and file storage', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO subscription_plans (name, description, base_price, billing_cycle, created_at)
VALUES
('Basic', 'Starter plan for small practices', 49.00, 'monthly', CURRENT_TIMESTAMP),
('Pro', 'Full features for growing practices', 129.00, 'monthly', CURRENT_TIMESTAMP),
('Enterprise', 'Unlimited with priority support', 299.00, 'monthly', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

WITH plan_ids AS (SELECT id FROM subscription_plans WHERE name = 'Basic'),
     feature_ids AS (SELECT id, code FROM app_features WHERE code IN ('BOOKING', 'THERAPIST_SEATS', 'CLIENT_LIMIT', 'STORAGE_MB'))
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id, true,
  CASE f.code WHEN 'THERAPIST_SEATS' THEN 1 WHEN 'CLIENT_LIMIT' THEN 50 WHEN 'STORAGE_MB' THEN 500 ELSE NULL END,
  false, CURRENT_TIMESTAMP, NULL
FROM plan_ids p CROSS JOIN feature_ids f
WHERE NOT EXISTS (SELECT 1 FROM plan_feature_versions pfv WHERE pfv.plan_id = p.id AND pfv.feature_id = f.id);

WITH plan_ids AS (SELECT id FROM subscription_plans WHERE name = 'Pro'),
     feature_ids AS (SELECT id, code FROM app_features)
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id, true,
  CASE f.code WHEN 'THERAPIST_SEATS' THEN 5 WHEN 'CLIENT_LIMIT' THEN 200 WHEN 'STORAGE_MB' THEN 2000 ELSE NULL END,
  true, CURRENT_TIMESTAMP, NULL
FROM plan_ids p CROSS JOIN feature_ids f
WHERE NOT EXISTS (SELECT 1 FROM plan_feature_versions pfv WHERE pfv.plan_id = p.id AND pfv.feature_id = f.id);

WITH plan_ids AS (SELECT id FROM subscription_plans WHERE name = 'Enterprise'),
     feature_ids AS (SELECT id, code FROM app_features)
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id, true, NULL, true, CURRENT_TIMESTAMP, NULL
FROM plan_ids p CROSS JOIN feature_ids f
WHERE NOT EXISTS (SELECT 1 FROM plan_feature_versions pfv WHERE pfv.plan_id = p.id AND pfv.feature_id = f.id);

-- Align with market-ready pricing: Free, Starter, Professional, Enterprise.
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS annual_price NUMERIC(12,2);
COMMENT ON COLUMN subscription_plans.annual_price IS 'Price when billing_cycle=yearly; null = custom (Enterprise).';

UPDATE subscription_plans SET name = 'Starter', base_price = 49.00, annual_price = 470.00 WHERE name = 'Basic';
UPDATE subscription_plans SET name = 'Professional', base_price = 199.00, annual_price = 1900.00 WHERE name = 'Pro';
UPDATE subscription_plans SET annual_price = NULL WHERE name = 'Enterprise';

INSERT INTO subscription_plans (name, description, base_price, annual_price, billing_cycle, created_at)
VALUES ('Free', 'Solo therapist getting started', 0.00, 0.00, 'monthly', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO app_features (code, name, description, created_at) VALUES
('SESSIONS_PER_MONTH', 'Sessions per month', 'Monthly session limit', CURRENT_TIMESTAMP),
('FORM_TEMPLATES', 'Form templates', 'Max form templates', CURRENT_TIMESTAMP),
('CLIENT_PORTAL', 'Client portal', 'Client self-service portal', CURRENT_TIMESTAMP),
('ROLES_PERMISSIONS', 'Roles & permissions', 'Role-based access control', CURRENT_TIMESTAMP),
('BILLING_MODULE', 'Billing & invoicing', 'Session billing and invoicing', CURRENT_TIMESTAMP),
('AUDIT_EXPORT', 'Audit logs export', 'Export audit logs (CSV/JSON)', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

WITH free_plan AS (SELECT id FROM subscription_plans WHERE name = 'Free' LIMIT 1),
     feat AS (SELECT id, code FROM app_features WHERE code IN ('BOOKING', 'THERAPIST_SEATS', 'CLIENT_LIMIT', 'STORAGE_MB', 'SESSIONS_PER_MONTH', 'FORM_TEMPLATES', 'FORMS', 'REPORTS', 'ASSESSMENTS', 'CLIENT_PORTAL', 'ROLES_PERMISSIONS', 'BILLING_MODULE', 'AUDIT_EXPORT'))
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id,
  CASE f.code WHEN 'BOOKING' THEN true WHEN 'FORMS' THEN true WHEN 'ASSESSMENTS' THEN true WHEN 'CLIENT_PORTAL' THEN false WHEN 'ROLES_PERMISSIONS' THEN false WHEN 'BILLING_MODULE' THEN false WHEN 'AUDIT_EXPORT' THEN false WHEN 'REPORTS' THEN false ELSE true END,
  CASE f.code WHEN 'THERAPIST_SEATS' THEN 1 WHEN 'CLIENT_LIMIT' THEN 50 WHEN 'STORAGE_MB' THEN 500 WHEN 'SESSIONS_PER_MONTH' THEN 100 WHEN 'FORM_TEMPLATES' THEN 1 ELSE NULL END,
  false, CURRENT_TIMESTAMP, NULL
FROM free_plan p CROSS JOIN feat f
WHERE NOT EXISTS (SELECT 1 FROM plan_feature_versions pfv WHERE pfv.plan_id = p.id AND pfv.feature_id = f.id);

WITH starter_plan AS (SELECT id FROM subscription_plans WHERE name = 'Starter' LIMIT 1)
UPDATE plan_feature_versions SET usage_limit = 3
WHERE plan_id = (SELECT id FROM starter_plan) AND feature_id = (SELECT id FROM app_features WHERE code = 'THERAPIST_SEATS' LIMIT 1);

WITH starter_plan AS (SELECT id FROM subscription_plans WHERE name = 'Starter' LIMIT 1),
     feat AS (SELECT id, code FROM app_features WHERE code IN ('SESSIONS_PER_MONTH', 'FORM_TEMPLATES', 'CLIENT_PORTAL', 'ROLES_PERMISSIONS', 'BILLING_MODULE', 'AUDIT_EXPORT'))
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id, false, NULL, false, CURRENT_TIMESTAMP, NULL
FROM starter_plan p CROSS JOIN feat f
WHERE NOT EXISTS (SELECT 1 FROM plan_feature_versions pfv WHERE pfv.plan_id = p.id AND pfv.feature_id = f.id);

WITH pro_plan AS (SELECT id FROM subscription_plans WHERE name = 'Professional' LIMIT 1)
UPDATE plan_feature_versions SET usage_limit = 10
WHERE plan_id = (SELECT id FROM pro_plan) AND feature_id = (SELECT id FROM app_features WHERE code = 'THERAPIST_SEATS' LIMIT 1);

WITH pro_plan AS (SELECT id FROM subscription_plans WHERE name = 'Professional' LIMIT 1),
     feat AS (SELECT id, code FROM app_features WHERE code IN ('SESSIONS_PER_MONTH', 'FORM_TEMPLATES', 'CLIENT_PORTAL', 'ROLES_PERMISSIONS', 'BILLING_MODULE', 'AUDIT_EXPORT'))
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id, true, NULL, true, CURRENT_TIMESTAMP, NULL
FROM pro_plan p CROSS JOIN feat f
WHERE NOT EXISTS (SELECT 1 FROM plan_feature_versions pfv WHERE pfv.plan_id = p.id AND pfv.feature_id = f.id);

WITH ent_plan AS (SELECT id FROM subscription_plans WHERE name = 'Enterprise' LIMIT 1),
     feat AS (SELECT id, code FROM app_features WHERE code IN ('SESSIONS_PER_MONTH', 'FORM_TEMPLATES', 'CLIENT_PORTAL', 'ROLES_PERMISSIONS', 'BILLING_MODULE', 'AUDIT_EXPORT'))
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id, true, NULL, true, CURRENT_TIMESTAMP, NULL
FROM ent_plan p CROSS JOIN feat f
WHERE NOT EXISTS (SELECT 1 FROM plan_feature_versions pfv WHERE pfv.plan_id = p.id AND pfv.feature_id = f.id);
