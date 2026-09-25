import React, { useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { PanelRight, ChevronDown, ChevronRight } from "lucide-react";
import { useSidebar } from "../../contexts/sidebar";
import type { MenuItem, User } from "../../types/user.type";
import { cn } from "../../lib/utils";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

const Sidebar: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  menuItems: MenuItem[];
  user: User;
}> = ({ isOpen, onClose, menuItems }) => {
  const { isCollapsed, setIsCollapsed } = useSidebar();
  const location = useLocation();

  // State for expanded nested menus
  const [expandedMenus, setExpandedMenus] = useState<Record<string, boolean>>(
    {},
  );

  const toggleMenu = (id: string) => {
    if (isCollapsed) setIsCollapsed(false);
    setExpandedMenus((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const isActiveLink = (path: string) =>
    location.pathname === path || location.pathname.startsWith(path + "/");

  return (
    <>
      <TooltipProvider disableHoverableContent>
        {/* Overlay for mobile */}
        {isOpen && (
          <div
            className="fixed inset-0 bg-(--mobile-overlay) z-50 md:hidden transition-all duration-700 ease-in-out"
            onClick={onClose}
          />
        )}

        {/* Sidebar */}
        <aside
          className={cn(
            "fixed bg-(--bg-primary-dark) text-white z-50 transform transition-all duration-700 ease-in-out flex flex-col overflow-x-hidden",
            isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0",
            isCollapsed ? "md:w-16 md:px-2" : "md:w-63 px-3",
            "w-72.5 top-0 left-0 bottom-0 rounded-none",
            "md:top-4 md:left-4 md:bottom-4 md:rounded-2xl",
            "py-6.5",
          )}
        >
          <div className="flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar">
            {/* Logo Section */}
            <div className="flex items-center mb-6 pl-1">
              <div className="flex items-center gap-3 w-full">
                <div
                  onClick={() => setIsCollapsed(!isCollapsed)}
                  className="w-9.5 h-9.5 bg-white rounded-[0.6875rem] cursor-pointer pointer-events-none md:pointer-events-auto flex items-center justify-center uppercase text-(--text-primary-dark) font-semibold shrink-0"
                >
                  SH
                </div>
                <div
                  className={cn(
                    "flex flex-col overflow-hidden transition-all duration-700 ease-in-out whitespace-nowrap",
                    isCollapsed ? "w-0 opacity-0" : "w-auto opacity-100",
                  )}
                >
                  <h1 className="text-base font-semibold">SmartHub</h1>
                  <p className="text-xs text-(--text-primary-light) font-medium leading-4.5">
                    Intelligent Insights
                  </p>
                </div>

                <div className="ml-auto flex items-center">
                  <button onClick={onClose} className="md:hidden">
                    <PanelRight className="size-5 text-(--text-primary-light)" />
                  </button>
                  {!isCollapsed && (
                    <button
                      onClick={() => setIsCollapsed(!isCollapsed)}
                      className="hidden md:flex cursor-pointer shrink-0 transition-all duration-700 ease-in-out ml-auto"
                    >
                      <PanelRight className="size-5 text-(--text-primary-light)" />
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Menu Items */}
            <nav className="space-y-1">
              {menuItems?.map((item) => {
                if (item.children) {
                  // Expandable Menu Item
                  const isExpanded = expandedMenus[item.id];
                  const isActiveParent = item.children.some((child) =>
                    isActiveLink(child.path),
                  );

                  const MenuItemContent = (
                    <button
                      onClick={() => toggleMenu(item.id)}
                      onPointerDown={(e) => e.preventDefault()}
                      className={cn(
                        "group w-full flex items-center justify-between min-h-11.5 px-3.5 py-3 rounded-lg transition-all duration-200 cursor-pointer mb-1",
                        isActiveParent
                          ? "bg-white/10 text-white"
                          : "text-white hover:bg-white/20",
                        isCollapsed && "justify-center px-0 hover:bg-white/20",
                      )}
                    >
                      <div className="flex items-center gap-3 overflow-hidden w-full">
                        <span
                          className={cn(
                            "shrink-0 flex items-center justify-center",
                            isCollapsed ? "mx-auto" : "",
                          )}
                        >
                          {item.icon}
                        </span>
                        <span
                          className={cn(
                            "text-sm font-normal leading-5.5 whitespace-nowrap transition-all duration-700",
                            isCollapsed
                              ? "w-0 opacity-0 hidden"
                              : "w-auto opacity-100",
                          )}
                        >
                          {item.label}
                        </span>
                      </div>
                      {!isCollapsed && (
                        <div className="text-white/70">
                          {isExpanded ? (
                            <ChevronDown className="size-4" />
                          ) : (
                            <ChevronRight className="size-4" />
                          )}
                        </div>
                      )}
                    </button>
                  );

                  return (
                    <div key={item.id} className="w-full">
                      {isCollapsed ? (
                        <Tooltip delayDuration={300}>
                          <TooltipTrigger asChild>
                            {MenuItemContent}
                          </TooltipTrigger>
                          <TooltipContent side="right" sideOffset={10}>
                            {item.label}
                          </TooltipContent>
                        </Tooltip>
                      ) : (
                        MenuItemContent
                      )}

                      {/* Nested Children */}
                      <div
                        className={cn(
                          "overflow-hidden transition-all duration-300 ease-in-out space-y-1",
                          isExpanded && !isCollapsed
                            ? "max-h-125 opacity-100 mt-1"
                            : "max-h-0 opacity-0",
                        )}
                      >
                        {item.children.map((child) => (
                          <NavLink
                            key={child.id}
                            to={child.path}
                            onClick={onClose}
                            className={({ isActive }) =>
                              cn(
                                "flex items-center pl-11 pr-4 py-2 text-[0.8125rem] font-medium rounded-lg transition-colors duration-200 block w-full mb-1",
                                isActive
                                  ? "text-white font-semibold blink-0"
                                  : "text-white/70 hover:text-white hover:bg-white/10",
                              )
                            }
                          >
                            <span className="w-1.5 h-1.5 rounded-full bg-current mr-2 shrink-0 opacity-50" />
                            {child.label}
                          </NavLink>
                        ))}
                      </div>
                    </div>
                  );
                }

                // Standard Menu Item
                const isActive = isActiveLink(item.path);
                const StandardItemContent = (
                  <NavLink
                    to={item.path}
                    end={item.path === "/"} // Assuming dashboard is sometimes root
                    onClick={onClose}
                    onPointerDown={(e) => e.preventDefault()}
                    className={cn(
                      "group w-full flex items-center min-h-11.5 px-3.5 py-3 rounded-lg mb-1 transition-all duration-200",
                      isCollapsed ? "justify-center" : "justify-start",
                      isActive
                        ? "bg-white text-black"
                        : "text-white hover:bg-white/20",
                    )}
                  >
                    <div className="flex items-center w-full">
                      <span
                        className={cn(
                          "flex items-center justify-center shrink-0 transition-colors",
                          isActive
                            ? "text-(--text-primary-dark)"
                            : "text-white",
                        )}
                      >
                        {item.icon}
                      </span>

                      <span
                        className={cn(
                          "text-sm font-normal leading-5.5 ml-3 whitespace-nowrap overflow-hidden transition-all duration-700",
                          isCollapsed ? "w-0 opacity-0" : "w-auto opacity-100",
                          isActive
                            ? "text-(--text-primary-dark)"
                            : "text-(--text-neutral-100)",
                        )}
                      >
                        {item.label}
                      </span>
                    </div>
                  </NavLink>
                );

                return isCollapsed ? (
                  <Tooltip key={item.id} delayDuration={300}>
                    <TooltipTrigger asChild>
                      {StandardItemContent}
                    </TooltipTrigger>
                    <TooltipContent side="right" sideOffset={10}>
                      {item.label}
                    </TooltipContent>
                  </Tooltip>
                ) : (
                  <React.Fragment key={item.id}>
                    {StandardItemContent}
                  </React.Fragment>
                );
              })}
            </nav>
          </div>

          {/* Footer Toggle Button only when collapsed to uncollapse */}
          {isCollapsed && (
            <button
              onClick={() => setIsCollapsed(!isCollapsed)}
              className={cn(
                "hidden md:flex items-center justify-center cursor-pointer mt-auto shrink-0 transition-all duration-700 ease-in-out",
                isCollapsed ? "mx-auto" : "ml-auto",
              )}
            >
              <PanelRight className="size-5 text-(--text-primary-light)" />
            </button>
          )}
        </aside>
      </TooltipProvider>
    </>
  );
};

export default Sidebar;
