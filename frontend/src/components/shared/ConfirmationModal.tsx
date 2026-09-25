import { X } from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { cn } from "@/lib/utils";

/** "confirm" is for a consequential but non-destructive action, e.g. signing a note. */
export type ConfirmationModalType =
  | "delete"
  | "close"
  | "disable"
  | "activate"
  | "confirm";

interface ConfirmationModalProps {
  type: ConfirmationModalType;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  clientName?: string;
  title?: string;
  description?: string;
  items?: string[];
  confirmButtonText?: string;
  confirmButtonDisabled?: boolean;
  confirmButtonLoading?: boolean;
  confirmButtonLoadingText?: string;
  reasonLabel?: string;
  reasonPlaceholder?: string;
  reasonValue?: string;
  onReasonChange?: (value: string) => void;
  overlayClassName?: string;
}

const ConfirmationModal = ({
  type,
  isOpen,
  onClose,
  onConfirm,
  clientName,
  title,
  description,
  items,
  confirmButtonText,
  confirmButtonDisabled,
  confirmButtonLoading,
  confirmButtonLoadingText,
  reasonLabel,
  reasonPlaceholder,
  reasonValue,
  onReasonChange,
  overlayClassName,
}: ConfirmationModalProps) => {
  if (!isOpen) return null;

  // Default configurations based on type
  const getDefaultConfig = () => {
    switch (type) {
      case "delete":
        return {
          title: title || `Delete Client "${clientName || ""}"`,
          description:
            description ||
            "Are you sure you want to delete? This action cannot be undone and will permanently remove:",
          items: items || [
            "Client profile and personal information",
            "All session records and notes",
            "Documents and attachments",
            "Tasks and assessments",
          ],
          confirmButtonText: confirmButtonText || "Delete client",
        };
      case "close":
        return {
          title: title || "Close Client File",
          description:
            description ||
            "Are you sure you want to close this client file? The file will become inactive and new sessions/notes cannot be added. All historical data will remain accessible.",
          items: undefined,
          confirmButtonText: confirmButtonText || "Close file",
        };
      case "disable":
        return {
          title: title || `Disable portal access for "${clientName || ""}"`,
          description:
            description || "The client will no longer be able to log in to the portal",
          items: undefined,
          confirmButtonText: confirmButtonText || "Disable",
        };
      case "activate":
        return {
          title: title || "Open Client File",
          description:
            description ||
            "Are you sure you want to open this client file? The client status will be set to active and you can add sessions, tasks, and other records again.",
          items: undefined,
          confirmButtonText: confirmButtonText || "Open file",
        };
      default:
        return {
          title: title || "Confirm Action",
          description: description || "Are you sure you want to proceed?",
          items: undefined,
          confirmButtonText: confirmButtonText || "Confirm",
        };
    }
  };

  const config = getDefaultConfig();

  return (
    <div
      data-scheduling-nested-modal
      className={cn(
        "app-modal-overlay fixed inset-0 z-[60] flex items-center justify-center p-4",
        overlayClassName,
      )}
      onMouseDown={(event) => event.stopPropagation()}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirmation-modal-title"
        className="app-modal-surface relative flex max-h-[calc(100dvh-2rem)] w-full max-w-[38.125rem] flex-col overflow-hidden rounded-2xl"
      >
        <div className="min-h-0 overflow-y-auto px-6 pt-6 pb-5">
          <h2
            id="confirmation-modal-title"
            className="min-w-0 pr-10 text-xl font-semibold leading-7 text-[#1B1C20] [overflow-wrap:anywhere]"
            title={config.title}
          >
            {config.title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            disabled={confirmButtonLoading}
            aria-label="Close confirmation"
            className="absolute top-2.5 right-2.5 flex size-11 cursor-pointer items-center justify-center rounded-full text-[#1B1C20] transition-colors hover:bg-[#F6F6F6] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3C4D58]/30 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <X size={24} strokeWidth={2} />
          </button>

          <p
            className="mt-2 break-words text-base leading-6 text-[#5B616E]"
            title={typeof config.description === "string" ? config.description : undefined}
          >
            {config.description}
          </p>

          {onReasonChange ? (
            <div className="mt-5 space-y-2">
              <label className="text-sm font-medium leading-[1.375rem] text-[#1B1C20]">
                {reasonLabel || "Reason"}
              </label>
              <Input
                value={reasonValue ?? ""}
                onChange={(event) => onReasonChange(event.target.value)}
                placeholder={reasonPlaceholder || "Enter reason"}
                className="h-12 rounded-xl border-[#D8DBDF] text-base sm:text-sm"
              />
            </div>
          ) : null}

          {config.items && config.items.length > 0 && (
            <ul className="text-base leading-6 text-[#5B616E]">
              {config.items.map((item, index) => (
                <li key={index} className="flex items-start gap-2">
                  <span className="mt-0.5 text-[#5B616E]">•</span>
                  <span>{item}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="flex shrink-0 flex-wrap items-center justify-end gap-3 px-6 pt-3 pb-6">
          <Button
            type="button"
            variant="secondary"
            size="lg"
            onClick={onClose}
            disabled={confirmButtonLoading}
            className="min-w-[6.9375rem] cursor-pointer"
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant={type === "activate" || type === "confirm" ? "primary" : "destructive"}
            size="lg"
            onClick={onConfirm}
            disabled={confirmButtonDisabled || confirmButtonLoading}
            loading={confirmButtonLoading}
            loadingLabel={confirmButtonLoadingText || "Processing..."}
            className="cursor-pointer"
          >
            {config.confirmButtonText}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmationModal;
