-- Full PHI rollout: re-queue encryption + blind-index backfill for every tenant schema.
-- Jobs cancelled earlier (MindCare-only allow-list) become PENDING again.
-- Seeding of missing jobs still happens at runtime for all orgs when allow-list is empty.

UPDATE public.platform_phi_encryption_backfill_jobs
SET status = 'PENDING',
    last_processed_id = 0,
    processed_count = 0,
    error_count = 0,
    last_error = NULL,
    updated_at = NOW()
WHERE status IN ('CANCELLED', 'FAILED');

UPDATE public.platform_phi_blind_index_backfill_jobs
SET status = 'PENDING',
    last_processed_id = 0,
    processed_count = 0,
    error_count = 0,
    last_error = NULL,
    updated_at = NOW()
WHERE status IN ('CANCELLED', 'FAILED');

COMMENT ON TABLE public.platform_phi_encryption_backfill_jobs IS
    'PHI column encryption backfill — all tenant schemas (full rollout)';
COMMENT ON TABLE public.platform_phi_blind_index_backfill_jobs IS
    'PHI blind-index backfill — all V56+ tenant schemas (full rollout)';
