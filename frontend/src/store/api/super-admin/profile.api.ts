import { baseApi } from "../baseApi";
import * as Shared from "./shared";

export const superAdminProfileApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSuperAdminMe: builder.query<Shared.SuperAdminProfileInfo, void>({
      query: () => "/api/v1/super-admin/me",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: Shared.getNumber(root, ["id"]),
          username: Shared.getString(root, ["username"]),
          fullName: Shared.getString(root, ["fullName", "name"]),
          email: Shared.getString(root, ["email"]),
          phone: Shared.getString(root, ["phone", "phoneNumber"]),
          profilePicture: Shared.getString(root, ["profilePicture", "avatarUrl", "photoUrl"]),
          active: root.active !== false,
          roles: Array.isArray(root.roles)
            ? (root.roles.filter((role): role is string => typeof role === "string"))
            : [],
          createdAt: Shared.getString(root, ["createdAt"]),
          updatedAt: Shared.getString(root, ["updatedAt"]),
        };
      },
    }),
    updateSuperAdminMe: builder.mutation<
      Shared.SuperAdminProfileInfo,
      Shared.UpdateSuperAdminProfilePayload
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/me",
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: Shared.getNumber(root, ["id"]),
          username: Shared.getString(root, ["username"]),
          fullName: Shared.getString(root, ["fullName", "name"]),
          email: Shared.getString(root, ["email"]),
          phone: Shared.getString(root, ["phone", "phoneNumber"]),
          profilePicture: Shared.getString(root, ["profilePicture", "avatarUrl", "photoUrl"]),
          active: root.active !== false,
          roles: Array.isArray(root.roles)
            ? (root.roles.filter((role): role is string => typeof role === "string"))
            : [],
          createdAt: Shared.getString(root, ["createdAt"]),
          updatedAt: Shared.getString(root, ["updatedAt"]),
        };
      },
    }),
    getSuperAdminMeProfile: builder.query<Shared.SuperAdminMeProfile, void>({
      query: () => "/api/v1/super-admin/me/profile",
      transformResponse: (payload) => Shared.mapSuperAdminMeProfile(payload),
    }),
    updateSuperAdminMeProfile: builder.mutation<
      Shared.SuperAdminMeProfile,
      Shared.UpdateSuperAdminMeProfilePayload
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/me/profile",
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => Shared.mapSuperAdminMeProfile(payload),
    }),
    uploadSuperAdminProfilePicture: builder.mutation<
      Shared.SuperAdminProfilePictureUploadResponse,
      FormData
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/me/profile-picture",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => {
        const mapped = Shared.mapSuperAdminMeProfile(payload);
        const root = Shared.extractRoot(payload);
        return {
          ...mapped,
          supported:
            typeof root.supported === "boolean" ? root.supported : undefined,
          message: Shared.getString(root, ["message"]) || undefined,
        };
      },
    }),
    getSuperAdmin2faStatus: builder.query<Shared.SuperAdmin2faStatus, void>({
      query: () => "/api/v1/super-admin/me/2fa",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          message: Shared.getString(root, ["message"]),
          supported: Boolean(root.supported),
          enabled: Boolean(root.enabled),
        };
      },
    }),
    enableSuperAdmin2fa: builder.mutation<Shared.SuperAdmin2faStatus, void>({
      query: () => ({
        url: "/api/v1/super-admin/me/2fa/enable",
        method: "POST",
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          message: Shared.getString(root, ["message"]),
          supported: Boolean(root.supported),
          enabled: root.enabled !== false,
        };
      },
    }),
    disableSuperAdmin2fa: builder.mutation<Shared.SuperAdmin2faStatus, void>({
      query: () => ({
        url: "/api/v1/super-admin/me/2fa/disable",
        method: "POST",
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          message: Shared.getString(root, ["message"]),
          supported: Boolean(root.supported),
          enabled: Boolean(root.enabled),
        };
      },
    }),
    changeSuperAdminPassword: builder.mutation<
      Shared.SuperAdminChangePasswordResponse,
      Shared.SuperAdminChangePasswordPayload
    >({
      query: (body) => ({
        url: "/api/v1/super-admin/me/change-password",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          message: Shared.getString(root, ["message"]) || "Password updated successfully.",
        };
      },
    }),
  }),
});

export const {
  useGetSuperAdminMeQuery,
  useUpdateSuperAdminMeMutation,
  useGetSuperAdminMeProfileQuery,
  useUpdateSuperAdminMeProfileMutation,
  useUploadSuperAdminProfilePictureMutation,
  useGetSuperAdmin2faStatusQuery,
  useEnableSuperAdmin2faMutation,
  useDisableSuperAdmin2faMutation,
  useChangeSuperAdminPasswordMutation,
} = superAdminProfileApi;
