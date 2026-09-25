import { useEffect } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "../form/CustomInput";
import CustomTextarea from "../form/CustomTextarea";
import { useForm, type SubmitHandler } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form, FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form";
import {
  serviceCodeSchema,
  type ServiceCodeFormData,
} from "@/schemas/settings.schema";
import {
  SERVICE_FIELD_LIMITS,
  sanitizeServiceBaseRateInput,
  sanitizeServiceCodeInput,
  sanitizeServiceDurationInput,
} from "@/utils/serviceCodeInput";

interface AddServiceCodeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: ServiceCodeFormData) => void;
  initialData?: ServiceCodeFormData;
  isSaving?: boolean;
  isLoadingInitialData?: boolean;
}

const AddServiceCodeModal = ({
  isOpen,
  onClose,
  onSave,
  initialData,
  isSaving = false,
  isLoadingInitialData = false,
}: AddServiceCodeModalProps) => {
  const isEdit = !!initialData;

  const form = useForm<ServiceCodeFormData>({
    resolver: zodResolver(serviceCodeSchema),
    mode: "onChange",
    defaultValues: {
      code: "",
      name: "",
      description: "",
      duration: "",
      price: "",
    },
  });

  const {
    handleSubmit,
    reset,
    formState: { isValid },
    control,
  } = form;

  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        reset(initialData);
      } else {
        reset({
          code: "",
          name: "",
          description: "",
          duration: "",
          price: "",
        });
      }
    }
  }, [initialData, isOpen, reset]);

  if (!isOpen) return null;

  const onSubmit: SubmitHandler<ServiceCodeFormData> = (data) => {
    onSave(data);
  };

  return (
    <div className="app-modal-overlay fixed inset-0 z-100 flex items-center justify-center p-4 backdrop-blur-[0.125rem]">
      <div className="app-modal-surface relative flex max-h-[90vh] w-full max-w-147 flex-col overflow-hidden rounded-xl transition-all duration-300 animate-in fade-in zoom-in-95">
        {/* Close Button */}
        <button
          onClick={onClose}
          disabled={isSaving}
          className="absolute top-6 right-6 z-10 p-1.5 hover:bg-slate-100 rounded-full transition-colors duration-300 cursor-pointer text-(--text-neutral-500)"
        >
          <X size={20} />
        </button>

        <div className="shrink-0 px-5 pt-5 pr-16">
          {/* Header */}
          <div className="mb-4">
            <h2 className="text-xl font-bold text-(--text-primary-dark)">
              {isEdit ? "Edit Service Code" : "Add New Service Code"}
            </h2>
            <p className="text-sm text-(--text-neutral-500) mt-1">
              {isEdit
                ? "Update service code and pricing details"
                : "Create a new service code with pricing"}
            </p>
          </div>
        </div>

        <Form {...form}>
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="flex min-h-0 flex-1 flex-col overflow-hidden"
          >
            <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 pb-4">
              <div className="space-y-4">
            {isLoadingInitialData ? (
              <div className="rounded-xl border border-(--neutral-100) px-4 py-8 text-center text-sm text-(--text-neutral-500)">
                Loading service details...
              </div>
            ) : null}

            <FormField
              control={control}
              name="code"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <CustomInput
                      label="Service Code"
                      required
                      hint="e.g. PSY-60"
                      type="text"
                      {...field}
                      value={field.value ?? ""}
                      onChange={(event) => {
                        const sanitized = sanitizeServiceCodeInput(event.target.value);
                        event.target.value = sanitized;
                        field.onChange(sanitized);
                      }}
                      maxLength={SERVICE_FIELD_LIMITS.serviceCode}
                      className={!isEdit ? "border-(--neutral-950) border-[0.09375rem]" : ""}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="mt-3">
              <CustomInput
                control={control}
                name="name"
                label="Service Name"
                required
                hint="e.g. Diagnostic Interview"
                maxLength={SERVICE_FIELD_LIMITS.serviceName}
              />
            </div>

            <div className="mt-3">
              <CustomTextarea
                control={control}
                name="description"
                label="Description"
                placeholder="Standard session"
                maxLength={SERVICE_FIELD_LIMITS.description}
                textareaClassName="pt-1.5 placeholder:text-xs placeholder:leading-5"
              />
            </div>

            <div className="mt-3">
              <FormField
                control={control}
                name="duration"
                render={({ field }) => (
                  <FormItem>
                    <FormControl>
                      <CustomInput
                        label="Session Duration (minutes)"
                        required
                        type="text"
                        inputMode="numeric"
                        hint={`Whole number ${SERVICE_FIELD_LIMITS.durationMin}-${SERVICE_FIELD_LIMITS.durationMax}`}
                        {...field}
                        value={field.value ?? ""}
                        onChange={(event) => {
                          const sanitized = sanitizeServiceDurationInput(event.target.value);
                          event.target.value = sanitized;
                          field.onChange(sanitized);
                        }}
                        maxLength={4}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="mt-3">
              <FormField
                control={control}
                name="price"
                render={({ field }) => (
                  <FormItem>
                    <FormControl>
                      <CustomInput
                        label="Base Rate (USD)"
                        required
                        type="text"
                        inputMode="decimal"
                        hint="Required. Up to 8 digits and 2 decimal places"
                        {...field}
                        value={field.value ?? ""}
                        onChange={(event) => {
                          const sanitized = sanitizeServiceBaseRateInput(event.target.value);
                          event.target.value = sanitized;
                          field.onChange(sanitized);
                        }}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
              </div>
            </div>

            {/* Footer Actions */}
            <div className="flex shrink-0 items-center justify-end gap-3 border-t border-(--neutral-100) px-5 py-4">
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
                disabled={!isValid || isSaving || isLoadingInitialData}
                loading={isSaving}
                loadingLabel="Saving..."
              >
                {isEdit ? "Update Service Code" : "Add Service Code"}
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default AddServiceCodeModal;
