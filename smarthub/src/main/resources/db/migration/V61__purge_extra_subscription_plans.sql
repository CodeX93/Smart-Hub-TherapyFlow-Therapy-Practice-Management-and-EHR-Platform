-- Idempotent purge: keep only Starter, Professional, Enterprise.
-- Merges duplicate tiers first, then applies canonical names/codes.

DO $$
DECLARE
    v_starter_id bigint;
    v_pro_id bigint;
    v_ent_id bigint;
    v_remove_id bigint;
BEGIN
    SELECT id INTO v_starter_id
    FROM public.subscription_plans
    WHERE code = 'STARTER'
    ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, id
    LIMIT 1;

    IF v_starter_id IS NULL THEN
        SELECT id INTO v_starter_id
        FROM public.subscription_plans
        WHERE name ILIKE 'Starter'
        ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, id
        LIMIT 1;
    END IF;

    IF v_starter_id IS NULL THEN
        SELECT id INTO v_starter_id
        FROM public.subscription_plans
        WHERE code IN ('BASIC', 'FREE') OR name ILIKE ANY (ARRAY['Basic', 'Free'])
        ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, id
        LIMIT 1;
    END IF;

    IF v_starter_id IS NULL THEN
        RAISE EXCEPTION 'Cannot purge plans: no Starter/Basic/Free row found to keep as STARTER';
    END IF;

    FOR v_remove_id IN
        SELECT id
        FROM public.subscription_plans
        WHERE id <> v_starter_id
          AND (
              code IN ('FREE', 'BASIC', 'STARTER')
              OR name ILIKE ANY (ARRAY['Free', 'Basic', 'Starter'])
          )
    LOOP
        UPDATE public.org_subscriptions SET plan_id = v_starter_id WHERE plan_id = v_remove_id;
        DELETE FROM public.plan_pricing_tiers WHERE plan_id = v_remove_id;
        DELETE FROM public.plan_feature_versions WHERE plan_id = v_remove_id;
        DELETE FROM public.subscription_plans WHERE id = v_remove_id;
    END LOOP;

    UPDATE public.subscription_plans
    SET name = 'Starter',
        code = 'STARTER',
        status = 'ACTIVE',
        description = COALESCE(description, 'Plan for small practices')
    WHERE id = v_starter_id;

    SELECT id INTO v_pro_id
    FROM public.subscription_plans
    WHERE code = 'PROFESSIONAL'
    ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, id
    LIMIT 1;

    IF v_pro_id IS NULL THEN
        SELECT id INTO v_pro_id
        FROM public.subscription_plans
        WHERE code = 'PRO' OR name ILIKE ANY (ARRAY['Pro', 'Professional'])
        ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, id
        LIMIT 1;
    END IF;

    IF v_pro_id IS NULL THEN
        RAISE EXCEPTION 'Cannot purge plans: PROFESSIONAL plan row not found';
    END IF;

    FOR v_remove_id IN
        SELECT id
        FROM public.subscription_plans
        WHERE id <> v_pro_id
          AND (
              code IN ('PRO', 'PROFESSIONAL')
              OR name ILIKE ANY (ARRAY['Pro', 'Professional'])
          )
    LOOP
        UPDATE public.org_subscriptions SET plan_id = v_pro_id WHERE plan_id = v_remove_id;
        DELETE FROM public.plan_pricing_tiers WHERE plan_id = v_remove_id;
        DELETE FROM public.plan_feature_versions WHERE plan_id = v_remove_id;
        DELETE FROM public.subscription_plans WHERE id = v_remove_id;
    END LOOP;

    UPDATE public.subscription_plans
    SET name = 'Professional',
        code = 'PROFESSIONAL',
        status = 'ACTIVE'
    WHERE id = v_pro_id;

    SELECT id INTO v_ent_id
    FROM public.subscription_plans
    WHERE code = 'ENTERPRISE'
    ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, id
    LIMIT 1;

    IF v_ent_id IS NULL THEN
        SELECT id INTO v_ent_id
        FROM public.subscription_plans
        WHERE name ILIKE 'Enterprise'
        ORDER BY id
        LIMIT 1;
    END IF;

    IF v_ent_id IS NULL THEN
        RAISE EXCEPTION 'Cannot purge plans: ENTERPRISE plan row not found';
    END IF;

    FOR v_remove_id IN
        SELECT id
        FROM public.subscription_plans
        WHERE id <> v_ent_id
          AND (code = 'ENTERPRISE' OR name ILIKE 'Enterprise')
    LOOP
        UPDATE public.org_subscriptions SET plan_id = v_ent_id WHERE plan_id = v_remove_id;
        DELETE FROM public.plan_pricing_tiers WHERE plan_id = v_remove_id;
        DELETE FROM public.plan_feature_versions WHERE plan_id = v_remove_id;
        DELETE FROM public.subscription_plans WHERE id = v_remove_id;
    END LOOP;

    UPDATE public.subscription_plans
    SET name = 'Enterprise',
        code = 'ENTERPRISE',
        status = 'ACTIVE'
    WHERE id = v_ent_id;

    FOR v_remove_id IN
        SELECT id
        FROM public.subscription_plans
        WHERE id NOT IN (v_starter_id, v_pro_id, v_ent_id)
    LOOP
        UPDATE public.org_subscriptions SET plan_id = v_starter_id WHERE plan_id = v_remove_id;
        DELETE FROM public.plan_pricing_tiers WHERE plan_id = v_remove_id;
        DELETE FROM public.plan_feature_versions WHERE plan_id = v_remove_id;
        DELETE FROM public.subscription_plans WHERE id = v_remove_id;
    END LOOP;
END $$;

DELETE FROM public.platform_dashboard_tier_aliases
WHERE plan_code NOT IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE')
   OR tier_name NOT IN ('Starter', 'Professional', 'Enterprise');

INSERT INTO public.platform_dashboard_tier_aliases (tier_name, plan_code)
VALUES
    ('Starter', 'STARTER'),
    ('Professional', 'PROFESSIONAL'),
    ('Enterprise', 'ENTERPRISE')
ON CONFLICT (tier_name, plan_code) DO NOTHING;
