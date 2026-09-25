import { baseApi } from "../baseApi";
import * as Shared from "./shared";

export type IntegrationKey = "stripe" | "zoom" | "openai" | "sparkpost" | "ses";

export interface IntegrationConfig {
  key: IntegrationKey;
  enabled: boolean;
  clientId: string | null;
  publishableKey: string | null;
  secret: string | null;
  connectClientSecret: string | null;
  connectWebhookSecret: string | null;
  platformWebhookSecret: string | null;
  webhookUrl: string | null;
  lastConfiguredAt: string | null;
}

export interface IntegrationUpsertRequest {
  enabled: boolean;
  clientId?: string;
  publishableKey?: string;
  secret?: string;
  connectClientSecret?: string;
  connectWebhookSecret?: string;
  platformWebhookSecret?: string;
  webhookUrl?: string;
}

export interface IntegrationTestResult {
  success: boolean;
  latencyMs: number;
  error: string | null;
}

export interface PlatformApiKey {
  id: number;
  name: string;
  keyPrefix: string;
  isActive: boolean;
  expiresAt: string | null;
  scopes?: string[];
  createdAt?: string | null;
}

export interface RotateApiKeyResponse {
  id: number;
  name: string;
  keyPrefix: string;
  apiKey: string;
  expiresAt: string | null;
}

const INTEGRATION_KEYS: IntegrationKey[] = [
  "stripe",
  "zoom",
  "openai",
  "sparkpost",
  "ses",
];

function nullableString(record: Record<string, unknown>, key: string): string | null {
  if (record[key] === null || record[key] === undefined) return null;
  const value = Shared.getString(record, [key]);
  return value || null;
}

function asIntegrationKey(value: unknown): IntegrationKey | null {
  if (typeof value !== "string") return null;
  return INTEGRATION_KEYS.includes(value as IntegrationKey) ? (value as IntegrationKey) : null;
}

function normalizeIntegrationConfig(
  payload: unknown,
  fallbackKey?: IntegrationKey,
): IntegrationConfig {
  const root = Shared.extractRoot(payload);
  const key = asIntegrationKey(root.key) ?? fallbackKey ?? "stripe";

  return {
    key,
    enabled: Boolean(root.enabled),
    clientId: nullableString(root, "clientId"),
    publishableKey: nullableString(root, "publishableKey"),
    secret: nullableString(root, "secret"),
    connectClientSecret: nullableString(root, "connectClientSecret"),
    connectWebhookSecret: nullableString(root, "connectWebhookSecret"),
    platformWebhookSecret: nullableString(root, "platformWebhookSecret"),
    webhookUrl: nullableString(root, "webhookUrl"),
    lastConfiguredAt: nullableString(root, "lastConfiguredAt"),
  };
}

function normalizePlatformApiKey(entry: unknown): PlatformApiKey | null {
  if (!entry || typeof entry !== "object") return null;
  const root = entry as Record<string, unknown>;
  const id = Shared.getNumber(root, ["id"]);
  if (!Number.isFinite(id) || id <= 0) return null;

  return {
    id,
    name: Shared.getString(root, ["name"]) || "Unnamed key",
    keyPrefix: Shared.getString(root, ["keyPrefix"]) || "—",
    isActive: root.isActive === undefined ? true : Boolean(root.isActive),
    expiresAt: nullableString(root, "expiresAt"),
    scopes: Array.isArray(root.scopes) ? (root.scopes as string[]) : undefined,
    createdAt: nullableString(root, "createdAt"),
  };
}

function normalizePlatformApiKeys(payload: unknown): PlatformApiKey[] {
  if (Array.isArray(payload)) {
    return payload
      .map((entry) => normalizePlatformApiKey(entry))
      .filter(Boolean) as PlatformApiKey[];
  }

  const root = Shared.extractRoot(payload);
  const list = root.content ?? root.items ?? root.data ?? root.keys;
  if (!Array.isArray(list)) return [];

  return list
    .map((entry) => normalizePlatformApiKey(entry))
    .filter(Boolean) as PlatformApiKey[];
}

export function isMaskedIntegrationSecret(value: string | null | undefined): boolean {
  return value === "***";
}

export const superAdminIntegrationsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSuperAdminIntegration: builder.query<IntegrationConfig, IntegrationKey>({
      query: (integrationKey) =>
        `/api/v1/super-admin/integrations/${encodeURIComponent(integrationKey)}`,
      transformResponse: (payload, _meta, integrationKey) =>
        normalizeIntegrationConfig(payload, integrationKey),
      providesTags: (_result, _error, integrationKey) => [
        { type: "SuperAdminIntegrations", id: integrationKey },
      ],
    }),
    upsertSuperAdminIntegration: builder.mutation<
      IntegrationConfig,
      { integrationKey: IntegrationKey; body: IntegrationUpsertRequest }
    >({
      query: ({ integrationKey, body }) => ({
        url: `/api/v1/super-admin/integrations/${encodeURIComponent(integrationKey)}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload, _meta, { integrationKey }) =>
        normalizeIntegrationConfig(payload, integrationKey),
      invalidatesTags: (_result, _error, { integrationKey }) => [
        { type: "SuperAdminIntegrations", id: integrationKey },
      ],
    }),
    testSuperAdminIntegration: builder.mutation<
      IntegrationTestResult,
      IntegrationKey
    >({
      query: (integrationKey) => ({
        url: `/api/v1/super-admin/integrations/${encodeURIComponent(integrationKey)}/test`,
        method: "POST",
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          success: Boolean(root.success),
          latencyMs: Shared.getNumber(root, ["latencyMs"]),
          error: nullableString(root, "error"),
        };
      },
    }),
    getSuperAdminApiKeys: builder.query<PlatformApiKey[], void>({
      query: () => "/api/v1/super-admin/api-keys",
      transformResponse: (payload) => normalizePlatformApiKeys(payload),
      providesTags: ["SuperAdminApiKeys"],
    }),
    rotateSuperAdminApiKey: builder.mutation<RotateApiKeyResponse, number>({
      query: (keyId) => ({
        url: `/api/v1/super-admin/api-keys/${keyId}/rotate`,
        method: "PUT",
      }),
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          id: Shared.getNumber(root, ["id"]),
          name: Shared.getString(root, ["name"]) || "Rotated key",
          keyPrefix: Shared.getString(root, ["keyPrefix"]) || "—",
          apiKey: Shared.getString(root, ["apiKey"]),
          expiresAt: nullableString(root, "expiresAt"),
        };
      },
      invalidatesTags: ["SuperAdminApiKeys"],
    }),
  }),
});

export const {
  useGetSuperAdminIntegrationQuery,
  useUpsertSuperAdminIntegrationMutation,
  useTestSuperAdminIntegrationMutation,
  useGetSuperAdminApiKeysQuery,
  useRotateSuperAdminApiKeyMutation,
} = superAdminIntegrationsApi;
