ALTER TABLE public.org_subscriptions
    ADD COLUMN IF NOT EXISTS transaction_execution_id varchar(64);

CREATE INDEX IF NOT EXISTS idx_org_subscriptions_tx_execution
    ON public.org_subscriptions (transaction_execution_id);

