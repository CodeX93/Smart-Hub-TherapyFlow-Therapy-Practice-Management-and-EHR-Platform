-- Approach C: encrypt DOB at rest + remaining client demographic PHI column widening.
-- date_of_birth becomes TEXT (TFENC:v2); equality/dupe checks use date_of_birth_blind_idx.

ALTER TABLE clients
    ALTER COLUMN date_of_birth TYPE TEXT USING (
        CASE
            WHEN date_of_birth IS NULL THEN NULL
            ELSE date_of_birth::text
        END
    );

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS date_of_birth_blind_idx BYTEA;

DROP INDEX IF EXISTS idx_client_date_of_birth;

CREATE INDEX IF NOT EXISTS idx_clients_dob_blind
    ON clients (date_of_birth_blind_idx);

-- Widen short varchar PHI columns so AES-GCM ciphertext fits.
ALTER TABLE clients ALTER COLUMN gender TYPE TEXT;
ALTER TABLE clients ALTER COLUMN marital_status TYPE TEXT;
ALTER TABLE clients ALTER COLUMN preferred_language TYPE TEXT;
ALTER TABLE clients ALTER COLUMN pronouns TYPE TEXT;
ALTER TABLE clients ALTER COLUMN client_type TYPE TEXT;
ALTER TABLE clients ALTER COLUMN service_type TYPE TEXT;
ALTER TABLE clients ALTER COLUMN service_frequency TYPE TEXT;
ALTER TABLE clients ALTER COLUMN treatment_modality TYPE TEXT;

ALTER TABLE client_contacts ALTER COLUMN label TYPE TEXT;
ALTER TABLE client_contacts ALTER COLUMN relationship TYPE TEXT;
ALTER TABLE client_contacts ALTER COLUMN contact_person_name TYPE TEXT;

ALTER TABLE client_insurance ALTER COLUMN insurance_type TYPE TEXT;
ALTER TABLE client_insurance ALTER COLUMN subscriber_relationship TYPE TEXT;
ALTER TABLE client_insurance ALTER COLUMN verified_by TYPE TEXT;

ALTER TABLE client_referrals ALTER COLUMN referral_source TYPE TEXT;
ALTER TABLE client_referrals ALTER COLUMN reporting_frequency TYPE TEXT;
ALTER TABLE client_referrals ALTER COLUMN marketing_campaign TYPE TEXT;
ALTER TABLE client_referrals ALTER COLUMN promo_code TYPE TEXT;

ALTER TABLE client_employment ALTER COLUMN employment_status TYPE TEXT;
ALTER TABLE client_employment ALTER COLUMN education_level TYPE TEXT;
