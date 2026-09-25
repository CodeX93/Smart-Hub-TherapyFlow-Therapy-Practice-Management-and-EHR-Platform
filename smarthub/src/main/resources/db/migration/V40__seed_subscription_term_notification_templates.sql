-- PLATFORM
-- Seed templates for plan-term end lifecycle notifications.

INSERT INTO billing_notification_templates (event_key, subject_template, body_template, is_active)
VALUES
('SUBSCRIPTION_TERM_ENDING_SOON',
 'Subscription ends in {daysBefore} day(s)',
 'Hello {organisationName}, your current subscription term ends on {termEndAt}. To avoid service interruption, renew or change your plan before the end time.',
 true),
('SUBSCRIPTION_TERM_ENDED',
 'Subscription term ended',
 'Hello {organisationName}, your subscription term ended on {termEndAt}. Access is now restricted. Please renew or contact support to restore access.',
 true)
ON CONFLICT (event_key) DO NOTHING;
