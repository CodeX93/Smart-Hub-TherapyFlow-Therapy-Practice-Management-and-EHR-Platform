import {
  CREATE_ORG_FIELD_LIMITS,
  sanitizeEmail,
  sanitizeOrganisationName,
} from "../../../create/createOrganisation.utils";

export const GENERAL_INFO_LIMITS = {
  organisationName: CREATE_ORG_FIELD_LIMITS.organisationName,
  supportEmail: CREATE_ORG_FIELD_LIMITS.email,
} as const;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export { sanitizeOrganisationName };

export function sanitizeSupportEmail(value: string): string {
  return sanitizeEmail(value);
}

export function validateGeneralInformation(values: {
  organisationName: string;
  supportEmail: string;
}): string | null {
  const organisationName = values.organisationName.trim();
  if (!organisationName) return "Organization name is required.";
  if (organisationName.length > GENERAL_INFO_LIMITS.organisationName) {
    return "Organization name is too long.";
  }

  const supportEmail = values.supportEmail.trim();
  if (supportEmail.length > GENERAL_INFO_LIMITS.supportEmail) {
    return "Support email is too long.";
  }
  if (supportEmail && !EMAIL_PATTERN.test(supportEmail)) {
    return "Enter a valid support email address.";
  }

  return null;
}
