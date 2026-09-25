import { TrashIcon } from "@/components/icons/commonIcons";
import { useId, useMemo, useRef, useState } from "react";
import { FileText, FileUp, Sparkles } from "lucide-react";
import Toast from "@/components/shared/Toast";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import { ContentLoader } from "@/components/shared/ContentLoader";
import CustomSelect from "@/components/form/CustomSelect";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import ClientReportEditorDrawer from "./ClientReportEditorDrawer";
import {
  useDeleteClientReportMutation,
  useDeleteReportSupportingFileMutation,
  useGenerateClientReportMutation,
  useGetClientReportsQuery,
  useGetReportSupportingFilesQuery,
  useUploadReportSupportingFileMutation,
} from "@/store/api/admin/clientReports.api";
import { useGetReportTemplatesQuery } from "@/store/api/admin/reportTemplates.api";
import { readFileAsBase64 } from "@/utils/readFileAsBase64";
import {
  getApiErrorMessage,
  getClientReportGenerateErrorMessage,
  isAiProcessingConsentRequiredError,
} from "@/utils/apiError";
import { cn } from "@/lib/utils";

interface ReportsTabProps {
  clientId: number;
  readOnly?: boolean;
}

const ReportsTab = ({ clientId, readOnly = false }: ReportsTabProps) => {
  const formId = useId();
  const uploadInputRef = useRef<HTMLInputElement>(null);
  const skip = !clientId || clientId <= 0;
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | "">("");
  const [includeProfile, setIncludeProfile] = useState(true);
  const [includeNotes, setIncludeNotes] = useState(true);
  const [includeAssessments, setIncludeAssessments] = useState(true);
  const [selectedFileIds, setSelectedFileIds] = useState<number[]>([]);
  const [openReportId, setOpenReportId] = useState<number | null>(null);
  const [deleteReportTarget, setDeleteReportTarget] = useState<number | null>(null);
  const [toast, setToast] = useState<{
    message: string;
    type: "success" | "error" | "info";
  } | null>(null);
  const [uploadDocumentType, setUploadDocumentType] = useState("");

  const { data: templates = [] } = useGetReportTemplatesQuery(undefined, { skip });
  const {
    data: reports = [],
    isLoading: isReportsLoading,
    refetch: refetchReports,
  } = useGetClientReportsQuery(clientId, { skip });
  const { data: supportingFiles = [], refetch: refetchFiles } = useGetReportSupportingFilesQuery(
    clientId,
    { skip },
  );

  const [generateReport, { isLoading: isGenerating }] = useGenerateClientReportMutation();
  const [deleteReport, { isLoading: isDeletingReport }] = useDeleteClientReportMutation();
  const [uploadSupportingFile, { isLoading: isUploadingFile }] =
    useUploadReportSupportingFileMutation();
  const [deleteSupportingFile, { isLoading: isDeletingFile }] =
    useDeleteReportSupportingFileMutation();

  const templateOptions = useMemo(
    () =>
      templates.map((template) => ({
        value: String(template.id),
        label: template.name,
        description: template.originalName ?? undefined,
      })),
    [templates],
  );

  const selectedTemplate = useMemo(
    () => templates.find((template) => template.id === selectedTemplateId) ?? null,
    [templates, selectedTemplateId],
  );

  const draftKey = selectedTemplate?.id ?? null;
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (!selectedTemplate && seededDraftKey !== null) {
    setSeededDraftKey(null);
    setSelectedFileIds([]);
  }
  if (selectedTemplate && seededDraftKey !== draftKey) {
    setSeededDraftKey(draftKey);
    setIncludeProfile(Boolean(selectedTemplate.defaultIncludeProfile));
    setIncludeNotes(Boolean(selectedTemplate.defaultIncludeNotes));
    setIncludeAssessments(Boolean(selectedTemplate.defaultIncludeAssessments));
    setUploadDocumentType(selectedTemplate.supportingFileTypes?.[0] ?? "");
    setSelectedFileIds([]);
  }

  const showToast = (message: string, type: "success" | "error" | "info") =>
    setToast({ message, type });

  const handleTemplateChange = (value: string) => {
    setSelectedTemplateId(value ? Number(value) : "");
  };

  const handleGenerate = async () => {
    if (!selectedTemplateId) {
      showToast("Select a template first", "error");
      return;
    }
    try {
      const created = await generateReport({
        clientId,
        templateId: Number(selectedTemplateId),
        sources: { includeProfile, includeNotes, includeAssessments },
        supportingFileIds: selectedFileIds,
      }).unwrap();
      showToast("Report generated successfully", "success");
      setOpenReportId(created.id);
      await refetchReports();
    } catch (error) {
      const message = getClientReportGenerateErrorMessage(error);
      showToast(message, isAiProcessingConsentRequiredError(error) ? "info" : "error");
    }
  };

  const handleUploadSupportingFile = async (file: File) => {
    try {
      const fileContent = await readFileAsBase64(file);
      await uploadSupportingFile({
        clientId,
        fileContent,
        originalName: file.name,
        mimeType: file.type || "application/octet-stream",
        documentType: uploadDocumentType || undefined,
        templateId: selectedTemplateId ? Number(selectedTemplateId) : undefined,
      }).unwrap();
      showToast("Supporting file uploaded successfully", "success");
      await refetchFiles();
    } catch (error) {
      showToast(getApiErrorMessage(error) || "Failed to upload supporting file", "error");
    }
  };

  const toggleFileSelection = (fileId: number) => {
    setSelectedFileIds((previous) =>
      previous.includes(fileId)
        ? previous.filter((id) => id !== fileId)
        : [...previous, fileId],
    );
  };

  const handleDeleteReport = async () => {
    if (!deleteReportTarget) return;
    try {
      await deleteReport({ reportId: deleteReportTarget, clientId }).unwrap();
      showToast("Report deleted successfully", "success");
      setDeleteReportTarget(null);
      if (openReportId === deleteReportTarget) {
        setOpenReportId(null);
      }
      await refetchReports();
    } catch (error) {
      showToast(getApiErrorMessage(error) || "Failed to delete report", "error");
    }
  };

  const handleDeleteSupportingFile = async (fileId: number) => {
    try {
      await deleteSupportingFile({ fileId, clientId }).unwrap();
      setSelectedFileIds((previous) => previous.filter((id) => id !== fileId));
      showToast("Supporting file deleted", "success");
      await refetchFiles();
    } catch (error) {
      showToast(getApiErrorMessage(error) || "Failed to delete supporting file", "error");
    }
  };

  const isGenerateDisabled =
    isGenerating || isUploadingFile || !selectedTemplateId || isDeletingFile;

  return (
    <div className="flex h-full min-h-0 flex-col gap-6 overflow-y-auto p-4">
      {!readOnly ? (
        <section className="rounded-2xl border border-(--neutral-200) bg-white p-5">
          <h3 className="mb-4 text-lg font-medium text-(--neutral-950)">Generate Client Report</h3>

          <CustomSelect
            label="Template"
            value={selectedTemplateId ? String(selectedTemplateId) : ""}
            onChange={handleTemplateChange}
            options={templateOptions}
            placeholder="Search templates..."
            isSearch={templates.length > 4}
            required
          />

          {selectedTemplate ? (
            <div className="mt-5 space-y-5">
              {selectedTemplate.originalName ? (
                <div className="flex min-w-0 items-start gap-2 rounded-xl border border-(--neutral-100) bg-(--neutral-50) px-4 py-3">
                  <FileText size={18} className="mt-0.5 shrink-0 text-(--text-neutral-600)" />
                  <div className="min-w-0">
                    <p className="text-xs font-medium uppercase tracking-wide text-(--text-neutral-600)">
                      Template file
                    </p>
                    <p
                      className="truncate text-sm font-medium text-(--neutral-950)"
                      title={selectedTemplate.originalName}
                    >
                      {selectedTemplate.originalName}
                    </p>
                  </div>
                </div>
              ) : null}

              {selectedTemplate.supportingFilesGuidance ? (
                <div className="rounded-xl bg-(--bg-primary-50) px-4 py-3 text-sm text-(--text-primary-dark)">
                  {selectedTemplate.supportingFilesGuidance}
                </div>
              ) : null}

              {selectedTemplate.supportingFilesExpected && supportingFiles.length === 0 ? (
                <div className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                  This template expects supporting files, but none have been uploaded yet.
                </div>
              ) : null}

              <div className="rounded-xl border border-(--neutral-100) bg-(--neutral-50) p-3 space-y-2.5">
                <p className="text-xs font-semibold uppercase tracking-wide text-(--text-neutral-600)">
                  Data sources
                </p>
                <label className="flex cursor-pointer items-center gap-2.5 text-sm text-(--neutral-950)">
                  <Checkbox
                    id={`${formId}-include-profile`}
                    checked={includeProfile}
                    onChange={(event) => setIncludeProfile(event.target.checked)}
                  />
                  Include client profile
                </label>
                <label className="flex cursor-pointer items-center gap-2.5 text-sm text-(--neutral-950)">
                  <Checkbox
                    id={`${formId}-include-notes`}
                    checked={includeNotes}
                    onChange={(event) => setIncludeNotes(event.target.checked)}
                  />
                  Include sessions & session notes
                </label>
                <label className="flex cursor-pointer items-center gap-2.5 text-sm text-(--neutral-950)">
                  <Checkbox
                    id={`${formId}-include-assessments`}
                    checked={includeAssessments}
                    onChange={(event) => setIncludeAssessments(event.target.checked)}
                  />
                  Include assessments
                </label>
              </div>

              <div>
                <div className="mb-2 flex items-center justify-between gap-3">
                  <h4 className="text-sm font-medium text-(--neutral-950)">Supporting files</h4>
                  <Button
                    type="button"
                    variant="outline"
                    disabled={isUploadingFile || isGenerating}
                    loading={isUploadingFile}
                    loadingLabel="Uploading..."
                    onClick={() => uploadInputRef.current?.click()}
                    className="h-9 shrink-0 cursor-pointer rounded-full border-(--neutral-200) px-3 text-sm"
                  >
                    <FileUp size={16} />
                    Upload
                  </Button>
                  <input
                    ref={uploadInputRef}
                    type="file"
                    className="hidden"
                    accept=".docx,.pdf,.txt,text/plain,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    disabled={isUploadingFile}
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      if (file) void handleUploadSupportingFile(file);
                      event.currentTarget.value = "";
                    }}
                  />
                </div>

                {selectedTemplate.supportingFileTypes?.length ? (
                  <CustomSelect
                    label="Document type"
                    value={uploadDocumentType}
                    onChange={setUploadDocumentType}
                    options={selectedTemplate.supportingFileTypes.map((type) => ({
                      value: type,
                      label: type,
                    }))}
                    placeholder="Search types..."
                    isSearch={selectedTemplate.supportingFileTypes.length > 4}
                    className="mb-3"
                  />
                ) : null}

                {supportingFiles.length === 0 ? (
                  <p className="text-sm text-(--text-neutral-600)">No supporting files uploaded.</p>
                ) : (
                  <div className="space-y-2">
                    {supportingFiles.map((file) => (
                      <div
                        key={file.id}
                        className="flex items-center justify-between gap-2 rounded-xl border border-(--neutral-100) px-3 py-2 text-sm"
                      >
                        <label className="flex min-w-0 flex-1 cursor-pointer items-center gap-2.5">
                          <Checkbox
                            id={`${formId}-file-${file.id}`}
                            checked={selectedFileIds.includes(file.id)}
                            onChange={() => toggleFileSelection(file.id)}
                          />
                          <span className="min-w-0 truncate" title={file.originalName}>
                            {file.documentType ? `${file.documentType}: ` : ""}
                            {file.originalName}
                          </span>
                        </label>
                        <button
                          type="button"
                          className="shrink-0 text-red-500 disabled:opacity-50"
                          disabled={isDeletingFile || isUploadingFile}
                          onClick={() => void handleDeleteSupportingFile(file.id)}
                        >
                          <TrashIcon size={16} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <Button
                type="button"
                disabled={isGenerateDisabled}
                loading={isGenerating}
                loadingLabel="Generating..."
                onClick={() => void handleGenerate()}
                className={cn(
                  "h-10 rounded-full px-5 text-sm font-normal",
                  isGenerateDisabled ? "cursor-not-allowed" : "cursor-pointer",
                )}
              >
                <Sparkles size={16} />
                Generate report
              </Button>
            </div>
          ) : (
            <p className="mt-4 text-sm text-(--text-neutral-600)">
              Select a template to configure data sources and supporting files.
            </p>
          )}
        </section>
      ) : null}

      <section className="rounded-2xl border border-(--neutral-200) bg-white p-5">
        <h3 className="mb-4 text-lg font-medium text-(--neutral-950)">Generated Reports</h3>
        {isReportsLoading ? (
          <ContentLoader size="md" className="min-h-40" />
        ) : reports.length === 0 ? (
          <p className="text-sm text-(--text-neutral-600)">No client reports yet.</p>
        ) : (
          <div className="space-y-3">
            {reports.map((report) => (
              <div
                key={report.id}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-(--neutral-100) px-4 py-3"
              >
                <div className="min-w-0">
                  <div
                    className="truncate font-medium text-(--neutral-950)"
                    title={report.templateName ?? "Client Report"}
                  >
                    {report.templateName ?? "Client Report"}
                  </div>
                  <div className="text-xs text-(--text-neutral-600)">
                    {report.generatedAt
                      ? new Date(report.generatedAt).toLocaleString()
                      : "No date"}
                    {" · "}
                    {report.isFinalized ? "Finalized" : "Draft"}
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setOpenReportId(report.id)}
                    className="h-9 rounded-full border-(--neutral-200) px-3 text-sm"
                  >
                    {report.isFinalized ? "View" : "Review & Edit"}
                  </Button>
                  {!readOnly ? (
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setDeleteReportTarget(report.id)}
                      className="h-9 rounded-full border-red-200 px-3 text-sm text-red-600 hover:bg-red-50"
                    >
                      Delete
                    </Button>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <ClientReportEditorDrawer
        reportId={openReportId}
        clientId={clientId}
        readOnly={readOnly}
        onClose={() => setOpenReportId(null)}
        onUpdated={() => void refetchReports()}
      />

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(deleteReportTarget)}
        title="Delete report?"
        description="This report will be permanently deleted."
        items={[]}
        confirmButtonText="Delete"
        confirmButtonLoading={isDeletingReport}
        onConfirm={handleDeleteReport}
        onClose={() => setDeleteReportTarget(null)}
      />

      {toast ? (
        <Toast
          message={toast.message}
          type={toast.type}
          duration={toast.type === "info" ? 8000 : undefined}
          onClose={() => setToast(null)}
        />
      ) : null}
    </div>
  );
};

export default ReportsTab;
