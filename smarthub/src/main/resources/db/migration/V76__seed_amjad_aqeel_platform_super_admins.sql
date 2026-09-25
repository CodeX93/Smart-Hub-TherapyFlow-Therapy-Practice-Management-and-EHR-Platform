-- PLATFORM
-- Seed Amjad and Aqeel platform super admins (same capabilities as superadmin@therapyflow.com).
-- Role SUPER_ADMIN with organisation_id NULL grants ROLE_PLATFORM_SUPER_ADMIN at login.
-- Idempotent.

DO $$
DECLARE
  ts TIMESTAMPTZ := NOW();
  pwd_hash VARCHAR(255);
  v_role_id BIGINT;
  v_auth_id BIGINT;
  v_email TEXT;
  emails TEXT[] := ARRAY[
    'amjad-superadmin@therapyflow.pro',
    'aqeel-superadmin@therapyflow.pro'
  ];
BEGIN
  SELECT id INTO v_role_id FROM public.roles WHERE name = 'SUPER_ADMIN' LIMIT 1;
  IF v_role_id IS NULL THEN
    RAISE EXCEPTION 'Seed failed: role SUPER_ADMIN not found';
  END IF;

  -- Prefer current password of the primary superadmin; fall back to shared seed hash.
  SELECT ai.password_hash INTO pwd_hash
  FROM public.auth_identities ai
  WHERE ai.normalised_login_identifier = 'superadmin@therapyflow.com'
  LIMIT 1;

  IF pwd_hash IS NULL OR pwd_hash = '' THEN
    pwd_hash := '$2a$10$zHJZ0RLC9E/t9fSc5g4KYeZ1iLAMhJbUv519dJvcaGx4eIzaB4oZW';
  END IF;

  FOREACH v_email IN ARRAY emails
  LOOP
    INSERT INTO public.auth_identities (
      login_identifier, normalised_login_identifier, password_hash, identity_type, auth_provider,
      is_active, email_verified, account_locked, failed_login_attempts, version,
      created_at, updated_at, created_by, updated_by, is_deleted,
      sso_enabled, organisation_id, provider_user_id
    )
    SELECT
      v_email,
      v_email,
      pwd_hash,
      'STAFF',
      'LOCAL',
      true,
      true,
      false,
      0,
      0,
      ts,
      ts,
      0,
      0,
      false,
      false,
      NULL,
      NULL
    WHERE NOT EXISTS (
      SELECT 1
      FROM public.auth_identities ai
      WHERE ai.normalised_login_identifier = v_email
    );

    -- Keep identity usable as a platform super admin.
    UPDATE public.auth_identities
    SET
      is_active = true,
      email_verified = true,
      account_locked = false,
      is_deleted = false,
      deleted_at = NULL,
      updated_at = ts
    WHERE normalised_login_identifier = v_email;

    SELECT id INTO v_auth_id
    FROM public.auth_identities
    WHERE normalised_login_identifier = v_email
    LIMIT 1;

    IF v_auth_id IS NULL THEN
      RAISE EXCEPTION 'Seed failed: auth identity not found for %', v_email;
    END IF;

    INSERT INTO public.auth_identity_roles (
      created_at, updated_at, version, auth_id, role_id, organisation_id,
      created_by, updated_by, is_deleted, deleted_at
    )
    SELECT
      ts, ts, 0, v_auth_id, v_role_id, NULL,
      0, 0, false, NULL
    WHERE NOT EXISTS (
      SELECT 1
      FROM public.auth_identity_roles air
      WHERE air.auth_id = v_auth_id
        AND air.role_id = v_role_id
        AND air.organisation_id IS NULL
    );
  END LOOP;
END $$;
