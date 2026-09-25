import type {
  PortalFormAssignmentDetail,
  PortalFormContextData,
} from "@/store/api/portalFormsApi";
import { sanitizeHtml } from "@/utils/sanitizeHtml";

const PLACEHOLDER_PATTERN = /\{\{\s*([A-Za-z0-9_]+)\s*\}\}/g;
const HTML_TAG_PATTERN = /<\/?[a-z][\s\S]*>/i;

export function buildPortalFormPlaceholderMap(
  context?: PortalFormContextData | null,
): Record<string, string> {
  const client = context?.clientData;
  const therapist = context?.therapistData;
  const practice = context?.practiceData;

  const clientName = client?.fullName?.trim() ?? "";
  const therapistName = therapist?.fullName?.trim() ?? "";

  return {
    CLIENT_FULL_NAME: clientName,
    CLIENT_NAME: clientName,
    CLIENT_ID: client?.clientId?.trim() ?? "",
    CLIENT_MRN: client?.clientId?.trim() ?? "",
    CLIENT_EMAIL: client?.email?.trim() ?? "",
    CLIENT_PHONE: client?.phone?.trim() ?? "",
    CLIENT_DOB: client?.dateOfBirth?.trim() ?? "",
    THERAPIST_FULL_NAME: therapistName,
    THERAPIST_NAME: therapistName,
    THERAPIST_EMAIL: therapist?.email?.trim() ?? "",
    THERAPIST_PHONE: therapist?.phone?.trim() ?? "",
    PRACTICE_NAME: practice?.name?.trim() ?? "",
    PRACTICE_ADDRESS: practice?.address?.trim() ?? "",
    PRACTICE_PHONE: practice?.phone?.trim() ?? "",
    PRACTICE_EMAIL: practice?.email?.trim() ?? "",
    PRACTICE_WEBSITE: practice?.website?.trim() ?? "",
  };
}

export function applyPortalFormPlaceholders(
  text: string | null | undefined,
  placeholders: Record<string, string>,
): string {
  if (!text) return "";
  return text.replace(PLACEHOLDER_PATTERN, (_match, key: string) => {
    const value = placeholders[key.toUpperCase()];
    return value != null && value !== "" ? value : "";
  });
}

export function portalFormContentLooksLikeHtml(content: string): boolean {
  return HTML_TAG_PATTERN.test(content);
}

export function resolvePortalFormFieldContent(
  text: string | null | undefined,
  placeholders: Record<string, string>,
): { html: string | null; plain: string } {
  const filled = applyPortalFormPlaceholders(text, placeholders);
  if (!filled) {
    return { html: null, plain: "" };
  }
  if (portalFormContentLooksLikeHtml(filled)) {
    return { html: sanitizeHtml(filled), plain: filled };
  }
  return { html: null, plain: filled };
}

export function getPortalFormPlaceholdersFromAssignment(
  assignment?: PortalFormAssignmentDetail | null,
): Record<string, string> {
  return buildPortalFormPlaceholderMap(assignment?.context);
}
