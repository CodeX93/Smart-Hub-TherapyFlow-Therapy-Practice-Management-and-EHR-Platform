-- Add CLIENT_VIEW_OWN permission to THERAPIST, BILLING_SPECIALIST, and RECEPTIONIST roles
-- This permission is required for these roles to view billing records for their assigned clients

-- Add CLIENT_VIEW_OWN permission to THERAPIST role
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.name = 'THERAPIST'
  AND p.name = 'CLIENT_VIEW_OWN'
  AND NOT EXISTS (
      SELECT 1
      FROM public.role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- Add CLIENT_VIEW_OWN permission to BILLING_SPECIALIST role
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.name = 'BILLING_SPECIALIST'
  AND p.name = 'CLIENT_VIEW_OWN'
  AND NOT EXISTS (
      SELECT 1
      FROM public.role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- Add CLIENT_VIEW_OWN permission to RECEPTIONIST role
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.name = 'RECEPTIONIST'
  AND p.name = 'CLIENT_VIEW_OWN'
  AND NOT EXISTS (
      SELECT 1
      FROM public.role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- Note: This migration is idempotent and safe to run multiple times
-- The NOT EXISTS clause ensures no duplicate permissions are created
