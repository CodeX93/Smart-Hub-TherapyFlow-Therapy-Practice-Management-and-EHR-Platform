import { useEffect, useMemo, useState } from "react";
import { FileText, Lock } from "lucide-react";

import { Button } from "@/components/ui/button";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import CustomJoditEditor from "@/components/shared/CustomJoditEditor";
import { ContentLoader } from "@/components/shared/ContentLoader";
import {
  useFinalizeAssessmentReportMutation,
  useGenerateAssessmentReportMutation,
  useGetAssessmentReportQuery,
  useUpdateAssessmentReportDraftMutation,
} from "@/store/api/admin/clients.api";
import {
  downloadAssessmentReportFile,
  triggerBrowserDownload,
} from "@/utils/downloadAssessmentReport";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  extractAssessmentReportContent,
  normalizeReportContentForEditor,
} from "@/utils/normalizeReportContent";

interface AssessmentReportStepProps {
  assignmentId: number;
  /** Whether a report has been generated for this assignment yet. */
  hasReport: boolean;
  readOnly?: boolean;
  answeredCount: number;
  totalQuestions: number;
  /** Persist the answers on screen before the report is written from them. */
  saveAnswers: () => Promise<void>;
  onGenerated: () => void;
  onFinalized: () => void;
  onBackToQuestions: () => void;
  onDirtyChange: (dirty: boolean) => void;
  onNotify: (message: string, type: "success" | "error" | "info") => void;
}

function formatTimestamp(value?: string): string | null {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed.toLocaleString();
}

/**
 * Step 2 of an assessment: write the report from the answers, edit it, and
 * sign it off. Like the session note, it saves as a draft or finalizes
 * through a confirmation, and a finalized report reads as a locked record.
 */
