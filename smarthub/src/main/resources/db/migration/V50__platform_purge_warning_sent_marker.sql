ALTER TABLE public.platform_purge_jobs
    ADD COLUMN IF NOT EXISTS warning_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_platform_purge_jobs_warning_schedule
    ON public.platform_purge_jobs (status, warning_sent_at, scheduled_at);
