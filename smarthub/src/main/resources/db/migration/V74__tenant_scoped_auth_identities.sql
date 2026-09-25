-- Tenant-scoped login uniqueness: first-class email/username on auth_identities,
-- one identity per organisation for tenant STAFF/CLIENT, DB partial unique indexes.

-- ---------------------------------------------------------------------------
-- 1. Columns
-- ---------------------------------------------------------------------------
ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS email CHARACTER VARYING(255);

ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS normalised_email CHARACTER VARYING(255);

ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS username CHARACTER VARYING(255);

ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS normalised_username CHARACTER VARYING(255);

COMMENT ON COLUMN public.auth_identities.email IS 'Login/contact email; unique per organisation for tenant identities';
COMMENT ON COLUMN public.auth_identities.username IS 'Staff login username; unique per organisation; NULL for CLIENT';
COMMENT ON COLUMN public.auth_identities.normalised_email IS 'Lowercased trimmed email for lookup/uniqueness';
COMMENT ON COLUMN public.auth_identities.normalised_username IS 'Lowercased trimmed username for lookup/uniqueness';

-- ---------------------------------------------------------------------------
-- 2. Collision report (ops visibility before unique indexes)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.auth_identity_uniqueness_collisions (
    id              BIGSERIAL PRIMARY KEY,
    organisation_id BIGINT,
    conflict_type   VARCHAR(40) NOT NULL,
    normalised_value VARCHAR(255) NOT NULL,
    auth_ids        BIGINT[] NOT NULL,
    detected_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes           TEXT
);

-- ---------------------------------------------------------------------------
-- 3. Initial backfill from login_identifier + organisation_id from membership
-- ---------------------------------------------------------------------------
UPDATE public.auth_identities ai
SET organisation_id = sub.org_id
FROM (
    SELECT uo.auth_id, MIN(uo.organisation_id) AS org_id
    FROM public.user_organisations uo
    GROUP BY uo.auth_id
    HAVING COUNT(*) = 1
) sub
WHERE ai.id = sub.auth_id
  AND ai.organisation_id IS NULL
  AND ai.identity_type IN ('STAFF', 'CLIENT');

-- STAFF: username <- login_identifier; email <- login if looks like email
UPDATE public.auth_identities
SET username = login_identifier,
    normalised_username = lower(trim(login_identifier)),
    email = CASE
        WHEN login_identifier LIKE '%@%' THEN login_identifier
        ELSE email
    END,
    normalised_email = CASE
        WHEN login_identifier LIKE '%@%' THEN lower(trim(login_identifier))
        ELSE normalised_email
    END
WHERE identity_type = 'STAFF'
  AND (username IS NULL OR normalised_username IS NULL);

-- CLIENT / others with email-like login: email <- login_identifier
UPDATE public.auth_identities
SET email = COALESCE(email, login_identifier),
    normalised_email = COALESCE(normalised_email, lower(trim(login_identifier))),
    username = NULL,
    normalised_username = NULL
WHERE identity_type = 'CLIENT';

-- Platform identities (no org): treat login as email when email-shaped
UPDATE public.auth_identities
SET email = COALESCE(email, login_identifier),
    normalised_email = COALESCE(normalised_email, lower(trim(login_identifier))),
    username = COALESCE(username, CASE WHEN login_identifier NOT LIKE '%@%' THEN login_identifier ELSE NULL END),
    normalised_username = COALESCE(
        normalised_username,
        CASE WHEN login_identifier NOT LIKE '%@%' THEN lower(trim(login_identifier)) ELSE NULL END
    )
WHERE organisation_id IS NULL
  AND identity_type NOT IN ('CLIENT');

-- ---------------------------------------------------------------------------
-- 4. Split multi-org identities (one auth_id -> many user_organisations)
--    Keep the lowest organisation_id on the original row; clone for the rest.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    rec RECORD;
    clone_org RECORD;
    new_auth_id BIGINT;
    keep_org_id BIGINT;
