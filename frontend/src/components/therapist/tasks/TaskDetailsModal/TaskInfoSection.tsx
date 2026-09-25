import type { Task } from "@/pages/therapist/tasks/tasks.static";
import { Badge } from "@/components/ui/badge";
import {
    getTaskPriorityBadgeClass,
    getTaskStatusBadgeClass,
} from "@/utils/taskOptionPresentation";
import { useTaskOptionPresentation } from "@/hooks/useSystemOptionCatalog";

interface TaskInfoSectionProps {
    task: Task;
    onViewClient?: (clientId: string) => void;
}

const TaskInfoSection = ({ task, onViewClient }: TaskInfoSectionProps) => {
    const displayDueDate = task.dueDate?.trim() || "---";
    const { priorityLabel, statusLabel } = useTaskOptionPresentation();

    return (
        <div className="min-w-0 pb-4">
            <div className="grid min-w-0 grid-cols-1 gap-x-4 gap-y-4 sm:grid-cols-2">
                {/* Client */}
                {onViewClient && task.clientId ? (
                <div className="min-w-0 overflow-hidden sm:col-span-2">
                    <div className="mb-1 text-sm leading-[1.375rem] text-[#5B616E]">Client</div>
                    <button
                        type="button"
                        onClick={() => {
                            if (task.clientId) {
                                onViewClient?.(task.clientId);
                            }
                        }}
                        className="block w-full max-w-full truncate text-left text-sm font-medium leading-[1.375rem] text-[#1B1C20] underline transition-colors hover:text-(--bg-primary-dark)"
                        title={task.clientName}
                    >
                        {task.clientName}
                    </button>
                </div>
                ) : null}

                <div className="min-w-0 overflow-hidden">
                    <div className="mb-1 text-sm leading-[1.375rem] text-[#5B616E]">Created Date</div>
                    <div className="truncate text-sm font-medium leading-[1.375rem] text-[#1B1C20]">
                        {task.createdDate || "---"}
                    </div>
                </div>

                {/* Due Date */}
                <div className="min-w-0 overflow-hidden">
                    <div className="mb-1 text-sm leading-[1.375rem] text-[#5B616E]">Due Date</div>
                    <div
                        className="truncate text-sm font-medium leading-[1.375rem] text-[#1B1C20]"
                        title={displayDueDate}
                    >
                        {displayDueDate}
                    </div>
                </div>

                {/* Priority */}
                <div className="min-w-0 overflow-hidden">
                    <div className="mb-1 text-sm leading-[1.375rem] text-[#5B616E]">Priority</div>
                    <Badge className={`${getTaskPriorityBadgeClass(task.priority)} h-5 rounded-full px-2 py-0 text-xs font-normal capitalize leading-[1.125rem]`}>
                        {priorityLabel(task.priority)}
                    </Badge>
                </div>

                {/* Status */}
                <div className="min-w-0 overflow-hidden">
                    <div className="mb-1 text-sm leading-[1.375rem] text-[#5B616E]">Status</div>
                    <Badge className={`${getTaskStatusBadgeClass(task.status)} h-5 rounded-full px-2 py-0 text-xs font-normal capitalize leading-[1.125rem]`}>
                        {statusLabel(task.status)}
                    </Badge>
                </div>
            </div>

            {/* Description */}
            {task.description && (
                <div className="mt-4 min-w-0">
                    <div className="mb-1 text-sm leading-[1.375rem] text-[#5B616E]">Description</div>
                    <p className="max-w-full whitespace-pre-wrap break-words text-sm font-medium leading-[1.375rem] text-[#1B1C20] [overflow-wrap:anywhere]">
                        {task.description}
                    </p>
                </div>
            )}
        </div>
    );
};

export default TaskInfoSection;
