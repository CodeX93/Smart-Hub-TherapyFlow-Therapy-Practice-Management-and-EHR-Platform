import { baseApi } from "../baseApi";

export type AdminConsentManagementType =
  | "ALL"
  | "AI_PROCESSING"
  | "DATA_SHARING"
  | "RESEARCH"
  | "MARKETING";

export type AdminConsentManagementStatus =
  | "ALL"
  | "GRANTED"
  | "DENIED"
  | "WITHDRAWN"
  | "DENIED_WITHDRAWN";

export interface AdminConsentManagementParams {
  consentType?: AdminConsentManagementType;
  status?: AdminConsentManagementStatus;
  search?: string;
}

export interface AdminConsentHistoryQuery {
  consentType?: string;
  granted?: boolean;
}

export interface AdminConsentSummaryItem {
  id: number;
  consentType: string;
  status: string;
  version: string;
  grantedAt?: string;
  withdrawnAt?: string;
}

export interface AdminConsentManagementRow {
  clientId: string;
  fullName: string;
  email: string;
  portalAccess: boolean;
  aiProcessing: string;
  dataSharing: string;
  research: string;
  marketing: string;
  allConsents: AdminConsentSummaryItem[];
}

export interface AdminConsentRecordItem {
  id: number;
  consentType: string;
  granted: boolean;
  grantedAt?: string;
  withdrawnAt?: string;
  consentVersion?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminConsentRecordGroup {
  clientId: string;
  fullName: string;
  email: string;
  hasPortalAccess?: boolean | null;
  consents: AdminConsentRecordItem[];
}

export interface AdminClientConsentHistoryItem {
  id: number;
  clientId: number;
  consentType: string;
  consentVersion?: string;
  granted: boolean;
  grantedAt?: string;
  withdrawnAt?: string;
  ipAddress?: string;
  userAgent?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RecordAdminClientConsentRequest {
  clientId: number;
  consentType: string;
  granted: boolean;
  consentVersion: string;
  source: string;
  notes?: string;
  auditReason: string;
}

export interface RecordAdminClientVerbalAiConsentRequest {
  clientId: number;
  granted: boolean;
  notes?: string;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asStringValue(value: unknown): string {
  if (typeof value === "string") return value;
  if (typeof value === "number" && Number.isFinite(value)) return String(value);
  return "";
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  return 0;
}

function asBoolean(value: unknown): boolean {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") {
    if (value === "true") return true;
    if (value === "false") return false;
  }
  return false;
}

function asOptionalString(value: unknown): string | undefined {
  const result = asString(value).trim();
  return result || undefined;
}

function normalizeConsentState(value: unknown): string {
  const normalized = asString(value).trim().toUpperCase();
  if (normalized === "GRANTED") return "Granted";
  if (
    normalized === "DENIED" ||
    normalized === "WITHDRAWN" ||
    normalized === "DENIED_WITHDRAWN"
  ) {
    return "Denied";
  }
  return "Not Set";
}

function normalizeConsentSummaryItem(entry: unknown): AdminConsentSummaryItem | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    consentType: asString(entry.consentType),
    status: asString(entry.status),
    version: asString(entry.version),
    grantedAt: asOptionalString(entry.grantedAt),
    withdrawnAt: asOptionalString(entry.withdrawnAt),
  };
}

function normalizeConsentManagementRow(entry: unknown): AdminConsentManagementRow | null {
  if (!isRecord(entry)) return null;
  const allConsents = Array.isArray(entry.allConsents) ? entry.allConsents : [];
  return {
    clientId: asStringValue(entry.clientId),
    fullName: asString(entry.fullName),
    email: asString(entry.email),
    portalAccess: asBoolean(entry.portalAccess),
    aiProcessing: normalizeConsentState(entry.aiProcessing),
    dataSharing: normalizeConsentState(entry.dataSharing),
    research: normalizeConsentState(entry.research),
    marketing: normalizeConsentState(entry.marketing),
    allConsents: allConsents
      .map((item) => normalizeConsentSummaryItem(item))
      .filter(Boolean) as AdminConsentSummaryItem[],
  };
}

function normalizeConsentRecordItem(entry: unknown): AdminConsentRecordItem | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    consentType: asString(entry.consentType),
    granted: asBoolean(entry.granted),
    grantedAt: asOptionalString(entry.grantedAt),
    withdrawnAt: asOptionalString(entry.withdrawnAt),
    consentVersion: asOptionalString(entry.consentVersion),
    createdAt: asOptionalString(entry.createdAt),
    updatedAt: asOptionalString(entry.updatedAt),
  };
}

function normalizeConsentRecordGroup(entry: unknown): AdminConsentRecordGroup | null {
  if (!isRecord(entry)) return null;
  const consents = Array.isArray(entry.consents) ? entry.consents : [];
  const portalAccessValue = entry.hasPortalAccess;
  return {
    clientId: asStringValue(entry.clientId),
    fullName: asString(entry.fullName),
    email: asString(entry.email),
    hasPortalAccess:
      portalAccessValue === null || portalAccessValue === undefined
        ? null
        : asBoolean(portalAccessValue),
    consents: consents
      .map((item) => normalizeConsentRecordItem(item))
      .filter(Boolean) as AdminConsentRecordItem[],
  };
}

