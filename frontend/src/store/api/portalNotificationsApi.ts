import { baseApi } from "./baseApi";

export interface PortalNotificationItem {
  id: number;
  userId: number | null;
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

export interface PortalNotificationsQueryParams {
  page?: number;
  pageSize?: number;
  unreadOnly?: boolean;
}

export interface PortalNotificationsPage {
  items: PortalNotificationItem[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export const PORTAL_NOTIFICATIONS_PAGE_SIZE = 20;
export const PORTAL_NOTIFICATIONS_DROPDOWN_PAGE_SIZE = 5;

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

function asNullableNumber(value: unknown): number | null {
  if (value === null || value === undefined) return null;
  return asNumber(value);
}

function asNullableString(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed || null;
}

function normalizePortalNotificationItem(
  entry: unknown,
): PortalNotificationItem | null {
  if (!isRecord(entry)) return null;

  return {
    id: asNumber(entry.id),
    userId: asNullableNumber(entry.userId),
    clientId: asNullableNumber(entry.clientId),
    type: asNullableString(entry.type) ?? "",
    category: asNullableString(entry.category) ?? "",
    title: asNullableString(entry.title) ?? "",
    message: asNullableString(entry.message) ?? "",
    data: asNullableString(entry.data),
    priority: asNullableString(entry.priority) ?? "",
    isRead: Boolean(entry.isRead),
    readAt: asNullableString(entry.readAt),
    actionUrl: asNullableString(entry.actionUrl),
    actionLabel: asNullableString(entry.actionLabel),
    relatedEntityType: asNullableString(entry.relatedEntityType),
    relatedEntityId: asNullableNumber(entry.relatedEntityId),
    expiresAt: asNullableString(entry.expiresAt),
    createdAt: asNullableString(entry.createdAt) ?? "",
  };
}

function normalizePortalNotificationsPageResponse(
  payload: unknown,
): PortalNotificationsPage {
  if (Array.isArray(payload)) {
    const items = payload
      .map((entry) => normalizePortalNotificationItem(entry))
      .filter((entry): entry is PortalNotificationItem => entry !== null);

    return {
      items,
      totalCount: items.length,
      page: 1,
      pageSize: items.length || PORTAL_NOTIFICATIONS_PAGE_SIZE,
      totalPages: 1,
    };
  }

  const root = isRecord(payload) ? payload : {};
  const itemsRaw = Array.isArray(root.items) ? root.items : [];

  return {
    items: itemsRaw
      .map((entry) => normalizePortalNotificationItem(entry))
      .filter((entry): entry is PortalNotificationItem => entry !== null),
    totalCount: asNumber(root.totalCount),
    page: asNumber(root.page) || 1,
    pageSize: asNumber(root.pageSize) || PORTAL_NOTIFICATIONS_PAGE_SIZE,
    totalPages: asNumber(root.totalPages) || 1,
  };
}

function normalizePortalUnreadCount(payload: unknown): number {
  if (typeof payload === "number" && Number.isFinite(payload)) return payload;
  if (!isRecord(payload)) return 0;
  return asNumber(payload.count ?? payload.unread ?? payload.unreadCount);
}

export const portalNotificationsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getPortalNotifications: builder.query<
      PortalNotificationsPage,
      PortalNotificationsQueryParams | void
    >({
      query: (params) => {
        const unreadOnly =
          params && "unreadOnly" in params ? params.unreadOnly : undefined;
        const queryParams: Record<string, string | number | boolean> = {};

        if (params?.page) queryParams.page = params.page;
        if (params?.pageSize) queryParams.pageSize = params.pageSize;
        if (unreadOnly !== undefined) queryParams.unreadOnly = unreadOnly;

        return {
          url: "/api/v1/portal/notifications",
          params: Object.keys(queryParams).length > 0 ? queryParams : undefined,
        };
      },
      transformResponse: (payload) =>
        normalizePortalNotificationsPageResponse(payload),
      providesTags: ["PortalNotifications"],
    }),
    getPortalNotificationUnreadCount: builder.query<number, void>({
      query: () => "/api/v1/portal/notifications/unread/count",
      transformResponse: (payload) => normalizePortalUnreadCount(payload),
      providesTags: ["PortalNotifications"],
    }),
    markPortalNotificationRead: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/portal/notifications/${id}/read`,
        method: "PATCH",
      }),
      invalidatesTags: ["PortalNotifications"],
    }),
    markAllPortalNotificationsRead: builder.mutation<void, void>({
      query: () => ({
        url: "/api/v1/portal/notifications/read-all",
        method: "PATCH",
      }),
      invalidatesTags: ["PortalNotifications"],
    }),
    deletePortalNotification: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/portal/notifications/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["PortalNotifications"],
    }),
  }),
});

export const {
  useGetPortalNotificationsQuery,
  useLazyGetPortalNotificationsQuery,
  useGetPortalNotificationUnreadCountQuery,
  useMarkPortalNotificationReadMutation,
  useMarkAllPortalNotificationsReadMutation,
  useDeletePortalNotificationMutation,
} = portalNotificationsApi;
