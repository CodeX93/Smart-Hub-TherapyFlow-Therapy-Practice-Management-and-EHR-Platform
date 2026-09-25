import { cn } from "../../../../lib/utils";

interface ClientProfileTabsProps {
  tabs: string[];
  activeTab: string;
  onTabChange: (tab: string) => void;
}

const ClientProfileTabs = ({
  tabs,
  activeTab,
  onTabChange,
}: ClientProfileTabsProps) => {

  return (
    <div className="scrollbar-hide flex h-10 gap-1 overflow-x-auto rounded-full bg-(--neutral-100) p-1">
      <style>{`
        .scrollbar-hide::-webkit-scrollbar {
          display: none;
        }
      `}</style>
      {tabs.map((tab) => (
        <button
          key={tab}
          type="button"
          onClick={() => onTabChange(tab)}
          className={cn(
            "h-8 flex-1 cursor-pointer whitespace-nowrap rounded-full px-8 py-0 text-center text-sm font-medium leading-[1.375rem] transition-all",
            activeTab === tab
              ? "bg-white text-(--text-primary-dark) shadow-[0_1px_2px_#1E282E1A]"
              : "text-(--text-neutral-600) hover:text-(--text-primary-dark)"
          )}
        >
          {tab}
        </button>
      ))}
    </div>
  );
};

export default ClientProfileTabs;

