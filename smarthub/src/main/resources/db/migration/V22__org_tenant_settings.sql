ALTER TABLE public.organisations
    ADD COLUMN IF NOT EXISTS locale VARCHAR(20),
    ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS brand_primary_color VARCHAR(16),
    ADD COLUMN IF NOT EXISTS brand_secondary_color VARCHAR(16),
    ADD COLUMN IF NOT EXISTS brand_accent_color VARCHAR(16),
    ADD COLUMN IF NOT EXISTS support_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS support_address VARCHAR(500);

