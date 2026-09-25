import { TrashIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { Pen2 } from "@solar-icons/react-perf/category/messages/Linear/Pen2";
import type { Comment } from "@/pages/therapist/tasks/tasks.static";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";

const COMMENT_MAX_LENGTH = 100;

interface CommentItemProps {
    comment: Comment;
    onEdit: (commentId: string, newText: string) => void;
    onDelete: (commentId: string) => void;
}

const CommentItem = ({ comment, onEdit, onDelete }: CommentItemProps) => {
    const [isEditing, setIsEditing] = useState(false);
    const [editText, setEditText] = useState(comment.text);
    const isEditTooLong = editText.length > COMMENT_MAX_LENGTH;

    const handleSave = () => {
        if (editText.trim() && !isEditTooLong) {
            onEdit(comment.id, editText.trim());
            setIsEditing(false);
        }
    };

    const handleCancel = () => {
        setEditText(comment.text);
        setIsEditing(false);
    };

    return (
        <div className="group relative flex min-w-0 gap-2 overflow-hidden rounded-2xl border border-[#EDEEF1] bg-[#FAFAFB] p-3">
            {/* Avatar */}
            <div className="flex size-7 shrink-0 items-center justify-center rounded-full bg-[#3C4D58]">
                <span className="text-xs font-normal leading-[1.125rem] text-white">{comment.userInitials}</span>
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
                <div className="flex h-[1.375rem] min-w-0 items-center gap-2 pr-16">
                    <span className="truncate text-sm font-semibold leading-[1.375rem] text-[#1B1C20]" title={comment.userName}>{comment.userName}</span>
                    <span className="hidden text-sm leading-[1.375rem] text-[#5B616E] sm:inline">|</span>
                    <span className="text-sm leading-[1.375rem] text-[#5B616E]">{comment.timestamp}</span>
                </div>

                {isEditing ? (
                    <div className="mt-2">
                        <div className="relative">
                            <Textarea
                                value={editText}
                                onChange={(e) => setEditText(e.target.value)}
                                aria-invalid={isEditTooLong}
                                className={`h-[4.625rem] min-h-[4.625rem] resize-none rounded-2xl bg-white p-3 text-sm font-normal leading-[1.375rem] text-[#1B1C20] ${
                                  editText.length > 0 ? "pb-7" : ""
                                } ${
                                  isEditTooLong
                                    ? "border-red-500 focus-visible:border-red-500 focus-visible:ring-red-500/20"
                                    : "border-[#EDEEF1] focus-visible:border-[#EDEEF1] focus-visible:ring-0"
                                }`}
                                autoFocus
                            />
                            {editText.length > 0 ? (
                            <span
                                className={`absolute bottom-2 right-3 text-xs ${
                                  isEditTooLong
                                    ? "font-medium text-red-500"
                                    : "text-[#5B616E]"
                                }`}
                            >
                                {editText.length}/{COMMENT_MAX_LENGTH}
                            </span>
                            ) : null}
                        </div>
                        <div className="mt-2 flex items-center justify-end gap-2">
                            <Button
                                onClick={handleCancel}
                                variant="outline"
                                className="h-9 w-[5.9375rem] cursor-pointer rounded-full border-[#D8DBDF] bg-transparent px-6 text-sm font-semibold text-[#3C4D58] shadow-none hover:bg-[#F6F6F6]"
                            >
                                Cancel
                            </Button>
                            <Button
                                onClick={handleSave}
                                disabled={!editText.trim() || isEditTooLong}
                                className="h-9 w-[5.0625rem] cursor-pointer rounded-full bg-[#3C4D58] px-6 text-sm font-semibold text-white shadow-none hover:bg-[#323E47] disabled:cursor-not-allowed disabled:bg-[#EDEEF1] disabled:text-[#8E95A2] disabled:opacity-100"
                            >
                                Save
                            </Button>
                        </div>
                    </div>
                ) : (
                    <p
                        className="line-clamp-3 break-words text-sm leading-[1.375rem] text-[#5B616E]"
                        title={comment.text}
                    >
                        {comment.text}
                    </p>
                )}
            </div>

            {/* Actions */}
            {!isEditing && (
                <div className="absolute right-3 top-3 flex items-center">
                    <button
                        onClick={() => {
                            setEditText(comment.text);
                            setIsEditing(true);
                        }}
                        className="flex size-8 cursor-pointer items-center justify-center rounded-lg transition-colors hover:bg-[#F6F6F6]"
                        title="Edit comment"
                    >
                        <Pen2 size={20} color="#1B1C20" />
                    </button>
                    <button
                        onClick={() => onDelete(comment.id)}
                        className="flex size-8 cursor-pointer items-center justify-center rounded-lg transition-colors hover:bg-red-50"
                        title="Delete comment"
                    >
                        <TrashIcon size={20} color="#EF4444" />
                    </button>
                </div>
            )}
        </div>
    );
};

export default CommentItem;
