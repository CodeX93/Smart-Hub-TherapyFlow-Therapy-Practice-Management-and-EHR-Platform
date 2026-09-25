DO $$
BEGIN
    IF to_regclass('public.form_template_versions') IS NULL THEN
        RAISE NOTICE 'Skipping V54: table public.form_template_versions does not exist';
        RETURN;
    END IF;

    ALTER TABLE public.form_template_versions
        ADD COLUMN IF NOT EXISTS requires_signature BOOLEAN;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_form_template_versions_template_version_number'
          AND conrelid = 'public.form_template_versions'::regclass
    ) THEN
        ALTER TABLE public.form_template_versions
            ADD CONSTRAINT uk_form_template_versions_template_version_number
            UNIQUE (template_id, version_number);
    END IF;

    CREATE UNIQUE INDEX IF NOT EXISTS ux_form_template_versions_single_active
        ON public.form_template_versions (template_id)
        WHERE status = 'ACTIVE' AND is_deleted = false;
END $$;
