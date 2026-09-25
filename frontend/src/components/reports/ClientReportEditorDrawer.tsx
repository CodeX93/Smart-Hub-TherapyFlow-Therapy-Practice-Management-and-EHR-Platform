
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo, useState } from "react";
import { Download, Save, X } from "lucide-react";
import CustomJoditEditor from "@/components/shared/CustomJoditEditor";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import {
  useFinalizeClientReportMutation,
  useGetClientReportQuery,
  useUpdateClientReportDraftMutation,
} from "@/store/api/admin/clientReports.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { fetchWithAuth } from "@/utils/fetchWithAuth";
import { normalizeReportContentForEditor, resolveReportEditorContent } from "@/utils/normalizeReportContent";
import { cn } from "@/lib/utils";

interface ClientReportEditorDrawerProps {
  reportId: number | null;
  clientId: number;
  readOnly?: boolean;
  onClose: () => void;
  onUpdated?: () => void;
}

function parseDownloadFilename(
  disposition: string,
  format: "pdf" | "docx",
  reportId: number,
): string {
  const fallbackDate = new Date().toISOString().split("T")[0];
  const fallback = `client-report-${reportId}-${fallbackDate}.${format}`;
  if (!disposition) return fallback;

  const utf8Match = disposition.match(/filename\*=(?:UTF-8''|utf-8'')([^;]+)/i);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].trim().replace(/^"|"$/g, ""));
    } catch {
      /* fall through */
    }
  }

  const quoted = disposition.match(/filename="([^"]+)"/i);
  if (quoted?.[1]?.trim()) return quoted[1].trim();

  const bare = disposition.match(/filename=([^;]+)/i);
  if (bare?.[1]?.trim()) return bare[1].trim().replace(/^"|"$/g, "");

  return fallback;
}

