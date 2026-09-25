import { useEffect } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "@/components/form/CustomSelect";
import { useForm, type SubmitHandler } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Form, FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import type { Invoice } from "@/types/invoice.type";
import { BILLING_FILTERS } from "@/pages/therapist/therapist.static";

const refundSchema = z.object({
  refundAmount: z.string().min(1, "Refund amount is required"),
  paymentMethod: z.string().min(1, "Payment method is required"),
  referenceNumber: z.string().optional(),
  notes: z.string().optional(),
});

type RefundFormData = z.infer<typeof refundSchema>;

interface RefundPaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoice: Invoice | null;
  isLoading?: boolean;
  onRefund: (data: RefundFormData) => void;
}

const RefundPaymentModal = ({
  isOpen,
  onClose,
  invoice,
  isLoading = false,
  onRefund,
}: RefundPaymentModalProps) => {
  const form = useForm<RefundFormData>({
    resolver: zodResolver(refundSchema),
    defaultValues: {
      refundAmount: "",
      paymentMethod: "bank_transfer",
      referenceNumber: "",
      notes: "",
    },
  });

  useEffect(() => {
    if (isOpen) {
      form.reset({
        refundAmount: invoice?.paid ?? "",
        paymentMethod: "bank_transfer",
        referenceNumber: "",
        notes: "",
      });
    }
  }, [form, invoice, isOpen]);

  if (!isOpen || !invoice) return null;

  const onSubmit: SubmitHandler<RefundFormData> = (data) => onRefund(data);

  const methodOptions = BILLING_FILTERS.paymentMethodOptions.filter((o) => o.value);

  return (
    <div className="app-modal-overlay fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="app-modal-surface w-full max-w-lg rounded-3xl">
        <div className="flex items-center justify-between border-b border-(--neutral-100) px-6 py-5">
          <div>
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">Refund payment</h2>
            <p className="text-sm text-(--text-neutral-600)">
              Invoice #{invoice.id} · {invoice.client}
            </p>
          </div>
          <button type="button" onClick={onClose} aria-label="Close">
            <X size={22} />
          </button>
        </div>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 px-6 py-5">
            <FormField
              control={form.control}
              name="refundAmount"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomInput
                      {...field}
                      label="Refund amount"
                      stopFloating
                      type="number"
                      decimalOnly
                      required
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="paymentMethod"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomSelect
                      label="Refund method"
                      value={field.value}
                      onChange={field.onChange}
                      options={methodOptions}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="referenceNumber"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomInput {...field} label="Reference number" stopFloating />
                  </FormControl>
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomInput {...field} label="Notes" stopFloating />
                  </FormControl>
                </FormItem>
              )}
            />
            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="secondary" size="md" onClick={onClose}>
                Cancel
              </Button>
              <Button
                type="submit"
                variant="destructive"
                size="md"
                disabled={isLoading}
                loading={isLoading}
                loadingLabel="Processing..."
              >
                Issue refund
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default RefundPaymentModal;
