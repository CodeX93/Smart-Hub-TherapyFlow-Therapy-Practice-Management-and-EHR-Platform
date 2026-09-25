-- Lock SUPERVISOR to therapist-parity permissions (team-scoped).
-- Strip org-wide / admin-only permissions. Ensure therapist clinical perms remain.

-- Remove over-broad permissions from SUPERVISOR
DELETE FROM public.role_permissions rp
USING public.roles r, public.permissions p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND UPPER(r.name) = 'SUPERVISOR'
  AND p.name IN (
    'CLIENT_VIEW_ALL',
    'CLIENT_DELETE',
    'FORM_TEMPLATE_MANAGE',
    'BILLING_MANAGE',
    'REPORT_VIEW',
    'AUDIT_VIEW',
    'AUDIT_EXPORT',
    'ROOM_MANAGE',
    'USER_VIEW'
  );

-- Ensure therapist-parity permissions are present on SUPERVISOR
INSERT INTO public.role_permissions (
  createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version, role_id, permission_id
)
SELECT
  NOW(), 0, NULL::timestamptz, false, NOW(), 0, 0, r.id, p.id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE UPPER(r.name) = 'SUPERVISOR'
  AND p.name IN (
    'CLIENT_VIEW_OWN', 'CLIENT_VIEW_TEAM', 'CLIENT_VIEW', 'CLIENT_CREATE', 'CLIENT_EDIT', 'CLIENT_EXPORT',
    'SESSION_VIEW', 'SESSION_CREATE', 'SESSION_EDIT', 'SESSION_DELETE',
    'ASSESSMENT_VIEW', 'ASSESSMENT_ASSIGN', 'FORM_FILL', 'FORM_VIEW',
    'BILLING_VIEW', 'BILLING_CREATE', 'BILLING_EDIT', 'BILLING_EXPORT',
    'CONSENT_ADMIN_VIEW', 'AI_USE'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM public.role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
  );
