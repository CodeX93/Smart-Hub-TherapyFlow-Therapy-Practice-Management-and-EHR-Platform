-- Prepare tenant PHI columns for AES-GCM ciphertext (TFENC:v2:...).
-- Randomized ciphertext cannot participate in uniqueness on plaintext values.

-- Clients: widen full_name for ciphertext
ALTER TABLE clients
    ALTER COLUMN full_name TYPE TEXT;

-- Contacts: drop unique/index on encrypted value
ALTER TABLE client_contacts
    DROP CONSTRAINT IF EXISTS uk_client_contact_unique;
DROP INDEX IF EXISTS idx_client_contact_value;
ALTER TABLE client_contacts
    ALTER COLUMN contact_person_name TYPE TEXT;

-- Addresses: widen short columns used for encrypted PHI; drop postal index
DROP INDEX IF EXISTS idx_client_address_postal_code;
ALTER TABLE client_addresses
    ALTER COLUMN city TYPE TEXT,
    ALTER COLUMN state_province TYPE TEXT,
    ALTER COLUMN postal_code TYPE TEXT,
    ALTER COLUMN country TYPE TEXT,
    ALTER COLUMN state_legacy TYPE TEXT,
    ALTER COLUMN zip_code_legacy TYPE TEXT;

-- Insurance: drop unique/indexes on encrypted identifiers; widen short columns
ALTER TABLE client_insurance
    DROP CONSTRAINT IF EXISTS uk_client_insurance_policy;
DROP INDEX IF EXISTS idx_client_insurance_provider;
DROP INDEX IF EXISTS idx_client_insurance_policy;
ALTER TABLE client_insurance
    ALTER COLUMN subscriber_name TYPE TEXT,
    ALTER COLUMN insurance_phone TYPE TEXT,
    ALTER COLUMN insurance_email TYPE TEXT,
    ALTER COLUMN authorization_number TYPE TEXT;
