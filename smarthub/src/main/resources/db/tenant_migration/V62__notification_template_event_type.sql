-- Persist notification template event separately from channel type (IN_APP/EMAIL/SMS).
ALTER TABLE notification_templates
    ADD COLUMN IF NOT EXISTS event_type varchar(100);

CREATE INDEX IF NOT EXISTS idx_notification_templates_event_type
    ON notification_templates (event_type);

-- Backfill from legacy name pattern: {eventType}_{in_app|email|sms}
UPDATE notification_templates
SET event_type = CASE
    WHEN lower(name) LIKE '%\_in_app' ESCAPE '\' THEN left(name, greatest(length(name) - 7, 0))
    WHEN lower(name) LIKE '%\_email' ESCAPE '\' THEN left(name, greatest(length(name) - 6, 0))
    WHEN lower(name) LIKE '%\_sms' ESCAPE '\' THEN left(name, greatest(length(name) - 4, 0))
    ELSE event_type
END
WHERE event_type IS NULL
   OR btrim(event_type) = '';
