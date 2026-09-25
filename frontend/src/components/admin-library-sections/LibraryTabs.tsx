import React from "react";
import { cn } from "@/lib/utils";

export interface LibraryTabItem {
  id: string;
  name: string;
}

interface LibraryTabsProps {
  tabs: LibraryTabItem[];
  activeTabId: string;
  onTabChange: (tabId: string) => void;
}

const LibraryTabs: React.FC<LibraryTabsProps> = ({
  tabs,
  activeTabId,
  onTabChange,
}) => {
  return (
    <div className="w-full rounded-full bg-(--neutral-100) p-1">
      <div className="overflow-x-auto overflow-y-hidden">
        <div className="flex min-w-max items-center gap-1">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              title={tab.name}
              className={cn(
                "max-w-[13.75rem] shrink-0 rounded-full px-4 py-2 text-sm font-medium transition-all duration-200 cursor-pointer",
                "truncate text-left sm:text-center",
                activeTabId === tab.id
                  ? "bg-white text-(--neutral-950) shadow-sm"
                  : "text-(--text-neutral-600) hover:text-(--neutral-950)"
              )}
            >
              {tab.name}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default LibraryTabs;
