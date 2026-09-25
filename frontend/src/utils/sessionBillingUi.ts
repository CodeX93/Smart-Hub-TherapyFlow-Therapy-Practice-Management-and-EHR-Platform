import { DateTime } from "luxon";

export function normalizeSessionStatusKey(status: string): string {
  return status.trim().toLowerCase().replace(/[\s_-]+/g, "_");
}

export function isBillableSessionStatus(status: string): boolean {
  const normalized = normalizeSessionStatusKey(status);
  return ["completed", "no_show", "noshow"].includes(normalized);
}

export function isSessionEligibleForManualBilling(session: {
  status?: string | null;
  sessionDate?: string | Date | null;
  billingId?: number | null;
  hasInvoice?: boolean | null;
}, now: Date = new Date(), practiceTimezone?: string | null): boolean {
  if (!isBillableSessionStatus(session.status ?? "")) return false;
  if (session.billingId != null || session.hasInvoice === true) return false;
  if (!session.sessionDate) return false;
  if (session.sessionDate instanceof Date) {
    return !Number.isNaN(session.sessionDate.getTime()) && now.getTime() > session.sessionDate.getTime();
  }
  const value = session.sessionDate.trim();
  const hasExplicitOffset = /(?:z|[+-]\d{2}:?\d{2})$/i.test(value);
  const scheduled = DateTime.fromISO(value, hasExplicitOffset
    ? { setZone: true }
    : { zone: practiceTimezone?.trim() || "UTC" });
  return scheduled.isValid && now.getTime() > scheduled.toMillis();
}

export function parseRemainingDue(invoice: {
  remainingDue?: string | number | null;
  amount?: string | number | null;
  paid?: string | number | null;
}): number {
  if (invoice.remainingDue !== undefined && invoice.remainingDue !== null) {
    const parsed = Number(invoice.remainingDue);
    if (Number.isFinite(parsed)) return parsed;
  }

  const amount = Number(invoice.amount ?? 0);
  const paid = Number(invoice.paid ?? 0);
  if (!Number.isFinite(amount) || !Number.isFinite(paid)) return 0;
  return Math.max(amount - paid, 0);
}

export function canRecordBillingPayment(invoice: {
  status: string;
  remainingDue?: string | number | null;
  amount?: string | number | null;
  paid?: string | number | null;
}): boolean {
  if (invoice.status === "paid" || invoice.status === "denied" || invoice.status === "cancelled") {
    return false;
  }
  return parseRemainingDue(invoice) > 0;
}

/** Match ClientHub: show Copay when the bill is insurance-covered (or has a known copay). */
export function shouldShowCopayBadge(invoice: {
  insuranceCovered?: boolean | null;
  copay?: string | number | null;
}): boolean {
  if (invoice.insuranceCovered) return true;
  if (invoice.copay == null || invoice.copay === "") return false;
  const amount = Number(invoice.copay);
  return Number.isFinite(amount) && amount > 0;
}

function formatBillingMoney(value: string | number | null | undefined): string | null {
  if (value === null || value === undefined || value === "") return null;
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    const asString = String(value).replace(/^\$/, "").trim();
    return asString || null;
  }
  return numeric.toFixed(2);
}

/**
 * Amount column: actual bill amount on top; remaining due only when partially paid.
 * With no partial balance, show a single amount (do not stack duplicates).
 */
export function getBillingAmountDisplay(invoice: {
  amountDue?: string | number | null;
  remainingDue?: string | number | null;
  amount?: string | number | null;
  status?: string | null;
}): { primary: string; partialRemaining: string | null } {
  const primary =
    formatBillingMoney(invoice.amountDue) ??
    formatBillingMoney(invoice.amount) ??
    formatBillingMoney(invoice.remainingDue) ??
    "0.00";
  const remaining = formatBillingMoney(invoice.remainingDue ?? invoice.amount);
  const isPartial = (invoice.status ?? "").trim().toLowerCase() === "partial";
  const showPartialRemaining =
    isPartial && remaining != null && remaining !== primary;

  return {
    primary,
    partialRemaining: showPartialRemaining ? remaining : null,
  };
}

export function canShowSessionPayNow(session: {
  status?: string | null;
  billingId?: number | null;
  remainingDue?: number | null;
  invoicePaid?: boolean | null;
  paymentStatus?: string | null;
}): boolean {
  if (normalizeSessionStatusKey(session.status ?? "") !== "completed") return false;
  if (!session.billingId) return false;
  if (session.invoicePaid === true) return false;
  const payment = (session.paymentStatus ?? "").trim().toLowerCase();
  if (["paid", "denied", "cancelled", "canceled"].includes(payment)) return false;
  if (session.remainingDue != null && Number.isFinite(Number(session.remainingDue))) {
    return Number(session.remainingDue) > 0;
  }
  return true;
}
