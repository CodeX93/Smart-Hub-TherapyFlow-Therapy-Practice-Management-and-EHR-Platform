import { useEffect, useRef } from "react";
import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "../ui/button";
import CustomSelect from "../form/CustomSelect";
import CustomInput from "../form/CustomInput";
import { discountOptions } from "@/pages/therapist/therapist.static";
import type { ApplyDiscountModalProps } from "@/types/billing.type";
import { Form } from "../ui/form";
import {
  applyDiscountSchema,
  type ApplyDiscountFormValues,
} from "../../schemas/billings.schema";

function parseInvoiceAmount(amount: string | number | null | undefined): number {
  const parsed = Number.parseFloat(String(amount ?? "").replace(/[^0-9.-]/g, ""));
  return Number.isFinite(parsed) ? parsed : 0;
}

function hasExistingDiscount(invoice: NonNullable<ApplyDiscountModalProps["invoice"]>): boolean {
  const type = (invoice.discountType ?? "").toLowerCase();
  const amount = parseInvoiceAmount(invoice.discountAmount);
  return Boolean(type && type !== "none" && type !== "no_discount" && amount > 0);
}

const ApplyDiscountModal = ({
  isOpen,
  onClose,
  invoice,
  onApply,
  isLoading,
}: ApplyDiscountModalProps) => {
  const form = useForm<ApplyDiscountFormValues>({
    resolver: zodResolver(applyDiscountSchema),
    defaultValues: {
      discountType: "no_discount",
      discountValue: "",
    },
  });

  const discountType = form.watch("discountType");
  const lastValidDiscountValueRef = useRef<string | number>("");

  useEffect(() => {
    if (isOpen) {
      form.reset({
        discountType: "no_discount",
        discountValue: "",
      });
      lastValidDiscountValueRef.current = "";
    }
  }, [isOpen, form]);

  useEffect(() => {
    if (discountType === "no_discount") {
      form.setValue("discountValue", "");
      form.clearErrors("discountValue");
      lastValidDiscountValueRef.current = "";
    }
  }, [discountType, form]);

  if (!isOpen || !invoice) return null;

  const alreadyDiscounted = hasExistingDiscount(invoice);
  // Discount is applied against outstanding (remaining due), not the original service total.
  const outstandingAmount = parseInvoiceAmount(
    invoice.remainingDue ?? invoice.amount,
  );
  const serviceListAmount = parseInvoiceAmount(
    invoice.afterPolicyAmount ?? invoice.originalSubtotal ?? invoice.amountDue,
  );

  const handleDiscountValueChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const raw = event.target.value;

    if (!raw) {
      lastValidDiscountValueRef.current = "";
      form.clearErrors("discountValue");
      return;
    }

    const numeric = Number.parseFloat(raw);
    if (!Number.isFinite(numeric)) return;

    const maxValue =
      discountType === "percentage"
        ? 100
        : discountType === "fixed"
          ? outstandingAmount
          : Number.POSITIVE_INFINITY;

    if (numeric > maxValue) {
      const previousValue = lastValidDiscountValueRef.current;
      const restored =
        previousValue === "" || previousValue === undefined || previousValue === null
          ? ""
          : String(previousValue);
      event.target.value = restored;
      form.setValue(
        "discountValue",
        restored === "" ? "" : Number(restored),
        { shouldValidate: false, shouldDirty: true },
      );
      form.setError("discountValue", {
        type: "manual",
        message:
          discountType === "percentage"
            ? "Percentage discount cannot exceed 100%"
            : "Fixed discount cannot exceed the outstanding balance",
      });
      return;
    }

    lastValidDiscountValueRef.current =
      discountType === "percentage" || discountType === "fixed"
        ? numeric
        : raw;
    form.clearErrors("discountValue");
  };

  const onSubmit = (data: ApplyDiscountFormValues) => {
    if (alreadyDiscounted) {
      form.setError("discountType", {
        type: "manual",
        message: "A discount has already been applied to this invoice",
      });
      return;
    }

    const discountValue = Number(data.discountValue);

    if (
      data.discountType === "fixed" &&
      Number.isFinite(outstandingAmount) &&
      Number.isFinite(discountValue) &&
      discountValue > outstandingAmount
    ) {
      form.setError("discountValue", {
        type: "manual",
        message: "Fixed discount cannot exceed the outstanding balance",
      });
      return;
    }

    onApply(data);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center md:p-4 p-2">
      <div
        className="app-modal-overlay fixed inset-0 transition-opacity animate-in fade-in duration-200"
        onClick={onClose}
      />

      <div className="app-modal-surface relative z-50 w-full max-w-135 overflow-hidden rounded-2xl animate-in fade-in zoom-in duration-200">
        <div className="flex min-w-0 items-start justify-between px-3 pb-4 pt-8 md:px-8">
          <div className="flex min-w-0 flex-1 flex-col gap-1 pr-4">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Apply Discount
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
            className="cursor-pointer rounded-full p-1 transition-colors hover:bg-gray-100"
          >
            <X size={24} className="text-(--text-neutral-600)" />
          </button>
        </div>

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit)}
            className="space-y-6 px-3 pb-8 md:px-8"
          >
            <div className="space-y-2 rounded-2xl border border-(--neutral-100) bg-[#F9FAFB] px-4 py-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-(--text-neutral-600)">
                  Outstanding balance
                </span>
                <span className="text-lg font-bold text-(--text-primary-dark)">
                  ${outstandingAmount.toFixed(2)}
                </span>
              </div>
              {serviceListAmount > 0 ? (
                <div className="flex items-center justify-between text-xs text-(--text-neutral-500)">
                  <span>Service amount</span>
                  <span>${serviceListAmount.toFixed(2)}</span>
                </div>
              ) : null}
              <p className="text-xs text-(--text-neutral-500)">
                Percentage and fixed discounts apply to the outstanding balance
                only. Each invoice can receive one discount.
              </p>
            </div>

            {alreadyDiscounted ? (
              <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                A discount of ${parseInvoiceAmount(invoice.discountAmount).toFixed(2)} is
                already applied. Only one discount is allowed per invoice.
              </div>
            ) : null}

            <div className="grid grid-cols-2 gap-2 md:gap-4">
              <CustomSelect
                control={form.control}
                name="discountType"
                label="Discount Type"
                options={discountOptions}
                placeholder="Select type"
                required
                disabled={alreadyDiscounted}
              />

              <CustomInput
                control={form.control}
                name="discountValue"
                label="Amount"
                type="number"
                stopFloating={true}
                disabled={alreadyDiscounted || discountType === "no_discount"}
                placeholder="0.00"
                icon={discountType === "fixed" ? "$" : undefined}
                suffix={discountType === "percentage" ? "%" : undefined}
                required={discountType !== "no_discount"}
                min={0}
                max={
                  discountType === "percentage"
                    ? 100
                    : discountType === "fixed"
                      ? outstandingAmount
                      : undefined
                }
                onChange={handleDiscountValueChange}
              />
            </div>

            <div className="flex items-center justify-between gap-3 pt-2 md:justify-end">
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
                disabled={
                  alreadyDiscounted || discountType === "no_discount" || isLoading
                }
                loading={isLoading}
                loadingLabel="Applying..."
              >
                Apply Discount
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default ApplyDiscountModal;
