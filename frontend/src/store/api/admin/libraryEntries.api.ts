import { baseApi } from "../baseApi";

export interface LibraryEntry {
  id: number;
  categoryId: number;
  categoryName: string;
  title: string;
  content: string;
  tags: string[];
  createdById: number;
  createdByName: string;
  isActive: boolean;
  sortOrder: number;
  usageCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface LibraryTag {
  id: number;
  name: string;
  usageCount: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface LibraryConnection {
  id: number;
  fromEntryId: number;
  fromEntryTitle: string;
  toEntryId: number;
  toEntryTitle: string;
  connectionType: string;
  strength: number;
  description?: string;
  createdByUserId: number;
  createdByName: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConnectedLibraryEntry {
  connectionId: number;
  fromEntryId: number;
  toEntryId: number;
  entryId: number;
  entryTitle: string;
  entryContent: string;
  tags: string[];
  categoryId: number;
  categoryName: string;
  connectionType: string;
  connectionStrength: number;
  description?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface LibraryEntryWithConnections {
  entry: LibraryEntry;
  connectedEntries: ConnectedLibraryEntry[];
}

export interface UpsertLibraryEntryPayload {
  categoryId: number;
  title: string;
  content: string;
  tags: string[];
  sortOrder?: number;
  isActive?: boolean;
}

export interface BulkLibraryEntryTagsPayload {
  entryIds: number[];
  tagsToAdd?: string[];
  tagsToRemove?: string[];
}

export interface BulkLibraryEntryImportRow {
  title: string;
  content: string;
  domain?: string;
  subdomain?: string;
  tags?: string[];
  sortOrder?: number;
}

export interface BulkLibraryEntriesImportPayload {
  categoryId: number;
  entries: BulkLibraryEntryImportRow[];
}

export interface BulkLibraryEntriesImportError {
  row: number;
  title?: string;
  error: string;
}

export interface BulkLibraryEntriesImportResponse {
  total: number;
  successful: number;
  skipped: number;
  failed: number;
  categoriesCreated: number;
  connectionsCreated: number;
  errors: BulkLibraryEntriesImportError[];
}

interface LibraryEntriesQueryArgs {
  categoryId?: number;
  page?: number;
  limit?: number;
}

interface LibrarySearchQueryArgs {
  q: string;
  categoryId?: number;
  page?: number;
  limit?: number;
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

function asStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.map((entry) => asString(entry)).filter((entry) => entry.trim().length > 0);
}

function normalizeEntry(payload: unknown): LibraryEntry | null {
  if (!isRecord(payload)) return null;
  return {
    id: asNumber(payload.id),
    categoryId: asNumber(payload.categoryId),
    categoryName: asString(payload.categoryName),
    title: asString(payload.title),
    content: asString(payload.content),
    tags: asStringArray(payload.tags),
    createdById: asNumber(payload.createdById),
    createdByName: asString(payload.createdByName),
    isActive: asBoolean(payload.isActive, true),
    sortOrder: asNumber(payload.sortOrder),
    usageCount: asNumber(payload.usageCount),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

function normalizeEntryList(payload: unknown): LibraryEntry[] {
  if (Array.isArray(payload)) {
    return payload.map((item) => normalizeEntry(item)).filter(Boolean) as LibraryEntry[];
  }
  if (isRecord(payload)) {
    const items = payload.items;
    if (Array.isArray(items)) {
      return items.map((item) => normalizeEntry(item)).filter(Boolean) as LibraryEntry[];
    }
  }
  const one = normalizeEntry(payload);
  return one ? [one] : [];
}

function normalizeTag(payload: unknown): LibraryTag | null {
  if (!isRecord(payload)) return null;
  return {
    id: asNumber(payload.id),
    name: asString(payload.name),
    usageCount: asNumber(payload.usageCount),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

function normalizeTagList(payload: unknown): LibraryTag[] {
  if (Array.isArray(payload)) {
    return payload.map((item) => normalizeTag(item)).filter(Boolean) as LibraryTag[];
  }
  const one = normalizeTag(payload);
  return one ? [one] : [];
}

function normalizeConnection(payload: unknown): LibraryConnection | null {
  if (!isRecord(payload)) return null;
  return {
    id: asNumber(payload.id),
    fromEntryId: asNumber(payload.fromEntryId),
    fromEntryTitle: asString(payload.fromEntryTitle),
    toEntryId: asNumber(payload.toEntryId),
    toEntryTitle: asString(payload.toEntryTitle),
    connectionType: asString(payload.connectionType),
    strength: asNumber(payload.strength),
    description: asOptionalString(payload.description),
    createdByUserId: asNumber(payload.createdByUserId),
    createdByName: asString(payload.createdByName),
    active: asBoolean(payload.active, true),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

function normalizeConnectionList(payload: unknown): LibraryConnection[] {
  if (Array.isArray(payload)) {
    return payload
      .map((item) => normalizeConnection(item))
      .filter(Boolean) as LibraryConnection[];
  }
  const one = normalizeConnection(payload);
  return one ? [one] : [];
}

function normalizeConnectedEntry(payload: unknown): ConnectedLibraryEntry | null {
  if (!isRecord(payload)) return null;
  return {
    connectionId: asNumber(payload.connectionId),
    fromEntryId: asNumber(payload.fromEntryId),
    toEntryId: asNumber(payload.toEntryId),
    entryId: asNumber(payload.entryId),
    entryTitle: asString(payload.entryTitle),
    entryContent: asString(payload.entryContent),
    tags: asStringArray(payload.tags),
    categoryId: asNumber(payload.categoryId),
    categoryName: asString(payload.categoryName),
    connectionType: asString(payload.connectionType),
    connectionStrength: asNumber(payload.connectionStrength),
    description: asOptionalString(payload.description),
    active: asBoolean(payload.active, true),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

function normalizeConnectedEntryList(payload: unknown): ConnectedLibraryEntry[] {
  if (Array.isArray(payload)) {
    return payload
      .map((item) => normalizeConnectedEntry(item))
      .filter(Boolean) as ConnectedLibraryEntry[];
  }
  const one = normalizeConnectedEntry(payload);
  return one ? [one] : [];
}

function normalizeEntryWithConnections(payload: unknown): LibraryEntryWithConnections | null {
  if (!isRecord(payload)) return null;
  const entry = normalizeEntry(payload.entry);
  if (!entry) return null;
  return {
    entry,
    connectedEntries: normalizeConnectedEntryList(payload.connectedEntries),
  };
}

function normalizeBulkImportError(payload: unknown): BulkLibraryEntriesImportError | null {
  if (!isRecord(payload)) return null;
  return {
    row: asNumber(payload.row),
    title: asOptionalString(payload.title),
    error: asString(payload.error) || "Import validation error",
  };
}

function normalizeBulkImportResponse(payload: unknown): BulkLibraryEntriesImportResponse {
  if (!isRecord(payload)) {
    return {
      total: 0,
      successful: 0,
      skipped: 0,
      failed: 0,
      categoriesCreated: 0,
      connectionsCreated: 0,
      errors: [],
    };
  }

  const errors = Array.isArray(payload.errors)
    ? payload.errors
        .map((entry) => normalizeBulkImportError(entry))
        .filter(Boolean) as BulkLibraryEntriesImportError[]
    : [];

  return {
    total: asNumber(payload.total),
    successful: asNumber(payload.successful),
    skipped: asNumber(payload.skipped),
    failed: asNumber(payload.failed),
    categoriesCreated: asNumber(payload.categoriesCreated),
    connectionsCreated: asNumber(payload.connectionsCreated),
    errors,
  };
}

function normalizeEntriesWithConnections(payload: unknown): LibraryEntryWithConnections[] {
  if (Array.isArray(payload)) {
    return payload
      .map((item) => normalizeEntryWithConnections(item))
      .filter(Boolean) as LibraryEntryWithConnections[];
  }
  const one = normalizeEntryWithConnections(payload);
  return one ? [one] : [];
}

export const libraryEntriesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getLibraryEntries: builder.query<LibraryEntry[], LibraryEntriesQueryArgs | void>({
      query: (params) => ({
        url: "/api/v1/library/entries",
        method: "GET",
        params: params ?? undefined,
      }),
      providesTags: ["LibraryEntries"],
      transformResponse: (payload) => normalizeEntryList(payload),
    }),
    getLibraryEntriesWithConnections: builder.query<
      LibraryEntryWithConnections[],
      { categoryId?: number } | void
    >({
      query: (params) => ({
        url: "/api/v1/library/entries/with-connections",
        method: "GET",
        params: params ?? undefined,
      }),
      providesTags: ["LibraryEntries", "LibraryConnections"],
      transformResponse: (payload) => normalizeEntriesWithConnections(payload),
    }),
    getLibraryEntryConnected: builder.query<ConnectedLibraryEntry[], number>({
      query: (id) => ({
        url: `/api/v1/library/entries/${id}/connected`,
        method: "GET",
      }),
      providesTags: (_result, _error, id) => [{ type: "LibraryConnections", id }],
      transformResponse: (payload) => normalizeConnectedEntryList(payload),
    }),
    searchLibraryEntries: builder.query<LibraryEntry[], LibrarySearchQueryArgs>({
      query: (params) => ({
        url: "/api/v1/library/search",
        method: "GET",
        params,
      }),
      providesTags: ["LibraryEntries"],
      transformResponse: (payload) => normalizeEntryList(payload),
    }),
    getLibraryEntryById: builder.query<LibraryEntry, number>({
      query: (id) => ({
        url: `/api/v1/library/entries/${id}`,
        method: "GET",
      }),
      providesTags: (_result, _error, id) => [{ type: "LibraryEntries", id }],
      transformResponse: (payload) =>
        normalizeEntry(payload) ?? {
          id: 0,
          categoryId: 0,
          categoryName: "",
          title: "",
          content: "",
          tags: [],
          createdById: 0,
          createdByName: "",
          isActive: true,
          sortOrder: 0,
          usageCount: 0,
        },
    }),
    createLibraryEntry: builder.mutation<LibraryEntry, UpsertLibraryEntryPayload>({
      query: (body) => ({
        url: "/api/v1/library/entries",
        method: "POST",
        body,
      }),
      invalidatesTags: ["LibraryEntries"],
      transformResponse: (payload) =>
        normalizeEntry(payload) ?? {
          id: 0,
          categoryId: 0,
          categoryName: "",
          title: "",
          content: "",
          tags: [],
          createdById: 0,
          createdByName: "",
          isActive: true,
          sortOrder: 0,
          usageCount: 0,
        },
    }),
    updateLibraryEntry: builder.mutation<
      LibraryEntry,
      { id: number; body: UpsertLibraryEntryPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/library/entries/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        "LibraryEntries",
        { type: "LibraryEntries", id },
      ],
      transformResponse: (payload) =>
        normalizeEntry(payload) ?? {
          id: 0,
          categoryId: 0,
          categoryName: "",
          title: "",
          content: "",
          tags: [],
          createdById: 0,
          createdByName: "",
          isActive: true,
          sortOrder: 0,
          usageCount: 0,
        },
    }),
    deleteLibraryEntry: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/library/entries/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["LibraryEntries"],
    }),
    bulkDeleteLibraryEntries: builder.mutation<
      {
        total: number;
        deleted: number;
        failed: number;
        errors: Array<{ entryId: number; error: string }>;
      },
      { entryIds: number[] }
    >({
      query: (body) => ({
        url: "/api/v1/library/entries/bulk",
        method: "DELETE",
        body,
      }),
      invalidatesTags: ["LibraryEntries", "LibraryConnections"],
    }),
    bulkUpdateLibraryEntryTags: builder.mutation<void, BulkLibraryEntryTagsPayload>({
      query: (body) => ({
        url: "/api/v1/library/entries/tags/bulk",
        method: "POST",
        body,
      }),
      invalidatesTags: ["LibraryEntries", "LibraryTags"],
    }),
    bulkImportLibraryEntries: builder.mutation<
      BulkLibraryEntriesImportResponse,
      BulkLibraryEntriesImportPayload
    >({
      query: (body) => ({
        url: "/api/v1/library/bulk-entries",
        method: "POST",
        body,
      }),
      invalidatesTags: ["LibraryEntries", "LibraryCategories"],
      transformResponse: (payload) => normalizeBulkImportResponse(payload),
    }),
    getLibraryTags: builder.query<LibraryTag[], void>({
      query: () => ({
        url: "/api/v1/library/tags",
        method: "GET",
      }),
      providesTags: ["LibraryTags"],
      transformResponse: (payload) => normalizeTagList(payload),
    }),
    getLibraryConnections: builder.query<LibraryConnection[], { entryId?: number } | void>({
      query: (params) => ({
        url: "/api/v1/library/connections",
        method: "GET",
        params: params ?? undefined,
      }),
      providesTags: ["LibraryConnections"],
      transformResponse: (payload) => normalizeConnectionList(payload),
    }),
    deleteLibraryConnection: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/library/connections/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["LibraryConnections"],
    }),
    deleteAllLibraryEntryConnections: builder.mutation<void, number>({
      query: (entryId) => ({
        url: `/api/v1/library/entries/${entryId}/connections`,
        method: "DELETE",
      }),
      invalidatesTags: ["LibraryConnections"],
    }),
    createLibraryConnection: builder.mutation<
      LibraryConnection,
      {
        fromEntryId: number;
        toEntryId: number;
        connectionType: string;
        strength: number;
        description?: string;
      }
    >({
      query: (body) => ({
        url: "/api/v1/library/connections",
        method: "POST",
        body,
      }),
      invalidatesTags: ["LibraryConnections"],
      transformResponse: (payload) =>
        normalizeConnection(payload) ?? {
          id: 0,
          fromEntryId: 0,
          fromEntryTitle: "",
          toEntryId: 0,
          toEntryTitle: "",
          connectionType: "Related",
          strength: 0,
          description: "",
          createdByUserId: 0,
          createdByName: "",
          active: true,
        },
    }),
    createLibraryConnectionsBatch: builder.mutation<
      unknown,
      {
        connections: Array<{
          fromEntryId: number;
          toEntryId: number;
          connectionType: string;
          strength: number;
          description?: string;
        }>;
      }
    >({
      query: (body) => ({
        url: "/api/v1/library/connections/batch",
        method: "POST",
        body,
      }),
      invalidatesTags: ["LibraryConnections"],
    }),
    incrementLibraryEntryUsage: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/library/entries/${id}/increment-usage`,
        method: "POST",
      }),
      invalidatesTags: ["LibraryEntries"],
    }),
  }),
});

export const {
  useGetLibraryEntriesQuery,
  useGetLibraryEntriesWithConnectionsQuery,
  useLazyGetLibraryEntryConnectedQuery,
  useLazySearchLibraryEntriesQuery,
  useLazyGetLibraryEntryByIdQuery,
  useCreateLibraryEntryMutation,
  useBulkUpdateLibraryEntryTagsMutation,
  useBulkImportLibraryEntriesMutation,
  useUpdateLibraryEntryMutation,
  useDeleteLibraryEntryMutation,
  useBulkDeleteLibraryEntriesMutation,
  useGetLibraryTagsQuery,
  useGetLibraryConnectionsQuery,
  useDeleteLibraryConnectionMutation,
  useDeleteAllLibraryEntryConnectionsMutation,
  useCreateLibraryConnectionMutation,
  useCreateLibraryConnectionsBatchMutation,
  useIncrementLibraryEntryUsageMutation,
} = libraryEntriesApi;
