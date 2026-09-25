ALTER TABLE public.auth_sessions
    ADD COLUMN IF NOT EXISTS impersonation_session_id bigint;

CREATE INDEX IF NOT EXISTS idx_auth_sessions_impersonation_session_id
    ON public.auth_sessions (impersonation_session_id);

ALTER TABLE public.auth_sessions
    DROP CONSTRAINT IF EXISTS fk_auth_sessions_impersonation_session;

ALTER TABLE public.auth_sessions
    ADD CONSTRAINT fk_auth_sessions_impersonation_session
    FOREIGN KEY (impersonation_session_id)
    REFERENCES public.platform_impersonation_sessions(id)
    ON DELETE SET NULL;
