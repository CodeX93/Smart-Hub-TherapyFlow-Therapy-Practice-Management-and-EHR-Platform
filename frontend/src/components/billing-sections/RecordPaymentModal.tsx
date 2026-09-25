import { useEffect, useMemo, useRef } from "react";
import { X } from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "../ui/button";
import CustomInput from "../form/CustomInput";
import CustomSelect from "../form/CustomSelect";
import CustomTextarea from "../form/CustomTextarea";
import CustomDatePicker from "../form/CustomDatePicker";
import { Form } from "../ui/form";
import {
  recordPaymentSchema,
  type RecordPaymentFormValues,
} from "../../schemas/billings.schema";
import type { RecordPaymentModalProps } from "@/types/billing.type";
import { useLazyGetPaymentGuidanceQuery } from "@/store/api/admin/billing.api";

function parseInvoiceAmount(amount: string | number | null | undefined): number {
  const parsed = Number.parseFloat(String(amount ?? "").replace(/[^0-9.-]/g, ""));
  return Number.isFinite(parsed) ? parsed : 0;
}

function todayLocalDateInput(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

/** Convert YYYY-MM-DD to ISO instant at UTC midnight (date-only; avoids TZ future-date rejects). */
function localDateInputToIso(dateInput: string): string {
  const [y, m, d] = dateInput.split("-").map((part) => Number.parseInt(part, 10));
  if (!y || !m || !d) {
    return new Date().toISOString();
  }
  return new Date(Date.UTC(y, m - 1, d)).toISOString();
}

const CLIENT_PAYMENT_METHODS = [
  { value: "cash", label: "Cash" },
  { value: "check", label: "Check" },
  { value: "credit_card", label: "Credit Card" },
  { value: "debit_card", label: "Debit Card" },
  { value: "bank_transfer", label: "Bank Transfer" },
  { value: "online_payment", label: "Online Payment" },
  { value: "credit_balance", label: "Credit Balance" },
];

const INSURANCE_PAYMENT_METHODS = [
  { value: "insurance", label: "Insurance EOB" },
  { value: "check", label: "Check" },
  { value: "bank_transfer", label: "Bank Transfer (EFT)" },
];

const PAID_BY_OPTIONS = [
  { value: "client", label: "Client" },
  { value: "insurance", label: "Insurance" },
];

const RecordPaymentModal = ({
  isOpen,
  onClose,
  invoice,
  onRecord,
  isLoading,
}: RecordPaymentModalProps) => {
  const form = useForm<RecordPaymentFormValues>({
    resolver: zodResolver(recordPaymentSchema),
    defaultValues: {
      paymentSide: "client",
      paymentAmount: "",
      paymentDate: todayLocalDateInput(),
      paymentMethod: "",
      referenceNumber: "",
      notes: "",
    },
  });
  const lastValidPaymentAmountRef = useRef("");
  const [triggerGuidance, { data: guidance }] =
    useLazyGetPaymentGuidanceQuery();
  const paymentSide = useWatch({ control: form.control, name: "paymentSide" });

  const methodOptions = useMemo(
    () =>
      paymentSide === "insurance" ? INSURANCE_PAYMENT_METHODS : CLIENT_PAYMENT_METHODS,
    [paymentSide],
  );

  const maxDueForSide = useMemo(() => {
    if (!guidance) {
      return parseInvoiceAmount(invoice?.remainingDue ?? invoice?.amount);
    }
    const totalRemaining = Math.max(0, guidance.totalRemainingDue);
    // Backend accepts any source up to outstanding balance. Expected insurance/client
    // remainings are advisory for prefill — do not hard-lock insurance to $0 when the
    // bill is not flagged insuranceCovered but still has a balance due.
    if (paymentSide === "insurance") {
      if (guidance.insuranceCovered && guidance.insuranceRemaining > 0) {
        return Math.min(Math.max(0, guidance.insuranceRemaining), totalRemaining);
      }
      return totalRemaining;
    }
    if (guidance.insuranceCovered && guidance.clientRemaining > 0) {
      return Math.min(Math.max(0, guidance.clientRemaining), totalRemaining);
    }
    return totalRemaining;
  }, [guidance, invoice, paymentSide]);

  const alreadyPaidForSide = useMemo(() => {
    if (!guidance) {
      return paymentSide === "insurance"
        ? 0
        : parseInvoiceAmount(invoice?.paid ?? "0");
    }
    return paymentSide === "insurance"
      ? guidance.insuranceAlreadyPaid
      : guidance.clientAlreadyPaid;
  }, [guidance, invoice, paymentSide]);

  const resolvePrefillAmount = (
    side: "client" | "insurance",
    g: NonNullable<typeof guidance>,
  ) => {
    const totalRemaining = Math.max(0, g.totalRemainingDue);
    if (side === "insurance") {
      if (g.insuranceCovered && g.insuranceRemaining > 0) {
        return Math.min(Math.max(0, g.insuranceRemaining), totalRemaining);
      }
      return totalRemaining;
    }
    if (g.insuranceCovered && g.clientRemaining > 0) {
      return Math.min(Math.max(0, g.clientRemaining), totalRemaining);
    }
    return totalRemaining;
  };

  const applyPrefillForSide = (side: "client" | "insurance") => {
    let amount = 0;
    if (guidance) {
      amount = resolvePrefillAmount(side, guidance);
    } else if (invoice) {
      amount = parseInvoiceAmount(invoice.remainingDue ?? invoice.amount);
    }
    const amountStr = amount > 0 ? amount.toFixed(2) : "";
    form.setValue("paymentAmount", amountStr, { shouldValidate: false });
    lastValidPaymentAmountRef.current = amountStr.replace(/[^0-9.]/g, "");
    form.clearErrors("paymentAmount");
    const defaultMethod = side === "insurance" ? "insurance" : "";
    form.setValue("paymentMethod", defaultMethod, { shouldValidate: false });
  };

  useEffect(() => {
    if (!isOpen || !invoice) return;
    const billingId = Number.parseInt(invoice.id, 10);
    if (Number.isFinite(billingId)) {
      void triggerGuidance(billingId);
    }
    form.reset({
      paymentSide: "client",
      paymentAmount: "",
      paymentDate: todayLocalDateInput(),
      paymentMethod: "",
      referenceNumber: "",
      notes: "",
    });
    lastValidPaymentAmountRef.current = "";
  }, [isOpen, invoice, form, triggerGuidance]);

  useEffect(() => {
    if (!isOpen || !guidance) return;
    applyPrefillForSide(paymentSide || "client");
    // Only re-prefill when guidance first loads or paid-by changes
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, guidance, paymentSide]);

  if (!isOpen || !invoice) return null;

  const invoiceAmount = maxDueForSide;

  const servicePrice =
    guidance?.originalSubtotalAmount ??
    parseInvoiceAmount(invoice.originalSubtotal ?? invoice.amount);
  const afterPolicy =
    guidance?.amountAfterPolicy ??
    parseInvoiceAmount(invoice.afterPolicyAmount ?? invoice.amountDue ?? invoice.amount);
  const afterDiscount =
    guidance?.amountAfterDiscount ??
    parseInvoiceAmount(invoice.amountDue ?? invoice.amount);

  const handlePaymentAmountChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const raw = event.target.value;

    if (!raw || raw === ".") {
      if (!raw) {
        lastValidPaymentAmountRef.current = "";
        form.clearErrors("paymentAmount");
      }
      return;
    }

    const numeric = Number.parseFloat(raw);
    if (!Number.isFinite(numeric)) return;

    if (numeric > invoiceAmount) {
      const restored = lastValidPaymentAmountRef.current;
      event.target.value = restored;
      form.setValue("paymentAmount", restored, {
        shouldValidate: false,
        shouldDirty: true,
      });
      form.setError("paymentAmount", {
        type: "manual",
        message: "Payment amount cannot exceed the due amount for this side",
      });
      return;
    }

    lastValidPaymentAmountRef.current = raw;
    form.clearErrors("paymentAmount");
  };

  const onSubmit = async (data: RecordPaymentFormValues) => {
    const paymentDelta = Number(data.paymentAmount);

    // Validate payment delta is a valid positive number
    if (!Number.isFinite(paymentDelta) || paymentDelta <= 0) {
      form.setError("paymentAmount", {
        type: "manual",
        message: "Please enter a valid positive payment amount",
      });
      return;
    }

    // Validate alreadyPaidForSide is valid
    if (!Number.isFinite(alreadyPaidForSide) || alreadyPaidForSide < 0) {
      form.setError("paymentAmount", {
        type: "manual",
        message: "Unable to calculate previous payments. Please refresh and try again.",
      });
      return;
    }

    if (
      Number.isFinite(invoiceAmount) &&
      Number.isFinite(paymentDelta) &&
      paymentDelta > invoiceAmount
    ) {
      form.setError("paymentAmount", {
        type: "manual",
        message: "Payment amount cannot exceed the due amount for this side",
      });
      return;
    }

    const cumulativeAmount = Number(
      (alreadyPaidForSide + paymentDelta).toFixed(2),
    );

    // Final validation of cumulative amount
    if (!Number.isFinite(cumulativeAmount) || cumulativeAmount <= 0) {
      form.setError("paymentAmount", {
        type: "manual",
        message: "Invalid cumulative payment amount. Please refresh and try again.",
      });
      return;
    }

    await onRecord({
      ...data,
      paymentAmount: String(cumulativeAmount),
      paymentDate: localDateInputToIso(data.paymentDate),
      expectedPreviousForSource: Number(alreadyPaidForSide.toFixed(2)),
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 md:p-0">
      <div
        className="app-modal-overlay fixed inset-0 transition-opacity animate-in fade-in duration-200"
        onClick={onClose}
      />

      <div className="app-modal-surface relative rounded-2xl w-full max-w-135 z-50 overflow-hidden animate-in fade-in zoom-in duration-200 flex flex-col max-h-[95vh]">
        <div className="md:px-8 px-3 pt-8 pb-4 flex items-start justify-between min-w-0">
          <div className="flex min-w-0 flex-1 flex-col gap-1 pr-4">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Record Payment
            </h2>
            <p
              className="truncate text-sm text-(--text-neutral-600)"
              title={`${invoice.service} • ${invoice.client}`}
            >
              {invoice.service} • {invoice.client}
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1 hover:bg-gray-100 rounded-full transition-colors cursor-pointer"
            type="button"
            aria-label="Close"
          >
            <X size={24} className="text-(--text-neutral-600)" />
          </button>
        </div>

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit)}
            className="flex-1 flex flex-col overflow-hidden"
          >
            <div className="md:px-8 px-3 pb-8 space-y-6 overflow-y-auto overflow-x-hidden flex-1 scrollbar-hide">
              <div className="bg-[#F9FAFB] rounded-2xl p-4 space-y-3 border border-(--neutral-100)">
                <div className="flex justify-between items-center text-sm">
                  <span className="text-(--text-neutral-600)">Service price</span>
                  <span className="text-(--text-primary-dark)">
                    ${servicePrice.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-(--text-neutral-600)">After policy</span>
                  <span className="text-(--text-primary-dark)">
                    ${afterPolicy.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-(--text-neutral-600)">After discount</span>
                  <span className="text-(--text-primary-dark)">
                    ${afterDiscount.toFixed(2)}
                  </span>
                </div>
                <div className="border-b border-(--neutral-100)" />
                <div className="flex justify-between items-center">
                  <span className="text-base font-semibold text-(--text-primary-dark)">
                    Amount due ({paymentSide === "insurance" ? "insurance" : "client"})
                  </span>
                  <span className="text-base font-bold text-(--text-primary-dark)">
                    ${invoiceAmount.toFixed(2)}
                  </span>
                </div>
                {guidance &&
                paymentSide === "insurance" &&
                !guidance.insuranceCovered ? (
                  <p className="text-xs text-(--text-neutral-500)">
                    This invoice is not marked insurance-covered, so there is no
                    expected insurance split. You can still record insurance money
                    against the outstanding balance (${guidance.totalRemainingDue.toFixed(2)}).
                  </p>
                ) : null}
              </div>

              <CustomSelect
                control={form.control}
                name="paymentSide"
                label="Paid By"
                options={PAID_BY_OPTIONS}
                placeholder="Select who paid"
                required
              />

              <div className="grid grid-cols-2 md:gap-4 gap-2">
                <CustomInput
                  control={form.control}
                  name="paymentAmount"
                  label="Amount received ($)"
                  type="text"
                  inputMode="decimal"
                  decimalOnly
                  required
                  onChange={handlePaymentAmountChange}
                />

                <CustomDatePicker
                  control={form.control}
                  name="paymentDate"
                  label="Date received"
                  required
                />
              </div>

              <CustomSelect
                control={form.control}
                name="paymentMethod"
                label="Payment Method"
                options={methodOptions}
                placeholder="Select method"
                required
              />

              <div className="space-y-1.5">
                <CustomInput
                  control={form.control}
                  name="referenceNumber"
                  label="Reference Number"
                  maxLength={50}
                />
                <p className="text-xs text-(--text-neutral-600) px-1">
                  Check number, EOB / transaction ID, etc.
                </p>
              </div>

              <CustomTextarea
                control={form.control}
                name="notes"
                label="Any additional notes..."
              />
            </div>

            <div className="md:px-8 px-3 pb-8 pt-2">
              <div className="flex items-center md:justify-end justify-between gap-3">
                <Button
                  variant="secondary"
                  size="lg"
                  type="button"
                  onClick={onClose}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  disabled={isLoading}
                  loading={isLoading}
                  loadingLabel="Recording..."
                >
                  Record Payment
                </Button>
              </div>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default RecordPaymentModal;
