import { useId, useMemo, useState } from "react";
import { FileUp, X } from "lucide-react";
import type { CreateReportTemplatePayload } from "@/store/api/admin/reportTemplates.api";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import { cn } from "@/lib/utils";
import { readFileAsBase64 } from "@/utils/readFileAsBase64";
import {
  clampToMaxLength,
  parseDocumentTypes,
  REPORT_TEMPLATE_LIMITS,
  validateReportTemplateFile,
  validateReportTemplateUploadForm,
} from "@/utils/reportTemplateValidation";

interface UploadTemplateModalProps {
  isOpen: boolean;
  isSubmitting?: boolean;
  onClose: () => void;
  onSubmit: (payload: CreateReportTemplatePayload) => Promise<void>;
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className="px-1 text-xs text-red-600">{message}</p>;
}

const UploadTemplateModalContent = ({
  isOpen,
  isSubmitting = false,
  onClose,
  onSubmit,
}: UploadTemplateModalProps) => {
  const formId = useId();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [aiInstructions, setAiInstructions] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [defaultIncludeProfile, setDefaultIncludeProfile] = useState(true);
  const [defaultIncludeNotes, setDefaultIncludeNotes] = useState(true);
  const [defaultIncludeAssessments, setDefaultIncludeAssessments] = useState(true);
  const [supportingFilesGuidance, setSupportingFilesGuidance] = useState("");
  const [supportingFilesExpected, setSupportingFilesExpected] = useState(false);
  const [documentTypesText, setDocumentTypesText] = useState("");
  const [fileError, setFileError] = useState<string | null>(null);
  const [submitAttempted, setSubmitAttempted] = useState(false);



  const validation = useMemo(
    () =>
      validateReportTemplateUploadForm({
        name,
        description,
        aiInstructions,
        supportingFilesGuidance,
        documentTypesText,
        file,
      }),
    [name, description, aiInstructions, supportingFilesGuidance, documentTypesText, file],
  );

  if (!isOpen) return null;

  const showErrors = submitAttempted;
  const canSubmit = validation.ok;

  const handleFileChange = (nextFile: File | null) => {
    if (!nextFile) {
      setFile(null);
      setFileError(null);
      return;
    }

    const error = validateReportTemplateFile(nextFile);
    setFile(nextFile);
    setFileError(error);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitAttempted(true);

    if (!validation.ok || !file) return;

    const fileContent = await readFileAsBase64(file);
    const supportingFileTypes = parseDocumentTypes(documentTypesText);

    await onSubmit({
      name: name.trim(),
      description: description.trim() || undefined,
      aiInstructions: aiInstructions.trim() || undefined,
      fileContent,
      originalName: file.name,
      mimeType: file.type || "application/octet-stream",
      defaultIncludeProfile,
      defaultIncludeNotes,
      defaultIncludeAssessments,
      supportingFilesGuidance: supportingFilesGuidance.trim() || undefined,
      supportingFilesExpected,
      supportingFileTypes,
    });
  };

  const handleClose = () => {
    if (isSubmitting) return;
    onClose();
  };

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
      <div className="relative flex max-h-[88vh] w-full max-w-lg flex-col overflow-hidden rounded-2xl bg-white shadow-xl">
        <div className="flex shrink-0 items-start justify-between gap-3 border-b border-(--neutral-100) px-5 py-4">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-(--neutral-950)">Upload Report Template</h2>
            <p className="mt-0.5 text-sm text-(--text-neutral-600)">
              Word or PDF layout for AI-filled client reports
            </p>
          </div>
          <button
            type="button"
            onClick={handleClose}
            disabled={isSubmitting}
            className="cursor-pointer rounded-full p-1.5 text-(--text-neutral-600) transition-colors hover:bg-(--neutral-50) hover:text-(--neutral-950)"
          >
            <X size={20} />
          </button>
        </div>

        <form
          id={formId}
          onSubmit={(event) => void handleSubmit(event)}
          className="flex min-h-0 flex-1 flex-col overflow-hidden"
        >
          <div className="flex flex-1 flex-col gap-6 overflow-y-auto px-5 py-4 custom-scrollbar">
            <div>
              <CustomInput
                label="Template name"
                value={name}
                onChange={(event) =>
                  setName(clampToMaxLength(event.target.value, REPORT_TEMPLATE_LIMITS.name))
                }
                maxLength={REPORT_TEMPLATE_LIMITS.name}
                required
              />
              <FieldError message={showErrors ? validation.errors.name : undefined} />
            </div>

            <div>
              <CustomTextarea
                label="Description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                hint="Optional admin notes"
                rows={2}
                className="min-h-0"
                textareaClassName="min-h-16 resize-y"
              />
            </div>

            <div>
              <CustomTextarea
                label="AI instructions"
                value={aiInstructions}
                onChange={(event) => setAiInstructions(event.target.value)}
                hint="Optional tone or structure guidance"
                rows={2}
                className="min-h-0"
                textareaClassName="min-h-16 resize-y"
              />
            </div>

            <div className="space-y-1.5">
              <p className="text-sm font-medium text-(--neutral-950)">
                Template file (.docx or .pdf) <span className="text-red-500">*</span>
              </p>
              <label
                className={cn(
                  "flex cursor-pointer items-center gap-3 rounded-xl border border-dashed px-4 py-3 transition-colors",
                  fileError || (showErrors && validation.errors.file)
                    ? "border-red-200 bg-red-50/40"
                    : "border-(--neutral-200) bg-(--neutral-50) hover:border-(--neutral-300) hover:bg-white",
                  file && !fileError && "border-(--neutral-100) bg-white",
                )}
              >
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-(--bg-primary-50) text-(--text-primary-500)">
                  <FileUp size={18} />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium text-(--neutral-950)">
                    {file ? file.name : "Choose file"}
                  </span>
                  <span className="block text-xs text-(--text-neutral-600)">
                    {file ? "Click to replace" : ".docx or .pdf · Max 15 MB"}
                  </span>
                </span>
                <input
                  type="file"
                  className="hidden"
                  accept=".docx,.pdf,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  onChange={(event) => {
                    handleFileChange(event.target.files?.[0] ?? null);
                    event.currentTarget.value = "";
                  }}
                />
              </label>
              <FieldError message={fileError ?? (showErrors ? validation.errors.file : undefined)} />
            </div>

            <div className="rounded-xl border border-(--neutral-100) bg-(--neutral-50) p-3 space-y-2.5">
              <p className="text-xs font-semibold uppercase tracking-wide text-(--text-neutral-600)">
                Default data sources
              </p>
              <label className="flex cursor-pointer items-center gap-2.5 text-sm text-(--neutral-950)">
                <Checkbox
                  id={`${formId}-include-profile`}
                  checked={defaultIncludeProfile}
                  onChange={(event) => setDefaultIncludeProfile(event.target.checked)}
                />
                Include client profile
              </label>
              <label className="flex cursor-pointer items-center gap-2.5 text-sm text-(--neutral-950)">
                <Checkbox
                  id={`${formId}-include-notes`}
                  checked={defaultIncludeNotes}
                  onChange={(event) => setDefaultIncludeNotes(event.target.checked)}
                />
                Include sessions & notes
              </label>
              <label className="flex cursor-pointer items-center gap-2.5 text-sm text-(--neutral-950)">
                <Checkbox
                  id={`${formId}-include-assessments`}
                  checked={defaultIncludeAssessments}
                  onChange={(event) => setDefaultIncludeAssessments(event.target.checked)}
                />
                Include assessments
              </label>
            </div>

            <div>
              <CustomTextarea
                label="Supporting files guidance"
                value={supportingFilesGuidance}
                onChange={(event) => setSupportingFilesGuidance(event.target.value)}
                hint="Shown to therapists when generating a report"
                rows={2}
                className="min-h-0"
                textareaClassName="min-h-16 resize-y"
              />
            </div>

            <label className="flex cursor-pointer items-center gap-2.5 text-sm text-(--neutral-950)">
              <Checkbox
                id={`${formId}-supporting-expected`}
                checked={supportingFilesExpected}
                onChange={(event) => setSupportingFilesExpected(event.target.checked)}
              />
              Expect supporting files before generation
            </label>

            <div>
              <CustomTextarea
                label="Document types"
                value={documentTypesText}
                onChange={(event) => setDocumentTypesText(event.target.value)}
                hint="One type per line"
                rows={3}
                className="min-h-0"
                textareaClassName="min-h-20 resize-y"
              />
            </div>
          </div>

          <div className="flex shrink-0 items-center justify-end gap-3 border-t border-(--neutral-100) px-5 py-4">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={isSubmitting}
              className="h-10 cursor-pointer rounded-full border-(--neutral-100) bg-white px-5 text-sm font-normal text-(--text-neutral-600) hover:bg-(--neutral-50)"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={!canSubmit || isSubmitting}
              className={cn(
                "h-10 rounded-full px-5 text-sm font-normal transition-all",
                canSubmit && !isSubmitting ? "cursor-pointer" : "cursor-not-allowed",
              )}
              loading={isSubmitting}
              loadingLabel="Uploading..."
            >
              Upload
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

const UploadTemplateModal = (props: UploadTemplateModalProps) => props.isOpen ? <UploadTemplateModalContent {...props} /> : null;

export default UploadTemplateModal;
