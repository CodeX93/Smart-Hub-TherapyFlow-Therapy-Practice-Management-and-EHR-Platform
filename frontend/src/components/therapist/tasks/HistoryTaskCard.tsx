import { CalendarIcon, MenuDotsIcon } from "@/components/icons/commonIcons";
import { MessageSquare } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { Task } from "@/pages/therapist/tasks/tasks.static";
import {
    getTaskPriorityBadgeClass,
    getTaskStatusBadgeClass,
} from "@/utils/taskOptionPresentation";

interface HistoryTaskCardProps {
    task: Task;
    onViewDetails: (task: Task) => void;
    onMenuClick?: (task: Task) => void;
}

const HistoryTaskCard = ({ task, onViewDetails, onMenuClick }: HistoryTaskCardProps) => {
    const hasDueDate = Boolean(task.dueDate?.trim());

    return (
        <div className="bg-white rounded-2xl border border-gray-200 p-5 hover:shadow-md transition-shadow overflow-hidden min-w-0">
            {/* Header: Badges and Menu */}
            <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-2">
                    <Badge className={`${getTaskPriorityBadgeClass(task.priority)} px-2 py-1 text-xs font-medium`}>
                        {task.priority}
                    </Badge>
                    <Badge className={`${getTaskStatusBadgeClass(task.status)} px-2 py-1 text-xs font-medium`}>
                        {task.status}
                    </Badge>
                </div>
                <div className="flex items-center gap-2">
                    <div className="flex items-center gap-1 text-[#8E95A2] text-sm">
                        <MessageSquare className="w-4 h-4" />
                        <span>{task.commentsCount}</span>
                    </div>
                    {onMenuClick && (
                        <button
                            onClick={() => onMenuClick(task)}
                            className="text-gray-400 hover:text-gray-600 transition-colors cursor-pointer"
                        >
                            <MenuDotsIcon className="w-5 h-5" />
                        </button>
                    )}
                </div>
            </div>

            {/* Task Title */}
            <h3 className="text-base font-semibold text-[#101828] mb-3 line-clamp-2">
                {task.title}
            </h3>

            {/* Task Info */}
            <div className="space-y-2 mb-4 min-w-0">
                <div className="flex min-w-0 items-center gap-2 text-sm text-[#667085]">
                    <span className="shrink-0 text-[#8E95A2]">Client:</span>
                    <span className="min-w-0 flex-1 truncate font-medium" title={task.clientName}>
                        {task.clientName}
                    </span>
                </div>
                <div className="flex items-center text-sm text-[#667085]">
                    <span className="text-[#8E95A2] min-w-[3.75rem]">Created:</span>
                    <span>{task.createdDate}</span>
                </div>
            </div>

            {/* Footer: Due Date and Action */}
            <div className="flex items-center justify-between pt-3 border-t border-gray-100">
                {hasDueDate ? (
                    <div className="flex items-center gap-1.5 text-sm text-[#667085]">
                        <CalendarIcon className="w-4 h-4 text-[#8E95A2]" />
                        <span className="text-[#8E95A2]">Due:</span>
                        <span>{task.dueDate}</span>
                    </div>
                ) : (
                    <div />
                )}
                <Button
                    onClick={() => onViewDetails(task)}
                    variant="outline"
                    className="h-9 px-4 rounded-lg border border-gray-200 text-[#344054] font-medium hover:bg-gray-50 cursor-pointer text-sm"
                >
                    View Details
                </Button>
            </div>
        </div>
    );
};

export default HistoryTaskCard;
