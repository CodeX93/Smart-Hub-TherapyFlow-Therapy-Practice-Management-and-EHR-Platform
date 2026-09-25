UPDATE public.billing_export_jobs
SET export_type = UPPER(export_type)
WHERE export_type IS NOT NULL;

UPDATE public.billing_export_jobs
SET status = UPPER(status)
WHERE status IS NOT NULL;

ALTER TABLE public.billing_export_jobs
    ADD COLUMN IF NOT EXISTS token_consumed boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS token_consumed_at timestamp with time zone;

ALTER TABLE public.billing_export_jobs
    ALTER COLUMN status SET DEFAULT 'QUEUED';

ALTER TABLE public.billing_export_jobs
    DROP CONSTRAINT IF EXISTS chk_billing_export_jobs_status;

ALTER TABLE public.billing_export_jobs
    ADD CONSTRAINT chk_billing_export_jobs_status
        CHECK (status IN ('QUEUED','IN_PROGRESS','COMPLETED','FAILED'));

ALTER TABLE public.billing_export_jobs
    DROP CONSTRAINT IF EXISTS chk_billing_export_jobs_export_type;

ALTER TABLE public.billing_export_jobs
    ADD CONSTRAINT chk_billing_export_jobs_export_type
        CHECK (export_type IN ('BILLING_INVOICES_CSV','REVENUE_ANALYTICS_CSV'));
