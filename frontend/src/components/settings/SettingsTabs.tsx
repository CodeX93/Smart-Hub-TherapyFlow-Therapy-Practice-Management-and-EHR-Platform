import { cn } from "@/lib/utils";

interface SettingsTabsProps {
  activeTab: string;
  onTabChange: (tabId: string) => void;
}

const tabs = [
  { id: "system-options", label: "System Options" },
  { id: "services", label: "Services" },
  { id: "public-site", label: "Public Site" },
  { id: "invoice-policies", label: "Invoice Policies" },
  { id: "therapy-rooms", label: "Therapy Rooms" },
  { id: "library-categories", label: "Library Categories" },
  { id: "administration", label: "Administration" },
];

const SettingsTabs = ({ activeTab, onTabChange }: SettingsTabsProps) => {
  return (
    <div className="overflow-x-auto pb-1 custom-scrollbar">
      <div className="inline-flex min-w-full w-max items-center rounded-full border border-(--neutral-100) bg-(--neutral-100) p-1 shadow-xs">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => onTabChange(tab.id)}
            className={cn(
              "shrink-0 whitespace-nowrap rounded-full px-4 py-2.5 text-sm font-medium transition-all duration-300 cursor-pointer sm:px-6 lg:px-8",
              activeTab === tab.id
                ? "bg-white text-(--text-primary-dark) shadow-xs"
                : "text-(--text-neutral-600) hover:text-(--text-primary-dark)",
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </div>
  );
};

export default SettingsTabs;
