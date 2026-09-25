import { useEffect, useState } from "react";
import { Bell } from "lucide-react";
import ProfileDropdown from "@/components/shared/ProfileDropdown";
import NotificationSidePanel from "@/components/notification/NotificationSidePanel";
import { useAppSelector } from "@/store/hooks";
import { 
  useGetSuperAdminUnreadNotificationCountQuery,
  useGetSuperAdminNotificationHistoryQuery 
} from "@/store/api/superAdminApi";

interface SuperAdminHeaderActionsProps {
  userInitials?: string;
  userFullName?: string;
  notificationCount?: number;
}

function SuperAdminHeaderActions(props: SuperAdminHeaderActionsProps) {
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const authUser = useAppSelector((state) => state.auth.user);
  const authClient = useAppSelector((state) => state.auth.client);
  const { data: unreadCount = 0, refetch: refetchUnreadCount } =
    useGetSuperAdminUnreadNotificationCountQuery(undefined, {
      refetchOnMountOrArgChange: true,
    });
  const { data: notificationHistory = [], refetch: refetchNotificationHistory } =
    useGetSuperAdminNotificationHistoryQuery(
      { page: 0, size: 25 },
      { refetchOnMountOrArgChange: true }
    );

  useEffect(() => {
    if (!isNotificationOpen) return;
    void refetchUnreadCount();
    void refetchNotificationHistory();
  }, [isNotificationOpen, refetchNotificationHistory, refetchUnreadCount]);

  function handleLogout() {
    window.location.href = "/super-admin/login";
  }

  function handleMarkAllRead() {
    // Notification history API currently has no mark-as-read endpoint.
  }

  const badgeCount = props.notificationCount ?? unreadCount;

  return (
    <>
      <div className="flex items-center gap-3">
        <button
          type="button"
          className="relative flex h-9 w-9 items-center justify-center rounded-full text-[#24313f] transition-colors hover:bg-[#eff4f8]"
          aria-label="Notifications"
          onClick={() => setIsNotificationOpen(true)}
        >
          <Bell size={17} strokeWidth={1.9} aria-hidden="true" />
          <span className="absolute right-0.5 top-0.5 flex h-[1.125rem] min-w-[1.125rem] items-center justify-center rounded-full bg-[#ef4444] px-1 text-[0.625rem] font-semibold leading-none text-white">
            {badgeCount}
          </span>
        </button>

        <div aria-hidden="true" className="h-5 w-px shrink-0 bg-[#D8DBDF]" />

        <ProfileDropdown
          initials={props.userInitials ?? (authUser?.fullName?.[0] ?? "JS")}
          fullName={
            props.userFullName ??
            authUser?.fullName ??
            authClient?.fullName ??
            "Jordan Smith"
          }
          onLogout={handleLogout}
        />
      </div>

      <NotificationSidePanel
        isOpen={isNotificationOpen}
        onClose={() => setIsNotificationOpen(false)}
        notifications={[]}
        onMarkAllRead={handleMarkAllRead}
        isAdmin
        totalNotificationsCount={notificationHistory.length}
      />
    </>
  );
}

export default SuperAdminHeaderActions;
