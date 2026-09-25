import { TrashIcon } from "@/components/icons/commonIcons";
import { Pencil } from "lucide-react";
import type { Task } from "@/pages/therapist/tasks/tasks.static";
import { isTaskStatusCompleted } from "@/utils/taskOptionPresentation";
import { cn } from "@/lib/utils";

interface TaskHeaderProps {
    task: Task;
    onEdit?: () => void;
    onDelete?: () => void;
    readOnly?: boolean;
}

const TaskHeader = ({ task, onEdit, onDelete, readOnly = false }: TaskHeaderProps) => {
    const editDisabled = isTaskStatusCompleted(task.status);

    return (
        <div className="pt-4 pb-6 min-w-0">
            <div className="flex items-start justify-between gap-4 min-w-0">
                <div className="flex-1 min-w-0">
                    <div className="text-sm text-[#8E95A2] mb-1">Title</div>
                    <h3
                        className="truncate text-lg font-semibold text-[#101828]"
                        title={task.title}
                    >
                        {task.title}
                    </h3>
                </div>
                {!readOnly && (onEdit || onDelete) ? (
                    <div className="flex shrink-0 items-center gap-3">
                    {onEdit ? (
                        <button
                            type="button"
                            onClick={onEdit}
                            disabled={editDisabled}
                            className={cn(
                                "p-2 rounded-lg transition-colors",
                                editDisabled
                                    ? "cursor-not-allowed opacity-50"
                                    : "cursor-pointer hover:bg-gray-100",
                            )}
                            title={editDisabled ? "Completed tasks cannot be edited" : "Edit task"}
                        >
                            <Pencil className="w-5 h-5 text-[#667085]" />
                        </button>
                    ) : null}
                    {onDelete ? (
                        <button
                            type="button"
                            onClick={onDelete}
                            className="p-2 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                            title="Delete task"
                        >
                            <TrashIcon className="w-5 h-5 text-[#F04438]" />
                        </button>
                    ) : null}
                    </div>
                ) : null}
            </div>
        </div>
    );
};

export default TaskHeader;
