import { useCallback, useState } from "react";
import RecordPaymentModal from "@/components/billing-sections/RecordPaymentModal";
import {
  useLazyGetSessionBillingQuery,
  useRecordPaymentMutation,
} from "@/store/api/admin/billing.api";
import type { Invoice } from "@/types/invoice.type";
import { getApiErrorMessage } from "@/utils/apiError";
import { isStalePaymentStateError } from "@/utils/billingErrors";
import { mapSessionBillingToInvoice } from "@/utils/billingHistory";
import { canRecordBillingPayment } from "@/utils/sessionBillingUi";

type ToastHandler = (message: string, type?: "success" | "error" | "info") => void;

/**
 * Opens the same Record Payment ("Pay now") modal used on the invoice/billings page
 * after a session is marked COMPLETED (billing is auto-created by the backend).
 */
export function useSessionCompletedPayNow(options?: {
  onToast?: ToastHandler;
  onPaymentRecorded?: () => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [triggerGetBilling] = useLazyGetSessionBillingQuery();
  const [recordPayment, { isLoading }] = useRecordPaymentMutation();

  const openPayNowForSession = useCallback(
    async (sessionId: number, optionsOverride?: { notifyIfUnpayable?: boolean }) => {
      if (!Number.isFinite(sessionId) || sessionId <= 0) return;
      try {
        const billing = await triggerGetBilling(sessionId).unwrap();
        const mapped = mapSessionBillingToInvoice(billing);
        if (!canRecordBillingPayment(mapped)) {
          if (optionsOverride?.notifyIfUnpayable) {
            options?.onToast?.("This invoice is already paid.", "info");
          }
          return;
        }
        setInvoice(mapped);
        setIsOpen(true);
      } catch {
        if (optionsOverride?.notifyIfUnpayable) {
          options?.onToast?.("No unpaid invoice was found for this session.", "info");
        }
      }
    },
    [triggerGetBilling, options],
  );

  const maybeOpenAfterStatusChange = useCallback(
    (sessionId: number, status: string) => {
      const normalized = status.trim().toUpperCase().replace(/[\s-]+/g, "_");
      if (normalized !== "COMPLETED") return;
      void openPayNowForSession(sessionId);
    },
    [openPayNowForSession],
  );

  const payNowModal = (
    <RecordPaymentModal
      key={invoice?.id ?? "session-complete-pay-now"}
      isOpen={isOpen}
      onClose={() => setIsOpen(false)}
      invoice={invoice}
      isLoading={isLoading}
      onRecord={async (data) => {
        if (!invoice) return;
        try {
          await recordPayment({
            billingId: invoice.id,
            paymentAmount: Number(data.paymentAmount),
            paymentMethod: data.paymentMethod,
            paymentSide: data.paymentSide,
            paymentDate: data.paymentDate,
            expectedPreviousForSource: data.expectedPreviousForSource,
            referenceNumber: data.referenceNumber,
            notes: data.notes,
          }).unwrap();
          options?.onToast?.("Payment recorded successfully", "success");
          setIsOpen(false);
          setInvoice(null);
          options?.onPaymentRecorded?.();
        } catch (error) {
          if (isStalePaymentStateError(error)) {
            options?.onToast?.(
              "Payment totals changed. Reload latest totals in the dialog and try again.",
              "error",
            );
            return;
          }
          options?.onToast?.(getApiErrorMessage(error), "error");
        }
      }}
    />
  );

  return {
    maybeOpenAfterStatusChange,
    openPayNowForSession,
    payNowModal,
  };
}
