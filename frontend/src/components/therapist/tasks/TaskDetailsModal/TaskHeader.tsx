import { TrashIcon } from "@/components/icons/commonIcons";
import { Pen2 } from "@solar-icons/react-perf/category/messages/Linear/Pen2";
import type { Task } from "@/pages/therapist/tasks/tasks.static";
import { isTaskStatusCompleted } from "@/utils/taskOptionPresentation";
import { cn } from "@/lib/utils";

interface TaskHeaderProps {
    task: Task;
    onEdit?: () => void;
    onDelete?: () => void;
}

const TaskHeader = ({ task, onEdit, onDelete }: TaskHeaderProps) => {
    const editDisabled = isTaskStatusCompleted(task.status);

    return (
        <div className="min-w-0 pb-4">
            <div className="flex items-start justify-between gap-4 min-w-0">
                <div className="flex-1 min-w-0">
                    <div className="mb-1 text-sm leading-[1.375rem] text-[#5B616E]">Title</div>
                    <h3
                        className="truncate text-sm font-medium leading-[1.375rem] text-[#1B1C20]"
                        title={task.title}
                    >
                        {task.title}
                    </h3>
                </div>
                {(onEdit || onDelete) ? (
                <div className="flex shrink-0 items-center gap-3">
                    {onEdit ? (
                    <button
                        type="button"
                        onClick={onEdit}
                        disabled={editDisabled}
                        className={cn(
                            "flex size-10 items-center justify-center rounded-lg transition-colors",
                            editDisabled
                                ? "cursor-not-allowed opacity-50"
                                : "cursor-pointer hover:bg-[#F6F6F6]",
                        )}
                        title={editDisabled ? "Completed tasks cannot be edited" : "Edit task"}
                    >
                        <Pen2 size={24} color="#1B1C20" />
                    </button>
                    ) : null}
                    {onDelete ? (
                    <button
                        type="button"
                        onClick={onDelete}
                        className="flex size-10 cursor-pointer items-center justify-center rounded-lg transition-colors hover:bg-red-50"
                        title="Delete task"
                    >
                        <TrashIcon size={24} color="#EF4444" />
                    </button>
                    ) : null}
                </div>
                ) : null}
            </div>
        </div>
    );
};

export default TaskHeader;
