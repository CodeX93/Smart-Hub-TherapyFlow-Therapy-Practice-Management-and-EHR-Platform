import { useAnimatedPresence } from "@/hooks/useAnimatedPresence";
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useMemo } from "react";
import { X } from "lucide-react";
import type { ClientFilters } from "@/types/client.type";
import CustomSelect, {
  type CustomSelectOption,
} from "../../form/CustomSelect";
import { Button } from "../../ui/button";
import { Checkbox } from "../../ui/checkbox";
import { cn } from "@/lib/utils";
import { useClientFilterOptions } from "@/hooks/useSystemOptionCatalog";
import { useGetChecklistTemplatesQuery } from "@/store/api/admin/checklists.api";
import { useGetReportTemplatesQuery } from "@/store/api/admin/reportTemplates.api";

const QUICK_FILTERS: Array<{
  id: string;
  label: string;
  key: keyof ClientFilters;
}> = [
  {
    id: "has-portal-access",
    label: "Has portal access",
    key: "hasPortalAccess",
  },
  {
    id: "has-pending-tasks",
    label: "Has pending tasks",
    key: "hasPendingTasks",
  },
  {
    id: "has-no-sessions",
    label: "Has no sessions",
    key: "hasNoSessions",
  },
  {
    id: "needs-follow-up",
    label: "Needs follow-up",
    key: "needsFollowUp",
  },
  {
    id: "unassigned",
    label: "Unassigned",
    key: "unassigned",
  },
];

interface ClientsFilterSidebarProps {
  isOpen: boolean;
  onClose: () => void;
  filters: ClientFilters;
  onFiltersChange: (filters: ClientFilters) => void;
  onApply: () => void;
  onClearAll: () => void;
  therapistOptions: CustomSelectOption[];
  checklistTemplateOptions?: CustomSelectOption[];
  reportTemplateOptions?: CustomSelectOption[];
}

