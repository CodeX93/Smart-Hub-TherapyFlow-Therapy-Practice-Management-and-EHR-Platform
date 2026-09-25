import React from "react";
import { X } from "lucide-react";
import { Button } from "../ui/button";

interface DeleteUserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  userName: string;
  isLoading?: boolean;
}

const DeleteUserModal: React.FC<DeleteUserModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  userName,
  isLoading = false,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-150 bg-white rounded-[1.5rem] shadow-xl overflow-hidden relative">
        <button
          onClick={onClose}
          disabled={isLoading}
          className="absolute right-4 top-3 text-(--text-neutral-600) hover:text-(--neutral-950) transition-colors cursor-pointer p-1 hover:bg-(--bg-primary-50) rounded-full"
        >
          <X size={24} />
        </button>

        <div className="p-8 pt-10">
          <h3
            className="text-xl font-semibold text-(--text-primary-dark) mb-3 truncate"
            title={`Delete User "${userName}"`}
          >
            Delete User “{userName}”
          </h3>
          <p className="text-(--text-neutral-600) mb-10">
            This will permanently delete the user. This action cannot be undone.
          </p>

          <div className="flex justify-end gap-4">
            <Button
              variant="outline"
              className="rounded-full px-8 h-11.5 border-(--neutral-200) text-(--neutral-950) hover:bg-(--neutral-50) font-semibold transition-all cursor-pointer"
              onClick={onClose}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button
              className="rounded-full px-8 h-11.5 bg-(--status-denied) hover:bg-(--status-denied) text-white hover:opacity-90 font-semibold transition-all cursor-pointer border-none"
              onClick={onConfirm}
              disabled={isLoading}
              loading={isLoading}
              loadingLabel="Deleting..."
            >
              Delete User
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DeleteUserModal;
