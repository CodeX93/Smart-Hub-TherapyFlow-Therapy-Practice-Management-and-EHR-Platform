export const TENANT_SETTINGS_LIMITS = {
  logoUrl: 500,
  supportEmail: 254,
  supportAddress: 500,
  brandColor: 7,
} as const;

export {
  DATA_RESIDENCY_OPTIONS,
  REGION_OPTIONS,
  TIMEZONE_OPTIONS,
  normalizeSelectValue,
  validateRegionAndDataResidency,
} from "../complianceOptions";

import {
  isValidSelectValue,
  TIMEZONE_OPTIONS,
  validateRegionAndDataResidency,
} from "../complianceOptions";

export const LOCALE_OPTIONS = [
  { value: "en-US", label: "English (United States)" },
  { value: "en-GB", label: "English (United Kingdom)" },
  { value: "en-CA", label: "English (Canada)" },
  { value: "es-ES", label: "Spanish (Spain)" },
  { value: "fr-FR", label: "French (France)" },
] as const;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const HEX_COLOR_PATTERN = /^#([0-9a-fA-F]{6})$/;

export function sanitizeSupportEmail(value: string): string {
  return value.replace(/\s/g, "").slice(0, TENANT_SETTINGS_LIMITS.supportEmail);
}

export function sanitizeLogoUrl(value: string): string {
  return value.trim().slice(0, TENANT_SETTINGS_LIMITS.logoUrl);
}

export function sanitizeSupportAddress(value: string): string {
  return value.slice(0, TENANT_SETTINGS_LIMITS.supportAddress);
}

export function sanitizeBrandColor(value: string): string {
  const trimmed = value.trim().slice(0, TENANT_SETTINGS_LIMITS.brandColor);
  if (!trimmed) return "";
  if (trimmed.startsWith("#")) return trimmed;
  return `#${trimmed}`.slice(0, TENANT_SETTINGS_LIMITS.brandColor);
}

export function validateTenantSettings(values: {
  timezone: string;
  region: string;
  dataResidency: string;
  locale: string;
  logoUrl: string;
  brandPrimaryColor: string;
  brandSecondaryColor: string;
  brandAccentColor: string;
  supportEmail: string;
  supportAddress: string;
}): string | null {
  const timezone = values.timezone.trim();
  if (!timezone) return "Timezone is required.";
  if (!isValidSelectValue(timezone, TIMEZONE_OPTIONS)) {
    return "Select a valid timezone.";
  }

  if (!values.locale.trim()) return "Locale is required.";
  if (!isValidSelectValue(values.locale.trim(), LOCALE_OPTIONS)) {
    return "Select a valid locale.";
  }

  const regionResidencyError = validateRegionAndDataResidency({
    region: values.region,
    dataResidency: values.dataResidency,
  });
  if (regionResidencyError) return regionResidencyError;

  const email = values.supportEmail.trim();
  if (email && !EMAIL_PATTERN.test(email)) {
    return "Enter a valid support email address.";
  }

  for (const color of [
    values.brandPrimaryColor,
    values.brandSecondaryColor,
    values.brandAccentColor,
  ]) {
    const trimmed = color.trim();
    if (trimmed && !HEX_COLOR_PATTERN.test(trimmed)) {
      return "Brand colors must use a valid hex value (e.g. #1A2B3C).";
    }
  }

  return null;
}