const ClientReportEditorDrawer = ({
  reportId,
  clientId,
  readOnly = false,
  onClose,
  onUpdated,
}: ClientReportEditorDrawerProps) => {
  const [content, setContent] = useState("");
  const [downloadingFormat, setDownloadingFormat] = useState<"pdf" | "docx" | null>(null);
  const [toast, setToast] = useState<{ message: string; type: "success" | "error" } | null>(null);

  const skip = !reportId;
  const { data: report, isLoading, refetch } = useGetClientReportQuery(reportId ?? 0, { skip });
  const [saveDraft, { isLoading: isSaving }] = useUpdateClientReportDraftMutation();
  const [finalizeReport, { isLoading: isFinalizing }] = useFinalizeClientReportMutation();

  const isFinalized = Boolean(report?.isFinalized);
  const editorReadOnly = readOnly || isFinalized;
  const isBusy = isSaving || isFinalizing || Boolean(downloadingFormat);

  const resolvedContent = useMemo(() => {
    if (!report) return "";
    return resolveReportEditorContent(report);
  }, [report]);

  useEffect(() => {
    setContent(resolvedContent);
  }, [resolvedContent, reportId]);

  if (!reportId) return null;

  const showToast = (message: string, type: "success" | "error") => setToast({ message, type });

  const handleClose = () => {
    if (isBusy) return;
    onClose();
  };

  const handleSave = async () => {
    if (!reportId) return;
    try {
      await saveDraft({
        reportId,
        clientId,
        draftContent: normalizeReportContentForEditor(content),
      }).unwrap();
      showToast("Draft saved successfully", "success");
      refetch();
      onUpdated?.();
    } catch (error) {
      showToast(getApiErrorMessage(error) || "Failed to save draft", "error");
    }
  };

  const handleFinalize = async () => {
    if (!reportId) return;
    try {
      if (!isFinalized && content !== resolvedContent) {
        await saveDraft({
        reportId,
        clientId,
        draftContent: normalizeReportContentForEditor(content),
      }).unwrap();
      }
      await finalizeReport({ reportId, clientId }).unwrap();
      showToast("Report finalized successfully", "success");
      refetch();
      onUpdated?.();
    } catch (error) {
      showToast(getApiErrorMessage(error) || "Failed to finalize report", "error");
    }
  };

  const openDownload = async (format: "pdf" | "docx") => {
    if (!reportId) return;
    setDownloadingFormat(format);
    try {
      const response = await fetchWithAuth(`/api/v1/reports/${reportId}/download/${format}`);
      if (!response.ok) {
        throw new Error("Download failed");
      }
      const blob = await response.blob();
      const disposition = response.headers.get("Content-Disposition") || "";
      const filename = parseDownloadFilename(disposition, format, reportId);
      const url = window.URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.URL.revokeObjectURL(url);
      showToast(format === "pdf" ? "PDF downloaded" : "Word document downloaded", "success");
    } catch (error) {
      showToast(getApiErrorMessage(error) || "Failed to download report", "error");
    } finally {
      setDownloadingFormat(null);
    }
  };

  const generatedLabel = report?.generatedAt
    ? new Date(report.generatedAt).toLocaleString()
    : null;

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
      <button
        type="button"
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={handleClose}
        disabled={isBusy}
        aria-label="Close report editor"
      />
      <div
        className="relative flex max-h-[92vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl bg-white shadow-xl"
        role="dialog"
        aria-modal="true"
        aria-labelledby="client-report-editor-title"
      >
        <div className="flex shrink-0 items-start justify-between gap-3 border-b border-(--neutral-100) px-5 py-4">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2
                id="client-report-editor-title"
                className="truncate text-lg font-bold text-(--neutral-950)"
                title={report?.templateName ?? "Client Report"}
              >
                {report?.templateName ?? "Client Report"}
              </h2>
              <span
                className={cn(
                  "shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium",
                  isFinalized
                    ? "bg-(--bg-primary-50) text-(--text-primary-dark)"
                    : "bg-(--neutral-100) text-(--text-neutral-600)",
                )}
              >
                {isFinalized ? "Finalized" : "Draft"}
              </span>
            </div>
            <p className="mt-0.5 text-sm text-(--text-neutral-600)">
              {editorReadOnly ? "View report content" : "Review and edit report content"}
              {generatedLabel ? ` · Generated ${generatedLabel}` : ""}
            </p>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            onClick={handleClose}
            disabled={isBusy}
            aria-label="Close report editor"
          >
            <X size={20} aria-hidden="true" />
          </Button>
        </div>

        <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
          {isLoading ? (
            <ContentLoader size="md" className="-1 px-5 py-16 text-sm text-(--text-neutral-600) mr-2" />
          ) : (
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden px-5 py-4">
              <div className="overflow-hidden rounded-xl border border-(--neutral-200) bg-white">
                <CustomJoditEditor
                  key={`report-editor-${reportId}-${isLoading ? "loading" : "ready"}`}
                  content={content}
                  setContent={setContent}
                  readonly={editorReadOnly}
                  placeholder=""
                  contentVariant="report"
                  minHeight={320}
                  editorHeight={440}
                />
              </div>
            </div>
          )}

          <div className="flex shrink-0 flex-wrap items-center justify-end gap-3 border-t border-(--neutral-100) px-5 py-4">
            {!editorReadOnly ? (
              <Button
                type="button"
                variant="secondary"
                size="lg"
                onClick={() => void handleSave()}
                disabled={isSaving || isFinalizing}
                loading={isSaving}
                loadingLabel="Saving..."
              >
                <Save size={16} />
                Save draft
              </Button>
            ) : null}

            {!readOnly && !isFinalized ? (
              <Button
                type="button"
                variant="primary"
                size="lg"
                onClick={() => void handleFinalize()}
                disabled={isFinalizing || isSaving}
                loading={isFinalizing}
                loadingLabel="Finalizing..."
              >
                Finalize
              </Button>
            ) : null}

            <Button
              type="button"
              variant="secondary"
              size="lg"
              onClick={() => void openDownload("pdf")}
              disabled={Boolean(downloadingFormat)}
              loading={downloadingFormat === "pdf"}
              loadingLabel="Downloading PDF..."
            >
              <Download size={16} />
              PDF
            </Button>
            <Button
              type="button"
              variant="secondary"
              size="lg"
              onClick={() => void openDownload("docx")}
              disabled={Boolean(downloadingFormat)}
              loading={downloadingFormat === "docx"}
              loadingLabel="Downloading Word document..."
            >
              <Download size={16} />
              Word
            </Button>
          </div>
        </div>
      </div>

      {toast ? (
        <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />
      ) : null}
    </div>
  );
};

export default ClientReportEditorDrawer;
