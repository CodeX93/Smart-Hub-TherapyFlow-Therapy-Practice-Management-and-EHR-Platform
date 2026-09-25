ALTER TABLE public.auth_sessions
    ADD COLUMN IF NOT EXISTS last_activity_at timestamp with time zone,
    ADD COLUMN IF NOT EXISTS idle_expires_at timestamp with time zone;

UPDATE public.auth_sessions
   SET last_activity_at = coalesce(last_activity_at, now()),
       idle_expires_at = coalesce(
           idle_expires_at,
           least(expires_at, now() + interval '15 minutes')
       )
 WHERE last_activity_at IS NULL
    OR idle_expires_at IS NULL;

ALTER TABLE public.auth_sessions
    ALTER COLUMN last_activity_at SET NOT NULL,
    ALTER COLUMN idle_expires_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_sessions_idle_expires
    ON public.auth_sessions (idle_expires_at);
