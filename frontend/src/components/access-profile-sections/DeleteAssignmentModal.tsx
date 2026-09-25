import React from "react";
import { X } from "lucide-react";
import { Button } from "../ui/button";

interface DeleteAssignmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

const DeleteAssignmentModal: React.FC<DeleteAssignmentModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-150 bg-white rounded-[1.5rem] shadow-xl overflow-hidden relative">
        <button
          onClick={onClose}
          className="absolute right-4 top-3 text-(--text-neutral-600) hover:text-(--neutral-950) transition-colors cursor-pointer p-1 hover:bg-(--bg-primary-50) rounded-full"
        >
          <X size={24} />
        </button>

        <div className="p-8 pt-10">
          <h3 className="text-xl font-semibold text-(--text-primary-dark) mb-3">
            Delete Supervisor Assignment
          </h3>
          <p className="text-(--text-neutral-600) mb-10">
            Are you sure you want to delete this supervisor assignment? This
            action cannot be undone.
          </p>

          <div className="flex justify-end gap-4">
            <Button
              variant="outline"
              className="rounded-full px-8 h-11.5 border-(--neutral-200) text-(--neutral-950) hover:bg-(--neutral-50) font-semibold transition-all cursor-pointer"
              onClick={onClose}
            >
              Cancel
            </Button>
            <Button
              className="rounded-full px-8 h-11.5 bg-(--status-denied) hover:bg-(--status-denied) text-white font-semibold transition-all cursor-pointer border-none"
              onClick={onConfirm}
            >
              Delete Assignment
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DeleteAssignmentModal;
