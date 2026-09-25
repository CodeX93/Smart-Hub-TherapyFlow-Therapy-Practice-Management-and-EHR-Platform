import type {
  PortalFormAssignment,
  PortalFormAssignmentDetail,
  PortalFormAssignmentField,
  PortalFormResponse,
} from "@/store/api/portalFormsApi";
import type { ClinicalForm, FormStatus } from "@/types/clinical-form.type";

function formatPortalFormDate(value: string | null | undefined): string {
  if (!value) return "-";

  const dateOnlyMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  const parsed = dateOnlyMatch
    ? new Date(
        Number.parseInt(dateOnlyMatch[1], 10),
        Number.parseInt(dateOnlyMatch[2], 10) - 1,
        Number.parseInt(dateOnlyMatch[3], 10),
      )
    : new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleDateString("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  });
}

export function mapPortalFormStatus(status: string | null | undefined): FormStatus {
  const normalized = (status ?? "").trim().toLowerCase().replace(/[\s-]+/g, "_");

  if (
    [
      "submitted",
      "completed",
      "signed",
      "done",
      "closed",
      "reviewed",
    ].includes(normalized)
  ) {
    return "completed";
  }

  if (normalized === "assigned") {
    return "pending";
  }

  if (
    [
      "in_progress",
      "started",
      "draft",
      "open",
      "partial",
      "inprogress",
    ].includes(normalized)
  ) {
    return "in-progress";
  }

  return "pending";
}

function formatPortalFormCategory(category: string): string {
  const normalized = category.trim();
  if (!normalized) return "Form";

  return normalized
    .toLowerCase()
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function mapPortalFormAssignmentToCard(
  assignment: PortalFormAssignment,
): ClinicalForm {
  const status = mapPortalFormStatus(assignment.status);

  return {
    id: String(assignment.id),
    title: assignment.templateName,
    category: formatPortalFormCategory(assignment.templateCategory),
    status,
    assignedDate: formatPortalFormDate(
      assignment.assignedAt ?? assignment.dueDate,
    ),
    completedDate:
      status === "completed"
        ? formatPortalFormDate(
            assignment.completedAt ??
              assignment.submittedAt ??
              assignment.dueDate,
          )
        : undefined,
  };
}

export function filterPortalFormCards(
  forms: ClinicalForm[],
  searchQuery: string,
  selectedFilters: FormStatus[],
): ClinicalForm[] {
  const normalizedSearch = searchQuery.trim().toLowerCase();

  return forms.filter((form) => {
    const matchesFilter =
      selectedFilters.length === 0 || selectedFilters.includes(form.status);
    const matchesSearch =
      normalizedSearch === "" ||
      form.title.toLowerCase().includes(normalizedSearch) ||
      form.category.toLowerCase().includes(normalizedSearch);

    return matchesFilter && matchesSearch;
  });
}

export function isPortalFormReadOnly(status: string | null | undefined): boolean {
  return mapPortalFormStatus(status) === "completed";
}

export function isPortalFormSignatureField(field: PortalFormAssignmentField): boolean {
  const type = normalizePortalFieldType(field.fieldType);
  const label = (field.label || "").trim().toLowerCase();
  
  return (
    type.includes("signature") ||
    label === "client full name" ||
    label === "date" ||
    label === "signatures"
  );
}

export function isPortalFormEditableField(field: PortalFormAssignmentField): boolean {
  return !isPortalFormReadOnlyField(field) && !isPortalFormSignatureField(field);
}

export function normalizePortalFieldType(fieldType: string): string {
  return fieldType.trim().toLowerCase();
}

export function isPortalFormReadOnlyField(field: PortalFormAssignmentField): boolean {
  const fieldType = normalizePortalFieldType(field.fieldType);
  if (fieldType === "heading" || fieldType === "info_text") {
    return true;
  }
  // Some consent templates store long readable copy in helpText on a non-required TEXT field.
  return isPortalFormInformationalTextField(field);
}

/** Non-required TEXT with long helpText is display copy, not an answer input. */
export function isPortalFormInformationalTextField(
  field: PortalFormAssignmentField,
): boolean {
  const fieldType = normalizePortalFieldType(field.fieldType);
  if (fieldType !== "text" && fieldType !== "short_text" && fieldType !== "single_line") {
    return false;
  }
  if (field.isRequired) return false;
  const helpText = field.helpText?.trim() ?? "";
  return helpText.length > 80;
}

export function parsePortalFormFieldOptions(options: string | null): string[] {
  if (!options?.trim()) return [];

  try {
    const parsed = JSON.parse(options) as unknown;
    if (Array.isArray(parsed)) {
      return parsed
        .map((entry) => {
          if (typeof entry === "string") return entry.trim();
          if (isRecord(entry) && typeof entry.label === "string") {
            return entry.label.trim();
          }
          if (isRecord(entry) && typeof entry.value === "string") {
            return entry.value.trim();
          }
          return "";
        })
        .filter(Boolean);
    }
  } catch {
    // Fall back to delimiter parsing.
  }

  return options
    .split(/[\n,|]/)
    .map((entry) => entry.trim())
    .filter(Boolean);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function buildPortalFormFieldValues(
  responses: PortalFormResponse[],
): Record<number, string> {
  return responses.reduce<Record<number, string>>((accumulator, response) => {
    if (response.value !== null && response.value !== undefined) {
      accumulator[response.assignmentFieldId] = response.value;
    }
    return accumulator;
  }, {});
}

export function calculatePortalFormProgress(
  fields: PortalFormAssignmentField[],
  values: Record<number, string>,
  hasSignature: boolean,
  requiresSignature: boolean,
): number {
  const interactiveFields = fields.filter(
    (field) =>
      !isPortalFormReadOnlyField(field) && !isPortalFormSignatureField(field),
  );

  const requiredFields = interactiveFields.filter((field) => field.isRequired);
  const requiredCount = requiredFields.length + (requiresSignature ? 1 : 0);

  if (requiredCount === 0) {
    return 100;
  }

  const answeredRequired = requiredFields.filter((field) =>
    Boolean(values[field.id]?.trim()),
  ).length;
  const signatureScore = requiresSignature && hasSignature ? 1 : 0;

  return Math.round(
    ((answeredRequired + signatureScore) / requiredCount) * 100,
  );
}

export function getPortalFormStatusLabel(status: FormStatus): string {
  switch (status) {
    case "completed":
      return "Completed";
    case "in-progress":
      return "In progress";
    default:
      return "Pending";
  }
}

export function getPortalFormDetailTitle(detail: PortalFormAssignmentDetail): string {
  return detail.templateName || "Clinical Form";
}
