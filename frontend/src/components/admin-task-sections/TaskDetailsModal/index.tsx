
import { ContentLoader } from "@/components/shared/ContentLoader";
import { X } from "lucide-react";
import type { Task } from "@/pages/therapist/tasks/tasks.static";
import TaskHeader from "./TaskHeader";
import TaskInfoSection from "./TaskInfoSection";
import CommentSection from "@/components/therapist/tasks/TaskDetailsModal/CommentSection";

interface TaskDetailsModalProps {
    isOpen: boolean;
    task: Task | null;
    isLoadingTask?: boolean;
    isLoadingComments?: boolean;
    onClose: () => void;
    onEdit?: (task: Task) => void;
    onDelete?: (taskId: string) => void;
    onUpdateTask: (updatedTask: Task) => void;
    readOnly?: boolean;
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
        if (!task || !onDelete) return;
        onDelete(task.id);
    };

    return (
        <div className="app-modal-overlay fixed inset-0 z-[60] flex items-center justify-center backdrop-blur-sm">
            <div className="fixed inset-0" onClick={onClose} />
            <div className="app-modal-surface relative w-full max-w-[43.75rem] min-w-0 rounded-3xl overflow-hidden m-4 flex flex-col max-h-[90vh]">
                {/* Header */}
                <div className="flex items-center justify-between p-6 pb-4">
                    <h2 className="text-xl font-bold text-[#101828]">Task Details</h2>
                    <button
                        onClick={onClose}
                        className="text-(--text-neutral-600) hover:text-(--text-primary-dark) transition-colors cursor-pointer"
                    >
                        <X size={24} />
                    </button>
                </div>

                {/* Scrollable Content */}
                <div className="min-w-0 flex-1 overflow-y-auto overflow-x-hidden px-6 pb-6">
                    {isLoadingTask || !task ? (
                        <div className="flex flex-col items-center justify-center py-20 gap-3">
                            <ContentLoader size="xl" />
                            <p className="text-sm text-(--text-neutral-500)">Loading task details...</p>
                        </div>
                    ) : (
                        <>
                            <TaskHeader
                                task={task}
                                onEdit={onEdit ? () => onEdit(task) : undefined}
                                onDelete={onDelete ? handleDeleteTask : undefined}
                                readOnly={readOnly}
                            />

                            <TaskInfoSection task={task} onViewClient={onViewClient} />

                            <CommentSection
                                task={task}
                                onUpdateTask={onUpdateTask}
                                onAddComment={onAddComment}
                                onEditComment={onEditComment}
                                onDeleteComment={onDeleteComment}
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
