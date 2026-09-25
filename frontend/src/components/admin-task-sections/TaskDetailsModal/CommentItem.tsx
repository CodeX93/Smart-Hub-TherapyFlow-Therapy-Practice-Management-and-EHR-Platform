import { TrashIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { Pencil } from "lucide-react";
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
            onEdit(comment.id, editText);
            setIsEditing(false);
        }
    };

    const handleCancel = () => {
        setEditText(comment.text);
        setIsEditing(false);
    };

    return (
        <div className="group relative bg-[#FCFCFD] border border-[#EAECF0] rounded-2xl p-4 flex gap-3 overflow-hidden">
            {/* Avatar */}
            <div className="flex-shrink-0 w-10 h-10 rounded-full bg-[#344054] flex items-center justify-center">
                <span className="text-white text-sm font-medium">{comment.userInitials}</span>
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
                <div className="flex min-w-0 items-center gap-2 mb-1 pr-16">
                    <span className="truncate text-sm font-semibold text-[#101828]" title={comment.userName}>{comment.userName}</span>
                    <span className="text-xs text-[#98A2B3] hidden sm:inline">|</span>
                    <span className="text-xs text-[#98A2B3]">{comment.timestamp}</span>
                </div>

                {isEditing ? (
                    <div className="space-y-3 mt-2">
                        <Textarea
                            value={editText}
                            onChange={(e) => setEditText(e.target.value)}
                            aria-invalid={isEditTooLong}
                            className={`min-h-[5rem] resize-none rounded-xl bg-white text-sm ${
                              isEditTooLong
                                ? "border-red-500 focus-visible:border-red-500 focus-visible:ring-red-500/20"
                                : "border-(--neutral-200)"
                            }`}
                            autoFocus
                        />
                        <div className="-mt-2 flex justify-end">
                            <span
                                className={`text-xs ${
                                  isEditTooLong
                                    ? "font-medium text-red-500"
                                    : "text-[#667085]"
                                }`}
                            >
                                {editText.length}/{COMMENT_MAX_LENGTH}
                            </span>
                        </div>
                        <div className="flex items-center gap-2 justify-end">
                            <Button
                                onClick={handleCancel}
                                variant="outline"
                                className="px-6 h-10 rounded-full border border-(--neutral-200) text-(--text-primary-dark) font-medium hover:bg-gray-50 cursor-pointer"
                            >
                                Cancel
                            </Button>
                            <Button
                                onClick={handleSave}
                                disabled={!editText.trim() || isEditTooLong}
                                className="px-6 h-10 rounded-full bg-[#344054] text-white font-medium hover:bg-[#344054]/90 cursor-pointer shadow-none"
                            >
                                Save
                            </Button>
                        </div>
                    </div>
                ) : (
                    <p
                        className="mt-0.5 line-clamp-3 break-all text-sm leading-relaxed text-[#475467]"
                        title={comment.text}
                    >
                        {comment.text}
                    </p>
                )}
            </div>

            {/* Actions */}
            {!isEditing && (
                <div className="absolute top-4 right-4 flex items-center gap-1">
                    <button
                        onClick={() => setIsEditing(true)}
                        className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer text-[#98A2B3] hover:text-[#475467]"
                        title="Edit comment"
                    >
                        <Pencil size={16} />
                    </button>
                    <button
                        onClick={() => onDelete(comment.id)}
                        className="p-1.5 hover:bg-red-50 rounded-lg transition-colors cursor-pointer text-[#98A2B3] hover:text-[#F04438]"
                        title="Delete comment"
                    >
                        <TrashIcon size={16} />
                    </button>
                </div>
            )}
        </div>
    );
};

export default CommentItem;
