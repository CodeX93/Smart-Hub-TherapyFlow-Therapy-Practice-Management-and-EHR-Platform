import { useEffect } from "react";
import { X, AlertTriangle } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "../form/CustomInput";
import { Switch } from "@/components/ui/switch";
import { useForm, type SubmitHandler, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form";
import { optionSchema, type OptionFormData } from "@/schemas/settings.schema";

interface OptionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: {
    label: string;
    value: string;
    isDefault: boolean;
    isSystem: boolean;
    isActive: boolean;
  }) => void | Promise<void>;
  initialData?: { label: string; value: string; isDefault?: boolean; isSystem?: boolean; isActive?: boolean };
  categoryName?: string;
  isSubmitting?: boolean;
}

const OptionModal = ({
  isOpen,
  onClose,
  onSave,
  initialData,
  categoryName,
  isSubmitting = false,
}: OptionModalProps) => {
  const isEdit = !!initialData;

  const form = useForm<OptionFormData>({
    resolver: zodResolver(optionSchema),
    mode: "onChange",
    defaultValues: {
      label: "",
      value: "",
      isDefault: false,
      isSystem: false,
      isActive: true,
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
        reset({
          label: initialData.label,
          value: initialData.value,
          isDefault: !!initialData.isDefault,
          isSystem: !!initialData.isSystem,
          isActive: initialData.isActive ?? true,
        });
      } else {
        reset({
          label: "",
          value: "",
          isDefault: false,
          isSystem: false,
          isActive: true,
        });
      }
    }
  }, [initialData, isOpen, reset]);

  if (!isOpen) return null;

  const onSubmit: SubmitHandler<OptionFormData> = async (data) => {
    await onSave(data);
  };

  return (
    <div className="app-modal-overlay fixed inset-0 z-100 flex items-center justify-center backdrop-blur-[0.125rem]">
      <div className="app-modal-surface relative w-full max-w-147 rounded-xl mx-4 flex flex-col p-5 transition-all duration-300 animate-in fade-in zoom-in-95">
        {/* Close Button */}
        <button
          onClick={onClose}
          disabled={isSubmitting}
          className="absolute top-6 right-6 p-1.5 hover:bg-slate-100 rounded-full transition-colors duration-300 cursor-pointer text-(--text-neutral-500)"
        >
          <X size={20} />
        </button>

        {/* Header */}
        <div className="mb-8 pr-8">
          <h2 className="text-xl font-bold text-(--text-primary-dark)">
            {isEdit ? "Edit Option" : "Add New Option"}
          </h2>
          <p className="text-sm text-(--text-neutral-500) mt-1">
            {isEdit
              ? "Update option information"
              : `Add a new option to ${categoryName || "this category"}`}
          </p>
        </div>

        {/* Content */}
        <Form {...form}>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <CustomInput
                control={control}
                name="value"
                label="Option Key"
                required
                hint={!isEdit ? "e.g. cbt" : undefined}
                maxLength={50}
              />
              {isEdit && (
                <div className="flex items-start gap-2 text-(--status-pending) bg-white text-xs font-medium mt-2">
                  <AlertTriangle size={14} className="mt-0.5 shrink-0" />
                  <p>
                    Changing this will update all existing data that uses this
                    option
                  </p>
                </div>
              )}
            </div>

            <div className="mt-3">
              <CustomInput
                control={control}
                name="label"
                label="Option Label"
                required
                hint={!isEdit ? "e.g. Cognitive Behavioral Therapy" : undefined}
                maxLength={50}
              />
            </div>

            <div className="flex items-center gap-3 py-2">
              <Controller
                control={control}
                name="isDefault"
                render={({ field }) => (
                  <Switch
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    disabled={isSubmitting}
                  />
                )}
              />
              <span className="text-sm font-medium text-(--text-neutral-600)">
                Default Selection
              </span>
            </div>

            <div className="flex items-center gap-3 py-2">
              <Controller
                control={control}
                name="isSystem"
                render={({ field }) => (
                  <Switch
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    disabled={isSubmitting || isEdit}
                  />
                )}
              />
              <span className="text-sm font-medium text-(--text-neutral-600)">
                System Option
              </span>
            </div>

            <div className="flex items-center gap-3 py-2">
              <Controller
                control={control}
                name="isActive"
                render={({ field }) => (
                  <Switch
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    disabled={isSubmitting}
                  />
                )}
              />
              <span className="text-sm font-medium text-(--text-neutral-600)">
                Active
              </span>
            </div>

            {/* Footer Actions */}
            <div className="flex items-center justify-end gap-3 pt-4">
              <Button
                type="button"
                variant="secondary"
                size="lg"
                onClick={onClose}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="lg"
                disabled={!isValid || isSubmitting}
                loading={isSubmitting}
                loadingLabel="Saving..."
              >
                {isEdit ? "Update Option" : "Add Option"}
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default OptionModal;
