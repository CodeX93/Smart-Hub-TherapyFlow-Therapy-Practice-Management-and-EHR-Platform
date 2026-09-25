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
        <div className="min-w-0 pb-6">
            <div className="mb-6 grid min-w-0 grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-3">
                {/* Client */}
                <div className="min-w-0 overflow-hidden">
                    <div className="mb-1 text-sm text-[#8E95A2]">Client</div>
                    <button
                        type="button"
                        onClick={() => {
                            if (task.clientId) {
                                onViewClient?.(task.clientId);
                            }
                        }}
                        className="block w-full max-w-full truncate text-left text-base font-medium text-[#101828] underline transition-colors hover:text-(--bg-primary-dark)"
                        title={task.clientName}
                    >
                        {task.clientName}
                    </button>
                </div>

                {/* Therapist */}
                <div className="min-w-0 overflow-hidden">
                    <div className="mb-1 text-sm text-[#8E95A2]">Therapist</div>
                    <span
                        className="block w-full max-w-full truncate text-base font-medium text-[#101828]"
                        title={task.assignee || "Unassigned"}
                    >
                        {task.assignee || "Unassigned"}
                    </span>
                </div>

                {/* Due Date */}
                <div className="min-w-0 overflow-hidden">
                    <div className="mb-1 text-sm text-[#8E95A2]">Due Date</div>
                    <div
                        className="truncate text-base text-[#101828]"
                        title={displayDueDate}
                    >
                        {displayDueDate}
                    </div>
                </div>

                {/* Priority */}
                <div className="min-w-0 overflow-hidden">
                    <div className="mb-2 text-sm text-[#8E95A2]">Priority</div>
                    <Badge className={`${getTaskPriorityBadgeClass(task.priority)} font-medium capitalize`}>
                        {priorityLabel(task.priority)}
                    </Badge>
                </div>

                {/* Status */}
                <div className="min-w-0 overflow-hidden sm:col-span-2">
                    <div className="mb-2 text-sm text-[#8E95A2]">Status</div>
                    <Badge className={`${getTaskStatusBadgeClass(task.status)} font-medium capitalize`}>
                        {statusLabel(task.status)}
                    </Badge>
                </div>
            </div>

            {/* Description */}
            {task.description && (
                <div className="min-w-0">
                    <div className="text-sm text-[#8E95A2] mb-2">Description</div>
                    <p className="max-w-full whitespace-pre-wrap break-all text-base text-[#101828] leading-relaxed [overflow-wrap:anywhere]">
                        {task.description}
                    </p>
                </div>
            )}
        </div>
    );
};

export default TaskInfoSection;
