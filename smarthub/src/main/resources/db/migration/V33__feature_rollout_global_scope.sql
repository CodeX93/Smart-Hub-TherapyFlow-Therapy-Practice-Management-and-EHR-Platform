ALTER TABLE public.feature_rollout_rules
    ALTER COLUMN organisation_id DROP NOT NULL;

ALTER TABLE public.feature_rollout_rules
    DROP CONSTRAINT IF EXISTS chk_feature_rollout_scope;

ALTER TABLE public.feature_rollout_rules
    ADD CONSTRAINT chk_feature_rollout_scope
        CHECK (scope IN ('GLOBAL', 'ORGANISATION', 'THERAPIST'));

ALTER TABLE public.feature_rollout_rules
    DROP CONSTRAINT IF EXISTS chk_feature_rollout_scope_org;

ALTER TABLE public.feature_rollout_rules
    ADD CONSTRAINT chk_feature_rollout_scope_org
        CHECK (
            (scope = 'GLOBAL' AND organisation_id IS NULL AND target_id IS NULL AND target_key IS NULL)
            OR (scope <> 'GLOBAL' AND organisation_id IS NOT NULL)
        );

DROP INDEX IF EXISTS uq_feature_rollout_rules_identity;
DROP INDEX IF EXISTS idx_feature_rollout_rules_identity;

DO $$
BEGIN
    CREATE UNIQUE INDEX IF NOT EXISTS uq_feature_rollout_rules_identity
        ON public.feature_rollout_rules (
            COALESCE(organisation_id, -1),
            feature_key,
            scope,
            COALESCE(target_id, -1),
            COALESCE(target_key, '')
        );
EXCEPTION
    WHEN OTHERS THEN
        CREATE INDEX IF NOT EXISTS idx_feature_rollout_rules_identity
            ON public.feature_rollout_rules (
                COALESCE(organisation_id, -1),
                feature_key,
                scope,
                COALESCE(target_id, -1),
                COALESCE(target_key, '')
            );
END $$;

CREATE INDEX IF NOT EXISTS idx_feature_rollout_global_feature
    ON public.feature_rollout_rules (feature_key, start_at, end_at)
    WHERE scope = 'GLOBAL';