BEGIN
    FOR rec IN
        SELECT uo.auth_id
        FROM public.user_organisations uo
        GROUP BY uo.auth_id
        HAVING COUNT(*) > 1
    LOOP
        SELECT MIN(organisation_id) INTO keep_org_id
        FROM public.user_organisations
        WHERE auth_id = rec.auth_id;

        UPDATE public.auth_identities
        SET organisation_id = keep_org_id
        WHERE id = rec.auth_id
          AND (organisation_id IS NULL OR organisation_id <> keep_org_id);

        FOR clone_org IN
            SELECT organisation_id AS org_id
            FROM public.user_organisations
            WHERE auth_id = rec.auth_id
              AND organisation_id <> keep_org_id
        LOOP
            INSERT INTO public.auth_identities (
                account_locked, auth_provider, created_at, created_by,
                email_verification_expiry, email_verification_token, email_verified,
                failed_login_attempts, identity_type, is_active,
                last_failed_login, last_password_change_by, last_successful_login,
                locked_reason, locked_until,
                login_identifier, login_identifier_changed_at, normalised_login_identifier,
                password_changed_at, password_hash, password_reset_expiry, password_reset_token,
                updated_at, version, provider_user_id, sso_enabled, organisation_id,
                updated_by, is_deleted, deleted_at, full_name, phone, must_change_password,
                email, normalised_email, username, normalised_username
            )
            SELECT
                account_locked, auth_provider, NOW(), created_by,
                email_verification_expiry, email_verification_token, email_verified,
                failed_login_attempts, identity_type, is_active,
                last_failed_login, last_password_change_by, last_successful_login,
                locked_reason, locked_until,
                login_identifier, login_identifier_changed_at, normalised_login_identifier,
                password_changed_at, password_hash, password_reset_expiry, password_reset_token,
                NOW(), 0, provider_user_id, sso_enabled, clone_org.org_id,
                updated_by, is_deleted, deleted_at, full_name, phone, COALESCE(must_change_password, FALSE),
                email, normalised_email, username, normalised_username
            FROM public.auth_identities
            WHERE id = rec.auth_id
            RETURNING id INTO new_auth_id;

            UPDATE public.user_organisations
            SET auth_id = new_auth_id
            WHERE auth_id = rec.auth_id
              AND organisation_id = clone_org.org_id;

            UPDATE public.auth_identity_roles
            SET auth_id = new_auth_id
            WHERE auth_id = rec.auth_id
              AND organisation_id = clone_org.org_id;
        END LOOP;
    END LOOP;
END $$;

-- Ensure every STAFF/CLIENT with a single membership has organisation_id set
UPDATE public.auth_identities ai
SET organisation_id = uo.organisation_id
FROM public.user_organisations uo
WHERE ai.id = uo.auth_id
  AND ai.organisation_id IS NULL
  AND ai.identity_type IN ('STAFF', 'CLIENT');

-- ---------------------------------------------------------------------------
-- 5. Backfill email from tenant users.email (dynamic schemas)
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    org RECORD;
    sql TEXT;
