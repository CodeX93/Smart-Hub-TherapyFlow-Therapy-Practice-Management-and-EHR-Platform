-- Normalize legacy enum-style priority values to configurable option keys.

UPDATE tasks
SET priority = LOWER(REPLACE(priority, '-', '_'))
WHERE priority IS NOT NULL;

UPDATE clients
SET follow_up_priority = LOWER(REPLACE(follow_up_priority, '-', '_'))
WHERE follow_up_priority IS NOT NULL;

ALTER TABLE tasks
    ALTER COLUMN priority TYPE VARCHAR(100);

ALTER TABLE clients
    ALTER COLUMN follow_up_priority TYPE VARCHAR(100);
