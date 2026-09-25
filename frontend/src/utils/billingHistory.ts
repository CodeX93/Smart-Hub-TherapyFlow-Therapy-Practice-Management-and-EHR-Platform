import { useAccumulatedPages } from "@/hooks/useAccumulatedPages";
import { useCallback, useMemo } from "react";
import type { Invoice } from "@/types/invoice.type";
import type { SessionBillingResponse } from "@/types/sessionBilling.type";
import { formatPaymentMethodLabel } from "@/utils/paymentMethodDisplay";

export const extractBillingItems = (payload: unknown): unknown[] => {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  if (typeof payload !== "object") return [];

  const root = payload as Record<string, unknown>;
  const direct =
    root.items ??
    root.records ??
    root.results ??
    root.data ??
    root.content;

  if (Array.isArray(direct)) return direct;
  if (direct && typeof direct === "object") {
    const nested = direct as Record<string, unknown>;
    if (Array.isArray(nested.items)) return nested.items;
    if (Array.isArray(nested.records)) return nested.records;
    if (Array.isArray(nested.results)) return nested.results;
    if (Array.isArray(nested.content)) return nested.content;
  }
  return [];
};

function formatBillingDate(value: unknown): string {
  if (!value) return "---";
  const parsed = new Date(String(value));
  if (Number.isNaN(parsed.getTime())) return "---";
  return parsed.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function normalizeBillingStatus(value: unknown): Invoice["status"] {
  const normalized = String(value ?? "pending").toLowerCase().replace(/-/g, "_");
  if (normalized === "follow_up" || normalized === "followup") return "follow_up";
  if (
    normalized === "paid" ||
    normalized === "pending" ||
    normalized === "partial" ||
    normalized === "billed" ||
    normalized === "denied" ||
    normalized === "cancelled" ||
    normalized === "refunded"
  ) {
    return normalized;
  }
  return "pending";
}

function resolveBillingPaymentStatus(
  paymentStatus: unknown,
  billingStatus: unknown,
): Invoice["status"] {
  const payment = String(paymentStatus ?? "")
    .toLowerCase()
    .replace(/-/g, "_")
    .trim();
  if (payment === "partial") return "partial";
  if (payment === "paid") return "paid";
  if (payment === "unpaid") return "pending";
  if (payment === "denied") return "denied";
  if (payment === "cancelled" || payment === "canceled") return "cancelled";
  return normalizeBillingStatus(billingStatus);
}

export function formatTotalCollectedSubtext(
  paidRecords?: number | null,
  partialRecords?: number | null,
): string {
  const paid = Number(paidRecords ?? 0);
  const partial = Number(partialRecords ?? 0);
  const paidLabel = `${paid} paid invoice${paid === 1 ? "" : "s"}`;
  const partialLabel = `${partial} partial invoice${partial === 1 ? "" : "s"}`;
  return `${paidLabel} · ${partialLabel}`;
}

export function mapSessionBillingToInvoice(record: SessionBillingResponse): Invoice {
  const amountDue = Number(record.amountDue ?? record.totalAmount ?? 0);
  const clientPaid = Number(record.clientPaidAmount ?? 0);
  const insurancePaid = Number(record.insurancePaidAmount ?? 0);
  const paymentAmount = Number(
    record.paymentAmount ?? clientPaid + insurancePaid,
  );
  const remainingDue = Number(record.remainingDue ?? Math.max(0, amountDue - paymentAmount));
  const originalSubtotal = Number(
    record.originalSubtotalAmount ?? record.totalAmount ?? amountDue,
  );
  const afterPolicy = Number(record.totalAmount ?? amountDue);

  return {
    id: String(record.id),
    clientId: record.clientId ? String(record.clientId) : undefined,
    clientReferenceNumber: record.clientReferenceNumber ? String(record.clientReferenceNumber).trim() : undefined,
    clientMrn: record.clientMrn ? String(record.clientMrn) : undefined,
    client: String(record.clientName ?? "Unknown Client"),
    therapist: String(record.therapistName ?? ""),
    clientType: undefined,
    date: formatBillingDate(record.billingDate ?? record.sessionDate),
    billingDate: record.billingDate ?? null,
    sessionDate: record.sessionDate ?? null,
    sessionStatus: record.sessionStatus ?? null,
    service: String(record.serviceName ?? record.serviceCode ?? "General Session"),
    amount: remainingDue.toFixed(2),
    amountDue: amountDue.toFixed(2),
    originalSubtotal: originalSubtotal.toFixed(2),
    afterPolicyAmount: afterPolicy.toFixed(2),
    remainingDue: remainingDue.toFixed(2),
    paid: paymentAmount.toFixed(2),
    insuranceCovered: Boolean(record.insuranceCovered),
    copay:
      record.copayAmount != null && Number.isFinite(Number(record.copayAmount))
        ? Number(record.copayAmount).toFixed(2)
        : undefined,
    status: resolveBillingPaymentStatus(record.paymentStatus, record.billingStatus),
    paidDate: record.paymentDate ? formatBillingDate(record.paymentDate) : undefined,
    paymentMethod: formatPaymentMethodLabel(
      record.paymentMethod ? String(record.paymentMethod) : null,
    ),
    invoicePolicyId: record.invoicePolicyId ?? null,
    policyName: record.policyName ?? null,
    sessionId: record.sessionId,
    amountTrail: null,
    discountType: record.discountType ?? null,
    discountAmount:
      record.discountAmount != null ? Number(record.discountAmount).toFixed(2) : null,
  };
}

export const mapBillingHistoryToInvoices = (responseArray: unknown[]): Invoice[] =>
  responseArray
    .map((inv) => {
      if (!inv || typeof inv !== "object") return null;
      const record = inv as Record<string, unknown>;
      const mappedId = String(record.billingId ?? record.id ?? "");
      if (!mappedId) return null;

      const amountDue = Number(record.amountDue ?? 0);
      const clientPaid = Number(record.clientPaidAmount ?? 0);
      const insurancePaid = Number(record.insurancePaidAmount ?? 0);
      const paymentAmount = Number(
        record.paymentAmount ?? clientPaid + insurancePaid,
      );
      const remainingDue = Number(
        record.remainingDue ?? Math.max(0, amountDue - paymentAmount),
      );
      const clientId = record.clientId ?? (record.client as { id?: unknown } | undefined)?.id;
      const originalSubtotal = Number(
        record.originalSubtotalAmount ?? record.totalAmount ?? amountDue,
      );
      const afterPolicy = Number(record.totalAmount ?? amountDue);

      return {
        id: mappedId,
        clientId: clientId ? String(clientId) : undefined,
        clientReferenceNumber: record.clientReferenceNumber ? String(record.clientReferenceNumber).trim() : undefined,
        clientMrn: record.clientMrn ? String(record.clientMrn) : undefined,
        client: String(record.clientName ?? "Unknown Client"),
        therapist: String(record.assignedTherapistName ?? record.therapistName ?? ""),
        clientType: record.clientType ? String(record.clientType) : undefined,
        date: formatBillingDate(record.billingDate ?? record.sessionDate),
        billingDate:
          record.billingDate != null ? String(record.billingDate) : null,
        sessionDate:
          record.sessionDate != null ? String(record.sessionDate) : null,
        sessionStatus:
          record.sessionStatus != null ? String(record.sessionStatus) : null,
        service: String(record.serviceName ?? record.serviceCode ?? "General Session"),
        amount: remainingDue.toFixed(2),
        amountDue: amountDue.toFixed(2),
        originalSubtotal: originalSubtotal.toFixed(2),
        afterPolicyAmount: afterPolicy.toFixed(2),
        remainingDue: remainingDue.toFixed(2),
        paid: paymentAmount.toFixed(2),
        insuranceCovered: Boolean(record.insuranceCovered),
        copay:
          record.copayAmount != null && Number.isFinite(Number(record.copayAmount))
            ? Number(record.copayAmount).toFixed(2)
            : undefined,
        status: resolveBillingPaymentStatus(record.paymentStatus, record.billingStatus),
        paidDate: record.paymentDate ? formatBillingDate(record.paymentDate) : undefined,
        paymentMethod: formatPaymentMethodLabel(
          record.paymentMethod ? String(record.paymentMethod) : null,
        ),
        invoicePolicyId:
          typeof record.invoicePolicyId === "number" ? record.invoicePolicyId : null,
        policyName: record.policyName ? String(record.policyName) : null,
        sessionId:
          typeof record.sessionId === "number" ? record.sessionId : undefined,
        amountTrail: null,
        discountType: record.discountType ? String(record.discountType) : null,
        discountAmount:
          record.discountAmount != null
            ? Number(record.discountAmount).toFixed(2)
            : null,
      } as Invoice;
    })
    .filter((entry): entry is Invoice => entry !== null);

function mergeInvoicePages(
  pagesByNumber: Record<number, Invoice[]>,
  upToPage: number,
): Invoice[] {
  const merged: Invoice[] = [];
  const seen = new Set<string>();

  for (let pageNumber = 1; pageNumber <= upToPage; pageNumber += 1) {
    const rows = pagesByNumber[pageNumber];
    if (!rows) continue;

    rows.forEach((row) => {
      if (seen.has(row.id)) return;
      seen.add(row.id);
      merged.push(row);
    });
  }

  return merged;
}

function getBillingRecordId(item: unknown): string {
  if (!item || typeof item !== "object") return "";
  const record = item as Record<string, unknown>;
  return String(record.billingId ?? record.id ?? "");
}

function mergeBillingItemPages(
  pagesByNumber: Record<number, unknown[]>,
  upToPage: number,
): unknown[] {
  const merged: unknown[] = [];
  const seen = new Set<string>();

  for (let pageNumber = 1; pageNumber <= upToPage; pageNumber += 1) {
    const rows = pagesByNumber[pageNumber];
    if (!rows) continue;

    rows.forEach((row) => {
      const id = getBillingRecordId(row);
      if (!id || seen.has(id)) return;
      seen.add(id);
      merged.push(row);
    });
  }

  return merged;
}

function useAccumulatedBillingPages(
  historyResponse: unknown | undefined,
  page: number,
  mapPageItems: (items: unknown[]) => Invoice[],
  scopeKey: string,
) {
  const currentPageRows = useMemo(() => historyResponse === undefined
    ? undefined : mapPageItems(extractBillingItems(historyResponse)), [historyResponse, mapPageItems]);
  const { pages, clearPages, isReadyToLoadMore } = useAccumulatedPages(currentPageRows, page, scopeKey);
  const displayedInvoices = useMemo(() => mergeInvoicePages(pages, page), [pages, page]);
  return { displayedInvoices, clearInvoices: clearPages, isReadyToLoadMore };
}

/** Accumulates raw billing history API items by page (for custom row mappers). */
export function useAccumulatedBillingHistoryItems(
  historyResponse: unknown | undefined,
  page: number,
  scopeKey = "",
) {
  const currentPageItems = useMemo(() => historyResponse === undefined
    ? undefined : extractBillingItems(historyResponse), [historyResponse]);
  const { pages, clearPages, isReadyToLoadMore } = useAccumulatedPages(currentPageItems, page, scopeKey);
  const displayedItems = useMemo(() => mergeBillingItemPages(pages, page), [pages, page]);
  return { displayedItems, clearItems: clearPages, isReadyToLoadMore };
}

/**
 * Accumulates billing list pages by number. Each page is stored synchronously
 * when its current query response arrives so page 2 can never replace page 1.
 */
export function useAccumulatedBillingInvoices(
  historyResponse: unknown | undefined,
  page: number,
  scopeKey = "",
) {
  const mapPageItems = useCallback(
    (items: unknown[]) => {
      return items
        .map((item) => {
          if (!item || typeof item !== "object") return null;
          const record = item as Record<string, unknown>;
          if (record.billingId !== undefined || record.sessionId !== undefined) {
            return mapBillingHistoryToInvoices([item])[0] ?? null;
          }
          return mapSessionBillingToInvoice(item as SessionBillingResponse);
        })
        .filter((entry): entry is Invoice => entry !== null);
    },
    [],
  );

  return useAccumulatedBillingPages(historyResponse, page, mapPageItems, scopeKey);
}

export function useAccumulatedBillingRecords(
  recordsResponse: SpringPageLike | undefined,
  page: number,
  scopeKey = "",
) {
  const mapPageItems = useCallback(
    (items: unknown[]) =>
      items
        .map((item) => {
          if (!item || typeof item !== "object") return null;
          return mapSessionBillingToInvoice(item as SessionBillingResponse);
        })
        .filter((entry): entry is Invoice => entry !== null),
    [],
  );

  return useAccumulatedBillingPages(recordsResponse, page, mapPageItems, scopeKey);
}

interface SpringPageLike {
  content?: SessionBillingResponse[];
}
