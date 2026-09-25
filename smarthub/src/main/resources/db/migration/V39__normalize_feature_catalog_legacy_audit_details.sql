UPDATE public.platform_audit_logs
SET details = jsonb_build_object(
        'before', '{}'::jsonb,
        'after', '{}'::jsonb,
        'details',
        CASE
            WHEN details LIKE 'key=%'
                THEN 'Feature ' || split_part(details, '=', 2) || ' updated (legacy audit normalized)'
            ELSE 'Feature catalog event normalized from legacy plain-text audit payload'
        END
    )::text
WHERE action IN ('FEATURE_CATALOG_UPDATED', 'FEATURE_CATALOG_PATCHED')
  AND details IS NOT NULL
  AND details <> ''
  AND details NOT LIKE '{%';

