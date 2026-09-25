import { useEffect, useState, type ComponentProps } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import {
  INVOICE_ADJUSTMENT_LIMITS,
  sanitizeAdjustmentAmount,
  sanitizeAdjustmentReason,
} from "../invoiceAdjustment.utils";

interface RefundDisputeModalData {
  invoiceId: string;
  currentBalance: string;
  outstandingBalance?: string;
  invoiceNumericId?: number;
}

interface RefundDisputeModalProps {
  open: boolean;
  invoice: RefundDisputeModalData | null;
  mode?: "refund" | "credit";
  onClose(): void;
  isSubmitting?: boolean;
  onSubmit(refundAmount: string, reason: string): void;
}

function getReadOnlyRowClassName(): string {
  return cn(
    "flex min-h-[2.875rem] min-w-0 items-center justify-between gap-4 rounded-[0.875rem] border border-[#dbe4ec] bg-[#f3f7fc] px-[0.8125rem] py-2 shadow-none",
    "text-[0.8125rem] leading-5",
  );
}

function getEditableFieldShellClassName(): string {
  return cn(
    "relative h-[2.75rem] overflow-hidden rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem]",
    "shadow-none",
  );
}

function getEditableLabelClassName(): string {
  return "pointer-events-none absolute left-[0.8125rem] top-[0.4375rem] text-[0.625rem] font-medium leading-3 text-[#8b97a3]";
}

function getEditableInputClassName(): string {
  return cn(
    "h-full border-0 bg-transparent px-0 pb-[0.4375rem] pt-[1.125rem] shadow-none",
    "text-[0.8125rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0",
  );
}

function ReadOnlyRow(props: { label: string; value: string }) {
  return (
    <div className={getReadOnlyRowClassName()}>
      <div className="shrink-0 text-[0.75rem] font-normal leading-4 text-[#8b97a3]">
        {props.label}
      </div>
      <div
        className="min-w-0 truncate text-right text-[0.8125rem] font-medium leading-5 text-[#25313d]"
        title={props.value}
      >
        {props.value}
      </div>
    </div>
  );
}

function EditableField(props: {
  label: string;
  value: string;
  onChange(value: string): void;
  inputMode?: ComponentProps<typeof Input>["inputMode"];
  placeholder?: string;
  maxLength?: number;
}) {
  return (
    <div className={getEditableFieldShellClassName()}>
      <div className={getEditableLabelClassName()}>{props.label}</div>
      <Input
        value={props.value}
        inputMode={props.inputMode}
        placeholder={props.placeholder}
        maxLength={props.maxLength}
        onChange={function (event) {
          props.onChange(event.target.value);
        }}
        className={getEditableInputClassName()}
      />
    </div>
  );
}

function RefundDisputeModalContent(props: RefundDisputeModalProps) {
  const mode = props.mode ?? "refund";
  const [refundAmount, setRefundAmount] = useState("");
  const [reason, setReason] = useState(mode === "credit" ? "Service credit" : "");

  useEffect(
    function () {
      if (!props.open) return;

      function handleKeyDown(event: KeyboardEvent) {
        if (event.key === "Escape") {
          props.onClose();
        }
      }

      window.addEventListener("keydown", handleKeyDown);

      return function () {
        window.removeEventListener("keydown", handleKeyDown);
      };
    },
    [mode, props.onClose, props.open],
  );

  if (!props.open || !props.invoice) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-[10040]">
      <button
        type="button"
        className="absolute inset-0 bg-[rgba(15,23,42,0.26)] backdrop-blur-[0.125rem]"
        aria-label="Close modal"
        onClick={props.onClose}
      />

      <div className="absolute inset-0 flex items-end justify-center p-0 sm:items-center sm:p-4 sm:py-8">
        <div
          className={cn(
            "flex max-h-[min(100dvh,100%)] w-full max-w-[36.6875rem] flex-col overflow-hidden",
            "rounded-t-[0.875rem] border border-[#e3eaf1] bg-white sm:max-h-[min(90dvh,calc(100dvh-2rem))] sm:rounded-[0.875rem]",
            "shadow-[0_24px_60px_rgba(15,23,42,0.16)]",
          )}
          role="dialog"
          aria-modal="true"
          aria-label={mode === "credit" ? "Apply credit" : "Process refund"}
        >
          <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#e8eef4] px-[1.125rem] py-[0.875rem]">
            <div
              className="min-w-0 truncate pr-2 text-[1.25rem] font-semibold leading-7 text-[#1f2d38]"
              title={mode === "credit" ? "Apply Credit" : "Process Refund"}
            >
              {mode === "credit" ? "Apply Credit" : "Process Refund"}
            </div>

            <Button
              type="button"
              variant="ghost"
              size="icon-sm"
              onClick={props.onClose}
              className="shrink-0"
              aria-label="Close"
            >
              <X size={18} strokeWidth={1.8} aria-hidden="true" />
            </Button>
          </div>

          <div
            className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-[1.125rem] py-[1.125rem]"
            style={{ WebkitOverflowScrolling: "touch" }}
          >
            <div className="flex flex-col gap-[0.75rem]">
              <ReadOnlyRow label="Invoice" value={props.invoice.invoiceId} />

              <ReadOnlyRow
                label="Current Balance"
                value={props.invoice.currentBalance}
              />
              {mode === "credit" ? (
                <ReadOnlyRow
                  label="Outstanding Balance"
                  value={props.invoice.outstandingBalance || props.invoice.currentBalance}
                />
              ) : null}

              <EditableField
                label={mode === "credit" ? "Credit Amount (USD)" : "Refund Amount (USD)"}
                value={refundAmount}
                inputMode="decimal"
                placeholder="0.00"
                onChange={(value) => setRefundAmount(sanitizeAdjustmentAmount(value))}
              />

              <EditableField
                label="Reason"
                value={reason}
                placeholder="Enter reason"
                maxLength={INVOICE_ADJUSTMENT_LIMITS.reason}
                onChange={(value) => setReason(sanitizeAdjustmentReason(value))}
              />
            </div>
          </div>

          <div className="flex shrink-0 flex-wrap items-center justify-end gap-[0.625rem] border-t border-[#e8eef4] px-[1.125rem] py-[0.9375rem]">
            <Button
              type="button"
              variant="secondary"
              size="lg"
              className="min-w-[5.1875rem]"
              onClick={props.onClose}
              disabled={props.isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="primary"
              size="lg"
              className="min-w-[7.125rem]"
              disabled={props.isSubmitting}
              onClick={function () {
                props.onSubmit(refundAmount, reason);
              }}
              loading={props.isSubmitting}
              loadingLabel={mode === "credit"
                  ? "Applying..."
                  : "Processing..."}
            >
              {mode === "credit"
                  ? "Apply Credit"
                  : "Process Refund"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

export type { RefundDisputeModalData };
function RefundDisputeModal(props: RefundDisputeModalProps) { return props.open && Boolean(props.invoice) ? <RefundDisputeModalContent key={props.mode} {...props} /> : null; }
export default RefundDisputeModal;
