import { baseApi } from "../baseApi";
import * as Shared from "./shared";

function formatLocalDateTime(value: string | undefined): string {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  }).format(parsed);
}

export const superAdminOrganisationsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getOrganisations: builder.query<Shared.OrganisationListResult, Shared.OrganisationListFilters>({
      query: (filters) => {
        const query = new URLSearchParams();
        if (filters.search.trim()) query.set("search", filters.search.trim());
        if (filters.status.trim()) query.set("status", filters.status.trim());
        if (filters.plan.trim()) query.set("plan", filters.plan.trim());
        if (filters.createdFrom.trim()) query.set("createdFrom", filters.createdFrom.trim());
        if (filters.createdTo.trim()) query.set("createdTo", filters.createdTo.trim());
        if (filters.region.trim()) query.set("region", filters.region.trim());
        if (filters.dataResidency.trim()) query.set("dataResidency", filters.dataResidency.trim());
        query.set("page", String(filters.page));
        query.set("pageSize", String(filters.pageSize));
        query.set("sort", filters.sort);
        query.set("order", filters.order);
        query.set("export", String(filters.exportData));
        return `/api/v1/super-admin/organisations?${query.toString()}`;
      },
      transformResponse: (payload, _meta, arg) =>
        Shared.mapOrganisationsListResponse(payload, arg),
      providesTags: (result) =>
        result
          ? [
              ...result.rows.map((row) => ({
                type: "Organisations" as const,
                id: String(row.id),
              })),
              { type: "Organisations", id: "LIST" },
            ]
          : [{ type: "Organisations", id: "LIST" }],
    }),
    getOrganisationById: builder.query<Shared.OrganisationDetailsResult, number>({
      query: (id) => `/api/v1/super-admin/organisations/${id}`,
      transformResponse: (payload) => Shared.mapOrganisationDetailsResponse(payload),
      providesTags: (_result, _error, id) => [
        { type: "Organisations", id: String(id) },
      ],
    }),
    getOrganisationUsers: builder.query<Shared.OrganisationUserListItem[], Shared.OrganisationUsersQueryParams>({
      query: ({ id, search, role, status, page = 0, pageSize = 50 }) => {
        const query = new URLSearchParams();
        if (search && search.trim()) query.set("search", search.trim());
        if (role && role.trim()) query.set("role", role.trim());
        if (status && status.trim()) query.set("status", status.trim());
        query.set("page", String(page));
        query.set("pageSize", String(pageSize));
        return `/api/v1/super-admin/organisations/${id}/users?${query.toString()}`;
      },
      transformResponse: (payload) => {
        return Shared.getArrayPayload(payload)
          .map((entry, index) => {
            if (!Shared.isRecord(entry)) return null;
            const idFromString = Shared.getString(entry, ["id", "userId", "authId"]);
            const authIdRaw = entry.authId;
            const userIdRaw = entry.userId;
            const id =
              idFromString ||
              (typeof authIdRaw === "number" && Number.isFinite(authIdRaw)
                ? String(authIdRaw)
                : typeof userIdRaw === "number" && Number.isFinite(userIdRaw)
                  ? String(userIdRaw)
                  : "");
            const loginIdentifier = Shared.getString(entry, ["loginIdentifier"]);
            const firstName = Shared.getString(entry, ["firstName", "givenName"]);
            const lastName = Shared.getString(entry, ["lastName", "familyName"]);
            const fullName = Shared.getString(entry, ["name", "fullName", "displayName"]);
            const roles = Array.isArray(entry.roles)
              ? entry.roles.filter((role): role is string => typeof role === "string")
              : [];
            const identityType = Shared.getString(entry, ["identityType"]).toUpperCase();
            const activeFlag =
              typeof entry.isActive === "boolean"
                ? entry.isActive
                : typeof entry.active === "boolean"
                  ? entry.active
                  : true;
            const accountLocked = Boolean(entry.accountLocked);
            const resolvedRoleRaw = (roles[0] || "").toUpperCase();
            const resolvedRole = resolvedRoleRaw.includes("THERAPIST")
              ? "Therapist"
              : resolvedRoleRaw.includes("SUPERVISOR")
                ? "Supervisor"
                : resolvedRoleRaw.includes("CLIENT") || identityType === "CLIENT"
                  ? "Client"
                  : "Admin";
            const resolvedStatus = accountLocked
              ? "Suspended"
              : activeFlag
                ? "Active"
                : "Inactive";
            const fallbackNameFromLogin = loginIdentifier
              ? loginIdentifier.split("@")[0].replaceAll(".", " ")
              : "";
            const name =
              fullName ||
              `${firstName} ${lastName}`.trim() ||
              fallbackNameFromLogin ||
              Shared.getString(entry, ["email"]);

            const rawLastLogin = Shared.getString(entry, [
              "lastSuccessfulLogin",
              "lastLogin",
              "lastLoginAt",
            ]);
            let lastLogin = "-";
            if (rawLastLogin) {
              const parsedLastLogin = new Date(rawLastLogin);
              lastLogin = Number.isNaN(parsedLastLogin.getTime())
                ? rawLastLogin
                : parsedLastLogin.toLocaleString("en-US", {
                    year: "numeric",
                    month: "short",
                    day: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  });
            }

            return {
              id: id || String(index),
              name: name || "-",
              email: Shared.getString(entry, ["email", "username", "loginIdentifier"]) || "-",
              role: resolvedRole,
              status: resolvedStatus,
              lastLogin,
            } as Shared.OrganisationUserListItem;
          })
          .filter(Boolean) as Shared.OrganisationUserListItem[];
      },
    }),
    getOrganisationInvoices: builder.query<
      Shared.OrganisationInvoiceListItem[],
      Shared.OrganisationInvoicesQueryParams
    >({
      query: ({ id, status, sort, page = 0, pageSize = 50 }) => {
        const query = new URLSearchParams();
        if (status && status.trim()) query.set("status", status.trim());
        if (sort && sort.trim()) query.set("sort", sort.trim());
        query.set("page", String(page));
        query.set("pageSize", String(pageSize));
        return `/api/v1/super-admin/organisations/${id}/invoices?${query.toString()}`;
      },
      transformResponse: (payload) => {
        return Shared.getArrayPayload(payload)
          .map((entry, index) => {
            const mappedInvoice = Shared.mapSuperAdminBillingInvoice(entry, index);
            if (!mappedInvoice) return null;
            return {
              id: mappedInvoice.id,
              invoiceId: mappedInvoice.invoiceId,
              invoiceNumericId: mappedInvoice.invoiceNumericId,
              period: mappedInvoice.period,
              dueDate: mappedInvoice.dueDate,
              amount: mappedInvoice.amount,
              currentBalance: mappedInvoice.currentBalance,
              status: mappedInvoice.status,
              paidAt: formatLocalDateTime(mappedInvoice.paidAt),
              organisation: mappedInvoice.organisation,
            } as Shared.OrganisationInvoiceListItem;
          })
          .filter(Boolean) as Shared.OrganisationInvoiceListItem[];
      },
    }),
    getOrganisationAddOns: builder.query<Shared.OrganisationAddOnListItem[], number>({
      query: (id) => `/api/v1/super-admin/organisations/${id}/addons`,
      providesTags: (_result, _error, id) => [{ type: "AddOnsCatalog", id: `ORG_${id}` }],
      transformResponse: (payload) => {
        return Shared.getArrayPayload(payload)
          .map((entry, index) => {
            if (!Shared.isRecord(entry)) return null;

            const purchaseIdRaw = Shared.getNumber(entry, ["purchaseId", "id"]);
            const purchaseId = purchaseIdRaw > 0 ? purchaseIdRaw : null;
            const featureCode =
              Shared.getString(entry, ["featureCode", "code", "addonCode", "key"]) ||
              `ADDON-${index + 1}`;
            const featureName =
              Shared.getString(entry, ["featureName", "name", "addOnName", "title"]) ||
              "Add-on";
            const endAtRaw = Shared.getString(entry, ["endAt"]);
            const endAt = endAtRaw || null;
            const startAtRaw = Shared.getString(entry, ["startAt"]);
            const isActive = !endAt;
            const statusFromApi = Shared.getString(entry, ["status"]);
            const status = isActive
              ? statusFromApi || "ACTIVE"
              : statusFromApi || "ENDED";

            return {
              purchaseId,
              featureCode,
              featureName,
              quantity: Shared.getNumber(entry, ["quantity"]) || 1,
              pricePerUnitAtTime: Shared.getNumber(entry, [
                "pricePerUnitAtTime",
                "priceUsd",
                "price",
                "amount",
                "amountUsd",
              ]),
              unitValue: Shared.getNumber(entry, ["unitValue"]) || 1,
              billingCycle: Shared.getString(entry, ["billingCycle", "cycle"]) || "MONTHLY",
              startAt: startAtRaw || null,
              endAt,
              id: purchaseId != null ? String(purchaseId) : `addon-${index + 1}`,
              code: featureCode,
              name: featureName,
              description: Shared.getString(entry, ["description"]),
              priceUsd: Shared.getNumber(entry, [
                "pricePerUnitAtTime",
                "priceUsd",
                "price",
                "amount",
                "amountUsd",
              ]),
              status,
              isActive,
            } as Shared.OrganisationAddOnListItem;
          })
          .filter(Boolean) as Shared.OrganisationAddOnListItem[];
      },
    }),
    assignOrganisationAddOn: builder.mutation<void, Shared.AssignOrganisationAddOnPayload>({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/organisations/${id}/addons`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "AddOnsCatalog", id: `ORG_${id}` },
      ],
    }),
    unassignOrganisationAddOnByFeature: builder.mutation<
      Shared.OrganisationAddOnListItem | void,
      Shared.UnassignOrganisationAddOnByFeaturePayload
    >({
      query: ({ organisationId, featureCode }) => ({
        url: `/api/v1/super-admin/organisations/${organisationId}/addons?featureCode=${encodeURIComponent(featureCode)}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, { organisationId }) => [
        { type: "AddOnsCatalog", id: `ORG_${organisationId}` },
      ],
    }),
    unassignOrganisationAddOnByPurchase: builder.mutation<
      Shared.OrganisationAddOnListItem | void,
      Shared.UnassignOrganisationAddOnByPurchasePayload
    >({
      query: ({ organisationId, purchaseId }) => ({
        url: `/api/v1/super-admin/organisations/${organisationId}/addons/${purchaseId}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, { organisationId }) => [
        { type: "AddOnsCatalog", id: `ORG_${organisationId}` },
      ],
    }),
    disableSuperAdminUser: builder.mutation<
      Record<string, unknown>,
      { authId: number | string; reason: string }
    >({
      query: ({ authId, reason }) => ({
        url: `/api/v1/super-admin/users/${encodeURIComponent(String(authId))}/disable`,
        method: "POST",
        body: { reason },
      }),
      transformResponse: (payload) => Shared.extractRoot(payload),
    }),
    enableSuperAdminUser: builder.mutation<Record<string, unknown>, number | string>({
      query: (authId) => ({
        url: `/api/v1/super-admin/users/${encodeURIComponent(String(authId))}/enable`,
        method: "POST",
      }),
      transformResponse: (payload) => Shared.extractRoot(payload),
    }),
    getOrganisationFeatures: builder.query<Shared.OrganisationFeaturesResponse, number>({
      query: (id) => `/api/v1/super-admin/organisations/${id}/features`,
      transformResponse: (payload) => Shared.mapOrganisationFeaturesResponse(payload),
    }),
    getOrganisationSettings: builder.query<Shared.OrganisationSettings, number>({
      query: (id) => `/api/v1/super-admin/organisations/${id}/settings`,
      transformResponse: (payload) => Shared.mapOrganisationSettingsResponse(payload),
    }),
    getOrganisationSubscription: builder.query<Shared.OrganisationSubscription, number>({
      query: (id) => `/api/v1/super-admin/organisations/${id}/subscription`,
      transformResponse: (payload) => Shared.mapOrganisationSubscriptionResponse(payload),
      providesTags: (_result, _error, id) => [
        { type: "Organisations", id: `SUBSCRIPTION_${id}` },
        { type: "Organisations", id: String(id) },
      ],
    }),
    getOrganisationSchemaVersion: builder.query<Shared.OrganisationSchemaVersion, number>({
      query: (id) => `/api/v1/super-admin/organisations/${id}/schema-version`,
      transformResponse: (payload) => Shared.mapOrganisationSchemaVersionResponse(payload),
    }),
    getOrganisationHealth: builder.query<Record<string, unknown>, number>({
      query: (id) => `/api/v1/super-admin/organisations/${id}/health`,
      transformResponse: (payload) => Shared.extractRoot(payload),
    }),
    provisionOrganisationSchema: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/organisations/${id}/provision`,
        method: "POST",
      }),
    }),
    lockOrganisation: builder.mutation<Shared.OrganisationOperationResponse, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/organisations/${id}/lock`,
        method: "POST",
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          success: Boolean(root.success),
          organisationId: Shared.getNumber(root, ["organisationId", "id"]),
          status: Shared.getString(root, ["status"]),
          message: Shared.getString(root, ["message"]),
        };
      },
      invalidatesTags: (_result, _error, id) => [
        { type: "Organisations", id: String(id) },
        { type: "Organisations", id: "LIST" },
      ],
    }),
    backupOrganisation: builder.mutation<Shared.OrganisationOperationResponse, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/organisations/${id}/backup`,
        method: "POST",
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          success: Boolean(root.success),
          organisationId: Shared.getNumber(root, ["organisationId", "id"]),
          jobId:
            typeof root.jobId === "number"
              ? root.jobId
              : typeof root.jobID === "number"
                ? root.jobID
                : undefined,
          status: Shared.getString(root, ["status"]),
          message: Shared.getString(root, ["message"]),
        };
      },
    }),
    bootstrapOrganisationStaffProfiles: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/super-admin/organisations/${id}/staff-profiles/bootstrap`,
        method: "POST",
      }),
    }),
    bootstrapStaffProfilesBulk: builder.mutation<void, number | void>({
      query: (limit) => {
        const query = new URLSearchParams();
        if (typeof limit === "number" && Number.isFinite(limit)) {
          query.set("limit", String(limit));
        }
        const qs = query.toString();
        return {
          url: `/api/v1/super-admin/organisations/staff-profiles/bootstrap${
            qs ? `?${qs}` : ""
          }`,
          method: "POST",
        };
      },
    }),
    replaceOrganisationFeatures: builder.mutation<
      Shared.OrganisationFeaturesResponse,
      Shared.UpdateOrganisationFeaturesPayload
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/organisations/${id}/features`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => Shared.mapOrganisationFeaturesResponse(payload),
    }),
    updateOrganisationSettings: builder.mutation<void, { id: number; body: Partial<Shared.OrganisationSettings> }>({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/organisations/${id}/settings`,
        method: "PUT",
        body,
      }),
    }),
    updateOrganisationSubscription: builder.mutation<
      Shared.OrganisationSubscription,
      Shared.UpdateOrganisationSubscriptionPayload
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/organisations/${id}/subscription`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => Shared.mapOrganisationSubscriptionResponse(payload),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Organisations", id: String(id) },
        { type: "Organisations", id: "LIST" },
      ],
    }),
    updateOrganisationById: builder.mutation<
      Shared.OrganisationDetailsResult,
      Shared.UpdateOrganisationPayload
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/organisations/${id}`,
        method: "PATCH",
        body,
      }),
      transformResponse: (payload) => Shared.mapOrganisationDetailsResponse(payload),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Organisations", id: String(id) },
        { type: "Organisations", id: "LIST" },
      ],
    }),
    createOrganisation: builder.mutation<void, Shared.CreateOrganisationPayload>({
      query: (body) => ({
        url: "/api/v1/super-admin/organisations",
        method: "POST",
        body,
      }),
      invalidatesTags: [{ type: "Organisations", id: "LIST" }],
    }),
    suspendOrganisation: builder.mutation<
      void,
      { id: number; body: Shared.SuspendOrganisationPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/organisations/${id}/suspend`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Organisations", id: String(id) },
        { type: "Organisations", id: "LIST" },
      ],
    }),
    reactivateOrganisation: builder.mutation<
      void,
      { id: number; body: Shared.ReactivateOrganisationPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/organisations/${id}/reactivate`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Organisations", id: String(id) },
        { type: "Organisations", id: "LIST" },
      ],
    }),
    terminateOrganisation: builder.mutation<
      void,
      { id: number; body: Shared.TerminateOrganisationPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/super-admin/organisations/${id}/terminate`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Organisations", id: String(id) },
        { type: "Organisations", id: "LIST" },
      ],
    }),
  }),
});

