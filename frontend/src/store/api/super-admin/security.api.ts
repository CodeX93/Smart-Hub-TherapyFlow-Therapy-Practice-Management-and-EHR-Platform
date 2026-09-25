import { baseApi } from "../baseApi";
import * as Shared from "./shared";

export const superAdminSecurityApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSuperAdminSecuritySettings: builder.query<Shared.SuperAdminSecuritySettings, void>({
      query: () => "/api/v1/super-admin/security",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: typeof root.id === "number" ? root.id : undefined,
          enabled: Boolean(root.enabled),
          requireReason: Boolean(root.requireReason),
          minReasonLength: Shared.getNumber(root, ["minReasonLength"]),
          maxDurationMinutes: Shared.getNumber(root, ["maxDurationMinutes"]),
          allowCrossOrganisation: Boolean(root.allowCrossOrganisation),
          allowedRoles: Array.isArray(root.allowedRoles) ? (root.allowedRoles as string[]) : [],
          deniedRoles: Array.isArray(root.deniedRoles) ? (root.deniedRoles as string[]) : [],
          allowedOrgIds: Array.isArray(root.allowedOrgIds) ? (root.allowedOrgIds as number[]) : [],
          deniedOrgIds: Array.isArray(root.deniedOrgIds) ? (root.deniedOrgIds as number[]) : [],
          updatedByAuthId:
            typeof root.updatedByAuthId === "number" ? root.updatedByAuthId : undefined,
          createdAt: Shared.getString(root, ["createdAt"]) || undefined,
          updatedAt: Shared.getString(root, ["updatedAt"]) || undefined,
        };
      },
    }),
    updateSuperAdminSecuritySettings: builder.mutation<
      Shared.SuperAdminSecuritySettings,
      Omit<
        Shared.SuperAdminSecuritySettings,
        "id" | "updatedByAuthId" | "createdAt" | "updatedAt"
      >
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/security",
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: typeof root.id === "number" ? root.id : undefined,
          enabled: Boolean(root.enabled),
          requireReason: Boolean(root.requireReason),
          minReasonLength: Shared.getNumber(root, ["minReasonLength"]),
          maxDurationMinutes: Shared.getNumber(root, ["maxDurationMinutes"]),
          allowCrossOrganisation: Boolean(root.allowCrossOrganisation),
          allowedRoles: Array.isArray(root.allowedRoles) ? (root.allowedRoles as string[]) : [],
          deniedRoles: Array.isArray(root.deniedRoles) ? (root.deniedRoles as string[]) : [],
          allowedOrgIds: Array.isArray(root.allowedOrgIds) ? (root.allowedOrgIds as number[]) : [],
          deniedOrgIds: Array.isArray(root.deniedOrgIds) ? (root.deniedOrgIds as number[]) : [],
          updatedByAuthId:
            typeof root.updatedByAuthId === "number" ? root.updatedByAuthId : undefined,
          createdAt: Shared.getString(root, ["createdAt"]) || undefined,
          updatedAt: Shared.getString(root, ["updatedAt"]) || undefined,
        };
      },
    }),
    getTenantRoutingSettings: builder.query<Shared.TenantRoutingSettings, void>({
      query: () => "/api/v1/super-admin/settings/tenant-routing",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          emailAutoRouting: Boolean(root.emailAutoRouting),
          pathBasedRouting: Boolean(root.pathBasedRouting),
          pathPrefix: Shared.getString(root, ["pathPrefix"]) || "/org",
          orgIdentifier: Shared.getString(root, ["orgIdentifier"]) || "slug",
          updatedAt: Shared.getString(root, ["updatedAt"]) || new Date().toISOString(),
        };
      },
    }),
    updateTenantRoutingSettings: builder.mutation<Shared.TenantRoutingSettings, Omit<Shared.TenantRoutingSettings, "updatedAt">>({
      query: (body) => ({
        url: "/api/v1/super-admin/settings/tenant-routing",
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          emailAutoRouting: Boolean(root.emailAutoRouting),
          pathBasedRouting: Boolean(root.pathBasedRouting),
          pathPrefix: Shared.getString(root, ["pathPrefix"]) || "/org",
          orgIdentifier: Shared.getString(root, ["orgIdentifier"]) || "slug",
          updatedAt: Shared.getString(root, ["updatedAt"]) || new Date().toISOString(),
        };
      },
    }),
    getImpersonationPolicy: builder.query<Shared.ImpersonationPolicy, void>({
      query: () => "/api/v1/super-admin/impersonation/policy",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: root.id as number | undefined,
          enabled: Boolean(root.enabled),
          requireReason: Boolean(root.requireReason),
          minReasonLength: typeof root.minReasonLength === "number" ? root.minReasonLength : 0,
          maxDurationMinutes: typeof root.maxDurationMinutes === "number" ? root.maxDurationMinutes : 60,
          allowCrossOrganisation: Boolean(root.allowCrossOrganisation),
          allowedRoles: Array.isArray(root.allowedRoles) ? root.allowedRoles as string[] : [],
          deniedRoles: Array.isArray(root.deniedRoles) ? root.deniedRoles as string[] : [],
          allowedOrgIds: Array.isArray(root.allowedOrgIds) ? root.allowedOrgIds as number[] : [],
          deniedOrgIds: Array.isArray(root.deniedOrgIds) ? root.deniedOrgIds as number[] : [],
          updatedByAuthId: root.updatedByAuthId as number | undefined,
          createdAt: root.createdAt as string | undefined,
          updatedAt: root.updatedAt as string | undefined,
        };
      },
    }),
    updateImpersonationPolicy: builder.mutation<Shared.ImpersonationPolicy, Shared.UpdateImpersonationPolicyPayload>({
      query: (body) => ({
        url: "/api/v1/super-admin/impersonation/policy",
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: root.id as number | undefined,
          enabled: Boolean(root.enabled),
          requireReason: Boolean(root.requireReason),
          minReasonLength: typeof root.minReasonLength === "number" ? root.minReasonLength : 0,
          maxDurationMinutes: typeof root.maxDurationMinutes === "number" ? root.maxDurationMinutes : 60,
          allowCrossOrganisation: Boolean(root.allowCrossOrganisation),
          allowedRoles: Array.isArray(root.allowedRoles) ? root.allowedRoles as string[] : [],
          deniedRoles: Array.isArray(root.deniedRoles) ? root.deniedRoles as string[] : [],
          allowedOrgIds: Array.isArray(root.allowedOrgIds) ? root.allowedOrgIds as number[] : [],
          deniedOrgIds: Array.isArray(root.deniedOrgIds) ? root.deniedOrgIds as number[] : [],
          updatedByAuthId: root.updatedByAuthId as number | undefined,
          createdAt: root.createdAt as string | undefined,
          updatedAt: root.updatedAt as string | undefined,
        };
      },
    }),
  }),
});

export const {
  useGetSuperAdminSecuritySettingsQuery,
  useUpdateSuperAdminSecuritySettingsMutation,
  useGetTenantRoutingSettingsQuery,
  useUpdateTenantRoutingSettingsMutation,
  useGetImpersonationPolicyQuery,
  useUpdateImpersonationPolicyMutation,
} = superAdminSecurityApi;
