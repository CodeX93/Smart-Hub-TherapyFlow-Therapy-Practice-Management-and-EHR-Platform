-- V59 used pgcrypto digest(), but Azure Flexible Server installs extensions once
-- into a single schema. CREATE EXTENSION IF NOT EXISTS then no-ops for other
-- tenants, so audit inserts fail with: function digest(bytea, unknown) does not exist.
-- Use PostgreSQL built-in sha256() (PG11+) — same approach as V50's md5() choice.

CREATE OR REPLACE FUNCTION audit_logs_chain_insert()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    chain_hash varchar(64);
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext(current_schema()), hashtext('audit_logs'));

    SELECT entry_hash
      INTO chain_hash
      FROM audit_logs
     ORDER BY id DESC
     LIMIT 1;

    -- Genesis for empty tables uses 64 zeroes (SHA-256 width); existing MD5 chains
    -- continue from the last entry_hash value regardless of prior algorithm.
    chain_hash := coalesce(chain_hash, repeat('0', 64));
    NEW.previous_hash := chain_hash;
    NEW.hash_algorithm := 'SHA-256';
    NEW.integrity_version := 2;
    NEW.entry_hash := encode(
        sha256(
            convert_to(
                concat_ws('|',
                    chain_hash,
                    NEW.id::text,
                    coalesce(extract(epoch FROM NEW.timestamp)::text, ''),
                    coalesce(NEW.action, ''),
                    coalesce(NEW.result, ''),
                    coalesce(NEW.resource_type, ''),
                    coalesce(NEW.resource_id, ''),
                    coalesce(NEW.user_id::text, ''),
                    coalesce(NEW.client_id::text, ''),
                    coalesce(NEW.hipaa_relevant::text, ''),
                    coalesce(NEW.details, '')
                ),
                'UTF8'
            )
        ),
        'hex'
    );
    RETURN NEW;
END
$$;

COMMENT ON TABLE audit_logs IS
    'Append-only audit trail. Legacy rows: MD5 (integrity_version=1). New rows: SHA-256 via built-in sha256() (integrity_version=2).';
