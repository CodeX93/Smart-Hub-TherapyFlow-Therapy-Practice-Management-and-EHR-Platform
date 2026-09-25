-- Normalize mixed-case / aliased option keys and domain values so stats and
-- filters match SystemOptionKeyMatcher (lowercase + hyphen→underscore).

-- 1) Catalog: client_stage "Closed" → "closed"
UPDATE system_options so
SET option_key = 'closed',
    updatedat = CURRENT_TIMESTAMP
FROM option_categories oc
WHERE so.category_id = oc.id
  AND oc.category_key = 'client_stage'
  AND so.option_key = 'Closed'
  AND NOT EXISTS (
      SELECT 1
      FROM system_options existing
      WHERE existing.category_id = so.category_id
        AND existing.option_key = 'closed'
        AND existing.id <> so.id
  );

-- If both "Closed" and "closed" already exist, drop the legacy capitalised key.
DELETE FROM system_options so
USING option_categories oc
WHERE so.category_id = oc.id
  AND oc.category_key = 'client_stage'
  AND so.option_key = 'Closed'
  AND EXISTS (
      SELECT 1
      FROM system_options existing
      WHERE existing.category_id = so.category_id
        AND existing.option_key = 'closed'
  );

-- 2) Clients: stage / status aliases
UPDATE clients
SET stage = LOWER(REPLACE(TRIM(stage), '-', '_'))
WHERE stage IS NOT NULL
  AND stage <> LOWER(REPLACE(TRIM(stage), '-', '_'));

UPDATE clients
SET status = LOWER(REPLACE(TRIM(status), '-', '_'))
WHERE status IS NOT NULL
  AND status <> LOWER(REPLACE(TRIM(status), '-', '_'));

-- ClientHub legacy used status "closed" for file-closed clients; SmartHub key is inactive.
UPDATE clients
SET status = 'inactive'
WHERE LOWER(REPLACE(TRIM(status), '-', '_')) IN ('closed', 'close', 'file_closed');

-- 3) Tasks: status / priority keys
UPDATE tasks
SET status = LOWER(REPLACE(TRIM(status), '-', '_'))
WHERE status IS NOT NULL
  AND status <> LOWER(REPLACE(TRIM(status), '-', '_'));

UPDATE tasks
SET priority = LOWER(REPLACE(TRIM(priority), '-', '_'))
WHERE priority IS NOT NULL
  AND priority <> LOWER(REPLACE(TRIM(priority), '-', '_'));

-- Truncated / typo priority seen in some imports
UPDATE tasks
SET priority = 'urgent'
WHERE LOWER(TRIM(priority)) IN ('urgen', 'urgency');
