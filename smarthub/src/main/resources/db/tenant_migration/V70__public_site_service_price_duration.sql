-- Add billing-like duration and price to public site counseling services.

ALTER TABLE public_site_services
    ADD COLUMN IF NOT EXISTS duration_minutes INT NOT NULL DEFAULT 30;

ALTER TABLE public_site_services
    ADD COLUMN IF NOT EXISTS base_rate NUMERIC(10, 2) NOT NULL DEFAULT 0.00;

-- Consultation defaults to 30 min / free; other counseling types keep 30 / 0 until edited
UPDATE public_site_services
SET duration_minutes = 30, base_rate = 0.00
WHERE slug = 'consultation' AND is_deleted = FALSE;
