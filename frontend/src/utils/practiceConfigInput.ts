export const PRACTICE_CONFIG_FIELD_LIMITS = {
  name: 200,
  subtitle: 200,
  description: 1000,
  address: 500,
  phone: 20,
  email: 254,
  website: 500,
} as const;

export const PRACTICE_PHONE_PATTERN = /^\+?\d+$/;

export function sanitizePracticePhoneInput(value: string): string {
  let cleaned = "";
  for (const char of value) {
    if (char === "+" && cleaned.length === 0) {
      cleaned += "+";
    } else if (/\d/.test(char)) {
      cleaned += char;
    }
  }
  return cleaned.slice(0, PRACTICE_CONFIG_FIELD_LIMITS.phone);
}

export function isValidPracticePhone(value?: string | null): boolean {
  const trimmed = value?.trim();
  if (!trimmed) return true;
  return PRACTICE_PHONE_PATTERN.test(trimmed);
}
