import { CalendarIcon, MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import type { Task } from "@/pages/therapist/tasks/tasks.static";
import { ChatRoundDots } from "@solar-icons/react-perf/category/messages/Linear/ChatRoundDots";
import { Pen2 } from "@solar-icons/react-perf/category/messages/Linear/Pen2";
import { Eye } from "@solar-icons/react-perf/category/security/Linear/Eye";
import { Badge } from "../ui/badge";
import ActionDropdown from "../shared/ActionDropdown";
import { Button } from "../ui/button";
import { useTaskOptionPresentation } from "@/hooks/useSystemOptionCatalog";
import {
  getTaskPriorityBadgeClass,
  getTaskStatusBadgeClass,
  isTaskStatusCompleted,
} from "@/utils/taskOptionPresentation";

interface TaskCardProps {
  task: Task;
  onDelete: (task: Task) => void;
  onEdit: (task: Task) => void;
  onViewDetails: (task: Task) => void;
  onViewComments: (task: Task) => void;
}

const TaskCard = ({
  task,
  onDelete,
  onEdit,
  onViewDetails,
  onViewComments,
}: TaskCardProps) => {
  const displayDueDate = task.dueDate?.trim() || "---";
  const { priorityLabel, statusLabel, taskTypeLabel } = useTaskOptionPresentation();
  const taskTypeDisplay = task.taskType ? taskTypeLabel(task.taskType) : "";

  const actions = [
    {
      label: "Task Details",
      icon: <Eye size={20} />,
      onClick: () => onViewDetails(task),
    },
    {
      label: "Edit Task",
      icon: <Pen2 size={20} />,
      onClick: () => onEdit(task),
      disabled: isTaskStatusCompleted(task.status),
    },
    {
      label: "Delete Task",
      icon: <TrashIcon size={20} />,
      variant: "destructive" as const,
      onClick: () => onDelete(task),
      className: "text-red-500",
      iconClassName: "text-red-500",
    },
  ];

  return (
    <div className="bg-white rounded-[1.25rem] border border-[#EDEEF1] shadow-sm flex flex-col overflow-hidden">
      <div className="p-5 flex flex-col gap-4">
        <div className="flex justify-between items-start">
          <div className="flex flex-wrap gap-2">
            <Badge
              className={`rounded-full px-3 py-1 text-xs font-medium capitalize shadow-none ${getTaskPriorityBadgeClass(task.priority)}`}
            >
              {priorityLabel(task.priority)}
            </Badge>
            <Badge
              className={`rounded-full px-3 py-1 text-xs font-medium capitalize shadow-none ${getTaskStatusBadgeClass(task.status)}`}
            >
              {statusLabel(task.status)}
            </Badge>
            {taskTypeDisplay ? (
              <Badge variant="outline" className="rounded-full px-3 py-1 text-xs font-medium shadow-none">
                {taskTypeDisplay}
              </Badge>
            ) : null}
          </div>
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5 text-[#1B1C20]">
              <ChatRoundDots size={20} />
              <span className="text-sm font-medium">{task.commentsCount}</span>
            </div>
            <ActionDropdown
              actions={actions}
              align="end"
              trigger={
                <button
                  type="button"
                  aria-label="Open task actions"
                  className="flex size-8 cursor-pointer items-center justify-center rounded-md hover:bg-[#F6F6F6]"
                >
                  <MenuDotsIcon size={16} color="#1B1C20" />
                </button>
              }
            />
          </div>
        </div>

        <div className="flex flex-col gap-1.5 min-w-0">
          <h3 className="break-words text-lg font-bold text-[#101828]" title={task.title}>
            {task.title}
          </h3>
          <p className="whitespace-pre-wrap break-words text-sm leading-[1.375rem] text-[#5B616E]">
            {task.description || "No description"}
          </p>
          <p className="flex min-w-0 items-center justify-between gap-2 text-sm text-[#5B616E]">
            <span className="shrink-0">Client:</span>
            <span
              className="min-w-0 flex-1 truncate text-right text-[#101828]"
              title={task.clientName}
            >
              {task.clientName}
            </span>
          </p>
          <p className="text-sm text-[#5B616E] flex items-center justify-between gap-2 min-w-0">
            <span className="shrink-0">Therapist:</span>
            <span
              className="text-[#101828] truncate min-w-0 text-right"
              title={task.assignee || "Unassigned"}
            >
              {task.assignee || "Unassigned"}
            </span>
          </p>
          <p className="text-sm text-[#5B616E] flex justify-between items-center gap-1.5">
            Created Date:{" "}
            <span className="text-[#101828]">{task.createdDate}</span>
          </p>
        </div>
      </div>

      <div className="bg-(--bg-upload-container) px-5 py-4 border-t border-[#EDEEF1] flex justify-between items-center mt-auto">
        <div className="flex items-center gap-2 text-sm text-[#344054] font-medium">
          <CalendarIcon size={16} className="shrink-0 text-[#5B616E]" />
          <span>Due: {displayDueDate}</span>
        </div>
        <Button
          variant="outline"
          className="rounded-full h-9 px-4 font-semibold text-sm border-(--neutral-200) bg-transparent text-(--text-primary-dark) cursor-pointer"
          onClick={() => onViewComments(task)}
        >
          View Comments
        </Button>
      </div>
    </div>
  );
};

export default TaskCard;
