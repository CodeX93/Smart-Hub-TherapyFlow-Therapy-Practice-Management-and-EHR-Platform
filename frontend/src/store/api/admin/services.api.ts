import { baseApi } from "../baseApi";

export interface AdminBillingService {
  id: number;
  serviceCode: string;
  serviceName: string;
  description?: string;
  durationInMinutes?: number;
  baseRate: number;
  isActive: boolean;
  therapistVisible: boolean;
  clientPortalVisible: boolean;
  publicSiteEnabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateBillingServicePayload {
  serviceCode: string;
  serviceName: string;
  description?: string;
  durationInMinutes?: number;
  baseRate: number;
  isActive?: boolean;
  therapistVisible?: boolean;
  clientPortalVisible?: boolean;
  publicSiteEnabled?: boolean;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number.parseFloat(value);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

function asOptionalNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === "") return undefined;
  const parsed = asNumber(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function normalizeService(entry: unknown): AdminBillingService | null {
  if (!isRecord(entry)) return null;

  return {
    id: asNumber(entry.id),
    serviceCode: asString(entry.serviceCode),
    serviceName: asString(entry.serviceName),
    description: asString(entry.description) || undefined,
    durationInMinutes: asOptionalNumber(entry.durationInMinutes),
    baseRate: asNumber(entry.baseRate),
    isActive: typeof entry.isActive === "boolean" ? entry.isActive : true,
    therapistVisible:
      typeof entry.therapistVisible === "boolean" ? entry.therapistVisible : false,
    clientPortalVisible:
      typeof entry.clientPortalVisible === "boolean" ? entry.clientPortalVisible : false,
    publicSiteEnabled:
      typeof entry.publicSiteEnabled === "boolean" ? entry.publicSiteEnabled : false,
    createdAt: asString(entry.createdAt) || undefined,
    updatedAt: asString(entry.updatedAt) || undefined,
  };
}

function normalizeServices(payload: unknown): AdminBillingService[] {
  if (!Array.isArray(payload)) return [];
  return payload.map((entry) => normalizeService(entry)).filter(Boolean) as AdminBillingService[];
}

export const adminServicesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminBillingServices: builder.query<AdminBillingService[], void>({
      query: () => "/api/v1/billing/services",
      providesTags: ["BillingServices"],
      transformResponse: (payload) => normalizeServices(payload),
    }),
    getAdminBillingServiceById: builder.query<AdminBillingService, number>({
      query: (id) => `/api/v1/billing/services/${id}`,
      providesTags: (_result, _error, id) => [{ type: "BillingServices", id }],
      transformResponse: (payload) =>
        normalizeService(payload) ?? {
          id: 0,
          serviceCode: "",
          serviceName: "",
          baseRate: 0,
          isActive: true,
          therapistVisible: false,
          clientPortalVisible: false,
        },
    }),
    createAdminBillingService: builder.mutation<
      AdminBillingService,
      UpdateBillingServicePayload
    >({
      query: (body) => ({
        url: "/api/v1/billing/services",
        method: "POST",
        body,
      }),
      invalidatesTags: ["BillingServices"],
      transformResponse: (payload) =>
        normalizeService(payload) ?? {
          id: 0,
          serviceCode: "",
          serviceName: "",
          baseRate: 0,
          isActive: true,
          therapistVisible: false,
          clientPortalVisible: false,
        },
    }),
    updateAdminBillingService: builder.mutation<
      AdminBillingService,
      { id: number; body: UpdateBillingServicePayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/billing/services/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        "BillingServices",
        { type: "BillingServices", id },
      ],
      transformResponse: (payload) =>
        normalizeService(payload) ?? {
          id: 0,
          serviceCode: "",
          serviceName: "",
          baseRate: 0,
          isActive: true,
          therapistVisible: false,
          clientPortalVisible: false,
        },
    }),
    deleteAdminBillingService: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/billing/services/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        "BillingServices",
        { type: "BillingServices", id },
      ],
    }),
    showAllBillingServicesToTherapists: builder.mutation<void, void>({
      query: () => ({
        url: "/api/v1/billing/services/visibility/therapists/show-all",
        method: "POST",
      }),
      invalidatesTags: ["BillingServices"],
    }),
    hideAllBillingServicesFromTherapists: builder.mutation<void, void>({
      query: () => ({
        url: "/api/v1/billing/services/visibility/therapists/hide-all",
        method: "POST",
      }),
      invalidatesTags: ["BillingServices"],
    }),
  }),
});

export const {
  useGetAdminBillingServicesQuery,
  useLazyGetAdminBillingServiceByIdQuery,
  useCreateAdminBillingServiceMutation,
  useUpdateAdminBillingServiceMutation,
  useDeleteAdminBillingServiceMutation,
  useShowAllBillingServicesToTherapistsMutation,
  useHideAllBillingServicesFromTherapistsMutation,
} = adminServicesApi;
