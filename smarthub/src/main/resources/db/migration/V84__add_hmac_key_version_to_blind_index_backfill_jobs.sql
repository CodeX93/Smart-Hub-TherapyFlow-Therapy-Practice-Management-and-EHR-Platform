-- Add HMAC key version tracking to blind index backfill jobs
-- This enables automatic re-indexing when the HMAC search key is rotated

ALTER TABLE public.platform_phi_blind_index_backfill_jobs
    ADD COLUMN IF NOT EXISTS hmac_key_version VARCHAR(64);

-- Add index for performance when checking key versions
CREATE INDEX IF NOT EXISTS idx_phi_blind_backfill_key_version
    ON public.platform_phi_blind_index_backfill_jobs(hmac_key_version);

-- Add comment explaining the column
COMMENT ON COLUMN public.platform_phi_blind_index_backfill_jobs.hmac_key_version IS
    'SHA-256 hash (first 16 chars) of the HMAC key material used for blind indexes. When this changes, jobs are automatically reset to PENDING for re-indexing.';
