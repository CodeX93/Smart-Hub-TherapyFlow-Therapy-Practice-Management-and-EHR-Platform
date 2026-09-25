import { baseApi } from "../baseApi";

export interface StaffNotificationItem {
  id: number;
  userId: number;
  clientId: number | null;
  type: string;
  category: string;
  title: string;
  message: string;
  data: string | null;
  priority: string;
  isRead: boolean;
  readAt: string | null;
  actionUrl: string | null;
  actionLabel: string | null;
  relatedEntityType: string | null;
  relatedEntityId: number | null;
  expiresAt: string | null;
  createdAt: string;
}

export interface StaffNotificationRequest {
  userId?: number;
  clientId?: number;
  type: string;
  category: string;
  title: string;
  message: string;
  data?: string;
  priority?: string;
  actionUrl?: string;
  actionLabel?: string;
  relatedEntityType?: string;
  relatedEntityId?: number;
  expiresAt?: string;
}

export interface StaffNotificationBroadcastRequest {
  targetType: "USERS" | "CLIENTS" | "BOTH";
  type: string;
  category: string;
  title: string;
  message: string;
  data?: string;
  priority?: string;
  actionUrl?: string;
  actionLabel?: string;
  relatedEntityType?: string;
  relatedEntityId?: number | null;
  expiresAt?: string;
}

