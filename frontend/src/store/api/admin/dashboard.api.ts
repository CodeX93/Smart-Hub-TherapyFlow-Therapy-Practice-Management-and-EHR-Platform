import { baseApi } from "../baseApi";

export interface AdminClientStats {
  totalClients: number;
  activeClients: number;
  pendingClients: number;
  completedClients: number;
}

export interface AdminDashboardSummary {
  client: {
    active: number;
    total: number;
  };
  session: {
    scheduledToday: number;
  };
  task: {
    pending: number;
    urgent: number;
    total: number;
  };
  billing: {
    estimatedThisMonth: number;
    totalCollected: number;
    outstandingBalance: number;
  };
}

export interface AdminDashboardSession {
  id: number;
  clientId?: number;
  clientName: string;
  therapistId?: number;
  therapistName: string;
  sessionDate: string;
  duration?: number;
  sessionType?: string;
  sessionMode?: string;
  status: string;
  serviceId?: number;
  serviceName?: string;
  roomId?: number;
  roomName?: string;
  notes?: string;
  zoomEnabled?: boolean;
  zoomMeetingId?: string;
  zoomJoinUrl?: string;
  zoomPassword?: string;
  recurrenceGroupId?: string;
  billingId?: number;
  hasInvoice?: boolean;
  remainingDue?: number;
  invoicePaid?: boolean;
  hasTranscript?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminSessionsListParams {
  page?: number;
  pageSize?: number;
  startDate?: string;
  endDate?: string;
  therapistId?: number;
  clientId?: number;
  clientSearch?: string;
  status?: string;
  sessionType?: string;
  serviceId?: number;
  serviceCode?: string;
  roomId?: number;
  mySessionsOnly?: boolean;
  includeHiddenServices?: boolean;
  /** Lean calendar payload for month/week/day views */
  view?: "summary" | "calendar";
}

export interface AdminSessionsListResponse {
  items: AdminDashboardSession[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface DashboardSessionSlice {
  items: AdminDashboardSession[];
  totalCount: number;
}

export interface SessionOverviewStats {
  scope?: string;
  therapistId?: number;
  clientId?: number;
  timezone?: string;
  totalSessions: number;
  todaySessions: number;
  thisWeekSessions: number;
  thisMonthSessions: number;
  upcomingSessions: number;
  completedSessions: number;
  cancelledSessions: number;
  startDate?: string;
  endDate?: string;
}

export interface SessionOverviewStatsParams {
  therapistId?: number;
  clientId?: number;
  startDate?: string;
  endDate?: string;
  timezone?: string;
}

export interface AdminDashboardTask {
  id: number;
  title: string;
  description?: string;
  status: string;
  priority: string;
  dueDate?: string;
  clientId?: number;
  clientName: string;
  assignedToId?: number;
  assignedToName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateAdminTaskPayload {
  title: string;
  description?: string;
  priority: string;
  status: string;
  clientId: number;
  assignedToId?: number;
  dueDate?: string;
}

export interface CreateSessionPayload {
  clientId: number;
  therapistId: number;
  sessionDate: string;
  sessionMode: string;
  sessionType?: string;
  status?: string;
  duration?: number;
  serviceId?: number;
  roomId?: number;
  notes?: string;
  zoomEnabled?: boolean;
  timezone?: string;
  ignoreConflicts?: boolean;
}

export interface UpdateSessionPayload {
  clientId?: number;
  therapistId?: number;
  sessionDate?: string;
  sessionMode?: string;
  sessionType?: string;
  status?: string;
  duration?: number;
  serviceId?: number;
  roomId?: number;
  notes?: string;
  zoomEnabled?: boolean;
  timezone?: string;
  ignoreConflicts?: boolean;
}

export interface UpdateSessionStatusPayload {
  status: string;
}

export interface BulkUploadSessionsPayload {
  sessions: Record<string, unknown>[];
}

export interface BulkUploadSessionsResponse {
  total: number;
  successful: number;
  failed: number;
  errors?: Record<string, unknown>[];
}

export interface AdminBillingStatistics {
  totalInvoices: number;
  paidInvoices: number;
  pendingInvoices: number;
  totalBilledAmount: number;
  totalPaidAmount: number;
  outstandingAmount: number;
  monthlyRevenue: number;
  currency: string;
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
    const parsed = Number.parseFloat(value);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

function asOptionalNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === "") return undefined;
  const parsed = asNumber(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

function pickNumber(record: Record<string, unknown>, keys: string[]): number {
  for (const key of keys) {
    const value = record[key];
    if (value === undefined || value === null || value === "") continue;
    return asNumber(value);
  }
  return 0;
}

function getArrayPayload(payload: unknown): unknown[] {
  if (Array.isArray(payload)) return payload;
  if (!isRecord(payload)) return [];
  if (Array.isArray(payload.data)) return payload.data;
  if (isRecord(payload.data)) {
    const data = payload.data;
    const candidates = [data.items, data.content, data.rows, data.results];
    for (const candidate of candidates) {
      if (Array.isArray(candidate)) return candidate;
    }
  }
  const candidates = [payload.items, payload.content, payload.rows, payload.results];
  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate;
  }
  return [];
}

function normalizeClientStats(payload: unknown): AdminClientStats {
  const root = isRecord(payload) ? (isRecord(payload.data) ? payload.data : payload) : {};
  return {
    totalClients: pickNumber(root, ["totalClients", "total", "count"]),
    activeClients: pickNumber(root, ["activeClients", "active", "activeCount"]),
    pendingClients: pickNumber(root, ["pendingClients", "pending", "pendingCount"]),
    completedClients: pickNumber(root, ["completedClients", "completed", "completedCount"]),
  };
}

function normalizeDashboardSummary(payload: unknown): AdminDashboardSummary {
  const root = isRecord(payload) ? (isRecord(payload.data) ? payload.data : payload) : {};
  const client = isRecord(root.client) ? root.client : {};
  const session = isRecord(root.session) ? root.session : {};
  const task = isRecord(root.task) ? root.task : {};
  const billing = isRecord(root.billing) ? root.billing : {};

  return {
    client: {
      active: pickNumber(client, ["active"]),
      total: pickNumber(client, ["total"]),
    },
    session: {
      scheduledToday: pickNumber(session, ["scheduledToday", "today", "count"]),
    },
    task: {
      pending: pickNumber(task, ["pending", "pendingCount"]),
      urgent: pickNumber(task, ["urgent", "urgentTasks", "urgentCount"]),
      total: pickNumber(task, ["total", "count"]),
    },
    billing: {
      estimatedThisMonth: pickNumber(billing, [
        "estimatedThisMonth",
        "monthlyRevenue",
        "currentMonthRevenue",
      ]),
      totalCollected: pickNumber(billing, ["totalCollected", "totalPaidAmount", "paidAmount"]),
      outstandingBalance: pickNumber(billing, [
        "outstandingBalance",
        "outstandingAmount",
        "pendingAmount",
      ]),
    },
  };
}

export function normalizeSession(entry: unknown): AdminDashboardSession | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    clientId: asOptionalNumber(entry.clientId),
    clientName: asString(entry.clientName),
    therapistId: asOptionalNumber(entry.therapistId),
    therapistName: asString(entry.therapistName),
    sessionDate: asString(entry.sessionDate),
    duration: asOptionalNumber(entry.duration),
    sessionType: asString(entry.sessionType) || undefined,
    sessionMode: asString(entry.sessionMode) || undefined,
    status: asString(entry.status),
    serviceId: asOptionalNumber(entry.serviceId),
    serviceName: asString(entry.serviceName) || undefined,
    roomId: asOptionalNumber(entry.roomId),
    roomName: asString(entry.roomName) || undefined,
    notes: asString(entry.notes) || undefined,
    zoomEnabled: typeof entry.zoomEnabled === "boolean" ? entry.zoomEnabled : undefined,
    zoomMeetingId: asString(entry.zoomMeetingId) || undefined,
    zoomJoinUrl: asString(entry.zoomJoinUrl) || undefined,
    zoomPassword: asString(entry.zoomPassword) || undefined,
    recurrenceGroupId: asString(entry.recurrenceGroupId) || undefined,
    billingId:
      asOptionalNumber(entry.billingId) ??
      asOptionalNumber(entry.billingRecordId) ??
      (isRecord(entry.billing) ? asOptionalNumber(entry.billing.id) : undefined),
    hasInvoice: typeof entry.hasInvoice === "boolean" ? entry.hasInvoice : undefined,
    remainingDue: asOptionalNumber(entry.remainingDue),
    invoicePaid: typeof entry.invoicePaid === "boolean" ? entry.invoicePaid : undefined,
    hasTranscript: typeof entry.hasTranscript === "boolean" ? entry.hasTranscript : undefined,
    createdAt: asString(entry.createdAt) || undefined,
    updatedAt: asString(entry.updatedAt) || undefined,
  };
}

function normalizeSessionsListResponse(payload: unknown): AdminSessionsListResponse {
  const root = isRecord(payload) ? payload : {};
  const rawItems = Array.isArray(root.items) ? root.items : getArrayPayload(payload);
  const items = rawItems
    .map((entry) => normalizeSession(entry))
    .filter(Boolean) as AdminDashboardSession[];

  return {
    items,
    totalCount: pickNumber(root, ["totalCount", "count", "total"]),
    page: Math.max(1, pickNumber(root, ["page"]) || 1),
    pageSize: Math.max(1, pickNumber(root, ["pageSize"]) || items.length || 25),
    totalPages: Math.max(0, pickNumber(root, ["totalPages"])),
  };
}

function normalizeDashboardSessionSlice(payload: unknown): DashboardSessionSlice {
  // New shape: { items, totalCount }. Legacy: bare array.
  if (Array.isArray(payload)) {
    const items = payload
      .map((entry) => normalizeSession(entry))
      .filter(Boolean) as AdminDashboardSession[];
    return { items, totalCount: items.length };
  }
  const root = isRecord(payload) ? payload : {};
  const rawItems = Array.isArray(root.items) ? root.items : getArrayPayload(payload);
  const items = rawItems
    .map((entry) => normalizeSession(entry))
    .filter(Boolean) as AdminDashboardSession[];
  const totalCount = pickNumber(root, ["totalCount", "count", "total"]);
  return {
    items,
    totalCount: totalCount > 0 ? totalCount : items.length,
  };
}

function normalizeSessionOverviewStats(payload: unknown): SessionOverviewStats {
  const root = isRecord(payload) ? (isRecord(payload.data) ? payload.data : payload) : {};
  return {
    scope: asString(root.scope) || undefined,
    therapistId: asOptionalNumber(root.therapistId),
    clientId: asOptionalNumber(root.clientId),
    timezone: asString(root.timezone) || undefined,
    totalSessions: pickNumber(root, ["totalSessions"]),
    todaySessions: pickNumber(root, ["todaySessions"]),
    thisWeekSessions: pickNumber(root, ["thisWeekSessions"]),
    thisMonthSessions: pickNumber(root, ["thisMonthSessions"]),
    upcomingSessions: pickNumber(root, ["upcomingSessions"]),
    completedSessions: pickNumber(root, ["completedSessions"]),
    cancelledSessions: pickNumber(root, ["cancelledSessions"]),
    startDate: asString(root.startDate) || undefined,
    endDate: asString(root.endDate) || undefined,
  };
}

function normalizeTask(entry: unknown): AdminDashboardTask | null {
  if (!isRecord(entry)) return null;
  return {
    id: asNumber(entry.id),
    title: asString(entry.title),
    description: asString(entry.description) || undefined,
    status: asString(entry.status),
    priority: asString(entry.priority),
    dueDate: asString(entry.dueDate) || undefined,
    clientId: asOptionalNumber(entry.clientId),
    clientName: asString(entry.clientName),
    assignedToId: asOptionalNumber(entry.assignedToId),
    assignedToName: asString(entry.assignedToName) || undefined,
    createdAt: asString(entry.createdAt) || undefined,
    updatedAt: asString(entry.updatedAt) || undefined,
  };
}

function normalizePendingCount(payload: unknown): number {
  if (typeof payload === "number") return payload;
  if (!isRecord(payload)) return 0;
  const root = isRecord(payload.data) ? payload.data : payload;
  return pickNumber(root, [
    "pendingCount",
    "count",
    "totalPending",
    "pendingTasks",
    "total",
    "value",
  ]);
}

function normalizeBillingStatistics(payload: unknown): AdminBillingStatistics {
  const root = isRecord(payload) ? (isRecord(payload.data) ? payload.data : payload) : {};

  return {
    totalInvoices: pickNumber(root, ["totalInvoices", "invoiceCount", "total"]),
    paidInvoices: pickNumber(root, ["paidInvoices", "paidCount"]),
    pendingInvoices: pickNumber(root, ["pendingInvoices", "pendingCount"]),
    totalBilledAmount: pickNumber(root, ["totalBilledAmount", "totalBilled", "billedAmount"]),
    totalPaidAmount: pickNumber(root, ["totalPaidAmount", "totalPaid", "paidAmount"]),
    outstandingAmount: pickNumber(root, [
      "outstandingAmount",
      "outstandingBalance",
      "totalOutstanding",
      "pendingAmount",
    ]),
    monthlyRevenue: pickNumber(root, [
      "monthlyRevenue",
      "estimatedThisMonth",
      "currentMonthRevenue",
      "thisMonthRevenue",
      "revenue",
    ]),
    currency: asString(root.currency) || "USD",
  };
}

export const adminDashboardApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminDashboardSummary: builder.query<AdminDashboardSummary, void>({
      query: () => "/api/v1/admin/dashboard/summary",
      transformResponse: (payload) => normalizeDashboardSummary(payload),
    }),
    getTherapistDashboardSummary: builder.query<AdminDashboardSummary, void>({
      query: () => "/api/v1/therapists/dashboard/summary",
      transformResponse: (payload) => normalizeDashboardSummary(payload),
    }),
    getAdminClientStats: builder.query<AdminClientStats, void>({
      query: () => "/api/v1/clients/stats",
      transformResponse: (payload) => normalizeClientStats(payload),
    }),
    getAdminSessionsUpcoming: builder.query<
      DashboardSessionSlice,
      { limit?: number; startDate?: string; endDate?: string } | void
    >({
      query: (arg) => {
        const query = new URLSearchParams();
        query.set("limit", String(arg?.limit ?? 5));
        if (arg?.startDate) query.set("startDate", arg.startDate);
        if (arg?.endDate) query.set("endDate", arg.endDate);
        return `/api/v1/sessions/upcoming?${query.toString()}`;
      },
      transformResponse: (payload) => normalizeDashboardSessionSlice(payload),
      providesTags: [{ type: "Sessions", id: "LIST" }],
    }),
    getAdminSessionsRecent: builder.query<AdminDashboardSession[], { limit?: number } | void>({
      query: (arg) => {
        const query = new URLSearchParams();
        if (typeof arg?.limit === "number" && Number.isFinite(arg.limit)) {
          query.set("limit", String(arg.limit));
        }
        const qs = query.toString();
        return `/api/v1/sessions/recent${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) => getArrayPayload(payload).map(normalizeSession).filter(Boolean) as AdminDashboardSession[],
      providesTags: [{ type: "Sessions", id: "LIST" }],
    }),
    getAdminSessionsPrevious: builder.query<
      DashboardSessionSlice,
      { limit?: number; startDate?: string; endDate?: string } | void
    >({
      query: (arg) => {
        const query = new URLSearchParams();
        query.set("limit", String(arg?.limit ?? 5));
        if (arg?.startDate) query.set("startDate", arg.startDate);
        if (arg?.endDate) query.set("endDate", arg.endDate);
        return `/api/v1/sessions/previous?${query.toString()}`;
      },
      transformResponse: (payload) => normalizeDashboardSessionSlice(payload),
      providesTags: [{ type: "Sessions", id: "LIST" }],
    }),
    getAdminSessionsOverdue: builder.query<
      DashboardSessionSlice,
      { limit?: number; startDate?: string; endDate?: string } | void
    >({
      query: (arg) => {
        const query = new URLSearchParams();
        query.set("limit", String(arg?.limit ?? 5));
        if (arg?.startDate) query.set("startDate", arg.startDate);
        if (arg?.endDate) query.set("endDate", arg.endDate);
        return `/api/v1/sessions/overdue?${query.toString()}`;
      },
      transformResponse: (payload) => normalizeDashboardSessionSlice(payload),
      providesTags: [{ type: "Sessions", id: "LIST" }],
    }),
    getAdminSessionsList: builder.query<AdminSessionsListResponse, AdminSessionsListParams | void>({
      query: (arg) => {
        const params = new URLSearchParams();
        params.set("page", String(arg?.page ?? 1));
        params.set("pageSize", String(arg?.pageSize ?? 25));
        if (arg?.startDate) params.set("startDate", arg.startDate);
        if (arg?.endDate) params.set("endDate", arg.endDate);
        if (typeof arg?.therapistId === "number") params.set("therapistId", String(arg.therapistId));
        if (typeof arg?.clientId === "number") params.set("clientId", String(arg.clientId));
        if (arg?.clientSearch?.trim()) params.set("clientSearch", arg.clientSearch.trim());
        if (arg?.status?.trim()) params.set("status", arg.status.trim());
        if (arg?.sessionType?.trim()) params.set("sessionType", arg.sessionType.trim());
        if (typeof arg?.serviceId === "number") params.set("serviceId", String(arg.serviceId));
        if (arg?.serviceCode?.trim()) params.set("serviceCode", arg.serviceCode.trim());
        if (typeof arg?.roomId === "number") params.set("roomId", String(arg.roomId));
        if (typeof arg?.mySessionsOnly === "boolean") {
          params.set("mySessionsOnly", String(arg.mySessionsOnly));
        }
        if (typeof arg?.includeHiddenServices === "boolean") {
          params.set("includeHiddenServices", String(arg.includeHiddenServices));
        }
        if (arg?.view) params.set("view", arg.view);
        return `/api/v1/sessions?${params.toString()}`;
      },
      transformResponse: (payload) => normalizeSessionsListResponse(payload),
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((session) => ({
                type: "Sessions" as const,
                id: session.id,
              })),
              { type: "Sessions", id: "LIST" },
            ]
          : [{ type: "Sessions", id: "LIST" }],
    }),
    getSessionOverviewStats: builder.query<SessionOverviewStats, SessionOverviewStatsParams | void>({
      query: (arg) => {
        const params = new URLSearchParams();
        if (typeof arg?.therapistId === "number") params.set("therapistId", String(arg.therapistId));
        if (typeof arg?.clientId === "number") params.set("clientId", String(arg.clientId));
        if (arg?.startDate) params.set("startDate", arg.startDate);
        if (arg?.endDate) params.set("endDate", arg.endDate);
        if (arg?.timezone?.trim()) params.set("timezone", arg.timezone.trim());
        const qs = params.toString();
        return `/api/v1/sessions/stats/overview${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) => normalizeSessionOverviewStats(payload),
      providesTags: [{ type: "Sessions", id: "STATS" }],
    }),
    getAdminPendingTasksCount: builder.query<number, void>({
      query: () => "/api/v1/tasks/pending/count",
      transformResponse: (payload) => normalizePendingCount(payload),
      providesTags: [{ type: "Tasks", id: "PENDING_COUNT" }],
    }),
    getAdminTasksRecent: builder.query<AdminDashboardTask[], { limit?: number } | void>({
      query: (arg) => {
        const query = new URLSearchParams();
        if (typeof arg?.limit === "number" && Number.isFinite(arg.limit)) {
          query.set("limit", String(arg.limit));
        }
        const qs = query.toString();
        return `/api/v1/tasks/recent${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) => getArrayPayload(payload).map(normalizeTask).filter(Boolean) as AdminDashboardTask[],
      providesTags: [{ type: "Tasks", id: "LIST" }],
    }),
    getAdminTasksUpcoming: builder.query<AdminDashboardTask[], { limit?: number } | void>({
      query: (arg) => {
        const query = new URLSearchParams();
        if (typeof arg?.limit === "number" && Number.isFinite(arg.limit)) {
          query.set("limit", String(arg.limit));
        }
        const qs = query.toString();
        return `/api/v1/tasks/upcoming${qs ? `?${qs}` : ""}`;
      },
      transformResponse: (payload) => getArrayPayload(payload).map(normalizeTask).filter(Boolean) as AdminDashboardTask[],
      providesTags: [{ type: "Tasks", id: "LIST" }],
    }),
    createAdminTask: builder.mutation<AdminDashboardTask, CreateAdminTaskPayload>({
      query: (body) => ({
        url: "/api/v1/tasks",
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizeTask(payload) ?? {
          id: 0,
          title: "",
          status: "",
          priority: "",
          clientName: "",
        },
      invalidatesTags: [
        { type: "Tasks", id: "LIST" },
        { type: "Tasks", id: "HISTORY" },
        { type: "Tasks", id: "PENDING_COUNT" },
      ],
    }),
    createAdminSession: builder.mutation<AdminDashboardSession, CreateSessionPayload>({
      query: (body) => ({
        url: "/api/v1/sessions",
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizeSession(payload) ?? {
          id: 0,
          clientName: "",
          therapistName: "",
          sessionDate: "",
          status: "",
        },
      invalidatesTags: [
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: "STATS" },
      ],
    }),
    getSessionById: builder.query<AdminDashboardSession, number>({
      query: (id) => `/api/v1/sessions/${id}`,
      transformResponse: (payload) =>
        normalizeSession(payload) ?? {
          id: 0,
          clientName: "",
          therapistName: "",
          sessionDate: "",
          status: "",
        },
      providesTags: (_result, _error, id) => [{ type: "Sessions", id }],
    }),
    updateSession: builder.mutation<
      AdminDashboardSession,
      { id: number; body: UpdateSessionPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/sessions/${id}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) =>
        normalizeSession(payload) ?? {
          id: 0,
          clientName: "",
          therapistName: "",
          sessionDate: "",
          status: "",
        },
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Sessions", id },
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: "STATS" },
      ],
    }),
    deleteSession: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/sessions/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "Sessions", id },
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: "STATS" },
      ],
    }),
    updateSessionStatus: builder.mutation<
      AdminDashboardSession,
      { id: number; body: UpdateSessionStatusPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/sessions/${id}/status`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) =>
        normalizeSession(payload) ?? {
          id: 0,
          clientName: "",
          therapistName: "",
          sessionDate: "",
          status: "",
        },
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Sessions", id },
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: "STATS" },
      ],
    }),
    downloadSessionBulkUploadTemplate: builder.query<Blob, void>({
      query: () => ({
        url: "/api/v1/sessions/bulk-upload/template",
        responseHandler: async (response) => response.blob(),
      }),
    }),
    bulkUploadSessions: builder.mutation<
      BulkUploadSessionsResponse,
      BulkUploadSessionsPayload
    >({
      query: (body) => ({
        url: "/api/v1/sessions/bulk-upload",
        method: "POST",
        body,
      }),
      transformResponse: (payload: unknown) => {
        const root = isRecord(payload) ? payload : {};
        const errors = Array.isArray(root.errors)
          ? (root.errors as Record<string, unknown>[])
          : [];
        return {
          total: asNumber(root.total),
          successful: asNumber(root.successful),
          failed: asNumber(root.failed),
          errors,
        };
      },
      invalidatesTags: [
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: "STATS" },
      ],
    }),
    getAdminBillingStatistics: builder.query<AdminBillingStatistics, void>({
      query: () => "/api/v1/billing/statistics",
      transformResponse: (payload) => normalizeBillingStatistics(payload),
    }),
  }),
});

export const {
  useGetAdminDashboardSummaryQuery,
  useGetTherapistDashboardSummaryQuery,
  useGetAdminClientStatsQuery,
  useGetAdminSessionsUpcomingQuery,
  useGetAdminSessionsRecentQuery,
  useGetAdminSessionsPreviousQuery,
  useGetAdminSessionsOverdueQuery,
  useGetAdminSessionsListQuery,
  useLazyGetAdminSessionsListQuery,
  useGetSessionOverviewStatsQuery,
  useGetAdminPendingTasksCountQuery,
  useGetAdminTasksRecentQuery,
  useGetAdminTasksUpcomingQuery,
  useCreateAdminTaskMutation,
  useCreateAdminSessionMutation,
  useGetSessionByIdQuery,
  useUpdateSessionMutation,
  useDeleteSessionMutation,
  useUpdateSessionStatusMutation,
  useLazyDownloadSessionBulkUploadTemplateQuery,
  useBulkUploadSessionsMutation,
  useGetAdminBillingStatisticsQuery,
} = adminDashboardApi;
