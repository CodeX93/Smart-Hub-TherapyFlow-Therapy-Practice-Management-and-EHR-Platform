
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState } from "react";
import type { Task, Comment } from "@/pages/therapist/tasks/tasks.static";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import CommentItem from "./CommentItem";
import EmptyComments from "./EmptyComments";

const COMMENT_MAX_LENGTH = 100;

interface CommentSectionProps {
    task: Task;
    onUpdateTask: (updatedTask: Task) => void;
    onAddComment?: (taskId: string, content: string, isInternal: boolean) => Promise<void>;
    onEditComment?: (
      taskId: string,
      commentId: string,
      content: string,
      isInternal: boolean,
    ) => Promise<void>;
    onDeleteComment?: (taskId: string, commentId: string) => Promise<void>;
    isLoading?: boolean;
    isMutating?: boolean;
}

const CommentSection = ({
  task,
  onUpdateTask,
  onAddComment,
  onEditComment,
  onDeleteComment,
  isLoading = false,
  isMutating = false,
}: CommentSectionProps) => {
    const [commentText, setCommentText] = useState("");
    const [isInternal, setIsInternal] = useState(false);
    const isCommentTooLong = commentText.length > COMMENT_MAX_LENGTH;

    const handleAddComment = async () => {
        if (!commentText.trim() || isCommentTooLong) return;

        if (onAddComment) {
            await onAddComment(task.id, commentText.trim(), isInternal);
            setCommentText("");
            setIsInternal(false);
            return;
        }

        const newComment: Comment = {
            id: `c${Date.now()}`,
            userId: "currentUser",
            userName: "Current User",
            userInitials: "CU",
            text: commentText,
            timestamp: "Just now",
            isInternal,
        };

        const updatedTask = {
            ...task,
            comments: [...(task.comments || []), newComment],
            commentsCount: (task.commentsCount || 0) + 1,
        };

        onUpdateTask(updatedTask);
        setCommentText("");
        setIsInternal(false);
    };

    const handleEditComment = async (commentId: string, newText: string) => {
        if (onEditComment) {
            const edited = (task.comments || []).find((comment) => comment.id === commentId);
            await onEditComment(task.id, commentId, newText, edited?.isInternal ?? false);
            return;
        }

        const updatedComments = (task.comments || []).map(comment =>
            comment.id === commentId ? { ...comment, text: newText } : comment
        );

        onUpdateTask({
            ...task,
            comments: updatedComments,
        });
    };

    const handleDeleteComment = async (commentId: string) => {
        if (onDeleteComment) {
            await onDeleteComment(task.id, commentId);
            return;
        }

        const updatedComments = (task.comments || []).filter(comment => comment.id !== commentId);

        onUpdateTask({
            ...task,
            comments: updatedComments,
            commentsCount: updatedComments.length,
        });
    };

    const hasComments = task.comments && task.comments.length > 0;
    const canAddComment = Boolean(onAddComment);

    return (
        <div className="pt-6">
            <h3 className="text-base font-semibold text-[#101828] mb-6">
                Track progress and communicate with team members
            </h3>

            {canAddComment ? (
            <div className="mb-6">
                <Textarea
                    placeholder="Add comments here..."
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    aria-invalid={isCommentTooLong}
                    className={`min-h-[7.5rem] resize-none rounded-xl text-base ${
                      isCommentTooLong
                        ? "border-red-500 focus-visible:border-red-500 focus-visible:ring-red-500/20"
                        : "border-(--neutral-200)"
                    }`}
                />
                <div className="mb-4 mt-1 flex justify-end">
                    <span
                        className={`text-xs ${
                          isCommentTooLong
                            ? "font-medium text-red-500"
                            : "text-[#667085]"
                        }`}
                    >
                        {commentText.length}/{COMMENT_MAX_LENGTH}
                    </span>
                </div>

                <div className="flex items-center justify-between">
                    <label className="flex cursor-pointer items-center gap-2">
                        <Switch
                            checked={isInternal}
                            onCheckedChange={setIsInternal}
                            aria-label="Internal staff note"
                        />
                        <span className="text-sm text-[#667085]">
                            Internal staff note (not visible to client)
                        </span>
                    </label>

                    <Button
                        onClick={() => void handleAddComment()}
                        disabled={!commentText.trim() || isCommentTooLong || isMutating}
                        className="px-6 h-10 rounded-full bg-[#EFF1F4] text-[#344054] font-medium hover:bg-gray-300 cursor-pointer shadow-none disabled:opacity-50 disabled:cursor-not-allowed"
                      loading={isMutating}
                      loadingLabel="Adding comment..."
                    >

                        Add Comment
                    </Button>
                </div>
            </div>
            ) : null}

            {/* Comments List */}
            <div className="pt-4 border-t border-gray-200">
                <h4 className="text-sm font-medium text-[#101828] mb-4">
                    Comments ({task.commentsCount || 0})
                </h4>

                {isLoading ? (
                    <div className="flex items-center gap-2 text-sm text-[#667085]">
                        <ContentLoader variant="inline" size="sm" />
                    </div>
                ) : hasComments ? (
                    <div className="space-y-4">
                        {task.comments!.map((comment) => (
                            <CommentItem
                                key={comment.id}
                                comment={comment}
                                onEdit={(commentId, text) => {
                                  void handleEditComment(commentId, text);
                                }}
                                onDelete={(commentId) => {
                                  void handleDeleteComment(commentId);
                                }}
                            />
                        ))}
                    </div>
                ) : (
                    <EmptyComments />
                )}
            </div>
        </div>
    );
};

export default CommentSection;
