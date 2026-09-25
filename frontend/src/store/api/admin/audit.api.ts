import { baseApi } from "../baseApi";

export type AdminAuditPeriod = "weekly" | "monthly" | "yearly";
export type AdminAuditRiskFilter = "all" | "low" | "medium" | "high" | "critical";

export interface AdminAuditFilters {
  startDate?: string;
  endDate?: string;
  riskLevel?: AdminAuditRiskFilter;
  hipaaOnly?: boolean;
  action?: string;
  username?: string;
  clientId?: number;
  resourceType?: string;
}

export interface AdminAuditDashboardParams extends AdminAuditFilters {
  period?: AdminAuditPeriod;
  page?: number;
  size?: number;
}

export interface AdminAuditLogsParams extends AdminAuditFilters {
  page?: number;
  size?: number;
}

export type AdminAuditStatsParams = Pick<
  AdminAuditFilters,
  "startDate" | "endDate" | "riskLevel" | "hipaaOnly"
>;

export interface AdminAuditExportParams extends AdminAuditFilters {
  limit?: number;
}

export interface AdminClientAuditHistoryParams {
  clientId: number;
  page?: number;
  size?: number;
}

export interface AdminAuditLogEntry {
  id: number;
  userId?: number;
  username: string;
  action: string;
  result: string;
  resourceType: string;
  resourceId: string;
  clientId?: number;
  /** Decrypted MRN — never patient name */
  clientMrn?: string;
  ipAddress: string;
  riskLevel: string;
  hipaaRelevant: boolean;
  details: string;
  timestamp: string;
}

export interface AdminAuditUserActivity {
  username: string;
  activityCount: number;
  lastActivity: string;
}

export interface AdminAuditRiskDistribution {
  totalEvents: number;
  lowRiskEvents: number;
  mediumRiskEvents: number;
  highRiskEvents: number;
  criticalRiskEvents: number;
  lowRiskPercentage: number;
  mediumRiskPercentage: number;
  highRiskPercentage: number;
  criticalRiskPercentage: number;
}

export interface AdminAuditPagination {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface AdminAuditDashboardResponse {
  startDate: string;
  endDate: string;
  period: AdminAuditPeriod;
  totalActivities: number;
  phiAccessEvents: number;
  highRiskEvents: number;
  failedAttempts: number;
  lowRiskEvents: number;
  mediumRiskEvents: number;
  criticalRiskEvents: number;
  userActivitySummary: AdminAuditUserActivity[];
  riskDistribution: AdminAuditRiskDistribution;
  logs: AdminAuditLogEntry[];
  pagination: AdminAuditPagination;
}

export interface AdminAuditLogsResponse {
  content: AdminAuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AdminAuditStatsResponse {
  totalActivities: number;
  phiAccess: number;
  highRiskEvents: number;
  failedAttempts: number;
  lowRiskEvents: number;
  mediumRiskEvents: number;
  criticalRiskEvents: number;
  userActivity: AdminAuditUserActivity[];
}

export interface AdminClientAuditHistoryEntry {
  id: number;
  username: string;
  action: string;
  clientId?: number;
  clientMrn?: string;
  riskLevel: string;
  hipaaRelevant: boolean;
  timestamp: string;
}

export interface AdminAuditGlobalHealthResponse {
  scope: string;
  globalAuditEnabled: boolean;
  dedupEnabled: boolean;
  dedupWindowSeconds: number;
  hours: number;
  topN: number;
  since: string;
  totalEvents: number;
  errorEvents: number;
  countsByResourceType: Record<string, number>;
  countsByAction: Record<string, number>;
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
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  return 0;
}

function asBoolean(value: unknown): boolean {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    return normalized === "true" || normalized === "1" || normalized === "yes";
  }
  if (typeof value === "number") return value > 0;
  return false;
}

function asOptionalNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === "") return undefined;
  const parsed = asNumber(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function extractRoot(payload: unknown): Record<string, unknown> {
  if (!isRecord(payload)) return {};
  return isRecord(payload.data) ? payload.data : payload;
}

function appendAuditFilters(params: URLSearchParams, filters?: AdminAuditFilters): void {
  if (!filters) return;
  if (filters.startDate) params.set("startDate", filters.startDate);
  if (filters.endDate) params.set("endDate", filters.endDate);
  if (filters.riskLevel) params.set("riskLevel", filters.riskLevel);
  if (typeof filters.hipaaOnly === "boolean") params.set("hipaaOnly", String(filters.hipaaOnly));
  if (filters.action) params.set("action", filters.action);
  if (filters.username) params.set("username", filters.username);
  if (typeof filters.clientId === "number") params.set("clientId", String(filters.clientId));
  if (filters.resourceType) params.set("resourceType", filters.resourceType);
}

function normalizeAuditLogEntry(entry: unknown): AdminAuditLogEntry | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    userId: asOptionalNumber(entry.userId),
    username: asString(entry.username),
    action: asString(entry.action),
    result: asString(entry.result),
    resourceType: asString(entry.resourceType),
    resourceId: asString(entry.resourceId),
    clientId: asOptionalNumber(entry.clientId),
    clientMrn: asString(entry.clientMrn) || asString(entry.clientName),
    ipAddress: asString(entry.ipAddress),
    riskLevel: asString(entry.riskLevel),
    hipaaRelevant: asBoolean(entry.hipaaRelevant),
    details: asString(entry.details),
    timestamp: asString(entry.timestamp),
  };
}