export const {
  useGetOrganisationsQuery,
  useLazyGetOrganisationsQuery,
  useGetOrganisationByIdQuery,
  useGetOrganisationUsersQuery,
  useGetOrganisationInvoicesQuery,
  useGetOrganisationAddOnsQuery,
  useAssignOrganisationAddOnMutation,
  useUnassignOrganisationAddOnByFeatureMutation,
  useUnassignOrganisationAddOnByPurchaseMutation,
  useDisableSuperAdminUserMutation,
  useEnableSuperAdminUserMutation,
  useGetOrganisationFeaturesQuery,
  useGetOrganisationSettingsQuery,
  useGetOrganisationSubscriptionQuery,
  useGetOrganisationSchemaVersionQuery,
  useGetOrganisationHealthQuery,
  useProvisionOrganisationSchemaMutation,
  useLockOrganisationMutation,
  useBackupOrganisationMutation,
  useBootstrapOrganisationStaffProfilesMutation,
  useBootstrapStaffProfilesBulkMutation,
  useReplaceOrganisationFeaturesMutation,
  useUpdateOrganisationSettingsMutation,
  useUpdateOrganisationSubscriptionMutation,
  useUpdateOrganisationByIdMutation,
  useCreateOrganisationMutation,
  useSuspendOrganisationMutation,
  useReactivateOrganisationMutation,
  useTerminateOrganisationMutation,
} = superAdminOrganisationsApi;
