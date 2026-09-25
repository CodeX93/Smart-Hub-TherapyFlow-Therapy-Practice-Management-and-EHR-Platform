import { useEffect } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "../form/CustomInput";
import CustomTextarea from "../form/CustomTextarea";
import { Switch } from "@/components/ui/switch";
import { useForm, type SubmitHandler } from "react-hook-form";
import { Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form } from "@/components/ui/form";
import {
  categorySchema,
  type CategoryFormData,
} from "@/schemas/settings.schema";

interface AddCategoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (data: {
    key: string;
    name: string;
    description: string;
    isSystem: boolean;
    isActive: boolean;
  }) => void | Promise<void>;
  initialData?: {
    key: string;
    name: string;
    description?: string;
    isSystem?: boolean;
    isActive?: boolean;
  };
  isSubmitting?: boolean;
}

const AddCategoryModal = ({
  isOpen,
  onClose,
  onAdd,
  initialData,
  isSubmitting = false,
}: AddCategoryModalProps) => {
  const isEdit = !!initialData;

  const form = useForm<CategoryFormData>({
    resolver: zodResolver(categorySchema),
    mode: "onChange",
    defaultValues: {
      key: "",
      name: "",
      description: "",
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
          key: initialData.key,
          name: initialData.name,
          description: initialData.description || "",
          isSystem: initialData.isSystem ?? false,
          isActive: initialData.isActive ?? true,
        });
      } else {
        reset({
          key: "",
          name: "",
          description: "",
          isSystem: false,
          isActive: true,
        });
      }
    }
  }, [initialData, isOpen, reset]);

  if (!isOpen) return null;

  const onSubmit: SubmitHandler<CategoryFormData> = async (data) => {
    await onAdd(data);
  };

  return (
    <div className="app-modal-overlay fixed inset-0 z-100 flex items-center justify-center p-4 backdrop-blur-[0.125rem]">
      <div className="app-modal-surface relative flex max-h-[90vh] w-full max-w-147 flex-col overflow-hidden rounded-xl transition-all duration-300 animate-in fade-in zoom-in-95">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-6 right-6 z-10 p-1.5 hover:bg-slate-100 rounded-full transition-colors duration-300 cursor-pointer text-(--text-neutral-500)"
        >
          <X size={20} />
        </button>

        <div className="shrink-0 px-5 pt-5 pr-16">
          {/* Header */}
          <div className="mb-4">
            <h2 className="text-xl font-bold text-(--text-primary-dark)">
              {isEdit ? "Edit Category" : "Add New Category"}
            </h2>
            <p className="text-sm text-(--text-neutral-500) mt-1">
              {isEdit
                ? "Update option category details"
                : "Create a new option category for dropdown selections"}
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
            <CustomInput
              control={control}
              name="key"
              label="Category Key"
              required
              hint="e.g. therapy_types"
              maxLength={50}
            />

            <div className="mt-3">
              <CustomInput
                control={control}
                name="name"
                label="Category Name"
                required
                hint="e.g. Therapy Types"
                maxLength={50}
              />
            </div>

            <CustomTextarea
              control={control}
              name="description"
              label="Description"
            />
            <div className="grid grid-cols-2 gap-4">
              <Controller
                control={control}
                name="isSystem"
                render={({ field }) => (
                  <div className="flex items-center justify-between rounded-xl border border-(--neutral-200) px-4 py-3">
                    <div>
                      <p className="text-sm font-semibold text-(--text-primary-dark)">
                        System Category
                      </p>
                      <p className="text-xs text-(--text-neutral-500)">
                        Mark as protected system type.
                      </p>
                    </div>
                    <Switch
                      checked={Boolean(field.value)}
                      onCheckedChange={field.onChange}
                      disabled={isSubmitting}
                    />
                  </div>
                )}
              />
              <Controller
                control={control}
                name="isActive"
                render={({ field }) => (
                  <div className="flex items-center justify-between rounded-xl border border-(--neutral-200) px-4 py-3">
                    <div>
                      <p className="text-sm font-semibold text-(--text-primary-dark)">
                        Active
                      </p>
                      <p className="text-xs text-(--text-neutral-500)">
                        Show category in system option lists.
                      </p>
                    </div>
                    <Switch
                      checked={Boolean(field.value)}
                      onCheckedChange={field.onChange}
                      disabled={isSubmitting}
                    />
                  </div>
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
                loadingLabel={isEdit ? "Updating..." : "Adding..."}
              >
                {isEdit ? "Update Category" : "Add Category"}
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default AddCategoryModal;
