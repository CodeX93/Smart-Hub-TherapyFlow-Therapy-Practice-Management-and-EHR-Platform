import { useState, useMemo, useEffect, useRef } from "react";
import { X } from "lucide-react";
import { Filter as SolarFilter } from "@solar-icons/react-perf/category/ui/Linear/Filter";
import { cn } from "@/lib/utils";
import CustomSelect, {
  type CustomSelectOption,
} from "@/components/form/CustomSelect";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import { useTaskSystemOptions } from "@/hooks/useSystemOptionCatalog";
import { toSelectOptions } from "@/utils/systemOptions";
import { useLazyGetAdminUsersQuery } from "@/store/api/admin/users.api";
import { Button } from "../ui/button";

export interface AdminTaskFiltersState {
  status: string | null;
  priority: string | null;
  assignee: string | null;
  assigneeName?: string | null;
  startDate: Date | null;
  endDate: Date | null;
}

interface AdminTaskFiltersProps {
  onApplyFilters: (filters: AdminTaskFiltersState) => void;
  appliedFilters?: AdminTaskFiltersState;
  showAssigneeFilter?: boolean;
}

const AdminTaskFilters = ({
  onApplyFilters,
  appliedFilters: externalAppliedFilters,
  showAssigneeFilter = false,
}: AdminTaskFiltersProps) => {
  const [isOpen, setIsOpen] = useState(false);

  // Initial empty state
  const initialFilters: AdminTaskFiltersState = {
    status: null,
    priority: null,
    assignee: null,
    assigneeName: null,
    startDate: null,
    endDate: null,
  };

  const [appliedFilters, setAppliedFilters] =
    useState<AdminTaskFiltersState>(initialFilters);
  const [localFilters, setLocalFilters] =
    useState<AdminTaskFiltersState>(initialFilters);

  const taskOptions = useTaskSystemOptions(isOpen);
  const statusOptions = useMemo<CustomSelectOption[]>(
    () => [
      { value: "", label: "All Statuses" },
      ...toSelectOptions(taskOptions.statusOptions),
    ],
    [taskOptions.statusOptions],
  );
  const priorityOptions = useMemo<CustomSelectOption[]>(
    () => [
      { value: "", label: "All Priority" },
      ...toSelectOptions(taskOptions.priorityOptions),
    ],
    [taskOptions.priorityOptions],
  );

  const [triggerGetUsers] = useLazyGetAdminUsersQuery();
  const [assigneeOptions, setAssigneeOptions] = useState<CustomSelectOption[]>([]);
  const assigneesRequestedRef = useRef(false);

  useEffect(() => {
    if (!showAssigneeFilter || !isOpen || assigneesRequestedRef.current) return;
    assigneesRequestedRef.current = true;
    void (async () => {
      try {
        const options: CustomSelectOption[] = [];
        let page = 1;
        let totalPages = 1;
        do {
          const response = await triggerGetUsers({ page, pageSize: 100 }).unwrap();
          response.items
            .filter((user) =>
              user.roles.some((role) => role === "THERAPIST" || role === "SUPERVISOR"),
            )
            .forEach((user) =>
              options.push({
                value: String(user.id),
                label: user.fullName?.trim() || user.email || user.username,
              }),
            );
          totalPages = response.totalPages || 1;
          page += 1;
        } while (page <= totalPages && page <= 5);
        setAssigneeOptions(options);
      } catch {
        // Hide the assignee filter when the user list is unavailable.
        setAssigneeOptions([]);
      }
    })();
  }, [showAssigneeFilter, isOpen, triggerGetUsers]);

  const [previousAppliedFilters, setPreviousAppliedFilters] = useState(externalAppliedFilters);
  if (externalAppliedFilters && externalAppliedFilters !== previousAppliedFilters) {
    setPreviousAppliedFilters(externalAppliedFilters);
    setAppliedFilters(externalAppliedFilters);
    if (!isOpen) setLocalFilters(externalAppliedFilters);
  }

  // Sync local filters with applied filters when opening
  const toggleOpen = () => {
    if (!isOpen) {
      setLocalFilters(appliedFilters);
    }
    setIsOpen(!isOpen);
  };

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

  const handleApply = () => {
    if (!error) {
      setAppliedFilters(localFilters);
      onApplyFilters(localFilters);
      setIsOpen(false);
    }
  };

  const handleClear = () => {
    setLocalFilters(initialFilters);
    setAppliedFilters(initialFilters);
    onApplyFilters(initialFilters);
    setIsOpen(false);
  };

  return (
    <>
      <div
        onClick={toggleOpen}
        className={cn(
          "flex gap-2 items-center bg-white rounded-full px-4 py-2 text-sm font-medium border border-(--neutral-100) shadow-(--shadow) cursor-pointer hover:bg-(--bg-primary-50) transition-colors duration-300",
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
            {/* Status Dropdown */}
            <CustomSelect
              label="Status"
              placeholder="All Statuses"
              value={localFilters.status || ""}
              onChange={(v) =>
                setLocalFilters({ ...localFilters, status: v || null })
              }
              options={statusOptions}
              isSearch={false}
            />

            {/* Priority Dropdown */}
            <CustomSelect
              label="Priority"
              placeholder="All Priority"
              value={localFilters.priority || ""}
              onChange={(v) =>
                setLocalFilters({ ...localFilters, priority: v || null })
              }
              options={priorityOptions}
              isSearch={false}
            />

            {/* Assignee Dropdown */}
            {showAssigneeFilter && assigneeOptions.length > 0 && (
              <CustomSelect
                label="Assignee"
                placeholder="All Assignees"
                value={localFilters.assignee || ""}
                onChange={(v) =>
                  setLocalFilters({
                    ...localFilters,
                    assignee: v || null,
                    assigneeName: v
                      ? (assigneeOptions.find((option) => option.value === v)
                          ?.label ?? null)
                      : null,
                  })
                }
                options={[
                  { value: "", label: "All Assignees" },
                  ...assigneeOptions,
                ]}
                isSearch
              />
            )}

            {/* Due Date Section */}
            <div className="flex flex-col gap-4">
              <h3 className="text-sm font-medium text-(--text-neutral-500)">
                Due Date
              </h3>
              <div className="flex flex-col gap-4">
                <CustomDatePicker
                  label="From"
                  date={localFilters.startDate}
                  onDateChange={(date) =>
                    setLocalFilters({ ...localFilters, startDate: date })
                  }
                />
                <CustomDatePicker
                  label="To"
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
              disabled={!!error}
            >
              Apply filters
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};

export default AdminTaskFilters;
