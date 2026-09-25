import { baseApi } from "./baseApi";

export type ClientProfile = {
  id: number;
  clientId: string;
  fullName: string;
  email: string;
  phone?: string;
  assignedTherapistId?: number | null;
  timezone?: string | null;
  avatarUrl?: string | null;
  portalEmail?: string;
};

export type ClientLoginResponse = {
  accessToken: string;
  tokenType?: string;
  expiresIn?: number;
  client: ClientProfile;
};

export type StaffLoginRequest = {
  username: string;
  password: string;
  orgSlug?: string;
  orgId?: string;
  deviceTrustToken?: string;
  trustDevice?: boolean;
  staySignedIn?: boolean;
};

export type StaffProfile = {
  id: number;
  username: string;
  email: string;
  fullName: string;
};

export type StaffLoginResponse = {
  accessToken: string;
  user: StaffProfile;
  roles: string[];
  permissions?: string[];
  tokenType?: string;
  expiresIn?: number;
  passwordChangeRequired?: boolean;
  changePasswordToken?: string;
  message?: string;
  tenantSchema?: string;
  organisationId?: number;
  organisationSlug?: string;
  mfaRequired?: boolean;
  mfaChallengeToken?: string;
  mfaEnrollmentRequired?: boolean;
  mfaMethod?: "TOTP" | "SMS" | "EMAIL";
  mfaMaskedDestination?: string;
  mfaMethods?: MfaMethod[];
  mfaSmsAvailable?: boolean;
  mfaEmailAvailable?: boolean;
  deviceTrustToken?: string;
  mfaSkippedTrustedDevice?: boolean;
  staySignedIn?: boolean;
  statusSuccess?: boolean;
  mfaRecoveryCodes?: string[];
  enrolledMethods?: MfaMethod[];
  canAddMoreMethods?: boolean;
};

export type MfaMethod = "TOTP" | "SMS" | "EMAIL";

export type MfaVerifyLoginRequest = {
  mfaChallengeToken: string;
  code: string;
  method?: MfaMethod;
  trustDevice?: boolean;
  staySignedIn?: boolean;
};

export type MfaEnrollmentStartRequest = {
  mfaChallengeToken: string;
  method?: MfaMethod;
  phone?: string;
};

export type MfaEnrollmentStartResponse = {
  method: MfaMethod;
  secret?: string;
  provisioningUri?: string;
  maskedDestination?: string;
  codeSent?: boolean;
  smsAvailable?: boolean;
  emailAvailable?: boolean;
};

export type MfaEnrollmentConfirmResponse = {
  enabled: boolean;
  recoveryCodes: string[];
  enrolledMethods?: MfaMethod[];
  canAddMoreMethods?: boolean;
  // Required-enrollment confirm may return a full login session
  accessToken?: string;
  user?: StaffProfile;
  roles?: string[];
  permissions?: string[];
  tenantSchema?: string;
  organisationId?: number;
  organisationSlug?: string;
  deviceTrustToken?: string;
  staySignedIn?: boolean;
  mfaRecoveryCodes?: string[];
  message?: string;
};

export type ResolveTenantRequest = {
  email: string;
};

export type ResolveTenantOrganisation = {
  organisationId: number;
  name: string;
  slug: string;
  subdomain: string;
  status: string;
  roles?: string[];
  /** Login identifier for this org's auth identity (may differ from profile email). */
  username?: string;
  branding?: {
    logoUrl?: string;
    brandPrimaryColor?: string;
    brandSecondaryColor?: string;
    brandAccentColor?: string;
  };
};

export type ResolveTenantResponse = {
  email: string;
  organisations: ResolveTenantOrganisation[];
  count: number;
  orgSlug?: string;
  organisationId?: number;
  organisationName?: string;
  branding?: ResolveTenantOrganisation["branding"];
  ssoProviders?: string[];
  resolvedAt?: string;
};

export type StaffForgotPasswordRequest = {
  email: string;
  orgSlug?: string;
  orgId?: string;
};

export type StaffForgotPasswordResponse = {
  needsTenantSelection: boolean;
};

export type StaffChangePasswordRequest = {
  currentPassword?: string;
  newPassword: string;
  changePasswordToken?: string;
};

export type StaffResetPasswordRequest = {
  token: string;
  newPassword: string;
};

export type StaffChangePasswordResponse = StaffLoginResponse;

