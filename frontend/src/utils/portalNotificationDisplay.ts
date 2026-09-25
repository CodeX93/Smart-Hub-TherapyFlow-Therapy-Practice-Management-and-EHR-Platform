import type { PortalNotificationItem } from "@/store/api/portalNotificationsApi";
import type { Notification } from "@/types/notification";
import {
  formatNotificationDateGroup,
  formatNotificationMessage,
  formatNotificationTimestamp,
} from "@/utils/notificationDisplay";
import { resolveNotificationActionPath } from "@/utils/notificationActionUrl";

export function resolvePortalNotificationActionUrl(
  actionUrl: string | null | undefined,
): string | null {
  if (!actionUrl?.trim()) return null;

  const trimmed = actionUrl.trim();
  if (trimmed.startsWith("/user/")) return trimmed;
  if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    return resolveNotificationActionPath(trimmed, window.location.origin);
  }

  if (trimmed.startsWith("/portal/")) {
    const remainder = trimmed.slice("/portal/".length);
    const [section] = remainder.split("/").filter(Boolean);
    const routeMap: Record<string, string> = {
      appointments: "/user/appointments",
      "booked-sessions": "/user/booked-sessions",
      invoices: "/user/invoices",
      documents: "/user/documents",
      forms: "/user/clinical-forms",
      "clinical-forms": "/user/clinical-forms",
      assessments: "/user/clinical-forms",
      "privacy-settings": "/user/privacy-settings",
      "my-profile": "/user/my-profile",
    };

    if (section && routeMap[section]) {
      return routeMap[section];
    }
  }

  return trimmed.startsWith("/") ? trimmed : null;
}

export function mapPortalNotificationToUi(
  notification: PortalNotificationItem,
  previousCreatedAt?: string,
): Notification {
  const dateGroup =
    previousCreatedAt &&
    formatNotificationDateGroup(previousCreatedAt) ===
      formatNotificationDateGroup(notification.createdAt)
      ? undefined
      : formatNotificationDateGroup(notification.createdAt);

  return {
    id: String(notification.id),
    title: notification.title || notification.type || "Notification",
    description: formatNotificationMessage(notification.message),
    timestamp: formatNotificationTimestamp(notification.createdAt),
    isRead: Boolean(notification.isRead),
    dateGroup,
    actionUrl:
      resolvePortalNotificationActionUrl(notification.actionUrl) ?? undefined,
    actionLabel: notification.actionLabel || undefined,
  };
}

export function mapPortalNotificationsToUi(
  notifications: PortalNotificationItem[],
): Notification[] {
  return notifications.map((notification, index) =>
    mapPortalNotificationToUi(
      notification,
      index > 0 ? notifications[index - 1]?.createdAt : undefined,
    ),
  );
}
