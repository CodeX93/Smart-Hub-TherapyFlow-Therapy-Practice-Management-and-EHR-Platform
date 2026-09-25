-- PLATFORM
-- Seed control-plane defaults: integrations, templates, impersonation policy, platform roles/users, reserved subdomains.

INSERT INTO platform_integration_configs (integration_key, enabled, config_json, masked_summary)
SELECT 'stripe', FALSE, '{}', ''
WHERE NOT EXISTS (SELECT 1 FROM platform_integration_configs WHERE integration_key = 'stripe');
INSERT INTO platform_integration_configs (integration_key, enabled, config_json, masked_summary)
SELECT 'zoom', FALSE, '{}', ''
WHERE NOT EXISTS (SELECT 1 FROM platform_integration_configs WHERE integration_key = 'zoom');
INSERT INTO platform_integration_configs (integration_key, enabled, config_json, masked_summary)
SELECT 'openai', FALSE, '{}', ''
WHERE NOT EXISTS (SELECT 1 FROM platform_integration_configs WHERE integration_key = 'openai');
INSERT INTO platform_integration_configs (integration_key, enabled, config_json, masked_summary)
SELECT 'sparkpost', FALSE, '{}', ''
WHERE NOT EXISTS (SELECT 1 FROM platform_integration_configs WHERE integration_key = 'sparkpost');

INSERT INTO platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'maintenance', 'Scheduled maintenance notice', 'We will perform maintenance at {{startAt}}. Expected duration: {{duration}}.', TRUE
WHERE NOT EXISTS (SELECT 1 FROM platform_notification_templates WHERE template_key = 'maintenance');
INSERT INTO platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'broadcast', 'Platform update', '{{message}}', TRUE
WHERE NOT EXISTS (SELECT 1 FROM platform_notification_templates WHERE template_key = 'broadcast');

INSERT INTO platform_impersonation_policy (
    enabled, require_reason, min_reason_length, max_duration_minutes, allow_cross_organisation
)
SELECT TRUE, TRUE, 10, 60, FALSE
WHERE NOT EXISTS (SELECT 1 FROM platform_impersonation_policy);

INSERT INTO reserved_subdomains (subdomain) VALUES
    ('admin'), ('platform'), ('www'), ('api'), ('docs'), ('status'), ('cdn'), ('mail')
ON CONFLICT (subdomain) DO NOTHING;