export interface StaffNotificationTrigger {
  id: number;
  name: string;
  description: string;
  eventType: string;
  entityType: string;
  conditionRules: string;
  recipientRules: string;
  priority: string;
  isScheduled: boolean;
  scheduleOffsetMinutes: number;
  batchWindowMinutes: number;
  maxBatchSize: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export type StaffNotificationTriggerPayload = Omit<
  StaffNotificationTrigger,
  "id" | "createdAt" | "updatedAt"
>;

export interface StaffNotificationTemplate {
  id: number;
  name: string;
  type: string;
  eventType: string;
  subject: string;
  bodyTemplate: string;
  isSystem: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export type StaffNotificationTemplatePayload = Omit<
  StaffNotificationTemplate,
  "id" | "createdAt" | "updatedAt"
>;

export interface StaffNotificationPreference {
  id: number;
  userId: number;
  clientId: number | null;
  notificationType: string;
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  inAppEnabled: boolean;
  timing: string;
  quietHoursStart: string | null;
  quietHoursEnd: string | null;
  weekendsEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StaffNotificationPreferencePayload {
  notificationType: string;
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  inAppEnabled: boolean;
  timing: string;
  quietHoursStart?: string | null;
  quietHoursEnd?: string | null;
  weekendsEnabled: boolean;
}

export interface StaffNotificationStats {
  total: number;
  unread: number;
}

export interface StaffNotificationListResponse {
  items: StaffNotificationItem[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface StaffNotificationSetupHealth {
  scope: string;
  events: Array<{
    eventType: string;
    hasAnyTrigger: boolean;
    hasActiveTrigger: boolean;
    hasInAppTemplate: boolean;
    hasActiveInAppTemplate: boolean;
    hasEmailTemplate: boolean;
    hasActiveEmailTemplate: boolean;
    triggerCount: number;
    activeTriggerCount: number;
  }>;
}

export interface StaffNotificationActionMetadata {
  scope: string;
  entities: Array<{
    relatedEntityType: string;
    actionUrlTemplate: string;
    defaultActionLabel: string;
    exampleActionUrl: string;
  }>;
}

export interface StaffNotificationEventCatalog {
  scope: string;
  events: Array<{
    eventType: string;
    defaultChannels: string[];
    required: boolean;
    sessionHealthEvent: boolean;
  }>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return 0;
}

function normalizeStaffNotificationStats(payload: unknown): StaffNotificationStats {
  if (!isRecord(payload)) {
    return { total: 0, unread: 0 };
  }

  return {
    total: asNumber(payload.total ?? payload.totalCount ?? payload.total_count),
    unread: asNumber(
      payload.unread ??
        payload.unreadCount ??
        payload.unread_count ??
        payload.unreadNotifications,
    ),
  };
}

function normalizeUnreadCount(payload: unknown): number {
  if (typeof payload === "number" && Number.isFinite(payload)) return payload;
  if (!isRecord(payload)) return 0;
  return asNumber(payload.count ?? payload.unread ?? payload.unreadCount ?? payload.unread_count);
}

function normalizeActionMetadata(payload: unknown): StaffNotificationActionMetadata {
  if (!isRecord(payload)) {
    return {
      scope: "",
      entities: [],
    };
  }
  return {
    scope: typeof payload.scope === "string" ? payload.scope : "",
    entities: Array.isArray(payload.entities)
      ? payload.entities
          .map((entry) => {
            if (!isRecord(entry)) return null;
            return {
              relatedEntityType:
                typeof entry.relatedEntityType === "string" ? entry.relatedEntityType : "",
              actionUrlTemplate:
                typeof entry.actionUrlTemplate === "string" ? entry.actionUrlTemplate : "",
              defaultActionLabel:
                typeof entry.defaultActionLabel === "string" ? entry.defaultActionLabel : "",
              exampleActionUrl: typeof entry.exampleActionUrl === "string" ? entry.exampleActionUrl : "",
            };
          })
          .filter(Boolean) as StaffNotificationActionMetadata["entities"]
      : [],
  };
}

export const staffNotificationsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getStaffNotifications: builder.query<
      StaffNotificationItem[],
      { unreadOnly?: boolean } | void
    >({
      query: (params) => {
        const unreadOnly = params && "unreadOnly" in params ? params.unreadOnly : undefined;
        return {
          url: "/api/v1/notifications",
          params: unreadOnly === undefined ? undefined : { unreadOnly },
        };
      },
      transformResponse: (payload: unknown) =>
        Array.isArray(payload) ? (payload as StaffNotificationItem[]) : [],
      providesTags: ["StaffNotifications"],
    }),
    getStaffNotificationsPaginated: builder.query<
      StaffNotificationListResponse,
      { unreadOnly?: boolean; page?: number; pageSize?: number } | void
    >({
      query: (params) => {
        const unreadOnly = params && "unreadOnly" in params ? params.unreadOnly : undefined;
        const page = Math.max(1, params?.page ?? 1);
        const pageSize = Math.max(1, params?.pageSize ?? 20);
        return {
          url: "/api/v1/notifications",
          params: {
            unreadOnly,
            page,
            pageSize,
          },
        };
      },
      transformResponse: (payload: unknown, _meta, args) => {
        const page = Math.max(1, args?.page ?? 1);
        const pageSize = Math.max(1, args?.pageSize ?? 20);

        if (Array.isArray(payload)) {
          return {
            items: payload as StaffNotificationItem[],
            totalCount: (payload as StaffNotificationItem[]).length,
            page,
            pageSize,
            totalPages: (payload as StaffNotificationItem[]).length < pageSize ? page : page + 1,
          };
        }

        const root = isRecord(payload) ? payload : {};
        const rawItems = Array.isArray(root.items)
          ? root.items
          : Array.isArray(root.data)
            ? root.data
            : Array.isArray(root.results)
              ? root.results
              : [];

        const items = rawItems as StaffNotificationItem[];
        const totalCountRaw =
          typeof root.totalCount === "number"
            ? root.totalCount
            : typeof root.total === "number"
              ? root.total
              : items.length;
        const responsePage =
          typeof root.page === "number" ? Math.max(1, root.page) : page;
        const responsePageSize =
          typeof root.pageSize === "number" ? Math.max(1, root.pageSize) : pageSize;
        const responseTotalPages =
          typeof root.totalPages === "number"
            ? Math.max(1, root.totalPages)
            : Math.max(1, Math.ceil(totalCountRaw / responsePageSize));

        return {
          items,
          totalCount: Math.max(totalCountRaw, items.length),
          page: responsePage,
          pageSize: responsePageSize,
          totalPages: responseTotalPages,
        };
      },
      providesTags: ["StaffNotifications"],
    }),

    createStaffNotification: builder.mutation<
      StaffNotificationItem,
      StaffNotificationRequest
    >({
      query: (body) => ({
        url: "/api/v1/notifications",
        method: "POST",
        body,
      }),
      invalidatesTags: ["StaffNotifications", "StaffNotificationStats"],
    }),
    broadcastStaffNotification: builder.mutation<unknown, StaffNotificationBroadcastRequest>({
      query: (body) => ({
        url: "/api/v1/notifications/broadcast",
        method: "POST",
        body,
      }),
      invalidatesTags: ["StaffNotifications", "StaffNotificationStats"],
    }),

    deleteStaffNotification: builder.mutation<unknown, number>({
      query: (id) => ({
        url: `/api/v1/notifications/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["StaffNotifications", "StaffNotificationStats"],
    }),

    readStaffNotification: builder.mutation<unknown, number>({
      query: (id) => ({
        url: `/api/v1/notifications/${id}/read`,
        method: "PATCH",
      }),
      invalidatesTags: ["StaffNotifications", "StaffNotificationStats"],
    }),

    readAllStaffNotifications: builder.mutation<unknown, void>({
      query: () => ({
        url: "/api/v1/notifications/read-all",
        method: "PATCH",
      }),
      invalidatesTags: ["StaffNotifications", "StaffNotificationStats"],
    }),

    markAllStaffNotificationsRead: builder.mutation<unknown, void>({
      query: () => ({
        url: "/api/v1/notifications/mark-all-read",
        method: "PUT",
      }),
      invalidatesTags: ["StaffNotifications", "StaffNotificationStats"],
    }),

    getStaffNotificationStats: builder.query<StaffNotificationStats, void>({
      query: () => "/api/v1/notifications/stats",
      transformResponse: (payload: unknown) => normalizeStaffNotificationStats(payload),
      providesTags: ["StaffNotificationStats"],
    }),
    getStaffNotificationUnreadCount: builder.query<number, void>({
      query: () => "/api/v1/notifications/unread/count",
      transformResponse: (payload: unknown) => normalizeUnreadCount(payload),
      providesTags: ["StaffNotificationStats"],
    }),
    getStaffNotificationUnreadCountMap: builder.query<Record<string, number>, void>({
      query: () => "/api/v1/notifications/unread-count",
    }),
    getStaffNotificationSetupHealth: builder.query<StaffNotificationSetupHealth, void>({
      query: () => "/api/v1/notifications/setup-health",
    }),
    getStaffNotificationActionMetadata: builder.query<StaffNotificationActionMetadata, void>({
      query: () => "/api/v1/notifications/setup/action-metadata",
      transformResponse: (payload: unknown) => normalizeActionMetadata(payload),
    }),
    getStaffNotificationEventCatalog: builder.query<StaffNotificationEventCatalog, void>({
      query: () => "/api/v1/notifications/setup/events",
      transformResponse: (payload: unknown) => {
        if (!isRecord(payload)) {
          return { scope: "", events: [] };
        }
        const events = Array.isArray(payload.events)
          ? payload.events
              .map((entry) => {
                if (!isRecord(entry)) return null;
                return {
                  eventType: typeof entry.eventType === "string" ? entry.eventType : "",
                  defaultChannels: Array.isArray(entry.defaultChannels)
                    ? entry.defaultChannels.filter(
                        (channel): channel is string => typeof channel === "string",
                      )
                    : [],
                  required: Boolean(entry.required),
                  sessionHealthEvent: Boolean(entry.sessionHealthEvent),
                };
              })
              .filter(Boolean) as StaffNotificationEventCatalog["events"]
          : [];
        return {
          scope: typeof payload.scope === "string" ? payload.scope : "",
          events,
        };
      },
    }),
    cleanupStaffNotifications: builder.mutation<Record<string, unknown>, void>({
      query: () => ({
        url: "/api/v1/notifications/cleanup",
        method: "POST",
      }),
      invalidatesTags: ["StaffNotifications", "StaffNotificationStats"],
    }),

    getStaffNotificationTriggers: builder.query<StaffNotificationTrigger[], void>({
      query: () => "/api/v1/notifications/triggers",
      transformResponse: (payload: unknown) =>
        Array.isArray(payload) ? (payload as StaffNotificationTrigger[]) : [],
      providesTags: ["StaffNotificationTriggers"],
    }),

    createStaffNotificationTrigger: builder.mutation<
      StaffNotificationTrigger,
      StaffNotificationTriggerPayload
    >({
      query: (body) => ({
        url: "/api/v1/notifications/triggers",
        method: "POST",
        body,
      }),
      invalidatesTags: ["StaffNotificationTriggers"],
    }),

    updateStaffNotificationTrigger: builder.mutation<
      StaffNotificationTrigger,
      { id: number; body: StaffNotificationTriggerPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/notifications/triggers/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: ["StaffNotificationTriggers"],
    }),

    deleteStaffNotificationTrigger: builder.mutation<unknown, number>({
      query: (id) => ({
        url: `/api/v1/notifications/triggers/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["StaffNotificationTriggers"],
    }),

    getStaffNotificationTemplates: builder.query<
      StaffNotificationTemplate[],
      { type?: string } | void
    >({
      query: (params) => ({
        url: "/api/v1/notifications/templates",
        params: params?.type ? { type: params.type } : undefined,
      }),
      transformResponse: (payload: unknown) =>
        Array.isArray(payload) ? (payload as StaffNotificationTemplate[]) : [],
      providesTags: ["StaffNotificationTemplates"],
    }),

    createStaffNotificationTemplate: builder.mutation<
      StaffNotificationTemplate,
      StaffNotificationTemplatePayload
    >({
      query: (body) => ({
        url: "/api/v1/notifications/templates",
        method: "POST",
        body,
      }),
      invalidatesTags: ["StaffNotificationTemplates"],
    }),

    updateStaffNotificationTemplate: builder.mutation<
      StaffNotificationTemplate,
      { id: number; body: StaffNotificationTemplatePayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/notifications/templates/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: ["StaffNotificationTemplates"],
    }),

    deleteStaffNotificationTemplate: builder.mutation<unknown, number>({
      query: (id) => ({
        url: `/api/v1/notifications/templates/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["StaffNotificationTemplates"],
    }),

    getStaffNotificationPreferences: builder.query<StaffNotificationPreference[], void>({
      query: () => "/api/v1/notifications/preferences",
      transformResponse: (payload: unknown) =>
        Array.isArray(payload) ? (payload as StaffNotificationPreference[]) : [],
      providesTags: ["StaffNotificationPreferences"],
    }),

    updateStaffNotificationPreference: builder.mutation<
      StaffNotificationPreference,
      { triggerType: string; body: StaffNotificationPreferencePayload }
    >({
      query: ({ triggerType, body }) => ({
        url: `/api/v1/notifications/preferences/${encodeURIComponent(triggerType)}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: ["StaffNotificationPreferences"],
    }),
  }),
});

export const {
  useGetStaffNotificationsQuery,
  useGetStaffNotificationsPaginatedQuery,
  useLazyGetStaffNotificationsQuery,
  useCreateStaffNotificationMutation,
  useBroadcastStaffNotificationMutation,
  useDeleteStaffNotificationMutation,
  useReadStaffNotificationMutation,
  useReadAllStaffNotificationsMutation,
  useMarkAllStaffNotificationsReadMutation,
  useGetStaffNotificationStatsQuery,
  useGetStaffNotificationUnreadCountQuery,
  useGetStaffNotificationUnreadCountMapQuery,
  useGetStaffNotificationSetupHealthQuery,
  useGetStaffNotificationActionMetadataQuery,
  useGetStaffNotificationEventCatalogQuery,
  useCleanupStaffNotificationsMutation,
  useGetStaffNotificationTriggersQuery,
  useCreateStaffNotificationTriggerMutation,
  useUpdateStaffNotificationTriggerMutation,
  useDeleteStaffNotificationTriggerMutation,
  useGetStaffNotificationTemplatesQuery,
  useCreateStaffNotificationTemplateMutation,
  useUpdateStaffNotificationTemplateMutation,
  useDeleteStaffNotificationTemplateMutation,
  useGetStaffNotificationPreferencesQuery,
  useUpdateStaffNotificationPreferenceMutation,
} = staffNotificationsApi;
