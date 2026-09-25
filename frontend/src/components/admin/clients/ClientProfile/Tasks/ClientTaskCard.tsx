
import { CalendarIcon, MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { ChatRoundDots } from "@solar-icons/react-perf/category/messages/Linear/ChatRoundDots";
import { Pen2 } from "@solar-icons/react-perf/category/messages/Linear/Pen2";
import { Eye } from "@solar-icons/react-perf/category/security/Linear/Eye";
import ActionDropdown from "../../../../shared/ActionDropdown";
import { Badge } from "../../../../ui/badge";
import { Button } from "../../../../ui/button";
import type { Task } from "@/pages/therapist/tasks/tasks.static";
import {
    getTaskPriorityBadgeClass,
    getTaskStatusBadgeClass,
    isTaskStatusCompleted,
} from "@/utils/taskOptionPresentation";
import { useTaskOptionPresentation } from "@/hooks/useSystemOptionCatalog";

interface ClientTaskCardProps {
    task: Task;
    readOnly?: boolean;
    hideEditDelete?: boolean;
    onDelete: (task: Task) => void;
    onEdit: (task: Task) => void;
    onViewDetails: (task: Task) => void;
    onViewComments: (task: Task) => void;
}

const ClientTaskCard = ({
    task,
    readOnly = false,
    hideEditDelete = false,
    onDelete,
    onEdit,
    onViewDetails,
    onViewComments,
}: ClientTaskCardProps) => {
    const hasDueDate = Boolean(task.dueDate?.trim());
    const { priorityLabel, statusLabel, taskTypeLabel } = useTaskOptionPresentation();
    const taskTypeDisplay = task.taskType ? taskTypeLabel(task.taskType) : "";

    const actions = [
        {
            label: "Task Details",
            icon: <Eye size={20} />,
            onClick: () => onViewDetails(task),
        },
        ...(!(readOnly || hideEditDelete)
            ? [
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
            ]
            : []),
    ];

    return (
        <div className="flex min-h-[11.125rem] flex-col overflow-hidden rounded-xl border border-[#EDEEF1] bg-white shadow-[0_2px_2px_#1E282E0A]">
            {/* Top Section */}
            <div className="flex flex-1 flex-col gap-3 p-3">
                {/* Header with badges and actions */}
                <div className="flex justify-between items-start">
                    <div className="flex gap-2">
                        <Badge className={`h-5 rounded-full px-2 py-0 text-xs font-normal capitalize leading-[1.125rem] shadow-none ${getTaskPriorityBadgeClass(task.priority)}`}>
                            {priorityLabel(task.priority)}
                        </Badge>
                        <Badge className={`h-5 rounded-full px-2 py-0 text-xs font-normal capitalize leading-[1.125rem] shadow-none ${getTaskStatusBadgeClass(task.status)}`}>
                            {statusLabel(task.status)}
                        </Badge>
                    </div>
                    <div className="flex items-center gap-2">
                        <div className="flex items-center gap-1 text-[#1B1C20]">
                            <ChatRoundDots size={20} />
                            <span className="text-sm font-medium leading-[1.375rem]">{task.commentsCount}</span>
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

                {/* Task Info */}
                <div className="flex min-h-0 flex-col">
                    <h3 className="break-words text-base font-semibold leading-6 text-[#25272C]" title={task.title}>
                        {task.title}
                    </h3>
                    <p className="whitespace-pre-wrap break-words text-sm leading-[1.375rem] text-[#5B616E]">
                        {task.description || "No description"}
                    </p>
                </div>

                {task.taskType || task.assignee ? (
                    <div className="grid grid-cols-1 gap-3 border-t border-[#EDEEF1] pt-3 sm:grid-cols-2">
                        {task.taskType ? (
                            <div className="min-w-0">
                                <p className="text-xs leading-[1.125rem] text-[#5B616E]">Task Type</p>
                                <p className="truncate text-sm font-medium leading-[1.375rem] text-[#1B1C20]">
                                    {taskTypeDisplay || task.taskType}
                                </p>
                            </div>
                        ) : null}
                        {task.assignee ? (
                            <div className="min-w-0">
                                <p className="text-xs leading-[1.125rem] text-[#5B616E]">Assigned To</p>
                                <p className="truncate text-sm font-medium leading-[1.375rem] text-[#1B1C20]">
                                    {task.assignee}
                                </p>
                            </div>
                        ) : null}
                    </div>
                ) : null}
            </div>

            {/* Footer */}
            <div className="mt-auto flex h-[3.25rem] items-center justify-between gap-3 border-t border-[#EDEEF1] bg-[#F3F7F8] px-4 py-2">
                <div className="flex min-w-0 items-center gap-2 text-sm leading-[1.375rem] text-[#1B1C20]">
                    <div className="flex min-w-0 items-center gap-2">
                        <CalendarIcon size={16} className="shrink-0" />
                        <span>Created: {task.createdDate}</span>
                    </div>
                    {hasDueDate ? (
                        <>
                        <span aria-hidden="true" className="h-3.5 w-px shrink-0 bg-[#D8DBDF]" />
                        <div className="flex min-w-0 items-center">
                            <span>Due: {task.dueDate}</span>
                        </div>
                        </>
                    ) : null}
                </div>
                <Button
                    variant="outline"
                    className="h-[2.375rem] shrink-0 cursor-pointer whitespace-nowrap rounded-full border-[#D8DBDF] bg-white px-3 text-sm font-semibold leading-[1.375rem] text-[#3C4D58] hover:bg-[#F6F6F6]"
                    onClick={() => onViewComments(task)}
                >
                    View Comments
                </Button>
            </div>
        </div>
    );
};

export default ClientTaskCard;
