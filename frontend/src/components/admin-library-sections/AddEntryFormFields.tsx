import type { Control } from "react-hook-form";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomSelect from "@/components/form/CustomSelect";
import type { AddEntryFormData } from "@/schemas/admin-library.schema";

interface AddEntryFormFieldsProps {
  control: Control<AddEntryFormData>;
  categories: { value: string; label: string }[];
  portalContainerRef?: React.RefObject<HTMLElement | null>;
}

const AddEntryFormFields = ({
  control,
  categories,
  portalContainerRef,
}: AddEntryFormFieldsProps) => {
  return (
    <div className="flex flex-col gap-5">
      <CustomInput
        control={control}
        name="title"
        label="Title"
        required
        maxLength={120}
        hint="Maximum 120 characters"
      />

      <CustomTextarea
        control={control}
        name="content"
        label="Content"
        required
        className="min-h-30"
      />

      <CustomSelect
        control={control}
        name="category"
        label="Category"
        options={categories}
        required
        closeOnScroll
        portalContainerRef={portalContainerRef}
      />
      <div className="space-y-1.5">
        <CustomInput
          control={control}
          name="tags"
          label="Tags (comma-separated)"
          required
        />
        <p className="text-xs text-(--text-neutral-600) px-1">
          e.g., therapy, anxiety, CBT, etc.
        </p>
      </div>

      <CustomInput
        control={control}
        name="sortOrder"
        label="Sort Order"
        type="number"
      />
    </div>
  );
};

export default AddEntryFormFields;
