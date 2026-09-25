import { X } from "lucide-react";

import SessionNoteSignedView, {
  type SessionNoteSignedViewProps,
} from "./SessionNoteSignedView";

interface SessionNotePreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  note: SessionNoteSignedViewProps["note"];
  practiceTimezone?: string | null;
  subtitle?: string;
}

/**
 * The note as it will read once finalized, from what is on screen now —
 * the same view a signed note opens in, marked as not yet signed.
 */
const SessionNotePreviewModal = ({
  isOpen,
  onClose,
  note,
  practiceTimezone,
  subtitle,
}: SessionNotePreviewModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[10001] flex items-center justify-center bg-black/50 p-4">
      <div className="flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl bg-white shadow-xl">
        <div className="flex items-start justify-between gap-3 border-b border-(--neutral-200) p-6">
          <div className="min-w-0">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Preview session note
            </h2>
            {subtitle ? (
              <p className="mt-1 text-sm text-(--text-neutral-600)">{subtitle}</p>
            ) : null}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="shrink-0 rounded-lg p-1 transition-colors hover:bg-(--neutral-50)"
            aria-label="Close preview"
          >
            <X size={22} className="text-(--text-neutral-600)" />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto overscroll-contain">
          <SessionNoteSignedView note={note} practiceTimezone={practiceTimezone} preview />
        </div>
      </div>
    </div>
  );
};

export default SessionNotePreviewModal;
