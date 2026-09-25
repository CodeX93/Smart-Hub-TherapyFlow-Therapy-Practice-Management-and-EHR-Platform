import { baseApi } from "../baseApi";

export interface SystemOptionValue {
  id: number;
  categoryId: number;
  categoryKey: string;
  categoryName: string;
  optionKey: string;
  optionLabel: string;
  sortOrder: number;
  isDefault: boolean;
  isSystem: boolean;
  isActive: boolean;
  price: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface SystemOptionCategory {
  id: number;
  categoryKey: string;
  categoryName: string;
  description?: string;
  isSystem: boolean;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  options: SystemOptionValue[];
}

export interface CreateSystemOptionCategoryPayload {
  categoryKey: string;
  categoryName: string;
  description?: string;
  isSystem?: boolean;
  isActive?: boolean;
}

export interface CreateSystemOptionPayload {
  categoryId: number;
  optionKey: string;
  optionLabel: string;
  sortOrder?: number;
  isDefault?: boolean;
  isSystem?: boolean;
  isActive?: boolean;
  price?: number;
}

export type UpdateSystemOptionPayload = Omit<CreateSystemOptionPayload, "isSystem">;

export interface PracticeConfiguration {
  id?: number;
  practiceName?: string;
  practiceAddress?: string;
  practicePhone?: string;
  practiceEmail?: string;
  practiceWebsite?: string;
  taxId?: string;
  licenseNumber?: string;
  licenseState?: string;
  npiNumber?: string;
  description?: string;
  subtitle?: string;
  timezone?: string;
  createdAt?: string;
  updatedAt?: string;
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

function normalizeOption(entry: unknown): SystemOptionValue | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    categoryId: asNumber(entry.categoryId),
    categoryKey: asString(entry.categoryKey),
    categoryName: asString(entry.categoryName),
    optionKey: asString(entry.optionKey),
    optionLabel: asString(entry.optionLabel),
    sortOrder: asNumber(entry.sortOrder),
    isDefault: asBoolean(entry.isDefault),
    isSystem: asBoolean(entry.isSystem),
    isActive: asBoolean(entry.isActive, true),
    price: asNumber(entry.price),
    createdAt: asOptionalString(entry.createdAt),
    updatedAt: asOptionalString(entry.updatedAt),
  };
}

function normalizeCategory(entry: unknown): SystemOptionCategory | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    categoryKey: asString(entry.categoryKey),
    categoryName: asString(entry.categoryName),
    description: asOptionalString(entry.description),
    isSystem: asBoolean(entry.isSystem),
    isActive: asBoolean(entry.isActive, true),
    createdAt: asOptionalString(entry.createdAt),
    updatedAt: asOptionalString(entry.updatedAt),
    options: Array.isArray(entry.options)
      ? entry.options
        .map((optionEntry) => normalizeOption(optionEntry))
        .filter(Boolean) as SystemOptionValue[]
      : [],
  };
}

function normalizeCategories(payload: unknown): SystemOptionCategory[] {
  if (!Array.isArray(payload)) return [];
  return payload
    .map((entry) => normalizeCategory(entry))
    .filter(Boolean) as SystemOptionCategory[];
}

function normalizePracticeConfiguration(payload: unknown): PracticeConfiguration {
  const root = isRecord(payload) ? payload : {};
  return {
    id: asNumber(root.id),
    practiceName: asOptionalString(root.practiceName),
    practiceAddress: asOptionalString(root.practiceAddress),
    practicePhone: asOptionalString(root.practicePhone),
    practiceEmail: asOptionalString(root.practiceEmail),
    practiceWebsite: asOptionalString(root.practiceWebsite),
    taxId: asOptionalString(root.taxId),
    licenseNumber: asOptionalString(root.licenseNumber),
    licenseState: asOptionalString(root.licenseState),
    npiNumber: asOptionalString(root.npiNumber),
    description: asOptionalString(root.description),
    subtitle: asOptionalString(root.subtitle),
    timezone: asOptionalString(root.timezone),
    createdAt: asOptionalString(root.createdAt),
    updatedAt: asOptionalString(root.updatedAt),
  };
}

