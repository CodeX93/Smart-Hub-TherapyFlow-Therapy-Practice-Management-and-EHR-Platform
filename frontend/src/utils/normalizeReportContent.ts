const HTML_TAG_PATTERN = /<\/?[a-z][\s\S]*>/i;
const CODE_FENCE_LINE_PATTERN = /^```(\w+)?\s*$/;
const HTML_FENCE_PARAGRAPH_PATTERN = /<p[^>]*>\s*```\w*\s*<\/p>/gi;
const HTML_CLOSING_FENCE_PARAGRAPH_PATTERN = /<p[^>]*>\s*```\s*<\/p>/gi;
const EMBEDDED_FENCE_PATTERN = /```\w*\s*([\s\S]*?)\s*```/;

function stripMarkdownCodeFence(value: string): string {
  const trimmed = value.trim();
  if (!trimmed.startsWith("```")) return trimmed;

  const lines = trimmed.split("\n");
  if (!CODE_FENCE_LINE_PATTERN.test(lines[0]?.trim() ?? "")) return trimmed;

  lines.shift();
  if (lines.length > 0 && lines[lines.length - 1]?.trim() === "```") {
    lines.pop();
  }

  return lines.join("\n").trim();
}

function stripHtmlFenceArtifacts(value: string): string {
  return value
    .replace(HTML_FENCE_PARAGRAPH_PATTERN, "")
    .replace(HTML_CLOSING_FENCE_PARAGRAPH_PATTERN, "")
    .replace(/<p[^>]*>\s*```\s*<\/p>/gi, "")
    .trim();
}

function stripEmbeddedCodeFences(value: string): string {
  if (!value.includes("```")) return value;

  const match = value.match(EMBEDDED_FENCE_PATTERN);
  if (!match?.[1]) return value;

  return match[1].trim();
}

function plainTextToHtml(value: string): string {
  const escaped = value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

  return escaped
    .split(/\n{2,}/)
    .filter(Boolean)
    .map((paragraph) => `<p>${paragraph.replace(/\n/g, "<br>")}</p>`)
    .join("");
}

/** Prepare AI-generated report text for rich-text editing (strip fences, render HTML). */
export function normalizeReportContentForEditor(content: string): string {
  if (!content) return "";

  let normalized = content.trim();
  normalized = stripMarkdownCodeFence(normalized);
  normalized = stripHtmlFenceArtifacts(normalized);
  normalized = stripEmbeddedCodeFences(normalized);
  normalized = stripHtmlFenceArtifacts(normalized);

  if (!normalized) return "";

  if (HTML_TAG_PATTERN.test(normalized)) {
    return normalized;
  }

  return plainTextToHtml(normalized);
}

export function resolveReportEditorContent(report: {
  isFinalized?: boolean;
  finalContent?: string | null;
  draftContent?: string | null;
  generatedContent?: string | null;
}): string {
  const raw = report.isFinalized
    ? report.finalContent ?? report.draftContent ?? report.generatedContent
    : report.draftContent ?? report.generatedContent ?? report.finalContent;

  return normalizeReportContentForEditor(raw ?? "");
}

export function extractAssessmentReportContent(report: {
  isFinalized?: boolean;
  finalContent?: string | null;
  draftContent?: string | null;
  generatedContent?: string | null;
  editorContent?: string | null;
  reportData?: string | null;
}): string {
  const resolved = resolveReportEditorContent({
    isFinalized: report.isFinalized,
    finalContent: report.finalContent,
    draftContent: report.draftContent ?? report.editorContent,
    generatedContent: report.generatedContent ?? report.editorContent,
  });

  if (resolved.trim()) return resolved;

  return normalizeReportContentForEditor(report.reportData ?? "");
}
