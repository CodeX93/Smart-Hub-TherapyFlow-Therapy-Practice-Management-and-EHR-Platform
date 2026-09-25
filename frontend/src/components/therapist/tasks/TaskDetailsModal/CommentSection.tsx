
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
            createdAt: new Date().toISOString(),
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
    const newestComments = (task.comments ?? [])
        .map((comment, index) => ({ comment, index }))
        .sort((a, b) => {
            const aCreatedAt = a.comment.createdAt
                ? new Date(a.comment.createdAt).getTime()
                : Number.NaN;
            const bCreatedAt = b.comment.createdAt
                ? new Date(b.comment.createdAt).getTime()
                : Number.NaN;
            if (Number.isFinite(aCreatedAt) && Number.isFinite(bCreatedAt)) {
                return bCreatedAt - aCreatedAt;
            }
            const aId = Number(a.comment.id);
            const bId = Number(b.comment.id);
            if (Number.isFinite(aId) && Number.isFinite(bId)) {
                return bId - aId;
            }
            return b.index - a.index;
        })
        .map(({ comment }) => comment);

    return (
        <div>
            <h3 className="mb-2 text-base font-semibold leading-6 text-[#1B1C20]">
                Track progress and communicate with team members
            </h3>

            {canAddComment ? (
            <div>
                <div className="relative">
                    <Textarea
                        placeholder="Add comments here..."
                        value={commentText}
                        onChange={(e) => setCommentText(e.target.value)}
                        aria-invalid={isCommentTooLong}
                        className={`h-[8.125rem] min-h-[8.125rem] resize-none rounded-2xl px-4 py-[1.125rem] text-base font-medium leading-6 placeholder:font-medium placeholder:text-[#8E95A2] ${
                          commentText.length > 0 ? "pb-8" : ""
                        } ${
                          isCommentTooLong
                            ? "border-red-500 focus-visible:border-red-500 focus-visible:ring-red-500/20"
                            : "border-[#D8DBDF] focus-visible:border-[#D8DBDF] focus-visible:ring-0"
                        }`}
                    />
                    {commentText.length > 0 ? (
                    <span
                        className={`absolute bottom-3 right-4 text-xs ${
                          isCommentTooLong
                            ? "font-medium text-red-500"
                            : "text-[#5B616E]"
                        }`}
                    >
                        {commentText.length}/{COMMENT_MAX_LENGTH}
                    </span>
                    ) : null}
                </div>

                <div className="mt-4 flex flex-col items-stretch gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <label className="flex cursor-pointer items-center gap-2">
                        <Switch
                            checked={isInternal}
                            onCheckedChange={setIsInternal}
                            aria-label="Internal staff note"
                        />
                        <span className="text-sm font-medium leading-[1.375rem] text-[#5B616E]">
                            Internal staff note (not visible to client)
                        </span>
                    </label>

                    <Button
                        variant="primary"
                        size="sm"
                        onClick={() => void handleAddComment()}
                        disabled={!commentText.trim() || isCommentTooLong || isMutating}
                        loading={isMutating}
                        loadingLabel="Adding comment..."
                        className="self-end"
                    >
                        Add Comment
                    </Button>
                </div>
            </div>
            ) : null}

            {isLoading ? (
                <div className="mt-10 flex items-center gap-2 text-sm text-[#667085]">
                    <ContentLoader variant="inline" size="sm" />
                </div>
            ) : hasComments ? (
                <div className="mt-10">
                    <h4 className="mb-2 text-sm font-medium leading-[1.375rem] text-[#1B1C20]">
                        Comments ({task.commentsCount || 0})
                    </h4>
                    <div className="max-h-[16.25rem] space-y-4 overflow-y-auto overscroll-contain pr-1">
                        {newestComments.map((comment) => (
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
                </div>
            ) : (
                <div className="mt-10">
                    <EmptyComments />
                </div>
            )}
        </div>
    );
};

export default CommentSection;
