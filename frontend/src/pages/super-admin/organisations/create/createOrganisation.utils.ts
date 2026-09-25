export const CREATE_ORG_FIELD_LIMITS = {
  organisationName: 150,
  tenantSlug: 63,
  industry: 80,
  firstName: 50,
  lastName: 50,
  email: 254,
  trialDaysMax: 365,
  userLimitMax: 999_999,
} as const;

const USER_LIMIT_MAX_DIGITS = String(CREATE_ORG_FIELD_LIMITS.userLimitMax).length;
const TRIAL_DAYS_MAX_DIGITS = String(CREATE_ORG_FIELD_LIMITS.trialDaysMax).length;

export function sanitizeOrganisationName(value: string): string {
  return value.slice(0, CREATE_ORG_FIELD_LIMITS.organisationName);
}

export function sanitizeTenantSlug(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9-]/g, "")
    .replace(/-+/g, "-")
    .slice(0, CREATE_ORG_FIELD_LIMITS.tenantSlug);
}

export function sanitizePersonName(value: string, maxLength: number): string {
  return value
    .replace(/[^A-Za-z\s'-]/g, "")
    .slice(0, maxLength);
}

export function sanitizeEmail(value: string): string {
  return value.replace(/\s/g, "").slice(0, CREATE_ORG_FIELD_LIMITS.email);
}

export function sanitizeTrialDays(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, TRIAL_DAYS_MAX_DIGITS);
  if (!digitsOnly) return "";

  const parsed = Number.parseInt(digitsOnly, 10);
  if (!Number.isFinite(parsed)) return "";
  if (parsed > CREATE_ORG_FIELD_LIMITS.trialDaysMax) {
    return String(CREATE_ORG_FIELD_LIMITS.trialDaysMax);
  }

  return digitsOnly;
}

export function sanitizeUserLimit(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, USER_LIMIT_MAX_DIGITS);
  if (!digitsOnly) return "";

  const parsed = Number.parseInt(digitsOnly, 10);
  if (!Number.isFinite(parsed)) return "";
  if (parsed > CREATE_ORG_FIELD_LIMITS.userLimitMax) {
    return String(CREATE_ORG_FIELD_LIMITS.userLimitMax);
  }

  return digitsOnly;
}
