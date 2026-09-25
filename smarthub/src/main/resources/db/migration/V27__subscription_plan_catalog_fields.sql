ALTER TABLE public.subscription_plans
    ADD COLUMN IF NOT EXISTS code varchar(50),
    ADD COLUMN IF NOT EXISTS trial_days int,
    ADD COLUMN IF NOT EXISTS status varchar(20);

UPDATE public.subscription_plans
SET code = UPPER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '_', 'g'))
WHERE code IS NULL;

UPDATE public.subscription_plans
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE public.subscription_plans
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'subscription_plans_code_key'
    ) THEN
        ALTER TABLE public.subscription_plans
            ADD CONSTRAINT subscription_plans_code_key UNIQUE (code);
    END IF;
END$$;
