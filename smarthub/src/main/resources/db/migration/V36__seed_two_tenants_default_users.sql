-- PLATFORM
-- Seed two realistic tenants and default users per role for each tenant.
-- SUPER_ADMIN is seeded as platform-only (not tenant-scoped).
--
-- Notes:
-- - This schema uses BIGINT identity PKs; UUIDs are stored in auth_identities.provider_user_id.
-- - Script is idempotent: it checks existence before insert.

DO $$
DECLARE
  ts TIMESTAMPTZ := NOW();
  pwd_hash VARCHAR(255) := '$2a$10$zHJZ0RLC9E/t9fSc5g4KYeZ1iLAMhJbUv519dJvcaGx4eIzaB4oZW';
  v_org_id BIGINT;
  v_role_id BIGINT;
  v_auth_id BIGINT;
  t RECORD;
  u RECORD;
BEGIN
  -- Ensure BILLING role exists in global role catalog.
  IF NOT EXISTS (SELECT 1 FROM public.roles WHERE name = 'BILLING') THEN
    INSERT INTO public.roles (
      createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version,
      description, display_name, is_active, is_system, name, organisation_id
    )
    VALUES (
      ts, 0, NULL, false, ts, 0, 0,
      'Billing operations and invoicing', 'Billing', true, true, 'BILLING', NULL
    );
  END IF;

  -- 1) Tenants (organisations)
  WITH tenant_seed AS (
    SELECT *
    FROM (
      VALUES
        (
          'Northstar Wellness Center', 'northstar-wellness', 'northstar',
          'tenant_northstar', 'CLINIC', 'America/Toronto', 'Canada Central', 'CA',
          'en-CA', '#0F4C81', '#4FA3D1', '#F4B400', 'https://cdn.example.com/logos/northstar.png',
          'support@northstarwellness.com', '120 King St W, Toronto, ON, Canada'
        ),
        (
          'Harbor Mental Health Group', 'harbor-mental-health', 'harbor',
          'tenant_harbor', 'ENTERPRISE', 'America/New_York', 'Canada Central', 'US',
          'en-US', '#1E3A8A', '#14B8A6', '#F97316', 'https://cdn.example.com/logos/harbor.png',
          'help@harbormh.com', '550 Atlantic Ave, Boston, MA, USA'
        )
    ) AS v(
      name, slug, subdomain, schema_name, organisation_type, timezone, region, data_residency,
      locale, brand_primary_color, brand_secondary_color, brand_accent_color, logo_url,
      support_email, support_address
    )
  )
  INSERT INTO public.organisations (
    created_at, updated_at, version, created_by, updated_by, is_deleted,
    name, slug, status, subdomain, schema_name, organisation_type, timezone, region, data_residency,
    locale, brand_primary_color, brand_secondary_color, brand_accent_color, logo_url,
    support_email, support_address, backup_status
  )
  SELECT
    ts, ts, 0, 0, 0, false,
    s.name, s.slug, 'ACTIVE', s.subdomain, s.schema_name, s.organisation_type, s.timezone, s.region, s.data_residency,
    s.locale, s.brand_primary_color, s.brand_secondary_color, s.brand_accent_color, s.logo_url,
    s.support_email, s.support_address, 'PENDING'
  FROM tenant_seed s
  WHERE NOT EXISTS (SELECT 1 FROM public.organisations o WHERE o.slug = s.slug);

  -- Optional but useful for scheduler visibility.
  FOR t IN
    SELECT o.id AS org_id
    FROM public.organisations o
    WHERE o.slug IN ('northstar-wellness', 'harbor-mental-health')
  LOOP
    IF NOT EXISTS (SELECT 1 FROM public.tenant_schema_versions v WHERE v.organisation_id = t.org_id) THEN
      INSERT INTO public.tenant_schema_versions (organisation_id, version, migrated_at, status, error_message)
      VALUES (t.org_id, '35', ts, 'SUCCESS', NULL);
    END IF;
  END LOOP;

  -- 2) Users per tenant and role
  FOR u IN
    SELECT *
    FROM (
      VALUES
        -- Tenant 1: Northstar Wellness Center
        ('northstar-wellness','ADMIN','northstar.admin1@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a002'),
        ('northstar-wellness','ADMIN','northstar.admin2@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a003'),
        ('northstar-wellness','THERAPIST','northstar.therapist1@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a004'),
        ('northstar-wellness','THERAPIST','northstar.therapist2@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a005'),
        ('northstar-wellness','THERAPIST','northstar.therapist3@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a006'),
        ('northstar-wellness','CLIENT','northstar.client1@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a007'),
        ('northstar-wellness','CLIENT','northstar.client2@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a008'),
        ('northstar-wellness','CLIENT','northstar.client3@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a009'),
        ('northstar-wellness','CLIENT','northstar.client4@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a010'),
        ('northstar-wellness','CLIENT','northstar.client5@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a011'),
        ('northstar-wellness','BILLING','northstar.billing@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a012'),
        ('northstar-wellness','SUPERVISOR','northstar.supervisor@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a013'),

        -- Tenant 2: Harbor Mental Health Group
        ('harbor-mental-health','ADMIN','harbor.admin1@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a015'),
        ('harbor-mental-health','ADMIN','harbor.admin2@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a016'),
        ('harbor-mental-health','THERAPIST','harbor.therapist1@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a017'),
        ('harbor-mental-health','THERAPIST','harbor.therapist2@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a018'),
        ('harbor-mental-health','THERAPIST','harbor.therapist3@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a019'),
        ('harbor-mental-health','CLIENT','harbor.client1@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a020'),
        ('harbor-mental-health','CLIENT','harbor.client2@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a021'),
        ('harbor-mental-health','CLIENT','harbor.client3@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a022'),
        ('harbor-mental-health','CLIENT','harbor.client4@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a023'),
        ('harbor-mental-health','CLIENT','harbor.client5@therapyflowseed.com','CLIENT','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a024'),
        ('harbor-mental-health','BILLING','harbor.billing@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a025'),
        ('harbor-mental-health','SUPERVISOR','harbor.supervisor@therapyflowseed.com','STAFF','6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a026')
    ) AS v(tenant_slug, role_name, email, identity_type, provider_uuid)
  LOOP
    SELECT id INTO v_org_id FROM public.organisations WHERE slug = u.tenant_slug LIMIT 1;
    SELECT id INTO v_role_id FROM public.roles WHERE name = u.role_name LIMIT 1;

    IF v_org_id IS NULL THEN
      RAISE EXCEPTION 'Seed failed: tenant % not found', u.tenant_slug;
    END IF;
    IF v_role_id IS NULL THEN
      RAISE EXCEPTION 'Seed failed: role % not found', u.role_name;
    END IF;

    -- Auth identity
    INSERT INTO public.auth_identities (
      login_identifier, normalised_login_identifier, password_hash, identity_type, auth_provider,
      is_active, email_verified, account_locked, failed_login_attempts, version,
      created_at, updated_at, created_by, updated_by, is_deleted,
      sso_enabled, organisation_id, provider_user_id
    )
    SELECT
      u.email, lower(u.email), pwd_hash, u.identity_type, 'LOCAL',
      true, true, false, 0, 0,
      ts, ts, 0, 0, false,
      false, v_org_id, u.provider_uuid
    WHERE NOT EXISTS (
      SELECT 1
      FROM public.auth_identities ai
      WHERE ai.normalised_login_identifier = lower(u.email)
    );

    SELECT id INTO v_auth_id
    FROM public.auth_identities
    WHERE normalised_login_identifier = lower(u.email)
    LIMIT 1;

    -- Tenant membership
    INSERT INTO public.user_organisations (auth_id, organisation_id, created_at)
    SELECT v_auth_id, v_org_id, ts
    WHERE NOT EXISTS (
      SELECT 1
      FROM public.user_organisations uo
      WHERE uo.auth_id = v_auth_id
        AND uo.organisation_id = v_org_id
    );

    -- Role assignment in tenant
    INSERT INTO public.auth_identity_roles (
      created_at, updated_at, version, auth_id, role_id, organisation_id,
      created_by, updated_by, is_deleted, deleted_at
    )
    SELECT
      ts, ts, 0, v_auth_id, v_role_id, v_org_id,
      0, 0, false, NULL
    WHERE NOT EXISTS (
      SELECT 1
      FROM public.auth_identity_roles air
      WHERE air.auth_id = v_auth_id
        AND air.role_id = v_role_id
        AND air.organisation_id = v_org_id
    );
  END LOOP;

  -- Platform-only SUPER_ADMIN (no tenant scope).
  SELECT id INTO v_role_id FROM public.roles WHERE name = 'SUPER_ADMIN' LIMIT 1;
  IF v_role_id IS NULL THEN
    RAISE EXCEPTION 'Seed failed: role SUPER_ADMIN not found';
  END IF;

  INSERT INTO public.auth_identities (
    login_identifier, normalised_login_identifier, password_hash, identity_type, auth_provider,
    is_active, email_verified, account_locked, failed_login_attempts, version,
    created_at, updated_at, created_by, updated_by, is_deleted,
    sso_enabled, organisation_id, provider_user_id
  )
  SELECT
    'platform.superadmin@therapyflowseed.com',
    'platform.superadmin@therapyflowseed.com',
    pwd_hash, 'STAFF', 'LOCAL',
    true, true, false, 0, 0,
    ts, ts, 0, 0, false,
    false, NULL, '6ea67ab0-b4b8-4cf1-9c0f-3eb26f53a001'
  WHERE NOT EXISTS (
    SELECT 1
    FROM public.auth_identities ai
    WHERE ai.normalised_login_identifier = 'platform.superadmin@therapyflowseed.com'
  );

  SELECT id INTO v_auth_id
  FROM public.auth_identities
  WHERE normalised_login_identifier = 'platform.superadmin@therapyflowseed.com'
  LIMIT 1;

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
END $$;
