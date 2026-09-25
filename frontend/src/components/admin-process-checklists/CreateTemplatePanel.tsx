
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, useEffect, useRef } from "react";
import { useForm, useFieldArray, type FieldErrors } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X, Plus } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomItemsList from "./CustomItemsList";
import { Form } from "@/components/ui/form";
import {
  createTemplateSchema,
  type CreateTemplateValues,
} from "@/schemas/admin-checklist.schemas";
interface CreateTemplatePanelProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: CreateTemplateValues) => void;
  isLoading?: boolean;
  initialData?: CreateTemplateValues | null;
  mode?: "create" | "edit";
  isFetching?: boolean;
}

const CreateTemplatePanelContent = ({
  isOpen,
  onClose,
  onSubmit,
  isLoading,
  initialData,
  mode = "create",
  isFetching,
}: CreateTemplatePanelProps) => {
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());
  const seededOpenRef = useRef(false);

  const form = useForm<CreateTemplateValues>({
    resolver: zodResolver(createTemplateSchema),
    defaultValues: {
      templateName: "",
      description: "",
      customItems: [],
    },
  });

  const fieldArray = useFieldArray({
    control: form.control,
    name: "customItems",
  });

  const isBootstrapping = mode === "edit" && Boolean(isFetching) && !initialData;

  useEffect(() => {
    if (!isOpen) {
      seededOpenRef.current = false;
      return;
    }
    if (mode === "edit" && !initialData) return;
    if (seededOpenRef.current) return;

    if (initialData) {
      form.reset(initialData);
    } else {
      form.reset({
        templateName: "",
        description: "",
        customItems: [],
      });
    }
    seededOpenRef.current = true;
  }, [isOpen, form, initialData, mode]);

  const handleAddCustomItem = () => {
    const id = `custom-${Date.now()}`;
    fieldArray.append({
      id,
      title: "",
      category: "",
      description: "",
      required: false,
    });
    setExpandedItems(new Set([...expandedItems, id]));
  };

  const handleValidSubmit = (values: CreateTemplateValues) => {
    onSubmit(values);
    onClose();
  };

  const handleInvalidSubmit = (errors: FieldErrors<CreateTemplateValues>) => {
    const itemErrors = errors.customItems;
    if (!itemErrors) return;

    const idsToExpand = new Set<string>();
    fieldArray.fields.forEach((field, index) => {
      const entry = Array.isArray(itemErrors) ? itemErrors[index] : undefined;
      if (entry) {
        idsToExpand.add(field.id);
      }
    });

    if (idsToExpand.size > 0) {
      setExpandedItems(idsToExpand);
    }
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className={cn(
          "fixed inset-0 bg-black/50 z-50 transition-opacity duration-300",
          isOpen ? "opacity-100" : "opacity-0 pointer-events-none",
        )}
        onClick={onClose}
      />

      {/* Sidebar Panel */}
      <div
        className={cn(
          "fixed right-0 top-0 h-full w-full max-w-125 flex flex-col bg-white z-50 shadow-2xl overflow-hidden transition-transform duration-300",
          isOpen ? "translate-x-0" : "translate-x-full",
        )}
      >
        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(handleValidSubmit, handleInvalidSubmit)}
            className="flex flex-col h-full overflow-hidden"
          >
            {/* Header */}
            <div className="shrink-0 flex items-center justify-between p-6 border-b border-(--neutral-100) bg-white">
              <h2 className="text-xl font-semibold text-(--text-primary-dark)">
                {mode === "edit"
                  ? "Edit Checklist Template"
                  : "Create Checklist Template"}
              </h2>
              <button
                type="button"
                onClick={onClose}
                className="text-(--text-neutral-400) hover:text-(--text-neutral-600) transition-colors duration-200 cursor-pointer"
              >
                <X size={24} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto custom-scrollbar">
              {/* Form Content */}
              <div className="p-6 space-y-6">
                {isBootstrapping ? (
                  <div className="py-20 flex justify-center">
                    <ContentLoader variant="inline" size="md" />
                  </div>
                ) : (
                  <>
                    {/* Template Name */}
                    <CustomInput
                      control={form.control}
                      name="templateName"
                      label="Template Name"
                      required
                      maxLength={120}
                    />

                    {/* Description */}
                    <div className="space-y-1.5">
                      <CustomTextarea
                        control={form.control}
                        name="description"
                        label="Description"
                        rows={4}
                      />
                      <p className="text-xs text-(--text-neutral-500)">
                        Describe the checklist purpose and usage
                      </p>
                    </div>

                    {/* Add Custom Item Button */}
                    <button
                      type="button"
                      onClick={handleAddCustomItem}
                      className="flex items-center gap-2 text-sm text-(--text-primary-500) font-semibold hover:opacity-80 transition-opacity cursor-pointer"
                    >
                      <Plus size={16} />
                      Add Custom Checklist Item
                    </button>

                    {/* Custom Items */}
                    {fieldArray.fields.length > 0 && (
                      <CustomItemsList
                        control={form.control}
                        fieldArray={fieldArray}
                        expandedItems={expandedItems}
                        setExpandedItems={setExpandedItems}
                      />
                    )}
                  </>
                )}
              </div>
            </div>

            {/* Footer */}
            <div className="shrink-0 flex items-center justify-end gap-3 p-6 bg-white border-t border-(--neutral-100)">
              <Button
                type="button"
                variant="ghost"
                onClick={onClose}
                className="px-6 h-11.5 rounded-full border border-(--neutral-100) text-(--text-neutral-800) hover:bg-(--neutral-50) cursor-pointer"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isLoading || isBootstrapping}
                className="px-6 h-11.5 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full cursor-pointer disabled:opacity-70"
                loading={isLoading}
                loadingLabel={mode === "edit" ? "Updating..." : "Creating..."}
              >
                {mode === "edit" ? (
                  "Update Template"
                ) : (
                  "Create Template"
                )}
              </Button>
            </div>
          </form>
        </Form>
      </div>
    </>
  );
};

const CreateTemplatePanel = (props: CreateTemplatePanelProps) => props.isOpen ? <CreateTemplatePanelContent {...props} /> : null;

export default CreateTemplatePanel;
