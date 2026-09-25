-- Hash chain uses PostgreSQL built-in md5() so tenant schemas do not depend on
-- pgcrypto being installed into the tenant search_path (Azure Flexible Server).
-- hash_algorithm is recorded as MD5 for honesty; a later migration can rehash to SHA-256.

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS previous_hash varchar(64),
    ADD COLUMN IF NOT EXISTS entry_hash varchar(64),
    ADD COLUMN IF NOT EXISTS hash_algorithm varchar(20) NOT NULL DEFAULT 'MD5',
    ADD COLUMN IF NOT EXISTS integrity_version integer NOT NULL DEFAULT 1;

DO $$
DECLARE
    audit_row record;
    chain_hash varchar(64) := repeat('0', 32);
BEGIN
    FOR audit_row IN
        SELECT id,
               timestamp,
               action,
               result,
               resource_type,
               resource_id,
               user_id,
               client_id,
               hipaa_relevant,
               details
          FROM audit_logs
         ORDER BY id
    LOOP
        UPDATE audit_logs
           SET previous_hash = chain_hash,
               hash_algorithm = 'MD5',
               entry_hash = md5(
                   concat_ws('|',
                       chain_hash,
                       audit_row.id::text,
                       coalesce(extract(epoch FROM audit_row.timestamp)::text, ''),
                       coalesce(audit_row.action, ''),
                       coalesce(audit_row.result, ''),
                       coalesce(audit_row.resource_type, ''),
                       coalesce(audit_row.resource_id, ''),
                       coalesce(audit_row.user_id::text, ''),
                       coalesce(audit_row.client_id::text, ''),
                       coalesce(audit_row.hipaa_relevant::text, ''),
                       coalesce(audit_row.details, '')
                   )
               )
         WHERE id = audit_row.id
         RETURNING entry_hash INTO chain_hash;
    END LOOP;
END
$$;

ALTER TABLE audit_logs
    ALTER COLUMN previous_hash SET NOT NULL,
    ALTER COLUMN entry_hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_logs_entry_hash
    ON audit_logs (entry_hash);

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

    chain_hash := coalesce(chain_hash, repeat('0', 32));
    NEW.previous_hash := chain_hash;
    NEW.hash_algorithm := 'MD5';
    NEW.integrity_version := 1;
    NEW.entry_hash := md5(
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
        )
    );
    RETURN NEW;
END
$$;

CREATE OR REPLACE FUNCTION audit_logs_reject_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only'
        USING ERRCODE = '55000';
END
$$;

DROP TRIGGER IF EXISTS trg_audit_logs_chain_insert ON audit_logs;
CREATE TRIGGER trg_audit_logs_chain_insert
    BEFORE INSERT ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION audit_logs_chain_insert();

DROP TRIGGER IF EXISTS trg_audit_logs_reject_mutation ON audit_logs;
CREATE TRIGGER trg_audit_logs_reject_mutation
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION audit_logs_reject_mutation();

COMMENT ON TABLE audit_logs IS
    'Append-only audit trail protected by an MD5 hash chain and database mutation guards.';
