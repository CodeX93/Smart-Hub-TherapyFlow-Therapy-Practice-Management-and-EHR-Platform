import { baseApi } from "./baseApi";
import type { UserProfileEducationEntry } from "@/types/user-profile-education.type";
import { normalizeEducationEntries } from "@/utils/userProfileEducation";

export interface UserTimezoneResponse {
  timezone: string | null;
}

export interface UserTimezonesResponse {
  timezones: string[];
}

export interface ZoomStatusResponse {
  configured: boolean;
  lastUpdatedAt?: string;
  accountId?: string;
}

export interface ZoomCredentialsPayload {
  accountId: string;
  clientId: string;
  clientSecret: string;
}

export interface CurrentUserResponse {
  id: number;
  username?: string;
  fullName?: string;
  email?: string;
  phone?: string;
  profilePicture?: string;
  active?: boolean;
  roles?: string[];
  lastLogin?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserProfileResponse {
  id: number;
  fullName?: string;
  email?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  licenseNumber?: string;
  licenseType?: string;
  licenseState?: string;
  licenseExpiry?: string | null;
  specializations?: string[];
  languages?: string[];
  yearsOfExperience?: number;
  clinicalExperience?: string;
  researchBackground?: string;
  supervisoryExperience?: string;
  careerObjectives?: string;
  education?: UserProfileEducationEntry[];
  maxClientsPerDay?: number;
  workingDays?: string[];
  workingHours?: string | null;
  consultationWorkingHours?: string | null;
  sessionDuration?: number;
  availabilityStatus?: string;
  timezone?: string | null;
  virtualRoomId?: number | null;
  availablePhysicalRoomIds?: number[];
}

export interface UpdateMyProfilePayload {
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
  yearsOfExperience?: number;
  clinicalExperience?: string;
  researchBackground?: string;
  supervisoryExperience?: string;
  careerObjectives?: string;
  education?: UserProfileEducationEntry[];
  maxClientsPerDay?: number;
  workingDays?: string[];
  workingHours?: string;
  consultationWorkingHours?: string;
  availabilityStatus?: string;
  timezone?: string;
  sessionDuration?: number;
  virtualRoomId?: number | null;
  availablePhysicalRoomIds?: number[];
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
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

function asOptionalNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === "") return undefined;
  const parsed = asNumber(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function normalizeTimezoneResponse(payload: unknown): UserTimezoneResponse {
  if (isRecord(payload)) {
    if (typeof payload.timezone === "string") {
      return { timezone: payload.timezone };
    }

    const fallback = Object.values(payload).find(
      (value): value is string => typeof value === "string" && Boolean(value.trim()),
    );

    return { timezone: fallback ?? null };
  }

  return { timezone: null };
}

function normalizeTimezonesResponse(payload: unknown): UserTimezonesResponse {
  const root = isRecord(payload) ? payload : {};
  const raw = Array.isArray(root.timezones) ? root.timezones : [];
  return {
    timezones: raw.filter((value): value is string => typeof value === "string"),
  };
}

function normalizeUserProfileResponse(payload: unknown): UserProfileResponse {
  const root = isRecord(payload) ? payload : {};
  return {
    id: asNumber(root.id),
    fullName: asString(root.fullName) || undefined,
    email: asString(root.email) || undefined,
    emergencyContactName: asString(root.emergencyContactName) || undefined,
    emergencyContactPhone: asString(root.emergencyContactPhone) || undefined,
    emergencyContactRelationship: asString(root.emergencyContactRelationship) || undefined,
    licenseNumber: asString(root.licenseNumber) || undefined,
    licenseType: asString(root.licenseType) || undefined,
    licenseState: asString(root.licenseState) || undefined,
    licenseExpiry: asString(root.licenseExpiry) || null,
    specializations: Array.isArray(root.specializations)
      ? root.specializations.filter((value): value is string => typeof value === "string")
      : undefined,
    languages: Array.isArray(root.languages)
      ? root.languages.filter((value): value is string => typeof value === "string")
      : undefined,
    yearsOfExperience: asOptionalNumber(root.yearsOfExperience),
    clinicalExperience: asString(root.clinicalExperience) || undefined,
    researchBackground: asString(root.researchBackground) || undefined,
    supervisoryExperience: asString(root.supervisoryExperience) || undefined,
    careerObjectives: asString(root.careerObjectives) || undefined,
    education: normalizeEducationEntries(root.education),
    workingDays: Array.isArray(root.workingDays)
      ? root.workingDays.filter((value): value is string => typeof value === "string")
      : undefined,
    workingHours: asString(root.workingHours) || null,
    consultationWorkingHours: asString(root.consultationWorkingHours) || null,
    maxClientsPerDay: asOptionalNumber(root.maxClientsPerDay),
    sessionDuration: asOptionalNumber(root.sessionDuration),
    availabilityStatus: asString(root.availabilityStatus) || undefined,
    timezone: asString(root.timezone) || null,
    virtualRoomId: asOptionalNumber(root.virtualRoomId) ?? null,
    availablePhysicalRoomIds: Array.isArray(root.availablePhysicalRoomIds)
      ? root.availablePhysicalRoomIds
          .map((value) => asOptionalNumber(value))
          .filter((value): value is number => typeof value === "number")
      : [],
  };
}

export const userProfileApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMyTimezones: builder.query<UserTimezonesResponse, void>({
      query: () => "/api/v1/users/timezones",
      transformResponse: (payload) => normalizeTimezonesResponse(payload),
    }),
    getMyTimezone: builder.query<UserTimezoneResponse, void>({
      query: () => "/api/v1/users/me/timezone",
      transformResponse: (payload) => normalizeTimezoneResponse(payload),
    }),
    getMyProfile: builder.query<UserProfileResponse, void>({
      query: () => "/api/v1/users/me/profile",
      transformResponse: (payload) => normalizeUserProfileResponse(payload),
    }),
    updateMyProfile: builder.mutation<UserProfileResponse, UpdateMyProfilePayload>({
      query: (body) => ({
        url: "/api/v1/users/me/profile",
        method: "PATCH",
        body,
      }),
      transformResponse: (payload) => normalizeUserProfileResponse(payload),
    }),
    getCurrentUser: builder.query<CurrentUserResponse, void>({
      query: () => "/api/v1/users/me",
      providesTags: ["ZoomCredentials"],
    }),
    getZoomStatus: builder.query<ZoomStatusResponse, void>({
      query: () => "/api/v1/users/me/zoom-credentials/status",
      providesTags: ["ZoomCredentials"],
    }),
    // Scheduling on another therapist's behalf needs their Zoom state, not the caller's.
    // Requires USER_VIEW, so callers must treat a rejection as "unknown", not "not configured".
    getUserZoomStatus: builder.query<ZoomStatusResponse, number>({
      query: (userId) => `/api/v1/users/${userId}/zoom-credentials/status`,
      providesTags: ["ZoomCredentials"],
    }),
    saveZoomCredentials: builder.mutation<ZoomStatusResponse, ZoomCredentialsPayload>({
      query: (body) => ({
        url: "/api/v1/users/me/zoom-credentials",
        method: "PUT",
        body,
      }),
      invalidatesTags: ["ZoomCredentials"],
    }),
    deleteZoomCredentials: builder.mutation<void, void>({
      query: () => ({
        url: "/api/v1/users/me/zoom-credentials",
        method: "DELETE",
      }),
      invalidatesTags: ["ZoomCredentials"],
    }),
    testZoomCredentials: builder.mutation<ZoomStatusResponse, void>({
      query: () => ({
        url: "/api/v1/users/me/zoom-credentials/test",
        method: "POST",
      }),
    }),
  }),
});

export const {
  useGetMyTimezonesQuery,
  useGetMyTimezoneQuery,
  useGetMyProfileQuery,
  useUpdateMyProfileMutation,
  useGetCurrentUserQuery,
  useGetZoomStatusQuery,
  useGetUserZoomStatusQuery,
  useSaveZoomCredentialsMutation,
  useDeleteZoomCredentialsMutation,
  useTestZoomCredentialsMutation,
} = userProfileApi;
