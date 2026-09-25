import { useEffect } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { InvoiceDetailsData } from "../invoiceDetails.data";

function DetailRow(props: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-6">
      <div className="shrink-0 text-sm font-normal leading-5 text-(--text-neutral-400)">
        {props.label}
      </div>
      <div
        className="min-w-0 text-right text-sm font-medium leading-5 break-words text-(--text-gray-900)"
        title={props.value}
      >
        {props.value}
      </div>
    </div>
  );
}

function SendReminderModal(props: {
  open: boolean;
  invoice: InvoiceDetailsData;
  isSending?: boolean;
  onClose: () => void;
  onSend: () => void;
}) {
  useEffect(() => {
    if (!props.open) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !props.isSending) {
        props.onClose();
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [props.isSending, props.onClose, props.open]);

  if (!props.open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-9999">
      <button
        type="button"
        className="absolute inset-0 bg-(--overlay)"
        aria-label="Close modal"
        onClick={() => {
          if (!props.isSending) props.onClose();
        }}
      />

      <div className="absolute inset-0 flex items-center justify-center px-4 py-10">
        <div
          className={cn(
            "w-full max-w-[40.625rem] rounded-[1rem] border border-(--neutral-100) bg-(--surface-white)",
            "shadow-[var(--modal-shadow)]",
          )}
          role="dialog"
          aria-modal="true"
          aria-label="Send Reminder"
        >
          <div className="flex items-start justify-between gap-4 px-5 pt-4">
            <div>
              <div className="text-[1.125rem] font-semibold leading-7 text-(--text-gray-900)">
                Send Reminder
              </div>
              <p className="mt-2 max-w-[27.5rem] text-sm leading-5 text-(--text-neutral-400)">
                Email org admin logins and billing contacts, and create in-app
                payment notifications for tenant admins. Limited to once per
                organization every 15 minutes.
              </p>
            </div>

            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={props.onClose}
              disabled={props.isSending}
              aria-label="Close"
            >
              <X size={16} aria-hidden="true" />
            </Button>
          </div>

          <div className="mt-5 border-t border-(--neutral-100)" />

          <div className="px-5 pb-4 pt-6">
            <div className="rounded-[0.875rem] border border-(--neutral-100) bg-(--surface-white) px-4 py-4">
              <div className="flex flex-col gap-3.5">
                <DetailRow label="Invoice" value={props.invoice.invoiceId} />
                <DetailRow
                  label="Organization"
                  value={props.invoice.customer.organisation}
                />
                <DetailRow label="Amount Due" value={props.invoice.amountDue} />
                <DetailRow label="Status" value={props.invoice.status} />
                <DetailRow
                  label="Recipients"
                  value="Org admins + billing contacts"
                />
              </div>
            </div>

            <div className="mt-5 flex items-center justify-end gap-2">
              <Button
                type="button"
                variant="secondary"
                size="lg"
                onClick={props.onClose}
                disabled={props.isSending}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="primary"
                size="lg"
                onClick={props.onSend}
                disabled={props.isSending}
                loading={props.isSending}
                loadingLabel="Sending..."
              >
                Send Reminder
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default SendReminderModal;
