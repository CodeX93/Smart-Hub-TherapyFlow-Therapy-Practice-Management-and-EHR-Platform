import { useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { Form } from "@/components/ui/form";
import AddEntryFormFields from "./AddEntryFormFields";
import SmartConnectSection from "./SmartConnectSection";
import CustomSelect from "@/components/form/CustomSelect";
import CustomTextarea from "@/components/form/CustomTextarea";
import {
  addEntrySchema,
  type AddEntryFormData,
} from "@/schemas/admin-library.schema";

interface AddEntryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (data: AddEntryFormData) => Promise<void> | void;
  categories: { value: string; label: string }[];
  initialData?: AddEntryFormData | null;
  isSubmitting?: boolean;
  mode?: "create" | "edit";
}

const AddEntryModal = ({
  isOpen,
  onClose,
  onAdd,
  categories,
  initialData,
  isSubmitting = false,
  mode = "create",
}: AddEntryModalProps) => {
  const modalContainerRef = useRef<HTMLDivElement>(null);
  const form = useForm<AddEntryFormData>({
    resolver: zodResolver(addEntrySchema),
    defaultValues: {
      title: "",
      content: "",
      category: "",
      tags: "",
      sortOrder: 0,
      smartConnect: [],
      connectionType: "RELATED",
      connectionDescription: "",
    },
  });

  useEffect(() => {
    if (isOpen) {
      form.reset({
        title: initialData?.title ?? "",
        content: initialData?.content ?? "",
        category: initialData?.category ?? "",
        tags: initialData?.tags ?? "",
        sortOrder: initialData?.sortOrder ?? 0,
        smartConnect: initialData?.smartConnect ?? [],
        connectionType: initialData?.connectionType ?? "RELATED",
        connectionDescription: initialData?.connectionDescription ?? "",
      });
    }
  }, [isOpen, form, initialData]);

  if (!isOpen) return null;

  const onSubmit = async (values: AddEntryFormData) => {
    await onAdd(values);
  };

  const titleValue = form.watch("title");
  const smartConnectValue = form.watch("smartConnect");
  const smartConnectCount = smartConnectValue?.length ?? 0;
  const connectionTypeOptions = [
    { value: "RELATED", label: "Related" },
    { value: "REFERENCE", label: "Reference" },
    { value: "DERIVED", label: "Derived" },
    { value: "SUPPLEMENT", label: "Supplement" },
    { value: "OTHER", label: "Other" },
  ];
  const smartConnectTabs = categories.map((category) => ({
    id: category.value,
    name: category.label,
  }));

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div
        ref={modalContainerRef}
        className="relative w-full max-w-155 bg-white rounded-2xl shadow-lg mx-4 flex flex-col max-h-[90vh] overflow-hidden"
      >
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit)}
            className="flex flex-col h-full overflow-hidden"
          >
            <div className="sticky top-0 z-10 flex items-center justify-between rounded-t-2xl bg-white px-6 py-6">
              <h2 className="text-xl font-bold text-(--neutral-950)">
                {mode === "edit" ? "Edit Entry" : "Add New Entry"}
              </h2>
              <button
                type="button"
                onClick={onClose}
                disabled={isSubmitting}
                className="text-(--text-neutral-600) hover:text-(--neutral-950) transition-colors duration-300 cursor-pointer hover:bg-(--neutral-50) p-1.5 rounded-full"
              >
                <X size={20} />
              </button>
            </div>

            {/* Scrollable Content */}
            <div className="p-6 overflow-y-auto custom-scrollbar flex-1">
              <div className="flex flex-col gap-5">
                <AddEntryFormFields
                  control={form.control}
                  categories={categories}
                  portalContainerRef={modalContainerRef}
                />

                <SmartConnectSection
                  title={titleValue}
                  selectedIds={smartConnectValue || []}
                  onChange={(ids) => form.setValue("smartConnect", ids)}
                  categories={smartConnectTabs}
                />

                {smartConnectCount > 0 ? (
                  <div className="border border-(--neutral-100) rounded-xl p-4 space-y-4">
                    <p className="text-sm font-semibold text-(--neutral-950)">
                      Connection Details ({smartConnectCount})
                    </p>
                    <CustomSelect
                      control={form.control}
                      name="connectionType"
                      label="Connection Type"
                      options={connectionTypeOptions}
                      required
                      closeOnScroll
                      portalContainerRef={modalContainerRef}
                    />
                    <CustomTextarea
                      control={form.control}
                      name="connectionDescription"
                      label="Connection Description"
                      placeholder="Add details for these connections..."
                      className="min-h-24"
                    />
                  </div>
                ) : null}
              </div>
            </div>

            {/* Footer */}
            <div className="flex justify-end items-center gap-3 p-6">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isSubmitting}
                className="px-6 py-2 h-10 border-(--neutral-100) bg-white hover:bg-(--neutral-50) text-(--text-neutral-600) rounded-full cursor-pointer text-sm font-normal"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={!form.formState.isValid || isSubmitting}
                className={cn(
                  "px-6 py-2 h-10 rounded-full cursor-pointer text-sm font-normal transition-all duration-300",
                  form.formState.isValid && !isSubmitting
                    ? "bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/95 text-white"
                    : "bg-(--neutral-200) text-(--text-neutral-400) cursor-not-allowed",
                )}
                loading={isSubmitting}
                loadingLabel={mode === "edit"
                    ? "Updating..."
                    : "Adding..."}
              >
                {mode === "edit"
                    ? "Update"
                    : `Add${smartConnectCount > 0 ? ` + ${smartConnectCount} connections` : ""}`}
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </div>
  );
};

export default AddEntryModal;
