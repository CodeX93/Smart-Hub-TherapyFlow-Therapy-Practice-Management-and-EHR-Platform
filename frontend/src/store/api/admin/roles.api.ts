import { baseApi } from "../baseApi";

export interface AdminRolePermission {
  id: number;
  name: string;
  displayName?: string;
  description?: string;
  category?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminRole {
  id: number;
  name: string;
  displayName?: string;
  description?: string;
  isSystem?: boolean;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
  permissions?: AdminRolePermission[] | null;
  permissionsAssignedCount?: number;
  userCount?: number;
}

export interface AdminRolesPagedResponse {
  items: AdminRole[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AdminRolesPagedParams {
  search?: string;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDirection?: "asc" | "desc";
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  return 0;
}

function asOptionalString(value: unknown): string | undefined {
  const parsed = asString(value).trim();
  return parsed || undefined;
}

function asOptionalBoolean(value: unknown): boolean | undefined {
  return typeof value === "boolean" ? value : undefined;
}

function mapPermission(payload: unknown): AdminRolePermission | null {
  if (!isRecord(payload)) return null;
  const id = asNumber(payload.id ?? payload.permissionId);
  const name = asString(payload.name ?? payload.permissionName).trim();
  if (!id && !name) return null;
  return {
    id,
    name,
    displayName: asOptionalString(payload.displayName),
    description: asOptionalString(payload.description),
    category: asOptionalString(payload.category),
    isActive: asOptionalBoolean(payload.isActive),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

function mapRolePermissions(payload: Record<string, unknown>): AdminRolePermission[] | null {
  const rawPermissions = Array.isArray(payload.permissions)
    ? payload.permissions
    : Array.isArray(payload.permissionIds)
      ? payload.permissionIds
      : null;
  if (!rawPermissions) return null;

  return rawPermissions
    .map((entry) => {
      if (typeof entry === "number" && Number.isFinite(entry) && entry > 0) {
        return { id: entry, name: "" } satisfies AdminRolePermission;
      }
      if (typeof entry === "string" && entry.trim()) {
        const parsed = Number(entry);
        if (Number.isFinite(parsed) && parsed > 0) {
          return { id: parsed, name: "" } satisfies AdminRolePermission;
        }
        return { id: 0, name: entry.trim() } satisfies AdminRolePermission;
      }
      return mapPermission(entry);
    })
    .filter(Boolean) as AdminRolePermission[];
}

function mapRole(payload: unknown): AdminRole | null {
  if (!isRecord(payload)) return null;
  return {
    id: asNumber(payload.id),
    name: asString(payload.name),
    displayName: asOptionalString(payload.displayName),
    description: asOptionalString(payload.description),
    isSystem: asOptionalBoolean(payload.isSystem),
    isActive: asOptionalBoolean(payload.isActive),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
    permissions: mapRolePermissions(payload),
    permissionsAssignedCount: asNumber(payload.permissionsAssignedCount),
    userCount: asNumber(payload.userCount),
  };
}

function mapRolesPaged(payload: unknown, args?: AdminRolesPagedParams | void): AdminRolesPagedResponse {
  const root = isRecord(payload) ? payload : {};
  const itemsRaw = Array.isArray(root.items) ? root.items : [];
  const items = itemsRaw.map((entry) => mapRole(entry)).filter(Boolean) as AdminRole[];
  const page = Math.max(1, asNumber(root.page) || args?.page || 1);
  const pageSize = Math.max(1, asNumber(root.pageSize) || args?.pageSize || 20);
  const totalCount = Math.max(asNumber(root.totalCount) || items.length, items.length);
  const totalPages = Math.max(asNumber(root.totalPages) || Math.ceil(totalCount / pageSize), 1);
  return { items, totalCount, page, pageSize, totalPages };
}

export const adminRolesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminRolesPaged: builder.query<AdminRolesPagedResponse, AdminRolesPagedParams | void>({
      query: (params) => {
        const query = new URLSearchParams();
        if (params?.search?.trim()) query.set("search", params.search.trim());
        query.set("page", String(Math.max(1, params?.page ?? 1)));
        query.set("pageSize", String(Math.max(1, params?.pageSize ?? 20)));
        if (params?.sortBy?.trim()) query.set("sortBy", params.sortBy.trim());
        if (params?.sortDirection) query.set("sortDirection", params.sortDirection);
        return `/api/v1/tenant-admin/roles/paged?${query.toString()}`;
      },
      transformResponse: (payload, _meta, args) => mapRolesPaged(payload, args),
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((role) => ({
                type: "AdminRoles" as const,
                id: role.id,
              })),
              { type: "AdminRoles", id: "LIST" },
            ]
          : [{ type: "AdminRoles", id: "LIST" }],
    }),
    getPermissionsList: builder.query<AdminRolePermission[], void>({
      query: () => "/api/v1/tenant-admin/permissions",
      transformResponse: (payload) =>
        Array.isArray(payload)
          ? payload.map((entry) => mapPermission(entry)).filter(Boolean) as AdminRolePermission[]
          : [],
    }),
    getTenantAdminRolesPaged: builder.query<AdminRolesPagedResponse, AdminRolesPagedParams | void>({
      query: (params) => {
        const query = new URLSearchParams();
        if (params?.search?.trim()) query.set("search", params.search.trim());
        query.set("page", String(Math.max(1, params?.page ?? 1)));
        query.set("pageSize", String(Math.max(1, params?.pageSize ?? 20)));
        if (params?.sortBy?.trim()) query.set("sortBy", params.sortBy.trim());
        if (params?.sortDirection) query.set("sortDirection", params.sortDirection);
        return `/api/v1/tenant-admin/roles/paged?${query.toString()}`;
      },
      transformResponse: (payload, _meta, args) => mapRolesPaged(payload, args),
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((role) => ({
                type: "AdminRoles" as const,
                id: role.id,
              })),
              { type: "AdminRoles", id: "LIST" },
            ]
          : [{ type: "AdminRoles", id: "LIST" }],
    }),
    createRole: builder.mutation<AdminRole, {
      name: string;
      displayName: string;
      description?: string;
      isSystem?: boolean;
      isActive?: boolean;
      permissions?: number[];
    }>({
      query: (body) => ({
        url: "/api/v1/tenant-admin/roles",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => mapRole(payload) ?? { id: 0, name: "" },
      invalidatesTags: [{ type: "AdminRoles", id: "LIST" }],
    }),
    getRoleById: builder.query<AdminRole, number>({
      query: (id) => `/api/v1/tenant-admin/roles/${id}`,
      transformResponse: (payload) => mapRole(payload) ?? { id: 0, name: "" },
      providesTags: (_result, _error, id) => [{ type: "AdminRoles", id }],
    }),
    updateRole: builder.mutation<AdminRole, {
      id: number;
      body: {
        name: string;
        displayName: string;
        description?: string;
        isSystem?: boolean;
        isActive?: boolean;
        permissions?: number[];
      };
    }>({
      query: ({ id, body }) => ({
        url: `/api/v1/tenant-admin/roles/${id}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => mapRole(payload) ?? { id: 0, name: "" },
      invalidatesTags: (_result, _error, { id }) => [
        { type: "AdminRoles", id },
        { type: "AdminRoles", id: "LIST" },
      ],
    }),
    updateRolePermissions: builder.mutation<unknown, {
      id: number;
      permissionIds: number[];
    }>({
      query: ({ id, permissionIds }) => ({
        url: `/api/v1/tenant-admin/roles/${id}/permissions`,
        method: "PUT",
        body: { permissionIds },
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "AdminRoles", id },
        { type: "AdminRoles", id: "LIST" },
      ],
    }),
    deleteRole: builder.mutation<void, number>({
      query: (roleId) => ({
        url: `/api/v1/tenant-admin/roles/${roleId}`,
        method: "DELETE",
      }),
      invalidatesTags: [{ type: "AdminRoles", id: "LIST" }],
    }),
  }),
});

export const {
  useGetAdminRolesPagedQuery,
  useGetTenantAdminRolesPagedQuery,
  useLazyGetTenantAdminRolesPagedQuery,
  useGetPermissionsListQuery,
  useCreateRoleMutation,
  useLazyGetRoleByIdQuery,
  useUpdateRoleMutation,
  useDeleteRoleMutation,
} = adminRolesApi;
