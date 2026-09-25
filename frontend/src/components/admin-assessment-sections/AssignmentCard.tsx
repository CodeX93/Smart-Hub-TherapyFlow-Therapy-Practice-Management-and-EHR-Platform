
import { CalendarIcon } from "@/components/icons/commonIcons";
import type { RefObject } from "react";
import type { ActiveAssignment } from "../../pages/admin/content/content.static";
import { ASSESSMENT_STATUS_OPTIONS } from "../../pages/admin/content/content.static";
import CustomSelect from "../form/CustomSelect";

interface AssignmentCardProps {
  assignment: ActiveAssignment;
  onStatusChange?: (assignmentId: string, status: ActiveAssignment["status"]) => void;
  isUpdating?: boolean;
  scrollContainerRef?: RefObject<HTMLElement | null>;
}

const AssignmentCard = ({
  assignment,
  onStatusChange,
  isUpdating = false,
  scrollContainerRef,
}: AssignmentCardProps) => {
  return (
    <div className="flex min-w-0 flex-col gap-4 rounded-xl border border-(--neutral-100) bg-white p-4 shadow-sm transition-shadow duration-300 hover:shadow-xl">
      <div className="flex min-w-0 items-start justify-between gap-3">
        <h3
          className="text-base font-bold text-(--neutral-950) leading-tight max-w-[70%] border-b border-(--neutral-950) cursor-pointer hover:opacity-80 transition-opacity truncate"
          title={assignment.title}
        >
          {assignment.title}
        </h3>
        <div className="w-32">
          <CustomSelect
            options={ASSESSMENT_STATUS_OPTIONS}
            value={assignment.status}
            onChange={(val) =>
              onStatusChange?.(assignment.id, val as ActiveAssignment["status"])
            }
            disabled={isUpdating}
            closeOnScroll
            scrollContainerRefs={scrollContainerRef ? [scrollContainerRef] : undefined}
            className={`rounded-full max-h-9 pb-0 pt-0 bg-white shadow-sm
             ${
               assignment.status === "Completed"
                 ? "bg-(--status-completed-light) text-(--status-completed-dark)"
                 : assignment.status === "Pending"
                   ? "bg-(--neutral-100) text-(--text-neutral-600)"
                   : "bg-(--light-blue) text-(--status-billed)"
             }`}
            isSearch={false}
          />
        </div>
      </div>

      <div className="min-w-0 space-y-3">
        <div className="flex min-w-0 items-start justify-between gap-3 text-xs">
          <span className="shrink-0 text-(--text-neutral-600)">Assigned to</span>
          <div className="min-w-0 text-right">
            <p
              className="truncate font-medium text-(--neutral-950)"
              title={assignment.assignedTo.name}
            >
              {assignment.assignedTo.name}
            </p>
            <p
              className="truncate text-(--text-neutral-950)"
              title={assignment.assignedTo.id}
            >
              ({assignment.assignedTo.id})
            </p>
          </div>
        </div>
        <div className="flex min-w-0 items-start justify-between gap-3 text-xs">
          <span className="shrink-0 text-(--text-neutral-600)">Assigned by</span>
          <span
            className="min-w-0 truncate text-right font-medium text-(--neutral-950)"
            title={assignment.assignedBy}
          >
            {assignment.assignedBy}
          </span>
        </div>
        <div className="flex min-w-0 items-center justify-between gap-3 text-xs">
          <span className="shrink-0 text-(--text-neutral-600)">Due date</span>
          <div className="flex min-w-0 items-center justify-end gap-1.5 font-medium text-(--neutral-950)">
            <CalendarIcon size={14} className="shrink-0 text-(--text-neutral-400)" />
            <span className="truncate" title={assignment.dueDate}>
              {assignment.dueDate}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AssignmentCard;
