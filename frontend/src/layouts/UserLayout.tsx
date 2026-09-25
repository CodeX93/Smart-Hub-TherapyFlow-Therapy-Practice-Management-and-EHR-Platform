import { useState } from "react";
import { useLocation } from "react-router-dom";
import { SuspenseOutlet } from "@/components/shared/SuspenseOutlet";
import { userMenuItems } from "../utils/menuList";
import { userStaffData } from "../types/user.type";
import Sidebar from "../components/sidebar";
import Topbar from "../components/topbar";
import { useSidebar } from "../contexts/sidebar";
import { SidebarProvider } from "../contexts/SidebarContext";
import { cn } from "../lib/utils";
import { usePortalNotificationUnreadBootstrap } from "@/hooks/useNotificationUnreadBootstrap";
import { usePortalTimezoneBootstrap } from "@/hooks/usePortalTimezoneBootstrap";
import { usePortalIdleLogout } from "@/hooks/usePortalIdleLogout";

const UserLayoutContent: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const location = useLocation();
  const { isCollapsed } = useSidebar();

  const user = userStaffData;

  // Hide sidebar and topbar for clinical form detail pages and privacy settings
  const isFormDetailPage = location.pathname.match(
    /\/user\/clinical-forms\/[^/]+$/
  );
  const isPrivacySettingsPage = location.pathname === "/user/privacy-settings";

  if (isFormDetailPage || isPrivacySettingsPage) {
    return (
      <div className="dashboard-shell h-screen overflow-y-auto bg-(--bg-primary-light)">
        <SuspenseOutlet />
      </div>
    );
  }

  return (
    <div className="dashboard-shell h-screen overflow-hidden bg-(--bg-primary-light) md:px-4">
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        menuItems={userMenuItems}
        user={user}
      />

      <div
        className={cn(
          "transition-all duration-700 flex flex-col h-full min-h-0 ease-in-out pb-5 md:pt-2 md:pb-5 relative",
          isCollapsed ? "md:ml-16" : "md:ml-63"
        )}
      >
        <Topbar onMenuClick={() => setSidebarOpen(true)} user={user} />
        <main className="flex flex-1 min-h-0 flex-col overflow-hidden pt-3">
          <div className="flex min-h-0 flex-1 flex-col overflow-auto px-4 pb-6 sm:px-5 lg:px-6 [scrollbar-gutter:stable]">
            <SuspenseOutlet />
          </div>
        </main>
      </div>
    </div>
  );
};

const UserLayout: React.FC = () => {
  usePortalIdleLogout();
  usePortalTimezoneBootstrap();
  usePortalNotificationUnreadBootstrap();

  return (
    <SidebarProvider>
      <UserLayoutContent />
    </SidebarProvider>
  );
};

export default UserLayout;
