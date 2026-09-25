import { useMemo, useState } from "react";
import { X } from "lucide-react";
import { Filter as SolarFilter } from "@solar-icons/react-perf/category/ui/Linear/Filter";
import { Button } from "../ui/button";
import { cn } from "../../lib/utils";
import CustomSelect from "../form/CustomSelect";
import CustomDatePicker from "../form/CustomDatePicker";
import { useLocation } from "react-router-dom";
import { Checkbox } from "../ui/checkbox";

export interface SchedulingFilters {
  startDate: Date | null;
  endDate: Date | null;
  status: string | null;
  serviceCode: string | null;
  therapist?: string | null;
  includeHiddenServices?: boolean;
}

interface SchedulingFilterDropdownProps {
  className?: string;
  filters: SchedulingFilters;
  setFilters: (filters: SchedulingFilters) => void;
  statusOptions: { value: string; label: string }[];
  serviceCodeOptions: { value: string; label: string }[];
  therapistOptions?: { value: string; label: string }[];
  onApply?: (f: SchedulingFilters) => void;
  onClear?: (f: SchedulingFilters) => void;
}

const SchedulingFilterDropdown = ({
  className,
  filters,
  setFilters,
  statusOptions,
  serviceCodeOptions,
  therapistOptions,
  onApply,
  onClear,
}: SchedulingFilterDropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [localFilters, setLocalFilters] = useState<SchedulingFilters>(filters);
  const location = useLocation();

  const isAdmin = location.pathname.includes("/admin");

  const error = useMemo(() => {
    if (
      localFilters.startDate &&
      localFilters.endDate &&
      localFilters.startDate > localFilters.endDate
    ) {
      return "Start date cannot be after end date";
    }
    return null;
  }, [localFilters.startDate, localFilters.endDate]);

  const hasActiveFilters = useMemo(
    () =>
      Boolean(
        localFilters.startDate ||
          localFilters.endDate ||
          localFilters.status ||
          localFilters.serviceCode ||
          localFilters.therapist ||
          localFilters.includeHiddenServices,
      ),
    [
      localFilters.endDate,
      localFilters.includeHiddenServices,
      localFilters.serviceCode,
      localFilters.startDate,
      localFilters.status,
      localFilters.therapist,
    ],
  );
  const hasAppliedFilters = useMemo(
    () =>
      Boolean(
        filters.startDate ||
          filters.endDate ||
          filters.status ||
          filters.serviceCode ||
          filters.therapist ||
          filters.includeHiddenServices,
      ),
    [
      filters.endDate,
      filters.includeHiddenServices,
      filters.serviceCode,
      filters.startDate,
      filters.status,
      filters.therapist,
    ],
  );

  const handleApply = () => {
    if (error || !hasActiveFilters) return;
    setFilters(localFilters);
    onApply?.(localFilters);
    setIsOpen(false);
  };

  const handleClear = () => {
    const cleared = {
      startDate: null,
      endDate: null,
      status: null,
      serviceCode: null,
      therapist: null,
      includeHiddenServices: false,
    };
    setLocalFilters(cleared);
    setFilters(cleared);
    onClear?.(cleared);
    setIsOpen(false);
  };

  const toggleOpen = () => {
    if (!isOpen) {
      setLocalFilters(filters);
    }
    setIsOpen(!isOpen);
  };

  return (
    <>
      <button
        type="button"
        onClick={toggleOpen}
        className={cn(
          "relative flex items-center gap-2 rounded-full border border-(--neutral-100) bg-white px-4 py-2 text-sm font-medium shadow-(--shadow) transition-colors duration-300 hover:bg-(--neutral-50) cursor-pointer",
          hasAppliedFilters &&
            "border-(--primary-200) bg-(--bg-primary-50)",
          className,
        )}
      >
        <span className="font-medium text-(--text-neutral-800)">Filters</span>
        <SolarFilter
          size={20}
          color={hasAppliedFilters ? "#1B1C20" : "#5B616E"}
        />
        {hasAppliedFilters && (
          <span className="absolute right-2 top-2 inline-block h-2 w-2 rounded-full bg-(--primary-500)" />
        )}
      </button>

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
              Filters
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
            {isAdmin && (
              <CustomSelect
                label="Therapist"
                placeholder="All Therapists"
                value={localFilters.therapist || ""}
                onChange={(v) =>
                  setLocalFilters({ ...localFilters, therapist: v })
                }
                options={
                  therapistOptions ?? [{ value: "", label: "All Therapists" }]
                }
                isSearch={false}
              />
            )}

            {/* Status Picker */}
            <CustomSelect
              label="Status"
              placeholder="All Statuses"
              value={localFilters.status || ""}
              onChange={(v) => setLocalFilters({ ...localFilters, status: v })}
              options={statusOptions}
              isSearch={false}
            />

            {/* Service Code Picker */}
            <CustomSelect
              label="Service Code"
              placeholder="All Services"
              value={localFilters.serviceCode || ""}
              onChange={(v) =>
                setLocalFilters({ ...localFilters, serviceCode: v })
              }
              options={serviceCodeOptions}
              // isSearch={false}
            />

            <label
              htmlFor="include-hidden-services"
              className="flex items-center justify-between gap-3 rounded-2xl border border-(--neutral-100) px-4 py-3 cursor-pointer"
            >
              <div className="flex flex-col">
                <span className="text-sm font-medium text-(--text-primary-dark)">
                  Include Hidden Services
                </span>
                <span className="text-xs text-(--text-neutral-500)">
                  Show sessions linked to hidden services
                </span>
              </div>
              <Checkbox
                id="include-hidden-services"
                checked={Boolean(localFilters.includeHiddenServices)}
                onCheckedChange={(checked) =>
                  setLocalFilters({
                    ...localFilters,
                    includeHiddenServices: checked,
                  })
                }
              />
            </label>

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
              {error && (
                <span className="text-xs text-red-500 mt-1">{error}</span>
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
              disabled={!!error || !hasActiveFilters}
            >
              Apply filters
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};

export default SchedulingFilterDropdown;
