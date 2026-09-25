
import { useState } from "react";
import { SuspenseOutlet } from "@/components/shared/SuspenseOutlet";
import { adminMenuItems } from "../utils/menuList";
import { userStaffData } from "../types/user.type";
import Sidebar from "../components/sidebar";
import Topbar from "../components/topbar";
import { useSidebar } from "../contexts/sidebar";
import { SidebarProvider } from "../contexts/SidebarContext";
import { cn } from "../lib/utils";
import { useStaffNotificationUnreadBootstrap } from "@/hooks/useNotificationUnreadBootstrap";
import SubscriptionAccessBanner from "@/components/billing-sections/SubscriptionAccessBanner";
import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";

const AdminLayoutContent: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { isCollapsed } = useSidebar();

  const user = userStaffData;

  return (
    <div className="dashboard-shell h-screen overflow-hidden bg-(--bg-primary-light) md:px-4">
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        menuItems={adminMenuItems}
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
          <div className="flex min-h-0 flex-1 flex-col overflow-auto px-4 pb-4 sm:px-5 lg:px-6">
            <SubscriptionAccessBanner />
            <SuspenseOutlet />
          </div>
        </main>
      </div>
    </div>
  );
};

const AdminLayout: React.FC = () => {
  useStaffNotificationUnreadBootstrap();
  // The shell's menu is fixed for this role and nothing here reads the
  // permissions the bootstrap fetches, so render immediately and let it refine
  // the session in the background. Blocking on it only ever cost a blank page.
  useAuthBootstrap();

  return (
    <SidebarProvider>
      <AdminLayoutContent />
    </SidebarProvider>
  );
};

export default AdminLayout;
