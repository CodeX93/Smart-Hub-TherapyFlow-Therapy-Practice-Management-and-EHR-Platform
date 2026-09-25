import { Plus, Search } from "lucide-react";
import type { RefObject } from "react";
import { Button } from "@/components/ui/button";
import CustomInput from "@/components/form/CustomInput";
import { CHECKLIST_CATEGORIES } from "@/pages/admin/content/content.static";
import CustomSelect from "../form/CustomSelect";

interface ChecklistToolbarProps {
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  selectedCategory: string;
  setSelectedCategory: (category: string) => void;
  onCreateClick?: () => void;
  onChecklistItemsClick?: () => void;
  scrollContainerRef?: RefObject<HTMLElement | null>;
  showCreateTemplate?: boolean;
}

const ChecklistToolbar = ({
  searchQuery,
  setSearchQuery,
  selectedCategory,
  setSelectedCategory,
  onCreateClick,
  onChecklistItemsClick,
  scrollContainerRef,
  showCreateTemplate = true,
}: ChecklistToolbarProps) => {
  return (
    <div className="flex flex-col md:flex-row items-center justify-between gap-4">
      <div className="flex items-center gap-3 flex-1 min-w-0 w-full md:w-auto">
        <div className="max-w-xs w-full shrink-0">
          <CustomInput
            placeholder="Search..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="rounded-full min-h-10 pt-1.75 pb-0 shadow-xs w-full"
            icon={<Search className="size-4.5 text-(--text-neutral-400)" />}
            stopFloating
          />
        </div>

        <div className="w-48 shrink-0">
          <CustomSelect
            value={selectedCategory}
            onChange={setSelectedCategory}
            options={CHECKLIST_CATEGORIES.map((cat) => ({
              value: cat,
              label: cat,
            }))}
            className="rounded-full max-h-10 w-40 pb-0 pt-0 bg-white shadow-xs"
            isSearch={false}
            closeOnScroll
            scrollContainerRefs={scrollContainerRef ? [scrollContainerRef] : undefined}
          />
        </div>
      </div>

      <div className="flex items-center gap-3 w-full md:w-auto">
        <Button
          onClick={onChecklistItemsClick}
          variant="outline"
          className="hidden h-10 rounded-full shadow-xs border border-(--neutral-100) bg-white text-(--text-neutral-800) hover:bg-(--neutral-50) cursor-pointer text-sm font-medium"
        >
          Checklist Items
        </Button>
        {showCreateTemplate ? (
          <Button
            onClick={onCreateClick}
            className="h-10 bg-(--bg-primary-dark) shadow-xs hover:bg-(--bg-primary-dark)/90 text-white rounded-full cursor-pointer flex items-center gap-2 text-sm font-medium"
          >
            <Plus size={18} />
            Create Template
          </Button>
        ) : null}
      </div>
    </div>
  );
};

export default ChecklistToolbar;
