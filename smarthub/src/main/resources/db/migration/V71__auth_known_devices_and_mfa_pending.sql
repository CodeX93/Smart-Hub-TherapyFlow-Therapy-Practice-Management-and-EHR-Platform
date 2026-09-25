-- Known devices for new-login alerts + pending MFA fields for change-method flow

CREATE TABLE IF NOT EXISTS auth_known_devices (
    id              BIGSERIAL PRIMARY KEY,
    auth_identity_id BIGINT NOT NULL REFERENCES auth_identities(id) ON DELETE CASCADE,
    fingerprint_hash VARCHAR(64) NOT NULL,
    device_label    VARCHAR(120) NOT NULL,
    user_agent      TEXT,
    last_ip         VARCHAR(50),
    first_seen_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_notified_at TIMESTAMPTZ,
    createdat       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updatedat       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT NOT NULL DEFAULT 0,
    updated_by      BIGINT NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uk_auth_known_devices_identity_fp UNIQUE (auth_identity_id, fingerprint_hash)
);

CREATE INDEX IF NOT EXISTS idx_auth_known_devices_identity
    ON auth_known_devices (auth_identity_id, last_seen_at DESC);

ALTER TABLE auth_mfa_credentials
    ADD COLUMN IF NOT EXISTS pending_mfa_method VARCHAR(20),
    ADD COLUMN IF NOT EXISTS pending_phone_e164 VARCHAR(20);
