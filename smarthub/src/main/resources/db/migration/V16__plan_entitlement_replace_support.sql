INSERT INTO public.app_features (code, name, description)
SELECT 'SSO_ENABLED', 'SSO Enabled', 'Single sign-on switch'
WHERE NOT EXISTS (SELECT 1 FROM public.app_features WHERE code = 'SSO_ENABLED');

INSERT INTO public.app_features (code, name, description)
SELECT 'ADVANCED_BILLING', 'Advanced Billing', 'Advanced billing switch'
WHERE NOT EXISTS (SELECT 1 FROM public.app_features WHERE code = 'ADVANCED_BILLING');

CREATE INDEX IF NOT EXISTS idx_plan_feature_plan_current
    ON public.plan_feature_versions (plan_id, effective_to);
