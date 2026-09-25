ALTER TABLE public.invoices
    ADD COLUMN IF NOT EXISTS outstanding_balance numeric(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_paid numeric(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS refunded_amount numeric(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS paid_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS provider_invoice_id varchar(120),
    ADD COLUMN IF NOT EXISTS provider_charge_id varchar(120),
    ADD COLUMN IF NOT EXISTS provider_payment_intent_id varchar(120);

UPDATE public.invoices
SET outstanding_balance = COALESCE(outstanding_balance, amount),
    total_paid = COALESCE(total_paid, 0),
    refunded_amount = COALESCE(refunded_amount, 0)
WHERE outstanding_balance = 0 AND total_paid = 0 AND refunded_amount = 0;

UPDATE public.invoices SET status = UPPER(status) WHERE status IS NOT NULL;
UPDATE public.invoice_adjustments SET adjustment_type = UPPER(adjustment_type) WHERE adjustment_type IS NOT NULL;
UPDATE public.invoice_adjustments SET status = UPPER(status) WHERE status IS NOT NULL;
UPDATE public.invoice_disputes SET status = UPPER(status) WHERE status IS NOT NULL;

ALTER TABLE public.invoices
    DROP CONSTRAINT IF EXISTS chk_invoice_status;

ALTER TABLE public.invoices
    ADD CONSTRAINT chk_invoice_status
        CHECK (status IN ('PENDING','PAID','FAILED','PAST_DUE','VOID'));

ALTER TABLE public.invoice_adjustments
    ADD COLUMN IF NOT EXISTS provider_ref_id varchar(120);

ALTER TABLE public.invoice_disputes
    ADD COLUMN IF NOT EXISTS amount_usd numeric(12,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS public.refund_retry_tasks (
    id bigserial PRIMARY KEY,
    invoice_id bigint NOT NULL REFERENCES public.invoices(id),
    amount_usd numeric(12,2) NOT NULL,
    reason text NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    retry_count integer NOT NULL DEFAULT 0,
    next_retry_at timestamp with time zone NOT NULL DEFAULT now(),
    last_error text,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    created_by bigint
);

CREATE INDEX IF NOT EXISTS idx_refund_retry_tasks_status_next_retry
    ON public.refund_retry_tasks(status, next_retry_at);
