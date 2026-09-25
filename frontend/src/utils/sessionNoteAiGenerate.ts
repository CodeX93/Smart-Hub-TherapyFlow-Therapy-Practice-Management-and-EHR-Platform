import {
  AI_PROCESSING_CONSENT_REQUIRED_MESSAGE,
  getApiErrorMessage,
  isAiProcessingConsentRequiredError,
} from "./apiError.ts";

export interface SessionNoteGenerateFormData {
  sessionFocus: string;
  symptoms: string;
  shortTermGoals: string;
  intervention: string;
  progress: string;
  remarks: string;
  recommendations: string;
}

// Every field that is sent to the model counts as content worth generating
// from. Leaving one out disables Generate on a note that does have something
// in it, and then tells the clinician to fill in a field they already filled.
const GENERATE_FIELD_KEYS: (keyof SessionNoteGenerateFormData)[] = [
  "sessionFocus",
  "symptoms",
  "shortTermGoals",
  "intervention",
  "progress",
  "remarks",
  "recommendations",
];

export function hasClinicalFieldsForGenerate(
  formData: SessionNoteGenerateFormData,
): boolean {
  return GENERATE_FIELD_KEYS.some((key) => formData[key].trim().length > 0);
}

export function buildSessionNoteGenerateFormData(
  formData: SessionNoteGenerateFormData,
): Record<string, string> {
  return {
    sessionFocus: formData.sessionFocus.trim(),
    symptoms: formData.symptoms.trim(),
    shortTermGoals: formData.shortTermGoals.trim(),
    intervention: formData.intervention.trim(),
    progress: formData.progress.trim(),
    remarks: formData.remarks.trim(),
    recommendations: formData.recommendations.trim(),
  };
}

const SESSION_NOTE_SECTION_HEADERS = new Set(
  [
    "Client Information",
    "Session Information",
    "Presenting Concerns",
    "Session Focus",
    "Symptoms",
    "Short-term Goals",
    "Short-Term Goals",
    "Interventions",
    "Progress",
    "Additional Notes",
    "Recommendations",
    "Remarks",
    "Plan",
    "Risk Assessment",
    "Assessment",
    "Goals",
    "Homework",
    "Follow-up Plan",
    "Follow Up Plan",
  ].map((header) => header.toLowerCase()),
);

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function isSessionNoteSectionHeader(line: string): boolean {
  const trimmed = line.trim();
  if (!trimmed || trimmed.includes(":")) return false;

  if (SESSION_NOTE_SECTION_HEADERS.has(trimmed.toLowerCase())) {
    return true;
  }

  if (
    trimmed === trimmed.toUpperCase() &&
    trimmed.length <= 50 &&
    /[A-Z]/.test(trimmed)
  ) {
    return true;
  }

  if (trimmed.length > 50) return false;

  return /^([A-Z][a-zA-Z]*(?:-[A-Z][a-zA-Z]*)?)(\s+([A-Z][a-zA-Z]*(?:-[A-Z][a-zA-Z]*)?))*$/.test(
    trimmed,
  );
}

function formatLabelValueLine(line: string): string | null {
  const match = line.match(/^([^:]{1,40}):\s*(.+)$/);
  if (!match) return null;

  const label = match[1].trim();
  const value = match[2].trim();
  if (!label || !value || label.split(/\s+/).length > 5) return null;

  return `<p><strong>${escapeHtml(label)}:</strong> ${escapeHtml(value)}</p>`;
}

export function plainTextToHtml(value: string): string {
  const normalized = value.replace(/\r\n/g, "\n").trim();
  if (!normalized) return "";

  const lines = normalized.split("\n");
  const parts: string[] = [];
  let paragraphLines: string[] = [];

  const flushParagraph = () => {
    if (!paragraphLines.length) return;
    const text = paragraphLines.join(" ").trim();
    if (text) {
      parts.push(`<p>${escapeHtml(text)}</p>`);
    }
    paragraphLines = [];
  };

  for (const rawLine of lines) {
    const line = rawLine.trim();

    if (!line) {
      flushParagraph();
      continue;
    }

    if (isSessionNoteSectionHeader(line)) {
      flushParagraph();
      parts.push(`<h3>${escapeHtml(line)}</h3>`);
      continue;
    }

    const labelValue = formatLabelValueLine(line);
    if (labelValue) {
      flushParagraph();
      parts.push(labelValue);
      continue;
    }

    paragraphLines.push(line);
  }

  flushParagraph();
  return parts.join("");
}

export function getSessionNoteGenerateErrorMessage(error: unknown): string {
  if (isAiProcessingConsentRequiredError(error)) {
    return AI_PROCESSING_CONSENT_REQUIRED_MESSAGE;
  }
  return getApiErrorMessage(error) || "Failed to generate session note";
}
