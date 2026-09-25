WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY job_type, title, message, channel, COALESCE(target_json, '{}')
            ORDER BY created_at ASC, id ASC
        ) AS rn
    FROM public.platform_notification_jobs
    WHERE title = 'Termination warning'
      AND message LIKE 'Organisation % is scheduled for purge on %'
)
DELETE FROM public.platform_notification_jobs j
USING ranked r
WHERE j.id = r.id
  AND r.rn > 1;
