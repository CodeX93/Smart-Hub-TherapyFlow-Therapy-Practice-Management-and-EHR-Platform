-- Normalize client stage values from legacy enum names to configurable option keys.
-- Drop legacy CHECK first: older schemas still enforce UPPERCASE stage literals (e.g. INTAKE).

ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_stage_check;

UPDATE clients
SET stage = LOWER(stage)
WHERE stage IS NOT NULL
  AND stage = UPPER(stage);
