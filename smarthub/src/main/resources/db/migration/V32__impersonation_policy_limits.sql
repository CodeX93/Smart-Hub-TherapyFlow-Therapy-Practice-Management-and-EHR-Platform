ALTER TABLE IF EXISTS platform_impersonation_policy
    ADD COLUMN IF NOT EXISTS allowed_role_names TEXT,
    ADD COLUMN IF NOT EXISTS denied_role_names TEXT,
    ADD COLUMN IF NOT EXISTS allowed_org_ids TEXT,
    ADD COLUMN IF NOT EXISTS denied_org_ids TEXT;
