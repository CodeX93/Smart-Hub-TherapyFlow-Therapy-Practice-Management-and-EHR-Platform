const USAGE_KEY_HINTS = ["LIMIT", "PER_MONTH", "TEMPLATES", "_GB"];

export function isUsageKey(keyName: string): boolean {
  return USAGE_KEY_HINTS.some((hint) => keyName.includes(hint));
}

export const FEATURE_OVERRIDE_LIMITS = {
  usageLimitMin: 0,
  usageLimitMax: 999_999,
} as const;

export const FEATURE_USAGE_LIMIT_MAX_DIGITS = String(
  FEATURE_OVERRIDE_LIMITS.usageLimitMax,
).length;

export function sanitizeUsageLimitOverride(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, FEATURE_USAGE_LIMIT_MAX_DIGITS);
  if (!digitsOnly) return "";

  const parsed = Number.parseInt(digitsOnly, 10);
  if (!Number.isFinite(parsed)) return "";
  if (parsed > FEATURE_OVERRIDE_LIMITS.usageLimitMax) {
    return String(FEATURE_OVERRIDE_LIMITS.usageLimitMax);
  }

  return digitsOnly;
}

export function parseUsageLimitOverride(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) return null;

  const parsed = Number.parseInt(trimmed, 10);
  if (!Number.isFinite(parsed)) return null;
  if (
    parsed < FEATURE_OVERRIDE_LIMITS.usageLimitMin ||
    parsed > FEATURE_OVERRIDE_LIMITS.usageLimitMax
  ) {
    return null;
  }

  return parsed;
}

export function validateUsageLimitOverrideValue(
  value: string,
  fieldLabel: string,
): string | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return `Usage limit is required for ${fieldLabel}.`;
  }

  if (!/^\d+$/.test(trimmed)) {
    return `Enter a valid number for ${fieldLabel}.`;
  }

  const parsed = parseUsageLimitOverride(trimmed);
  if (parsed === null) {
    return `${fieldLabel} must be between ${FEATURE_OVERRIDE_LIMITS.usageLimitMin} and ${FEATURE_OVERRIDE_LIMITS.usageLimitMax.toLocaleString()}.`;
  }

  return null;
}

export function validateEnabledUsageLimits(
  moduleRows: Array<{ keyName: string; overrideEnabled: boolean }>,
  limitRows: Array<{ keyName: string; overrideValue: string; title: string }>,
): string | null {
  const enabledUsageKeys = moduleRows
    .filter((row) => row.overrideEnabled && isUsageKey(row.keyName))
    .map((row) => row.keyName);

  for (const keyName of enabledUsageKeys) {
    const usageRow = limitRows.find((row) => row.keyName === keyName);
    const fieldLabel = usageRow?.title || keyName;
    if (!usageRow) {
      return `Usage limit is required for ${fieldLabel}.`;
    }

    const validationError = validateUsageLimitOverrideValue(
      usageRow.overrideValue,
      fieldLabel,
    );
    if (validationError) return validationError;
  }

  for (const row of limitRows) {
    if (!row.overrideValue.trim()) continue;

    const validationError = validateUsageLimitOverrideValue(
      row.overrideValue,
      row.title || row.keyName,
    );
    if (validationError) return validationError;
  }

  return null;
}
