-- TENANT
-- Per-therapist AI instruction templates for session note "Generate Final Note" flow.

CREATE TABLE IF NOT EXISTS session_note_ai_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    instructions TEXT NOT NULL,
    last_used_at TIMESTAMPTZ,
    createdat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updatedat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_session_note_ai_templates_owner
    ON session_note_ai_templates (created_by)
    WHERE is_deleted = FALSE;
