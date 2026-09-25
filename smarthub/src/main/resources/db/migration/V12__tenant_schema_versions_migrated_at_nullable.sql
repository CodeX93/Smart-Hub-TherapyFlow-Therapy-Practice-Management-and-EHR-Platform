ALTER TABLE public.tenant_schema_versions
    ALTER COLUMN migrated_at DROP NOT NULL;
