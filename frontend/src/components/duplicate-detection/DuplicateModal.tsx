import { X } from "lucide-react";
import { Button } from "@/components/ui/button";

interface DuplicateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  primaryName: string;
  duplicateName: string;
  isSubmitting?: boolean;
}

const DuplicateModal = ({
  isOpen,
  onClose,
  onConfirm,
  primaryName,
  duplicateName,
  isSubmitting = false,
}: DuplicateModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 backdrop-blur-sm transition-all duration-300">
      <div className="relative w-full max-w-152.5 bg-white rounded-2xl shadow-2xl mx-4 overflow-hidden animate-in fade-in zoom-in duration-200">
        <div className="p-6">
          <div className="flex items-center justify-between mb-2">
            <h3 className="text-xl font-bold text-(--text-primary-dark)">
              Mark client as duplicate?
            </h3>
            <button
              onClick={onClose}
              className="p-1.5 hover:bg-gray-100 rounded-full transition-colors cursor-pointer"
            >
              <X size={22} className="text-(--text-neutral-500)" />
            </button>
          </div>

          <div className="space-y-4 mb-6">
            <p className="text-(--text-neutral-600) leading-relaxed">
              <span className="font-bold text-(--text-primary-dark)">
                "{duplicateName}"
              </span>{" "}
              will be marked as a duplicate of{" "}
              <span className="font-bold text-(--text-primary-dark)">
                "{primaryName}"
              </span>
              . {duplicateName.split(" ")[0]}'s record will be hidden from
              standard views, and all data will remain saved.
            </p>
          </div>

          <div className="flex items-center justify-end gap-3">
            <Button
              variant="outline"
              onClick={onClose}
              disabled={isSubmitting}
              className="border border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--bg-primary-dark) hover:text-white transition-all duration-300 ease-in-out cursor-pointer rounded-full px-8 font-semibold h-11.5"
            >
              Cancel
            </Button>
            <Button
              onClick={onConfirm}
              disabled={isSubmitting}
              loading={isSubmitting}
              loadingLabel="Marking..."
              className="border border-transparent bg-(--bg-primary-dark) text-white hover:bg-transparent hover:border-(--neutral-200) hover:text-(--bg-primary-dark) rounded-full px-8 font-semibold h-11.5 transition-all duration-300 ease-in-out cursor-pointer"
            >
              Mark as duplicate
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DuplicateModal;
