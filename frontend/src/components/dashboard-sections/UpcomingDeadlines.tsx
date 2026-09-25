import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { DangerTriangle } from "@solar-icons/react-perf/category/ui/Linear/DangerTriangle";
import { ClockCircle } from "@solar-icons/react-perf/category/time/Linear/ClockCircle";
import { Eye } from "@solar-icons/react-perf/category/security/Linear/Eye";
import { Pen2 } from "@solar-icons/react-perf/category/messages/Linear/Pen2";
import type { DeadlineItem } from "@/types/therapist-dashboard.type";
import { Button } from "../ui/button";
import ActionDropdown from "@/components/shared/ActionDropdown";
import {
  getTaskPriorityBadgeClass,
  getTaskStatusBadgeClass,
  isTaskStatusCompleted,
} from "@/utils/taskOptionPresentation";
import { useTaskOptionPresentation } from "@/hooks/useSystemOptionCatalog";

interface UpcomingDeadlinesProps {
  deadlines: DeadlineItem[];
  onViewAll?: () => void;
  onTaskAction?: (action: "view" | "edit" | "delete", taskId: string) => void;
}

const UpcomingDeadlines = ({
  deadlines,
  onViewAll,
  onTaskAction,
}: UpcomingDeadlinesProps) => {
  const { priorityLabel, statusLabel } = useTaskOptionPresentation();

  return (
    <div className="bg-white rounded-lg border border-(--neutral-100) shadow-(--shadow) flex flex-col">
      <div className="flex items-center gap-2 text-(--text-primary-dark) text-lg font-semibold mb-4 px-4 pt-4">
        <DangerTriangle className="h-5 w-5" />
        Upcoming Deadlines
      </div>
      <div className="flex-1 flex flex-col gap-1 overflow-y-auto px-7 max-h-[25rem] custom-scrollbar">
        {deadlines && deadlines.length > 0 ? (
          deadlines.map((deadline: DeadlineItem, index: number) => (
            <div
              key={deadline.id}
              className={`py-4 flex flex-col gap-1 ${
                index !== deadlines.length - 1
                  ? "border-b border-(--neutral-100)"
                  : ""
              }`}
            >
              <div className="flex justify-between items-start gap-3">
                <div className="flex flex-col gap-1 min-w-0 flex-1">
                  <div className="flex items-center gap-2 min-w-0">
                    <h3 className="text-(--text-primary-dark) font-semibold text-base truncate">
                      {deadline.title}
                    </h3>
                    <span
                      className={`shrink-0 rounded-full px-3 py-1 text-xs font-medium capitalize ${getTaskStatusBadgeClass(
                        deadline.status,
                      )}`}
                    >
                      {statusLabel(deadline.status)}
                    </span>
                    <span
                      className={`shrink-0 rounded-full px-3 py-1 text-xs font-medium capitalize ${getTaskPriorityBadgeClass(
                        deadline.priority
                      )}`}
                    >
                      {priorityLabel(deadline.priority)}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 text-(--text-neutral-400) text-sm">
                    <div className="flex items-center gap-1">
                      <ClockCircle className="h-3.5 w-3.5" />
                      <span>{deadline.date}</span>
                    </div>
                    <span className="text-(--neutral-100)">|</span>
                    <span className="truncate">Client: {deadline.clientName}</span>
                  </div>
                </div>
                <ActionDropdown
                  align="end"
                  trigger={
                    <Button variant="ghost" size="icon" className="h-8 w-8 cursor-pointer">
                      <MenuDotsIcon size={20} />
                    </Button>
                  }
                  actions={[
                    {
                      label: "Task Details",
                      icon: <Eye size={16} />,
                      onClick: () => onTaskAction?.("view", deadline.id),
                    },
                    {
                      label: "Edit Task",
                      icon: <Pen2 size={16} />,
                      onClick: () => onTaskAction?.("edit", deadline.id),
                      disabled: isTaskStatusCompleted(deadline.status),
                    },
                    {
                      label: "Delete",
                      icon: <TrashIcon size={16} className="text-red-500" />,
                      onClick: () => onTaskAction?.("delete", deadline.id),
                    },
                  ]}
                />
              </div>
            </div>
          ))
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center gap-2 py-10">
            <img
              src="/assets/deadline.png"
              alt="empty-deadlines"
              className="w-30 h-17"
            />
            <h1 className="text-(--text-primary-dark) text-xl font-semibold">
              Upcoming Deadlines
            </h1>
            <p className="text-(--text-neutral-600)">
              Great! There's no deadline yet.
            </p>
          </div>
        )}
      </div>

      {deadlines && deadlines.length > 0 && (
        <div className="mt-auto py-4 text-center border-t border-(--neutral-100)">
          <Button
            variant="link"
            className="text-(--text-primary-500) font-medium text-sm cursor-pointer hover:underline h-auto p-0"
            onClick={onViewAll}
          >
            View all
          </Button>
        </div>
      )}
    </div>
  );
};

export default UpcomingDeadlines;
