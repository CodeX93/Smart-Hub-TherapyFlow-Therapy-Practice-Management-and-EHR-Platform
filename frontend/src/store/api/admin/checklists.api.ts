import { baseApi } from "../baseApi";

export interface ClientChecklistItemResponse {
  id: number;
  clientChecklistId: number;
  checklistItemId: number;
  checklistItemTitle?: string;
  checklistItemDescription?: string;
  checklistItemCategory?: string;
  isCompleted?: boolean;
  completedAt?: string;
  completedById?: number;
  completedByName?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}
export interface ClientChecklistResponse {
  id: number;
  clientId: number;
  clientName?: string;
  templateId: number;
  templateName?: string;
  isCompleted?: boolean;
  completedAt?: string;
  completedById?: number;
  completedByName?: string;
  notes?: string;
  description?: string;
  dueDate?: string;
  createdAt?: string;
  updatedAt?: string;
  items: ClientChecklistItemResponse[];
}

export interface ChecklistItem {
  id: number;
  templateId: number;
  title: string;
  description?: string;
  category: string;
  isRequired: boolean;
  itemOrder: number;
  daysFromStart: number;
  sortOrder: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ChecklistTemplate {
  id: number;
  name: string;
  description?: string;
  clientType: string;
  isActive: boolean;
  sortOrder: number;
  createdAt?: string;
  updatedAt?: string;
  itemCount?: number;
  items: ChecklistItem[];
}

export interface ChecklistTemplatesListParams {
  page?: number;
  pageSize?: number;
  search?: string;
  category?: string;
}

export interface ChecklistTemplatesListResponse {
  items: ChecklistTemplate[];
  totalCount: number;
}

export interface CreateChecklistItemPayload {
  title: string;
  description?: string;
  category: string;
  isRequired: boolean;
  itemOrder: number;
  daysFromStart: number;
  sortOrder: number;
}

export interface CreateChecklistTemplatePayload {
  name: string;
  description?: string;
  clientType: string;
  isActive: boolean;
  sortOrder: number;
  items: CreateChecklistItemPayload[];
}

export interface UpdateClientChecklistItemPayload {
  isCompleted: boolean;
  notes?: string;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asOptionalString(value: unknown): string | undefined {
  const parsed = asString(value).trim();
  return parsed.length ? parsed : undefined;
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number.parseFloat(value);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

function asBoolean(value: unknown, fallback = false): boolean {
  return typeof value === "boolean" ? value : fallback;
}

function normalizeChecklistItem(payload: unknown): ChecklistItem | null {
  if (!isRecord(payload)) return null;
  return {
    id: asNumber(payload.id),
    templateId: asNumber(payload.templateId),
    title: asString(payload.title),
    description: asOptionalString(payload.description),
    category: asString(payload.category),
    isRequired: asBoolean(payload.isRequired, false),
    itemOrder: asNumber(payload.itemOrder),
    daysFromStart: asNumber(payload.daysFromStart),
    sortOrder: asNumber(payload.sortOrder),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

function normalizeChecklistTemplate(payload: unknown): ChecklistTemplate | null {
  if (!isRecord(payload)) return null;
  const rawItems = Array.isArray(payload.items) ? payload.items : [];
  return {
    id: asNumber(payload.id),
    name: asString(payload.name),
    description: asOptionalString(payload.description),
    clientType: asString(payload.clientType),
    isActive: asBoolean(payload.isActive, true),
    sortOrder: asNumber(payload.sortOrder),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
    itemCount: asNumber(payload.itemCount),
    items: rawItems.map(normalizeChecklistItem).filter(Boolean) as ChecklistItem[],
  };
}

function normalizeChecklistTemplatesResponse(
  payload: unknown,
): ChecklistTemplatesListResponse {
  if (Array.isArray(payload)) {
    return {
      items: payload.map(normalizeChecklistTemplate).filter(Boolean) as ChecklistTemplate[],
      totalCount: payload.length,
    };
  }
  if (isRecord(payload)) {
    const items = Array.isArray(payload.items) ? payload.items : [];
    return {
      items: items.map(normalizeChecklistTemplate).filter(Boolean) as ChecklistTemplate[],
      totalCount: asNumber(payload.totalCount) || items.length,
    };
  }
  return { items: [], totalCount: 0 };
}

export const checklistsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getChecklistTemplates: builder.query<
      ChecklistTemplatesListResponse,
      ChecklistTemplatesListParams | void
    >({
      query: (params) => {
        const query = new URLSearchParams();
        if (params?.page) query.set("page", String(params.page));
        if (params?.pageSize) query.set("pageSize", String(params.pageSize));
        if (params?.search) query.set("search", params.search);
        if (params?.category && params.category !== "All Categories") {
          query.set("category", params.category.toUpperCase());
        }
        return `/api/v1/checklists/checklist-templates${
          query.toString() ? `?${query.toString()}` : ""
        }`;
      },
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((item) => ({
                type: "ChecklistTemplates" as const,
                id: item.id,
              })),
              { type: "ChecklistTemplates", id: "LIST" },
            ]
          : [{ type: "ChecklistTemplates", id: "LIST" }],
      transformResponse: (payload) =>
        normalizeChecklistTemplatesResponse(payload),
    }),
    createChecklistTemplate: builder.mutation<
      ChecklistTemplate,
      CreateChecklistTemplatePayload
    >({
      query: (body) => ({
        url: "/api/v1/checklists/checklist-templates",
        method: "POST",
        body,
      }),
      invalidatesTags: [{ type: "ChecklistTemplates", id: "LIST" }],
      transformResponse: (payload) =>
        normalizeChecklistTemplate(payload) ?? {
          id: 0,
          name: "",
          clientType: "",
          isActive: true,
          sortOrder: 0,
          items: [],
        },
    }),
    updateChecklistTemplate: builder.mutation<
      ChecklistTemplate,
      { id: number; body: Partial<CreateChecklistTemplatePayload> }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/checklists/checklist-templates/${id}`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "ChecklistTemplates", id: "LIST" },
        { type: "ChecklistTemplates", id },
      ],
      transformResponse: (payload) =>
        normalizeChecklistTemplate(payload) || {
          id: 0,
          name: "",
          clientType: "",
          isActive: true,
          sortOrder: 0,
          items: [],
        },
    }),
    getChecklistTemplateById: builder.query<ChecklistTemplate, number>({
      query: (id) => `/api/v1/checklists/checklist-templates/${id}`,
      providesTags: (_result, _error, id) => [{ type: "ChecklistTemplates", id }],
      transformResponse: (payload) =>
        normalizeChecklistTemplate(payload) || {
          id: 0,
          name: "",
          clientType: "",
          isActive: true,
          sortOrder: 0,
          items: [],
        },
    }),
    deleteChecklistTemplate: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/checklists/checklist-templates/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: [{ type: "ChecklistTemplates", id: "LIST" }],
    }),
    assignChecklistToClient: builder.mutation<
      ClientChecklistResponse,
      { clientId: string; templateId: number; dueDate: string }
    >({
      query: ({ clientId, templateId, dueDate }) => ({
        url: `/api/v1/checklists/clients/${clientId}/checklists`,
        method: "POST",
        body: { templateId, dueDate },
      }),
      invalidatesTags: (_result, _error, { clientId }) => [
        { type: "ClientChecklists", id: clientId },
      ],
    }),
    getClientChecklists: builder.query<ClientChecklistResponse[], string>({
      query: (clientId) => `/api/v1/checklists/clients/${clientId}/checklists`,
      providesTags: (_result, _error, clientId) => [
        { type: "ClientChecklists", id: clientId },
      ],
    }),
    updateClientChecklistItem: builder.mutation<
      ClientChecklistItemResponse,
      {
        id: number;
        clientId: string;
        body: UpdateClientChecklistItemPayload;
      }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/checklists/client-checklist-items/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: [],
    }),
  }),
});

export const {
  useGetChecklistTemplatesQuery,
  useCreateChecklistTemplateMutation,
  useUpdateChecklistTemplateMutation,
  useGetChecklistTemplateByIdQuery,
  useLazyGetChecklistTemplateByIdQuery,
  useDeleteChecklistTemplateMutation,
  useAssignChecklistToClientMutation,
  useGetClientChecklistsQuery,
  useUpdateClientChecklistItemMutation,
} = checklistsApi;
