import { useState } from "react";
import { X } from "lucide-react";

import { Button } from "../ui/button";
import CustomInput from "../form/CustomInput";
import CustomTextarea from "../form/CustomTextarea";

interface SessionNoteAmendmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (payload: { amendmentText: string; reason: string }) => void;
  isSaving?: boolean;
}

/**
 * An amendment is how a signed note is changed: the original stays exactly as
 * it was signed and the correction is recorded alongside it, with a reason.
 */
const SessionNoteAmendmentModalContent = ({
  onClose,
  onSubmit,
  isSaving = false,
}: SessionNoteAmendmentModalProps) => {
  const [amendmentText, setAmendmentText] = useState("");
  const [reason, setReason] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = () => {
    const trimmedText = amendmentText.trim();
    const trimmedReason = reason.trim();

    if (!trimmedReason) {
      setFormError("Give a reason for the amendment.");
      return;
    }
    if (!trimmedText) {
      setFormError("Write the amendment itself.");
      return;
    }

    setFormError(null);
    onSubmit({ amendmentText: trimmedText, reason: trimmedReason });
  };

  return (
    <div className="fixed inset-0 z-[10001] flex items-center justify-center bg-black/50 p-4">
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-xl">
        <div className="flex items-start justify-between gap-3 border-b border-(--neutral-200) p-6">
          <div>
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Amend this session note
            </h2>
            <p className="mt-1 text-sm text-(--text-neutral-600)">
              The signed note stays as it is. Your amendment is recorded alongside it,
              attributed to you and timestamped.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="shrink-0 rounded-lg p-1 transition-colors hover:bg-(--neutral-50)"
            aria-label="Close"
          >
            <X size={22} className="text-(--text-neutral-600)" />
          </button>
        </div>

        <div className="flex-1 space-y-5 overflow-y-auto p-6">
          <CustomInput
            label="Reason for the amendment"
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            hint="For example: corrects the medication name recorded in error."
          />
          <CustomTextarea
            label="Amendment"
            value={amendmentText}
            onChange={(event) => setAmendmentText(event.target.value)}
            hint="What the record should say. Write it so it stands on its own."
            rows={8}
            className="min-h-48"
            textareaClassName="min-h-40 resize-y"
          />
        </div>

        <div className="flex items-center justify-end gap-3 border-t border-(--neutral-200) p-6">
          {formError ? (
            <p className="mr-auto text-sm text-(--status-denied)">{formError}</p>
          ) : null}
          <Button
            variant="outline"
            onClick={onClose}
            className="h-11 rounded-full border-(--neutral-200) bg-white px-6 text-sm font-normal text-(--text-neutral-800) hover:bg-(--neutral-50)"
          >
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={isSaving}
            loading={isSaving}
            loadingLabel="Saving..."
            className="h-11 rounded-full bg-(--bg-primary-dark) px-6 text-sm font-semibold text-white hover:bg-(--bg-primary-dark)/90 disabled:opacity-50"
          >
            Save amendment
          </Button>
        </div>
      </div>
    </div>
  );
};

const SessionNoteAmendmentModal = (props: SessionNoteAmendmentModalProps) =>
  props.isOpen ? <SessionNoteAmendmentModalContent {...props} /> : null;

export default SessionNoteAmendmentModal;
