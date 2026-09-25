import { createContext, useContext } from "react";
interface SidebarContextType {
  isCollapsed: boolean;
  setIsCollapsed: (collapsed: boolean) => void;
}

export const SidebarContext = createContext<SidebarContextType | undefined>(undefined);

export const useSidebar = () => {
  const context = useContext(SidebarContext);
  if (!context) {
    throw new Error("useSidebar must be used within SidebarProvider");
  }
  return context;
};