const ClientsFilterSidebar = ({
  isOpen,
  onClose,
  filters,
  onFiltersChange,
  onApply,
  onClearAll,
  therapistOptions,
  checklistTemplateOptions: checklistTemplateOptionsProp,
  reportTemplateOptions: reportTemplateOptionsProp,
}: ClientsFilterSidebarProps) => {
  const isVisible = useAnimatedPresence(isOpen);
  const {
    isLoading,
    statusOptions,
    stageOptions,
    clientTypeOptions,
  } = useClientFilterOptions(isOpen);

  const shouldLoadChecklistTemplates =
    isOpen && !checklistTemplateOptionsProp;
  const shouldLoadReportTemplates = isOpen && !reportTemplateOptionsProp;

  const { data: checklistTemplatesResponse, isLoading: isLoadingChecklists } =
    useGetChecklistTemplatesQuery(
      { page: 1, pageSize: 200 },
      { skip: !shouldLoadChecklistTemplates },
    );

  const { data: reportTemplates, isLoading: isLoadingReportTemplates } =
    useGetReportTemplatesQuery(undefined, {
      skip: !shouldLoadReportTemplates,
    });

  const checklistTemplateOptions = useMemo(() => {
    if (checklistTemplateOptionsProp) return checklistTemplateOptionsProp;
    const items = checklistTemplatesResponse?.items ?? [];
    return [
      { label: "All Checklists", value: "all" },
      ...items
        .filter((item) => item.isActive !== false)
        .map((item) => ({
          label: item.name,
          value: String(item.id),
        })),
    ];
  }, [checklistTemplateOptionsProp, checklistTemplatesResponse?.items]);

  const reportTemplateOptions = useMemo(() => {
    if (reportTemplateOptionsProp) return reportTemplateOptionsProp;
    const items = reportTemplates ?? [];
    return [
      { label: "All Report Templates", value: "all" },
      ...items
        .filter((item) => item.isActive !== false)
        .map((item) => ({
          label: item.name,
          value: String(item.id),
        })),
    ];
  }, [reportTemplateOptionsProp, reportTemplates]);

  const isOptionsLoading =
    isLoading ||
    (shouldLoadChecklistTemplates && isLoadingChecklists) ||
    (shouldLoadReportTemplates && isLoadingReportTemplates);



  if (!isOpen && !isVisible) return null;

  const getSingleValue = (key: keyof ClientFilters): string => {
    const value = filters[key];
    return Array.isArray(value) && value.length > 0 ? value[0] : "all";
  };

  const setSingleValue = (key: keyof ClientFilters, value: string) => {
    onFiltersChange({
      ...filters,
      [key]: value === "all" ? [] : [value],
    });
  };

  const handleBooleanFilterChange = (
    key: keyof ClientFilters,
    checked: boolean,
  ) => {
    onFiltersChange({ ...filters, [key]: checked });
  };

  const therapistSelectOptions = [
    { label: "All Therapists", value: "all" },
    ...therapistOptions,
  ];

  return (
    <div className="fixed inset-0 z-60 flex justify-end isolate">
      <div
        className={cn(
          "fixed inset-0 bg-black/40 transition-opacity duration-300",
          isOpen ? "opacity-100" : "opacity-0",
        )}
        onClick={onClose}
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
            onClick={onClose}
            className="cursor-pointer rounded-full p-2 text-(--text-neutral-400) transition-colors hover:bg-(--neutral-50) hover:text-(--text-neutral-600)"
          >
            <X className="size-5" />
          </button>
        </div>

        <div className="flex-1 space-y-6 overflow-y-auto px-6 py-6">
          {isOptionsLoading ? (
            <ContentLoader className="py-12" />
          ) : (
            <>
              <CustomSelect
                label="Status"
                placeholder="All Statuses"
                value={getSingleValue("clientStatus")}
                options={statusOptions}
                onChange={(value) => setSingleValue("clientStatus", value)}
              />

              <CustomSelect
                label="Stage"
                placeholder="All Stages"
                value={getSingleValue("clientStage")}
                options={stageOptions}
                onChange={(value) => setSingleValue("clientStage", value)}
              />

              <CustomSelect
                label="Assigned Therapist"
                placeholder="All Therapists"
                value={getSingleValue("assignedTherapist")}
                options={therapistSelectOptions}
                onChange={(value) => setSingleValue("assignedTherapist", value)}
                isSearch={true}
              />

              <CustomSelect
                label="Client Type"
                placeholder="All Types"
                value={getSingleValue("clientType")}
                options={clientTypeOptions}
                onChange={(value) => setSingleValue("clientType", value)}
              />

              <CustomSelect
                label="Checklist"
                placeholder="All Checklists"
                value={getSingleValue("checklistTemplate")}
                options={checklistTemplateOptions}
                onChange={(value) => setSingleValue("checklistTemplate", value)}
                isSearch={true}
              />

              <CustomSelect
                label="Report Template"
                placeholder="All Report Templates"
                value={getSingleValue("reportTemplate")}
                options={reportTemplateOptions}
                onChange={(value) => setSingleValue("reportTemplate", value)}
                isSearch={true}
              />
            </>
          )}

          <div className="flex flex-col gap-3">
            <h3 className="font-semibold text-(--text-secondary-dark)">
              Quick Filters
            </h3>
            {QUICK_FILTERS.map((filter) => (
              <div key={filter.id} className="flex items-center gap-3">
                <Checkbox
                  id={filter.id}
                  checked={Boolean(filters[filter.key])}
                  onCheckedChange={(checked) =>
                    handleBooleanFilterChange(filter.key, Boolean(checked))
                  }
                />
                <label
                  htmlFor={filter.id}
                  className="flex-1 cursor-pointer select-none text-sm font-medium text-(--text-primary-dark)"
                >
                  {filter.label}
                </label>
              </div>
            ))}
          </div>
        </div>

        <div className="flex shrink-0 justify-end gap-3 bg-white p-6">
          <Button
            onClick={onClearAll}
            variant="outline"
            className="h-11 cursor-pointer rounded-full border-(--neutral-200) bg-white text-sm font-semibold text-(--text-neutral-800) hover:bg-(--neutral-50)"
          >
            Clear all
          </Button>
          <Button
            onClick={onApply}
            disabled={isOptionsLoading}
            className="h-11 cursor-pointer rounded-full bg-(--bg-primary-dark) text-sm font-semibold text-white shadow-sm hover:bg-(--bg-primary-dark)/90"
          >
            Apply filters
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ClientsFilterSidebar;
