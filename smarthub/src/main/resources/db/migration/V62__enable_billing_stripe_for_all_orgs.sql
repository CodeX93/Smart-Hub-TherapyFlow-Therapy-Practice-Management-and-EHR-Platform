-- Enable billing module and Stripe payments for every organisation.
-- 1) Plan entitlements on all active subscription tiers (enabled + trial-available).
-- 2) Global rollout rules so trial/plan gaps cannot block access.

INSERT INTO public.app_features (code, name, description, scope, feature_type, default_enabled, created_at)
SELECT v.code, v.name, v.description, 'TENANT', 'CORE', false, CURRENT_TIMESTAMP
FROM (
    VALUES
        ('STRIPE_PAYMENTS', 'Stripe Payments', 'Stripe payment processing'),
        ('BILLING_MODULE', 'Billing module access', 'Billing and invoicing module')
) AS v(code, name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM public.app_features af WHERE UPPER(af.code) = UPPER(v.code)
);

-- Upsert current plan_feature_versions for all subscription plans.
INSERT INTO public.plan_feature_versions (
    plan_id, feature_id, is_enabled, usage_limit, is_trial_available, effective_from, effective_to
)
SELECT sp.id, af.id, true, NULL, true, CURRENT_TIMESTAMP, NULL
FROM public.subscription_plans sp
CROSS JOIN public.app_features af
WHERE sp.is_deleted = false
  AND UPPER(af.code) IN ('STRIPE_PAYMENTS', 'BILLING_MODULE')
  AND NOT EXISTS (
      SELECT 1
      FROM public.plan_feature_versions pfv
      WHERE pfv.plan_id = sp.id
        AND pfv.feature_id = af.id
        AND pfv.effective_to IS NULL
  );

UPDATE public.plan_feature_versions pfv
SET is_enabled = true,
    is_trial_available = true,
    updatedat = CURRENT_TIMESTAMP
FROM public.subscription_plans sp,
     public.app_features af
WHERE pfv.plan_id = sp.id
  AND pfv.feature_id = af.id
  AND sp.is_deleted = false
  AND UPPER(af.code) IN ('STRIPE_PAYMENTS', 'BILLING_MODULE')
  AND pfv.effective_to IS NULL
  AND (pfv.is_enabled IS DISTINCT FROM true OR pfv.is_trial_available IS DISTINCT FROM true);

-- Global rollout: always on for every tenant regardless of plan/trial state.
INSERT INTO public.feature_rollout_rules (
    organisation_id, scope, target_key, feature_key, enabled, usage_limit, start_at, end_at,
    created_at, updated_at, createdat, updatedat, created_by, updated_by, version, is_deleted
)
SELECT NULL, 'GLOBAL', NULL, feature_key, true, NULL, CURRENT_TIMESTAMP, NULL,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 0, false
FROM (VALUES ('STRIPE_PAYMENTS'), ('BILLING_MODULE')) AS v(feature_key)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.feature_rollout_rules frr
    WHERE frr.scope = 'GLOBAL'
      AND frr.feature_key = v.feature_key
      AND frr.is_deleted = false
      AND frr.start_at <= CURRENT_TIMESTAMP
      AND (frr.end_at IS NULL OR frr.end_at > CURRENT_TIMESTAMP)
);
