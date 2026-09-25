-- TENANT
-- Preserve service list price before invoice-policy adjustment.

ALTER TABLE session_billing
    ADD COLUMN IF NOT EXISTS original_rate_per_unit NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS original_subtotal_amount NUMERIC(10, 2);

-- Best-effort backfill: when a policy was applied, recover list price from the linked service.
UPDATE session_billing sb
SET
    original_rate_per_unit = s.base_rate,
    original_subtotal_amount = ROUND(s.base_rate * COALESCE(sb.units, 1), 2)
FROM sessions sess
JOIN services s ON s.id = sess.service_id AND s.is_deleted = FALSE
WHERE sb.session_id = sess.id
  AND sb.invoice_policy_id IS NOT NULL
  AND sb.original_subtotal_amount IS NULL
  AND s.base_rate IS NOT NULL;

-- No policy: original equals the stored (post-policy = list) amounts.
UPDATE session_billing
SET
    original_rate_per_unit = COALESCE(original_rate_per_unit, rate_per_unit),
    original_subtotal_amount = COALESCE(original_subtotal_amount, total_amount)
WHERE original_subtotal_amount IS NULL;