export type AuthMeResponse = {
  authId: number;
  username: string;
  email: string;
  identityType: string;
  tenantSchema: string | null;
  organisationId: number | null;
  organisationSlug: string | null;
  roles: string[];
  permissions: string[];
  authorities: string[];
  isPlatformAdmin: boolean;
  isTenantAdmin: boolean;
  isTherapist: boolean;
  isSupervisor: boolean;
  isClient: boolean;
  user: StaffProfile | null;
  client: ClientProfile | null;
};

function parseOptionalNumber(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
  return undefined;
}

function normalizeStaffProfile(payload: Record<string, unknown>): StaffProfile {
  const nestedUser =
    payload.user && typeof payload.user === "object"
      ? (payload.user as Record<string, unknown>)
      : null;

  const idValue = nestedUser?.id ?? payload.userId ?? payload.id ?? 0;
  const id =
    typeof idValue === "number"
      ? idValue
      : typeof idValue === "string"
      ? Number.parseInt(idValue, 10) || 0
      : 0;

  const username =
    (typeof nestedUser?.username === "string" && nestedUser.username) ||
    (typeof payload.username === "string" && payload.username) ||
    "";
  const email =
    (typeof nestedUser?.email === "string" && nestedUser.email) ||
    (typeof payload.email === "string" && payload.email) ||
    username;
  const fullName =
    (typeof nestedUser?.fullName === "string" && nestedUser.fullName) ||
    (typeof payload.fullName === "string" && payload.fullName) ||
    username;

  return {
    id,
    username,
    email,
    fullName,
  };
}

function normalizeStaffLoginResponse(payload: unknown): StaffLoginResponse {
  const root = payload && typeof payload === "object" ? (payload as Record<string, unknown>) : {};
  const roles = Array.isArray(root.roles)
    ? root.roles.filter((role): role is string => typeof role === "string")
    : [];
  const permissions = Array.isArray(root.permissions)
    ? root.permissions.filter((permission): permission is string => typeof permission === "string")
    : undefined;

  return {
    accessToken: typeof root.accessToken === "string" ? root.accessToken : "",
    user: normalizeStaffProfile(root),
    roles,
    permissions,
    tokenType: typeof root.tokenType === "string" ? root.tokenType : undefined,
    expiresIn: typeof root.expiresIn === "number" ? root.expiresIn : undefined,
    passwordChangeRequired:
      typeof root.passwordChangeRequired === "boolean"
        ? root.passwordChangeRequired
        : undefined,
    changePasswordToken:
      typeof root.changePasswordToken === "string" ? root.changePasswordToken : undefined,
    message: typeof root.message === "string" ? root.message : undefined,
    tenantSchema:
      typeof root.tenantSchema === "string" ? root.tenantSchema : undefined,
    organisationId: parseOptionalNumber(root.organisationId),
    organisationSlug:
      typeof root.organisationSlug === "string"
        ? root.organisationSlug
        : typeof root.orgSlug === "string"
          ? root.orgSlug
          : undefined,
    mfaRequired: root.mfaRequired === true,
    mfaChallengeToken:
      typeof root.mfaChallengeToken === "string"
        ? root.mfaChallengeToken
        : undefined,
    mfaEnrollmentRequired: root.mfaEnrollmentRequired === true,
    mfaMethod:
      root.mfaMethod === "TOTP" ||
      root.mfaMethod === "SMS" ||
      root.mfaMethod === "EMAIL"
        ? root.mfaMethod
        : undefined,
    mfaMaskedDestination:
      typeof root.mfaMaskedDestination === "string"
        ? root.mfaMaskedDestination
        : undefined,
    mfaMethods: Array.isArray(root.mfaMethods)
      ? root.mfaMethods.filter(
          (method): method is MfaMethod =>
            method === "TOTP" || method === "SMS" || method === "EMAIL",
        )
      : undefined,
    mfaSmsAvailable: root.mfaSmsAvailable === true,
    mfaEmailAvailable: root.mfaEmailAvailable === true,
    deviceTrustToken:
      typeof root.deviceTrustToken === "string" ? root.deviceTrustToken : undefined,
    mfaSkippedTrustedDevice: root.mfaSkippedTrustedDevice === true,
    staySignedIn: root.staySignedIn === true,
    statusSuccess: root.statusSuccess === true || root.status === "success",
    mfaRecoveryCodes: Array.isArray(root.mfaRecoveryCodes)
      ? root.mfaRecoveryCodes.filter((c): c is string => typeof c === "string")
      : Array.isArray(root.recoveryCodes)
        ? root.recoveryCodes.filter((c): c is string => typeof c === "string")
        : undefined,
    enrolledMethods: Array.isArray(root.enrolledMethods)
      ? root.enrolledMethods.filter(
          (method): method is MfaMethod =>
            method === "TOTP" || method === "SMS" || method === "EMAIL",
        )
      : undefined,
    canAddMoreMethods: root.canAddMoreMethods === true,
  };
}

