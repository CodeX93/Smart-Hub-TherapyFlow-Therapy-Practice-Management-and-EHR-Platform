CREATE TABLE IF NOT EXISTS public.platform_password_reset_jobs (
    id bigserial PRIMARY KEY,
    organisation_id bigint NOT NULL REFERENCES public.organisations(id),
    status varchar(20) NOT NULL,
    effective_at timestamp with time zone NOT NULL,
    reason text,
    requested_by_auth_id bigint,
    processed_at timestamp with time zone,
    error_message text,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_platform_password_reset_jobs_status_effective
    ON public.platform_password_reset_jobs(status, effective_at);

CREATE INDEX IF NOT EXISTS idx_platform_password_reset_jobs_org
    ON public.platform_password_reset_jobs(organisation_id, created_at);
