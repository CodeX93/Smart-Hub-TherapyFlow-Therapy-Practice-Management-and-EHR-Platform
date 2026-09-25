-- Align tenant schemas with configurable system options (idempotent follow-up).

ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_status_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_stage_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_gender_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_marital_status_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_client_type_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_service_type_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_service_frequency_check;
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_follow_up_priority_check;

UPDATE clients
SET stage = LOWER(stage)
WHERE stage IS NOT NULL
  AND stage = UPPER(stage);

UPDATE option_categories
SET category_key = 'session_mode'
WHERE category_key = 'session_modes';
