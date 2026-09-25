import { useId, useMemo, useState } from "react";
import { X } from "lucide-react";
import type { ReportTemplate } from "@/store/api/admin/reportTemplates.api";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import { cn } from "@/lib/utils";
import {
  clampToMaxLength,
  parseDocumentTypes,
  REPORT_TEMPLATE_LIMITS,
  validateReportTemplateEditForm,
} from "@/utils/reportTemplateValidation";

interface EditTemplateModalProps {
  template: ReportTemplate | null;
  isSubmitting?: boolean;
  onClose: () => void;
  onSubmit: (payload: {
    name?: string;
    description?: string;
    aiInstructions?: string;
    structureText?: string;
    defaultIncludeProfile?: boolean;
    defaultIncludeNotes?: boolean;
    defaultIncludeAssessments?: boolean;
    supportingFilesGuidance?: string;
    supportingFilesExpected?: boolean;
    supportingFileTypes?: string[];
  }) => Promise<void>;
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className="px-1 text-xs text-red-600">{message}</p>;
}

const EditTemplateModalContent = ({
  template,
  isSubmitting = false,
  onClose,
  onSubmit,
}: EditTemplateModalProps) => {
  const formId = useId();
  const [name, setName] = useState(template?.name ?? "");
  const [description, setDescription] = useState(template?.description ?? "");
  const [aiInstructions, setAiInstructions] = useState(template?.aiInstructions ?? "");
  const [structureText, setStructureText] = useState(template?.structureText ?? "");
  const [defaultIncludeProfile, setDefaultIncludeProfile] = useState(Boolean(template?.defaultIncludeProfile));
  const [defaultIncludeNotes, setDefaultIncludeNotes] = useState(Boolean(template?.defaultIncludeNotes));
  const [defaultIncludeAssessments, setDefaultIncludeAssessments] = useState(Boolean(template?.defaultIncludeAssessments));
  const [supportingFilesGuidance, setSupportingFilesGuidance] = useState(template?.supportingFilesGuidance ?? "");
  const [supportingFilesExpected, setSupportingFilesExpected] = useState(Boolean(template?.supportingFilesExpected));
  const [documentTypesText, setDocumentTypesText] = useState((template?.supportingFileTypes ?? []).join("\n"));
  const [submitAttempted, setSubmitAttempted] = useState(false);



  const validation = useMemo(
    () =>
      validateReportTemplateEditForm({
        name,
        description,
        aiInstructions,
        structureText,
        supportingFilesGuidance,
        documentTypesText,
      }),
    [name, description, aiInstructions, structureText, supportingFilesGuidance, documentTypesText],
  );

  if (!template) return null;

  const showErrors = submitAttempted;
  const canSubmit = validation.ok;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitAttempted(true);
    if (!validation.ok) return;

    await onSubmit({
      name: name.trim(),
      description,
      aiInstructions,
      structureText,
      defaultIncludeProfile,
      defaultIncludeNotes,
      defaultIncludeAssessments,
      supportingFilesGuidance,
      supportingFilesExpected,
      supportingFileTypes: parseDocumentTypes(documentTypesText),
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
            <h2 className="truncate text-lg font-bold text-(--neutral-950)" title={template.name}>
              Edit Template
            </h2>
            <p className="mt-0.5 truncate text-sm text-(--text-neutral-600)" title={template.originalName ?? template.name}>
              {template.originalName ?? "Update template metadata and structure"}
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
                className="[&_input]:font-mono [&_input]:text-xs"
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
                textareaClassName="min-h-16 resize-y font-mono text-xs"
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
                textareaClassName="min-h-16 resize-y font-mono text-xs"
              />
            </div>

            <div>
              <CustomTextarea
                label="Structure text"
                value={structureText}
                onChange={(event) => setStructureText(event.target.value)}
                hint="Extracted outline used by AI generation"
                rows={6}
                className="min-h-0"
                textareaClassName="min-h-32 resize-y font-mono text-xs"
              />
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
                textareaClassName="min-h-16 resize-y font-mono text-xs"
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
              loadingLabel="Saving..."
            >
              Save
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

const EditTemplateModal = (props: EditTemplateModalProps) => props.template ? <EditTemplateModalContent key={props.template?.id} {...props} /> : null;

export default EditTemplateModal;
