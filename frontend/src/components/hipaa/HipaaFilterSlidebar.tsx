import { useState } from "react";
import { X } from "lucide-react";
import { Filter as SolarFilter } from "@solar-icons/react-perf/category/ui/Linear/Filter";
import { Button } from "../ui/button";
import { cn } from "../../lib/utils";
import CustomSelect from "../form/CustomSelect";
import CustomDatePicker from "../form/CustomDatePicker";
import type { HIPAAFilters } from "@/types/hipaa.types";
import type { FilterType } from "@/pages/admin/compliance/compliance.static";

interface FilterDropdownProps {
  className?: string;
  filters: HIPAAFilters;
  handleApplyFilter: (filters: HIPAAFilters) => void;
  handleClearFilters: (filters: HIPAAFilters) => void;
  actionTypes: FilterType[];
  riskLevels: FilterType[];
}

const HipaaFilterSlidebar = ({
  className,
  filters,
  handleApplyFilter,
  handleClearFilters,
  actionTypes,
  riskLevels,
}: FilterDropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [localFilters, setLocalFilters] = useState<HIPAAFilters>(filters);

  const toggleOpen = () => {
    if (!isOpen) {
      setLocalFilters(filters);
    }
    setIsOpen(!isOpen);
  };

  const handleApply = () => {
    handleApplyFilter(localFilters);
    setIsOpen(false);
  };

  const handleClear = () => {
    const clearedFilters: HIPAAFilters = {
      startDate: null,
      endDate: null,
      actionType: null,
      riskLevel: null,
      phiAccessOnly: null,
    };
    setLocalFilters(clearedFilters);
    handleClearFilters(clearedFilters);
    setIsOpen(false);
  };

  const dateError =
    localFilters.startDate &&
    localFilters.endDate &&
    localFilters.startDate > localFilters.endDate
      ? "Start date cannot be after end date"
      : null;

  return (
    <>
      <div
        onClick={toggleOpen}
        className={cn(
          "flex gap-2 items-center bg-white rounded-full px-4 py-2 text-sm shadow-xs font-medium border border-(--neutral-100) cursor-pointer hover:bg-(--bg-primary-50) transition-colors duration-300",
          className,
        )}
      >
        Filters
        <SolarFilter size={20} color="#1B1C20" />
      </div>

      <div
        className={cn(
          "fixed inset-0 z-50 flex justify-end transition-all duration-300",
          isOpen ? "visible" : "invisible",
        )}
      >
        {/* Backdrop */}
        <div
          className={cn(
            "fixed inset-0 bg-black/40 transition-opacity duration-300",
            isOpen ? "opacity-100" : "opacity-0",
          )}
          onClick={() => setIsOpen(false)}
        />

        {/* Side Panel */}
        <div
          className={cn(
            "relative w-full max-w-135 bg-white shadow-2xl transition-transform duration-300 flex flex-col h-full",
            isOpen ? "translate-x-0" : "translate-x-full",
          )}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-5 border-b border-(--neutral-100)">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Audit Filters
            </h2>
            <button
              onClick={() => setIsOpen(false)}
              className="text-(--text-neutral-600) hover:text-(--text-primary-dark) transition-colors cursor-pointer"
              aria-label="Close filters"
            >
              <X className="size-6" />
            </button>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-6">
            {/* Action Type Picker */}
            <CustomSelect
              label="Action Type"
              placeholder="All Actions"
              value={localFilters.actionType || ""}
              onChange={(v) =>
                setLocalFilters({ ...localFilters, actionType: v as string })
              }
              options={actionTypes}
              isSearch={false}
            />

            {/* Risk Level Picker */}
            <CustomSelect
              label="Risk Level"
              placeholder="All Levels"
              value={localFilters.riskLevel || ""}
              onChange={(v) =>
                setLocalFilters({ ...localFilters, riskLevel: v as string })
              }
              options={riskLevels}
              isSearch={false}
            />

            {/* Filter by Date Subheading */}
            <div className="flex flex-col gap-4">
              <h3 className="text-sm font-medium text-(--text-neutral-500)">
                Filter by Date
              </h3>
              <div className="flex flex-col gap-4">
                {/* Start Date */}
                <CustomDatePicker
                  label="Start Date"
                  date={localFilters.startDate}
                  onDateChange={(date) =>
                    setLocalFilters({ ...localFilters, startDate: date })
                  }
                />

                {/* End Date */}
                <CustomDatePicker
                  label="End Date"
                  date={localFilters.endDate}
                  onDateChange={(date) =>
                    setLocalFilters({ ...localFilters, endDate: date })
                  }
                />
              </div>
              {dateError && (
                <span className="text-xs text-red-500 mt-1">{dateError}</span>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="p-6 flex gap-3 justify-end mt-auto">
            <Button
              variant="outline"
              className="rounded-full px-8 py-2.5 h-auto text-(--text-primary-dark) border-(--neutral-100) font-medium hover:bg-(--bg-primary-50) cursor-pointer"
              onClick={handleClear}
            >
              Clear all
            </Button>
            <Button
              className="rounded-full px-8 py-2.5 h-auto bg-[#334155] hover:bg-[#1E293B] text-white font-medium transition-colors cursor-pointer"
              onClick={handleApply}
              disabled={!!dateError}
            >
              Apply filters
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};

export default HipaaFilterSlidebar;
