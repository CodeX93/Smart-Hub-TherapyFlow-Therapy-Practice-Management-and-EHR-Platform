ALTER TABLE public.feature_rollout_rules
    DROP CONSTRAINT IF EXISTS chk_feature_rollout_scope;

ALTER TABLE public.feature_rollout_rules
    ADD CONSTRAINT chk_feature_rollout_scope
        CHECK (scope IN ('ORGANISATION', 'THERAPIST'));

DO $$
BEGIN
    CREATE UNIQUE INDEX IF NOT EXISTS uq_feature_rollout_rules_identity
        ON public.feature_rollout_rules (
            organisation_id,
            feature_key,
            scope,
            COALESCE(target_id, -1),
            COALESCE(target_key, '')
        );
EXCEPTION
    WHEN OTHERS THEN
        CREATE INDEX IF NOT EXISTS idx_feature_rollout_rules_identity
            ON public.feature_rollout_rules (
                organisation_id,
                feature_key,
                scope,
                COALESCE(target_id, -1),
                COALESCE(target_key, '')
            );
END $$;
