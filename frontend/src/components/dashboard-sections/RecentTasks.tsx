import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { ClipboardList } from "@solar-icons/react-perf/category/notes/Linear/ClipboardList";
import { Eye } from "@solar-icons/react-perf/category/security/Linear/Eye";
import { Pen2 } from "@solar-icons/react-perf/category/messages/Linear/Pen2";
import { Button } from "../ui/button";
import type { RecentTaskItem } from "@/types/therapist-dashboard.type";
import ActionDropdown from "@/components/shared/ActionDropdown";
import {
  getTaskPriorityBadgeClass,
  getTaskStatusBadgeClass,
  isTaskStatusCompleted,
} from "@/utils/taskOptionPresentation";
import { useTaskOptionPresentation } from "@/hooks/useSystemOptionCatalog";

interface RecentTasksProps {
  recentTasks: RecentTaskItem[];
  onViewAll?: () => void;
  onCreateTask?: () => void;
  onTaskAction?: (action: "view" | "edit" | "delete", taskId: string) => void;
}

const RecentTasks = ({
  recentTasks,
  onViewAll,
  onCreateTask,
  onTaskAction,
}: RecentTasksProps) => {
  const { priorityLabel, statusLabel } = useTaskOptionPresentation();

  return (
    <div className="flex flex-col p-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-(--text-primary-dark) text-lg font-semibold">
          <ClipboardList className="w-5 h-5" />
          Recent Tasks
        </div>
        {recentTasks && recentTasks.length > 0 && (
          <Button
            variant="link"
            className="text-(--text-primary-500) font-medium text-sm cursor-pointer hover:underline h-auto p-0"
            onClick={onViewAll}
          >
            View all
          </Button>
        )}
      </div>

      <div className="flex-1 flex flex-col gap-1 overflow-y-auto p-3 max-h-[50rem] custom-scrollbar">
        {recentTasks && recentTasks.length > 0 ? (
          recentTasks.map((task: RecentTaskItem, index: number) => (
            <div
              key={task.id}
              className={`py-4 flex flex-col gap-1 ${
                index !== recentTasks.length - 1
                  ? "border-b border-(--neutral-100)"
                  : ""
              }`}
            >
              <div className="flex justify-between items-start gap-3">
                <div className="flex items-center gap-2 min-w-0 flex-1">
                  <h3 className="text-(--text-primary-dark) font-semibold text-base truncate">
                    {task.title}
                  </h3>
                  <span
                    className={`shrink-0 rounded-full px-3 py-0.5 text-xs font-medium capitalize ${getTaskStatusBadgeClass(
                      task.status
                    )}`}
                  >
                    {statusLabel(task.status)}
                  </span>
                </div>
                <div className="flex items-center gap-2">
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
                        onClick: () => onTaskAction?.("view", task.id),
                      },
                      {
                        label: "Edit Task",
                        icon: <Pen2 size={16} />,
                        onClick: () => onTaskAction?.("edit", task.id),
                        disabled: isTaskStatusCompleted(task.status),
                      },
                      {
                        label: "Delete",
                        icon: <TrashIcon size={16} className="text-red-500" />,
                        onClick: () => onTaskAction?.("delete", task.id),
                      },
                    ]}
                  />
                </div>
              </div>
              <div className="flex items-center gap-2 text-sm min-w-0">
                <span
                  className={`rounded-full px-3 py-0.5 text-xs font-medium capitalize ${getTaskPriorityBadgeClass(
                    task.priority
                  )}`}
                >
                  {priorityLabel(task.priority)}
                </span>
                <span className="text-(--neutral-100)">|</span>
                <span className="text-(--text-neutral-600) truncate min-w-0" title={task.clientName}>
                  Client: {task.clientName}
                </span>
                <span className="text-(--neutral-100)">|</span>
                <span className="text-(--text-neutral-400) text-xs">{task.time}</span>
              </div>
            </div>
          ))
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center gap-2 py-10">
            <img
              src="/assets/task.png"
              alt="empty-tasks"
              className="w-30 h-17"
            />
            <h1 className="text-(--text-primary-dark) text-xl font-semibold">
              No recent tasks
            </h1>
            <p className="text-(--text-neutral-600)">
              Create new tasks to see them here
            </p>
            <Button
              variant="outline"
              className="rounded-full cursor-pointer text-(--bg-primary-dark) mt-2"
              onClick={onCreateTask}
            >
              Create new task
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};

export default RecentTasks;
