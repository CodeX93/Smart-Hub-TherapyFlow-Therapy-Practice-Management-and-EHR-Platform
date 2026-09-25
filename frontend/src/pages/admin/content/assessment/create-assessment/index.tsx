
import { ContentLoader } from "@/components/shared/ContentLoader";
import CreateAssessmentHeader from "@/components/create-assessment-sections/CreateAssessmentHeader";
import { useState } from "react";
import type {
  AssessmentSection,
  AssessmentQuestion,
} from "../../../../../types/create-assessment";
import ReorderSectionsModal from "../../../../../components/create-assessment-sections/ReorderSectionsModal";
import DeleteAssessmentModal from "@/components/admin-assessment-sections/DeleteAssessmentModal";
import PreviewAssessment from "@/components/admin-assessment-sections/PreviewAssessment";
import AssessmentEmptyState from "../../../../../components/create-assessment-sections/AssessmentEmptyState";
import AssessmentSectionsList from "../../../../../components/create-assessment-sections/AssessmentSectionsList";
import { useLocation } from "react-router-dom";
import {
  useLazyGetAssessmentTemplateByIdQuery,
  useUpdateAssessmentTemplateMutation,
  type AdminAssessmentTemplate,
  type UpdateAssessmentTemplatePayload,
} from "@/store/api/admin/clients.api";
import { useEffect } from "react";

import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import type {
  AccessLevel,
  ReportSectionType,
  QuestionType,
} from "../../../../../types/create-assessment";

const generateId = () => Math.random().toString(36).substr(2, 9);

function toAccessLevelLabel(value?: string | null): AccessLevel {
  const normalized = (value || "")
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, "_");
  if (normalized === "therapist_only") return "Therapist Only";
  if (normalized === "client_only") return "Client Only";
  if (normalized === "shared") return "Shared";
  if (value === "Therapist Only" || value === "Client Only" || value === "Shared") {
    return value;
  }
  return "Shared";
}

function toAccessLevelApi(value: AccessLevel): string {
  switch (value) {
    case "Therapist Only":
      return "therapist_only";
    case "Client Only":
      return "client_only";
    case "Shared":
    default:
      return "shared";
  }
}

function mapApiToFrontend(apiTemplate: AdminAssessmentTemplate): AssessmentSection[] {
  return (apiTemplate.sections || []).map((s) => ({
    id: String(s.id),
    title: s.title || "",
    accessLevel: toAccessLevelLabel(s.accessLevel),
    description: s.description || "",
    reportSectionType:
      (s.reportMapping as ReportSectionType) || "None (Regular Section)",
    enableScoring: !!s.isScoring,
    aiReportInstructions: s.aiReportPrompt || "",
    questions: (s.questions || []).map((q) => ({
      id: String(q.id),
      text: q.questionText || "",
      type: (q.questionType as QuestionType) || "Short Answer Text",
      required: !!q.isRequired,
      options: (q.options || []).map((o) => ({
        id: String(o.id),
        text: o.optionText || "",
        score: o.optionValue,
      })),
    })),
  }));
}

