import { baseApi } from "../baseApi";

export interface LibraryCategory {
  id: number;
  name: string;
  description?: string;
  parentId?: number | null;
  parentName?: string;
  sortOrder: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateLibraryCategoryPayload {
  name: string;
  description?: string;
  parentId?: number;
  sortOrder?: number;
  isActive?: boolean;
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

function asOptionalNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === "") return null;
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number.parseFloat(value);
    return Number.isNaN(parsed) ? null : parsed;
  }
  return null;
}

function asBoolean(value: unknown, fallback = false): boolean {
  return typeof value === "boolean" ? value : fallback;
}

function normalizeCategory(entry: unknown): LibraryCategory | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    name: asString(entry.name),
    description: asOptionalString(entry.description),
    parentId: asOptionalNumber(entry.parentId),
    parentName: asOptionalString(entry.parentName),
    sortOrder: asNumber(entry.sortOrder),
    isActive: asBoolean(entry.isActive, true),
    createdAt: asOptionalString(entry.createdAt),
    updatedAt: asOptionalString(entry.updatedAt),
  };
}

function normalizeCategories(payload: unknown): LibraryCategory[] {
  if (Array.isArray(payload)) {
    return payload
      .map((entry) => normalizeCategory(entry))
      .filter(Boolean) as LibraryCategory[];
  }
  const one = normalizeCategory(payload);
  return one ? [one] : [];
}

export const libraryCategoriesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getLibraryCategories: builder.query<LibraryCategory[], void>({
      query: () => ({
        url: "/api/v1/library/categories",
        method: "GET",
      }),
      providesTags: ["LibraryCategories"],
      transformResponse: (payload) => normalizeCategories(payload),
    }),
    createLibraryCategory: builder.mutation<LibraryCategory, CreateLibraryCategoryPayload>({
      query: (body) => ({
        url: "/api/v1/library/categories",
        method: "POST",
        body,
      }),
      invalidatesTags: ["LibraryCategories"],
      transformResponse: (payload) =>
        normalizeCategory(payload) ?? {
          id: 0,
          name: "",
          description: "",
          parentId: null,
          parentName: "",
          sortOrder: 0,
          isActive: true,
        },
    }),
    getLibraryCategoryById: builder.query<LibraryCategory, number>({
      query: (id) => ({
        url: `/api/v1/library/categories/${id}`,
        method: "GET",
      }),
      providesTags: (_result, _error, id) => [{ type: "LibraryCategories", id }],
      transformResponse: (payload) =>
        normalizeCategory(payload) ?? {
          id: 0,
          name: "",
          description: "",
          parentId: null,
          parentName: "",
          sortOrder: 0,
          isActive: true,
        },
    }),
    updateLibraryCategory: builder.mutation<
      LibraryCategory,
      { id: number; body: CreateLibraryCategoryPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/library/categories/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        "LibraryCategories",
        { type: "LibraryCategories", id },
      ],
      transformResponse: (payload) =>
        normalizeCategory(payload) ?? {
          id: 0,
          name: "",
          description: "",
          parentId: null,
          parentName: "",
          sortOrder: 0,
          isActive: true,
        },
    }),
    deleteLibraryCategory: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/library/categories/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["LibraryCategories"],
    }),
  }),
});

export const {
  useGetLibraryCategoriesQuery,
  useCreateLibraryCategoryMutation,
  useLazyGetLibraryCategoryByIdQuery,
  useUpdateLibraryCategoryMutation,
  useDeleteLibraryCategoryMutation,
} = libraryCategoriesApi;
