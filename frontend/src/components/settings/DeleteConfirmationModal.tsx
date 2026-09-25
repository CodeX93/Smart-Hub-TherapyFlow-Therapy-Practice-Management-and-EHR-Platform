import { X } from "lucide-react";
import { Button } from "@/components/ui/button";

interface DeleteConfirmationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description: string;
  isDeleting?: boolean;
}

const DeleteConfirmationModal = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  isDeleting = false,
}: DeleteConfirmationModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="app-modal-overlay fixed inset-0 z-100 flex items-center justify-center backdrop-blur-[0.125rem]">
      <div className="app-modal-surface relative w-full max-w-147 min-w-0 overflow-hidden rounded-xl mx-4 p-6 transition-all duration-300 animate-in fade-in zoom-in-95">
        {/* Close Button */}
        <button
          onClick={onClose}
          disabled={isDeleting}
          className="absolute top-6 right-6 p-1.5 hover:bg-slate-100 rounded-full transition-colors duration-300 cursor-pointer text-(--text-neutral-500)"
        >
          <X size={20} />
        </button>

        {/* Header */}
        <div className="mb-8 pr-10">
          <h2
            className="min-w-0 truncate text-xl font-bold text-(--text-primary-dark)"
            title={title}
          >
            {title}
          </h2>
          <p className="text-sm text-(--text-neutral-500) mt-1">
            {description}
          </p>
        </div>

        {/* Actions */}
        <div className="flex items-center justify-end gap-3 pt-4">
          <Button
            type="button"
            variant="secondary"
            size="lg"
            onClick={onClose}
            disabled={isDeleting}
          >
            Cancel
          </Button>
          <Button
            variant="destructive"
            size="lg"
            onClick={onConfirm}
            disabled={isDeleting}
            loading={isDeleting}
            loadingLabel="Deleting..."
          >
            Delete
          </Button>
        </div>
      </div>
    </div>
  );
};

export default DeleteConfirmationModal;
