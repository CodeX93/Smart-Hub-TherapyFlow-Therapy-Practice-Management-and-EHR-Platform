ALTER TABLE public.subscription_plans
    ADD COLUMN IF NOT EXISTS provider_price_id_monthly varchar(255),
    ADD COLUMN IF NOT EXISTS provider_price_id_annual varchar(255);
