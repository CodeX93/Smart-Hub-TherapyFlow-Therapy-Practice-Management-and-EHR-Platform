-- Rename Basic tier back to Starter for environments that already applied V59 with BASIC code.
UPDATE public.subscription_plans
SET name = 'Starter',
    code = 'STARTER'
WHERE code = 'BASIC'
   OR name ILIKE 'Basic';

UPDATE public.platform_dashboard_tier_aliases
SET tier_name = 'Starter',
    plan_code = 'STARTER'
WHERE plan_code = 'BASIC'
   OR tier_name ILIKE 'Basic';