export const adminSystemOptionsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSystemOptionCategories: builder.query<
      SystemOptionCategory[],
      { includeInactive?: boolean } | void
    >({
      query: (args) => ({
        url: "/api/v1/system-options/categories",
        params: args?.includeInactive ? { includeInactive: true } : undefined,
      }),
      providesTags: ["SystemOptionCategories"],
      transformResponse: (payload) => normalizeCategories(payload),
    }),
    getSystemOptionsByCategory: builder.query<SystemOptionValue[], string>({
      query: (categoryKey) => `/api/v1/system-options/by-category/${categoryKey}`,
      providesTags: (_result, _error, categoryKey) => [
        { type: "SystemOptionCategories", id: categoryKey },
      ],
      transformResponse: (payload) => {
        if (!Array.isArray(payload)) return [];
        return payload
          .map((entry) => normalizeOption(entry))
          .filter(Boolean) as SystemOptionValue[];
      },
    }),
    createSystemOptionCategory: builder.mutation<
      SystemOptionCategory,
      CreateSystemOptionCategoryPayload
    >({
      query: (body) => ({
        url: "/api/v1/system-options/categories",
        method: "POST",
        body,
      }),
      invalidatesTags: ["SystemOptionCategories"],
      transformResponse: (payload) =>
        normalizeCategory(payload) ?? {
          id: 0,
          categoryKey: "",
          categoryName: "",
          isSystem: false,
          isActive: true,
          options: [],
        },
    }),
    getSystemOptionCategoryById: builder.query<
      SystemOptionCategory,
      { id: number; includeInactive?: boolean }
    >({
      query: ({ id, includeInactive }) => ({
        url: `/api/v1/system-options/categories/${id}`,
        params: includeInactive ? { includeInactive: true } : undefined,
      }),
      providesTags: (_result, _error, { id }) => [{ type: "SystemOptionCategories", id }],
      transformResponse: (payload) =>
        normalizeCategory(payload) ?? {
          id: 0,
          categoryKey: "",
          categoryName: "",
          isSystem: false,
          isActive: true,
          options: [],
        },
    }),
    updateSystemOptionCategory: builder.mutation<
      SystemOptionCategory,
      { id: number; body: CreateSystemOptionCategoryPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/system-options/categories/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        "SystemOptionCategories",
        { type: "SystemOptionCategories", id },
      ],
      transformResponse: (payload) =>
        normalizeCategory(payload) ?? {
          id: 0,
          categoryKey: "",
          categoryName: "",
          isSystem: false,
          isActive: true,
          options: [],
        },
    }),
    createSystemOption: builder.mutation<SystemOptionValue, CreateSystemOptionPayload>({
      query: (body) => ({
        url: "/api/v1/system-options",
        method: "POST",
        body,
      }),
      invalidatesTags: ["SystemOptionCategories"],
      transformResponse: (payload) =>
        normalizeOption(payload) ?? {
          id: 0,
          categoryId: 0,
          categoryKey: "",
          categoryName: "",
          optionKey: "",
          optionLabel: "",
          sortOrder: 0,
          isDefault: false,
          isSystem: false,
          isActive: true,
          price: 0,
        },
    }),
    getSystemOptionById: builder.query<SystemOptionValue, number>({
      query: (id) => `/api/v1/system-options/${id}`,
      transformResponse: (payload) =>
        normalizeOption(payload) ?? {
          id: 0,
          categoryId: 0,
          categoryKey: "",
          categoryName: "",
          optionKey: "",
          optionLabel: "",
          sortOrder: 0,
          isDefault: false,
          isSystem: false,
          isActive: true,
          price: 0,
        },
    }),
    updateSystemOption: builder.mutation<
      SystemOptionValue,
      { id: number; oldOptionKey: string; body: UpdateSystemOptionPayload }
    >({
      query: ({ id, oldOptionKey, body }) => {
        const updateBody = { ...body } as Partial<CreateSystemOptionPayload>;
        delete updateBody.isSystem;
        return {
          url: `/api/v1/system-options/${id}`,
          method: "PUT",
          params: { oldOptionKey },
          body: updateBody,
        };
      },
      invalidatesTags: ["SystemOptionCategories"],
      transformResponse: (payload) =>
        normalizeOption(payload) ?? {
          id: 0,
          categoryId: 0,
          categoryKey: "",
          categoryName: "",
          optionKey: "",
          optionLabel: "",
          sortOrder: 0,
          isDefault: false,
          isSystem: false,
          isActive: true,
          price: 0,
        },
    }),
    deleteSystemOptionCategory: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/system-options/categories/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["SystemOptionCategories"],
    }),
    deleteSystemOption: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/system-options/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["SystemOptionCategories"],
    }),
    reorderCategoryOptions: builder.mutation<void, { categoryId: number; options: { optionId: number; sortOrder: number }[] }>({
      query: ({ categoryId, options }) => ({
        url: `/api/v1/system-options/categories/${categoryId}/options/order`,
        method: "PUT",
        body: { options },
      }),
      invalidatesTags: ["SystemOptionCategories"],
    }),
    getPracticeConfiguration: builder.query<PracticeConfiguration, void>({
      query: () => "/api/v1/practice-configuration",
      providesTags: ["PracticeConfiguration"],
      transformResponse: (payload) => normalizePracticeConfiguration(payload),
    }),
    updatePracticeConfiguration: builder.mutation<
      PracticeConfiguration,
      Partial<PracticeConfiguration>
    >({
      query: (body) => ({
        url: "/api/v1/practice-configuration",
        method: "PUT",
        body,
      }),
      invalidatesTags: ["PracticeConfiguration"],
      transformResponse: (payload) => normalizePracticeConfiguration(payload),
    }),
  }),
});

export const {
  useGetSystemOptionCategoriesQuery,
  useGetSystemOptionsByCategoryQuery,
  useLazyGetSystemOptionsByCategoryQuery,
  useCreateSystemOptionCategoryMutation,
  useLazyGetSystemOptionCategoryByIdQuery,
  useUpdateSystemOptionCategoryMutation,
  useCreateSystemOptionMutation,
  useLazyGetSystemOptionByIdQuery,
  useUpdateSystemOptionMutation,
  useDeleteSystemOptionCategoryMutation,
  useDeleteSystemOptionMutation,
  useReorderCategoryOptionsMutation,
  useGetPracticeConfigurationQuery,
  useUpdatePracticeConfigurationMutation,
} = adminSystemOptionsApi;
