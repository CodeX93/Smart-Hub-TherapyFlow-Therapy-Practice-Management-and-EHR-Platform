import { baseApi } from "../baseApi";
import * as Shared from "./shared";

export const superAdminCatalogApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getFeatureCatalog: builder.query<Shared.FeatureCatalogItem[], { includeDeprecated?: boolean }>({
      query: ({ includeDeprecated } = {}) => {
        const query = new URLSearchParams();
        if (typeof includeDeprecated === "boolean") {
          query.set("includeDeprecated", String(includeDeprecated));
        }
        const qs = query.toString();
        return `/api/v1/super-admin/features/catalog${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) => Shared.mapFeatureCatalogResponse(payload),
    }),
    getFeatureCatalogHistory: builder.query<
      Shared.FeatureCatalogHistoryEntry[],
      { key: string; page?: number; size?: number }
    >({
      query: ({ key, page = 0, size = 50 }) =>
        `/api/v1/super-admin/features/catalog/${encodeURIComponent(
          key,
        )}/history?page=${page}&size=${size}`,
      transformResponse: (payload) =>
        Shared.getArrayPayload(payload)
          .map((entry) => {
            if (!Shared.isRecord(entry)) return null;
            return {
              id: Shared.getNumber(entry, ["id"]),
              authId:
                typeof entry.authId === "number" && Number.isFinite(entry.authId)
                  ? entry.authId
                  : null,
              action: Shared.getString(entry, ["action"]),
              resourceType: Shared.getString(entry, ["resourceType"]),
              resourceId: Shared.getString(entry, ["resourceId"]),
              details: Shared.getString(entry, ["details"]) || null,
              createdAt: Shared.getString(entry, ["createdAt"]),
            } as Shared.FeatureCatalogHistoryEntry;
          })
          .filter(Boolean) as Shared.FeatureCatalogHistoryEntry[],
    }),
    toggleFeatureFlagDefault: builder.mutation<
      { key: string; enabled: boolean },
      { key: string; enabled: boolean }
    >({
      query: ({ key, enabled }) => ({
        url: `/api/v1/super-admin/features/flags/${encodeURIComponent(key)}`,
        method: "PATCH",
        body: { enabled },
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          key: Shared.getString(root, ["key"]),
          enabled: Boolean(root.enabled),
        };
      },
    }),
    getAddOnsCatalog: builder.query<Shared.AddOnCatalogItem[], void>({
      query: () => "/api/super-admin/catalog/addons",
      providesTags: ["AddOnsCatalog"],
      transformResponse: (payload) =>
        Shared.getArrayPayload(payload)
          .map((entry) => {
            if (!Shared.isRecord(entry)) return null;
            return {
              id: Shared.getString(entry, ["id"]),
              code: Shared.getString(entry, ["code"]),
              name: Shared.getString(entry, ["name"]),
              description: Shared.getString(entry, ["description"]),
              priceUsd: Shared.getNumber(entry, ["priceUsd", "price", "amount"]),
              billingCycle: (Shared.getString(entry, ["billingCycle"]) as Shared.AddOnBillingCycle) || "MONTHLY",
              status: (Shared.getString(entry, ["status"]) as Shared.AddOnCatalogStatus) || "ACTIVE",
            } satisfies Shared.AddOnCatalogItem;
          })
          .filter(Boolean) as Shared.AddOnCatalogItem[],
    }),
    getAddOnCatalogItem: builder.query<Shared.AddOnCatalogItem, string>({
      query: (code) => `/api/super-admin/catalog/addons/${encodeURIComponent(code)}`,
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: Shared.getString(root, ["id"]),
          code: Shared.getString(root, ["code"]) || "",
          name: Shared.getString(root, ["name"]),
          description: Shared.getString(root, ["description"]),
          priceUsd: Shared.getNumber(root, ["priceUsd", "price", "amount"]),
          billingCycle: (Shared.getString(root, ["billingCycle"]) as Shared.AddOnBillingCycle) || "MONTHLY",
          status: (Shared.getString(root, ["status"]) as Shared.AddOnCatalogStatus) || "ACTIVE",
        };
      },
    }),
    createAddOnCatalogItem: builder.mutation<Shared.AddOnCatalogItem, Shared.CreateAddOnCatalogItemPayload>({
      query: (body) => ({
        url: "/api/super-admin/catalog/addons",
        method: "POST",
        body,
      }),
      invalidatesTags: ["AddOnsCatalog"],
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: Shared.getString(root, ["id"]),
          code: Shared.getString(root, ["code"]) || "",
          name: Shared.getString(root, ["name"]),
          description: Shared.getString(root, ["description"]),
          priceUsd: Shared.getNumber(root, ["priceUsd", "price", "amount"]),
          billingCycle: (Shared.getString(root, ["billingCycle"]) as Shared.AddOnBillingCycle) || "MONTHLY",
          status: (Shared.getString(root, ["status"]) as Shared.AddOnCatalogStatus) || "ACTIVE",
        };
      },
    }),
    updateAddOnCatalogItem: builder.mutation<Shared.AddOnCatalogItem, Shared.UpdateAddOnCatalogItemPayload>({
      query: ({ code, body }) => ({
        url: `/api/super-admin/catalog/addons/${encodeURIComponent(code)}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: ["AddOnsCatalog"],
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: Shared.getString(root, ["id"]),
          code: Shared.getString(root, ["code"]) || "",
          name: Shared.getString(root, ["name"]),
          description: Shared.getString(root, ["description"]),
          priceUsd: Shared.getNumber(root, ["priceUsd", "price", "amount"]),
          billingCycle: (Shared.getString(root, ["billingCycle"]) as Shared.AddOnBillingCycle) || "MONTHLY",
          status: (Shared.getString(root, ["status"]) as Shared.AddOnCatalogStatus) || "ACTIVE",
        };
      },
    }),
    deactivateAddOnCatalogItem: builder.mutation<void, string>({
      query: (code) => ({
        url: `/api/super-admin/catalog/addons/${encodeURIComponent(code)}`,
        method: "DELETE",
        responseHandler: async (response) => {
          if (response.status === 204) return null;
          const text = await response.text();
          if (!text.trim()) return null;
          try {
            return JSON.parse(text) as unknown;
          } catch {
            return { message: text };
          }
        },
      }),
      invalidatesTags: ["AddOnsCatalog"],
    }),
    archiveAddOnCatalogItem: builder.mutation<Shared.AddOnCatalogItem, string>({
      query: (code) => ({
        url: `/api/v1/super-admin/billing/addon-catalog/${encodeURIComponent(code)}/archive`,
        method: "PUT",
      }),
      invalidatesTags: ["AddOnsCatalog"],
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: Shared.getString(root, ["id"]),
          code: Shared.getString(root, ["code"]) || "",
          name: Shared.getString(root, ["name"]),
          description: Shared.getString(root, ["description"]),
          priceUsd: Shared.getNumber(root, ["priceUsd", "price", "amount"]),
          billingCycle:
            (Shared.getString(root, ["billingCycle"]) as Shared.AddOnBillingCycle) ||
            "MONTHLY",
          status:
            (Shared.getString(root, ["status"]) as Shared.AddOnCatalogStatus) ||
            "INACTIVE",
        };
      },
    }),
    activateAddOnCatalogItem: builder.mutation<Shared.AddOnCatalogItem, string>({
      query: (code) => ({
        url: `/api/v1/super-admin/catalog/addons/${encodeURIComponent(code)}/activate`,
        method: "POST",
      }),
      invalidatesTags: ["AddOnsCatalog"],
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: Shared.getString(root, ["id"]),
          code: Shared.getString(root, ["code"]) || "",
          name: Shared.getString(root, ["name"]),
          description: Shared.getString(root, ["description"]),
          priceUsd: Shared.getNumber(root, ["priceUsd", "price", "amount"]),
          billingCycle: (Shared.getString(root, ["billingCycle"]) as Shared.AddOnBillingCycle) || "MONTHLY",
          status: (Shared.getString(root, ["status"]) as Shared.AddOnCatalogStatus) || "ACTIVE",
        };
      },
    }),
    deleteFeatureCatalogEntry: builder.mutation<void, string>({
      query: (key) => ({
        url: `/api/v1/super-admin/features/catalog/${encodeURIComponent(key)}`,
        method: "DELETE",
      }),
    }),
    updateFeatureCatalogEntry: builder.mutation<
      void,
      {
        key: string;
        body: {
          key: string;
          name: string;
          description: string;
          scope: string;
          type: string;
          defaultEnabled: boolean;
        };
      }
    >({
      query: ({ key, body }) => ({
        url: `/api/v1/super-admin/features/catalog/${encodeURIComponent(key)}`,
        method: "PUT",
        body,
      }),
    }),
  }),
});

export const {
  useGetFeatureCatalogQuery,
  useGetFeatureCatalogHistoryQuery,
  useToggleFeatureFlagDefaultMutation,
  useGetAddOnsCatalogQuery,
  useGetAddOnCatalogItemQuery,
  useCreateAddOnCatalogItemMutation,
  useUpdateAddOnCatalogItemMutation,
  useDeactivateAddOnCatalogItemMutation,
  useArchiveAddOnCatalogItemMutation,
  useActivateAddOnCatalogItemMutation,
  useDeleteFeatureCatalogEntryMutation,
  useUpdateFeatureCatalogEntryMutation,
} = superAdminCatalogApi;
