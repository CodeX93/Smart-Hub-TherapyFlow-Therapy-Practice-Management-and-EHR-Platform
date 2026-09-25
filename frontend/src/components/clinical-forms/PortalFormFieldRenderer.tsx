import CustomDatePicker from "@/components/form/CustomDatePicker";
import CustomInput from "@/components/form/CustomInput";
import CustomRadio from "@/components/form/CustomRadio";
import CustomSelect from "@/components/form/CustomSelect";
import CustomTextarea from "@/components/form/CustomTextarea";
import { Checkbox } from "@/components/ui/checkbox";
import type { PortalFormAssignmentField } from "@/store/api/portalFormsApi";
import {
  normalizePortalFieldType,
  isPortalFormReadOnlyField,
  isPortalFormInformationalTextField,
  isPortalFormSignatureField,
  parsePortalFormFieldOptions,
} from "@/utils/portalFormDisplay";
import {
  applyPortalFormPlaceholders,
  resolvePortalFormFieldContent,
} from "@/utils/portalFormPlaceholders";

export interface PortalClinicalFormValues {
  clientName: string;
  date: string;
  signature: string;
}

interface PortalFormFieldRendererProps {
  field: PortalFormAssignmentField;
  value: string;
  disabled?: boolean;
  placeholders?: Record<string, string>;
  onValueChange: (fieldId: number, value: string) => void;
}

function PortalFormRichText({
  content,
  placeholders,
  className,
}: {
  content: string | null | undefined;
  placeholders: Record<string, string>;
  className?: string;
}) {
  const resolved = resolvePortalFormFieldContent(content, placeholders);
  if (!resolved.plain && !resolved.html) return null;

  if (resolved.html) {
    return (
      <div
        className={
          className ??
          "portal-form-rich-text text-sm leading-relaxed text-(--text-primary-dark) [&_p]:mb-3 [&_p:last-child]:mb-0 [&_ul]:my-3 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:my-3 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:mb-1 [&_strong]:font-semibold"
        }
        dangerouslySetInnerHTML={{ __html: resolved.html }}
      />
    );
  }

  return (
    <p
      className={
        className ??
        "text-sm leading-relaxed text-(--text-primary-dark) whitespace-pre-wrap"
      }
    >
      {resolved.plain}
    </p>
  );
}

