import { X } from "lucide-react";
import { Form } from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import CustomSelect from "@/components/form/CustomSelect";
import { fieldTypeOptions } from "@/pages/admin/content/content.static";
import type { AddFieldValues } from "@/types/consent.types";
import {
  HeadingConfig,
  InformationConfig,
  BasicTextFieldConfig,
  OptionBasedFieldConfig,
  SingleCheckboxConfig,
  DateFieldConfig,
  SignatureFieldConfig,
  FileUploadFieldConfig,
} from "./FieldConfigGroups";

interface AddFieldModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAdd: (data: AddFieldValues) => void;
  mode?: "add" | "edit";
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any;
  isSubmitting?: boolean;
}

const AddFieldModal = ({
  isOpen,
  onClose,
  onAdd,
  mode = "add",
  form,
  isSubmitting,
}: AddFieldModalProps) => {
  const fieldType = form.watch("type");
  const { isValid } = form.formState;

  if (!isOpen) return null;

  const renderFields = () => {
    switch (fieldType) {
      case "Heading (Read-only)":
        return <HeadingConfig form={form} />;
      case "Information Text (Read-only)":
        return <InformationConfig form={form} />;
      case "Dropdown":
      case "Radio Buttons":
      case "Multiple Checkboxes":
        return <OptionBasedFieldConfig form={form} />;
      case "Single Checkbox":
        return <SingleCheckboxConfig form={form} />;
      case "Date":
        return <DateFieldConfig form={form} />;
      case "Signature":
        return <SignatureFieldConfig form={form} />;
      case "File Upload":
        return <FileUploadFieldConfig form={form} />;
      case "Short Text":
      case "Long Text":
      default:
        return <BasicTextFieldConfig form={form} />;
    }
  };

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/50 backdrop-blur-sm">
      <div className="relative w-full max-w-155 bg-white rounded-2xl shadow-lg mx-4 flex flex-col max-h-[90vh]">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1 hover:bg-(--neutral-50) rounded-full transition-colors duration-300 cursor-pointer text-(--text-neutral-500)"
        >
          <X size={20} />
        </button>
        <div className="px-6 pt-6">
          <div className="space-y-1">
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              {mode === "edit" ? "Edit Form Field" : "Add Form Field"}
            </h2>
            <p className="text-sm text-(--text-neutral-500)">
              {mode === "edit"
                ? "Update this field for the form template"
                : "Configure a new field for this form template"}
            </p>
          </div>
        </div>

        {/* Form Content */}
        <div className="flex-1 overflow-y-auto p-6 custom-scrollbar">
          <Form {...form}>
            <div className="space-y-4">
              <CustomSelect
                control={form.control}
                name="type"
                label="Field Type"
                options={fieldTypeOptions}
              />

              {renderFields()}
            </div>
          </Form>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 pb-6">
          <Button
            variant="ghost"
            onClick={onClose}
            className="px-8 h-12 rounded-full border border-(--neutral-100) text-(--text-neutral-800) hover:bg-(--neutral-50) cursor-pointer"
          >
            Cancel
          </Button>
          <Button
            type="submit"
            onClick={form.handleSubmit(onAdd)}
            className="px-8 h-12 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full cursor-pointer disabled:opacity-50"
            disabled={!isValid || isSubmitting}
            loading={isSubmitting}
            loadingLabel={mode === "edit"
                ? "Updating..."
                : "Adding..."}
          >
            {mode === "edit"
                ? "Update Field"
                : "Add Field"}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default AddFieldModal;
