CREATE TABLE IF NOT EXISTS public.auth_mfa_credentials (
    id bigserial PRIMARY KEY,
    auth_identity_id bigint NOT NULL UNIQUE,
    encrypted_secret text,
    encrypted_pending_secret text,
    enabled boolean NOT NULL DEFAULT false,
    confirmed_at timestamptz,
    last_accepted_time_step bigint,
    failed_attempts integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    version bigint NOT NULL DEFAULT 0,
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamptz,
    CONSTRAINT fk_auth_mfa_credentials_identity
        FOREIGN KEY (auth_identity_id) REFERENCES public.auth_identities(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auth_mfa_credentials_lock
    ON public.auth_mfa_credentials (auth_identity_id, locked_until);

CREATE TABLE IF NOT EXISTS public.auth_mfa_recovery_codes (
    id bigserial PRIMARY KEY,
    credential_id bigint NOT NULL,
    code_hash varchar(255) NOT NULL,
    used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    version bigint NOT NULL DEFAULT 0,
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamptz,
    CONSTRAINT fk_auth_mfa_recovery_credential
        FOREIGN KEY (credential_id) REFERENCES public.auth_mfa_credentials(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auth_mfa_recovery_unused
    ON public.auth_mfa_recovery_codes (credential_id, used_at);

CREATE TABLE IF NOT EXISTS public.auth_mfa_login_challenges (
    id bigserial PRIMARY KEY,
    auth_identity_id bigint NOT NULL,
    jti_hash char(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    version bigint NOT NULL DEFAULT 0,
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamptz,
    CONSTRAINT fk_auth_mfa_challenge_identity
        FOREIGN KEY (auth_identity_id) REFERENCES public.auth_identities(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auth_mfa_challenge_expiry
    ON public.auth_mfa_login_challenges (auth_identity_id, expires_at, consumed_at);