function normalizeUserActivity(entry: unknown): AdminAuditUserActivity | null {
  if (!isRecord(entry)) return null;
  return {
    username: asString(entry.username),
    activityCount: asNumber(entry.activityCount),
    lastActivity: asString(entry.lastActivity),
  };
}

function normalizeRiskDistribution(value: unknown): AdminAuditRiskDistribution {
  const record = isRecord(value) ? value : {};
  const totalEvents = asNumber(record.totalEvents);
  const lowRiskEvents = asNumber(record.lowRiskEvents);
  const mediumRiskEvents = asNumber(record.mediumRiskEvents);
  const highRiskEvents = asNumber(record.highRiskEvents);
  const criticalRiskEvents = asNumber(record.criticalRiskEvents);
  const fromCount = (count: number) =>
    totalEvents > 0 ? Number(((count / totalEvents) * 100).toFixed(2)) : 0;

  return {
    totalEvents,
    lowRiskEvents,
    mediumRiskEvents,
    highRiskEvents,
    criticalRiskEvents,
    lowRiskPercentage:
      typeof record.lowRiskPercentage === "number"
        ? record.lowRiskPercentage
        : fromCount(lowRiskEvents),
    mediumRiskPercentage:
      typeof record.mediumRiskPercentage === "number"
        ? record.mediumRiskPercentage
        : fromCount(mediumRiskEvents),
    highRiskPercentage:
      typeof record.highRiskPercentage === "number"
        ? record.highRiskPercentage
        : fromCount(highRiskEvents),
    criticalRiskPercentage:
      typeof record.criticalRiskPercentage === "number"
        ? record.criticalRiskPercentage
        : fromCount(criticalRiskEvents),
  };
}

function normalizePagination(
  value: unknown,
  fallbackPage: number,
  fallbackSize: number,
): AdminAuditPagination {
  const record = isRecord(value) ? value : {};
  return {
    page: asNumber(record.page) || fallbackPage,
    size: asNumber(record.size) || fallbackSize,
    totalElements: asNumber(record.totalElements),
    totalPages: asNumber(record.totalPages),
    hasNext: asBoolean(record.hasNext),
    hasPrevious: asBoolean(record.hasPrevious),
  };
}