function normalizeAuthMeResponse(payload: unknown): AuthMeResponse {
  const root = payload && typeof payload === "object" ? (payload as Record<string, unknown>) : {};
  const roles = Array.isArray(root.roles)
    ? root.roles.filter((role): role is string => typeof role === "string")
    : [];
  const permissions = Array.isArray(root.permissions)
    ? root.permissions.filter((permission): permission is string => typeof permission === "string")
    : [];
  const authorities = Array.isArray(root.authorities)
    ? root.authorities.filter((authority): authority is string => typeof authority === "string")
    : [];

  const organisationIdRaw = root.organisationId;
  const organisationId =
    typeof organisationIdRaw === "number"
      ? organisationIdRaw
      : typeof organisationIdRaw === "string"
        ? Number.parseInt(organisationIdRaw, 10) || null
        : null;

  return {
    authId: parseOptionalNumber(root.authId) ?? 0,
    username: typeof root.username === "string" ? root.username : "",
    email: typeof root.email === "string" ? root.email : "",
    identityType: typeof root.identityType === "string" ? root.identityType : "",
    tenantSchema:
      typeof root.tenantSchema === "string" ? root.tenantSchema : null,
    organisationId,
    organisationSlug:
      typeof root.organisationSlug === "string"
        ? root.organisationSlug
        : typeof root.orgSlug === "string"
          ? root.orgSlug
          : null,
    roles,
    permissions,
    authorities,
    isPlatformAdmin: root.isPlatformAdmin === true,
    isTenantAdmin: root.isTenantAdmin === true,
    isTherapist: root.isTherapist === true,
    isSupervisor: root.isSupervisor === true,
    isClient: root.isClient === true,
    user:
      root.user && typeof root.user === "object"
        ? normalizeStaffProfile(root.user as Record<string, unknown>)
        : null,
    client: null,
  };
}

export function normalizeResolveLoginContext(payload: unknown): ResolveTenantResponse {
  const root = payload && typeof payload === "object" ? (payload as Record<string, unknown>) : {};
  const orgsRaw = Array.isArray(root.organisations) ? root.organisations : [];
  const organisations = orgsRaw
    .map((org) => {
      if (!org || typeof org !== "object") return null;
      const entry = org as Record<string, unknown>;
      const organisationIdRaw = entry.organisationId;
      const organisationId =
        typeof organisationIdRaw === "number"
          ? organisationIdRaw
          : typeof organisationIdRaw === "string"
          ? Number.parseInt(organisationIdRaw, 10) || 0
          : 0;
      return {
        organisationId,
        name: typeof entry.name === "string" ? entry.name : "",
        slug: typeof entry.slug === "string" ? entry.slug : "",
        subdomain: typeof entry.subdomain === "string" ? entry.subdomain : "",
        status: typeof entry.status === "string" ? entry.status : "",
        roles: Array.isArray(entry.roles)
          ? entry.roles.filter((role): role is string => typeof role === "string")
          : undefined,
        username:
          typeof entry.username === "string" && entry.username.trim()
            ? entry.username.trim()
            : undefined,
        branding:
          entry.branding && typeof entry.branding === "object"
            ? (entry.branding as ResolveTenantOrganisation["branding"])
            : undefined,
      } as ResolveTenantOrganisation;
    })
    .filter(Boolean) as ResolveTenantOrganisation[];

  const topOrgIdRaw = root.organisationId;
  const topOrganisationId =
    typeof topOrgIdRaw === "number"
      ? topOrgIdRaw
      : typeof topOrgIdRaw === "string"
      ? Number.parseInt(topOrgIdRaw, 10) || undefined
      : undefined;

  const topOrgSlug = typeof root.orgSlug === "string" ? root.orgSlug : undefined;
  const topOrgName =
    typeof root.organisationName === "string" ? root.organisationName : undefined;

  if (organisations.length === 0 && (topOrgSlug || topOrganisationId || topOrgName)) {
    organisations.push({
      organisationId: topOrganisationId ?? 0,
      name: topOrgName ?? topOrgSlug ?? "",
      slug: topOrgSlug ?? "",
      subdomain: "",
      status: "ACTIVE",
      branding:
        root.branding && typeof root.branding === "object"
          ? (root.branding as ResolveTenantOrganisation["branding"])
          : undefined,
    });
  }

  return {
    email: typeof root.email === "string" ? root.email : "",
    organisations,
    count:
      typeof root.count === "number"
        ? root.count
        : organisations.length,
    orgSlug: topOrgSlug,
    organisationId: topOrganisationId,
    organisationName: topOrgName,
    branding:
      root.branding && typeof root.branding === "object"
        ? (root.branding as ResolveTenantOrganisation["branding"])
        : undefined,
    ssoProviders: Array.isArray(root.ssoProviders)
      ? root.ssoProviders.filter((provider): provider is string => typeof provider === "string")
      : undefined,
    resolvedAt: typeof root.resolvedAt === "string" ? root.resolvedAt : undefined,
  };
}

