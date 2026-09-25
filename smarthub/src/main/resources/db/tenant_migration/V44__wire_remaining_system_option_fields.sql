-- Wire remaining system-option catalog fields to entity columns.

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS task_type VARCHAR(100);

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS title_key VARCHAR(100);

ALTER TABLE client_insurance
    ADD COLUMN IF NOT EXISTS insurance_type VARCHAR(100);

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS treatment_modality VARCHAR(100);
