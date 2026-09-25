import { useEffect } from "react";
import { X } from "lucide-react";
import { useForm, type SubmitHandler } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { Switch } from "@/components/ui/switch";
import { Controller } from "react-hook-form";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomSelect from "@/components/form/CustomSelect";
import type { CustomSelectOption } from "@/components/form/CustomSelect";

const schema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Category name is required")
    .max(50, "Category name must be 50 characters or fewer"),
  description: z.string().optional(),
  parentId: z.string().optional(),
  sortOrder: z
    .string()
    .optional()
    .refine((value) => {
      const trimmed = value?.trim();
      if (!trimmed) return true;
      if (!/^\d+$/.test(trimmed)) return false;
      const parsed = Number.parseInt(trimmed, 10);
      return Number.isFinite(parsed) && parsed >= 0 && parsed <= 9999;
    }, {
      message: "Sort order must be a whole number between 0 and 9999.",
    }),
  isActive: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

interface AddLibraryCategoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (data: {
    name: string;
    description?: string;
    parentId?: number;
    sortOrder?: number;
    isActive: boolean;
  }) => Promise<void> | void;
  parentOptions: CustomSelectOption[];
  isSubmitting?: boolean;
  initialData?: {
    id: number;
    name: string;
    description?: string;
    parentId?: number | null;
    sortOrder?: number;
    isActive: boolean;
  } | null;
}

const AddLibraryCategoryModal = ({
  isOpen,
  onClose,
  onAdd,
  parentOptions,
  isSubmitting = false,
  initialData = null,
}: AddLibraryCategoryModalProps) => {
  const isEdit = Boolean(initialData);
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: "onChange",
    defaultValues: {
      name: "",
      description: "",
      parentId: "",
      sortOrder: "",
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
      reset({
        name: initialData?.name ?? "",
        description: initialData?.description ?? "",
        parentId: initialData?.parentId ? String(initialData.parentId) : "",
        sortOrder:
          typeof initialData?.sortOrder === "number"
            ? String(initialData.sortOrder)
            : "",
        isActive: initialData?.isActive ?? true,
      });
    }
  }, [isOpen, reset, initialData]);

  if (!isOpen) return null;

  const onSubmit: SubmitHandler<FormValues> = async (values) => {
    await onAdd({
      name: values.name.trim(),
      description: values.description?.trim() || undefined,
      parentId: values.parentId ? Number(values.parentId) : undefined,
      sortOrder: values.sortOrder?.trim()
        ? Number.parseInt(values.sortOrder.trim(), 10)
        : undefined,
      isActive: values.isActive,
    });
  };

  return (
    <div className="app-modal-overlay fixed inset-0 z-100 flex items-center justify-center p-4 backdrop-blur-[0.125rem]">
      <div className="app-modal-surface relative flex max-h-[90vh] w-full max-w-147 flex-col overflow-hidden rounded-xl transition-all duration-300 animate-in fade-in zoom-in-95">
        <button
          onClick={onClose}
          className="absolute top-6 right-6 z-10 p-1.5 hover:bg-slate-100 rounded-full transition-colors duration-300 cursor-pointer text-(--text-neutral-500)"
        >
          <X size={20} />
        </button>

        <div className="shrink-0 px-5 pt-5 pr-16">
          <div className="mb-4">
            <h2 className="text-xl font-bold text-(--text-primary-dark)">
              {isEdit ? "Edit Library Category" : "Add Library Category"}
            </h2>
            <p className="text-sm text-(--text-neutral-500) mt-1">
              {isEdit
                ? "Update existing content library category."
                : "Create a new content library category."}
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
                <CustomInput control={control} name="name" label="Category Name" required maxLength={50} />
                <CustomTextarea control={control} name="description" label="Description" />
                <CustomSelect
                  control={control}
                  name="parentId"
                  label="Parent Category"
                  options={parentOptions}
                  placeholder="Search categories..."
                  isSearch
                />
                <CustomInput
                  control={control}
                  name="sortOrder"
                  label="Sort Order"
                  digitsOnly
                  inputMode="numeric"
                  hint="Optional. Whole number 0-9999"
                  maxLength={4}
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
                          Show this category in library category lists.
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

export default AddLibraryCategoryModal;
