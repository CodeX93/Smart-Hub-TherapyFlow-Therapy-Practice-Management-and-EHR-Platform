CREATE TABLE IF NOT EXISTS platform_api_key_scopes (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT NOT NULL REFERENCES platform_api_keys(id) ON DELETE CASCADE,
    scope VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_platform_api_key_scopes_key_scope UNIQUE (api_key_id, scope)
);

CREATE INDEX IF NOT EXISTS idx_platform_api_key_scopes_key
    ON platform_api_key_scopes(api_key_id);

CREATE INDEX IF NOT EXISTS idx_platform_api_key_scopes_scope
    ON platform_api_key_scopes(scope);

