-- MindCare: re-open CLIENT_DIGESTS so DOB blind indexes populate after V58,
-- and cancel non-MindCare clinical backfill PENDING jobs (local pilot allow-list).

UPDATE public.platform_phi_blind_index_backfill_jobs
SET status = 'PENDING',
    last_processed_id = 0,
    processed_count = 0,
    error_count = 0,
    last_error = NULL,
    updated_at = now()
WHERE schema_name = 'tenant_53'
  AND job_kind = 'CLIENT_DIGESTS';

UPDATE public.platform_phi_encryption_backfill_jobs
SET status = 'CANCELLED',
    last_error = COALESCE(last_error, '') || ' | cancelled: schema not in MindCare allow-list (V81)',
    updated_at = now()
WHERE schema_name <> 'tenant_53'
  AND status IN ('PENDING', 'IN_PROGRESS');

UPDATE public.platform_phi_encryption_backfill_jobs
SET status = 'PENDING',
    last_processed_id = 0,
    processed_count = 0,
    error_count = 0,
    last_error = NULL,
    updated_at = timestamptz '2020-01-01 00:00:00+00'
WHERE schema_name = 'tenant_53'
  AND table_name = 'clients'
  AND column_name IN (
      'date_of_birth', 'gender', 'marital_status', 'preferred_language', 'pronouns',
      'client_type', 'service_type', 'service_frequency', 'treatment_modality'
  )
  AND status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED');
