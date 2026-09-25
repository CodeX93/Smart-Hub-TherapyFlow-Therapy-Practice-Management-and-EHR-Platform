import { formatDateOnly } from "@/utils/transformer/dates.transformer";
import { endOfMonth, startOfMonth } from "date-fns";
import type { BillingFilters } from "@/types/billing.type";

export function createEmptyBillingFilters(): BillingFilters {
  return {
    startDate: null,
    endDate: null,
    paymentStatus: null,
    billingStatus: null,
    clientId: null,
    therapistId: null,
    serviceCode: null,
    clientType: null,
    sessionType: null,
    paymentMethod: null,
    minAmount: null,
    maxAmount: null,
  };
}

/** Default billing overview filter: first and last day of the current local month. */
export function createCurrentMonthBillingFilters(): BillingFilters {
  const now = new Date();
  return {
    ...createEmptyBillingFilters(),
    startDate: startOfMonth(now),
    endDate: endOfMonth(now),
  };
}

/** Shared scope for the records query and its summary cards. */
export function buildBillingQueryFilters(filters: BillingFilters, search: string) {
  return {
    startDate: filters.startDate ? formatDateOnly(filters.startDate) : undefined,
    endDate: filters.endDate ? formatDateOnly(filters.endDate) : undefined,
    status: filters.billingStatus || undefined,
    paymentStatus: filters.paymentStatus || undefined,
    paymentMethod: filters.paymentMethod || undefined,
    serviceCode: filters.serviceCode || undefined,
    clientType: filters.clientType || undefined,
    sessionType: filters.sessionType || undefined,
    minAmount: filters.minAmount ?? undefined,
    maxAmount: filters.maxAmount ?? undefined,
    clientId: filters.clientId || undefined,
    therapistId: filters.therapistId || undefined,
    clientSearch: search.trim() || undefined,
  };
}
