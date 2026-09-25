ALTER TABLE public.auth_mfa_credentials
    ADD COLUMN IF NOT EXISTS mfa_method varchar(20) NOT NULL DEFAULT 'TOTP',
    ADD COLUMN IF NOT EXISTS phone_e164 varchar(20);

CREATE TABLE IF NOT EXISTS public.auth_mfa_otp_challenges (
    id bigserial PRIMARY KEY,
    auth_identity_id bigint NOT NULL,
    jti_hash char(64) NOT NULL,
    purpose varchar(20) NOT NULL,
    channel varchar(20) NOT NULL,
    code_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    last_sent_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    created_by bigint NOT NULL DEFAULT 0,
    updated_by bigint NOT NULL DEFAULT 0,
    version bigint NOT NULL DEFAULT 0,
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamptz,
    CONSTRAINT fk_auth_mfa_otp_identity
        FOREIGN KEY (auth_identity_id) REFERENCES public.auth_identities(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_mfa_otp_jti_purpose
    ON public.auth_mfa_otp_challenges (jti_hash, purpose);

CREATE INDEX IF NOT EXISTS idx_auth_mfa_otp_expiry
    ON public.auth_mfa_otp_challenges (auth_identity_id, expires_at, consumed_at);
