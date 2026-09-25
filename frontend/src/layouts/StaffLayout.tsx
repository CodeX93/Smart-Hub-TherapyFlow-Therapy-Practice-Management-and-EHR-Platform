
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useMemo, useState } from "react";
import { Navigate } from "react-router-dom";
import { SuspenseOutlet } from "@/components/shared/SuspenseOutlet";
import { userStaffData } from "../types/user.type";
import Sidebar from "../components/sidebar";
import Topbar from "../components/topbar";
import { useSidebar } from "../contexts/sidebar";
import { SidebarProvider } from "../contexts/SidebarContext";
import { cn } from "../lib/utils";
import { useStaffNotificationUnreadBootstrap } from "@/hooks/useNotificationUnreadBootstrap";
import SubscriptionAccessBanner from "@/components/billing-sections/SubscriptionAccessBanner";
import { buildStaffMenuItems } from "@/utils/staffMenu";
import { useAppSelector } from "@/store/hooks";
import { getRedirectPathByRole } from "@/utils/redirectPathByRole";

import { useAuthBootstrap } from "@/hooks/useAuthBootstrap";

const StaffLayoutContent: React.FC<{ isAuthBootstrapLoading: boolean }> = ({
  isAuthBootstrapLoading,
}) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { isCollapsed } = useSidebar();
  const permissions = useAppSelector((state) => state.auth.permissions);
  const apiRoles = useAppSelector((state) => state.auth.apiRoles);
  const menuItems = useMemo(
    () => buildStaffMenuItems(permissions, apiRoles),
    [permissions, apiRoles],
  );

  if (isAuthBootstrapLoading) {
    return (
      <ContentLoader className="h-screen bg-(--bg-primary-light)" />
    );
  }

  return (
    <div className="dashboard-shell h-screen overflow-hidden bg-(--bg-primary-light) md:px-4">
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        menuItems={menuItems}
        user={userStaffData}
      />

      <div
        className={cn(
          "transition-all duration-700 flex flex-col h-full min-h-0 ease-in-out pb-5 md:pt-2 md:pb-5 relative",
          isCollapsed ? "md:ml-16" : "md:ml-63",
        )}
      >
        <Topbar onMenuClick={() => setSidebarOpen(true)} user={userStaffData} />
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

const StaffPortalGuard: React.FC<{ isAuthBootstrapLoading: boolean }> = ({
  isAuthBootstrapLoading,
}) => {
  const role = useAppSelector((state) => state.auth.role);

  if (role && role !== "staff") {
    return <Navigate to={getRedirectPathByRole(role)} replace />;
  }

  return <StaffLayoutContent isAuthBootstrapLoading={isAuthBootstrapLoading} />;
};

const StaffLayout: React.FC = () => {
  useStaffNotificationUnreadBootstrap();
  const { isAuthBootstrapLoading } = useAuthBootstrap();

  return (
    <SidebarProvider>
      <StaffPortalGuard isAuthBootstrapLoading={isAuthBootstrapLoading} />
    </SidebarProvider>
  );
};

export default StaffLayout;
