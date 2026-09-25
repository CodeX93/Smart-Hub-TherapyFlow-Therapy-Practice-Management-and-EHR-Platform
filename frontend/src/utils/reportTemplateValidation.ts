export const REPORT_TEMPLATE_LIMITS = {
  name: 255,
  description: 5000,
  aiInstructions: 4000,
  structureText: 30_000,
  supportingFilesGuidance: 2000,
  documentTypeLabel: 150,
  maxDocumentTypes: 20,
  maxFileBytes: 15 * 1024 * 1024,
} as const;

const TEMPLATE_FILE_EXTENSIONS = [".docx", ".pdf"] as const;

const TEMPLATE_FILE_MIME_TYPES = new Set([
  "application/pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
]);

export function clampToMaxLength(value: string, max: number): string {
  if (value.length <= max) return value;
  return value.slice(0, max);
}

export function formatCharacterHint(current: number, max: number): string {
  return `${current}/${max}`;
}

export function fieldHintWithLimit(
  base: string | undefined,
  current: number,
  max: number,
): string {
  const counter = formatCharacterHint(current, max);
  return base ? `${base} · ${counter}` : counter;
}

export function parseDocumentTypes(text: string): string[] {
  return text
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
}

export function normalizeDocumentTypesText(text: string): string {
  return text
    .split("\n")
    .map((line) => clampToMaxLength(line, REPORT_TEMPLATE_LIMITS.documentTypeLabel))
    .join("\n");
}

export function validateReportTemplateFile(file: File | null): string | null {
  if (!file) return "Template file is required.";

  const lowerName = file.name.toLowerCase();
  const hasAllowedExtension = TEMPLATE_FILE_EXTENSIONS.some((ext) => lowerName.endsWith(ext));
  const hasAllowedMime =
    !file.type || file.type === "application/octet-stream" || TEMPLATE_FILE_MIME_TYPES.has(file.type);

  if (!hasAllowedExtension || !hasAllowedMime) {
    return "Only .docx or .pdf files are allowed.";
  }

  if (file.size > REPORT_TEMPLATE_LIMITS.maxFileBytes) {
    return "File must be 15 MB or smaller.";
  }

  return null;
}

export interface ReportTemplateFormValues {
  name: string;
  description: string;
  aiInstructions: string;
  supportingFilesGuidance: string;
  documentTypesText: string;
  file: File | null;
}

export function truncateForDisplay(value: string, max = 80): string {
  const trimmed = value.trim();
  if (trimmed.length <= max) return trimmed;
  return `${trimmed.slice(0, max - 1)}…`;
}

export interface ReportTemplateEditFormValues {
  name: string;
  description: string;
  aiInstructions: string;
  structureText: string;
  supportingFilesGuidance: string;
  documentTypesText: string;
}

function validateReportTemplateTextFields(
  values: Pick<ReportTemplateFormValues, "name">,
  options?: { requireName?: boolean },
): Record<string, string> {
  const errors: Record<string, string> = {};
  const requireName = options?.requireName ?? true;

  const trimmedName = values.name.trim();
  if (requireName && !trimmedName) {
    errors.name = "Template name is required.";
  } else if (trimmedName.length > REPORT_TEMPLATE_LIMITS.name) {
    errors.name = `Name must be ${REPORT_TEMPLATE_LIMITS.name} characters or fewer.`;
  }

  return errors;
}

export function validateReportTemplateUploadForm(
  values: ReportTemplateFormValues,
): { ok: boolean; errors: Partial<Record<keyof ReportTemplateFormValues | "file", string>> } {
  const errors = validateReportTemplateTextFields({ name: values.name }) as Partial<
    Record<keyof ReportTemplateFormValues | "file", string>
  >;

  const fileError = validateReportTemplateFile(values.file);
  if (fileError) {
    errors.file = fileError;
  }

  return { ok: Object.keys(errors).length === 0, errors };
}

export function validateReportTemplateEditForm(
  values: ReportTemplateEditFormValues,
): { ok: boolean; errors: Partial<Record<keyof ReportTemplateEditFormValues, string>> } {
  const errors = validateReportTemplateTextFields({ name: values.name }) as Partial<
    Record<keyof ReportTemplateEditFormValues, string>
  >;

  return { ok: Object.keys(errors).length === 0, errors };
}
