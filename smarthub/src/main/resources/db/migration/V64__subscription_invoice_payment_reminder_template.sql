-- PLATFORM

INSERT INTO billing_notification_templates (event_key, subject_template, body_template, is_active)
VALUES
('SUBSCRIPTION_INVOICE_PAYMENT_REMINDER',
 'Payment reminder: invoice #{invoiceId} due {dueDate}',
 'Hello {organisationName}, this is a reminder to pay subscription invoice #{invoiceId} for {amount}. Outstanding balance: {outstandingBalance}. Please sign in to billing settings and complete payment before {dueDate}.',
 true)
ON CONFLICT (event_key) DO NOTHING;
