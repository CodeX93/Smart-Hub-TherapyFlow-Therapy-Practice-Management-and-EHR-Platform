-- MindCare (tenant_53): re-queue PHI backfill for rows left in plaintext.
--
-- Root cause: REENCRYPT_CONTACT_V2 / REENCRYPT_NAME_V2 only matched TFENC:v2d:%
-- and marked COMPLETED after advancing the cursor, leaving legacy plaintext untouched.
-- Clinical column jobs (platform_phi_encryption_backfill_jobs) were CANCELLED earlier.
--
-- Encryption itself is performed by the app schedulers (AES keys live in app config),
-- not in this SQL. This migration only resets job state so the schedulers re-run.

-- 1) Approach C contact/name re-encrypt jobs for MindCare
UPDATE public.platform_phi_blind_index_backfill_jobs
SET status = 'PENDING',
    last_processed_id = 0,
    processed_count = 0,
    error_count = 0,
    last_error = NULL,
    updated_at = now()
WHERE schema_name = 'tenant_53'
  AND job_kind IN ('REENCRYPT_CONTACT_V2', 'REENCRYPT_NAME_V2');

-- 2) Clinical plaintext columns still present on MindCare after cancelled Wave A/B/1 jobs
UPDATE public.platform_phi_encryption_backfill_jobs
SET status = 'PENDING',
    last_processed_id = 0,
    processed_count = 0,
    error_count = 0,
    last_error = NULL,
    updated_at = now()
WHERE schema_name = 'tenant_53'
  AND (
        (table_name = 'client_contacts' AND column_name = 'contact_value')
     OR (table_name = 'client_referrals' AND column_name = 'referrer_name')
     OR (table_name = 'patient_consents' AND column_name = 'notes')
     OR (table_name = 'client_history' AND column_name IN (
            'from_value', 'to_value', 'description', 'created_by_name'
         ))
  );
