import { Folder } from "lucide-react";
import { Button } from "../ui/button";
import { cn } from "@/lib/utils";

interface ClosedFileNoticeModalProps {
  isOpen: boolean;
  onClose: () => void;
  clientName?: string;
  overlayClassName?: string;
}

const ClosedFileNoticeModal = ({
  isOpen,
  onClose,
  clientName,
  overlayClassName,
}: ClosedFileNoticeModalProps) => {
  if (!isOpen) return null;

  const title = clientName ? `File Closed — ${clientName}` : "Client File Closed";

  return (
    <div
      className={cn(
        "app-modal-overlay fixed inset-0 z-[100] flex items-center justify-center backdrop-blur-sm",
        overlayClassName,
      )}
    >
      <div className="app-modal-surface w-full max-w-lg rounded-lg mx-4">
        <div className="flex items-start gap-4 p-6">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-amber-50">
            <Folder size={20} className="text-amber-600" />
          </div>
          <div className="min-w-0 flex-1">
            <h2
              className="text-lg font-semibold text-gray-900"
              style={{ overflowWrap: "anywhere" }}
            >
              {title}
            </h2>
            <p className="mt-3 text-sm text-gray-600" style={{ overflowWrap: "anywhere" }}>
              This client&apos;s file is closed. You can only view the client record and
              cannot perform any actions. If you need to make changes, you must open the
              file again.
            </p>
          </div>
        </div>

        <div className="flex justify-end p-6 pt-0">
          <Button
            type="button"
            onClick={onClose}
            className="h-10 px-6 rounded-lg cursor-pointer text-sm font-normal bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white"
          >
            OK
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ClosedFileNoticeModal;