const AssessmentReportStep = ({
  assignmentId,
  hasReport,
  readOnly = false,
  answeredCount,
  totalQuestions,
  saveAnswers,
  onGenerated,
  onFinalized,
  onBackToQuestions,
  onDirtyChange,
  onNotify,
}: AssessmentReportStepProps) => {
  const [content, setContent] = useState("");
  const [hasUserEdited, setHasUserEdited] = useState(false);
  const [finalizeConfirmOpen, setFinalizeConfirmOpen] = useState(false);
  const [downloadingFormat, setDownloadingFormat] = useState<"pdf" | "docx" | null>(null);

  const {
    data: report,
    isFetching: isReportFetching,
    isError: isReportError,
    error: reportError,
  } = useGetAssessmentReportQuery(assignmentId, {
    skip: !hasReport || !Number.isFinite(assignmentId) || assignmentId <= 0,
  });
  const [generateReport, { isLoading: isGenerating }] = useGenerateAssessmentReportMutation();
  const [saveDraft, { isLoading: isSaving }] = useUpdateAssessmentReportDraftMutation();
  const [finalizeReport, { isLoading: isFinalizing }] = useFinalizeAssessmentReportMutation();
  const [isSavingAnswers, setIsSavingAnswers] = useState(false);
  // A toast is gone before anyone reads why generating failed (missing AI
  // consent, most often), so the reason stays on the step until the next try.
  const [generateError, setGenerateError] = useState<string | null>(null);

  const isFinalized = Boolean(report?.isFinalized);
  const serverContent = useMemo(
    () => (report ? extractAssessmentReportContent(report) : ""),
    [report],
  );
  const isDirty =
    hasUserEdited &&
    normalizeReportContentForEditor(content) !== normalizeReportContentForEditor(serverContent);

  useEffect(() => {
    if (!hasUserEdited) setContent(normalizeReportContentForEditor(serverContent));
  }, [hasUserEdited, serverContent]);

  useEffect(() => {
    onDirtyChange(isDirty);
  }, [isDirty, onDirtyChange]);

  const isBusy = isGenerating || isSavingAnswers || isSaving || isFinalizing;
  const canEdit = !readOnly && !isFinalized;
  const unanswered = Math.max(totalQuestions - answeredCount, 0);

  const handleGenerate = async () => {
    if (isDirty && !window.confirm("Regenerating will replace your edits to the report. Continue?")) {
      return;
    }
    setGenerateError(null);
    try {
      setIsSavingAnswers(true);
      await saveAnswers();
    } finally {
      setIsSavingAnswers(false);
    }
    try {
      const generated = await generateReport(assignmentId).unwrap();
      setHasUserEdited(false);
      setContent(normalizeReportContentForEditor(extractAssessmentReportContent(generated)));
      onGenerated();
      onNotify(hasReport ? "Report regenerated." : "Report generated. Review it, then finalize.", "success");
    } catch (error) {
      setGenerateError(getApiErrorMessage(error));
    }
  };

  const persistDraftIfChanged = async () => {
    if (!isDirty) return;
    await saveDraft({
      assignmentId,
      draftContent: normalizeReportContentForEditor(content),
    }).unwrap();
  };

  const handleSaveDraft = async () => {
    try {
      await persistDraftIfChanged();
      setHasUserEdited(false);
      onNotify("Draft saved.", "success");
    } catch (error) {
      onNotify(getApiErrorMessage(error), "error");
    }
  };

  const handleFinalize = async () => {
    try {
      await persistDraftIfChanged();
      await finalizeReport(assignmentId).unwrap();
      setHasUserEdited(false);
      setFinalizeConfirmOpen(false);
      onNotify("Report finalized.", "success");
      onFinalized();
    } catch (error) {
      onNotify(getApiErrorMessage(error), "error");
    }
  };

  const handleDownload = async (format: "pdf" | "docx") => {
    if (downloadingFormat) return;
    setDownloadingFormat(format);
    try {
      const { blob, filename } = await downloadAssessmentReportFile(assignmentId, format);
      triggerBrowserDownload(blob, filename);
    } catch (error) {
      onNotify(getApiErrorMessage(error), "error");
    } finally {
      setDownloadingFormat(null);
    }
  };

  const generatedAt = formatTimestamp(report?.generatedAt);
  const finalizedAt = formatTimestamp(report?.finalizedAt);
  const isLoadingReport = hasReport && isReportFetching && !content;

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="min-h-0 flex-1 overflow-y-auto px-8 py-6">
        {!hasReport ? (
          <div className="mx-auto flex max-w-xl flex-col items-center rounded-2xl border border-(--neutral-100) bg-(--neutral-50) px-8 py-10 text-center">
            <span className="mb-4 flex size-12 items-center justify-center rounded-full bg-white text-(--text-primary-dark) shadow-xs">
              <FileText size={22} />
            </span>
            <h3 className="text-lg font-semibold text-(--text-primary-dark)">
              {readOnly ? "No report yet" : "Generate the report"}
            </h3>
            <p className="mt-2 text-sm leading-6 text-(--text-neutral-600)">
              {readOnly
                ? "The report has not been generated for this assessment yet."
                : "AI drafts the report from the answers. You can edit it before you finalize it."}
            </p>
            <p className="mt-4 text-sm font-medium text-(--text-primary-dark)">
              {answeredCount} of {totalQuestions} questions answered
            </p>
            {unanswered > 0 && !readOnly ? (
              <p className="mt-1 text-sm text-(--text-neutral-600)">
                {unanswered} unanswered {unanswered === 1 ? "question is" : "questions are"} left out
                of the report.
              </p>
            ) : null}
            {generateError ? (
              <p className="mt-5 w-full rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-left text-sm text-red-700">
                The report could not be generated. {generateError}
              </p>
            ) : null}
          </div>
        ) : isLoadingReport ? (
          <ContentLoader size="md" className="min-h-64" />
        ) : (
          <div className="mx-auto max-w-5xl space-y-3">
            {generateError ? (
              <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                The report could not be regenerated. {generateError}
              </p>
            ) : null}
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm text-(--text-neutral-600)">
                {isFinalized
                  ? `Finalized${finalizedAt ? ` on ${finalizedAt}` : ""}`
                  : `Draft${generatedAt ? ` · generated on ${generatedAt}` : ""}`}
              </p>
              {isFinalized ? (
                <span className="inline-flex items-center gap-1 rounded-full bg-[#E8F4EE] px-2 py-1 text-xs font-medium text-[#157347]">
                  <Lock size={12} />
                  Locked
                </span>
              ) : null}
            </div>
            {isReportError && !content ? (
              <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {getApiErrorMessage(reportError)}
              </p>
            ) : (
              <div className="overflow-hidden rounded-xl border border-(--neutral-200) bg-white">
                <CustomJoditEditor
                  content={content}
                  setContent={(value) => {
                    setHasUserEdited(true);
                    setContent(value);
                  }}
                  placeholder="Report content..."
                  contentVariant="report"
                  readonly={!canEdit}
                  minHeight={320}
                  editorHeight={440}
                  scrollable
                />
              </div>
            )}
          </div>
        )}
      </div>

      <div className="flex shrink-0 flex-wrap items-center justify-between gap-3 border-t border-(--neutral-100) bg-white px-8 py-4">
        <Button
          type="button"
          variant="outline"
          onClick={onBackToQuestions}
          disabled={isBusy}
          className="h-11 rounded-full px-6 text-sm font-medium"
        >
          {isFinalized || readOnly ? "View answers" : "Back to questions"}
        </Button>

        <div className="flex flex-wrap items-center gap-3">
          {isFinalized ? (
            <>
              <Button
                type="button"
                variant="outline"
                onClick={() => void handleDownload("docx")}
                disabled={Boolean(downloadingFormat)}
                loading={downloadingFormat === "docx"}
                loadingLabel="Preparing..."
                className="h-11 rounded-full px-6 text-sm font-medium"
              >
                Download Word
              </Button>
              <Button
                type="button"
                onClick={() => void handleDownload("pdf")}
                disabled={Boolean(downloadingFormat)}
                loading={downloadingFormat === "pdf"}
                loadingLabel="Preparing..."
                className="h-11 rounded-full bg-(--bg-primary-dark) px-6 text-sm font-semibold text-white"
              >
                Download PDF
              </Button>
            </>
          ) : readOnly ? null : !hasReport ? (
            <Button
              type="button"
              onClick={() => void handleGenerate()}
              disabled={isBusy || answeredCount === 0}
              loading={isGenerating || isSavingAnswers}
              loadingLabel="Generating..."
              className="h-11 rounded-full bg-(--bg-primary-dark) px-8 text-sm font-semibold text-white"
            >
              Generate report
            </Button>
          ) : (
            <>
              <Button
                type="button"
                variant="outline"
                onClick={() => void handleGenerate()}
                disabled={isBusy}
                loading={isGenerating || isSavingAnswers}
                loadingLabel="Regenerating..."
                className="h-11 rounded-full px-6 text-sm font-medium"
              >
                Regenerate
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => void handleSaveDraft()}
                disabled={isBusy || !isDirty}
                loading={isSaving && !finalizeConfirmOpen}
                loadingLabel="Saving..."
                className="h-11 rounded-full px-6 text-sm font-medium"
              >
                Save draft
              </Button>
              <Button
                type="button"
                onClick={() => setFinalizeConfirmOpen(true)}
                disabled={isBusy || !content.trim()}
                className="h-11 rounded-full bg-(--bg-primary-dark) px-8 text-sm font-semibold text-white"
              >
                Save &amp; finalize
              </Button>
            </>
          )}
        </div>
      </div>

      <ConfirmationModal
        type="confirm"
        isOpen={finalizeConfirmOpen}
        onClose={() => {
          if (!isFinalizing) setFinalizeConfirmOpen(false);
        }}
        onConfirm={() => void handleFinalize()}
        title="Finalize this assessment report?"
        description="Finalizing locks the report and the answers. They can no longer be edited or regenerated, and the finalization time is recorded."
        items={[]}
        confirmButtonText="Finalize report"
        confirmButtonLoading={isFinalizing || isSaving}
        confirmButtonLoadingText="Finalizing..."
        overlayClassName="z-[10001]"
      />
    </div>
  );
};

export default AssessmentReportStep;
