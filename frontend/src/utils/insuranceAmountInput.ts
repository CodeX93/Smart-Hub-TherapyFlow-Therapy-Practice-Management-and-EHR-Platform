/** Up to 8 digits before decimal, up to 2 after; must be >= 0 when set. */
export const INSURANCE_AMOUNT_PATTERN = /^\d{1,8}(\.\d{1,2})?$/;

export function sanitizeInsuranceAmountInput(value: string): string {
  const cleaned = value.replace(/[^\d.]/g, "");
  const dotIndex = cleaned.indexOf(".");

  if (dotIndex === -1) {
    return cleaned.slice(0, 8);
  }

  const whole = cleaned.slice(0, dotIndex).slice(0, 8);
  const fraction = cleaned
    .slice(dotIndex + 1)
    .replace(/\./g, "")
    .slice(0, 2);

  return `${whole}.${fraction}`;
}

export function isValidInsuranceAmount(value?: string | null): boolean {
  const trimmed = value?.trim();
  if (!trimmed) return true;

  if (!INSURANCE_AMOUNT_PATTERN.test(trimmed)) return false;

  const parsed = Number.parseFloat(trimmed);
  return Number.isFinite(parsed) && parsed >= 0;
}
