ALTER TABLE feature_usage
    ADD COLUMN IF NOT EXISTS target_key VARCHAR(64);

DROP INDEX IF EXISTS idx_feature_usage_sub_feature_period;
CREATE UNIQUE INDEX IF NOT EXISTS idx_feature_usage_sub_feature_period
    ON feature_usage(subscription_id, feature_id, period_start, target_key);

DROP INDEX IF EXISTS idx_feature_usage_sub_feature_period_null_target;
CREATE UNIQUE INDEX IF NOT EXISTS idx_feature_usage_sub_feature_period_null_target
    ON feature_usage(subscription_id, feature_id, period_start)
    WHERE target_key IS NULL;
