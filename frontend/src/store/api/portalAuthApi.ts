import { baseApi } from "./baseApi";
import type { ClientProfile, MfaMethod } from "./authApi";
import {
  normalizeResolveLoginContext,
  type ResolveTenantRequest,
  type ResolveTenantResponse,
} from "./authApi";
import { buildPortalPublicRequest } from "@/utils/portalTenantContext";

export type PortalLoginRequest = {
  email: string;
  password: string;
  orgSlug?: string;
  orgId?: string;
  deviceTrustToken?: string;
  trustDevice?: boolean;
  staySignedIn?: boolean;
};

export type PortalAuthTokens = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
};

export type PortalLoginResponse = PortalAuthTokens & {
  client: ClientProfile;
  mfaRequired?: boolean;
  mfaChallengeToken?: string;
  mfaEnrollmentRequired?: boolean;
  mfaMethod?: MfaMethod;
  mfaMaskedDestination?: string;
  mfaMethods?: MfaMethod[];
  mfaSmsAvailable?: boolean;
  mfaEmailAvailable?: boolean;
  message?: string;
  deviceTrustToken?: string;
  mfaSkippedTrustedDevice?: boolean;
  staySignedIn?: boolean;
};

export type PortalMfaVerifyLoginRequest = {
  mfaChallengeToken: string;
  code: string;
  method?: MfaMethod;
  trustDevice?: boolean;
  staySignedIn?: boolean;
};

export type PortalActivateRequest = {
  token: string;
  password: string;
  orgSlug?: string;
  tenantSubdomain?: string;
};

export type PortalActivateResponse = PortalAuthTokens & {
  message: string;
  client: ClientProfile;
};

export type PortalForgotPasswordRequest = {
  email: string;
  orgSlug?: string;
  orgId?: string;
};

export type PortalMessageResponse = {
  message: string;
};

export type PortalResetPasswordRequest = {
  token: string;
  password: string;
};

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

function normalizePortalClient(payload: unknown): ClientProfile {
  const root = isRecord(payload) ? payload : {};

  return {
    id: asNumber(root.id),
    clientId: asString(root.clientId),
    fullName: asString(root.fullName),
    email: asString(root.email),
    phone: asString(root.phone) || undefined,
    assignedTherapistId:
      root.assignedTherapistId === null || root.assignedTherapistId === undefined
        ? null
        : asNumber(root.assignedTherapistId),
    timezone: asString(root.timezone) || null,
    avatarUrl: asString(root.avatarUrl) || null,
    portalEmail: asString(root.portalEmail) || undefined,
  };
}

function normalizePortalAuthTokens(root: Record<string, unknown>): PortalAuthTokens {
  return {
    accessToken: asString(root.accessToken),
    tokenType: asString(root.tokenType) || "Bearer",
    expiresIn: asNumber(root.expiresIn),
  };
}

function normalizePortalMfaFields(root: Record<string, unknown>): {
  mfaRequired: boolean;
  mfaChallengeToken?: string;
  mfaEnrollmentRequired: boolean;
  mfaMethod?: MfaMethod;
  mfaMaskedDestination?: string;
  mfaMethods?: MfaMethod[];
  mfaSmsAvailable: boolean;
  mfaEmailAvailable: boolean;
  message?: string;
} {
  const mfaMethod: MfaMethod | undefined =
    root.mfaMethod === "TOTP" ||
    root.mfaMethod === "SMS" ||
    root.mfaMethod === "EMAIL"
      ? root.mfaMethod
      : undefined;

  return {
    mfaRequired: root.mfaRequired === true,
    mfaChallengeToken:
      typeof root.mfaChallengeToken === "string"
        ? root.mfaChallengeToken
        : undefined,
    mfaEnrollmentRequired: root.mfaEnrollmentRequired === true,
    mfaMethod,
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
    message: typeof root.message === "string" ? root.message : undefined,
  };
}

function normalizePortalLoginResponse(payload: unknown): PortalLoginResponse {
  const root = isRecord(payload) ? payload : {};
  return {
    ...normalizePortalAuthTokens(root),
    client: normalizePortalClient(root.client),
    ...normalizePortalMfaFields(root),
    deviceTrustToken: asString(root.deviceTrustToken) || undefined,
    mfaSkippedTrustedDevice: root.mfaSkippedTrustedDevice === true,
    staySignedIn: root.staySignedIn === true,
  };
}

function normalizePortalActivateResponse(payload: unknown): PortalActivateResponse {
  const root = isRecord(payload) ? payload : {};
  return {
    ...normalizePortalAuthTokens(root),
    message: asString(root.message),
    client: normalizePortalClient(root.client),
  };
}

function normalizePortalMessageResponse(payload: unknown): PortalMessageResponse {
  const root = isRecord(payload) ? payload : {};
  return {
    message: asString(root.message),
  };
}

export const portalAuthApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    resolvePortalTenant: builder.mutation<
      ResolveTenantResponse,
      ResolveTenantRequest
    >({
      query: ({ email }) => ({
        url: `/api/v1/portal/login-context?identifier=${encodeURIComponent(email)}`,
        method: "GET",
      }),
      transformResponse: (payload) => normalizeResolveLoginContext(payload),
    }),
    portalLogin: builder.mutation<PortalLoginResponse, PortalLoginRequest>({
      query: (body) => ({
        url: "/api/v1/portal/login",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizePortalLoginResponse(payload),
    }),
    verifyPortalMfaLogin: builder.mutation<
      PortalLoginResponse,
      PortalMfaVerifyLoginRequest
    >({
      query: (body) => ({
        url: "/api/v1/portal/mfa/verify-login",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizePortalLoginResponse(payload),
    }),
    // The refresh token to revoke travels in its HttpOnly cookie.
    portalLogout: builder.mutation<PortalMessageResponse, void>({
      query: () => ({
        url: "/api/v1/portal/logout",
        method: "POST",
        body: {},
      }),
      transformResponse: (payload) => normalizePortalMessageResponse(payload),
    }),
    portalActivate: builder.mutation<PortalActivateResponse, PortalActivateRequest>({
      query: ({ token, password, orgSlug, tenantSubdomain }) => {
        const body: Record<string, string> = { token, password };
        if (orgSlug) {
          body.orgSlug = orgSlug;
        }

        return buildPortalPublicRequest("/api/v1/portal/activate", body, {
          orgSlug,
          tenantSubdomain,
        });
      },
      transformResponse: (payload) => normalizePortalActivateResponse(payload),
    }),
    portalForgotPassword: builder.mutation<
      PortalMessageResponse,
      PortalForgotPasswordRequest
    >({
      query: (body) => ({
        url: "/api/v1/portal/forgot-password",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizePortalMessageResponse(payload),
    }),
    portalResetPassword: builder.mutation<
      PortalMessageResponse,
      PortalResetPasswordRequest
    >({
      query: (body) => ({
        url: "/api/v1/portal/reset-password",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizePortalMessageResponse(payload),
    }),
  }),
});

export const {
  useResolvePortalTenantMutation,
  usePortalLoginMutation,
  useVerifyPortalMfaLoginMutation,
  usePortalLogoutMutation,
  usePortalActivateMutation,
  usePortalForgotPasswordMutation,
  usePortalResetPasswordMutation,
} = portalAuthApi;
