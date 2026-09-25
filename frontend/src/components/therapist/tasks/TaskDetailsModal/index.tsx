
import { ContentLoader } from "@/components/shared/ContentLoader";
import { X } from "lucide-react";
import type { Task } from "@/pages/therapist/tasks/tasks.static";
import TaskHeader from "./TaskHeader";
import TaskInfoSection from "./TaskInfoSection";
import CommentSection from "./CommentSection";

interface TaskDetailsModalProps {
    isOpen: boolean;
    task: Task | null;
    isLoadingTask?: boolean;
    isLoadingComments?: boolean;
    onClose: () => void;
    onEdit?: (task: Task) => void;
    onDelete?: (taskId: string) => void;
    onUpdateTask?: (updatedTask: Task) => void;
    onAddComment?: (taskId: string, content: string, isInternal: boolean) => Promise<void>;
    onEditComment?: (
      taskId: string,
      commentId: string,
      content: string,
      isInternal: boolean,
    ) => Promise<void>;
    onDeleteComment?: (taskId: string, commentId: string) => Promise<void>;
    isCommentMutating?: boolean;
    onViewClient?: (clientId: string) => void;
    readOnly?: boolean;
}

const TaskDetailsModal = ({
    isOpen,
    task,
    isLoadingTask = false,
    isLoadingComments = false,
    onClose,
    onEdit,
    onDelete,
    onUpdateTask,
    onAddComment,
    onEditComment,
    onDeleteComment,
    isCommentMutating = false,
    onViewClient,
    readOnly = false,
}: TaskDetailsModalProps) => {
    if (!isOpen) return null;

    const handleDeleteTask = () => {
        if (!task || !onDelete || readOnly) return;
        onDelete(task.id);
    };

    return (
        <div className="app-modal-overlay fixed inset-0 z-[60] flex items-center justify-center p-4">
            <div className="fixed inset-0" onClick={onClose} />
            <div className="app-modal-surface relative flex max-h-[calc(100dvh-2rem)] w-full max-w-[36.6875rem] min-w-0 flex-col overflow-hidden rounded-3xl">
                {/* Header */}
                <div className="flex items-center justify-between gap-4 px-6 py-5">
                    <h2 className="text-lg font-semibold leading-[1.625rem] text-[#25272C]">Task Details</h2>
                    <button
                        onClick={onClose}
                        aria-label="Close task details"
                        className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-full text-[#1B1C20] transition-colors hover:bg-[#F6F6F6]"
                    >
                        <X size={24} />
                    </button>
                </div>

                {/* Scrollable Content */}
                <div className="min-w-0 flex-1 overflow-y-auto overflow-x-hidden px-6 pt-3 pb-6">
                    {isLoadingTask || !task ? (
                        <div className="flex flex-col items-center justify-center gap-3 py-20">
                            <ContentLoader size="xl" />
                            <p className="text-sm leading-[1.375rem] text-[#5B616E]">Loading task details...</p>
                        </div>
                    ) : (
                        <>
                            <TaskHeader
                                task={task}
                                onEdit={!readOnly && onEdit ? () => onEdit(task) : undefined}
                                onDelete={!readOnly && onDelete ? handleDeleteTask : undefined}
                            />

                            <TaskInfoSection task={task} onViewClient={onViewClient} />

                            <CommentSection
                                task={task}
                                onUpdateTask={onUpdateTask ?? (() => {})}
                                onAddComment={readOnly ? undefined : onAddComment}
                                onEditComment={readOnly ? undefined : onEditComment}
                                onDeleteComment={readOnly ? undefined : onDeleteComment}
                                isLoading={isLoadingComments}
                                isMutating={isCommentMutating}
                            />
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default TaskDetailsModal;
