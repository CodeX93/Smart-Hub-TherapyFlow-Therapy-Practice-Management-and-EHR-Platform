-- PLATFORM
-- Add limits for assessment template creation and AI usage.

INSERT INTO app_features (code, name, description, created_at) VALUES
('ASSESSMENT_TEMPLATES', 'Assessment templates', 'Maximum assessment templates per organisation', CURRENT_TIMESTAMP),
('AI_REPORTS_PER_MONTH', 'AI reports per month', 'Monthly AI report generation limit', CURRENT_TIMESTAMP),
('AI_CONTENT_GENERATIONS_PER_MONTH', 'AI content generations per month', 'Monthly AI content generation limit', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Free defaults: strict limits
WITH free_plan AS (
    SELECT id FROM subscription_plans WHERE name = 'Free' LIMIT 1
),
free_feats AS (
    SELECT id, code FROM app_features
    WHERE code IN ('ASSESSMENT_TEMPLATES', 'AI_REPORTS_PER_MONTH', 'AI_CONTENT_GENERATIONS_PER_MONTH')
)
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id,
       f.id,
       true,
       CASE f.code
           WHEN 'ASSESSMENT_TEMPLATES' THEN 3
           WHEN 'AI_REPORTS_PER_MONTH' THEN 20
           WHEN 'AI_CONTENT_GENERATIONS_PER_MONTH' THEN 100
       END,
       false,
       CURRENT_TIMESTAMP,
       NULL
FROM free_plan p
CROSS JOIN free_feats f
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_versions pf
    WHERE pf.plan_id = p.id AND pf.feature_id = f.id
);

-- Starter defaults: moderate limits
WITH starter_plan AS (
    SELECT id FROM subscription_plans WHERE name = 'Starter' LIMIT 1
),
starter_feats AS (
    SELECT id, code FROM app_features
    WHERE code IN ('ASSESSMENT_TEMPLATES', 'AI_REPORTS_PER_MONTH', 'AI_CONTENT_GENERATIONS_PER_MONTH')
)
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id,
       f.id,
       true,
       CASE f.code
           WHEN 'ASSESSMENT_TEMPLATES' THEN 20
           WHEN 'AI_REPORTS_PER_MONTH' THEN 100
           WHEN 'AI_CONTENT_GENERATIONS_PER_MONTH' THEN 500
       END,
       false,
       CURRENT_TIMESTAMP,
       NULL
FROM starter_plan p
CROSS JOIN starter_feats f
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_versions pf
    WHERE pf.plan_id = p.id AND pf.feature_id = f.id
);

-- Professional defaults: enabled + unlimited
WITH pro_plan AS (
    SELECT id FROM subscription_plans WHERE name = 'Professional' LIMIT 1
),
pro_feats AS (
    SELECT id, code FROM app_features
    WHERE code IN ('ASSESSMENT_TEMPLATES', 'AI_REPORTS_PER_MONTH', 'AI_CONTENT_GENERATIONS_PER_MONTH')
)
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id, true, NULL, true, CURRENT_TIMESTAMP, NULL
FROM pro_plan p
CROSS JOIN pro_feats f
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_versions pf
    WHERE pf.plan_id = p.id AND pf.feature_id = f.id
);

-- Enterprise defaults: enabled + unlimited
WITH ent_plan AS (
    SELECT id FROM subscription_plans WHERE name = 'Enterprise' LIMIT 1
),
ent_feats AS (
    SELECT id, code FROM app_features
    WHERE code IN ('ASSESSMENT_TEMPLATES', 'AI_REPORTS_PER_MONTH', 'AI_CONTENT_GENERATIONS_PER_MONTH')
)
INSERT INTO plan_feature_versions (plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to)
SELECT p.id, f.id, true, NULL, true, CURRENT_TIMESTAMP, NULL
FROM ent_plan p
CROSS JOIN ent_feats f
WHERE NOT EXISTS (
    SELECT 1 FROM plan_feature_versions pf
    WHERE pf.plan_id = p.id AND pf.feature_id = f.id
);
