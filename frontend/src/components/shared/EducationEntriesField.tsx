import { TrashIcon } from "@/components/icons/commonIcons";
import { useEffect, useState } from "react";
import { ChevronDown, Plus } from "lucide-react";
import { useFieldArray, useWatch, type Control } from "react-hook-form";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import { Checkbox } from "@/components/ui/checkbox";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { cn } from "@/lib/utils";
import {
  EDUCATION_FIELD_LIMITS,
  type EducationEntryFormValues,
} from "@/schemas/user-profile-education.schema";
import { createEmptyEducationEntry } from "@/utils/userProfileEducation";

function buildCollapsedSummary(
  parts: string[],
  fallback: string,
): { display: string; full: string } {
  const [degreeType, institution, fieldOfStudy] = parts.map((value) =>
    typeof value === "string" ? value.trim() : "",
  );

  const fullParts = [degreeType, institution, fieldOfStudy].filter(Boolean);
  const full = fullParts.length > 0 ? fullParts.join(" · ") : fallback;

  // Prefer degree + institution in the header; CSS ellipsis handles overflow.
  const displayParts = [degreeType, institution].filter(Boolean);
  const display = displayParts.length > 0 ? displayParts.join(" · ") : full;

  return { display, full };
}

export type EducationEntriesFormValues = {
  education: EducationEntryFormValues[];
};

interface EducationEntryCardProps {
  control: Control<EducationEntriesFormValues>;
  fieldId: string;
  index: number;
  isExpanded: boolean;
  onToggleExpanded: () => void;
  onRemove: () => void;
}

const EducationEntryCard = ({
  control,
  fieldId,
  index,
  isExpanded,
  onToggleExpanded,
  onRemove,
}: EducationEntryCardProps) => {
  const degreeType = useWatch({
    control,
    name: `education.${index}.degreeType`,
  });
  const institution = useWatch({
    control,
    name: `education.${index}.institution`,
  });
  const fieldOfStudy = useWatch({
    control,
    name: `education.${index}.fieldOfStudy`,
  });

  const summaryParts = [degreeType, institution, fieldOfStudy].map((value) =>
    typeof value === "string" ? value : "",
  );
  const { display: summaryDisplay, full: summaryFull } = buildCollapsedSummary(
    summaryParts,
    `Education ${index + 1}`,
  );

  return (
    <div className="w-full min-w-0 overflow-hidden rounded-xl border border-(--neutral-100)">
      <div className="flex w-full min-w-0 items-center gap-2 p-4">
        <button
          type="button"
          onClick={onToggleExpanded}
          className="flex min-w-0 flex-1 items-center gap-2 overflow-hidden text-left cursor-pointer"
        >
          <ChevronDown
            size={18}
            className={cn(
              "shrink-0 text-(--text-neutral-400) transition-transform duration-300",
              isExpanded && "rotate-180",
            )}
          />
          <span
            className="min-w-0 flex-1 truncate text-sm font-medium text-(--text-primary-dark)"
            title={summaryFull}
          >
            {summaryDisplay}
          </span>
        </button>
        <button
          type="button"
          onClick={onRemove}
          aria-label={`Remove ${summaryFull}`}
          className="inline-flex shrink-0 items-center justify-center p-1 text-(--status-denied) hover:opacity-80 cursor-pointer"
        >
          <TrashIcon size={16} />
        </button>
      </div>

      <div
        className={cn(
          "overflow-hidden transition-[max-height,opacity] duration-300 ease-in-out",
          isExpanded ? "max-h-[75rem] opacity-100" : "max-h-0 opacity-0",
        )}
      >
        <div className="space-y-4 border-t border-(--neutral-100) px-4 pb-4 pt-4">
          <CustomInput
            control={control}
            name={`education.${index}.degreeType`}
            label="Degree Type"
            required
            maxLength={EDUCATION_FIELD_LIMITS.degreeType}
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <CustomInput
              control={control}
              name={`education.${index}.fieldOfStudy`}
              label="Field of Study"
              maxLength={EDUCATION_FIELD_LIMITS.fieldOfStudy}
            />
            <CustomInput
              control={control}
              name={`education.${index}.institution`}
              label="Institution"
              required
              maxLength={EDUCATION_FIELD_LIMITS.institution}
            />
          </div>

          <CustomDatePicker
            control={control}
            name={`education.${index}.graduationDate`}
            label="Graduation Date"
            valueType="date"
            minYear={2000}
            maxYear={new Date().getFullYear() + 50}
          />

          <CustomInput
            control={control}
            name={`education.${index}.accreditationBody`}
            label="Accreditation Body"
            maxLength={EDUCATION_FIELD_LIMITS.accreditationBody}
          />

          <FormField
            control={control}
            name={`education.${index}.isAccredited`}
            render={({ field: accreditedField }) => (
              <FormItem className="flex items-center gap-3 space-y-0">
                <FormControl>
                  <Checkbox
                    id={`education-accredited-${fieldId}`}
                    checked={Boolean(accreditedField.value)}
                    onCheckedChange={(checked) =>
                      accreditedField.onChange(checked)
                    }
                  />
                </FormControl>
                <FormLabel
                  htmlFor={`education-accredited-${fieldId}`}
                  className="text-sm font-medium text-(--text-primary-dark)"
                >
                  Institution is accredited
                </FormLabel>
              </FormItem>
            )}
          />

          <CustomTextarea
            control={control}
            name={`education.${index}.notes`}
            label="Notes"
            rows={3}
            maxLength={EDUCATION_FIELD_LIMITS.notes}
          />
        </div>
      </div>
    </div>
  );
};

