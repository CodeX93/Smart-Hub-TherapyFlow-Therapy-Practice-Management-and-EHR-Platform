import { baseApi } from "../baseApi";

export interface PublicSiteService {
  id: number;
  name: string;
  slug: string;
  description?: string;
  enabled: boolean;
  displayOrder: number;
  isSystem: boolean;
  durationMinutes: number;
  baseRate: number;
}

export interface CreatePublicSiteServicePayload {
  name: string;
  description?: string;
  enabled?: boolean;
  displayOrder?: number;
  durationMinutes?: number;
  baseRate?: number;
}

export interface UpdatePublicSiteServicePayload {
  name?: string;
  description?: string;
  enabled?: boolean;
  displayOrder?: number;
  durationMinutes?: number;
  baseRate?: number;
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

function normalizeService(entry: unknown): PublicSiteService | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    name: asString(entry.name),
    slug: asString(entry.slug),
    description: asString(entry.description) || undefined,
    enabled: typeof entry.enabled === "boolean" ? entry.enabled : true,
    displayOrder: asNumber(entry.displayOrder),
    isSystem: typeof entry.isSystem === "boolean" ? entry.isSystem : false,
    durationMinutes: asNumber(entry.durationMinutes) || 30,
    baseRate: asNumber(entry.baseRate),
  };
}

function normalizeServices(payload: unknown): PublicSiteService[] {
  if (!Array.isArray(payload)) return [];
  return payload
    .map((entry) => normalizeService(entry))
    .filter(Boolean) as PublicSiteService[];
}

const emptyService = (): PublicSiteService => ({
  id: 0,
  name: "",
  slug: "",
  enabled: true,
  displayOrder: 0,
  isSystem: false,
  durationMinutes: 30,
  baseRate: 0,
});

export const publicSiteServicesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getPublicSiteServices: builder.query<PublicSiteService[], void>({
      query: () => "/api/v1/public-site/services",
      providesTags: ["PublicSiteServices"],
      transformResponse: (payload) => normalizeServices(payload),
    }),
    createPublicSiteService: builder.mutation<
      PublicSiteService,
      CreatePublicSiteServicePayload
    >({
      query: (body) => ({
        url: "/api/v1/public-site/services",
        method: "POST",
        body,
      }),
      invalidatesTags: ["PublicSiteServices"],
      transformResponse: (payload) => normalizeService(payload) ?? emptyService(),
    }),
    updatePublicSiteService: builder.mutation<
      PublicSiteService,
      { id: number; body: UpdatePublicSiteServicePayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/public-site/services/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: ["PublicSiteServices"],
      transformResponse: (payload) => normalizeService(payload) ?? emptyService(),
    }),
    deletePublicSiteService: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/public-site/services/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["PublicSiteServices"],
    }),
  }),
});

export const {
  useGetPublicSiteServicesQuery,
  useCreatePublicSiteServiceMutation,
  useUpdatePublicSiteServiceMutation,
  useDeletePublicSiteServiceMutation,
} = publicSiteServicesApi;
