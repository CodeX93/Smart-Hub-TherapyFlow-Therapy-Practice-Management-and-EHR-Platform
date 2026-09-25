import { cn } from "@/lib/utils";
import { ASSESSMENT_TABS } from "../../pages/admin/content/content.static";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";

interface AssessmentTabsProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
  counts: { [key: string]: number };
  onNewTemplate?: () => void;
  showNewTemplate?: boolean;
}

const AssessmentTabs = ({
  activeTab,
  onTabChange,
  counts,
  onNewTemplate,
  showNewTemplate = true,
}: AssessmentTabsProps) => {
  return (
    <div className="flex items-center justify-between w-full">
      <div className="flex bg-(--neutral-100) p-1 rounded-full w-fit">
        {ASSESSMENT_TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => onTabChange(tab)}
            className={cn(
              "px-6 py-2 rounded-full text-sm font-medium transition-all duration-300 cursor-pointer",
              activeTab === tab
                ? "bg-white text-(--neutral-950) shadow-sm"
                : "text-(--text-neutral-600) hover:text-(--neutral-950)"
            )}
          >
            {tab} ({counts[tab] || 0})
          </button>
        ))}
      </div>
      {showNewTemplate ? (
        <Button
          onClick={onNewTemplate}
          className="bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/95 text-white rounded-full px-6 py-2.5 h-auto flex items-center gap-2 font-semibold transition-all duration-300 cursor-pointer"
        >
          <Plus size={18} />
          New Template
        </Button>
      ) : null}
    </div>
  );
};

export default AssessmentTabs;