interface EducationEntriesFieldProps {
  control: Control<EducationEntriesFormValues>;
  initialEducation?: EducationEntryFormValues[];
  syncToken?: string | number;
}

const EducationEntriesField = ({
  control,
  initialEducation,
  syncToken,
}: EducationEntriesFieldProps) => {
  const { fields, append, remove, replace } = useFieldArray({
    control,
    name: "education",
  });
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
  const [pendingExpansion, setPendingExpansion] = useState<number | null>(null);
  const [previousSyncToken, setPreviousSyncToken] = useState(syncToken);
  if (previousSyncToken !== syncToken) {
    setPreviousSyncToken(syncToken);
    setExpandedIds(new Set());
    setPendingExpansion(null);
  }

  useEffect(() => {
    if (initialEducation === undefined) return;
    replace(initialEducation);
  }, [initialEducation, syncToken, replace]);

  if (pendingExpansion !== null && fields.length > pendingExpansion) {
    setExpandedIds(new Set([...expandedIds, fields[fields.length - 1].id]));
    setPendingExpansion(null);
  }

  const toggleExpanded = (fieldId: string) => {
    setExpandedIds((previous) => {
      const next = new Set(previous);
      if (next.has(fieldId)) {
        next.delete(fieldId);
      } else {
        next.add(fieldId);
      }
      return next;
    });
  };

  const handleAddEducation = () => {
    if (fields.length >= EDUCATION_FIELD_LIMITS.maxEntries) return;
    append(createEmptyEducationEntry());
    setPendingExpansion(fields.length);
  };

  return (
    <div className="w-full min-w-0 space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-sm font-semibold text-(--text-primary-dark)">
          Education
        </h3>
        <button
          type="button"
          onClick={handleAddEducation}
          disabled={fields.length >= EDUCATION_FIELD_LIMITS.maxEntries}
          className="inline-flex items-center gap-1 text-sm font-semibold text-(--text-primary-500) hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          <Plus size={16} />
          Add Education
        </button>
      </div>

      {fields.length === 0 ? (
        <p className="text-sm text-(--text-neutral-500)">
          No education entries added yet.
        </p>
      ) : (
        fields.map((field, index) => (
          <EducationEntryCard
            key={field.id}
            control={control}
            fieldId={field.id}
            index={index}
            isExpanded={expandedIds.has(field.id)}
            onToggleExpanded={() => toggleExpanded(field.id)}
            onRemove={() => {
              setExpandedIds((previous) => {
                const next = new Set(previous);
                next.delete(field.id);
                return next;
              });
              remove(index);
            }}
          />
        ))
      )}
      <FormField
        control={control}
        name="education"
        render={() => (
          <FormItem>
            <FormMessage />
          </FormItem>
        )}
      />
    </div>
  );
};

export default EducationEntriesField;
