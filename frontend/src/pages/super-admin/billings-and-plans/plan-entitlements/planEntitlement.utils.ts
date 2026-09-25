const LIMIT_FEATURE_KEYS = new Set([
  "CLIENT_LIMIT",
  "THERAPIST_LIMIT",
  "THERAPIST_SEATS",
  "SUPERVISOR_LIMIT",
  "SESSIONS_PER_MONTH",
  "FORM_TEMPLATES",
  "ASSESSMENT_TEMPLATES",
  "AI_REPORTS_PER_MONTH",
  "AI_CONTENT_GENERATIONS_PER_MONTH",
  "DOCUMENT_UPLOAD_GB",
  "STORAGE_MB",
  "TASK_LIMIT",
  "ZOOM_SESSIONS_PER_MONTH",
]);

export function isPlanLimitFeature(key: string, type?: string | null): boolean {
  const normalizedKey = key.trim().toUpperCase();
  if (LIMIT_FEATURE_KEYS.has(normalizedKey)) return true;

  const normalizedType = (type ?? "").trim().toLowerCase();
  return normalizedType === "limit" || normalizedType.includes("limit");
}

export function hasValidUsageLimit(value: number | null | undefined): boolean {
  return typeof value === "number" && Number.isFinite(value) && value >= 0;
}
