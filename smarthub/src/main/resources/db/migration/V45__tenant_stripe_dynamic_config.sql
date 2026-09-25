ALTER TABLE public.organisations
    ADD COLUMN IF NOT EXISTS stripe_publishable_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_secret_key_encrypted TEXT,
    ADD COLUMN IF NOT EXISTS stripe_webhook_secret_encrypted TEXT,
    ADD COLUMN IF NOT EXISTS stripe_webhook_endpoint_url VARCHAR(500);

