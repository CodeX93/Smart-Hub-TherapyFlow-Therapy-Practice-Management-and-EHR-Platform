import { TrashIcon } from "@/components/icons/commonIcons";
import { useEffect, useState } from "react";
import { FormLabel } from "@/components/ui/form";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomJoditEditor from "@/components/shared/CustomJoditEditor";
import AutofillVariablesAccordion from "./AutofillVariablesAccordion";
import RequiredFieldCheckbox from "./RequiredFieldCheckbox";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";

interface ConfigProps {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  form: any;
}

export const HeadingConfig = ({ form }: ConfigProps) => (
  <>
    <CustomInput
      control={form.control}
      name="headingText"
      label="Heading Text"
      hint="e.g., Informed Consent for Treatment"
      required
    />
    <CustomTextarea
      control={form.control}
      name="helpText"
      label="Help Text"
      hint="Optional help text for the users"
    />
  </>
);

export const InformationConfig = ({ form }: ConfigProps) => (
  <>
    <CustomInput
      control={form.control}
      name="sectionTitle"
      label="Section Title"
      hint="e.g., Important Information"
    />
    <div className="space-y-1.5">
      <FormLabel className="text-sm font-medium text-(--text-neutral-800)">
        Content Text <span className="text-red-500">*</span>
      </FormLabel>
      <div className="overflow-auto max-h-60 border border-(--neutral-200) rounded-xl bg-white">
        <CustomJoditEditor
          content={form.watch("contentText") || ""}
          setContent={(content) =>
            form.setValue("contentText", content, {
              shouldValidate: true,
              shouldDirty: true,
              shouldTouch: true,
            })
          }
          placeholder="Enter information content..."
        />
      </div>
    </div>
  </>
);

export const FillInTheBlankConfig = ({ form }: ConfigProps) => (
  <>
    <CustomInput
      control={form.control}
      name="label"
      label="Field Label"
      required
    />
    <CustomTextarea
      control={form.control}
      name="templateText"
      label="Template Text"
      required
      hint="Use {{VARIABLE_NAME}} for auto-filled data (UPPERCASE) or [field_name] for manual input (lowercase)"
    />
    <AutofillVariablesAccordion />
    <RequiredFieldCheckbox control={form.control} />
  </>
);

const AdvancedConfig = ({ form }: ConfigProps) => (
  <div className="pt-4 border-t border-(--neutral-100) mt-4 space-y-4">
    <h4 className="text-sm font-medium text-(--text-primary-dark)">Advanced Configuration</h4>
    <div className="grid grid-cols-2 gap-4">
      <CustomInput
        control={form.control}
        name="defaultValue"
        label="Default Value"
        hint="Initial value for the field"
      />
      <CustomInput
        control={form.control}
        name="autoPopulate"
        label="Auto-Populate"
        hint="e.g., client_name"
      />
    </div>
    <CustomTextarea
      control={form.control}
      name="conditionalDisplay"
      label="Conditional Display"
      hint="JSON logic e.g., {'showIf': {'fieldId': 1, 'value': 'yes'}}"
    />
    <CustomTextarea
      control={form.control}
      name="validation"
      label="Validation Rules"
      hint="JSON logic e.g., {'minLength': 2, 'maxLength': 100}"
    />
  </div>
);

export const OptionBasedFieldConfig = ({ form }: ConfigProps) => (
  <OptionBasedFieldConfigInner form={form} />
);

