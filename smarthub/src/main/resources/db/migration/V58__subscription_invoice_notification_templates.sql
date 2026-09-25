-- PLATFORM

INSERT INTO billing_notification_templates (event_key, subject_template, body_template, is_active)
VALUES
('SUBSCRIPTION_INVOICE_READY',
 'Subscription invoice ready for payment',
 'Hello {organisationName}, your subscription invoice for {amount} is ready. Sign in to your billing settings to pay before {dueDate}.',
 true),
('SUBSCRIPTION_CHECKOUT_REQUIRED',
 'Action required: complete subscription setup',
 'Hello {organisationName}, your trial has ended or your subscription requires payment. Please complete checkout in billing settings to restore full access.',
 true)
ON CONFLICT (event_key) DO NOTHING;
