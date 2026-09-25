-- Trusted-device MFA skip + stay-signed-in support (HIPAA-aligned: finite trust window, revocable)

ALTER TABLE auth_known_devices
    ADD COLUMN IF NOT EXISTS trusted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS trusted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS trust_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS trust_revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS trust_token_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_known_devices_trust_token_hash
    ON auth_known_devices (trust_token_hash)
    WHERE trust_token_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_known_devices_trusted_active
    ON auth_known_devices (auth_identity_id, trust_expires_at)
    WHERE trusted = TRUE AND trust_revoked_at IS NULL;
