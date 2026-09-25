import { useForm } from "react-hook-form";
import { useEffect, useRef } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "@/components/form/CustomSelect";
import CustomTextarea from "@/components/form/CustomTextarea";
import { Form } from "@/components/ui/form";
import { ASSESSMENT_CATEGORIES } from "../../pages/admin/content/content.static";
import {
  createTemplateFormSchema,
  type CreateTemplateFormValues,
} from "@/schemas/admin-assessment.schemas";

interface CreateTemplateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreateTemplateFormValues) => Promise<boolean | void> | boolean | void;
  isSubmitting?: boolean;
  mode?: "create" | "edit";
  initialValues?: Partial<CreateTemplateFormValues> | null;
}

const CreateTemplateModal = ({
  isOpen,
  onClose,
  onSubmit,
  isSubmitting = false,
  mode = "create",
  initialValues = null,
}: CreateTemplateModalProps) => {
  const seededOpenRef = useRef(false);
  const form = useForm<CreateTemplateFormValues>({
    resolver: zodResolver(createTemplateFormSchema),
    defaultValues: {
      templateName: "",
      category: "Clinical",
      description: "",
      version: "1.0",
      isStandardized: false,
    },
  });

  const categoryOptions = ASSESSMENT_CATEGORIES.filter(
    (cat) => cat !== "All Categories",
  ).map((cat) => ({
    value: cat,
    label: cat,
  }));

  useEffect(() => {
    if (!isOpen) {
      seededOpenRef.current = false;
      return;
    }
    if (mode === "edit" && !initialValues) return;
    if (seededOpenRef.current) return;

    form.reset({
      templateName: initialValues?.templateName ?? "",
      category: initialValues?.category ?? "Clinical",
      description: initialValues?.description ?? "",
      version: initialValues?.version ?? "1.0",
      isStandardized: initialValues?.isStandardized ?? false,
    });
    seededOpenRef.current = true;
  }, [form, initialValues, isOpen, mode]);

  const handleFormSubmit = async (data: CreateTemplateFormValues) => {
    const result = await onSubmit(data);
    if (result === false) return;
    onClose();
    form.reset();
  };

  if (!isOpen) return null;

  const isStandardizedValue = form.watch("isStandardized");

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 backdrop-blur-[0.125rem] transition-all duration-300">
      <div className="w-full max-w-135 bg-white rounded-[1.5rem] shadow-2xl mx-4 overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="flex items-start justify-between px-8 pt-8 pb-2">
          <div className="space-y-1">
            <h2 className="text-[1.375rem] font-bold text-(--neutral-950)">
              {mode === "edit" ? "Edit Assessment Template" : "Create Assessment Template"}
            </h2>
            <p className="text-sm text-(--text-neutral-600)">
              {mode === "edit"
                ? "Update this assessment template."
                : "Create a new assessment template that can be assigned to clients."}
            </p>
          </div>
          <button
            onClick={onClose}
            type="button"
            disabled={isSubmitting}
            className="p-1 text-(--text-neutral-400) hover:text-(--neutral-950) transition-colors cursor-pointer rounded-full hover:bg-(--neutral-50)"
          >
            <X size={24} />
          </button>
        </div>

        {/* Content */}
        <div className="px-8 py-6">
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit(handleFormSubmit)}
              className="space-y-5"
            >
              <div className="space-y-1">
                <CustomInput
                  label="Template Name"
                  name="templateName"
                  control={form.control}
                  required
                />
                <p className="text-xs text-(--text-neutral-400) px-1">
                  e.g., Initial Mental Health Assessment
                </p>
              </div>

              <CustomSelect
                label="Category"
                name="category"
                control={form.control}
                options={categoryOptions}
                required
                isSearch={false}
              />

              <CustomTextarea
                label="Description"
                name="description"
                control={form.control}
                required
                placeholder=" "
                hint="Describe the purpose and scope of this assessment"
              />

              <div className="flex items-center justify-between gap-4 pt-2">
                <div className="w-48">
                  <CustomInput
                    label="Version"
                    name="version"
                    control={form.control}
                    required
                    placeholder=" "
                  />
                </div>

                <div className="flex items-center gap-3 pr-2">
                  <Switch
                    checked={isStandardizedValue}
                    onCheckedChange={(checked) =>
                      form.setValue("isStandardized", checked)
                    }
                  />
                  <span className="text-[0.9375rem] font-medium text-(--text-neutral-600)">
                    Standardized Assessment
                  </span>
                </div>
              </div>

              {/* Footer */}
              <div className="flex justify-end items-center gap-4 pt-4 pb-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={onClose}
                  disabled={isSubmitting}
                  className="px-10 py-3 h-auto border-(--neutral-100) text-(--neutral-950) rounded-full cursor-pointer text-base font-semibold transition-all duration-300 hover:bg-(--neutral-50)"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={!form.formState.isValid || isSubmitting}
                  className="px-6 py-3 h-auto bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/95 text-white rounded-full cursor-pointer text-base font-semibold transition-all duration-300"
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
            </form>
          </Form>
        </div>
      </div>
    </div>
  );
};

export default CreateTemplateModal;
