import { baseApi } from "../baseApi";
import * as Shared from "./shared";

async function parseBlobExportResponse(
  response: Response,
): Promise<Blob | Record<string, unknown>> {
  if (!response.ok) {
    const text = await response.text();
    if (!text.trim()) {
      return { message: response.statusText || "Export failed." };
    }

    try {
      const parsed = JSON.parse(text) as unknown;
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
        return parsed as Record<string, unknown>;
      }
      return { message: text };
    } catch {
      return { message: text };
    }
  }

  return response.blob();
}

export const superAdminBillingApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getBillingPlans: builder.query<Shared.BillingPlan[], void>({
      query: () => "/api/v1/super-admin/plans",
      providesTags: ["BillingPlans"],
      transformResponse: (payload) =>
        Shared.getArrayPayload(payload)
          .map(function (entry) {
            if (!Shared.isRecord(entry)) return null;
            return Shared.mapBillingPlan(entry);
          })
          .filter(Boolean) as Shared.BillingPlan[],
    }),
    getBillingPlanDetails: builder.query<Shared.BillingPlan, string>({
      query: (planCode) =>
        `/api/v1/super-admin/plans/${encodeURIComponent(planCode)}/details`,
      providesTags: (_result, _error, planCode) => [
        { type: "BillingPlans", id: `DETAILS_${planCode}` },
      ],
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        if (!Shared.isRecord(root)) {
          throw new Error("Unable to load plan details right now.");
        }
        return Shared.mapBillingPlan(root);
      },
    }),
    createBillingPlan: builder.mutation<void, Shared.CreateBillingPlanPayload>({
      query: (body) => ({
        url: "/api/v1/super-admin/plans",
        method: "POST",
        body,
      }),
      invalidatesTags: ["BillingPlans"],
    }),
    updateBillingPlan: builder.mutation<Shared.BillingPlan, Shared.UpdateBillingPlanPayload>({
      query: ({ planName, body }) => ({
        url: `/api/v1/super-admin/plans/${encodeURIComponent(planName)}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (_result, _error, { planName }) => [
        "BillingPlans",
        { type: "BillingPlans", id: `DETAILS_${planName}` },
      ],
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        if (!Shared.isRecord(root)) {
          throw new Error("Unable to update plan right now.");
        }
        return Shared.mapBillingPlan(root);
      },
    }),
    archiveBillingPlan: builder.mutation<void, string>({
      query: (planCode) => ({
        url: `/api/v1/super-admin/plans/${encodeURIComponent(planCode)}`,
        method: "DELETE",
      }),
      invalidatesTags: ["BillingPlans"],
    }),
    archivePlan: builder.mutation<void, string>({
      query: (id) => ({
        url: `/api/v1/super-admin/plans/${encodeURIComponent(id)}/archive`,
        method: "PUT",
      }),
      invalidatesTags: ["BillingPlans"],
    }),
    unarchivePlan: builder.mutation<void, string>({
      query: (id) => ({
        url: `/api/v1/super-admin/plans/${encodeURIComponent(id)}/unarchive`,
        method: "PUT",
      }),
      invalidatesTags: ["BillingPlans"],
    }),
    deleteBillingPlan: builder.mutation<void, string>({
      query: (id) => ({
        url: `/api/v1/super-admin/plans/${encodeURIComponent(id)}`,
        method: "DELETE",
      }),
      invalidatesTags: ["BillingPlans"],
    }),
    getPlanEntitlements: builder.query<Shared.PlanEntitlementsResponse, string>({
      query: (planName) => `/api/v1/super-admin/plans/${encodeURIComponent(planName)}/entitlements`,
      providesTags: (_result, _error, planName) => [
        { type: "BillingPlans", id: `ENTITLEMENTS_${planName}` },
      ],
      transformResponse: (payload) => Shared.mapPlanEntitlementsResponse(payload),
    }),
    replacePlanEntitlements: builder.mutation<Shared.PlanEntitlementsResponse, Shared.UpdatePlanEntitlementsPayload>({
      query: ({ planName, body }) => ({
        url: `/api/v1/super-admin/plans/${encodeURIComponent(planName)}/entitlements`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (_result, _error, { planName }) => [
        "BillingPlans",
        { type: "BillingPlans", id: `ENTITLEMENTS_${planName}` },
        { type: "BillingPlans", id: `DETAILS_${planName}` },
      ],
      transformResponse: (payload) => Shared.mapPlanEntitlementsResponse(payload),
    }),
    getPlansCatalog: builder.query<Shared.PlanCatalogOption[], void>({
      query: () => "/api/v1/super-admin/plans",
      providesTags: ["BillingPlans"],
      transformResponse: (payload) => Shared.mapPlansCatalogResponse(payload),
    }),
    getSuperAdminBillingInvoices: builder.query<
      Shared.SuperAdminBillingInvoicesResult,
      Shared.SuperAdminBillingInvoicesQueryParams
    >({
      query: ({ orgId, organisationId, status, sort, page = 0, pageSize = 50 }) => {
        const query = new URLSearchParams();
        if (typeof orgId === "number") query.set("orgId", String(orgId));
        if (typeof organisationId === "number") {
          query.set("organisationId", String(organisationId));
        }
        if (status && status.trim()) query.set("status", status.trim());
        if (sort && sort.trim()) query.set("sort", sort.trim());
        query.set("page", String(page));
        query.set("pageSize", String(pageSize));
        return `/api/v1/super-admin/billing/invoices?${query.toString()}`;
      },
      providesTags: ["BillingInvoices"],
      transformResponse: (payload, _, filters) =>
        Shared.mapSuperAdminBillingInvoicesResponse(payload, filters),
    }),
    getSuperAdminBillingInvoiceById: builder.query<
      Shared.SuperAdminBillingInvoiceDetail,
      number
    >({
      query: (invoiceId) => `/api/v1/super-admin/billing/invoices/${invoiceId}`,
      providesTags: (_result, _error, invoiceId) => [
        { type: "BillingInvoices", id: invoiceId },
      ],
      transformResponse: (payload) => Shared.mapSuperAdminBillingInvoiceDetail(payload),
    }),
    getSuperAdminBillingInvoicePdf: builder.query<Blob, number>({
      query: (invoiceId) => ({
        url: `/api/v1/super-admin/billing/invoices/${invoiceId}/pdf`,
        responseHandler: parseBlobExportResponse,
      }),
      keepUnusedDataFor: 0,
    }),
    getDunningPolicy: builder.query<Shared.DunningPolicy, void>({
      query: () => "/api/v1/super-admin/billing/dunning-policy",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        const stepsRaw = Array.isArray(root.steps) ? root.steps : [];
        return {
          steps: stepsRaw
            .map((step) => {
              if (!Shared.isRecord(step)) return null;
              return {
                day: Shared.getNumber(step, ["day"]),
                action: Shared.getString(step, ["action"]),
              } as Shared.DunningPolicyStep;
            })
            .filter(Boolean) as Shared.DunningPolicyStep[],
          gracePeriodDays: Shared.getNumber(root, ["gracePeriodDays"]),
          trialNoticeDays: Shared.getNumber(root, ["trialNoticeDays"]),
        };
      },
    }),
    updateDunningPolicy: builder.mutation<Shared.DunningPolicy, Shared.DunningPolicy>({
      query: (body) => ({
        url: "/api/v1/super-admin/billing/dunning-policy",
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        const stepsRaw = Array.isArray(root.steps) ? root.steps : [];
        return {
          steps: stepsRaw
            .map((step) => {
              if (!Shared.isRecord(step)) return null;
              return {
                day: Shared.getNumber(step, ["day"]),
                action: Shared.getString(step, ["action"]),
              } as Shared.DunningPolicyStep;
            })
            .filter(Boolean) as Shared.DunningPolicyStep[],
          gracePeriodDays: Shared.getNumber(root, ["gracePeriodDays"]),
          trialNoticeDays: Shared.getNumber(root, ["trialNoticeDays"]),
        };
      },
    }),
    getSuperAdminBillingInvoicesExport: builder.query<
      Blob,
      Shared.SuperAdminBillingInvoicesExportQueryParams | void
    >({
      query: (filters) => {
        const query = new URLSearchParams();
        if (typeof filters?.organisationId === "number") {
          query.set("organisationId", String(filters.organisationId));
        }
        if (filters?.status?.trim()) {
          query.set("status", filters.status.trim());
        }
        const qs = query.toString();
        return {
          url: `/api/v1/super-admin/billing/invoices/export${qs ? `?${qs}` : ""}`,
          responseHandler: parseBlobExportResponse,
        };
      },
      keepUnusedDataFor: 0,
    }),
    applySuperAdminInvoiceRefund: builder.mutation<void, Shared.ApplySuperAdminInvoiceRefundPayload>({
      query: ({ invoiceId, body }) => ({
        url: `/api/v1/super-admin/billing/invoices/${invoiceId}/refunds`,
        method: "POST",
        body,
      }),
      invalidatesTags: ["BillingInvoices"],
    }),
    applySuperAdminInvoiceCredit: builder.mutation<void, Shared.ApplySuperAdminInvoiceCreditPayload>({
      query: ({ invoiceId, body }) => ({
        url: `/api/v1/super-admin/billing/invoices/${invoiceId}/credits`,
        method: "POST",
        body,
      }),
      invalidatesTags: ["BillingInvoices"],
    }),
    sendSuperAdminInvoiceReminder: builder.mutation<
      Shared.SuperAdminInvoiceSendReminderResult,
      number
    >({
      query: (invoiceId) => ({
        url: `/api/v1/super-admin/billing/invoices/${invoiceId}/send-reminder`,
        method: "POST",
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        const recipientsRaw = Array.isArray(root.emailRecipients)
          ? root.emailRecipients
          : [];
        return {
          invoiceId: Shared.getNumber(root, ["invoiceId"]),
          organisationId: Shared.getNumber(root, ["organisationId"]) || null,
          emailsSent: Shared.getNumber(root, ["emailsSent"]),
          emailsFailed: Shared.getNumber(root, ["emailsFailed"]),
          inAppNotificationsCreated: Shared.getNumber(root, [
            "inAppNotificationsCreated",
          ]),
          emailRecipients: recipientsRaw
            .map((item) => (typeof item === "string" ? item.trim() : ""))
            .filter(Boolean),
          warning:
            typeof root.warning === "string" && root.warning.trim()
              ? root.warning.trim()
              : null,
        } satisfies Shared.SuperAdminInvoiceSendReminderResult;
      },
    }),
  }),
});

export const {
  useGetBillingPlansQuery,
  useGetBillingPlanDetailsQuery,
  useCreateBillingPlanMutation,
  useUpdateBillingPlanMutation,
  useArchiveBillingPlanMutation,
  useArchivePlanMutation,
  useUnarchivePlanMutation,
  useDeleteBillingPlanMutation,
  useGetPlanEntitlementsQuery,
  useReplacePlanEntitlementsMutation,
  useGetPlansCatalogQuery,
  useGetSuperAdminBillingInvoicesQuery,
  useGetSuperAdminBillingInvoiceByIdQuery,
  useLazyGetSuperAdminBillingInvoicePdfQuery,
  useGetDunningPolicyQuery,
  useUpdateDunningPolicyMutation,
  useGetSuperAdminBillingInvoicesExportQuery,
  useLazyGetSuperAdminBillingInvoicesExportQuery,
  useApplySuperAdminInvoiceRefundMutation,
  useApplySuperAdminInvoiceCreditMutation,
  useSendSuperAdminInvoiceReminderMutation,
} = superAdminBillingApi;