export const adminAuditApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminAuditDashboard: builder.query<
      AdminAuditDashboardResponse,
      AdminAuditDashboardParams | void
    >({
      query: (filters) => {
        const params = new URLSearchParams();
        params.set("period", filters?.period ?? "monthly");
        params.set("page", String(filters?.page ?? 0));
        params.set("size", String(filters?.size ?? 25));
        appendAuditFilters(params, filters ?? undefined);
        return `/api/v1/audit/dashboard?${params.toString()}`;
      },
      transformResponse: (payload, _, filters) => {
        const root = extractRoot(payload);
        const logsRaw = Array.isArray(root.logs) ? root.logs : [];
        const userActivityRaw = Array.isArray(root.userActivitySummary)
          ? root.userActivitySummary
          : [];

        return {
          startDate: asString(root.startDate),
          endDate: asString(root.endDate),
          period:
            ((asString(root.period).toLowerCase() as AdminAuditPeriod) ||
              filters?.period ||
              "weekly"),
          totalActivities: asNumber(root.totalActivities),
          phiAccessEvents: asNumber(root.phiAccessEvents),
          highRiskEvents: asNumber(root.highRiskEvents),
          failedAttempts: asNumber(root.failedAttempts),
          lowRiskEvents: asNumber(root.lowRiskEvents),
          mediumRiskEvents: asNumber(root.mediumRiskEvents),
          criticalRiskEvents: asNumber(root.criticalRiskEvents),
          userActivitySummary: userActivityRaw
            .map((entry) => normalizeUserActivity(entry))
            .filter(Boolean) as AdminAuditUserActivity[],
          riskDistribution: normalizeRiskDistribution(root.riskDistribution),
          logs: logsRaw
            .map((entry) => normalizeAuditLogEntry(entry))
            .filter(Boolean) as AdminAuditLogEntry[],
          pagination: normalizePagination(
            root.pagination,
            filters?.page ?? 0,
            filters?.size ?? 25,
          ),
        };
      },
    }),
    getAdminAuditLogs: builder.query<AdminAuditLogsResponse, AdminAuditLogsParams | void>({
      query: (filters) => {
        const params = new URLSearchParams();
        params.set("page", String(filters?.page ?? 0));
        params.set("size", String(filters?.size ?? 50));
        appendAuditFilters(params, filters ?? undefined);
        return `/api/v1/audit/logs?${params.toString()}`;
      },
      transformResponse: (payload, _, filters) => {
        const root = extractRoot(payload);
        const content = Array.isArray(root.content) ? root.content : [];
        return {
          content: content
            .map((entry) => normalizeAuditLogEntry(entry))
            .filter(Boolean) as AdminAuditLogEntry[],
          totalElements: asNumber(root.totalElements),
          totalPages: asNumber(root.totalPages),
          number: asNumber(root.number) || (filters?.page ?? 0),
          size: asNumber(root.size) || (filters?.size ?? 50),
        };
      },
    }),
    getAdminAuditStats: builder.query<AdminAuditStatsResponse, AdminAuditStatsParams | void>({
      query: (filters) => {
        const params = new URLSearchParams();
        appendAuditFilters(params, filters ?? undefined);
        return `/api/v1/audit/stats?${params.toString()}`;
      },
      transformResponse: (payload) => {
        const root = extractRoot(payload);
        const userActivity = Array.isArray(root.userActivity) ? root.userActivity : [];
        return {
          totalActivities: asNumber(root.totalActivities),
          phiAccess: asNumber(root.phiAccess),
          highRiskEvents: asNumber(root.highRiskEvents),
          failedAttempts: asNumber(root.failedAttempts),
          lowRiskEvents: asNumber(root.lowRiskEvents),
          mediumRiskEvents: asNumber(root.mediumRiskEvents),
          criticalRiskEvents: asNumber(root.criticalRiskEvents),
          userActivity: userActivity
            .map((entry) => normalizeUserActivity(entry))
            .filter(Boolean) as AdminAuditUserActivity[],
        };
      },
    }),
    getAdminAuditExport: builder.query<Blob, AdminAuditExportParams | void>({
      query: (filters) => {
        const params = new URLSearchParams();
        appendAuditFilters(params, filters ?? undefined);
        if (typeof filters?.limit === "number") params.set("limit", String(filters.limit));
        const qs = params.toString();
        return {
          url: `/api/v1/audit/export${qs ? `?${qs}` : ""}`,
          responseHandler: async (response) => response.blob(),
        };
      },
      keepUnusedDataFor: 0,
    }),
    getAdminClientAuditHistory: builder.query<
      AdminClientAuditHistoryEntry[],
      AdminClientAuditHistoryParams
    >({
      query: ({ clientId, page = 0, size = 50 }) =>
        `/api/v1/audit/clients/${clientId}?page=${page}&size=${size}`,
      transformResponse: (payload) => {
        const rows = Array.isArray(payload) ? payload : [];
        return rows
          .map((entry) => {
            if (!isRecord(entry)) return null;
            return {
              id: asNumber(entry.id),
              username: asString(entry.username),
              action: asString(entry.action),
              clientId: asOptionalNumber(entry.clientId),
              clientMrn: asString(entry.clientMrn) || asString(entry.clientName),
              riskLevel: asString(entry.riskLevel),
              hipaaRelevant: asBoolean(entry.hipaaRelevant),
              timestamp: asString(entry.timestamp),
            } as AdminClientAuditHistoryEntry;
          })
          .filter(Boolean) as AdminClientAuditHistoryEntry[];
      },
    }),
    getAdminAuditGlobalHealth: builder.query<
      AdminAuditGlobalHealthResponse,
      { hours?: number; topN?: number } | void
    >({
      query: (filters) => {
        const params = new URLSearchParams();
        if (typeof filters?.hours === "number") params.set("hours", String(filters.hours));
        if (typeof filters?.topN === "number") params.set("topN", String(filters.topN));
        const qs = params.toString();
        return `/api/v1/audit/global-health${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) => {
        const root = extractRoot(payload);
        return {
          scope: asString(root.scope),
          globalAuditEnabled: asBoolean(root.globalAuditEnabled),
          dedupEnabled: asBoolean(root.dedupEnabled),
          dedupWindowSeconds: asNumber(root.dedupWindowSeconds),
          hours: asNumber(root.hours),
          topN: asNumber(root.topN),
          since: asString(root.since),
          totalEvents: asNumber(root.totalEvents),
          errorEvents: asNumber(root.errorEvents),
          countsByResourceType: isRecord(root.countsByResourceType)
            ? Object.fromEntries(
                Object.entries(root.countsByResourceType).map(([key, value]) => [
                  key,
                  asNumber(value),
                ]),
              )
            : {},
          countsByAction: isRecord(root.countsByAction)
            ? Object.fromEntries(
                Object.entries(root.countsByAction).map(([key, value]) => [
                  key,
                  asNumber(value),
                ]),
              )
            : {},
        };
      },
    }),
  }),
});

export const {
  useGetAdminAuditDashboardQuery,
  useGetAdminAuditLogsQuery,
  useGetAdminAuditStatsQuery,
  useGetAdminAuditExportQuery,
  useLazyGetAdminAuditExportQuery,
  useGetAdminClientAuditHistoryQuery,
  useGetAdminAuditGlobalHealthQuery,
} = adminAuditApi;
