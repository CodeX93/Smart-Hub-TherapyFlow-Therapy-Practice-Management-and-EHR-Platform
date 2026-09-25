-- PLATFORM

ALTER TABLE public.org_subscriptions
    ADD COLUMN IF NOT EXISTS provider_current_period_end TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_org_subscriptions_provider_period_end
    ON public.org_subscriptions (provider_current_period_end);
