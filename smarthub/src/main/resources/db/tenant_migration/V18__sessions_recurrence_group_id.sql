-- TENANT
-- Skip when sessions table is not provisioned yet (fresh/partial tenant schemas).

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'sessions'
    ) THEN
        ALTER TABLE sessions
            ADD COLUMN IF NOT EXISTS recurrence_group_id VARCHAR(64);

        CREATE INDEX IF NOT EXISTS sessions_recurrence_group_id_idx
            ON sessions (recurrence_group_id);
    END IF;
END $$;
