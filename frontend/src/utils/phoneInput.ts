export const INSURANCE_PHONE_PATTERN = /^[+]?[\d\s\-()]+$/;

export function sanitizeInsurancePhoneInput(value: string): string {
  let sanitized = value.replace(/[^+\d\s\-()]/g, "");
  if (sanitized.includes("+")) {
    sanitized = `+${sanitized.replace(/\+/g, "")}`;
  }
  return sanitized.slice(0, 20);
}

export function isValidInsurancePhone(value?: string | null): boolean {
  const trimmed = value?.trim();
  if (!trimmed) return true;
  return INSURANCE_PHONE_PATTERN.test(trimmed);
}
