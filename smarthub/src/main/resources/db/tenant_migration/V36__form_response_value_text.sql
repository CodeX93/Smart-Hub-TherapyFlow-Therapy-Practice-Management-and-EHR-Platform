-- TENANT
-- Encrypted form response values are opaque strings, not valid PostgreSQL JSON.
ALTER TABLE form_responses
    ALTER COLUMN response_value TYPE TEXT USING response_value::text;
