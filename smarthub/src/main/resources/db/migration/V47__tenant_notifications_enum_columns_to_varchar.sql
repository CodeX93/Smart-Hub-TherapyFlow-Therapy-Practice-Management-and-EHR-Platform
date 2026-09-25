DO $$
DECLARE
    rec RECORD;
    con_rec RECORD;
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
              AND t.table_name = 'notifications'
        ) THEN
            -- Drop legacy numeric CHECK constraints (e.g., type >= 0) before
            -- converting enum-ordinal columns to VARCHAR.
            FOR con_rec IN
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_class rel ON rel.oid = c.conrelid
                JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
                WHERE c.contype = 'c'
                  AND nsp.nspname = rec.schema_name
                  AND rel.relname = 'notifications'
                  AND (
                    pg_get_constraintdef(c.oid) ~* '("type"|type)\s*(>=|<=|>|<)\s*[0-9]+'
                    OR pg_get_constraintdef(c.oid) ~* '[0-9]+\s*(>=|<=|>|<)\s*("type"|type)'
                    OR pg_get_constraintdef(c.oid) ~* '("category"|category)\s*(>=|<=|>|<)\s*[0-9]+'
                    OR pg_get_constraintdef(c.oid) ~* '[0-9]+\s*(>=|<=|>|<)\s*("category"|category)'
                    OR pg_get_constraintdef(c.oid) ~* '("priority"|priority)\s*(>=|<=|>|<)\s*[0-9]+'
                    OR pg_get_constraintdef(c.oid) ~* '[0-9]+\s*(>=|<=|>|<)\s*("priority"|priority)'
                  )
            LOOP
                EXECUTE format(
                    'ALTER TABLE %I.notifications DROP CONSTRAINT IF EXISTS %I',
                    rec.schema_name,
                    con_rec.conname
                );
            END LOOP;

            EXECUTE format($f$
                ALTER TABLE %I.notifications
                ALTER COLUMN type TYPE VARCHAR(50)
                USING (
                    CASE
                        WHEN "type" IS NULL THEN 'SYSTEM_MAINTENANCE'
                        WHEN "type"::text ~ '^[0-9]+$' THEN
                            CASE ("type"::text)::int
                                WHEN 0 THEN 'APPOINTMENT_REMINDER'
                                WHEN 1 THEN 'APPOINTMENT_CONFIRMED'
                                WHEN 2 THEN 'APPOINTMENT_CANCELLED'
                                WHEN 3 THEN 'APPOINTMENT_RESCHEDULED'
                                WHEN 4 THEN 'APPOINTMENT_24H_REMINDER'
                                WHEN 5 THEN 'APPOINTMENT_1H_REMINDER'
                                WHEN 6 THEN 'FORM_ASSIGNED'
                                WHEN 7 THEN 'FORM_DUE_SOON'
                                WHEN 8 THEN 'FORM_OVERDUE'
                                WHEN 9 THEN 'FORM_SUBMITTED'
                                WHEN 10 THEN 'FORM_REVIEWED'
                                WHEN 11 THEN 'DOCUMENT_SHARED'
                                WHEN 12 THEN 'DOCUMENT_UPDATED'
                                WHEN 13 THEN 'SESSION_NOTES_AVAILABLE'
                                WHEN 14 THEN 'PROGRESS_REPORT_AVAILABLE'
                                WHEN 15 THEN 'PAYMENT_DUE'
                                WHEN 16 THEN 'PAYMENT_OVERDUE'
                                WHEN 17 THEN 'PAYMENT_RECEIVED'
                                WHEN 18 THEN 'PAYMENT_FAILED'
                                WHEN 19 THEN 'INVOICE_GENERATED'
                                WHEN 20 THEN 'PORTAL_ACCESS_GRANTED'
                                WHEN 21 THEN 'PASSWORD_RESET_REQUESTED'
                                WHEN 22 THEN 'PASSWORD_CHANGED'
                                WHEN 23 THEN 'ACCOUNT_LOCKED'
                                WHEN 24 THEN 'ACCOUNT_UNLOCKED'
                                WHEN 25 THEN 'NEW_MESSAGE'
                                WHEN 26 THEN 'MESSAGE_REPLY'
                                WHEN 27 THEN 'CRISIS_RESOURCES_SHARED'
                                WHEN 28 THEN 'EMERGENCY_CONTACT_UPDATED'
                                WHEN 29 THEN 'SYSTEM_MAINTENANCE'
                                WHEN 30 THEN 'SYSTEM_UPGRADE'
                                WHEN 31 THEN 'POLICY_UPDATE'
                                WHEN 32 THEN 'INSURANCE_VERIFICATION_NEEDED'
                                WHEN 33 THEN 'INSURANCE_AUTHORIZATION_EXPIRING'
                                WHEN 34 THEN 'INSURANCE_CLAIM_PROCESSED'
                                ELSE 'SYSTEM_MAINTENANCE'
                            END
                        ELSE "type"::text
                    END
                )
            $f$, rec.schema_name);

            EXECUTE format($f$
                ALTER TABLE %I.notifications
                ALTER COLUMN category TYPE VARCHAR(50)
                USING (
                    CASE
                        WHEN category IS NULL THEN 'SYSTEM'
                        WHEN category::text ~ '^[0-9]+$' THEN
                            CASE (category::text)::int
                                WHEN 0 THEN 'APPOINTMENT'
                                WHEN 1 THEN 'FORM'
                                WHEN 2 THEN 'DOCUMENT'
                                WHEN 3 THEN 'SESSION'
                                WHEN 4 THEN 'BILLING'
                                WHEN 5 THEN 'ACCOUNT'
                                WHEN 6 THEN 'SECURITY'
                                WHEN 7 THEN 'MESSAGE'
                                WHEN 8 THEN 'EMERGENCY'
                                WHEN 9 THEN 'SYSTEM'
                                WHEN 10 THEN 'INSURANCE'
                                ELSE 'SYSTEM'
                            END
                        ELSE category::text
                    END
                )
            $f$, rec.schema_name);

            EXECUTE format($f$
                ALTER TABLE %I.notifications
                ALTER COLUMN priority TYPE VARCHAR(20)
                USING (
                    CASE
                        WHEN priority IS NULL THEN 'MEDIUM'
                        WHEN priority::text ~ '^[0-9]+$' THEN
                            CASE (priority::text)::int
                                WHEN 0 THEN 'LOW'
                                WHEN 1 THEN 'MEDIUM'
                                WHEN 2 THEN 'HIGH'
                                WHEN 3 THEN 'URGENT'
                                WHEN 4 THEN 'URGENT'
                                ELSE 'MEDIUM'
                            END
                        ELSE priority::text
                    END
                )
            $f$, rec.schema_name);
        END IF;
    END LOOP;
END $$;
