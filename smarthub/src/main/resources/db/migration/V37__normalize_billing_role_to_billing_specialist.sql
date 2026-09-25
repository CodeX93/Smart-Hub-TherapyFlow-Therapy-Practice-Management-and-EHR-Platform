-- PLATFORM
-- Normalize legacy BILLING role usage to BILLING_SPECIALIST.
-- Keeps backward compatibility for existing environments seeded with V36.

DO $$
DECLARE
  ts TIMESTAMPTZ := NOW();
  v_billing_role_id BIGINT;
  v_specialist_role_id BIGINT;
BEGIN
  SELECT id INTO v_billing_role_id
  FROM public.roles
  WHERE name = 'BILLING'
  LIMIT 1;

  SELECT id INTO v_specialist_role_id
  FROM public.roles
  WHERE name = 'BILLING_SPECIALIST'
  LIMIT 1;

  -- Ensure canonical role exists.
  IF v_specialist_role_id IS NULL THEN
    INSERT INTO public.roles (
      createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version,
      description, display_name, is_active, is_system, name, organisation_id
    )
    VALUES (
      ts, 0, NULL, false, ts, 0, 0,
      'Billing and payments', 'Billing Specialist', true, true, 'BILLING_SPECIALIST', NULL
    );

    SELECT id INTO v_specialist_role_id
    FROM public.roles
    WHERE name = 'BILLING_SPECIALIST'
    LIMIT 1;
  END IF;

  IF v_billing_role_id IS NULL OR v_specialist_role_id IS NULL THEN
    RETURN;
  END IF;

  -- Copy permissions from legacy role to canonical role (idempotent).
  INSERT INTO public.role_permissions (
    createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version,
    permission_id, role_id
  )
  SELECT
    ts, 0, NULL, false, ts, 0, 0,
    rp.permission_id, v_specialist_role_id
  FROM public.role_permissions rp
  WHERE rp.role_id = v_billing_role_id
    AND NOT EXISTS (
      SELECT 1
      FROM public.role_permissions x
      WHERE x.role_id = v_specialist_role_id
        AND x.permission_id = rp.permission_id
    );

  -- Re-point auth role assignments where no canonical assignment exists yet.
  UPDATE public.auth_identity_roles air
  SET role_id = v_specialist_role_id,
      updated_at = ts,
      updated_by = 0
  WHERE air.role_id = v_billing_role_id
    AND NOT EXISTS (
      SELECT 1
      FROM public.auth_identity_roles x
      WHERE x.auth_id = air.auth_id
        AND x.role_id = v_specialist_role_id
        AND (
          (x.organisation_id IS NULL AND air.organisation_id IS NULL)
          OR x.organisation_id = air.organisation_id
        )
    );

  -- Remove remaining duplicates still pointing to legacy role.
  DELETE FROM public.auth_identity_roles
  WHERE role_id = v_billing_role_id;

  -- Keep legacy role disabled to prevent new assignments.
  UPDATE public.roles
  SET is_active = false,
      updatedat = ts,
      updated_by = 0
  WHERE id = v_billing_role_id;
END $$;

