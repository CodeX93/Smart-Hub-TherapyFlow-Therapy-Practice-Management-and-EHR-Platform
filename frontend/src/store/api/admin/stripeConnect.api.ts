import { baseApi } from "../baseApi";

export type StripeOnboardingStatus =
  | "NOT_CONNECTED"
  | "PENDING"
  | "CONNECTED"
  | "RESTRICTED";

export interface OrgStripeConnectStatusResponse {
  organisationId: number;
  connectAccountId?: string | null;
  onboardingStatus: StripeOnboardingStatus;
  chargesEnabled: boolean;
  payoutsEnabled: boolean;
  detailsSubmitted: boolean;
  country?: string | null;
  defaultCurrency?: string | null;
  lastSyncedAt?: string | null;
  disabledReason?: string | null;
  pastDueRequirements?: string[];
  currentlyDueRequirements?: string[];
}

export interface StripeConnectOAuthStartResponse {
  authorizeUrl: string;
  stateExpiresAt?: string;
}

export interface StripeConnectConfigResponse {
  organisationId: number;
  publishableKey?: string | null;
  secretKeyConfigured: boolean;
  webhookEndpointUrl?: string | null;
  webhookSecretConfigured: boolean;
  lastUpdatedAt?: string | null;
}

export interface StripeConnectConfigRequest {
  publishableKey?: string;
  secretKey?: string;
  webhookEndpointUrl?: string;
  webhookSecret?: string;
}

export const stripeConnectApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getStripeConnectStatus: builder.query<OrgStripeConnectStatusResponse, void>({
      query: () => ({
        url: "/api/v1/admin/stripe-connect/status",
        method: "GET",
      }),
      providesTags: ["StripeConnect"],
    }),
    startStripeConnectOAuth: builder.mutation<StripeConnectOAuthStartResponse, void>({
      query: () => ({
        url: "/api/v1/admin/stripe-connect/oauth/start",
        method: "POST",
      }),
    }),
    refreshStripeConnect: builder.mutation<OrgStripeConnectStatusResponse, void>({
      query: () => ({
        url: "/api/v1/admin/stripe-connect/refresh",
        method: "POST",
      }),
      invalidatesTags: ["StripeConnect"],
    }),
    disconnectStripeConnect: builder.mutation<OrgStripeConnectStatusResponse, void>({
      query: () => ({
        url: "/api/v1/admin/stripe-connect/disconnect",
        method: "POST",
      }),
      invalidatesTags: ["StripeConnect"],
    }),
    getStripeConnectConfig: builder.query<StripeConnectConfigResponse, void>({
      query: () => ({
        url: "/api/v1/admin/stripe-connect/config",
        method: "GET",
      }),
      providesTags: ["StripeConnect"],
    }),
    updateStripeConnectConfig: builder.mutation<
      StripeConnectConfigResponse,
      StripeConnectConfigRequest
    >({
      query: (body) => ({
        url: "/api/v1/admin/stripe-connect/config",
        method: "PUT",
        body,
      }),
      invalidatesTags: ["StripeConnect"],
    }),
  }),
});

export const {
  useGetStripeConnectStatusQuery,
  useLazyGetStripeConnectStatusQuery,
  useStartStripeConnectOAuthMutation,
  useRefreshStripeConnectMutation,
  useDisconnectStripeConnectMutation,
  useGetStripeConnectConfigQuery,
  useUpdateStripeConnectConfigMutation,
} = stripeConnectApi;