const PortalFormFieldRenderer = ({
  field,
  value,
  disabled = false,
  placeholders = {},
  onValueChange,
}: PortalFormFieldRendererProps) => {
  const fieldType = normalizePortalFieldType(field.fieldType);
  const filledHelpText = applyPortalFormPlaceholders(field.helpText, placeholders);
  const filledPlaceholder = applyPortalFormPlaceholders(
    field.placeholder,
    placeholders,
  );

  if (isPortalFormSignatureField(field)) {
    return null;
  }

  if (fieldType === "heading" || isPortalFormInformationalTextField(field)) {
    return (
      <section>
        {field.label ? (
          <h2
            className={
              fieldType === "heading"
                ? "text-lg font-semibold text-(--text-primary-dark)"
                : "mb-2 text-base font-semibold text-(--text-primary-dark)"
            }
          >
            {field.label}
          </h2>
        ) : null}
        <PortalFormRichText
          content={
            fieldType === "heading"
              ? field.helpText
              : field.helpText || field.placeholder || field.label
          }
          placeholders={placeholders}
          className={
            fieldType === "heading"
              ? "mt-2 text-sm text-(--text-neutral-600) [&_p]:mb-2 [&_p:last-child]:mb-0"
              : undefined
          }
        />
      </section>
    );
  }

  if (fieldType === "info_text") {
    return (
      <section>
        {field.label ? (
          <h3 className="mb-2 text-base font-semibold text-(--text-primary-dark)">
            {field.label}
          </h3>
        ) : null}
        <PortalFormRichText
          content={field.helpText || field.placeholder || field.label}
          placeholders={placeholders}
        />
      </section>
    );
  }

  if (fieldType === "textarea") {
    return (
      <section className="space-y-2">
        {filledHelpText && filledHelpText.length > 120 ? (
          <PortalFormRichText
            content={field.helpText}
            placeholders={placeholders}
            className="text-sm leading-relaxed text-(--text-neutral-600) whitespace-pre-wrap [&_p]:mb-2 [&_p:last-child]:mb-0"
          />
        ) : null}
        <CustomTextarea
          id={`portal-form-field-${field.id}`}
          name={`field-${field.id}`}
          label={field.label}
          required={field.isRequired}
          value={value}
          disabled={disabled}
          placeholder={filledPlaceholder || undefined}
          hint={
            filledHelpText && filledHelpText.length <= 120
              ? filledHelpText
              : undefined
          }
          onChange={(event) => onValueChange(field.id, event.target.value)}
        />
      </section>
    );
  }

  if (fieldType === "select") {
    const options = parsePortalFormFieldOptions(field.options);

    return (
      <section>
        <CustomSelect
          label={field.label}
          required={field.isRequired}
          value={value}
          disabled={disabled}
          onChange={(nextValue) => onValueChange(field.id, nextValue)}
          options={options.map((option) => ({ value: option, label: option }))}
          placeholder="Select an option"
          isSearch={false}
        />
      </section>
    );
  }

  if (fieldType === "radio") {
    const options = parsePortalFormFieldOptions(field.options);

    return (
      <section>
        <CustomRadio
          label={field.label}
          name={`field-${field.id}`}
          options={options}
          value={value}
          required={field.isRequired}
          disabled={disabled}
          onChange={(nextValue) => onValueChange(field.id, nextValue)}
        />
      </section>
    );
  }

  if (fieldType === "checkbox") {
    const checkboxId = `portal-form-field-${field.id}`;

    return (
      <section>
        <label
          htmlFor={checkboxId}
          className="flex cursor-pointer items-start gap-3 text-sm text-(--text-primary-dark)"
        >
          <Checkbox
            id={checkboxId}
            name={`field-${field.id}`}
            checked={value === "true"}
            disabled={disabled}
            required={field.isRequired}
            onCheckedChange={(checked) =>
              onValueChange(field.id, checked ? "true" : "false")
            }
            className="mt-0.5 shrink-0"
          />
          <span>
            {field.label}
            {field.isRequired ? <span className="text-red-500"> *</span> : null}
          </span>
        </label>
        {field.helpText ? (
          <div className="mt-2 pl-7">
            <PortalFormRichText
              content={field.helpText}
              placeholders={placeholders}
              className="text-xs text-(--text-neutral-600) whitespace-pre-wrap [&_p]:mb-2 [&_p:last-child]:mb-0"
            />
          </div>
        ) : null}
      </section>
    );
  }

  if (fieldType === "multi_select") {
    const options = parsePortalFormFieldOptions(field.options);
    const selectedValues = value
      ? value.split("||").map((entry) => entry.trim()).filter(Boolean)
      : [];

    return (
      <fieldset className="space-y-3" aria-required={field.isRequired}>
        <legend className="text-sm font-medium text-(--text-primary-dark)">
          {field.label}
          {field.isRequired ? <span className="text-red-500"> *</span> : null}
        </legend>
        <div className="space-y-2">
          {options.map((option, index) => {
            const optionId = `portal-form-field-${field.id}-option-${index}`;

            return (
              <label
                key={option}
                htmlFor={optionId}
                className="flex cursor-pointer items-center gap-2 text-sm text-(--text-primary-dark)"
              >
                <Checkbox
                  id={optionId}
                  name={`field-${field.id}`}
                  checked={selectedValues.includes(option)}
                  disabled={disabled}
                  onCheckedChange={(checked) => {
                    const nextValues = checked
                      ? [...selectedValues, option]
                      : selectedValues.filter((entry) => entry !== option);
                    onValueChange(field.id, nextValues.join("||"));
                  }}
                />
                <span>{option}</span>
              </label>
            );
          })}
        </div>
      </fieldset>
    );
  }

  if (fieldType === "date") {
    return (
      <section className="space-y-2">
        <CustomDatePicker
          label={field.label}
          required={field.isRequired}
          disabled={disabled}
          date={value ? new Date(value) : null}
          onDateChange={(nextDate) =>
            onValueChange(
              field.id,
              nextDate ? nextDate.toISOString().slice(0, 10) : "",
            )
          }
        />
      </section>
    );
  }

  if (isPortalFormReadOnlyField(field)) {
    return null;
  }

  const showHelpAsBody = Boolean(filledHelpText && filledHelpText.length > 80);

  return (
    <section className="space-y-2">
      {showHelpAsBody ? (
        <>
          <h3 className="text-base font-semibold text-(--text-primary-dark)">
            {field.label}
            {field.isRequired ? <span className="text-red-500"> *</span> : null}
          </h3>
          <PortalFormRichText
            content={field.helpText}
            placeholders={placeholders}
          />
          <CustomInput
            id={`portal-form-field-${field.id}`}
            name={`field-${field.id}`}
            label=""
            required={field.isRequired}
            disabled={disabled}
            placeholder={filledPlaceholder || `Enter ${field.label}`}
            value={value}
            onChange={(event) => onValueChange(field.id, event.target.value)}
          />
        </>
      ) : (
        <CustomInput
          id={`portal-form-field-${field.id}`}
          name={`field-${field.id}`}
          label={field.label}
          required={field.isRequired}
          disabled={disabled}
          placeholder={filledPlaceholder || undefined}
          hint={filledHelpText || undefined}
          value={value}
          onChange={(event) => onValueChange(field.id, event.target.value)}
        />
      )}
    </section>
  );
};

export default PortalFormFieldRenderer;
