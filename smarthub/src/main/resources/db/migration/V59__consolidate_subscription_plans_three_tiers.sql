-- Consolidate catalog to three plans: Starter, Professional, Enterprise.
-- Migrates org subscriptions off removed tiers, then deletes legacy plan rows.

DO $$
DECLARE
    v_starter_id bigint;
    v_pro_id bigint;
    v_ent_id bigint;
    v_keep_ids bigint[];
BEGIN
    -- Canonical Starter tier (legacy Basic/Free naming).
    UPDATE public.subscription_plans
    SET name = 'Starter',
        code = 'STARTER',
        status = 'ACTIVE',
        description = COALESCE(description, 'Plan for small practices')
    WHERE name ILIKE 'Starter'
       OR code ILIKE 'STARTER'
       OR name ILIKE 'Basic'
       OR code ILIKE 'BASIC';

    -- If Free exists and Starter does not, promote Free to Starter pricing.
    IF NOT EXISTS (SELECT 1 FROM public.subscription_plans WHERE code = 'STARTER')
       AND EXISTS (SELECT 1 FROM public.subscription_plans WHERE code = 'FREE' OR name ILIKE 'Free') THEN
        UPDATE public.subscription_plans
        SET name = 'Starter',
            code = 'STARTER',
            base_price = 49.00,
            annual_price = 470.00,
            status = 'ACTIVE',
            description = COALESCE(description, 'Plan for small practices')
        WHERE code = 'FREE' OR name ILIKE 'Free';
    END IF;

    -- Normalize Professional (legacy Pro code/name).
    UPDATE public.subscription_plans
    SET name = 'Professional',
        code = 'PROFESSIONAL',
        status = 'ACTIVE'
    WHERE name ILIKE 'Professional'
       OR name ILIKE 'Pro'
       OR code ILIKE 'PRO'
       OR code ILIKE 'PROFESSIONAL';

    -- Normalize Enterprise.
    UPDATE public.subscription_plans
    SET name = 'Enterprise',
        code = 'ENTERPRISE',
        status = 'ACTIVE'
    WHERE name ILIKE 'Enterprise'
       OR code ILIKE 'ENTERPRISE';

    SELECT id INTO v_starter_id
    FROM public.subscription_plans
    WHERE code = 'STARTER'
    ORDER BY id
    LIMIT 1;

    SELECT id INTO v_pro_id
    FROM public.subscription_plans
    WHERE code = 'PROFESSIONAL'
    ORDER BY id
    LIMIT 1;

    SELECT id INTO v_ent_id
    FROM public.subscription_plans
    WHERE code = 'ENTERPRISE'
    ORDER BY id
    LIMIT 1;

    IF v_starter_id IS NULL OR v_pro_id IS NULL OR v_ent_id IS NULL THEN
        RAISE EXCEPTION 'Plan consolidation failed: missing STARTER (%), PROFESSIONAL (%), or ENTERPRISE (%)',
            v_starter_id, v_pro_id, v_ent_id;
    END IF;

    v_keep_ids := ARRAY[v_starter_id, v_pro_id, v_ent_id];

    -- Move subscriptions on removed tiers to Starter (smallest paid tier).
    UPDATE public.org_subscriptions os
    SET plan_id = v_starter_id
    WHERE os.plan_id <> ALL (v_keep_ids);

    DELETE FROM public.plan_pricing_tiers
    WHERE plan_id <> ALL (v_keep_ids);

    DELETE FROM public.plan_feature_versions
    WHERE plan_id <> ALL (v_keep_ids);

    DELETE FROM public.subscription_plans
    WHERE id <> ALL (v_keep_ids);
END $$;

-- Dashboard chart aliases: three tiers only.
DELETE FROM public.platform_dashboard_tier_aliases;

INSERT INTO public.platform_dashboard_tier_aliases (tier_name, plan_code)
VALUES
    ('Starter', 'STARTER'),
    ('Professional', 'PROFESSIONAL'),
    ('Enterprise', 'ENTERPRISE')
ON CONFLICT (tier_name, plan_code) DO NOTHING;
