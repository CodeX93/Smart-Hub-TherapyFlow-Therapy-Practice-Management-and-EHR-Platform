import { useEffect } from "react";
import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "../ui/button";
import CustomSelect from "../form/CustomSelect";
import CustomTextarea from "../form/CustomTextarea";
import { Form } from "../ui/form";
import {
  changeBillingStatusSchema,
  type ChangeBillingStatusFormValues,
} from "../../schemas/billings.schema";
import type { ChangeStatusModalProps } from "@/types/billing.type";
import { BILLING_STATUS_OPTIONS } from "@/pages/therapist/therapist.static";

const ChangeStatusModal = ({
  isOpen,
  onClose,
  invoice,
  initialStatus,
  onApply,
  isLoading,
}: ChangeStatusModalProps) => {
  const form = useForm<ChangeBillingStatusFormValues>({
    resolver: zodResolver(changeBillingStatusSchema),
    defaultValues: {
      billingStatus: "",
      notes: "",
    },
  });

  useEffect(() => {
    if (isOpen) {
      form.reset({
        billingStatus: initialStatus || "pending",
        notes: "",
      });
    }
  }, [isOpen, initialStatus, form]);

  if (!isOpen || !invoice) return null;

  const onSubmit = (data: ChangeBillingStatusFormValues) => {
    onApply(data);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 md:p-0">
      {/* Overlay */}
      <div
        className="app-modal-overlay fixed inset-0 transition-opacity animate-in fade-in duration-200"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="app-modal-surface relative rounded-2xl w-full max-w-135 z-50 overflow-hidden animate-in fade-in zoom-in duration-200">
        {/* Header */}
        <div className="md:px-8 px-3 pt-8 pb-4 flex items-start justify-between border-b border-(--neutral-100) min-w-0">
          <div className="flex min-w-0 flex-1 flex-col gap-1 pr-4">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              Change Billing Status
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
          >
            <X size={24} className="text-(--text-neutral-600)" />
          </button>
        </div>

        {/* Content */}
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit)}
            className="md:px-8 px-3 pt-6 pb-8 space-y-6"
          >
            <CustomSelect
              control={form.control}
              name="billingStatus"
              label="New Status"
              options={BILLING_STATUS_OPTIONS}
              placeholder="Select status"
              required
            />

            <CustomTextarea
              control={form.control}
              name="notes"
              label="Optional Notes"
              placeholder="Enter reason or context for status change..."
            />

            {/* Footer */}
            <div className="flex items-center md:justify-end justify-between gap-3 pt-2">
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
                loadingLabel="Saving..."
              >
                Change Status
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default ChangeStatusModal;
