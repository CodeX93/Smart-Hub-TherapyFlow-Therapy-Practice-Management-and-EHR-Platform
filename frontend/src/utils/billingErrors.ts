import { getApiErrorMessage } from "@/utils/apiError";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function getErrorStatus(error: unknown): number | undefined {
  if (!isRecord(error)) return undefined;
  const status = error.status;
  if (typeof status === "number") return status;
  if (isRecord(error.data) && typeof error.data.status === "number") {
    return error.data.status;
  }
  return undefined;
}

export function isBillingModuleForbidden(error: unknown): boolean {
  if (getErrorStatus(error) !== 403) return false;
  const message = getApiErrorMessage(error).toLowerCase();
  return (
    message.includes("billing is not included") ||
    message.includes("billing module") ||
    message.includes("upgrade to access billing")
  );
}

export function isStripePaymentsForbidden(error: unknown): boolean {
  if (getErrorStatus(error) !== 403) return false;
  const message = getApiErrorMessage(error).toLowerCase();
  return (
    message.includes("stripe") ||
    message.includes("payment") && message.includes("not included")
  );
}

export function isDuplicatePolicyError(error: unknown): boolean {
  if (getErrorStatus(error) !== 400) return false;
  const message = getApiErrorMessage(error).toLowerCase();
  return message.includes("already exists") || message.includes("duplicate");
}

/** Concurrent record-payment: UI expectedPreviousForSource no longer matches DB. */
export function isStalePaymentStateError(error: unknown): boolean {
  if (getErrorStatus(error) !== 409) return false;
  if (!isRecord(error)) return true;
  const data = error.data;
  if (isRecord(data)) {
    const details = data.details;
    if (isRecord(details) && details.reason === "STALE_PAYMENT_STATE") {
      return true;
    }
    const message = typeof data.message === "string" ? data.message.toLowerCase() : "";
    if (message.includes("payment totals changed") || message.includes("stale")) {
      return true;
    }
  }
  const fallback = getApiErrorMessage(error).toLowerCase();
  return (
    fallback.includes("payment totals changed") ||
    fallback.includes("reload latest totals")
  );
}
