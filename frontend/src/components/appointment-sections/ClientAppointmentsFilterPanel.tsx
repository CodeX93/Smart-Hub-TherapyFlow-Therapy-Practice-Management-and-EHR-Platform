import { useMemo, useState } from "react";
import { X } from "lucide-react";
import { Filter as SolarFilter } from "@solar-icons/react-perf/category/ui/Linear/Filter";
import { Button } from "../ui/button";
import { cn } from "@/lib/utils";
import CustomSelect from "../form/CustomSelect";
import CustomDatePicker from "../form/CustomDatePicker";
import {
  CLIENT_APPOINTMENT_SESSION_MODE_OPTIONS,
  CLIENT_APPOINTMENT_STATUS_OPTIONS,
  DEFAULT_CLIENT_APPOINTMENT_FILTERS,
  hasActiveClientAppointmentFilters,
  type ClientAppointmentFilters,
} from "@/utils/clientAppointmentFilters";

interface ClientAppointmentsFilterPanelProps {
  filters: ClientAppointmentFilters;
  setFilters: (filters: ClientAppointmentFilters) => void;
  serviceOptions: { value: string; label: string }[];
}

const ClientAppointmentsFilterPanel = ({
  filters,
  setFilters,
  serviceOptions,
}: ClientAppointmentsFilterPanelProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [localFilters, setLocalFilters] =
    useState<ClientAppointmentFilters>(filters);

  const error = useMemo(() => {
    if (
      localFilters.startDate &&
      localFilters.endDate &&
      localFilters.startDate > localFilters.endDate
    ) {
      return "Start date cannot be after end date";
    }

    return null;
  }, [localFilters.endDate, localFilters.startDate]);

  const hasActiveFilters = hasActiveClientAppointmentFilters(filters);

  const toggleOpen = () => {
    if (!isOpen) {
      setLocalFilters(filters);
    }
    setIsOpen((open) => !open);
  };

  const handleApply = () => {
    if (error) return;

    setFilters(localFilters);
    setIsOpen(false);
  };

  const handleClear = () => {
    setLocalFilters(DEFAULT_CLIENT_APPOINTMENT_FILTERS);
    setFilters(DEFAULT_CLIENT_APPOINTMENT_FILTERS);
    setIsOpen(false);
  };

  return (
    <>
      <button
        type="button"
        onClick={toggleOpen}
        aria-label="Open appointment filters"
        className={cn(
          "relative rounded-lg border bg-white p-2 shadow-xs shadow-(--shadow) transition-colors cursor-pointer",
          hasActiveFilters
            ? "border-(--text-primary-500) bg-(--bg-primary-light)"
            : "border-(--neutral-100) hover:bg-(--bg-primary-light)",
        )}
      >
        <SolarFilter size={20} color="#1B1C20" />
        {hasActiveFilters ? (
          <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-(--text-primary-500)" />
        ) : null}
      </button>

      <div
        className={cn(
          "fixed inset-0 z-50 flex justify-end transition-all duration-300",
          isOpen ? "visible" : "invisible",
        )}
      >
        <div
          className={cn(
            "fixed inset-0 bg-black/40 transition-opacity duration-300",
            isOpen ? "opacity-100" : "opacity-0",
          )}
          onClick={() => setIsOpen(false)}
        />

        <div
          className={cn(
            "relative flex h-full w-full max-w-135 flex-col bg-white shadow-2xl transition-transform duration-300",
            isOpen ? "translate-x-0" : "translate-x-full",
          )}
        >
          <div className="flex items-center justify-between border-b border-(--neutral-100) px-6 py-5">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Filters
            </h2>
            <button
              type="button"
              onClick={() => setIsOpen(false)}
              className="cursor-pointer text-(--text-neutral-600) transition-colors hover:text-(--text-primary-dark)"
              aria-label="Close filters"
            >
              <X className="size-6" />
            </button>
          </div>

          <div className="flex flex-1 flex-col gap-6 overflow-y-auto p-4">
            <CustomSelect
              label="Status"
              placeholder="All Statuses"
              value={localFilters.status || ""}
              onChange={(value) =>
                setLocalFilters({
                  ...localFilters,
                  status: value || null,
                })
              }
              options={CLIENT_APPOINTMENT_STATUS_OPTIONS}
              isSearch={false}
            />

            <CustomSelect
              label="Service"
              placeholder="All Services"
              value={localFilters.serviceId || ""}
              onChange={(value) =>
                setLocalFilters({
                  ...localFilters,
                  serviceId: value || null,
                })
              }
              options={serviceOptions}
              isSearch
            />

            <CustomSelect
              label="Session Type"
              placeholder="All Types"
              value={localFilters.sessionMode || ""}
              onChange={(value) =>
                setLocalFilters({
                  ...localFilters,
                  sessionMode: value || null,
                })
              }
              options={CLIENT_APPOINTMENT_SESSION_MODE_OPTIONS}
              isSearch={false}
            />

            <div className="flex flex-col gap-4">
              <h3 className="text-sm font-medium text-(--text-neutral-500)">
                Filter by Date
              </h3>
              <div className="flex flex-col gap-4">
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
              </div>
              {error ? (
                <span className="mt-1 text-xs text-red-500">{error}</span>
              ) : null}
            </div>
          </div>

          <div className="mt-auto flex justify-end gap-3 p-6">
            <Button
              type="button"
              variant="outline"
              className="h-auto cursor-pointer rounded-full border-(--neutral-100) px-8 py-2.5 font-medium text-(--text-primary-dark) hover:bg-(--bg-primary-50)"
              onClick={handleClear}
            >
              Clear all
            </Button>
            <Button
              type="button"
              className="h-auto cursor-pointer rounded-full bg-[#334155] px-8 py-2.5 font-medium text-white transition-colors hover:bg-[#1E293B]"
              onClick={handleApply}
              disabled={Boolean(error)}
            >
              Apply filters
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};

export default ClientAppointmentsFilterPanel;
