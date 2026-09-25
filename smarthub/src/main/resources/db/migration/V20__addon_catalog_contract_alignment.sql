ALTER TABLE public.feature_pricing
    ADD COLUMN IF NOT EXISTS billing_cycle varchar(20) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS unit_value integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE public.feature_pricing
SET billing_cycle = UPPER(billing_cycle)
WHERE billing_cycle IS NOT NULL;

UPDATE public.feature_pricing
SET status = UPPER(status)
WHERE status IS NOT NULL;

ALTER TABLE public.feature_pricing
    DROP CONSTRAINT IF EXISTS chk_feature_pricing_billing_cycle;

ALTER TABLE public.feature_pricing
    ADD CONSTRAINT chk_feature_pricing_billing_cycle
        CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL', 'ONE_TIME'));

ALTER TABLE public.feature_pricing
    DROP CONSTRAINT IF EXISTS chk_feature_pricing_unit_value;

ALTER TABLE public.feature_pricing
    ADD CONSTRAINT chk_feature_pricing_unit_value
        CHECK (unit_value >= 1);

ALTER TABLE public.feature_pricing
    DROP CONSTRAINT IF EXISTS chk_feature_pricing_status;

ALTER TABLE public.feature_pricing
    ADD CONSTRAINT chk_feature_pricing_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'));

CREATE TABLE IF NOT EXISTS public.org_addon_billing_line_items (
    id bigserial PRIMARY KEY,
    organisation_id bigint NOT NULL REFERENCES public.organisations(id),
    subscription_id bigint NOT NULL REFERENCES public.org_subscriptions(id),
    org_feature_purchase_id bigint NOT NULL REFERENCES public.org_feature_purchases(id),
    feature_code varchar(100) NOT NULL,
    quantity integer NOT NULL,
    unit_value integer NOT NULL,
    unit_price_usd numeric(12,2) NOT NULL,
    total_amount_usd numeric(12,2) NOT NULL,
    billing_cycle varchar(20) NOT NULL,
    created_by bigint,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT chk_org_addon_billing_line_items_quantity CHECK (quantity >= 1),
    CONSTRAINT chk_org_addon_billing_line_items_unit_value CHECK (unit_value >= 1),
    CONSTRAINT chk_org_addon_billing_line_items_billing_cycle CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL', 'ONE_TIME'))
);

CREATE INDEX IF NOT EXISTS idx_org_addon_billing_line_items_org
    ON public.org_addon_billing_line_items(organisation_id, created_at);

CREATE INDEX IF NOT EXISTS idx_org_addon_billing_line_items_purchase
    ON public.org_addon_billing_line_items(org_feature_purchase_id);
