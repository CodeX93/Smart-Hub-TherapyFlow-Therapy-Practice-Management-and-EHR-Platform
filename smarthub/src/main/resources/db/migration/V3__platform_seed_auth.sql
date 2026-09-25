-- PLATFORM
-- Seed auth identities and auth_identity_roles for platform users.

DO $$
DECLARE
  ts TIMESTAMPTZ := '2025-01-01 00:00:00+00';
  org_id BIGINT;
  pwd_hash VARCHAR(255) := '$2a$10$zHJZ0RLC9E/t9fSc5g4KYeZ1iLAMhJbUv519dJvcaGx4eIzaB4oZW';
  auth_superadmin_id BIGINT;
  auth_admin_id BIGINT;
  auth_supervisor_id BIGINT;
  auth_therapist_id BIGINT;
  role_super_admin_id BIGINT;
  role_admin_id BIGINT;
  role_supervisor_id BIGINT;
  role_therapist_id BIGINT;
BEGIN
  SELECT id INTO org_id FROM organisations ORDER BY id LIMIT 1;

  SELECT id INTO role_super_admin_id FROM roles WHERE name = 'SUPER_ADMIN' LIMIT 1;
  SELECT id INTO role_admin_id FROM roles WHERE name = 'ADMIN' LIMIT 1;
  SELECT id INTO role_supervisor_id FROM roles WHERE name = 'SUPERVISOR' LIMIT 1;
  SELECT id INTO role_therapist_id FROM roles WHERE name = 'THERAPIST' LIMIT 1;

  INSERT INTO auth_identities (
    login_identifier, normalised_login_identifier, password_hash, identity_type, auth_provider,
    is_active, email_verified, account_locked, failed_login_attempts, version,
    created_at, updated_at
  )
  SELECT 'superadmin@therapyflow.com', 'superadmin@therapyflow.com', pwd_hash, 'STAFF', 'LOCAL', true, true, false, 0, 0, ts, ts
  WHERE NOT EXISTS (SELECT 1 FROM auth_identities WHERE normalised_login_identifier = 'superadmin@therapyflow.com');
  INSERT INTO auth_identities (
    login_identifier, normalised_login_identifier, password_hash, identity_type, auth_provider,
    is_active, email_verified, account_locked, failed_login_attempts, version,
    created_at, updated_at
  )
  SELECT 'admin@therapyflow.com', 'admin@therapyflow.com', pwd_hash, 'STAFF', 'LOCAL', true, true, false, 0, 0, ts, ts
  WHERE NOT EXISTS (SELECT 1 FROM auth_identities WHERE normalised_login_identifier = 'admin@therapyflow.com');
  INSERT INTO auth_identities (
    login_identifier, normalised_login_identifier, password_hash, identity_type, auth_provider,
    is_active, email_verified, account_locked, failed_login_attempts, version,
    created_at, updated_at
  )
  SELECT 'supervisor@therapyflow.com', 'supervisor@therapyflow.com', pwd_hash, 'STAFF', 'LOCAL', true, true, false, 0, 0, ts, ts
  WHERE NOT EXISTS (SELECT 1 FROM auth_identities WHERE normalised_login_identifier = 'supervisor@therapyflow.com');
  INSERT INTO auth_identities (
    login_identifier, normalised_login_identifier, password_hash, identity_type, auth_provider,
    is_active, email_verified, account_locked, failed_login_attempts, version,
    created_at, updated_at
  )
  SELECT 'therapist@therapyflow.com', 'therapist@therapyflow.com', pwd_hash, 'STAFF', 'LOCAL', true, true, false, 0, 0, ts, ts
  WHERE NOT EXISTS (SELECT 1 FROM auth_identities WHERE normalised_login_identifier = 'therapist@therapyflow.com');

  SELECT id INTO auth_superadmin_id FROM auth_identities WHERE normalised_login_identifier = 'superadmin@therapyflow.com' LIMIT 1;
  SELECT id INTO auth_admin_id FROM auth_identities WHERE normalised_login_identifier = 'admin@therapyflow.com' LIMIT 1;
  SELECT id INTO auth_supervisor_id FROM auth_identities WHERE normalised_login_identifier = 'supervisor@therapyflow.com' LIMIT 1;
  SELECT id INTO auth_therapist_id FROM auth_identities WHERE normalised_login_identifier = 'therapist@therapyflow.com' LIMIT 1;

  INSERT INTO auth_identity_roles (created_at, updated_at, version, auth_id, role_id, organisation_id)
  SELECT ts, ts, 0, auth_superadmin_id, role_super_admin_id, org_id
  WHERE auth_superadmin_id IS NOT NULL AND role_super_admin_id IS NOT NULL AND org_id IS NOT NULL
  ON CONFLICT (auth_id, role_id, organisation_id) WHERE organisation_id IS NOT NULL DO NOTHING;

  INSERT INTO auth_identity_roles (created_at, updated_at, version, auth_id, role_id, organisation_id)
  SELECT ts, ts, 0, auth_admin_id, role_admin_id, org_id
  WHERE auth_admin_id IS NOT NULL AND role_admin_id IS NOT NULL AND org_id IS NOT NULL
  ON CONFLICT (auth_id, role_id, organisation_id) WHERE organisation_id IS NOT NULL DO NOTHING;

  INSERT INTO auth_identity_roles (created_at, updated_at, version, auth_id, role_id, organisation_id)
  SELECT ts, ts, 0, auth_supervisor_id, role_supervisor_id, org_id
  WHERE auth_supervisor_id IS NOT NULL AND role_supervisor_id IS NOT NULL AND org_id IS NOT NULL
  ON CONFLICT (auth_id, role_id, organisation_id) WHERE organisation_id IS NOT NULL DO NOTHING;

  INSERT INTO auth_identity_roles (created_at, updated_at, version, auth_id, role_id, organisation_id)
  SELECT ts, ts, 0, auth_therapist_id, role_therapist_id, org_id
  WHERE auth_therapist_id IS NOT NULL AND role_therapist_id IS NOT NULL AND org_id IS NOT NULL
  ON CONFLICT (auth_id, role_id, organisation_id) WHERE organisation_id IS NOT NULL DO NOTHING;
END $$;
