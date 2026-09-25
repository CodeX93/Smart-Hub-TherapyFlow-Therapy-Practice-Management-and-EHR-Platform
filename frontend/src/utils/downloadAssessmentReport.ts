import { getAuthSession } from "@/utils/authStorage";
import { fetchWithAuth } from "@/utils/fetchWithAuth";

export async function downloadAssessmentReportFile(
  assignmentId: number,
  format: "pdf" | "docx",
): Promise<{ blob: Blob; filename: string }> {
  const session = getAuthSession();
  if (!session?.accessToken) {
    throw new Error("You must be signed in to download the report.");
  }

  const path =
    format === "pdf"
      ? `/api/v1/assessments/assignments/${assignmentId}/download/pdf-file`
      : `/api/v1/assessments/assignments/${assignmentId}/download/docx`;

  const response = await fetchWithAuth(path);

  if (!response.ok) {
    throw new Error(`Download failed (${response.status})`);
  }

  const blob = await response.blob();
  const filename =
    format === "pdf"
      ? `assessment-report-${assignmentId}.pdf`
      : `assessment-report-${assignmentId}.docx`;

  return { blob, filename };
}

export function triggerBrowserDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
