DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN
        SELECT DISTINCT o.schema_name
        FROM public.organisations o
        WHERE o.schema_name IS NOT NULL
          AND btrim(o.schema_name) <> ''
          AND lower(o.schema_name) <> 'public'
    LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables t
            WHERE t.table_schema = rec.schema_name
              AND t.table_name = 'notification_triggers'
        ) THEN
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns c
                WHERE c.table_schema = rec.schema_name
                  AND c.table_name = 'notification_triggers'
                  AND c.column_name = 'entityType'
            ) THEN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.columns c
                    WHERE c.table_schema = rec.schema_name
                      AND c.table_name = 'notification_triggers'
                      AND c.column_name = 'entity_type'
                ) THEN
                    EXECUTE format(
                        'UPDATE %I.notification_triggers SET entity_type = COALESCE(entity_type, "entityType")',
                        rec.schema_name
                    );

                    EXECUTE format(
                        'ALTER TABLE %I.notification_triggers DROP COLUMN "entityType"',
                        rec.schema_name
                    );
                ELSE
                    EXECUTE format(
                        'ALTER TABLE %I.notification_triggers RENAME COLUMN "entityType" TO entity_type',
                        rec.schema_name
                    );
                END IF;
            END IF;
        END IF;
    END LOOP;
END $$;
