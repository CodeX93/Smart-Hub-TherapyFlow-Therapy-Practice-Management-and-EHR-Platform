import { useState } from "react";
import { Trash2, X } from "lucide-react";
import { Button } from "../ui/button";
import CustomInput from "../form/CustomInput";
import CustomTextarea from "../form/CustomTextarea";
import CustomSelect from "../form/CustomSelect";
import type { SessionNoteAiTemplate } from "@/store/api/admin/sessionNoteAiTemplates.api";
import {
  SESSION_NOTE_AI_TEMPLATE_NAME_MAX,
  truncateSessionNoteAiTemplateName,
} from "@/utils/sessionNoteAiTemplateName";

interface SessionNoteAiTemplateModalProps {
  isOpen: boolean;
  onClose: () => void;
  templates: SessionNoteAiTemplate[];
  selectedTemplateId: number | null;
  isSaving?: boolean;
  startFresh?: boolean;
  onSave: (payload: { id?: number; name: string; instructions: string }) => void;
  /** Asks the parent to confirm and delete the template being edited. */
  onRequestDelete?: (templateId: number) => void;
}

const SessionNoteAiTemplateModalContent = ({
  isOpen,
  onClose,
  templates,
  selectedTemplateId,
  isSaving = false,
  startFresh = false,
  onSave,
  onRequestDelete,
}: SessionNoteAiTemplateModalProps) => {
  const initialTemplate = startFresh ? undefined : templates.find(template => template.id === selectedTemplateId) ?? templates[0];
  const [editingTemplateId, setEditingTemplateId] = useState<number | "new">(initialTemplate?.id ?? "new");
  const [templateName, setTemplateName] = useState(initialTemplate?.name.slice(0, SESSION_NOTE_AI_TEMPLATE_NAME_MAX) ?? "");
  const [instructions, setInstructions] = useState(initialTemplate?.instructions ?? "");
  const [formError, setFormError] = useState<string | null>(null);
  const [initialTemplateLoaded, setInitialTemplateLoaded] = useState(startFresh || Boolean(initialTemplate));
  if (!initialTemplateLoaded && initialTemplate) {
    setInitialTemplateLoaded(true);
    if (editingTemplateId === "new" && !templateName && !instructions) {
      setEditingTemplateId(initialTemplate.id);
      setTemplateName(initialTemplate.name.slice(0, SESSION_NOTE_AI_TEMPLATE_NAME_MAX));
      setInstructions(initialTemplate.instructions);
    }
  }





  if (!isOpen) return null;

  const templateOptions = [
    { value: "new", label: "New template" },
    ...templates.map((template) => ({
      value: String(template.id),
      label: truncateSessionNoteAiTemplateName(template.name),
    })),
  ];

  const handleTemplateSelect = (value: string) => {
    if (value === "new") {
      setEditingTemplateId("new");
      setTemplateName("");
      setInstructions("");
      setFormError(null);
      return;
    }

    const template = templates.find((entry) => entry.id === Number(value));
    if (!template) return;
    setEditingTemplateId(template.id);
    setTemplateName(template.name.slice(0, SESSION_NOTE_AI_TEMPLATE_NAME_MAX));
    setInstructions(template.instructions);
    setFormError(null);
  };

  const handleSave = () => {
    const trimmedName = templateName.trim();
    const trimmedInstructions = instructions.trim();

    if (!trimmedName) {
      setFormError("Template name is required.");
      return;
    }
    if (trimmedName.length > SESSION_NOTE_AI_TEMPLATE_NAME_MAX) {
      setFormError(
        `Template name must be at most ${SESSION_NOTE_AI_TEMPLATE_NAME_MAX} characters.`,
      );
      return;
    }
    if (!trimmedInstructions) {
      setFormError("Custom instructions are required.");
      return;
    }

    setFormError(null);
    onSave({
      id: editingTemplateId === "new" ? undefined : editingTemplateId,
      name: trimmedName,
      instructions: trimmedInstructions,
    });
  };

  return (
    <div className="fixed inset-0 z-10000 flex items-center justify-center bg-black/50 p-4">
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-xl">
        <div className="flex shrink-0 items-start justify-between border-b border-(--neutral-200) p-6">
          <div>
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">
              AI template
            </h2>
            <p className="mt-1 text-sm text-(--text-neutral-600)">
              The instructions that tell the AI how to write the final note.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1 transition-colors hover:bg-(--neutral-50)"
            aria-label="Close"
          >
            <X size={22} className="text-(--text-neutral-600)" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-6 py-5">
          <div className="space-y-5">
              {templates.length > 0 ? (
                <CustomSelect
                  label="Saved templates"
                  placeholder="Select a template"
                  value={editingTemplateId === "new" ? "new" : String(editingTemplateId)}
                  onChange={handleTemplateSelect}
                  options={templateOptions}
                  isSearch={templateOptions.length > 6}
                />
              ) : null}

              <CustomInput
                label="Template Name"
                placeholder="e.g., CBT Session Template, EMDR Progress Notes"
                value={templateName}
                maxLength={SESSION_NOTE_AI_TEMPLATE_NAME_MAX}
                stopFloating
                onChange={(event) =>
                  setTemplateName(
                    event.target.value.slice(0, SESSION_NOTE_AI_TEMPLATE_NAME_MAX),
                  )
                }
                hint={`Max ${SESSION_NOTE_AI_TEMPLATE_NAME_MAX} characters. Give your template a descriptive name.`}
              />

              <CustomTextarea
                label="Custom Instructions"
                placeholder="Example: Create a session note template focused on cognitive behavioral therapy techniques, including detailed mood tracking, homework assignments, and specific CBT interventions used. Format it professionally for clinical documentation."
                value={instructions}
                onChange={(event) => setInstructions(event.target.value)}
                hint="Specific instructions about format, focus areas, therapy approach, or any special requirements."
                rows={8}
                className="min-h-48"
                textareaClassName="min-h-40 resize-y"
              />

              {formError ? (
                <p className="text-sm text-(--status-denied)">{formError}</p>
              ) : null}
          </div>
        </div>

        <div className="flex shrink-0 items-center justify-end gap-3 border-t border-(--neutral-200) p-6">
          {/* Deleting lives with the template it removes, not on the note behind it. */}
          {onRequestDelete && editingTemplateId !== "new" ? (
            <Button
              type="button"
              variant="outline"
              onClick={() => onRequestDelete(editingTemplateId)}
              className="mr-auto h-11 gap-2 rounded-full border-(--neutral-200) px-5 text-sm font-normal text-(--status-denied) hover:bg-red-50"
            >
              <Trash2 size={16} />
              Delete template
            </Button>
          ) : null}
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            className="h-11 rounded-full border-(--neutral-200) px-6 text-sm font-normal"
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handleSave}
            disabled={isSaving}
            loading={isSaving}
            loadingLabel="Saving..."
            className="h-11 rounded-full bg-(--bg-primary-dark) px-6 text-sm font-semibold text-white hover:bg-(--bg-primary-dark)/90 disabled:opacity-50"
          >
            Save Template
          </Button>
        </div>
      </div>
    </div>
  );
};

const SessionNoteAiTemplateModal = (props: SessionNoteAiTemplateModalProps) => props.isOpen ? <SessionNoteAiTemplateModalContent key={`${props.startFresh}:${props.selectedTemplateId}`} {...props} /> : null;

export default SessionNoteAiTemplateModal;