const CreateAssessment = () => {
  const location = useLocation();
  const templateIdFromState = location.state?.templateId;

  const [sections, setSections] = useState<AssessmentSection[]>([]);
  const [templateName, setTemplateName] = useState("Mental Health Assessment");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("success");

  const [getTemplateById, { isFetching }] = useLazyGetAssessmentTemplateByIdQuery();

  const [fullTemplateData, setFullTemplateData] =
    useState<AdminAssessmentTemplate | null>(null);
  const isInitialTemplateLoad = isFetching && !fullTemplateData;

  const [updateAssessmentTemplate, { isLoading: isSaving }] =
    useUpdateAssessmentTemplateMutation();

  const [isReorderModalOpen, setIsReorderModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [sectionToDelete, setSectionToDelete] = useState<{
    index: number;
    title: string;
  } | null>(null);

  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [expandedPreviewSectionId, setExpandedPreviewSectionId] = useState<
    string | null
  >(null);

  const syncTemplateFromApi = (data: AdminAssessmentTemplate) => {
    setFullTemplateData(data);
    setTemplateName(data.name || data.title || "Mental Health Assessment");
    // Never replace in-editor sections with an empty/sparse API payload (common after PUT).
    setSections((previous) => {
      const mapped = mapApiToFrontend(data);
      if (mapped.length === 0 && previous.length > 0) {
        return previous;
      }
      return mapped;
    });
  };

  const showToast = (message: string, type: "success" | "error") => {
    setToastType(type);
    setToastMessage(message);
  };

  const reloadTemplateFromServer = async () => {
    if (!templateIdFromState) return;
    const freshData = await getTemplateById(Number(templateIdFromState)).unwrap();
    if (freshData) {
      syncTemplateFromApi(freshData);
    }
  };

  useEffect(() => {
    if (templateIdFromState) {
      const templateId = Number(templateIdFromState);
      if (templateId) {
        getTemplateById(templateId)
          .unwrap()
          .then((data) => {
            if (data) {
              syncTemplateFromApi(data);
            }
          })
          .catch((err) => {
            showToast(getApiErrorMessage(err), "error");
          });
      }
    }
  }, [templateIdFromState, getTemplateById]);

  const buildUpdatePayload = (): UpdateAssessmentTemplatePayload => ({
    name: templateName,
    description: fullTemplateData?.description,
    category: fullTemplateData?.category,
    isStandardized: fullTemplateData?.isStandardized,
    version:
      fullTemplateData?.version !== undefined && fullTemplateData?.version !== null
        ? String(fullTemplateData.version)
        : undefined,
    isActive: fullTemplateData?.isActive,
    sections: sections.map((s, sIdx) => ({
      title: s.title,
      description: s.description,
      accessLevel: toAccessLevelApi(s.accessLevel),
      isScoring: s.enableScoring,
      sortOrder: sIdx,
      reportMapping:
        s.reportSectionType === "None (Regular Section)" ? null : s.reportSectionType,
      aiReportPrompt: s.aiReportInstructions,
      questions: s.questions.map((q, qIdx) => ({
        questionText: q.text,
        questionType: q.type,
        isRequired: q.required,
        sortOrder: qIdx,
        options: (q.options || []).map((o, oIdx) => ({
          optionText: o.text,
          optionValue: o.score || 0,
          sortOrder: oIdx,
        })),
      })),
    })),
  });

  const onAddSection = () => {
    const newSection: AssessmentSection = {
      id: generateId(),
      title: "",
      accessLevel: "Shared",
      description: "",
      reportSectionType: "None (Regular Section)",
      enableScoring: false,
      aiReportInstructions: "",
      questions: [],
    };
    setSections([...sections, newSection]);
  };

  const onUpdateSection = (
    sIdx: number,
    updates: Partial<AssessmentSection>,
  ) => {
    const newSections = [...sections];
    newSections[sIdx] = { ...newSections[sIdx], ...updates };
    setSections(newSections);
  };

  const onDuplicateSection = (sIdx: number) => {
    const sec = sections[sIdx];
    const newSection: AssessmentSection = {
      ...sec,
      id: generateId(),
      questions: sec.questions.map((q) => ({
        ...q,
        id: generateId(),
        options: q.options?.map((opt) => ({ ...opt, id: generateId() })),
      })),
    };
    const newSections = [...sections];
    newSections.splice(sIdx + 1, 0, newSection);
    setSections(newSections);
  };

  const onDeleteSection = (sIdx: number) => {
    setSectionToDelete({
      index: sIdx,
      title: sections[sIdx].title || `Section ${sIdx + 1}`,
    });
    setIsDeleteModalOpen(true);
  };

  const onConfirmDeleteSection = () => {
    if (sectionToDelete !== null) {
      setSections(sections.filter((_, i) => i !== sectionToDelete.index));
      setIsDeleteModalOpen(false);
      setSectionToDelete(null);
    }
  };

  const onAddQuestion = (sIdx: number) => {
    const newQuestion: AssessmentQuestion = {
      id: generateId(),
      text: "",
      type: "Short Answer Text",
      required: false,
    };
    const newSections = [...sections];
    newSections[sIdx] = {
      ...newSections[sIdx],
      questions: [...newSections[sIdx].questions, newQuestion],
    };
    setSections(newSections);
  };

  const onUpdateQuestion = (
    sIdx: number,
    qIdx: number,
    updates: Partial<AssessmentQuestion>,
  ) => {
    const newSections = [...sections];
    newSections[sIdx].questions[qIdx] = {
      ...newSections[sIdx].questions[qIdx],
      ...updates,
    };
    setSections(newSections);
  };

  const onSaveTemplate = async () => {
    if (!templateIdFromState) {
      showToast("Template ID not found.", "error");
      return;
    }

    try {
      const updated = await updateAssessmentTemplate({
        id: Number(templateIdFromState),
        body: buildUpdatePayload(),
      }).unwrap();

      showToast("Assessment template updated successfully.", "success");

      // Keep editor content visible: PUT responses can omit nested questions.
      // Refresh from GET; if that is also sparse, preserve current sections.
      try {
        await reloadTemplateFromServer();
      } catch {
        if (updated) {
          setFullTemplateData((previous) => ({
            ...(previous ?? updated),
            ...updated,
            sections:
              (updated.sections?.length ?? 0) > 0
                ? updated.sections
                : previous?.sections ?? updated.sections,
          }));
          setTemplateName(
            updated.name || updated.title || templateName,
          );
        }
      }
    } catch (err) {
      showToast(getApiErrorMessage(err), "error");
      try {
        await reloadTemplateFromServer();
      } catch {
        // Keep the surfaced save error toast.
      }
    }
  };

  const onDuplicateQuestion = (sIdx: number, qIdx: number) => {
    const q = sections[sIdx].questions[qIdx];
    const newQuestion: AssessmentQuestion = {
      ...q,
      id: generateId(),
      options: q.options?.map((opt) => ({ ...opt, id: generateId() })),
    };
    const newSections = [...sections];
    newSections[sIdx].questions.splice(qIdx + 1, 0, newQuestion);
    setSections(newSections);
  };

  const onDeleteQuestion = (sIdx: number, qIdx: number) => {
    const newSections = [...sections];
    newSections[sIdx].questions = newSections[sIdx].questions.filter(
      (_, i) => i !== qIdx,
    );
    setSections(newSections);
  };

  return (
    <div className="bg-(--bg-primary-light) min-h-screen">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="max-w-4xl mx-auto min-w-0 p-5 pb-20 flex flex-col gap-5">
        <div className="sticky top-0 z-40 bg-(--bg-primary-light) py-2">
          <CreateAssessmentHeader
            title={isInitialTemplateLoad ? "Loading..." : templateName}
            onSaveTemplate={onSaveTemplate}
            onPreview={() => setIsPreviewModalOpen((prev) => !prev)}
            isPreviewModalOpen={isPreviewModalOpen}
            isSaving={isSaving}
          />
        </div>

        {isInitialTemplateLoad ? (
          <div className="py-20 flex justify-center">
            <ContentLoader variant="inline" size="md" />
          </div>
        ) : sections.length === 0 ? (
          <AssessmentEmptyState onAddSection={onAddSection} />
        ) : isPreviewModalOpen ? (
          <PreviewAssessment
            sections={sections}
            expandedSectionId={expandedPreviewSectionId}
            toggleSection={(id) =>
              setExpandedPreviewSectionId((p) => (p === id ? null : id))
            }
          />
        ) : (
          <AssessmentSectionsList
            sections={sections}
            onAddSection={onAddSection}
            onUpdateSection={onUpdateSection}
            onDuplicateSection={onDuplicateSection}
            onDeleteSection={onDeleteSection}
            onOpenReorder={() => setIsReorderModalOpen(true)}
            onAddQuestion={onAddQuestion}
            onUpdateQuestion={onUpdateQuestion}
            onDuplicateQuestion={onDuplicateQuestion}
            onDeleteQuestion={onDeleteQuestion}
            setSections={setSections}
          />
        )}
      </div>

      <ReorderSectionsModal
        isOpen={isReorderModalOpen}
        onClose={() => setIsReorderModalOpen(false)}
        sections={sections}
        onSave={(ns) => {
          setSections(ns);
          setIsReorderModalOpen(false);
        }}
      />

      <DeleteAssessmentModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          setIsDeleteModalOpen(false);
          setSectionToDelete(null);
        }}
        templateTitle={sectionToDelete?.title || ""}
        onConfirm={onConfirmDeleteSection}
        type="Section"
      />
    </div>
  );
};

export default CreateAssessment;
