-- TENANT
-- AI client report templates, supporting files, and generated client reports.

CREATE TABLE IF NOT EXISTS report_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    ai_instructions TEXT,
    original_name VARCHAR(500),
    mime_type VARCHAR(150),
    file_size INTEGER,
    file_blob_name VARCHAR(1000),
    file_url TEXT,
    structure_text TEXT,
    default_include_profile BOOLEAN NOT NULL DEFAULT TRUE,
    default_include_notes BOOLEAN NOT NULL DEFAULT TRUE,
    default_include_assessments BOOLEAN NOT NULL DEFAULT TRUE,
    supporting_files_guidance TEXT,
    supporting_files_expected BOOLEAN NOT NULL DEFAULT FALSE,
    supporting_file_types TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    createdat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updatedat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_by BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_report_templates_active ON report_templates (is_active);

CREATE TABLE IF NOT EXISTS report_supporting_files (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    original_name VARCHAR(500) NOT NULL,
    mime_type VARCHAR(150) NOT NULL,
    file_size INTEGER NOT NULL,
    file_blob_name VARCHAR(1000),
    file_url TEXT,
    document_type VARCHAR(150),
    extracted_text TEXT,
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    createdat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updatedat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_by BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_report_supporting_files_client ON report_supporting_files (client_id);

CREATE TABLE IF NOT EXISTS client_reports (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    template_id BIGINT REFERENCES report_templates(id) ON DELETE SET NULL,
    template_name VARCHAR(255),
    generated_content TEXT,
    draft_content TEXT,
    final_content TEXT,
    is_draft BOOLEAN NOT NULL DEFAULT TRUE,
    is_finalized BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMPTZ,
    edited_at TIMESTAMPTZ,
    finalized_at TIMESTAMPTZ,
    created_by_id BIGINT NOT NULL REFERENCES users(id),
    finalized_by_id BIGINT REFERENCES users(id),
    createdat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updatedat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_by BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_client_reports_client ON client_reports (client_id);
CREATE INDEX IF NOT EXISTS idx_client_reports_status ON client_reports (is_draft, is_finalized);
