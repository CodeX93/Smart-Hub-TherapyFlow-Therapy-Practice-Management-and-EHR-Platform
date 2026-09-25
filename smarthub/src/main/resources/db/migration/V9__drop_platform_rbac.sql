-- PLATFORM
-- Remove legacy platform RBAC tables (unified RBAC via auth_identity_roles).

DROP TABLE IF EXISTS public.platform_user_roles;
DROP TABLE IF EXISTS public.platform_users;
DROP TABLE IF EXISTS public.platform_roles;
