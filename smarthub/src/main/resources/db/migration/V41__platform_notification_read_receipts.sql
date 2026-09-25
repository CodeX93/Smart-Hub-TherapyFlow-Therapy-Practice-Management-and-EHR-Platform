CREATE TABLE IF NOT EXISTS public.platform_notification_read_receipts (
    id BIGSERIAL PRIMARY KEY,
    notification_job_id BIGINT NOT NULL REFERENCES public.platform_notification_jobs(id) ON DELETE CASCADE,
    auth_id BIGINT NOT NULL,
    read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_platform_notification_read_receipts_auth_job
    ON public.platform_notification_read_receipts(auth_id, notification_job_id);

CREATE INDEX IF NOT EXISTS idx_platform_notification_read_receipts_auth
    ON public.platform_notification_read_receipts(auth_id);

CREATE INDEX IF NOT EXISTS idx_platform_notification_read_receipts_job
    ON public.platform_notification_read_receipts(notification_job_id);
