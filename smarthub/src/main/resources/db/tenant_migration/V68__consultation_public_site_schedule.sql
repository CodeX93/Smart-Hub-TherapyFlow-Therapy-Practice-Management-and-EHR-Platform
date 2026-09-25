-- Consultation / public-site scheduling support:
-- 1) Nullable service_id on working hours (NULL = All-Services schedule)
-- 2) public_site_enabled on services for Public Site settings

ALTER TABLE user_profile_working_hours
    ADD COLUMN IF NOT EXISTS service_id BIGINT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_working_hours_service'
    ) THEN
        ALTER TABLE user_profile_working_hours
            ADD CONSTRAINT fk_working_hours_service
            FOREIGN KEY (service_id) REFERENCES services(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_working_hours_profile_day_service
    ON user_profile_working_hours (user_profile_id, day, service_id);

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS public_site_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure a Consultation service exists for public booking (idempotent per tenant)
INSERT INTO services (
    createdat, created_by, is_deleted, updatedat, updated_by, version,
    base_rate, category, client_portal_visible, description, duration,
    is_active, service_code, service_name, therapist_visible, public_site_enabled
)
SELECT
    NOW(), 0, FALSE, NOW(), 0, 0,
    0.00, 'Consultation', FALSE,
    'Public free consultation booking', 30,
    TRUE, 'CONSULTATION', 'Consultation', TRUE, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM services WHERE service_code = 'CONSULTATION' AND is_deleted = FALSE
);
