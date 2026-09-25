
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo } from "react";
import { X } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { Switch } from "@/components/ui/switch";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "@/components/form/CustomSelect";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import { cn } from "@/lib/utils";
import {
  invoicePolicySchema,
  type InvoicePolicyFormData,
} from "@/schemas/settings.schema";
import type { CustomSelectOption } from "@/components/form/CustomSelect";
import { useGetInvoicePolicyServicesQuery } from "@/store/api/admin/invoicePolicy.api";
import {
  INVOICE_POLICY_FIELD_LIMITS,
  toInvoicePolicyServiceSelectOptions,
} from "@/utils/invoicePolicyForm";

interface InvoicePolicyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: InvoicePolicyFormData) => void;
  initialData?: InvoicePolicyFormData;
  isEditMode?: boolean;
  isLoadingInitialData?: boolean;
  formKey?: string;
  clientTypeOptions: CustomSelectOption[];
  sessionStatusOptions: CustomSelectOption[];
  isSaving?: boolean;
}

const defaultValues: InvoicePolicyFormData = {
  clientTypeKey: "",
  appointmentStatusKey: "",
  priceType: "FIXED",
  invoicePrice: "",
  policyName: "",
  serviceId: "",
  priority: "",
  effectiveFrom: "",
  effectiveTo: "",
  enabled: true,
};

interface InvoicePolicyFormBodyProps {
  initialData?: InvoicePolicyFormData;
  isEditMode: boolean;
  onClose: () => void;
  onSave: (data: InvoicePolicyFormData) => void;
  clientTypeOptions: CustomSelectOption[];
  sessionStatusOptions: CustomSelectOption[];
  isSaving: boolean;
}

