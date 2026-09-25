-- TENANT
-- invoice_policies may be missing on legacy tenants that reached v45 via incremental migrations only.

CREATE TABLE IF NOT EXISTS invoice_policies (
    id BIGSERIAL PRIMARY KEY,
    client_type_key VARCHAR(100) NOT NULL,
    client_type_label VARCHAR(255) NOT NULL,
    appointment_status_key VARCHAR(100) NOT NULL,
    appointment_status_label VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    price_type VARCHAR(20) NOT NULL,
    invoice_price NUMERIC(10, 2) NOT NULL,
    policy_name VARCHAR(255),
    service_id BIGINT,
    effective_from DATE,
    effective_to DATE,
    priority INTEGER NOT NULL DEFAULT 0,
    createdat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updatedat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_by BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

ALTER TABLE invoice_policies
    ADD COLUMN IF NOT EXISTS policy_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS service_id BIGINT,
    ADD COLUMN IF NOT EXISTS effective_from DATE,
    ADD COLUMN IF NOT EXISTS effective_to DATE,
    ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'session_billing'
    ) THEN
        ALTER TABLE session_billing
            ADD COLUMN IF NOT EXISTS invoice_policy_id BIGINT,
            ADD COLUMN IF NOT EXISTS stripe_checkout_session_id VARCHAR(255),
            ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_invoice_policy_client ON invoice_policies (client_type_key);
CREATE INDEX IF NOT EXISTS idx_invoice_policy_appointment ON invoice_policies (appointment_status_key);
CREATE INDEX IF NOT EXISTS idx_invoice_policy_enabled ON invoice_policies (enabled);
CREATE INDEX IF NOT EXISTS idx_invoice_policy_service ON invoice_policies (service_id);
CREATE INDEX IF NOT EXISTS idx_invoice_policy_priority ON invoice_policies (priority);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'session_billing'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_session_billing_invoice_policy ON session_billing (invoice_policy_id);
    END IF;
END $$;

ALTER TABLE invoice_policies DROP CONSTRAINT IF EXISTS uk_invoice_policy_client_appointment;

CREATE UNIQUE INDEX IF NOT EXISTS uk_invoice_policy_scope
    ON invoice_policies (client_type_key, appointment_status_key, COALESCE(service_id, -1));
