
import { ContentLoader } from "@/components/shared/ContentLoader";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import CustomSelect from "@/components/form/CustomSelect";
import CustomTextarea from "@/components/form/CustomTextarea";
import { useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form";
import CustomInput from "@/components/form/CustomInput";
import {
  type CreateFormTemplateValues,
  createFormTemplateSchema,
} from "@/schemas/admin-clinical-schemas";

interface CreateFormTemplateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateFormTemplateValues) => void;
  isSubmitting?: boolean;
  mode?: "create" | "edit";
  isFetching?: boolean;
  initialValues?: Partial<CreateFormTemplateValues> | null;
}

const CreateFormTemplateModal = ({
  isOpen,
  onClose,
  onCreate,
  isSubmitting = false,
  mode = "create",
  isFetching = false,
  initialValues = null,
}: CreateFormTemplateModalProps) => {
  const modalContainerRef = useRef<HTMLDivElement>(null);
  const modalScrollRef = useRef<HTMLDivElement>(null);
  const seededOpenRef = useRef(false);
  const form = useForm<CreateFormTemplateValues>({
    resolver: zodResolver(createFormTemplateSchema),
    mode: "onChange",
    defaultValues: {
      formName: "",
      category: "consent",
      description: "",
      instructions: "",
      requiresSignature: false,
      active: true,
      isSystemTemplate: false,
      sortOrder: "",
    },
  });

  const isBootstrapping = mode === "edit" && isFetching && !initialValues;

  useEffect(() => {
    if (!isOpen) {
      seededOpenRef.current = false;
      return;
    }
    if (mode === "edit" && !initialValues) return;
    if (seededOpenRef.current) return;

    form.reset({
      formName: initialValues?.formName ?? "",
      category: initialValues?.category ?? "consent",
      description: initialValues?.description ?? "",
      instructions: initialValues?.instructions ?? "",
      requiresSignature: initialValues?.requiresSignature ?? false,
      active: initialValues?.active ?? true,
      isSystemTemplate: initialValues?.isSystemTemplate ?? false,
      sortOrder: initialValues?.sortOrder ?? "",
    });
    seededOpenRef.current = true;
  }, [form, initialValues, isOpen, mode]);

  if (!isOpen) return null;

  const onSubmit = (data: CreateFormTemplateValues) => {
    onCreate(data);
  };

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div
        ref={modalContainerRef}
        className="w-full max-w-155 bg-white rounded-2xl shadow-lg mx-4 flex flex-col max-h-[90vh] overflow-hidden"
      >
        {/* Header */}
        <div className="p-6 pb-4 flex items-start justify-between">
          <div>
            <h2 className="text-xl font-semibold text-(--neutral-950)">
              {mode === "edit" ? "Edit Form Template" : "Create Form Template"}
            </h2>
            <p className="text-sm text-(--text-neutral-600) mt-1">
              {mode === "edit"
                ? "Update this form template."
                : "Create a new form template that can be assigned to clients"}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-(--text-neutral-400) hover:text-(--neutral-950) transition-colors p-1 rounded-full hover:bg-(--neutral-50) cursor-pointer"
          >
            <X size={24} />
          </button>
        </div>

        {/* Content */}
        <div
          ref={modalScrollRef}
          className="p-6 pt-2 overflow-y-auto custom-scrollbar flex-1"
        >
          {isBootstrapping ? (
            <div className="py-20 flex justify-center">
              <ContentLoader variant="inline" size="md" />
            </div>
          ) : (
            <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
              <div className="space-y-1.5">
                <CustomInput
                  control={form.control}
                  name="formName"
                  label="Form Name"
                  required
                  maxLength={100}
                />
                <p className="text-(--text-neutral-600) text-xs px-1">
                  e.g., HIPAA Privacy Notice (max 100 characters)
                </p>
              </div>

              <div className="space-y-1.5">
                <CustomSelect
                  control={form.control}
                  name="category"
                  label="Category"
                  closeOnScroll
                  portalContainerRef={modalContainerRef}
                  scrollContainerRefs={[modalScrollRef]}
                  options={[
                    { value: "consent", label: "Informed Consent" },
                    { value: "intake", label: "Intake" },
                    { value: "release", label: "Release of Information" },
                    { value: "agreement", label: "Treatment Agreement" },
                    { value: "safety", label: "Safety Plan" },
                    { value: "discharge", label: "Discharge Summary" },
                    { value: "custom", label: "Custom Form" },
                  ]}
                />
              </div>

              <div className="space-y-1.5">
                <CustomTextarea
                  control={form.control}
                  name="description"
                  label="Description"
                  hint="Brief description of this form"
                />
              </div>

              <div className="space-y-1.5">
                <CustomTextarea
                  control={form.control}
                  name="instructions"
                  label="Instructions"
                  hint="Optional instructions for filling this form"
                />
              </div>

              <div className="space-y-4 pt-2">
                <div className="flex items-center gap-3">
                  <Switch
                    checked={form.watch("requiresSignature")}
                    onCheckedChange={(checked) =>
                      form.setValue("requiresSignature", checked)
                    }
                  />
                  <span className="text-sm font-medium text-(--neutral-950)">
                    Requires Signature
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <Switch
                    checked={form.watch("active")}
                    onCheckedChange={(checked) =>
                      form.setValue("active", checked)
                    }
                  />
                  <span className="text-sm font-medium text-(--neutral-950)">
                    Active
                  </span>
                </div>

                <div className="flex items-center gap-3">
                  <Switch
                    checked={form.watch("isSystemTemplate")}
                    onCheckedChange={(checked) =>
                      form.setValue("isSystemTemplate", checked)
                    }
                  />
                  <span className="text-sm font-medium text-(--neutral-950)">
                    System Template
                  </span>
                </div>
              </div>
            </form>
            </Form>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end items-center gap-3 p-6 pt-4 border-t border-(--neutral-100)">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isSubmitting}
            className="px-8 h-12 border-(--neutral-200) text-(--text-neutral-600) rounded-full cursor-pointer hover:bg-(--neutral-50)"
          >
            Cancel
          </Button>
          <Button
            type="submit"
            onClick={form.handleSubmit(onSubmit)}
            className="px-8 h-12 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full cursor-pointer"
            disabled={!form.formState.isValid || isSubmitting || isBootstrapping}
            loading={isSubmitting}
            loadingLabel={mode === "edit"
                ? "Updating..."
                : "Creating..."}
          >
            {mode === "edit"
                ? "Update Template"
                : "Create Template"}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default CreateFormTemplateModal;
