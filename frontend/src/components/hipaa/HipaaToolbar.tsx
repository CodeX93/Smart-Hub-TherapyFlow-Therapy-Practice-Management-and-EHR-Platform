import { Search, Download } from "lucide-react";
import { useMemo } from "react";
import { Button } from "@/components/ui/button";
import CustomInput from "@/components/form/CustomInput";
import type { HIPAAFilters } from "@/types/hipaa.types";
import type { FilterType } from "@/pages/admin/compliance/compliance.static";
import HipaaFilterSlidebar from "./HipaaFilterSlidebar";
import type { ChangeEvent } from "react";
import AppliedFiltersBar from "@/components/shared/AppliedFiltersBar";
import {
  buildHipaaFilterChips,
  EMPTY_HIPAA_FILTERS,
  removeHipaaFilterChip,
} from "@/utils/appliedFilterChips";

interface HIPAAToolbarProps {
  searchQuery: string;
  handleSearchChange: (e: ChangeEvent<HTMLInputElement>) => void;
  handleExport: () => void;
  filters: HIPAAFilters;
  handleClearFilters: (filters: HIPAAFilters) => void;
  handleApplyFilter: (filters: HIPAAFilters) => void;
  actionTypes: FilterType[];
  riskLevels: FilterType[];
}

const HipaaToolbar = ({
  searchQuery,
  handleSearchChange,
  handleExport,
  filters,
  handleClearFilters,
  handleApplyFilter,
  actionTypes,
  riskLevels,
}: HIPAAToolbarProps) => {
  const appliedFilterChips = useMemo(
    () => buildHipaaFilterChips(filters),
    [filters],
  );

  const handleRemoveFilterChip = (chipId: string) => {
    handleApplyFilter(removeHipaaFilterChip(filters, chipId));
  };

  const handleClearAllFilters = () => {
    handleClearFilters(EMPTY_HIPAA_FILTERS);
  };

  return (
    <div className="flex w-full flex-col gap-3">
    <div className="flex items-center justify-between w-full gap-4">
      <div className="flex items-center gap-3 flex-1 max-w-sm">
        <CustomInput
          placeholder="Search by username..."
          value={searchQuery}
          onChange={handleSearchChange}
          icon={<Search size={18} className="text-(--text-neutral-600)" />}
          className="rounded-full min-h-10 w-full pb-0 pt-1.75 shadow-xs"
        />
        <HipaaFilterSlidebar
          filters={filters}
          handleApplyFilter={handleApplyFilter}
          handleClearFilters={handleClearFilters}
          actionTypes={actionTypes}
          riskLevels={riskLevels}
        />
      </div>

      <Button
        onClick={handleExport}
        className="bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/95 text-white rounded-full px-5! py-2.5 h-auto flex items-center gap-2 font-semibold transition-all duration-300 cursor-pointer"
      >
        <Download size={18} />
        Export Report
      </Button>
    </div>
    <AppliedFiltersBar
      chips={appliedFilterChips}
      onRemove={handleRemoveFilterChip}
      onClearAll={handleClearAllFilters}
      className="w-full"
    />
    </div>
  );
};

export default HipaaToolbar;
