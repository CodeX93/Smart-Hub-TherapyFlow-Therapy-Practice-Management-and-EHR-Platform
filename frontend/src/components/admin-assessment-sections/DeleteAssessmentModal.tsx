import { X } from "lucide-react";
import { Button } from "@/components/ui/button";

interface DeleteAssessmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  templateTitle: string;
  type: "Assessment" | "Template" | "Section";
  isSubmitting?: boolean;
}

const DeleteAssessmentModal = ({
  isOpen,
  onClose,
  onConfirm,
  templateTitle,
  type,
  isSubmitting = false,
}: DeleteAssessmentModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 backdrop-blur-[0.125rem] transition-all duration-300">
      <div className=" relative w-full max-w-152.5 bg-white rounded-[1.5rem] shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <button
          onClick={onClose}
          type="button"
          disabled={isSubmitting}
          className="absolute top-4 right-4 p-1 text-(--text-neutral-400) hover:text-(--neutral-950) transition-colors cursor-pointer rounded-full hover:bg-(--neutral-50)"
        >
          <X size={24} />
        </button>

        <div className="flex flex-col px-6 pt-8">
          <h2
            className="text-[1.25rem] font-semibold text-(--text-primary-dark) max-w-[90%] truncate"
            title={`Delete ${type} "${templateTitle}"`}
          >
            Delete {type} &ldquo;{templateTitle}&rdquo;
          </h2>
          <p className="text-base text-(--text-neutral-600)">
            This will permanently delete the <span>{type.toLowerCase()}</span>{" "}
            and all its data. This action cannot be undone.
          </p>
        </div>

        {/* Footer */}
        <div className="flex justify-end items-center gap-4 px-6 pt-4 pb-8">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isSubmitting}
            className="px-8 py-3 h-auto border-(--neutral-100) text-(--neutral-950) rounded-full cursor-pointer text-base font-semibold transition-all duration-300 hover:bg-(--neutral-50)"
          >
            Cancel
          </Button>
          <Button
            type="button"
            disabled={isSubmitting}
            onClick={onConfirm}
            className="px-8 py-3 h-auto bg-(--status-denied) hover:bg-(--status-denied)/90 text-white rounded-full cursor-pointer text-base font-semibold transition-all duration-300"
            loading={isSubmitting}
            loadingLabel="Deleting..."
          >
            {`Delete ${type}`}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default DeleteAssessmentModal;
