-- TENANT
-- Close client file sets clients.stage = CLOSED.
-- Older tenant schemas only allow stages through FOLLOW_UP in clients_stage_check.

ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_stage_check;

ALTER TABLE clients ADD CONSTRAINT clients_stage_check CHECK (
    stage IS NULL OR stage IN (
        'INTAKE',
        'ASSESSMENT',
        'ACTIVE_TREATMENT',
        'MAINTENANCE',
        'DISCHARGE_PLANNING',
        'DISCHARGE',
        'FOLLOW_UP',
        'CLOSED'
    )
);
