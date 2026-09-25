import { baseApi } from "../baseApi";
import type { SystemOptionValue } from "./systemOptions.api";
import type { InvoicePolicyPriceType } from "@/types/sessionBilling.type";

export interface InvoicePolicyResponse {
  id: number;
  clientTypeKey: string;
  clientTypeLabel: string;
  appointmentStatusKey: string;
  appointmentStatusLabel: string;
  enabled: boolean;
  priceType: InvoicePolicyPriceType;
  invoicePrice: number;
  policyName?: string | null;
  serviceId?: number | null;
  serviceScopeKey?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  priority?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface InvoicePolicyRequest {
  clientTypeKey: string;
  clientTypeLabel: string;
  appointmentStatusKey: string;
  appointmentStatusLabel: string;
  enabled: boolean;
  priceType: InvoicePolicyPriceType;
  invoicePrice: number;
  policyName?: string | null;
  serviceId?: number | null;
  serviceScopeKey?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  priority?: number | null;
}

export interface InvoicePolicyServiceOption {
  serviceId: number | null;
  optionKey: string;
  optionLabel: string;
  allServices?: boolean;
}

export interface InvoicePolicyScopeOption {
  optionKey: string;
  optionLabel: string;
}

const ALL_SCOPE_KEY = "all";

function asString(value: unknown): string {
  return typeof value === "string" ? value : value == null ? "" : String(value);
}

function asNumber(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function unwrapOptionList(payload: unknown): unknown[] {
  if (Array.isArray(payload)) return payload;
  if (!payload || typeof payload !== "object") return [];

  const root = payload as Record<string, unknown>;
  for (const key of ["items", "data", "content", "options", "results"] as const) {
    const value = root[key];
    if (Array.isArray(value)) return value;
  }

  return [];
}

function isAllScopeKey(value: string | null | undefined): boolean {
  return (value ?? "").trim().toLowerCase() === ALL_SCOPE_KEY;
}

function normalizeScopeOption(entry: unknown): InvoicePolicyScopeOption | null {
  if (!entry || typeof entry !== "object") return null;
  const row = entry as Record<string, unknown>;

  const optionKey = asString(
    row.optionKey ?? row.key ?? row.value ?? row.code,
  ).trim();
  const optionLabel = asString(
    row.optionLabel ?? row.label ?? row.name ?? row.displayName,
  ).trim();

  if (!optionKey && !optionLabel) return null;

  const resolvedKey = optionKey || ALL_SCOPE_KEY;
  const resolvedLabel =
    optionLabel ||
    (isAllScopeKey(resolvedKey) ? "All" : resolvedKey);

  return {
    optionKey: resolvedKey,
    optionLabel: resolvedLabel,
  };
}

function ensureAllScopeOption(
  options: InvoicePolicyScopeOption[],
  allLabel: string,
): InvoicePolicyScopeOption[] {
  const withoutAll = options.filter((option) => !isAllScopeKey(option.optionKey));
  return [
    {
      optionKey: ALL_SCOPE_KEY,
      optionLabel: allLabel,
    },
    ...withoutAll,
  ];
}

function normalizeScopeOptions(
  payload: unknown,
  allLabel: string,
): InvoicePolicyScopeOption[] {
  const options = unwrapOptionList(payload)
    .map((entry) => normalizeScopeOption(entry))
    .filter(Boolean) as InvoicePolicyScopeOption[];

  return ensureAllScopeOption(options, allLabel);
}

function toSystemOptionValues(
  options: InvoicePolicyScopeOption[],
): SystemOptionValue[] {
  return options.map((option, index) => ({
    id: index + 1,
    categoryId: 0,
    categoryKey: "",
    categoryName: "",
    optionKey: option.optionKey,
    optionLabel: option.optionLabel,
    sortOrder: index,
    isDefault: isAllScopeKey(option.optionKey),
    isSystem: true,
    isActive: true,
    price: 0,
  }));
}

function normalizeServiceOption(entry: unknown): InvoicePolicyServiceOption | null {
  if (!entry || typeof entry !== "object") return null;
  const row = entry as Record<string, unknown>;

  const rawOptionKey = asString(
    row.optionKey ?? row.key ?? row.value ?? row.code,
  ).trim();
  const optionLabel = asString(
    row.optionLabel ?? row.label ?? row.serviceName ?? row.name,
  ).trim();
  const allServices =
    Boolean(row.allServices) ||
    isAllScopeKey(rawOptionKey) ||
    (row.serviceId === null && Boolean(optionLabel));

  const rawServiceId = row.serviceId ?? row.id;
  const serviceId =
    rawServiceId === null || rawServiceId === undefined || rawServiceId === ""
      ? null
      : asNumber(rawServiceId);

  if (allServices || serviceId == null) {
    if (!allServices && !rawOptionKey && !optionLabel) return null;
    return {
      serviceId: null,
      optionKey: ALL_SCOPE_KEY,
      optionLabel: optionLabel || "All services",
      allServices: true,
    };
  }

  if (!Number.isFinite(serviceId) || serviceId <= 0) return null;

  const serviceCode = asString(row.serviceCode).trim();
  const serviceName = asString(row.serviceName ?? row.name).trim();
  const resolvedLabel =
    optionLabel ||
    (serviceCode && serviceName
      ? `${serviceCode} — ${serviceName}`
      : serviceName || serviceCode || `Service #${serviceId}`);

  return {
    serviceId,
    optionKey: rawOptionKey || String(serviceId),
    optionLabel: resolvedLabel,
    allServices: false,
  };
}

function ensureAllServiceOption(
  options: InvoicePolicyServiceOption[],
): InvoicePolicyServiceOption[] {
  const withoutAll = options.filter(
    (option) => !(option.allServices || isAllScopeKey(option.optionKey) || option.serviceId == null),
  );

  return [
    {
      serviceId: null,
      optionKey: ALL_SCOPE_KEY,
      optionLabel: "All services",
      allServices: true,
    },
    ...withoutAll,
  ];
}

function normalizeServiceOptions(payload: unknown): InvoicePolicyServiceOption[] {
  const options = unwrapOptionList(payload)
    .map((entry) => normalizeServiceOption(entry))
    .filter(Boolean) as InvoicePolicyServiceOption[];

  return ensureAllServiceOption(options);
}

export const invoicePolicyApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getInvoicePolicies: builder.query<InvoicePolicyResponse[], void>({
      query: () => ({
        url: "/api/v1/billing/invoice-policies",
        method: "GET",
      }),
      providesTags: ["InvoicePolicies"],
    }),
    getInvoicePolicyById: builder.query<InvoicePolicyResponse, number>({
      query: (id) => ({
        url: `/api/v1/billing/invoice-policies/${id}`,
        method: "GET",
      }),
      providesTags: (_result, _error, id) => [{ type: "InvoicePolicies", id }],
    }),
    getInvoicePolicyClientTypes: builder.query<SystemOptionValue[], void>({
      query: () => ({
        url: "/api/v1/billing/invoice-policies/options/client-types",
        method: "GET",
      }),
      transformResponse: (payload: unknown) =>
        toSystemOptionValues(
          normalizeScopeOptions(payload, "All client types"),
        ),
    }),
    getInvoicePolicySessionStatuses: builder.query<SystemOptionValue[], void>({
      query: () => ({
        url: "/api/v1/billing/invoice-policies/options/appointment-statuses",
        method: "GET",
      }),
      transformResponse: (payload: unknown) =>
        toSystemOptionValues(
          normalizeScopeOptions(payload, "All session statuses"),
        ),
    }),
    getInvoicePolicyServices: builder.query<InvoicePolicyServiceOption[], void>({
      query: () => ({
        url: "/api/v1/billing/invoice-policies/options/services",
        method: "GET",
      }),
      transformResponse: (payload: unknown) => normalizeServiceOptions(payload),
    }),
    createInvoicePolicy: builder.mutation<InvoicePolicyResponse, InvoicePolicyRequest>({
      query: (body) => ({
        url: "/api/v1/billing/invoice-policies",
        method: "POST",
        body,
      }),
      invalidatesTags: ["InvoicePolicies"],
    }),
    updateInvoicePolicy: builder.mutation<
      InvoicePolicyResponse,
      { id: number; body: InvoicePolicyRequest }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/billing/invoice-policies/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: ["InvoicePolicies"],
    }),
    activateInvoicePolicy: builder.mutation<InvoicePolicyResponse, number>({
      query: (id) => ({
        url: `/api/v1/billing/invoice-policies/${id}/activate`,
        method: "PATCH",
      }),
      invalidatesTags: ["InvoicePolicies"],
    }),
    deactivateInvoicePolicy: builder.mutation<InvoicePolicyResponse, number>({
      query: (id) => ({
        url: `/api/v1/billing/invoice-policies/${id}/deactivate`,
        method: "PATCH",
      }),
      invalidatesTags: ["InvoicePolicies"],
    }),
    deleteInvoicePolicy: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/billing/invoice-policies/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["InvoicePolicies"],
    }),
  }),
});

export const {
  useGetInvoicePoliciesQuery,
  useGetInvoicePolicyByIdQuery,
  useLazyGetInvoicePolicyByIdQuery,
  useGetInvoicePolicyClientTypesQuery,
  useGetInvoicePolicySessionStatusesQuery,
  useGetInvoicePolicyServicesQuery,
  useCreateInvoicePolicyMutation,
  useUpdateInvoicePolicyMutation,
  useActivateInvoicePolicyMutation,
  useDeactivateInvoicePolicyMutation,
  useDeleteInvoicePolicyMutation,
} = invoicePolicyApi;
