-- PLATFORM
-- Ensure control-plane defaults survive data wipes that keep flyway history.
-- Idempotent: safe to re-run; does not overwrite existing rows.

INSERT INTO public.platform_tenant_routing_settings (
    email_auto_routing,
    path_based_routing,
    path_prefix,
    org_identifier,
    created_at,
    updated_at,
    updated_by_auth_id
)
SELECT TRUE, FALSE, '/t', 'slug', NOW(), NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM public.platform_tenant_routing_settings);

INSERT INTO public.platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'maintenance', 'Scheduled maintenance notice', 'We will perform maintenance at {{startAt}}. Expected duration: {{duration}}.', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public.platform_notification_templates WHERE template_key = 'maintenance');

INSERT INTO public.platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'broadcast', 'Platform update', '{{message}}', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public.platform_notification_templates WHERE template_key = 'broadcast');

INSERT INTO public.platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'platform_email', 'Platform Notification',
       '<div style="font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; padding: 20px;"><h2 style="color: #1f2937;">{{title}}</h2><p>Hi {{recipientName}},</p><p>{{message}}</p></div>',
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM public.platform_notification_templates WHERE template_key = 'platform_email');

INSERT INTO public.platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'platform_notification_email', 'Platform Notification',
       '<div style="font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; padding: 20px;"><h2 style="color: #1f2937;">{{title}}</h2><p>Hi {{recipientName}},</p><p>{{message}}</p></div>',
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM public.platform_notification_templates WHERE template_key = 'platform_notification_email');

INSERT INTO public.platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'platform_broadcast_email', 'Platform update',
       '<div style="font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; padding: 20px;"><h2 style="color: #1f2937;">{{title}}</h2><p>Hi {{recipientName}},</p><p>{{message}}</p></div>',
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM public.platform_notification_templates WHERE template_key = 'platform_broadcast_email');

INSERT INTO public.platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'broadcast_email', 'Platform update',
       '<div style="font-family: Arial, sans-serif; max-width: 640px; margin: 0 auto; padding: 20px;"><h2 style="color: #1f2937;">{{title}}</h2><p>Hi {{recipientName}},</p><p>{{message}}</p></div>',
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM public.platform_notification_templates WHERE template_key = 'broadcast_email');

INSERT INTO public.platform_notification_templates (template_key, subject_template, body_template, is_active)
SELECT 'platform_notification', 'Platform Notification', '{{message}}', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public.platform_notification_templates WHERE template_key = 'platform_notification');

INSERT INTO public.billing_notification_templates (event_key, subject_template, body_template, is_active)
VALUES
('TRIAL_EXPIRY_NOTICE', 'Your trial expires soon ({daysBefore} days)', 'Hello {organisationName}, your trial will end on {trialEndAt}. To avoid interruption, please add a payment method and choose a paid plan.', true),
('TRIAL_EXPIRED_PAST_DUE', 'Trial ended: account moved to past_due', 'Hello {organisationName}, your trial ended on {trialEndAt}. Your subscription is now past_due. Please complete payment to keep access.', true),
('DUNNING_ATTEMPT', 'Payment retry attempt #{attempt}', 'Hello {organisationName}, we could not process your payment. This is retry attempt #{attempt}. Next retry is scheduled at {nextDunningAt}.', true),
('SUBSCRIPTION_CANCELLED_NON_PAYMENT', 'Subscription cancelled due to non-payment', 'Hello {organisationName}, your subscription has been cancelled after multiple failed payment retries. Access is now restricted. Contact support to reactivate.', true),
('PAYMENT_RECOVERED', 'Payment received: access restored', 'Hello {organisationName}, payment was received successfully and your account access has been restored.', true),
('SUBSCRIPTION_TERM_ENDING_SOON', 'Subscription ends in {daysBefore} day(s)', 'Hello {organisationName}, your current subscription term ends on {termEndAt}. To avoid service interruption, renew or change your plan before the end time.', true),
('SUBSCRIPTION_TERM_ENDED', 'Subscription term ended', 'Hello {organisationName}, your subscription term ended on {termEndAt}. Access is now restricted. Please renew or contact support to restore access.', true),
('SUBSCRIPTION_INVOICE_READY', 'Subscription invoice ready for payment', 'Hello {organisationName}, your subscription invoice for {amount} is ready. Sign in to your billing settings to pay before {dueDate}.', true),
('SUBSCRIPTION_CHECKOUT_REQUIRED', 'Action required: complete subscription setup', 'Hello {organisationName}, your trial has ended or your subscription requires payment. Please complete checkout in billing settings to restore full access.', true)
ON CONFLICT (event_key) DO NOTHING;

INSERT INTO public.platform_dashboard_tier_aliases (tier_name, plan_code)
VALUES
    ('Enterprise', 'ENTERPRISE'),
    ('Pro', 'PRO'),
    ('Pro', 'PROFESSIONAL'),
    ('Growth', 'GROWTH'),
    ('Starter', 'STARTER'),
    ('Starter', 'FREE'),
    ('Starter', 'BASIC')
ON CONFLICT (tier_name, plan_code) DO NOTHING;

INSERT INTO public.dunning_policies (organisation_id, retry_1_hours, retry_2_hours, retry_3_hours, grace_period_days, auto_lock_on_cancel, trial_notice_days_before)
SELECT NULL, 24, 72, 120, 3, true, 7
WHERE NOT EXISTS (SELECT 1 FROM public.dunning_policies WHERE organisation_id IS NULL);
