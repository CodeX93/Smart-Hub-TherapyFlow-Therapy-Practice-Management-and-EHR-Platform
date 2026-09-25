-- Ensure core features used by backend exist in app_features.
-- Idempotent: inserts only when code is missing.
INSERT INTO public.app_features (code, name, description, scope, feature_type, default_enabled)
SELECT v.code, v.name, v.description, 'TENANT', 'CORE', v.default_enabled
FROM (
    VALUES
        ('THERAPIST_LIMIT', 'Therapist Limit', 'Maximum active therapists', FALSE),
        ('SUPERVISOR_LIMIT', 'Supervisor Limit', 'Maximum active supervisors', FALSE),
        ('DOCUMENT_UPLOAD_GB', 'Document Upload (GB)', 'Document upload storage limit (GB)', FALSE),
        ('TASK_LIMIT', 'Task Limit', 'Task creation limit', FALSE),
        ('ZOOM_SESSIONS_PER_MONTH', 'Zoom Sessions Per Month', 'Zoom sessions per month', FALSE),
        ('STRIPE_PAYMENTS', 'Stripe Payments', 'Stripe payment processing', FALSE)
) AS v(code, name, description, default_enabled)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.app_features af
    WHERE UPPER(af.code) = UPPER(v.code)
);
