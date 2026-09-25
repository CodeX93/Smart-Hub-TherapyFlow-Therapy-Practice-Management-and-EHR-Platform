-- PLATFORM
-- Allow platform-level roles without organisation_id.

ALTER TABLE public.auth_identity_roles
    ALTER COLUMN organisation_id DROP NOT NULL;

ALTER TABLE public.auth_identity_roles
    DROP CONSTRAINT IF EXISTS uq_auth_identity_roles_auth_role_org;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_identity_roles_auth_role_org
    ON public.auth_identity_roles (auth_id, role_id, organisation_id)
    WHERE organisation_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_identity_roles_auth_role_platform
    ON public.auth_identity_roles (auth_id, role_id)
    WHERE organisation_id IS NULL;