const InvoicePolicyFormBody = ({
  initialData,
  isEditMode,
  onClose,
  onSave,
  clientTypeOptions,
  sessionStatusOptions,
  isSaving,
}: InvoicePolicyFormBodyProps) => {
  const { data: serviceOptions = [], isLoading: isLoadingServices } =
    useGetInvoicePolicyServicesQuery();

  const serviceSelectOptions: CustomSelectOption[] = useMemo(
    () => toInvoicePolicyServiceSelectOptions(serviceOptions),
    [serviceOptions],
  );

  const form = useForm<InvoicePolicyFormData>({
    resolver: zodResolver(invoicePolicySchema),
    mode: "onChange",
    defaultValues: initialData ?? defaultValues,
  });

  const { handleSubmit, watch, setValue, control, trigger } = form;
  const priceType = watch("priceType");
  const enabled = watch("enabled");
  const policyName = watch("policyName") ?? "";
  const effectiveFrom = watch("effectiveFrom");

  useEffect(() => {
    if (effectiveFrom?.trim()) {
      void trigger("effectiveTo");
    }
  }, [effectiveFrom, trigger]);

  return (
    <Form {...form}>
      <form
        onSubmit={handleSubmit(onSave)}
        className="flex min-h-0 flex-1 flex-col overflow-hidden"
      >
        <div className="custom-scrollbar min-h-0 flex-1 space-y-5 overflow-y-auto px-6 py-5">
          <CustomInput
            control={control}
            name="policyName"
            label="Policy name"
            placeholder="Optional display name"
            stopFloating
            maxLength={INVOICE_POLICY_FIELD_LIMITS.policyName}
            hint={`${policyName.length}/${INVOICE_POLICY_FIELD_LIMITS.policyName}`}
          />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <CustomSelect
              control={control}
              name="clientTypeKey"
              label="Client type"
              required
              options={clientTypeOptions}
              placeholder="Select client type"
              isSearch
            />
            <CustomSelect
              control={control}
              name="appointmentStatusKey"
              label="Session status"
              required
              options={sessionStatusOptions}
              placeholder="Select session status"
              isSearch
            />
          </div>

          <div>
            <p className="mb-2 text-sm font-medium text-(--text-primary-dark)">
              Price type <span className="text-red-500">*</span>
            </p>
            <div className="inline-flex rounded-full border border-(--neutral-100) bg-(--neutral-100) p-1">
              {(["FIXED", "PERCENTAGE"] as const).map((type) => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setValue("priceType", type, { shouldValidate: true })}
                  className={cn(
                    "cursor-pointer rounded-full px-5 py-2 text-sm font-medium transition-all",
                    priceType === type
                      ? "bg-white text-(--text-primary-dark) shadow-xs"
                      : "text-(--text-neutral-600) hover:text-(--text-primary-dark)",
                  )}
                >
                  {type === "FIXED" ? "Fixed" : "Percentage"}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <CustomInput
              control={control}
              name="invoicePrice"
              label={priceType === "PERCENTAGE" ? "Invoice price (%)" : "Invoice price ($)"}
              required
              placeholder={priceType === "PERCENTAGE" ? "e.g. 100" : "e.g. 150.00"}
              stopFloating
              decimalOnly
              onChange={(event) => {
                if (priceType !== "PERCENTAGE") return;
                const parsed = Number.parseFloat(event.target.value);
                if (Number.isFinite(parsed) && parsed > 100) {
                  event.target.value = "100";
                }
              }}
            />
            <CustomInput
              control={control}
              name="priority"
              label="Priority"
              placeholder="Higher wins when multiple rules match"
              stopFloating
              digitsOnly
            />
          </div>

          <CustomSelect
            control={control}
            name="serviceId"
            label="Service scope"
            required
            options={
              isLoadingServices && serviceSelectOptions.length === 0
                ? [{ value: "", label: "Loading services...", disabled: true }]
                : serviceSelectOptions
            }
            placeholder="Select service"
            isSearch
          />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <CustomDatePicker
              control={control}
              name="effectiveFrom"
              label="Effective from"
              valueType="string"
            />
            <CustomDatePicker
              control={control}
              name="effectiveTo"
              label="Effective to"
              valueType="string"
              minDate={effectiveFrom?.trim() || undefined}
            />
          </div>

          <div className="flex items-center justify-between rounded-2xl border border-(--neutral-100) px-4 py-3">
            <div>
              <p className="text-sm font-medium text-(--text-primary-dark)">Enabled</p>
              <p className="text-xs text-(--text-neutral-600)">
                Disabled policies are kept but not applied to new billing.
              </p>
            </div>
            <Switch
              checked={enabled}
              onCheckedChange={(checked) =>
                setValue("enabled", checked, { shouldDirty: true })
              }
            />
          </div>
        </div>

        <div className="flex shrink-0 justify-end gap-3 border-t border-(--neutral-100) px-6 py-4">
          <Button
            type="button"
            variant="secondary"
            size="lg"
            onClick={onClose}
            disabled={isSaving}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            size="lg"
            disabled={isSaving}
            loading={isSaving}
            loadingLabel="Saving..."
          >
            {isEditMode ? "Save changes" : "Create policy"}
          </Button>
        </div>
      </form>
    </Form>
  );
};

const InvoicePolicyModal = ({
  isOpen,
  onClose,
  onSave,
  initialData,
  isEditMode = false,
  isLoadingInitialData = false,
  formKey = "create",
  clientTypeOptions,
  sessionStatusOptions,
  isSaving = false,
}: InvoicePolicyModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="app-modal-overlay fixed inset-0 z-[70] flex items-center justify-center backdrop-blur-sm">
      <div className="fixed inset-0" onClick={isSaving ? undefined : onClose} />
      <div className="app-modal-surface relative mx-4 flex max-h-[90vh] w-full max-w-[40rem] flex-col overflow-hidden rounded-3xl">
        <div className="flex shrink-0 items-start justify-between border-b border-(--neutral-100) px-6 py-5">
          <div>
            <h2 className="text-xl font-bold text-(--text-primary-dark)">
              {isEditMode ? "Edit invoice policy" : "Add invoice policy"}
            </h2>
            <p className="mt-1 text-sm text-(--text-neutral-600)">
              Policies apply when staff creates session billing. Rates are calculated server-side.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSaving}
            className="cursor-pointer text-(--text-neutral-600) transition-colors hover:text-(--text-primary-dark)"
          >
            <X size={24} />
          </button>
        </div>

        {isLoadingInitialData ? (
          <div className="flex min-h-[20rem] flex-col items-center justify-center gap-3 px-6 py-12">
            <ContentLoader size="lg" />
            <p className="text-sm text-(--text-neutral-500)">Loading policy details...</p>
          </div>
        ) : (
          <InvoicePolicyFormBody
            key={formKey}
            initialData={initialData}
            isEditMode={isEditMode}
            onClose={onClose}
            onSave={onSave}
            clientTypeOptions={clientTypeOptions}
            sessionStatusOptions={sessionStatusOptions}
            isSaving={isSaving}
          />
        )}
      </div>
    </div>
  );
};

export default InvoicePolicyModal;
