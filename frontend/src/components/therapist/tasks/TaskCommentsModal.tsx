
import { ContentLoader } from "@/components/shared/ContentLoader";
import { X } from "lucide-react";
import type { Task } from "@/pages/therapist/tasks/tasks.static";
import CommentSection from "./TaskDetailsModal/CommentSection";

interface TaskCommentsModalProps {
  isOpen: boolean;
  task: Task | null;
  isLoading?: boolean;
  onClose: () => void;
  onAddComment?: (
    taskId: string,
    content: string,
    isInternal: boolean,
  ) => Promise<void>;
  onEditComment?: (
    taskId: string,
    commentId: string,
    content: string,
    isInternal: boolean,
  ) => Promise<void>;
  onDeleteComment?: (taskId: string, commentId: string) => Promise<void>;
  isCommentMutating?: boolean;
}

const TaskCommentsModal = ({
  isOpen,
  task,
  isLoading = false,
  onClose,
  onAddComment,
  onEditComment,
  onDeleteComment,
  isCommentMutating = false,
}: TaskCommentsModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="app-modal-overlay fixed inset-0 z-[60] flex items-center justify-center p-4">
      <div className="fixed inset-0" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="task-comments-title"
        className="app-modal-surface relative flex max-h-[calc(100dvh-2rem)] w-full max-w-[36.6875rem] flex-col overflow-hidden rounded-3xl"
      >
        <div className="flex shrink-0 items-start justify-between gap-4 px-6 py-5">
          <div className="min-w-0">
            <h2
              id="task-comments-title"
              className="text-lg font-semibold leading-[1.625rem] text-[#25272C]"
            >
              Task Comments
            </h2>
            {task ? (
              <p className="mt-1 truncate text-sm leading-[1.375rem] text-[#5B616E]">
                {task.title}
              </p>
            ) : null}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close task comments"
            className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-full text-[#1B1C20] transition-colors hover:bg-[#F6F6F6]"
          >
            <X size={24} />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-6 pt-3 pb-6">
          {isLoading || !task ? (
            <div className="flex min-h-64 flex-col items-center justify-center gap-3">
              <ContentLoader size="xl" />
              <p className="text-sm leading-[1.375rem] text-[#5B616E]">
                Loading comments...
              </p>
            </div>
          ) : (
            <CommentSection
              task={task}
              onUpdateTask={() => {}}
              onAddComment={onAddComment}
              onEditComment={onEditComment}
              onDeleteComment={onDeleteComment}
              isMutating={isCommentMutating}
            />
          )}
        </div>
      </div>
    </div>
  );
};

export default TaskCommentsModal;
