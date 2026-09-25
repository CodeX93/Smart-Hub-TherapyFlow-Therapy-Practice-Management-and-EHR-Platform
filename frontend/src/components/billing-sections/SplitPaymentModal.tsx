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
import { useLazyGetPaymentGuidanceQuery } from "@/store/api/admin/billing.api";

const splitSchema = z.object({
  clientAmount: z.string().min(1, "Client amount is required"),
  clientPaymentMethod: z.string().min(1),
  clientReference: z.string().optional(),
  insuranceAmount: z.string().min(1, "Insurance amount is required"),
  insurancePaymentMethod: z.string().min(1),
  insuranceReference: z.string().optional(),
  notes: z.string().optional(),
});

export type SplitPaymentFormData = z.infer<typeof splitSchema>;

interface SplitPaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoice: Invoice | null;
  isLoading?: boolean;
  onSubmitSplit: (data: SplitPaymentFormData) => void;
}

const SplitPaymentModal = ({
  isOpen,
  onClose,
  invoice,
  isLoading = false,
  onSubmitSplit,
}: SplitPaymentModalProps) => {
  const [triggerGuidance, { data: guidance }] = useLazyGetPaymentGuidanceQuery();

  const form = useForm<SplitPaymentFormData>({
    resolver: zodResolver(splitSchema),
    defaultValues: {
      clientAmount: "",
      clientPaymentMethod: "credit_card",
      clientReference: "",
      insuranceAmount: "",
      insurancePaymentMethod: "insurance",
      insuranceReference: "",
      notes: "",
    },
  });

  useEffect(() => {
    if (!isOpen || !invoice) return;
    const billingId = Number.parseInt(invoice.id, 10);
    if (!Number.isFinite(billingId)) return;
    void triggerGuidance(billingId);
    form.reset({
      clientAmount: "",
      clientPaymentMethod: "credit_card",
      clientReference: "",
      insuranceAmount: "",
      insurancePaymentMethod: "insurance",
      insuranceReference: "",
      notes: "",
    });
  }, [form, invoice, isOpen, triggerGuidance]);

  useEffect(() => {
    if (!guidance) return;
    form.setValue("clientAmount", String(guidance.clientRemaining ?? ""));
    form.setValue("insuranceAmount", String(guidance.insuranceRemaining ?? ""));
  }, [form, guidance]);

  if (!isOpen || !invoice) return null;

  const methodOptions = BILLING_FILTERS.paymentMethodOptions.filter((o) => o.value);
  const onSubmit: SubmitHandler<SplitPaymentFormData> = (data) => onSubmitSplit(data);

  return (
    <div className="app-modal-overlay fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="app-modal-surface w-full max-w-2xl rounded-3xl">
        <div className="flex items-center justify-between border-b border-(--neutral-100) px-6 py-5">
          <div>
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">Split payment</h2>
            <p className="text-sm text-(--text-neutral-600)">
              Invoice #{invoice.id}
              {guidance
                ? ` · Balance due $${guidance.totalRemainingDue.toFixed(2)}`
                : ""}
            </p>
          </div>
          <button type="button" onClick={onClose} aria-label="Close">
            <X size={22} />
          </button>
        </div>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5 px-6 py-5">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-3 rounded-2xl border border-(--neutral-100) p-4">
                <p className="text-sm font-semibold text-(--text-primary-dark)">Client leg</p>
                <FormField
                  control={form.control}
                  name="clientAmount"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <CustomInput {...field} label="Amount" stopFloating decimalOnly required />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="clientPaymentMethod"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <CustomSelect
                          label="Method"
                          value={field.value}
                          onChange={field.onChange}
                          options={methodOptions}
                        />
                      </FormControl>
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="clientReference"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <CustomInput {...field} label="Reference" stopFloating />
                      </FormControl>
                    </FormItem>
                  )}
                />
              </div>
              <div className="space-y-3 rounded-2xl border border-(--neutral-100) p-4">
                <p className="text-sm font-semibold text-(--text-primary-dark)">Insurance leg</p>
                <FormField
                  control={form.control}
                  name="insuranceAmount"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <CustomInput {...field} label="Amount" stopFloating decimalOnly required />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="insurancePaymentMethod"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <CustomSelect
                          label="Method"
                          value={field.value}
                          onChange={field.onChange}
                          options={methodOptions}
                        />
                      </FormControl>
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="insuranceReference"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <CustomInput {...field} label="Reference" stopFloating />
                      </FormControl>
                    </FormItem>
                  )}
                />
              </div>
            </div>
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
            <div className="flex justify-end gap-3">
              <Button type="button" variant="secondary" size="md" onClick={onClose}>
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="md"
                disabled={isLoading}
                loading={isLoading}
                loadingLabel="Recording..."
              >
                Record split payment
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default SplitPaymentModal;
