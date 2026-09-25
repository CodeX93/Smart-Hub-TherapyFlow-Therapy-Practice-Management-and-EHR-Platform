import { Search } from "lucide-react";
import type { RefObject } from "react";
import CustomInput from "@/components/form/CustomInput";
import {
  ASSESSMENT_CATEGORIES,
  ASSESSMENT_STATUS_OPTIONS,
} from "../../pages/admin/content/content.static";
import CustomSelect from "../form/CustomSelect";

interface AssessmentFiltersProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  selectedFilter: string;
  onFilterChange: (value: string) => void;
  categoryOptions?: { value: string; label: string }[];
  activeTab: string;
  scrollContainerRef?: RefObject<HTMLElement | null>;
}

const AssessmentFilters = ({
  searchQuery,
  onSearchChange,
  selectedFilter,
  onFilterChange,
  categoryOptions: dynamicCategoryOptions,
  activeTab,
  scrollContainerRef,
}: AssessmentFiltersProps) => {
  const categoryOptions = (dynamicCategoryOptions ?? ASSESSMENT_CATEGORIES.map((cat) => ({
    value: cat,
    label: cat,
  })));
  const statusOptions = [
    { value: "All Statuses", label: "All Statuses" },
    ...ASSESSMENT_STATUS_OPTIONS,
  ];

  return (
    <div className="flex items-center justify-between gap-4">
      <div className="flex items-center gap-3 flex-1">
        <div className="max-w-xs w-full">
          <CustomInput
            placeholder={
              activeTab === "Templates"
                ? "Search templates..."
                : "Search assignments..."
            }
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
            className="rounded-full min-h-10 md:w-79 pb-0 pt-1.75 bg-white shadow-sm"
          />
        </div>

        <div className="w-48">
          <CustomSelect
            options={
              activeTab === "Templates"
                ? categoryOptions
                : statusOptions
            }
            value={selectedFilter}
            onChange={onFilterChange}
            className="rounded-full max-h-10 w-40 pb-0 pt-0 bg-white shadow-sm"
            isSearch={false}
            closeOnScroll
            scrollContainerRefs={scrollContainerRef ? [scrollContainerRef] : undefined}
            placeholder={
              activeTab === "Templates" ? "All Categories" : "All Statuses"
            }
          />
        </div>
      </div>
    </div>
  );
};

export default AssessmentFilters;
