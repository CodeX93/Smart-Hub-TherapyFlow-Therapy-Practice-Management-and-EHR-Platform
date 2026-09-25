import { 
  useGetSuperAdminUnreadNotificationCountQuery,
  useGetSuperAdminNotificationHistoryQuery
} from "@/store/api/superAdminApi";
import { Bell } from "lucide-react";
import ProfileDropdown from "@/components/shared/ProfileDropdown";
import NotificationSidePanel from "@/components/notification/NotificationSidePanel";
import { useState } from "react";

interface DashboardHeaderProps {
  userInitials: string;
  userFullName: string;
}

function DashboardHeader(props: DashboardHeaderProps) {
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const { data: unreadCount = 0 } = useGetSuperAdminUnreadNotificationCountQuery();
  const { data: notificationHistory = [] } = useGetSuperAdminNotificationHistoryQuery();

  function handleLogout() {
    window.location.href = "/super-admin/login";
  }

  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        className="relative flex h-9 w-9 items-center justify-center rounded-full text-[#24313f] transition-colors hover:bg-[#eff4f8]"
        aria-label="Notifications"
        onClick={() => setIsNotificationOpen(true)}
      >
        <Bell size={17} strokeWidth={1.9} aria-hidden="true" />
        <span className="absolute right-0.5 top-0.5 flex h-[1.125rem] min-w-[1.125rem] items-center justify-center rounded-full bg-[#ef4444] px-1 text-[0.625rem] font-semibold leading-none text-white">
          {unreadCount}
        </span>
      </button>

      <div className="rounded-full bg-white/80">
        <ProfileDropdown
          initials={props.userInitials}
          fullName={props.userFullName}
          onLogout={handleLogout}
        />
      </div>

      <NotificationSidePanel
        isOpen={isNotificationOpen}
        onClose={() => setIsNotificationOpen(false)}
        notifications={[]}
        onMarkAllRead={() => {}}
        isAdmin
        totalNotificationsCount={notificationHistory.length}
      />
    </div>
  );
}

export default DashboardHeader;
