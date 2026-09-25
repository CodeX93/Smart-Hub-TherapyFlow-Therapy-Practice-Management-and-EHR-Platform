
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useEffect } from "react";
import { X } from "lucide-react";
import type { ClientFilters } from "@/types/client.type";
import CustomSelect from "../form/CustomSelect";
import { Button } from "../ui/button";
import { Checkbox } from "../ui/checkbox";
import { cn } from "@/lib/utils";
import {
  QUICK_FILTERS,
  CHECKLIST_ITEMS,
  templateOptions,
} from "@/pages/therapist/therapist.static";
import { useClientFilterOptions } from "@/hooks/useSystemOptionCatalog";

interface ClientsFilterSidebarProps {
  isOpen: boolean;
  onClose: () => void;
  filters: ClientFilters;
  onFiltersChange: (filters: ClientFilters) => void;
  onApply: () => void;
  onClearAll: () => void;
}

const ClientsFilterDropdown = ({
  isOpen,
  onClose,
  filters,
  onFiltersChange,
  onApply,
  onClearAll,
}: ClientsFilterSidebarProps) => {
  const [isVisible, setIsVisible] = useState(isOpen);
  const {
    isLoading,
    statusOptions,
    stageOptions,
    clientTypeOptions,
  } = useClientFilterOptions(isOpen);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      const timer = setTimeout(() => setIsVisible(false), 300); // Wait for slide out
      document.body.style.overflow = "unset";
      return () => clearTimeout(timer);
    }
    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  if (!isOpen && !isVisible) return null;

  // Ensure isVisible matches isOpen immediately for rendering
  if (isOpen && !isVisible) {
    setIsVisible(true);
  }

  // Helpers to bridge Single Select UI with potential Multi-Select Logic
  const getSingleValue = (key: keyof ClientFilters): string => {
    const val = filters[key];
    // If array is defined and has items, return the first one.
    // If it's empty or undefined, return "all" to match the "All X" option.
    if (Array.isArray(val) && val.length > 0) {
      return val[0];
    }
    return "all";
  };

  const setSingleValue = (key: keyof ClientFilters, value: string) => {
    // If "All" is selected, clear the array
    if (value === "all") {
      onFiltersChange({ ...filters, [key]: [] });
    } else {
      // Otherwise set as single item array
      onFiltersChange({ ...filters, [key]: [value] });
    }
  };

  // Handler for boolean filter checkboxes
  const handleBooleanFilterChange = (
    key: keyof ClientFilters,
    checked: boolean,
  ) => {
    onFiltersChange({ ...filters, [key]: checked });
  };

  return (
    <div className="fixed inset-0 z-60 flex justify-end isolate">
      {/* Backdrop */}
      <div
        className={cn(
          "fixed inset-0 bg-black/40  transition-opacity duration-300",
          isOpen ? "opacity-100" : "opacity-0",
        )}
        onClick={onClose}
      />

      {/* Sidebar Panel */}
      <div
        className={cn(
          "relative w-full max-w-125 bg-white h-full shadow-2xl transition-transform duration-300 ease-in-out flex flex-col",
          isOpen ? "translate-x-0" : "translate-x-full",
        )}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-(--neutral-100) shrink-0">
          <h2 className="text-xl font-semibold text-(--text-primary-dark)">
            Filters
          </h2>
          <button
            onClick={onClose}
            className="p-2 -mr-2 text-(--text-neutral-400) hover:text-(--text-neutral-600) hover:bg-(--neutral-50) rounded-full transition-colors cursor-pointer"
          >
            <X className="size-5" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
          {isLoading ? (
            <ContentLoader className="py-12" />
          ) : (
            <>
              <CustomSelect
                label="Filter Clients"
                placeholder="All Clients"
                value={getSingleValue("clientStatus")}
                options={statusOptions}
                onChange={(val) => setSingleValue("clientStatus", val)}
                className="bg-(--bg-primary-light)"
              />

              <CustomSelect
                label="Client Stage"
                placeholder="All Stages"
                value={getSingleValue("clientStage")}
                options={stageOptions}
                onChange={(val) => setSingleValue("clientStage", val)}
              />

              <CustomSelect
                label="Client Type"
                placeholder="All Types"
                value={getSingleValue("clientType")}
                options={clientTypeOptions}
                onChange={(val) => setSingleValue("clientType", val)}
              />
            </>
          )}

          {/* Checklist Templates */}
          <CustomSelect
            label="Checklist Templates"
            placeholder="All Templates"
            value={getSingleValue("checklistTemplate")}
            options={templateOptions}
            onChange={(val) => setSingleValue("checklistTemplate", val)}
          />

          {/* Show Quick Filters when "All Templates" is selected, otherwise show Checklist Items */}
          {getSingleValue("checklistTemplate") === "all" ? (
            // Quick Filters
            <div className="flex flex-col gap-2">
              <h1 className="text-(--text-secondary-dark) font-semibold">
                Quick Filters
              </h1>
              {QUICK_FILTERS.map((filter) => (
                <div key={filter.id} className="flex items-center gap-3">
                  <Checkbox
                    id={filter.id}
                    checked={(filters[filter.key] as boolean) || false}
                    onCheckedChange={(checked) =>
                      handleBooleanFilterChange(filter.key, !!checked)
                    }
                  />
                  <label
                    htmlFor={filter.id}
                    className="cursor-pointer font-medium text-sm text-(--text-primary-dark) select-none flex-1"
                  >
                    {filter.label}
                  </label>
                </div>
              ))}
            </div>
          ) : (
            // Checklist Items
            <div>
              <label className="text-sm font-medium text-(--text-secondary-light)">
                Checklist Items (Select multiple - OR logic)
              </label>
              <div className="space-y-3 border border-(--neutral-100) rounded-xl p-4 bg-(--neutral-50) mt-2">
                {CHECKLIST_ITEMS.map((item) => (
                  <div key={item.id} className="flex items-center gap-3">
                    <Checkbox id={`checklist-${item.id}`} />
                    <label
                      htmlFor={`checklist-${item.id}`}
                      className="flex-1 cursor-pointer text-sm text-(--text-primary-dark) font-medium select-none"
                    >
                      {item.label}
                    </label>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-6 flex gap-3 bg-white shrink-0 justify-end">
          <Button
            onClick={onClearAll}
            variant="outline"
            className=" h-11 border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-800) rounded-full cursor-pointer text-sm font-semibold"
          >
            Clear all
          </Button>
          <Button
            onClick={onApply}
            className=" h-11 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full cursor-pointer text-sm font-semibold shadow-sm"
          >
            Apply filters
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ClientsFilterDropdown;
