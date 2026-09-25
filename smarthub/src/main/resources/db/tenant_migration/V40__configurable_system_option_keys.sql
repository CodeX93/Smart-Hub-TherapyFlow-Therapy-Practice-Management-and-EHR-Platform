-- Normalize legacy enum names to configurable system option keys.
-- Drop legacy CHECK constraints first (older tenant schemas still enforce enum literals).

ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_status_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_stage_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_gender_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_marital_status_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_client_type_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_service_type_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_service_frequency_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_follow_up_priority_check;

ALTER TABLE client_employment DROP CONSTRAINT IF EXISTS client_employment_employment_status_check;
ALTER TABLE client_employment DROP CONSTRAINT IF EXISTS client_employment_education_level_check;

ALTER TABLE client_referrals DROP CONSTRAINT IF EXISTS client_referrals_referral_source_check;

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_status_check;

ALTER TABLE sessions DROP CONSTRAINT IF EXISTS sessions_status_check;
ALTER TABLE sessions DROP CONSTRAINT IF EXISTS sessions_session_mode_check;

UPDATE clients SET status = LOWER(status) WHERE status IS NOT NULL AND status = UPPER(status);
UPDATE clients SET gender = LOWER(gender) WHERE gender IS NOT NULL AND gender = UPPER(gender);
UPDATE clients SET marital_status = LOWER(marital_status) WHERE marital_status IS NOT NULL AND marital_status = UPPER(marital_status);
UPDATE clients SET client_type = LOWER(client_type) WHERE client_type IS NOT NULL AND client_type = UPPER(client_type);
UPDATE clients SET service_type = LOWER(service_type) WHERE service_type IS NOT NULL AND service_type = UPPER(service_type);
UPDATE clients SET service_frequency = LOWER(service_frequency) WHERE service_frequency IS NOT NULL AND service_frequency = UPPER(service_frequency);

UPDATE client_employment SET employment_status = LOWER(employment_status)
WHERE employment_status IS NOT NULL AND employment_status = UPPER(employment_status);
UPDATE client_employment SET education_level = LOWER(education_level)
WHERE education_level IS NOT NULL AND education_level = UPPER(education_level);

UPDATE client_referrals SET referral_source = LOWER(referral_source)
WHERE referral_source IS NOT NULL AND referral_source = UPPER(referral_source);

UPDATE tasks SET status = LOWER(status) WHERE status IS NOT NULL AND status = UPPER(status);

UPDATE sessions SET status = 'scheduled' WHERE status IN ('SCHEDULED', 'scheduled');
UPDATE sessions SET status = 'confirmed' WHERE status IN ('CONFIRMED', 'confirmed');
UPDATE sessions SET status = 'in-progress' WHERE status IN ('IN_PROGRESS', 'in_progress', 'in-progress');
UPDATE sessions SET status = 'completed' WHERE status IN ('COMPLETED', 'completed');
UPDATE sessions SET status = 'cancelled' WHERE status IN ('CANCELLED', 'cancelled');
UPDATE sessions SET status = 'rescheduling' WHERE status IN ('RESCHEDULING', 'rescheduling');
UPDATE sessions SET status = 'no-show' WHERE status IN ('NO_SHOW', 'no_show', 'no-show');
UPDATE sessions SET status = 'overdue' WHERE status IN ('OVERDUE', 'overdue');

UPDATE sessions SET session_mode = LOWER(session_mode) WHERE session_mode IS NOT NULL AND session_mode = UPPER(session_mode);
