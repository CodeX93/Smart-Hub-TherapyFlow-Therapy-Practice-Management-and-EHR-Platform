import { useMemo } from "react";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "../ui/button";
import SchedulingFilterDropdown, {
  type SchedulingFilters,
} from "./SchedulingFilterDropdown";
import CustomInput from "../form/CustomInput";
import AppliedFiltersBar from "../shared/AppliedFiltersBar";
import {
  buildSchedulingFilterChips,
  removeSchedulingFilterChip,
} from "@/utils/appliedFilterChips";

interface SchedulingControlsProps {
  currentDateLabel?: string;
  onToday?: () => void;
  onPrev?: () => void;
  onNext?: () => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  filters: SchedulingFilters;
  setFilters: (filters: SchedulingFilters) => void;
  statusOptions: { value: string; label: string }[];
  serviceCodeOptions: { value: string; label: string }[];
  therapistOptions?: { value: string; label: string }[];
  onApplyFilters?: (f: SchedulingFilters) => void;
  onClearFilters?: (f: SchedulingFilters) => void;
  isAdmin?: boolean;
  isSession?: boolean;
  mySessionsOnly?: boolean;
  setMySessionsOnly?: (value: boolean) => void;
}

const SchedulingControls = ({
  currentDateLabel,
  onToday,
  onPrev,
  onNext,
  searchQuery,
  setSearchQuery,
  filters,
  setFilters,
  statusOptions,
  serviceCodeOptions,
  therapistOptions,
  onApplyFilters,
  onClearFilters,
  isSession,
}: SchedulingControlsProps) => {
  const appliedFilterChips = useMemo(
    () =>
      buildSchedulingFilterChips(filters, {
        statusOptions,
        serviceCodeOptions,
        therapistOptions,
      }),
    [filters, serviceCodeOptions, statusOptions, therapistOptions],
  );

  const handleRemoveFilterChip = (chipId: string) => {
    const nextFilters = removeSchedulingFilterChip(filters, chipId);
    setFilters(nextFilters);
    onApplyFilters?.(nextFilters);
  };

  const handleClearAllFilters = () => {
    const clearedFilters: SchedulingFilters = {
      startDate: null,
      endDate: null,
      status: null,
      serviceCode: null,
      therapist: null,
      includeHiddenServices: false,
    };
    setFilters(clearedFilters);
    onClearFilters?.(clearedFilters);
    onApplyFilters?.(clearedFilters);
  };

  return (
    <div className="my-4">
    <div className="flex md:flex-row flex-col gap-2 md:gap-0 md:items-center justify-between">
      {!isSession ? (
        <div className="flex items-center gap-4">
          <Button
            variant="outline"
            className="rounded-full px-4 border-(--neutral-100) text-(--text-neutral-800) hover:bg-(--neutral-50) cursor-pointer shadow-none h-9"
            onClick={onToday}
          >
            Today
          </Button>
          <div className="flex items-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              className="text-(--text-neutral-600) hover:text-(--neutral-950) cursor-pointer"
              onClick={onPrev}
            >
              <ChevronLeft size={20} />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="text-(--text-neutral-600) hover:text-(--neutral-950) cursor-pointer"
              onClick={onNext}
            >
              <ChevronRight size={20} />
            </Button>
          </div>
          <span className="text-[1rem] font-medium text-(--text-primary-dark)">
            {currentDateLabel}
          </span>
        </div>
      ) : (
        <div className="font-semibold text-(--text-primary-dark)">
          All Sessions
        </div>
      )}
      <div className="flex items-center gap-2">
        <CustomInput
          placeholder="Search clients..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
          className="rounded-full min-h-10 md:w-70 w-full pb-0 pt-1.75"
        />
        <SchedulingFilterDropdown
          filters={filters}
          setFilters={setFilters}
          statusOptions={statusOptions}
          serviceCodeOptions={serviceCodeOptions}
          therapistOptions={therapistOptions}
          onApply={onApplyFilters}
          onClear={onClearFilters}
        />
      </div>
    </div>
    <AppliedFiltersBar
      chips={appliedFilterChips}
      onRemove={handleRemoveFilterChip}
      onClearAll={handleClearAllFilters}
      className="mt-3"
    />
    </div>
  );
};

export default SchedulingControls;
