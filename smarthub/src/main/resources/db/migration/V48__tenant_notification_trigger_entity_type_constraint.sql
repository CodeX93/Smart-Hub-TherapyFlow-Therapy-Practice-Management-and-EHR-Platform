DO $$
DECLARE
    rec RECORD;
    entity_type_column text;
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
            EXECUTE format(
                'ALTER TABLE %I.notification_triggers DROP CONSTRAINT IF EXISTS notification_triggers_entity_type_check',
                rec.schema_name
            );

            entity_type_column := NULL;
            IF EXISTS (
                SELECT 1
                FROM information_schema.columns c
                WHERE c.table_schema = rec.schema_name
                  AND c.table_name = 'notification_triggers'
                  AND c.column_name = 'entity_type'
            ) THEN
                entity_type_column := 'entity_type';
            ELSIF EXISTS (
                SELECT 1
                FROM information_schema.columns c
                WHERE c.table_schema = rec.schema_name
                  AND c.table_name = 'notification_triggers'
                  AND c.column_name = 'entityType'
            ) THEN
                entity_type_column := '"entityType"';
            END IF;

            IF entity_type_column IS NOT NULL THEN
                EXECUTE format(
                    'ALTER TABLE %I.notification_triggers ADD CONSTRAINT notification_triggers_entity_type_check CHECK ((%s) IS NULL OR lower((%s)) IN (''general'',''client'',''session'',''task'',''checklist'',''billing'',''form'',''document'',''assessment'',''user'',''supervisor_assignment''))',
                    rec.schema_name,
                    entity_type_column,
                    entity_type_column
                );
            END IF;
        END IF;
    END LOOP;
END $$;
