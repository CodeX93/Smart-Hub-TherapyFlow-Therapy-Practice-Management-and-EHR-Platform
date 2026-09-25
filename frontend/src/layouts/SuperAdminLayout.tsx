import { useState } from "react";
import { SuspenseOutlet } from "@/components/shared/SuspenseOutlet";
import { superAdminMenuItems } from "../utils/menuList";
import { userStaffData } from "../types/user.type";
import Sidebar from "../components/sidebar";
import { useSidebar } from "../contexts/sidebar";
import { SidebarProvider } from "../contexts/SidebarContext";
import { cn } from "../lib/utils";
import { useStaffIdleLogout } from "@/hooks/useStaffIdleLogout";

function getSidebarOffsetClassName(isCollapsed: boolean): string {
  if (isCollapsed) {
    return "md:ml-16";
  }

  return "md:ml-63";
}

function SuperAdminLayoutContent() {
  useStaffIdleLogout();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { isCollapsed } = useSidebar();

  const user = userStaffData;

  function handleSidebarClose() {
    setSidebarOpen(false);
  }

  return (
    <div className="dashboard-shell h-screen overflow-hidden bg-(--bg-primary-light) px-2.5 md:px-4">
      <Sidebar
        isOpen={sidebarOpen}
        onClose={handleSidebarClose}
        menuItems={superAdminMenuItems}
        user={user}
      />

      <div
        className={cn(
          "relative flex h-full min-h-0 flex-col box-border py-5 transition-all duration-700 ease-in-out md:pb-5 md:pt-2",
          getSidebarOffsetClassName(isCollapsed)
        )}
      >
        <main className="flex-1 min-h-0 overflow-hidden pt-3">
          <div className="mx-auto h-full min-h-full w-full max-w-[100rem] overflow-auto pb-6 sm:px-5 lg:px-6 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            <SuspenseOutlet />
          </div>
        </main>
      </div>
    </div>
  );
}

function SuperAdminLayout() {
  return (
    <SidebarProvider>
      <SuperAdminLayoutContent />
    </SidebarProvider>
  );
}

export default SuperAdminLayout;
