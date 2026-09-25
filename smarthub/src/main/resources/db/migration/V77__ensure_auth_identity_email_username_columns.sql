-- Ensure tenant-scoped auth identity columns exist.
-- V74 may be recorded as applied without having created these columns (checksum drift / partial apply).

ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS email CHARACTER VARYING(255);

ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS normalised_email CHARACTER VARYING(255);

ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS username CHARACTER VARYING(255);

ALTER TABLE public.auth_identities
    ADD COLUMN IF NOT EXISTS normalised_username CHARACTER VARYING(255);

CREATE TABLE IF NOT EXISTS public.auth_identity_uniqueness_collisions (
    id               BIGSERIAL PRIMARY KEY,
    organisation_id  BIGINT,
    conflict_type    VARCHAR(40) NOT NULL,
    normalised_value VARCHAR(255) NOT NULL,
    auth_ids         BIGINT[] NOT NULL,
    detected_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes            TEXT
);

-- Minimal backfill so login-context lookups work immediately
UPDATE public.auth_identities
SET username = COALESCE(username, login_identifier),
    normalised_username = COALESCE(normalised_username, lower(trim(login_identifier))),
    email = COALESCE(
        email,
        CASE WHEN login_identifier LIKE '%@%' THEN login_identifier ELSE NULL END
    ),
    normalised_email = COALESCE(
        normalised_email,
        CASE WHEN login_identifier LIKE '%@%' THEN lower(trim(login_identifier)) ELSE NULL END
    )
WHERE identity_type = 'STAFF'
  AND (
        username IS NULL
     OR normalised_username IS NULL
     OR (login_identifier LIKE '%@%' AND (email IS NULL OR normalised_email IS NULL))
  );

UPDATE public.auth_identities
SET email = COALESCE(email, login_identifier),
    normalised_email = COALESCE(normalised_email, lower(trim(login_identifier)))
WHERE identity_type = 'CLIENT'
  AND (email IS NULL OR normalised_email IS NULL);

UPDATE public.auth_identities
SET email = COALESCE(email, login_identifier),
    normalised_email = COALESCE(normalised_email, lower(trim(login_identifier))),
    username = COALESCE(
        username,
        CASE WHEN login_identifier NOT LIKE '%@%' THEN login_identifier ELSE NULL END
    ),
    normalised_username = COALESCE(
        normalised_username,
        CASE WHEN login_identifier NOT LIKE '%@%' THEN lower(trim(login_identifier)) ELSE NULL END
    )
WHERE organisation_id IS NULL
  AND identity_type NOT IN ('CLIENT')
  AND (email IS NULL OR normalised_email IS NULL OR username IS NULL OR normalised_username IS NULL);

-- Synthetic staff emails when still missing
UPDATE public.auth_identities
SET email = COALESCE(email, COALESCE(username, login_identifier) || '@org-' || organisation_id || '.local'),
    normalised_email = COALESCE(
        normalised_email,
        lower(trim(COALESCE(username, login_identifier))) || '@org-' || organisation_id || '.local'
    )
WHERE identity_type = 'STAFF'
  AND organisation_id IS NOT NULL
  AND (email IS NULL OR normalised_email IS NULL);

CREATE INDEX IF NOT EXISTS idx_auth_identities_normalised_email
    ON public.auth_identities (normalised_email);

CREATE INDEX IF NOT EXISTS idx_auth_identities_normalised_username
    ON public.auth_identities (normalised_username);
