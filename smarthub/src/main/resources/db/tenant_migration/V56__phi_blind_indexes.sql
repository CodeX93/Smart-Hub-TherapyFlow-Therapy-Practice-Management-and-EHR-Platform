-- Approach C Wave 2 foundations: blind-index columns for exact-match search.
-- Name-token digests for first/last-name search are added in V64 (client_name_blind_indexes).
-- UNIQUE on client_id_blind_idx is deferred until cutover after backfill.
--
-- Rollout gate (see tenant.migration.gated-from-version / gate-allow-slugs):
--   - New tenants: applied on first provision (full Flyway to latest).
--   - Existing tenants: pilot allow-list only (default: mindcare) until full rollout.
-- Applied manually to MindCare (tenant_53) on 2026-07-23 for pilot testing.

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS client_id_blind_idx BYTEA,
    ADD COLUMN IF NOT EXISTS full_name_blind_idx BYTEA;

ALTER TABLE client_contacts
    ADD COLUMN IF NOT EXISTS contact_blind_idx BYTEA;

CREATE INDEX IF NOT EXISTS idx_clients_client_id_blind
    ON clients (client_id_blind_idx);

CREATE INDEX IF NOT EXISTS idx_clients_full_name_blind
    ON clients (full_name_blind_idx);

CREATE INDEX IF NOT EXISTS idx_client_contacts_contact_blind
    ON client_contacts (contact_blind_idx);