function normalizeClientConsentHistoryItem(entry: unknown): AdminClientConsentHistoryItem | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    clientId: asNumber(entry.clientId),
    consentType: asString(entry.consentType),
    consentVersion: asOptionalString(entry.consentVersion),
    granted: asBoolean(entry.granted),
    grantedAt: asOptionalString(entry.grantedAt),
    withdrawnAt: asOptionalString(entry.withdrawnAt),
    ipAddress: asOptionalString(entry.ipAddress),
    userAgent: asOptionalString(entry.userAgent),
    notes: asOptionalString(entry.notes),
    createdAt: asOptionalString(entry.createdAt),
    updatedAt: asOptionalString(entry.updatedAt),
  };
}

function buildConsentManagementQuery(params?: AdminConsentManagementParams): string {
  const query = new URLSearchParams();
  if (params?.consentType && params.consentType !== "ALL") {
    query.set("consentType", params.consentType);
  }
  if (params?.status && params.status !== "ALL") {
    query.set("status", params.status);
  }
  if (params?.search?.trim()) {
    query.set("search", params.search.trim());
  }
  const qs = query.toString();
  return qs ? `?${qs}` : "";
}

export const adminConsentsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminConsentManagement: builder.query<
      AdminConsentManagementRow[],
      AdminConsentManagementParams | void
    >({
      query: (params) =>
        `/api/v1/admin/consents/management${buildConsentManagementQuery(params ?? undefined)}`,
      transformResponse: (payload) => {
        const rows = Array.isArray(payload) ? payload : [];
        return rows
          .map((entry) => normalizeConsentManagementRow(entry))
          .filter(Boolean) as AdminConsentManagementRow[];
      },
    }),
    getAdminConsentManagementRefresh: builder.query<
      AdminConsentManagementRow[],
      AdminConsentManagementParams | void
    >({
      query: (params) =>
        `/api/v1/admin/consents/management/refresh${buildConsentManagementQuery(params ?? undefined)}`,
      transformResponse: (payload) => {
        const rows = Array.isArray(payload) ? payload : [];
        return rows
          .map((entry) => normalizeConsentManagementRow(entry))
          .filter(Boolean) as AdminConsentManagementRow[];
      },
      keepUnusedDataFor: 0,
    }),
    getAdminConsentRecords: builder.query<AdminConsentRecordGroup[], AdminConsentHistoryQuery | void>({
      query: (params) => {
        const query = new URLSearchParams();
        if (params?.consentType?.trim()) query.set("consentType", params.consentType.trim());
        if (typeof params?.granted === "boolean") query.set("granted", String(params.granted));
        const qs = query.toString();
        return `/api/v1/admin/consents${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) => {
        const rows = Array.isArray(payload) ? payload : [];
        return rows
          .map((entry) => normalizeConsentRecordGroup(entry))
          .filter(Boolean) as AdminConsentRecordGroup[];
      },
    }),
    getAdminClientConsentHistory: builder.query<AdminClientConsentHistoryItem[], number>({
      query: (clientId) => `/api/v1/admin/consents/clients/${clientId}`,
      transformResponse: (payload) => {
        const rows = Array.isArray(payload) ? payload : [];
        return rows
          .map((entry) => normalizeClientConsentHistoryItem(entry))
          .filter(Boolean) as AdminClientConsentHistoryItem[];
      },
    }),
    recordAdminClientConsent: builder.mutation<
      AdminClientConsentHistoryItem,
      RecordAdminClientConsentRequest
    >({
      query: ({ clientId, ...body }) => ({
        url: `/api/v1/admin/consents/clients/${clientId}`,
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizeClientConsentHistoryItem(payload) as AdminClientConsentHistoryItem,
    }),
    recordAdminClientVerbalAiConsent: builder.mutation<
      AdminClientConsentHistoryItem,
      RecordAdminClientVerbalAiConsentRequest
    >({
      query: ({ clientId, granted, notes }) => {
        const params = new URLSearchParams();
        params.set("granted", String(granted));
        if (notes?.trim()) params.set("notes", notes.trim());
        return {
          url: `/api/v1/admin/consents/clients/${clientId}/verbal-ai-consent?${params.toString()}`,
          method: "POST",
        };
      },
      transformResponse: (payload) =>
        normalizeClientConsentHistoryItem(payload) as AdminClientConsentHistoryItem,
    }),
  }),
});

export const {
  useGetAdminConsentManagementQuery,
  useGetAdminConsentManagementRefreshQuery,
  useLazyGetAdminConsentManagementRefreshQuery,
  useGetAdminConsentRecordsQuery,
  useGetAdminClientConsentHistoryQuery,
  useRecordAdminClientConsentMutation,
  useRecordAdminClientVerbalAiConsentMutation,
} = adminConsentsApi;
