-- PLATFORM
-- Records the production identity cleanup inventory without storing credentials or identifiers.
-- Evidence key: production_seed_identity_cleanup

CREATE TABLE IF NOT EXISTS public.platform_migration_evidence (
    evidence_key VARCHAR(120) PRIMARY KEY,
    observed_count BIGINT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    details VARCHAR(500)
);
