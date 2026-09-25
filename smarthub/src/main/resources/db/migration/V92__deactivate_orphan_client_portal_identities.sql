-- PLATFORM
-- Deleted clients kept an active CLIENT auth identity: the delete path only soft-deleted the
-- client row, so the login stayed usable and the identity kept counting toward the super admin
-- "Total End Users" KPI. The code now deactivates the identity on delete / portal revoke; this
-- backfills the rows that were left behind.
DO $$
DECLARE
    rec RECORD;
    deactivated BIGINT;
    total BIGINT := 0;
BEGIN
    FOR rec IN
        SELECT DISTINCT o.schema_name
        FROM public.organisations o
        WHERE o.schema_name IS NOT NULL
          AND btrim(o.schema_name) <> ''
          AND lower(o.schema_name) <> 'public'
    LOOP
        IF to_regclass(format('%I.clients', rec.schema_name)) IS NULL THEN
            CONTINUE;
        END IF;

        EXECUTE format($f$
            UPDATE public.auth_identities ai
            SET is_active = false,
                updated_at = now()
            FROM %I.clients c
            WHERE c.auth_id = ai.id
              AND COALESCE(c.is_deleted, false) = true
              AND ai.is_active = true
              AND UPPER(ai.identity_type) = 'CLIENT'
        $f$, rec.schema_name);

        GET DIAGNOSTICS deactivated = ROW_COUNT;
        total := total + deactivated;

        IF deactivated > 0 THEN
            RAISE NOTICE 'Deactivated % orphan portal identity(ies) in schema %', deactivated, rec.schema_name;
        END IF;
    END LOOP;

    RAISE NOTICE 'Deactivated % orphan portal identity(ies) in total', total;
END
$$;