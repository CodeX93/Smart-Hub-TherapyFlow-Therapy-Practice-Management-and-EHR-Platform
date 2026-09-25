import type { PortalInvoicesQueryParams } from "@/store/api/portalApi";

export interface ClientInvoiceFilters {
  startDate: Date | null;
  endDate: Date | null;
  paymentStatus: string | null;
  insuranceCovered: string | null;
}

export const DEFAULT_CLIENT_INVOICE_FILTERS: ClientInvoiceFilters = {
  startDate: null,
  endDate: null,
  paymentStatus: null,
  insuranceCovered: null,
};

export const CLIENT_INVOICE_STATUS_OPTIONS = [
  { value: "", label: "All Statuses" },
  { value: "unpaid", label: "Pending" },
  { value: "paid", label: "Paid" },
  { value: "partial", label: "Partial" },
  { value: "denied", label: "Denied" },
  { value: "cancelled", label: "Cancelled" },
];

export const CLIENT_INVOICE_INSURANCE_OPTIONS = [
  { value: "", label: "All Coverage" },
  { value: "covered", label: "Insurance covered" },
  { value: "self_pay", label: "Self-pay" },
];

export const PORTAL_INVOICES_PAGE_SIZE = 20;

function formatQueryDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function hasActiveClientInvoiceFilters(
  filters: ClientInvoiceFilters,
): boolean {
  return Boolean(
    filters.startDate ||
      filters.endDate ||
      filters.paymentStatus ||
      filters.insuranceCovered,
  );
}

export function buildPortalInvoicesQueryArgs(
  page: number,
  filters: ClientInvoiceFilters,
  search: string,
  pageSize = PORTAL_INVOICES_PAGE_SIZE,
): PortalInvoicesQueryParams {
  const params: PortalInvoicesQueryParams = {
    page,
    pageSize,
  };

  if (filters.paymentStatus) {
    params.paymentStatus = filters.paymentStatus;
  }

  if (filters.insuranceCovered === "covered") {
    params.insuranceCovered = true;
  } else if (filters.insuranceCovered === "self_pay") {
    params.insuranceCovered = false;
  }

  if (filters.startDate) {
    params.startDate = formatQueryDate(filters.startDate);
  }

  if (filters.endDate) {
    params.endDate = formatQueryDate(filters.endDate);
  }

  const trimmedSearch = search.trim();
  if (trimmedSearch) {
    params.search = trimmedSearch;
  }

  return params;
}
