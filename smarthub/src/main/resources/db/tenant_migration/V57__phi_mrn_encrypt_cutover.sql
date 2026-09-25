-- PHI Approach C cutover prep (pilot-gated like V56).
-- 1) Widen client_id for TFENC:v2 ciphertext
-- 2) Drop plaintext MRN uniqueness (ciphertext is non-deterministic)
-- 3) MRN year counters (no LIKE on ciphertext for generateClientId)
-- 4) UNIQUE on client_id_blind_idx (NULLs allowed until digests exist)

ALTER TABLE clients
    ALTER COLUMN client_id TYPE TEXT;

ALTER TABLE clients DROP CONSTRAINT IF EXISTS uq_clients_client_id;

CREATE TABLE IF NOT EXISTS client_mrn_counters (
    year       INTEGER PRIMARY KEY,
    next_value INTEGER NOT NULL
);

-- Unique digest for MRN search/uniqueness (multiple NULLs allowed in Postgres UNIQUE)
CREATE UNIQUE INDEX IF NOT EXISTS uq_clients_client_id_blind_idx
    ON clients (client_id_blind_idx)
    WHERE client_id_blind_idx IS NOT NULL;
