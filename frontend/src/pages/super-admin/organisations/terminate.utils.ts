// The API refuses anything outside this range (SuperAdminOperationsService
// rejects null and 30 > days > 365 with RETENTION_OUT_OF_RANGE), so the form
// has to know the bounds rather than discovering them from a failed request.
export const RETENTION_DAYS_MIN = 30;
export const RETENTION_DAYS_MAX = 365;
export const RETENTION_DAYS_DEFAULT = String(RETENTION_DAYS_MIN);

const RETENTION_DAYS_MAX_DIGITS = String(RETENTION_DAYS_MAX).length;

export function sanitizeRetentionDaysInput(value: string): string {
  return value.replace(/\D/g, "").slice(0, RETENTION_DAYS_MAX_DIGITS);
}

export function validateRetentionDaysInput(value: string): string | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return "Retention days is required.";
  }

  if (!/^\d+$/.test(trimmed)) {
    return "Retention days must be a whole number.";
  }

  const parsed = Number.parseInt(trimmed, 10);
  if (
    !Number.isFinite(parsed) ||
    parsed < RETENTION_DAYS_MIN ||
    parsed > RETENTION_DAYS_MAX
  ) {
    return `Retention days must be between ${RETENTION_DAYS_MIN} and ${RETENTION_DAYS_MAX}.`;
  }

  return null;
}
