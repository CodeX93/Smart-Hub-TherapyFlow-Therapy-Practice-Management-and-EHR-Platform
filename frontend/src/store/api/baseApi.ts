import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { BaseQueryFn, FetchArgs, FetchBaseQueryError } from "@reduxjs/toolkit/query";
import { getAuthSession } from "@/utils/authStorage";
import { refreshSession, type RefreshOutcome } from "@/utils/authRefresh";
import { getRefreshPathForRole } from "@/utils/authApiPaths";
import { clearSession, updateTokens } from "@/store/authSlice";
import { resolveApiBaseForSession } from "@/utils/tenantUrls";
import { getLoginPathForRole } from "@/utils/redirectPathByRole";
import type { AppRole } from "@/utils/roleMapper";

/** Refresh access token this many ms before JWT exp to avoid noisy 401s. */
const ACCESS_TOKEN_REFRESH_SKEW_MS = 90_000;

let sessionExpiredRedirectScheduled = false;

function redirectAfterSessionInvalid(
  dispatch: (action: ReturnType<typeof clearSession>) => void,
  role: AppRole | null | undefined,
): void {
  if (sessionExpiredRedirectScheduled || typeof window === "undefined") {
    return;
  }
  sessionExpiredRedirectScheduled = true;
  dispatch(clearSession());
  window.location.assign(getLoginPathForRole(role));
}

function getJwtExpiryMs(token: string | undefined | null): number | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length < 2) return null;
  try {
    const normalized = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
    const payload = JSON.parse(atob(padded)) as { exp?: unknown };
    return typeof payload.exp === "number" ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

function shouldRefreshAccessToken(accessToken: string | undefined): boolean {
  const expMs = getJwtExpiryMs(accessToken);
  if (expMs == null) return false;
  return expMs - Date.now() <= ACCESS_TOKEN_REFRESH_SKEW_MS;
}

function requestUrl(input: string | FetchArgs): string {
  return typeof input === "string" ? input : input.url;
}

function isAuthRefreshRequest(input: string | FetchArgs, role: AppRole | null | undefined): boolean {
  if (!role) return false;
  const url = requestUrl(input);
  const refreshPath = getRefreshPathForRole(role);
  return url.includes(refreshPath);
}

const rawBaseQuery = fetchBaseQuery({
  baseUrl: "",
  credentials: "include",
  prepareHeaders: (headers) => {
    const session = getAuthSession();
    if (session?.accessToken) {
      headers.set("Authorization", `Bearer ${session.accessToken}`);
    }
    return headers;
  },
});

/** Shared refresh (see authRefresh), mirrored into the store for the route guards. */
async function refreshAccessToken(
  api: Parameters<BaseQueryFn>[1],
): Promise<RefreshOutcome> {
  const outcome = await refreshSession();
  if (outcome.status === "ok") {
    api.dispatch(updateTokens({ accessToken: outcome.session.accessToken }));
  }
  return outcome;
}

const baseQueryWithReauth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, api, extraOptions) => {
  let session = getAuthSession();
  const base = resolveApiBaseForSession(session);

  const withBase = (input: string | FetchArgs): string | FetchArgs => {
    if (typeof input === "string") {
      if (input.startsWith("http://") || input.startsWith("https://")) {
        return input;
      }
      return base + (input.startsWith("/") ? input : `/${input}`);
    }

    const url = input.url;
    if (url.startsWith("http://") || url.startsWith("https://")) {
      return input;
    }

    return {
      ...input,
      url: base + (url.startsWith("/") ? url : `/${url}`),
    };
  };

  // Proactively rotate access JWT before expiry. Idle/session logout stays
  // backend-owned: when the refresh token/session is expired, refresh fails
  // and we clear the client session.
  if (
    session?.role &&
    !isAuthRefreshRequest(args, session.role) &&
    (!session.accessToken || shouldRefreshAccessToken(session.accessToken))
  ) {
    const refreshed = await refreshAccessToken(api);
    if (refreshed.status === "ok") {
      session = refreshed.session;
    }
  }

  let result = await rawBaseQuery(withBase(args), api, extraOptions);

  if (result.error && result.error.status === 401) {
    const hadStoredSession = Boolean(session?.accessToken);

    if (!session?.role) {
      if (hadStoredSession) {
        redirectAfterSessionInvalid(api.dispatch, session?.role ?? null);
      }
      return result;
    }

    // Never try to refresh the refresh call itself.
    if (isAuthRefreshRequest(args, session.role)) {
      redirectAfterSessionInvalid(api.dispatch, session.role);
      return result;
    }

    const refreshed = await refreshAccessToken(api);
    if (refreshed.status === "ok") {
      result = await rawBaseQuery(withBase(args), api, extraOptions);
    } else if (refreshed.status === "ended") {
      redirectAfterSessionInvalid(api.dispatch, session.role);
    }
    // "unavailable": the request fails with its own 401, the session stays.
  }

  return result;
};

export const baseApi = createApi({
  reducerPath: "api",
  baseQuery: baseQueryWithReauth,
  tagTypes: [
    "NotificationHistory",
    "BillingPlans",
    "AddOnsCatalog",
    "Clients",
    "NotificationTriggers",
    "Rooms",
    "BillingServices",
    "PublicSiteServices",
    "BillingInvoices",
    "BillingStatistics",
    "BillingTransactions",
    "InvoicePolicies",
    "StripeConnect",
    "TenantSubscription",
    "RolesPermissionsMatrix",
    "ZoomCredentials",
    "SystemOptionCategories",
    "PracticeConfiguration",
    "LibraryCategories",
    "LibraryEntries",
    "LibraryTags",
    "LibraryConnections",
    "StaffNotifications",
    "StaffNotificationStats",
    "StaffNotificationTriggers",
    "StaffNotificationTemplates",
    "StaffNotificationPreferences",
    "AssessmentTemplates",
    "AssessmentAssignments",
    "AssessmentReports",
    "ReportTemplates",
    "ClientReports",
    "ReportSupportingFiles",
    "ChecklistTemplates",
    "ClientChecklists",
    "FormTemplates",
    "ClientDocuments",
    "PortalDocuments",
    "PortalNotifications",
    "PortalAppointments",
    "PortalInvoices",
    "PortalConsents",
    "PortalForms",
    "SessionNoteAiTemplate",
    "SessionNotes",
    "Sessions",
    "Tasks",
    "AdminUsers",
    "AdminRoles",
    "SuperAdminIntegrations",
    "SuperAdminApiKeys",
    "SuperAdminCmsLanding",
    "SuperAdminCmsSettings",
    "SuperAdminCmsLearningHub",
    "BookingRequests",
    "Organisations",
  ],
  endpoints: () => ({}),
});