BEGIN
    FOR org IN
        SELECT id, schema_name
        FROM public.organisations
        WHERE schema_name IS NOT NULL
          AND schema_name <> 'public'
          AND EXISTS (
              SELECT 1
              FROM information_schema.schemata s
              WHERE s.schema_name = organisations.schema_name
          )
    LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables t
            WHERE t.table_schema = org.schema_name
              AND t.table_name = 'users'
        ) THEN
            sql := format(
                'UPDATE public.auth_identities ai
                 SET email = u.email,
                     normalised_email = lower(trim(u.email))
                 FROM %I.users u
                 WHERE u.auth_id = ai.id
                   AND u.email IS NOT NULL
                   AND trim(u.email) <> ''''
                   AND ai.organisation_id = %s
                   AND (ai.email IS NULL OR ai.normalised_email IS NULL
                        OR ai.normalised_email = lower(trim(ai.login_identifier)))',
                org.schema_name, org.id
            );
            EXECUTE sql;
        END IF;

        -- Point tenant users at cloned auth ids if membership was remapped (already handled via auth_id on uo;
        -- users.auth_id still references original — remap by organisation + login match best-effort)
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables t
            WHERE t.table_schema = org.schema_name
              AND t.table_name = 'users'
        ) THEN
            sql := format(
                'UPDATE %I.users u
                 SET auth_id = ai.id
                 FROM public.auth_identities ai
                 WHERE ai.organisation_id = %s
                   AND ai.identity_type = ''STAFF''
                   AND ai.normalised_email IS NOT NULL
                   AND lower(trim(u.email)) = ai.normalised_email
                   AND u.auth_id IS DISTINCT FROM ai.id
                   AND NOT EXISTS (
                       SELECT 1 FROM %I.users u2 WHERE u2.auth_id = ai.id AND u2.id <> u.id
                   )',
                org.schema_name, org.id, org.schema_name
            );
            BEGIN
                EXECUTE sql;
            EXCEPTION WHEN OTHERS THEN
                RAISE NOTICE 'Tenant users auth_id remap skipped for %: %', org.schema_name, SQLERRM;
            END;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.tables t
            WHERE t.table_schema = org.schema_name
              AND t.table_name = 'clients'
        ) THEN
            sql := format(
                'UPDATE %I.clients c
                 SET auth_id = ai.id
                 FROM public.auth_identities ai
                 WHERE ai.organisation_id = %s
                   AND ai.identity_type = ''CLIENT''
                   AND ai.normalised_email IS NOT NULL
                   AND c.auth_id IS NOT NULL
                   AND EXISTS (
                       SELECT 1 FROM public.auth_identities old
                       WHERE old.id = c.auth_id
                         AND old.normalised_login_identifier = ai.normalised_login_identifier
                         AND old.id <> ai.id
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM %I.clients c2 WHERE c2.auth_id = ai.id AND c2.id <> c.id
                   )',
                org.schema_name, org.id, org.schema_name
            );
            BEGIN
                EXECUTE sql;
            EXCEPTION WHEN OTHERS THEN
                RAISE NOTICE 'Tenant clients auth_id remap skipped for %: %', org.schema_name, SQLERRM;
            END;
        END IF;
    END LOOP;
END $$;

-- Ensure STAFF still missing email get a synthetic unique email from username+org
UPDATE public.auth_identities
SET email = COALESCE(email, username || '@org-' || organisation_id || '.local'),
    normalised_email = COALESCE(normalised_email, lower(trim(COALESCE(username, login_identifier))) || '@org-' || organisation_id || '.local')
WHERE identity_type = 'STAFF'
  AND organisation_id IS NOT NULL
  AND (email IS NULL OR normalised_email IS NULL);

-- Sync login_identifier display alias: prefer username for STAFF, email for CLIENT
UPDATE public.auth_identities
SET login_identifier = COALESCE(username, email, login_identifier),
    normalised_login_identifier = lower(trim(COALESCE(username, email, login_identifier)))
WHERE identity_type = 'STAFF'
  AND organisation_id IS NOT NULL;

UPDATE public.auth_identities
SET login_identifier = COALESCE(email, login_identifier),
    normalised_login_identifier = lower(trim(COALESCE(email, login_identifier)))
WHERE identity_type = 'CLIENT';

-- ---------------------------------------------------------------------------
-- 6. Record collisions (do not auto-resolve)
-- ---------------------------------------------------------------------------
INSERT INTO public.auth_identity_uniqueness_collisions (organisation_id, conflict_type, normalised_value, auth_ids, notes)
SELECT organisation_id,
       'EMAIL',
       normalised_email,
       array_agg(id ORDER BY id),
       'Duplicate normalised_email within organisation'
FROM public.auth_identities
WHERE organisation_id IS NOT NULL
  AND normalised_email IS NOT NULL
GROUP BY organisation_id, normalised_email
HAVING COUNT(*) > 1;

INSERT INTO public.auth_identity_uniqueness_collisions (organisation_id, conflict_type, normalised_value, auth_ids, notes)
SELECT organisation_id,
       'USERNAME',
       normalised_username,
       array_agg(id ORDER BY id),
       'Duplicate normalised_username within organisation'
FROM public.auth_identities
WHERE organisation_id IS NOT NULL
  AND normalised_username IS NOT NULL
GROUP BY organisation_id, normalised_username
HAVING COUNT(*) > 1;

INSERT INTO public.auth_identity_uniqueness_collisions (organisation_id, conflict_type, normalised_value, auth_ids, notes)
SELECT NULL,
       'PLATFORM_EMAIL',
       normalised_email,
       array_agg(id ORDER BY id),
       'Duplicate normalised_email among platform (org-null) identities'
FROM public.auth_identities
WHERE organisation_id IS NULL
  AND normalised_email IS NOT NULL
GROUP BY normalised_email
HAVING COUNT(*) > 1;

-- Disambiguate colliding rows so unique indexes can be applied (ops should review collision table).
-- Keep the lowest id unchanged; suffix others.
WITH ranked AS (
    SELECT id,
           organisation_id,
           normalised_email,
           ROW_NUMBER() OVER (PARTITION BY organisation_id, normalised_email ORDER BY id) AS rn
    FROM public.auth_identities
    WHERE organisation_id IS NOT NULL
      AND normalised_email IS NOT NULL
)
UPDATE public.auth_identities ai
SET normalised_email = ai.normalised_email || '#dup-' || ai.id,
    email = COALESCE(ai.email, ai.normalised_email) || '#dup-' || ai.id
FROM ranked r
WHERE ai.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id,
           organisation_id,
           normalised_username,
           ROW_NUMBER() OVER (PARTITION BY organisation_id, normalised_username ORDER BY id) AS rn
    FROM public.auth_identities
    WHERE organisation_id IS NOT NULL
      AND normalised_username IS NOT NULL
)
UPDATE public.auth_identities ai
SET normalised_username = ai.normalised_username || '#dup-' || ai.id,
    username = COALESCE(ai.username, ai.normalised_username) || '#dup-' || ai.id
FROM ranked r
WHERE ai.id = r.id
  AND r.rn > 1;

WITH ranked AS (
    SELECT id,
           normalised_email,
           ROW_NUMBER() OVER (PARTITION BY normalised_email ORDER BY id) AS rn
    FROM public.auth_identities
    WHERE organisation_id IS NULL
      AND normalised_email IS NOT NULL
)
UPDATE public.auth_identities ai
SET normalised_email = ai.normalised_email || '#dup-' || ai.id,
    email = COALESCE(ai.email, ai.normalised_email) || '#dup-' || ai.id
FROM ranked r
WHERE ai.id = r.id
  AND r.rn > 1;

-- ---------------------------------------------------------------------------
-- 7. Check constraints + unique indexes
-- ---------------------------------------------------------------------------
ALTER TABLE public.auth_identities
    DROP CONSTRAINT IF EXISTS chk_auth_client_no_username;
ALTER TABLE public.auth_identities
    ADD CONSTRAINT chk_auth_client_no_username
    CHECK (identity_type <> 'CLIENT' OR (username IS NULL AND normalised_username IS NULL));

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_identities_org_email
    ON public.auth_identities (organisation_id, normalised_email)
    WHERE organisation_id IS NOT NULL AND normalised_email IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_identities_org_username
    ON public.auth_identities (organisation_id, normalised_username)
    WHERE organisation_id IS NOT NULL AND normalised_username IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_identities_platform_email
    ON public.auth_identities (normalised_email)
    WHERE organisation_id IS NULL AND normalised_email IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_identities_platform_username
    ON public.auth_identities (normalised_username)
    WHERE organisation_id IS NULL AND normalised_username IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_identities_normalised_email
    ON public.auth_identities (normalised_email)
    WHERE normalised_email IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auth_identities_normalised_username
    ON public.auth_identities (normalised_username)
    WHERE normalised_username IS NOT NULL;
