import { baseApi } from "../baseApi";
import * as Shared from "./shared";

export const superAdminRolesPermissionsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getRolesPermissionsMatrix: builder.query<
      Shared.RolesPermissionsMatrixDetailed,
      Shared.RolesPermissionsMatrixFilters | void
    >({
      query: (filters) => {
        const query = new URLSearchParams();
        if (filters?.module?.trim()) {
          query.set("module", filters.module.trim());
        }
        if (filters?.permissionGroup?.trim()) {
          query.set("permissionGroup", filters.permissionGroup.trim());
        }
        const qs = query.toString();
        return `/api/v1/super-admin/users/roles-permissions-matrix${qs ? `?${qs}` : ""}`;
      },
      providesTags: ["RolesPermissionsMatrix"],
      transformResponse: (payload) => Shared.mapRolesPermissionsMatrixDetailed(payload),
    }),
    getPermissionsCatalog: builder.query<Shared.PermissionCatalogSection[], void>({
      query: () => "/api/v1/super-admin/permissions/catalog",
      transformResponse: (payload) => Shared.mapPermissionsCatalogResponse(payload),
    }),
    createSuperAdminRole: builder.mutation<void, Shared.CreateSuperAdminRolePayload>({
      query: (body) => ({
        url: "/api/v1/super-admin/roles",
        method: "POST",
        body,
      }),
      invalidatesTags: ["RolesPermissionsMatrix"],
    }),
    getSuperAdminRoleById: builder.query<Record<string, unknown>, number>({
      query: (roleId) => `/api/v1/super-admin/roles/${roleId}`,
      transformResponse: (payload) => Shared.extractRoot(payload),
    }),
    updateSuperAdminRole: builder.mutation<void, Shared.UpdateSuperAdminRolePayload>({
      query: ({ roleId, body }) => ({
        url: `/api/v1/super-admin/roles/${roleId}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: ["RolesPermissionsMatrix"],
    }),
    deleteSuperAdminRole: builder.mutation<void, number>({
      query: (roleId) => ({
        url: `/api/v1/super-admin/roles/${roleId}`,
        method: "DELETE",
      }),
      invalidatesTags: ["RolesPermissionsMatrix"],
    }),
    getRolesPermissionsMatrixExport: builder.query<
      Blob,
      Shared.RolesPermissionsMatrixFilters | void
    >({
      query: (filters) => {
        const query = new URLSearchParams();
        if (filters?.module?.trim()) {
          query.set("module", filters.module.trim());
        }
        if (filters?.permissionGroup?.trim()) {
          query.set("permissionGroup", filters.permissionGroup.trim());
        }
        const qs = query.toString();
        return {
          url: `/api/v1/super-admin/users/roles-permissions-matrix/export${qs ? `?${qs}` : ""}`,
          headers: {
            Accept: "text/csv",
          },
          responseHandler: async (response) => response.blob(),
        };
      },
      keepUnusedDataFor: 0,
    }),
    updateRolesPermissionsMatrix: builder.mutation<
      Record<string, unknown>,
      { updates: Shared.RolesPermissionsMatrixUpdateItem[] }
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/users/roles-permissions-matrix",
        method: "PUT",
        body,
      }),
      invalidatesTags: ["RolesPermissionsMatrix"],
      transformResponse: (payload) => Shared.extractRoot(payload),
    }),
    toggleRolesPermissionsMatrixCell: builder.mutation<
      Record<string, unknown>,
      Shared.RolesPermissionsMatrixUpdateItem
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/users/roles-permissions-matrix/toggle",
        method: "PATCH",
        body,
      }),
      invalidatesTags: ["RolesPermissionsMatrix"],
      transformResponse: (payload) => Shared.extractRoot(payload),
    }),
  }),
});

export const {
  useGetRolesPermissionsMatrixQuery,
  useGetPermissionsCatalogQuery,
  useCreateSuperAdminRoleMutation,
  useLazyGetSuperAdminRoleByIdQuery,
  useUpdateSuperAdminRoleMutation,
  useDeleteSuperAdminRoleMutation,
  useGetRolesPermissionsMatrixExportQuery,
  useLazyGetRolesPermissionsMatrixExportQuery,
  useUpdateRolesPermissionsMatrixMutation,
  useToggleRolesPermissionsMatrixCellMutation,
} = superAdminRolesPermissionsApi;
