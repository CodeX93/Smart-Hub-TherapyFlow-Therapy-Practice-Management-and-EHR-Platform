import { useAppSelector } from "@/store/hooks";
import { useGetStaffNotificationUnreadCountQuery } from "@/store/api/admin/notifications.api";
import { useGetPortalNotificationUnreadCountQuery } from "@/store/api/portalNotificationsApi";
import { canAccessStaffRoute } from "@/utils/staffPermissions";

/**
 * Prefetch staff notification unread count on admin/therapist portal entry
 * so the bell badge can render without opening the dropdown.
 */
export function useStaffNotificationUnreadBootstrap(): void {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const authRole = useAppSelector((state) => state.auth.role);
  const permissions = useAppSelector((state) => state.auth.permissions);
  const authBootstrapResolved = useAppSelector((state) => state.auth.authBootstrapResolved);

  const isAdminOrTherapist =
    Boolean(accessToken) && (authRole === "admin" || authRole === "therapist");
  const canFetchStaffPortalNotifications =
    Boolean(accessToken) &&
    authRole === "staff" &&
    authBootstrapResolved &&
    canAccessStaffRoute(permissions, "system-notifications");

  useGetStaffNotificationUnreadCountQuery(undefined, {
    skip: !isAdminOrTherapist && !canFetchStaffPortalNotifications,
    refetchOnMountOrArgChange: true,
  });
}

/**
 * Prefetch client portal notification unread count on portal entry
 * so the bell badge can render without opening the dropdown.
 */
export function usePortalNotificationUnreadBootstrap(): void {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const authRole = useAppSelector((state) => state.auth.role);
  const isPortalClient = Boolean(accessToken) && authRole === "user";

  useGetPortalNotificationUnreadCountQuery(undefined, {
    skip: !isPortalClient,
    refetchOnMountOrArgChange: true,
  });
}
