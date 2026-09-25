-- Split permission ownership by scope:
-- - organisation_id IS NULL  => platform/global permission
-- - organisation_id NOT NULL => tenant-owned permission

ALTER TABLE public.permissions
    ADD COLUMN IF NOT EXISTS organisation_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_permissions_organisation'
          AND conrelid = 'public.permissions'::regclass
    ) THEN
        ALTER TABLE public.permissions
            ADD CONSTRAINT fk_permissions_organisation
            FOREIGN KEY (organisation_id)
            REFERENCES public.organisations(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_permissions_organisation_id
    ON public.permissions (organisation_id);
