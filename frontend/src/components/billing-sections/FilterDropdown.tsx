import { useAnimatedPresence } from "@/hooks/useAnimatedPresence";
import { useMemo, useState } from "react";
import { X } from "lucide-react";
import { Filter } from "@solar-icons/react-perf/category/ui/Linear/Filter";
import { Button } from "../ui/button";
import { cn } from "../../lib/utils";
import CustomSelect from "../form/CustomSelect";
import CustomDatePicker from "../form/CustomDatePicker";
import type { BillingFilters } from "@/types/billing.type";
import { BILLING_FILTERS } from "@/pages/therapist/therapist.static";
import { createEmptyBillingFilters } from "@/utils/billingFilters";

interface FilterDropdownProps {
  className?: string;
  filters: BillingFilters;
  setFilters: (filters: BillingFilters) => void;
}

const FilterDropdown = ({
  className,
  filters,
  setFilters,
}: FilterDropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const isVisible = useAnimatedPresence(isOpen);
  const [localFilters, setLocalFilters] = useState<BillingFilters>(filters);
  const {
    billingStatusOptions,
    paymentStatusOptions,
    paymentMethodOptions,
    clientTypeFilterOptions,
    sessionTypeFilterOptions,
  } = BILLING_FILTERS;



  const error = useMemo(() => {
    if (
      localFilters.startDate &&
      localFilters.endDate &&
      localFilters.startDate > localFilters.endDate
    ) {
      return "Start date cannot be after end date";
    }
    return null;
  }, [
    localFilters.endDate,
    localFilters.startDate,
  ]);

  const handleApply = () => {
    if (error) return;
    setFilters(localFilters);
    setIsOpen(false);
  };

  const handleClear = () => {
    const cleared = createEmptyBillingFilters();
    setLocalFilters(cleared);
    setFilters(cleared);
    setIsOpen(false);
  };

  const openFilters = () => {
    setLocalFilters(filters);
    setIsOpen(true);
  };

  return (
    <>
      <button
        type="button"
        onClick={openFilters}
        className={cn(
          "flex h-10 cursor-pointer items-center gap-2 rounded-full border border-(--neutral-100) bg-white px-4 text-sm font-normal text-[#0A0A0A] shadow-(--shadow) transition-colors hover:bg-(--neutral-50)",
          className,
        )}
      >
        Filters
        <Filter size={20} color="#1B1C20" />
      </button>

      {isOpen || isVisible ? (
        <div className="fixed inset-0 z-60 flex justify-end isolate">
          <div
            className={cn(
              "fixed inset-0 bg-black/40 transition-opacity duration-300",
              isOpen ? "opacity-100" : "opacity-0",
            )}
            onClick={() => setIsOpen(false)}
          />

          <div
            className={cn(
              "relative flex h-full w-full max-w-125 flex-col bg-white shadow-2xl transition-transform duration-300 ease-in-out",
              isOpen ? "translate-x-0" : "translate-x-full",
            )}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex shrink-0 items-center justify-between border-b border-(--neutral-100) px-6 py-5">
              <h2 className="text-xl font-semibold text-(--text-primary-dark)">
                Filters
              </h2>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="cursor-pointer rounded-full p-2 text-(--text-neutral-400) transition-colors hover:bg-(--neutral-50) hover:text-(--text-neutral-600)"
                aria-label="Close filters"
              >
                <X className="size-5" />
              </button>
            </div>

            <div className="flex-1 space-y-6 overflow-y-auto px-6 py-6">
              <CustomSelect
                label="Billing Status"
                placeholder="All Billing Stages"
                value={localFilters.billingStatus || ""}
                onChange={(value) =>
                  setLocalFilters({ ...localFilters, billingStatus: value || null })
                }
                options={billingStatusOptions}
                isSearch={false}
              />

              <CustomSelect
                label="Payment Status"
                placeholder="All Payment Stages"
                value={localFilters.paymentStatus || ""}
                onChange={(value) =>
                  setLocalFilters({ ...localFilters, paymentStatus: value || null })
                }
                options={paymentStatusOptions}
                isSearch={false}
              />

              <CustomSelect
                label="Payment Method"
                placeholder="All Methods"
                value={localFilters.paymentMethod || ""}
                onChange={(value) =>
                  setLocalFilters({ ...localFilters, paymentMethod: value || null })
                }
                options={paymentMethodOptions}
                isSearch={false}
              />

              <CustomSelect
                label="Client Type"
                placeholder="All Client Types"
                value={localFilters.clientType || ""}
                onChange={(value) =>
                  setLocalFilters({ ...localFilters, clientType: value || null })
                }
                options={clientTypeFilterOptions}
                isSearch={false}
              />

              <CustomSelect
                label="Session Type"
                placeholder="All Session Types"
                value={localFilters.sessionType || ""}
                onChange={(value) =>
                  setLocalFilters({ ...localFilters, sessionType: value || null })
                }
                options={sessionTypeFilterOptions}
                isSearch={false}
              />

              <div className="flex flex-col gap-4">
                <h3 className="font-semibold text-(--text-secondary-dark)">
                  Filter by Date
                </h3>
                <CustomDatePicker
                  label="Start Date"
                  date={localFilters.startDate}
                  onDateChange={(date) =>
                    setLocalFilters({ ...localFilters, startDate: date })
                  }
                />
                <CustomDatePicker
                  label="End Date"
                  date={localFilters.endDate}
                  onDateChange={(date) =>
                    setLocalFilters({ ...localFilters, endDate: date })
                  }
                />
                {error ? (
                  <span className="text-xs text-red-500">{error}</span>
                ) : null}
              </div>
            </div>

            <div className="flex shrink-0 justify-end gap-3 bg-white p-6">
              <Button
                type="button"
                onClick={handleClear}
                variant="outline"
                className="h-11 cursor-pointer rounded-full border-(--neutral-200) bg-white text-sm font-semibold text-(--text-neutral-800) hover:bg-(--neutral-50)"
              >
                Clear all
              </Button>
              <Button
                type="button"
                onClick={handleApply}
                disabled={Boolean(error)}
                className="h-11 cursor-pointer rounded-full bg-(--bg-primary-dark) text-sm font-semibold text-white shadow-sm hover:bg-(--bg-primary-dark)/90"
              >
                Apply filters
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
};

export default FilterDropdown;
