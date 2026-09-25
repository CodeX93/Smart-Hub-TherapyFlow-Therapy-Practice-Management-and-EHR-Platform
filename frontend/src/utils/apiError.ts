import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";

const DEFAULT_ERROR_MESSAGE = "Something went wrong. Please try again.";

function looksLikeHtml(value: string): boolean {
  const trimmed = value.trim().toLowerCase();
  return (
    trimmed.startsWith("<!doctype html") ||
    trimmed.startsWith("<html") ||
    trimmed.includes("<title>") ||
    trimmed.includes("</body>") ||
    trimmed.includes("azure static web apps")
  );
}

function messageForHttpStatus(status: unknown): string | null {
  if (status === 404) {
    return "The requested resource was not found. Please try again.";
  }
  if (status === 429) {
    return "Too many requests. Please wait and try again.";
  }
  if (status === 502 || status === 503 || status === 504) {
    return "The service is temporarily unavailable. Please try again.";
  }
  if (status === "FETCH_ERROR") {
    return "Unable to reach the server. Check your connection and try again.";
  }
  if (status === "PARSING_ERROR" || status === "TIMEOUT_ERROR") {
    return DEFAULT_ERROR_MESSAGE;
  }
  return null;
}

function sanitizeErrorText(value: string, status?: unknown): string {
  const trimmed = value.trim();
  if (!trimmed) {
    return DEFAULT_ERROR_MESSAGE;
  }
  if (looksLikeHtml(trimmed)) {
    return messageForHttpStatus(status) ?? DEFAULT_ERROR_MESSAGE;
  }
  // Avoid dumping huge non-JSON payloads into the UI.
  if (trimmed.length > 400) {
    return messageForHttpStatus(status) ?? DEFAULT_ERROR_MESSAGE;
  }
  return trimmed;
}

function formatFieldValidationMessage(field: string, message: string): string {
  const trimmedField = field.trim();
  const trimmedMessage = message.trim();
  if (!trimmedMessage) return trimmedField;
  if (!trimmedField) return trimmedMessage;

  const normalizedField = trimmedField.toLowerCase();
  const normalizedMessage = trimmedMessage.toLowerCase();
  if (
    normalizedMessage.startsWith(`${normalizedField} `) ||
    normalizedMessage === normalizedField
  ) {
    return trimmedMessage;
  }

  return `${trimmedField} ${trimmedMessage}`;
}

function extractValidationMessages(errors: Record<string, unknown>): string[] {
  const messages: string[] = [];

  for (const [field, value] of Object.entries(errors)) {
    if (typeof value === "string" && value.trim()) {
      messages.push(formatFieldValidationMessage(field, value));
      continue;
    }

    if (Array.isArray(value)) {
      for (const item of value) {
        if (typeof item === "string" && item.trim()) {
          messages.push(formatFieldValidationMessage(field, item));
        }
      }
    }
  }

  return messages;
}

export function getApiErrorMessage(error: unknown): string {
  if (!error) {
    return DEFAULT_ERROR_MESSAGE;
  }

  const baseError = error as FetchBaseQueryError & {
    data?: {
      message?: string;
      error?: string;
      details?: {
        errors?: Record<string, unknown>;
      };
    };
    error?: string;
  };

  const statusMessage = messageForHttpStatus(baseError.status);

  // Rate limits: always show a clear wait message (ignore generic server body).
  if (baseError.status === 429) {
    return statusMessage ?? "Too many requests. Please wait and try again.";
  }

  if (baseError.data && typeof baseError.data === "object") {
    const detailsErrors = baseError.data.details?.errors;
    if (detailsErrors && typeof detailsErrors === "object") {
      const validationMessages = extractValidationMessages(detailsErrors);
      if (validationMessages.length > 0) {
        return validationMessages.join(" ");
      }
    }

    const message = (baseError.data as { message?: string }).message;
    if (typeof message === "string" && message.trim()) {
      return sanitizeErrorText(message, baseError.status);
    }

    const errorMessage = (baseError.data as { error?: string }).error;
    if (typeof errorMessage === "string" && errorMessage.trim()) {
      return sanitizeErrorText(errorMessage, baseError.status);
    }
  }

  if (typeof baseError.data === "string" && baseError.data.trim()) {
    try {
      const parsed = JSON.parse(baseError.data) as {
        message?: string;
        error?: string;
      };
      if (typeof parsed.message === "string" && parsed.message.trim()) {
        return sanitizeErrorText(parsed.message, baseError.status);
      }
      if (typeof parsed.error === "string" && parsed.error.trim()) {
        return sanitizeErrorText(parsed.error, baseError.status);
      }
    } catch {
      return sanitizeErrorText(baseError.data, baseError.status);
    }
  }

  if (statusMessage) {
    return statusMessage;
  }

  if (typeof baseError.error === "string") {
    return sanitizeErrorText(baseError.error, baseError.status);
  }

  if (error instanceof Error) {
    return sanitizeErrorText(error.message, baseError.status);
  }

  return "An unexpected error occurred. Please try again.";
}

export function getApiErrorCode(error: unknown): string | null {
  if (!error || typeof error !== "object") {
    return null;
  }

  const baseError = error as FetchBaseQueryError & {
    data?: {
      code?: string;
      errorCode?: string;
    };
  };

  if (baseError.data && typeof baseError.data === "object") {
    const code = baseError.data.code ?? baseError.data.errorCode;
    if (typeof code === "string" && code.trim()) {
      return code.trim();
    }
  }

  return null;
}

export function isApiErrorCode(error: unknown, code: string): boolean {
  return getApiErrorCode(error) === code;
}

const AUTH_ERROR_MESSAGES: Record<string, string> = {
  TENANT_SELECTION_REQUIRED: "Select an organization to continue.",
  EMAIL_NOT_IN_ORG: "This email is not registered in the selected organization.",
  INVALID_ORG_SELECTION: "Organization not found.",
  USER_BLOCKED_IN_ORG: "This account is blocked for the selected organization.",
};

export function getAuthErrorMessage(error: unknown): string {
  const code = getApiErrorCode(error);
  if (code && AUTH_ERROR_MESSAGES[code]) {
    return AUTH_ERROR_MESSAGES[code];
  }

  return getApiErrorMessage(error);
}

export const AI_PROCESSING_CONSENT_REQUIRED_MESSAGE =
  "AI processing consent is not granted. Record it under Edit Client → Consents.";

export function isAiProcessingConsentRequiredError(error: unknown): boolean {
  if (!error || typeof error !== "object") return false;

  const baseError = error as FetchBaseQueryError;
  if (baseError.status !== 403) return false;

  const data = baseError.data;
  if (!data || typeof data !== "object") return false;

  if ((data as { consentRequired?: boolean }).consentRequired === true) {
    return true;
  }

  const message = String((data as { message?: string }).message ?? "").toLowerCase();
  return (
    message.includes("consent") &&
    (message.includes("ai") || message.includes("processing"))
  );
}

export function getClientReportGenerateErrorMessage(error: unknown): string {
  if (isAiProcessingConsentRequiredError(error)) {
    return AI_PROCESSING_CONSENT_REQUIRED_MESSAGE;
  }
  return getApiErrorMessage(error) || "Failed to generate report";
}
