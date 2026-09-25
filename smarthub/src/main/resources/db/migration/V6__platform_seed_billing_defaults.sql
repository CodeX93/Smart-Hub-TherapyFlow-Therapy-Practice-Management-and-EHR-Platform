-- PLATFORM
-- Seed billing lifecycle defaults (dunning policy + notification templates).

-- Global default dunning policy row (organisation_id null)
INSERT INTO dunning_policies (organisation_id, retry_1_hours, retry_2_hours, retry_3_hours, grace_period_days, auto_lock_on_cancel, trial_notice_days_before)
SELECT NULL, 24, 72, 120, 3, true, 7
WHERE NOT EXISTS (SELECT 1 FROM dunning_policies WHERE organisation_id IS NULL);

INSERT INTO billing_notification_templates (event_key, subject_template, body_template, is_active)
VALUES
('TRIAL_EXPIRY_NOTICE', 'Your trial expires soon ({daysBefore} days)', 'Hello {organisationName}, your trial will end on {trialEndAt}. To avoid interruption, please add a payment method and choose a paid plan.', true),
('TRIAL_EXPIRED_PAST_DUE', 'Trial ended: account moved to past_due', 'Hello {organisationName}, your trial ended on {trialEndAt}. Your subscription is now past_due. Please complete payment to keep access.', true),
('DUNNING_ATTEMPT', 'Payment retry attempt #{attempt}', 'Hello {organisationName}, we could not process your payment. This is retry attempt #{attempt}. Next retry is scheduled at {nextDunningAt}.', true),
('SUBSCRIPTION_CANCELLED_NON_PAYMENT', 'Subscription cancelled due to non-payment', 'Hello {organisationName}, your subscription has been cancelled after multiple failed payment retries. Access is now restricted. Contact support to reactivate.', true),
('PAYMENT_RECOVERED', 'Payment received: access restored', 'Hello {organisationName}, payment was received successfully and your account access has been restored.', true)
ON CONFLICT (event_key) DO NOTHING;
