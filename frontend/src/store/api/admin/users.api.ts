import { baseApi } from "../baseApi";
import type { UserProfileEducationEntry } from "@/types/user-profile-education.type";
import { normalizeEducationEntries } from "@/utils/userProfileEducation";

export interface AdminUserSummary {
  id: number;
  username: string;
  fullName: string;
  email: string;
  phone?: string;
  profilePicture?: string;
  active: boolean;
  roles: string[];
  lastLogin?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserDetails extends AdminUserSummary {
  profile?: Record<string, unknown> | null;
}

export interface AdminUsersListParams {
  page?: number;
  pageSize?: number;
  search?: string;
  role?: string;
  active?: boolean;
}

export interface AdminUsersListResponse {
  items: AdminUserSummary[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface CreateAdminUserPayload {
  username: string;
  fullName: string;
  password: string;
  email: string;
  phone?: string;
  roles: string[];
  active?: boolean;
  idempotencyKey?: string;
}

export interface UpdateAdminUserPayload {
  id: number;
  body: {
    username?: string;
    fullName?: string;
    email?: string;
    phone?: string;
    active?: boolean;
    roles?: string[];
  };
}

export interface AssignAdminUserRolePayload {
  id: number;
  roles: string[];
}

export interface AdminUserProfessionalProfile {
  id: number;
  fullName?: string;
  email?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactEmail?: string;
  emergencyContactRelationship?: string;
  licenseNumber?: string;
  licenseType?: string;
  licenseState?: string;
  licenseExpiry?: string;
  licenseStatus?: string;
  specializations?: string[];
  languages?: string[];
  yearsOfExperience?: number | string;
  clinicalExperience?: string;
  researchBackground?: string;
  supervisoryExperience?: string;
  careerObjectives?: string;
  education?: UserProfileEducationEntry[];
  workingDays?: string[];
  workingHours?: string;
  consultationWorkingHours?: string;
  maxClientsPerDay?: number;
  sessionDuration?: number;
  availabilityStatus?: string;
  timezone?: string;
  virtualRoomId?: number;
  availablePhysicalRoomIds?: number[];
}

export type UpdateAdminUserProfessionalProfilePayload = Partial<AdminUserProfessionalProfile>;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

function normalizeAdminUsersResponse(payload: unknown): AdminUsersListResponse {
  const root = isRecord(payload) ? payload : {};
  const rawItems = Array.isArray(root.items) ? root.items : [];

  const items = rawItems
    .map((item) => {
      if (!isRecord(item)) return null;
      return {
        id: asNumber(item.id),
        username: asString(item.username),
        fullName: asString(item.fullName),
        email: asString(item.email),
        phone: asString(item.phone) || undefined,
        profilePicture: asString(item.profilePicture) || undefined,
        active: Boolean(item.active),
        roles: Array.isArray(item.roles)
          ? item.roles.filter((role): role is string => typeof role === "string")
          : [],
        lastLogin: asString(item.lastLogin) || null,
        createdAt: asString(item.createdAt),
        updatedAt: asString(item.updatedAt),
      } as AdminUserSummary;
    })
    .filter(Boolean) as AdminUserSummary[];

  return {
    items,
    totalCount: asNumber(root.totalCount),
    page: Math.max(1, asNumber(root.page) || 1),
    pageSize: Math.max(1, asNumber(root.pageSize) || 25),
    totalPages: Math.max(0, asNumber(root.totalPages)),
  };
}

function normalizeAdminUserDetails(payload: unknown): AdminUserDetails {
  const root = isRecord(payload) ? payload : {};
  return {
    id: asNumber(root.id),
    username: asString(root.username),
    fullName: asString(root.fullName),
    email: asString(root.email),
    phone: asString(root.phone) || undefined,
    profilePicture: asString(root.profilePicture) || undefined,
    active: Boolean(root.active),
    roles: Array.isArray(root.roles)
      ? root.roles.filter((role): role is string => typeof role === "string")
      : [],
    lastLogin: asString(root.lastLogin) || null,
    createdAt: asString(root.createdAt),
    updatedAt: asString(root.updatedAt),
    profile: isRecord(root.profile) ? root.profile : null,
  };
}

function normalizeAdminUserProfessionalProfile(
  payload: unknown,
): AdminUserProfessionalProfile {
  const root = isRecord(payload) ? payload : {};
  return {
    id: asNumber(root.id),
    fullName: asString(root.fullName) || undefined,
    email: asString(root.email) || undefined,
    emergencyContactName: asString(root.emergencyContactName) || undefined,
    emergencyContactPhone: asString(root.emergencyContactPhone) || undefined,
    emergencyContactEmail: asString(root.emergencyContactEmail) || undefined,
    emergencyContactRelationship: asString(root.emergencyContactRelationship) || undefined,
    licenseNumber: asString(root.licenseNumber) || undefined,
    licenseType: asString(root.licenseType) || undefined,
    licenseState: asString(root.licenseState) || undefined,
    licenseExpiry: asString(root.licenseExpiry) || undefined,
    licenseStatus: asString(root.licenseStatus) || undefined,
    specializations: Array.isArray(root.specializations)
      ? root.specializations.filter((value): value is string => typeof value === "string")
      : [],
    languages: Array.isArray(root.languages)
      ? root.languages.filter((value): value is string => typeof value === "string")
      : [],
    yearsOfExperience: asNumber(root.yearsOfExperience) || undefined,
    clinicalExperience: asString(root.clinicalExperience) || undefined,
    researchBackground: asString(root.researchBackground) || undefined,
    supervisoryExperience: asString(root.supervisoryExperience) || undefined,
    careerObjectives: asString(root.careerObjectives) || undefined,
    education: normalizeEducationEntries(root.education),
    workingDays: Array.isArray(root.workingDays)
      ? root.workingDays.filter((value): value is string => typeof value === "string")
      : [],
    workingHours: asString(root.workingHours) || undefined,
    consultationWorkingHours:
      asString(root.consultationWorkingHours) || undefined,
    maxClientsPerDay: asNumber(root.maxClientsPerDay) || undefined,
    sessionDuration: asNumber(root.sessionDuration) || undefined,
    availabilityStatus: asString(root.availabilityStatus) || undefined,
    timezone: asString(root.timezone) || undefined,
    virtualRoomId: asNumber(root.virtualRoomId) || undefined,
    availablePhysicalRoomIds: Array.isArray(root.availablePhysicalRoomIds)
      ? root.availablePhysicalRoomIds
          .map((value) => asNumber(value))
          .filter((value) => Number.isFinite(value) && value > 0)
      : [],
  };
}

export const adminUsersApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminUsers: builder.query<AdminUsersListResponse, AdminUsersListParams>({
      query: (params) => {
        const query = new URLSearchParams();
        const page = params.page ?? 1;
        const pageSize = params.pageSize ?? 25;
        query.set("page", String(page));
        query.set("pageSize", String(pageSize));
        if (params.search?.trim()) query.set("search", params.search.trim());
        if (params.role?.trim()) query.set("role", params.role.trim());
        if (typeof params.active === "boolean") query.set("active", String(params.active));
        return `/api/v1/admin/users?${query.toString()}`;
      },
      transformResponse: (payload) => normalizeAdminUsersResponse(payload),
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((user) => ({
                type: "AdminUsers" as const,
                id: user.id,
              })),
              { type: "AdminUsers", id: "LIST" },
            ]
          : [{ type: "AdminUsers", id: "LIST" }],
    }),
    createAdminUser: builder.mutation<AdminUserSummary, CreateAdminUserPayload>({
      query: (body) => ({
        url: "/api/v1/admin/users",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => {
        return normalizeAdminUserDetails(payload);
      },
      invalidatesTags: [{ type: "AdminUsers", id: "LIST" }],
    }),
    getAdminUserById: builder.query<AdminUserDetails, number>({
      query: (id) => `/api/v1/admin/users/${id}`,
      transformResponse: (payload) => normalizeAdminUserDetails(payload),
      providesTags: (_result, _error, id) => [{ type: "AdminUsers", id }],
    }),
    updateAdminUser: builder.mutation<AdminUserDetails, UpdateAdminUserPayload>({
      query: ({ id, body }) => ({
        url: `/api/v1/admin/users/${id}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => normalizeAdminUserDetails(payload),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "AdminUsers", id },
        { type: "AdminUsers", id: "LIST" },
      ],
    }),
    deleteAdminUser: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/admin/users/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "AdminUsers", id },
        { type: "AdminUsers", id: "LIST" },
      ],
    }),
    deactivateAdminUser: builder.mutation<AdminUserDetails, number>({
      query: (id) => ({
        url: `/api/v1/admin/users/${id}/deactivate`,
        method: "POST",
      }),
      transformResponse: (payload) => normalizeAdminUserDetails(payload),
      invalidatesTags: (_result, _error, id) => [
        { type: "AdminUsers", id },
        { type: "AdminUsers", id: "LIST" },
      ],
    }),
    activateAdminUser: builder.mutation<AdminUserDetails, number>({
      query: (id) => ({
        url: `/api/v1/admin/users/${id}/activate`,
        method: "POST",
      }),
      transformResponse: (payload) => normalizeAdminUserDetails(payload),
      invalidatesTags: (_result, _error, id) => [
        { type: "AdminUsers", id },
        { type: "AdminUsers", id: "LIST" },
      ],
    }),
    assignAdminUserRole: builder.mutation<AdminUserDetails, AssignAdminUserRolePayload>({
      query: ({ id, roles }) => ({
        url: `/api/v1/admin/users/${id}/assign-role`,
        method: "POST",
        body: { roles },
      }),
      transformResponse: (payload) => normalizeAdminUserDetails(payload),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "AdminUsers", id },
        { type: "AdminUsers", id: "LIST" },
      ],
    }),
    getAdminUserProfile: builder.query<AdminUserProfessionalProfile, number>({
      query: (userId) => `/api/v1/users/${userId}/profile`,
      transformResponse: (payload) => normalizeAdminUserProfessionalProfile(payload),
      providesTags: (_result, _error, userId) => [
        { type: "AdminUsers", id: `PROFILE_${userId}` },
      ],
    }),
    updateAdminUserProfile: builder.mutation<
      AdminUserProfessionalProfile,
      { userId: number; body: UpdateAdminUserProfessionalProfilePayload }
    >({
      query: ({ userId, body }) => ({
        url: `/api/v1/users/${userId}/profile`,
        method: "PATCH",
        body,
      }),
      transformResponse: (payload) => normalizeAdminUserProfessionalProfile(payload),
      invalidatesTags: (_result, _error, { userId }) => [
        { type: "AdminUsers", id: `PROFILE_${userId}` },
        { type: "AdminUsers", id: userId },
        { type: "AdminUsers", id: "LIST" },
      ],
    }),
    createAdminUserProfile: builder.mutation<
      AdminUserProfessionalProfile,
      { userId: number; body: UpdateAdminUserProfessionalProfilePayload }
    >({
      query: ({ userId, body }) => ({
        url: `/api/v1/users/${userId}/profile`,
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizeAdminUserProfessionalProfile(payload),
      invalidatesTags: (_result, _error, { userId }) => [
        { type: "AdminUsers", id: `PROFILE_${userId}` },
        { type: "AdminUsers", id: userId },
        { type: "AdminUsers", id: "LIST" },
      ],
    }),
  }),
});

export const {
  useLazyGetAdminUsersQuery,
  useCreateAdminUserMutation,
  useLazyGetAdminUserByIdQuery,
  useUpdateAdminUserMutation,
  useDeleteAdminUserMutation,
  useDeactivateAdminUserMutation,
  useActivateAdminUserMutation,
  useAssignAdminUserRoleMutation,
  useLazyGetAdminUserProfileQuery,
  useUpdateAdminUserProfileMutation,
  useCreateAdminUserProfileMutation,
} = adminUsersApi;

/** @deprecated Use adminUsersApi — kept for callers that import adminApi */
export const adminApi = adminUsersApi;
