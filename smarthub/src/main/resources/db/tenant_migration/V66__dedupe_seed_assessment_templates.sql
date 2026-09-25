-- Soft-delete seeded placeholder assessment templates that duplicate a
-- ClientHub-migrated template of the same name (seed v1 + migrated v2).
-- Also covers the known default seed description ("lorem ipsum dolor sit")
-- when another active template with the same name already exists.

DO $$
BEGIN
    IF to_regclass('public.clienthub_legacy_id_mappings') IS NULL THEN
        RETURN;
    END IF;

    -- Prefer keeping a ClientHub-mapped template when one exists for the name.
    WITH mapped AS (
        SELECT m.target_id
        FROM public.clienthub_legacy_id_mappings m
        WHERE m.source_system = 'ClientHubAI'
          AND m.entity_name = 'assessment_templates'
          AND m.target_schema = current_schema()
    ),
    dup_names AS (
        SELECT t.name
        FROM assessment_templates t
        WHERE COALESCE(t.is_deleted, false) = false
        GROUP BY t.name
        HAVING COUNT(*) > 1
    ),
    keepers AS (
        SELECT DISTINCT ON (t.name) t.id AS keep_id, t.name
        FROM assessment_templates t
        JOIN dup_names d ON d.name = t.name
        WHERE COALESCE(t.is_deleted, false) = false
          AND t.id IN (SELECT target_id FROM mapped)
        ORDER BY t.name, t.version_number DESC NULLS LAST, t.id DESC
    ),
    dupes AS (
        SELECT t.id AS dupe_id, k.keep_id
        FROM assessment_templates t
        JOIN keepers k ON k.name = t.name
        WHERE COALESCE(t.is_deleted, false) = false
          AND t.id <> k.keep_id
          AND t.id NOT IN (SELECT target_id FROM mapped)
    )
    UPDATE assessment_assignments a
    SET template_id = d.keep_id,
        updatedat = NOW(),
        updated_by = 0
    FROM dupes d
    WHERE a.template_id = d.dupe_id;

    WITH mapped AS (
        SELECT m.target_id
        FROM public.clienthub_legacy_id_mappings m
        WHERE m.source_system = 'ClientHubAI'
          AND m.entity_name = 'assessment_templates'
          AND m.target_schema = current_schema()
    ),
    dup_names AS (
        SELECT t.name
        FROM assessment_templates t
        WHERE COALESCE(t.is_deleted, false) = false
        GROUP BY t.name
        HAVING COUNT(*) > 1
    ),
    keepers AS (
        SELECT DISTINCT ON (t.name) t.id AS keep_id, t.name
        FROM assessment_templates t
        JOIN dup_names d ON d.name = t.name
        WHERE COALESCE(t.is_deleted, false) = false
          AND t.id IN (SELECT target_id FROM mapped)
        ORDER BY t.name, t.version_number DESC NULLS LAST, t.id DESC
    ),
    dupes AS (
        SELECT t.id AS dupe_id
        FROM assessment_templates t
        JOIN keepers k ON k.name = t.name
        WHERE COALESCE(t.is_deleted, false) = false
          AND t.id <> k.keep_id
          AND t.id NOT IN (SELECT target_id FROM mapped)
    )
    UPDATE assessment_templates t
    SET is_deleted = true,
        deleted_at = NOW(),
        is_active = false,
        updatedat = NOW(),
        updated_by = 0
    FROM dupes d
    WHERE t.id = d.dupe_id;
END $$;

-- Fallback: seed placeholder description without requiring a legacy mapping row.
WITH keepers AS (
    SELECT DISTINCT ON (t.name) t.id AS keep_id, t.name
    FROM assessment_templates t
    WHERE COALESCE(t.is_deleted, false) = false
      AND COALESCE(btrim(t.description), '') <> 'lorem ipsum dolor sit'
      AND EXISTS (
          SELECT 1
          FROM assessment_templates s
          WHERE s.name = t.name
            AND COALESCE(s.is_deleted, false) = false
            AND COALESCE(btrim(s.description), '') = 'lorem ipsum dolor sit'
            AND s.id <> t.id
      )
    ORDER BY t.name, t.version_number DESC NULLS LAST, t.id DESC
),
dupes AS (
    SELECT s.id AS dupe_id, k.keep_id
    FROM assessment_templates s
    JOIN keepers k ON k.name = s.name
    WHERE COALESCE(s.is_deleted, false) = false
      AND COALESCE(btrim(s.description), '') = 'lorem ipsum dolor sit'
)
UPDATE assessment_assignments a
SET template_id = d.keep_id,
    updatedat = NOW(),
    updated_by = 0
FROM dupes d
WHERE a.template_id = d.dupe_id;

WITH keepers AS (
    SELECT DISTINCT ON (t.name) t.id AS keep_id, t.name
    FROM assessment_templates t
    WHERE COALESCE(t.is_deleted, false) = false
      AND COALESCE(btrim(t.description), '') <> 'lorem ipsum dolor sit'
      AND EXISTS (
          SELECT 1
          FROM assessment_templates s
          WHERE s.name = t.name
            AND COALESCE(s.is_deleted, false) = false
            AND COALESCE(btrim(s.description), '') = 'lorem ipsum dolor sit'
            AND s.id <> t.id
      )
    ORDER BY t.name, t.version_number DESC NULLS LAST, t.id DESC
),
dupes AS (
    SELECT s.id AS dupe_id
    FROM assessment_templates s
    JOIN keepers k ON k.name = s.name
    WHERE COALESCE(s.is_deleted, false) = false
      AND COALESCE(btrim(s.description), '') = 'lorem ipsum dolor sit'
)
UPDATE assessment_templates t
SET is_deleted = true,
    deleted_at = NOW(),
    is_active = false,
    updatedat = NOW(),
    updated_by = 0
FROM dupes d
WHERE t.id = d.dupe_id;
