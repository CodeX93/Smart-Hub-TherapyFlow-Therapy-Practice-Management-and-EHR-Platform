-- V31 introduced per-target_key usage rows and new unique indexes, but the original
-- V1 constraint still enforces one row per (subscription_id, feature_id, period_start)
-- regardless of target_key, which blocks therapist-scoped session usage tracking.
ALTER TABLE feature_usage
    DROP CONSTRAINT IF EXISTS uq_feature_usage_sub_feature_period;
