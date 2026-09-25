ALTER TABLE public.organisations
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(64),
    ADD COLUMN IF NOT EXISTS region VARCHAR(100),
    ADD COLUMN IF NOT EXISTS data_residency VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_organisations_timezone ON public.organisations (timezone);
CREATE INDEX IF NOT EXISTS idx_organisations_region ON public.organisations (region);
CREATE INDEX IF NOT EXISTS idx_organisations_data_residency ON public.organisations (data_residency);