const OptionBasedFieldConfigInner = ({ form }: ConfigProps) => {
  const fieldType = form.watch("type");
  const marker = fieldType === "Multiple Checkboxes" ? "checkbox" : "radio";
  const [options, setOptions] = useState<string[]>([""]);

  useEffect(() => {
    const rawOptions = (form.watch("options") as string | undefined) ?? "";
    if (!rawOptions.trim()) {
      setOptions([""]);
      return;
    }
    const parsed = rawOptions
      .split(/\r?\n|,/)
      .map((item) => item.trim())
      .filter(Boolean);
    setOptions(parsed.length > 0 ? parsed : [""]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const syncOptions = (next: string[]) => {
    setOptions(next);
    const serialized = next.map((item) => item.trim()).filter(Boolean).join(", ");
    form.setValue("options", serialized, {
      shouldValidate: true,
      shouldDirty: true,
      shouldTouch: true,
    });
  };

  const handleOptionChange = (index: number, value: string) => {
    const next = [...options];
    next[index] = value;
    syncOptions(next);
  };

  const addOption = () => {
    syncOptions([...options, ""]);
  };

  const removeOption = (index: number) => {
    if (options.length === 1) return;
    const next = options.filter((_, i) => i !== index);
    syncOptions(next.length ? next : [""]);
  };

  return (
    <>
      <CustomInput
        control={form.control}
        name="label"
        label="Field Label"
        required
        hint="What the user will see for this question"
      />
      <CustomTextarea
        control={form.control}
        name="helpText"
        label="Help Text"
        hint="Optional help text for the users"
      />

      <div className="space-y-3">
        <FormLabel className="text-sm font-medium text-(--text-neutral-800)">
          Options <span className="text-red-500">*</span>
        </FormLabel>
        <div className="space-y-2">
          {options.map((option, index) => (
            <div key={`opt-${index}`} className="flex items-center gap-2">
              <span className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-(--neutral-200) bg-(--neutral-50) text-(--text-neutral-500)">
                <input
                  type={marker}
                  disabled
                  className="h-4 w-4 accent-(--bg-primary-dark)"
                />
              </span>
              <div className="flex-1">
                <CustomInput
                  value={option}
                  onChange={(event) => handleOptionChange(index, event.target.value)}
                  label={`Option ${index + 1}`}
                  required={index === 0}
                />
              </div>
              <Button
                type="button"
                variant="ghost"
                onClick={() => removeOption(index)}
                disabled={options.length === 1}
                className="h-10 w-10 p-0 rounded-full text-(--text-neutral-500) hover:text-red-600 disabled:opacity-40 cursor-pointer"
              >
                <TrashIcon size={16} />
              </Button>
            </div>
          ))}
        </div>
        <Button
          type="button"
          variant="outline"
          onClick={addOption}
          className="h-10 rounded-full border-(--neutral-200) text-(--bg-primary-dark) cursor-pointer"
        >
          <Plus size={14} className="mr-1" />
          Add Option
        </Button>
      </div>

      <RequiredFieldCheckbox control={form.control} />
      <AdvancedConfig form={form} />
    </>
  );
};

export const BasicTextFieldConfig = ({ form }: ConfigProps) => (
  <>
    <CustomInput
      control={form.control}
      name="label"
      label="Field Label"
      required
    />
    <CustomInput
      control={form.control}
      name="placeholder"
      label="Placeholder Text"
    />
    <CustomTextarea
      control={form.control}
      name="helpText"
      label="Help Text"
      hint="Optional help text for the users"
    />
    <RequiredFieldCheckbox control={form.control} />
    <AdvancedConfig form={form} />
  </>
);

export const SingleCheckboxConfig = ({ form }: ConfigProps) => (
  <>
    <CustomInput
      control={form.control}
      name="label"
      label="Checkbox Label"
      required
      hint="Example: I agree to the terms and conditions"
    />
    <CustomTextarea
      control={form.control}
      name="helpText"
      label="Help Text"
      hint="Optional help text for the users"
    />
    <RequiredFieldCheckbox control={form.control} />
    <AdvancedConfig form={form} />
  </>
);

export const DateFieldConfig = ({ form }: ConfigProps) => (
  <>
    <CustomInput
      control={form.control}
      name="label"
      label="Field Label"
      required
    />
    <CustomInput
      control={form.control}
      name="placeholder"
      label="Placeholder Text"
      hint="Example: Select date"
    />
    <CustomTextarea
      control={form.control}
      name="helpText"
      label="Help Text"
      hint="Optional help text for the users"
    />
    <RequiredFieldCheckbox control={form.control} />
    <AdvancedConfig form={form} />
  </>
);

export const SignatureFieldConfig = ({ form }: ConfigProps) => (
  <>
    <CustomInput
      control={form.control}
      name="label"
      label="Signature Label"
      required
      hint="Example: Client Signature"
    />
    <CustomTextarea
      control={form.control}
      name="helpText"
      label="Help Text"
      hint="Optional help text for the users"
    />
    <RequiredFieldCheckbox control={form.control} />
    <AdvancedConfig form={form} />
  </>
);

export const FileUploadFieldConfig = ({ form }: ConfigProps) => (
  <>
    <CustomInput
      control={form.control}
      name="label"
      label="File Upload Label"
      required
      hint="Example: Upload supporting document"
    />
    <CustomTextarea
      control={form.control}
      name="helpText"
      label="Help Text"
      hint="You can mention allowed file formats or size limit"
    />
    <RequiredFieldCheckbox control={form.control} />
    <AdvancedConfig form={form} />
  </>
);