export const authApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    loginStaff: builder.mutation<StaffLoginResponse, StaffLoginRequest>({
      query: (body) => ({
        url: "/api/v1/auth/login",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizeStaffLoginResponse(payload),
    }),
    verifyMfaLogin: builder.mutation<StaffLoginResponse, MfaVerifyLoginRequest>({
      query: (body) => ({
        url: "/api/v1/auth/mfa/verify-login",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizeStaffLoginResponse(payload),
    }),
    startRequiredMfaEnrollment: builder.mutation<
      MfaEnrollmentStartResponse,
      MfaEnrollmentStartRequest
    >({
      query: (body) => ({
        url: "/api/v1/auth/mfa/required-enrollment",
        method: "POST",
        body,
      }),
    }),
    sendMfaLoginCode: builder.mutation<
      { sent: boolean; method?: string; maskedDestination?: string },
      { mfaChallengeToken: string; method?: MfaMethod }
    >({
      query: (body) => ({
        url: "/api/v1/auth/mfa/send-login-code",
        method: "POST",
        body,
      }),
    }),
    confirmRequiredMfaEnrollment: builder.mutation<
      StaffLoginResponse,
      {
        mfaChallengeToken: string;
        code: string;
        trustDevice?: boolean;
        staySignedIn?: boolean;
      }
    >({
      query: (body) => ({
        url: "/api/v1/auth/mfa/required-enrollment/confirm",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizeStaffLoginResponse(payload),
    }),
    getMfaStatus: builder.query<
      {
        enabled: boolean;
        enrollmentRequired: boolean;
        enforcementMode: string;
        unusedRecoveryCodes: number;
        method?: MfaMethod;
        maskedDestination?: string;
        enrolledMethods?: MfaMethod[];
        smsAvailable?: boolean;
        emailAvailable?: boolean;
      },
      void
    >({
      query: () => ({
        url: "/api/v1/auth/mfa/status",
        method: "GET",
      }),
    }),
    sendMfaSettingsCode: builder.mutation<
      { sent: boolean; method?: string; maskedDestination?: string },
      void
    >({
      query: () => ({
        url: "/api/v1/auth/mfa/send-settings-code",
        method: "POST",
      }),
    }),
    startMfaChange: builder.mutation<
      MfaEnrollmentStartResponse,
      { currentCode: string; method: MfaMethod; phone?: string }
    >({
      query: (body) => ({
        url: "/api/v1/auth/mfa/change/start",
        method: "POST",
        body,
      }),
    }),
    confirmMfaChange: builder.mutation<
      MfaEnrollmentConfirmResponse,
      { code: string }
    >({
      query: (body) => ({
        url: "/api/v1/auth/mfa/change/confirm",
        method: "POST",
        body,
      }),
    }),
    getAuthSessions: builder.query<
      {
        activeCount: number;
        sessions: Array<{
          id: number;
          deviceLabel: string;
          ipAddress?: string;
          userAgent?: string;
          issuedAt?: string;
          lastActivityAt?: string;
          expiresAt?: string;
          current: boolean;
        }>;
      },
      void
    >({
      query: () => ({
        url: "/api/v1/auth/sessions",
        method: "GET",
      }),
    }),
    revokeAuthSession: builder.mutation<void, number>({
      query: (sessionId) => ({
        url: `/api/v1/auth/sessions/${sessionId}`,
        method: "DELETE",
      }),
    }),
    revokeOtherAuthSessions: builder.mutation<void, void>({
      query: () => ({
        url: "/api/v1/auth/sessions/others",
        method: "DELETE",
      }),
    }),
    getAuthDevices: builder.query<
      {
        count: number;
        trustedCount: number;
        devices: Array<{
          id: number;
          deviceLabel: string;
          ipAddress?: string;
          userAgent?: string;
          firstSeenAt?: string;
          lastSeenAt?: string;
          trusted: boolean;
          trustExpiresAt?: string;
          current: boolean;
        }>;
      },
      void
    >({
      query: () => {
        const trustToken =
          typeof window !== "undefined"
            ? (() => {
                try {
                  for (let i = 0; i < window.localStorage.length; i += 1) {
                    const key = window.localStorage.key(i);
                    if (key?.startsWith("tf.deviceTrust.")) {
                      const value = window.localStorage.getItem(key);
                      if (value?.trim()) return value.trim();
                    }
                  }
                } catch {
                  return undefined;
                }
                return undefined;
              })()
            : undefined;
        return {
          url: "/api/v1/auth/devices",
          method: "GET",
          headers: trustToken
            ? { "X-Device-Trust-Token": trustToken }
            : undefined,
        };
      },
    }),
    revokeAuthDevice: builder.mutation<void, number>({
      query: (deviceId) => ({
        url: `/api/v1/auth/devices/${deviceId}`,
        method: "DELETE",
      }),
    }),
    revokeAllTrustedAuthDevices: builder.mutation<void, void>({
      query: () => ({
        url: "/api/v1/auth/devices",
        method: "DELETE",
      }),
    }),
    resolveTenant: builder.mutation<ResolveTenantResponse, ResolveTenantRequest>({
      query: ({ email }) => ({
        url: `/api/v1/auth/login-context?identifier=${encodeURIComponent(email)}`,
        method: "GET",
      }),
      transformResponse: (payload) => normalizeResolveLoginContext(payload),
    }),
    forgotPasswordStaff: builder.mutation<
      StaffForgotPasswordResponse,
      StaffForgotPasswordRequest
    >({
      query: (body) => ({
        url: "/api/v1/auth/forgot-password",
        method: "POST",
        body,
      }),
      transformResponse: (_payload, meta) => {
        const response = (meta as { response?: Response } | undefined)?.response;
        return {
          needsTenantSelection:
            response?.headers.get("X-Auth-Hint") === "TENANT_SELECTION_REQUIRED",
        };
      },
    }),
    changePasswordStaff: builder.mutation<
      StaffChangePasswordResponse,
      StaffChangePasswordRequest
    >({
      query: (body) => ({
        url: "/api/v1/auth/change-password",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizeStaffLoginResponse(payload),
    }),
    resetPasswordStaff: builder.mutation<void, StaffResetPasswordRequest>({
      query: (body) => ({
        url: "/api/v1/auth/reset-password",
        method: "POST",
        body,
      }),
    }),
    // The refresh token to revoke travels in its HttpOnly cookie.
    logoutStaff: builder.mutation<void, void>({
      query: () => ({
        url: "/api/v1/auth/logout",
        method: "POST",
      }),
    }),
    getAuthMe: builder.query<AuthMeResponse, void>({
      query: () => ({
        url: "/api/v1/auth/me",
        method: "GET",
      }),
      transformResponse: (payload) => normalizeAuthMeResponse(payload),
    }),
  }),
});

export const {
  useLoginStaffMutation,
  useVerifyMfaLoginMutation,
  useStartRequiredMfaEnrollmentMutation,
  useSendMfaLoginCodeMutation,
  useConfirmRequiredMfaEnrollmentMutation,
  useGetMfaStatusQuery,
  useSendMfaSettingsCodeMutation,
  useStartMfaChangeMutation,
  useConfirmMfaChangeMutation,
  useGetAuthSessionsQuery,
  useRevokeAuthSessionMutation,
  useRevokeOtherAuthSessionsMutation,
  useGetAuthDevicesQuery,
  useRevokeAuthDeviceMutation,
  useRevokeAllTrustedAuthDevicesMutation,
  useResolveTenantMutation,
  useForgotPasswordStaffMutation,
  useChangePasswordStaffMutation,
  useResetPasswordStaffMutation,
  useLogoutStaffMutation,
  useGetAuthMeQuery,
} = authApi;
