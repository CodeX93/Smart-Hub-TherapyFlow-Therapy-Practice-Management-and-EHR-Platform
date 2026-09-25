-- Public marketing counseling services (separate from billing CPT services).
-- Booking always uses CONSULTATION hours; these rows are labels for the public dropdown.

CREATE TABLE IF NOT EXISTS public_site_services (
    id              BIGSERIAL PRIMARY KEY,
    createdat       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ NULL,
    updatedat       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      BIGINT NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    description     TEXT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    display_order   INT NOT NULL DEFAULT 0,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_public_site_services_slug_active
    ON public_site_services (slug)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_public_site_services_enabled_order
    ON public_site_services (enabled, display_order)
    WHERE is_deleted = FALSE;

-- Seed Consultation (system) + Resilience counseling categories (idempotent)
INSERT INTO public_site_services (
    createdat, created_by, is_deleted, updatedat, updated_by, version,
    name, slug, description, enabled, display_order, is_system
)
SELECT NOW(), 0, FALSE, NOW(), 0, 0, v.name, v.slug, v.description, TRUE, v.ord, v.is_system
FROM (VALUES
    ('Consultation', 'consultation', 'Public free consultation booking', 0, TRUE),
    ('Individual Counseling', 'individual', NULL, 10, FALSE),
    ('Family Counseling', 'family', NULL, 20, FALSE),
    ('Couples Counseling', 'couples', NULL, 30, FALSE),
    ('Child Counseling', 'child', NULL, 40, FALSE),
    ('Adolescent and Teen Counseling', 'adolescent', NULL, 50, FALSE),
    ('University and College Student Counseling', 'student', NULL, 60, FALSE),
    ('Motor Vehicle Accident Counseling (MVA)', 'mva', NULL, 70, FALSE),
    ('Anger management', 'anger', NULL, 80, FALSE),
    ('Refugee counseling (IFHP Inclusive)', 'refugee', NULL, 90, FALSE),
    ('Faith-Based Counseling', 'faith', NULL, 100, FALSE),
    ('Life-Review Counseling', 'transitions', NULL, 110, FALSE),
    ('Grief Counseling', 'grief', NULL, 120, FALSE),
    ('ADHD/Autism Counseling', 'adhd', NULL, 130, FALSE),
    ('Art-Based Counseling', 'art', NULL, 140, FALSE),
    ('Learning Disabilities Counseling', 'learning', NULL, 150, FALSE)
) AS v(name, slug, description, ord, is_system)
WHERE NOT EXISTS (
    SELECT 1 FROM public_site_services s
    WHERE s.slug = v.slug AND s.is_deleted = FALSE
);
