import { useEffect, useMemo, useState } from "react";
import { X, BookOpen, AlertCircle, Check, Eye, Sparkles } from "lucide-react";
import { Button } from "../ui/button";
import CustomInput from "../form/CustomInput";
import CustomTextarea from "../form/CustomTextarea";
import CustomJoditEditor from "../shared/CustomJoditEditor";
import RiskAssessmentItem from "./RiskAssessmentItem";
import SessionNoteSignedView from "./SessionNoteSignedView";
import SessionNoteAmendmentModal from "./SessionNoteAmendmentModal";
import SessionNotePreviewModal from "./SessionNotePreviewModal";
import SessionNoteSteps, {
  type SessionNoteStep,
} from "./SessionNoteSteps";
import SessionNoteClinicalActionsMenu from "./SessionNoteClinicalActionsMenu";
import SessionNoteAiTemplateSelect from "./SessionNoteAiTemplateSelect";
import SessionNoteAiTemplateModal from "./SessionNoteAiTemplateModal";
import { RISK_ASSESSMENT_ITEMS } from "../../pages/therapist/therapist.static";
import {
  SESSION_NOTE_STARTER_TEMPLATES,
  type SessionNoteStarterTemplate,
} from "@/constants/sessionNoteStarterTemplates";
import { cn } from "../../lib/utils";
import ReviewTranscriptionModal, {
  type TranscribedData,
} from "./recording/ReviewTranscriptionModal";
import SelectLibraryModal from "./SelectLibraryModal";
import ConfirmationModal from "../shared/ConfirmationModal";
import {
  useCreateSessionNoteMutation,
  useDeleteSessionNoteMutation,
  useGetSessionNoteByIdQuery,
  useGetSessionNotesBySessionIdQuery,
  useLazyGetSessionNotePdfQuery,
  useUpdateSessionNoteMutation,
  useFinalizeSessionNoteMutation,
  useGetSessionNoteAmendmentsQuery,
  useCreateSessionNoteAmendmentMutation,
  useUnfinalizeSessionNoteMutation,
} from "@/store/api/admin/sessionNotes.api";
import { useGetLibraryCategoriesQuery } from "@/store/api/admin/libraryCategories.api";
import {
  useGetLibraryEntriesQuery,
  useIncrementLibraryEntryUsageMutation,
} from "@/store/api/admin/libraryEntries.api";
import {
  useLazyGetSessionTranscriptQuery,
  useSmartFillSessionTranscriptMutation,
} from "@/store/api/admin/sessionTranscripts.api";
import {
  useGetSessionNoteAiTemplatesQuery,
  useCreateSessionNoteAiTemplateMutation,
  useUpdateSessionNoteAiTemplateMutation,
  useDeleteSessionNoteAiTemplateMutation,
  useGenerateSessionNoteFinalContentMutation,
} from "@/store/api/admin/sessionNoteAiTemplates.api";
import { getStoredSessionTranscript } from "./recording/sessionTranscriptStore";
import {
  buildTranscriptFieldMapping,
  getSmartFillEmptyMessage,
  hasExtractedSmartFillFields,
} from "./recording/transcriptFieldMapping";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import { hasRichTextContent } from "@/utils/richText";
import { useAppSelector } from "@/store/hooks";
import {
  getApiErrorMessage,
  isAiProcessingConsentRequiredError,
} from "@/utils/apiError";
import {
  buildSessionNoteGenerateFormData,
  getSessionNoteGenerateErrorMessage,
  hasClinicalFieldsForGenerate,
  plainTextToHtml,
} from "@/utils/sessionNoteAiGenerate";
import type { LibraryEntry } from "@/store/api/admin/libraryEntries.api";
import type { SessionNoteTranscriptionResponse } from "@/store/api/admin/sessionNotes.api";
import {
  appendLibraryContent,
  buildSessionNoteCategoryIdMap,
  SESSION_NOTE_FIELD_LABELS,
  type SessionNoteLibraryField,
} from "@/utils/sessionNoteLibrary";
import {
  buildRiskScorePayload,
  RISK_BAND_LABELS,
  summarizeRiskAssessment,
  type RiskBand,
} from "@/utils/sessionNoteRiskScore";

const RISK_BAND_BADGE_CLASSES: Record<RiskBand, string> = {
  low: "bg-(--status-completed-light) text-(--status-completed-dark)",
  moderate: "bg-(--status-pending-light) text-(--status-pending-dark)",
  high: "bg-(--status-pending-light) text-(--status-pending-dark)",
  severe: "bg-(--status-overdue-light) text-(--status-overdue-dark)",
};

const RISK_UNASSESSED_BADGE_CLASSES =
  "bg-(--status-pending-light) text-(--status-pending-dark)";

const RISK_INCOMPLETE_MESSAGE =
  "Answer every risk item, or confirm no risk factors were identified, before finalizing.";

function htmlToPlainPreviewText(html: string): string {
  return html
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/p>/gi, "\n\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .trim();
}

function truncateTemplateNameForConfirm(name: string, maxLength = 48): string {
  if (name.length <= maxLength) return name;
  return `${name.slice(0, maxLength - 1).trimEnd()}…`;
}

interface AddSessionNoteModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated?: () => void;
  onChanged?: () => void;
  onToast?: (message: string, type: "success" | "error" | "info") => void;
  mode?: "create" | "view";
  noteId?: number | null;
  sessionData: {
    sessionId: number;
    clientId: number | null;
    therapistId: number | null;
    dateIso: string | null;
    dateTime: string;
    sessionType: string;
    clientName: string;
  };
  initialTranscriptionData?: SessionNoteTranscriptionResponse | null;
  autoOpenTranscriptReview?: boolean;
  initialEditMode?: boolean;
  hideLibraryButtons?: boolean;
  hideRatingsTab?: boolean;
}

function buildEmptyRiskAssessments(): RiskAssessment {
  const state: RiskAssessment = {};
  RISK_ASSESSMENT_ITEMS.forEach((item) => {
    state[item.id] = null;
  });
  return state;
}

/** Answers every item with its least concerning option, as one explicit act. */
function buildNoRiskAssessments(): RiskAssessment {
  const state: RiskAssessment = {};
  RISK_ASSESSMENT_ITEMS.forEach((item) => {
    state[item.id] = item.options[0];
  });
  return state;
}

type TabType = "clinical" | "risk" | "final" | "ratings";

interface NoteFormData {
  sessionFocus: string;
  symptoms: string;
  shortTermGoals: string;
  intervention: string;
  progress: string;
  remarks: string;
  recommendations: string;
}

interface RiskAssessment {
  [key: string]: string | null;
}

interface RatingsData {
  clientRating: string;
  therapistRating: string;
  progressTowardGoals: string;
  moodBefore: string;
  moodAfter: string;
}

const parseOptionalRating = (value: string) => {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  if (!/^\d+$/.test(trimmed)) return undefined;
  const parsed = Number.parseInt(trimmed, 10);
  if (!Number.isFinite(parsed) || parsed < 1 || parsed > 10) return undefined;
  return parsed;
};

interface AdvancedData {
  isDraft: boolean;
  isFinalized: boolean;
  draftContent: string;
  aiProcessingStatus: string;
}

const AddSessionNoteModalContent = ({
  isOpen,
  onClose,
  onCreated,
  onChanged,
  mode = "create",
  noteId = null,
  sessionData,
  initialTranscriptionData = null,
  autoOpenTranscriptReview = false,
  initialEditMode = false,
  onToast,
  hideLibraryButtons = false,
  hideRatingsTab = false,
}: AddSessionNoteModalProps) => {
  const [selectedTemplate, setSelectedTemplate] = useState<TabType>("clinical");
  const [formData, setFormData] = useState<NoteFormData>({
    sessionFocus: "",
    symptoms: "",
    shortTermGoals: "",
    intervention: "",
    progress: "",
    remarks: "",
    recommendations: "",
  });
  const [ratingsData, setRatingsData] = useState<RatingsData>({
    clientRating: "",
    therapistRating: "",
    progressTowardGoals: "",
    moodBefore: "",
    moodAfter: "",
  });
  const [advancedData, setAdvancedData] = useState<AdvancedData>({
    isDraft: true,
    isFinalized: false,
    draftContent: "",
    aiProcessingStatus: "",
  });
  const [isEditingView, setIsEditingView] = useState(initialEditMode);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showFinalizeConfirm, setShowFinalizeConfirm] = useState(false);
  const [showAmendmentModal, setShowAmendmentModal] = useState(false);
  const [showNotePreview, setShowNotePreview] = useState(false);
  const [pendingFinalize, setPendingFinalize] = useState(false);
  const [showDeleteTemplateConfirm, setShowDeleteTemplateConfirm] = useState(false);

  // Nothing is pre-selected: an unanswered item must stay unanswered in the
  // record rather than silently reading as the least concerning option.
  const [riskAssessments, setRiskAssessments] = useState<RiskAssessment>(
    () => buildEmptyRiskAssessments(),
  );

  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [transcriptionData, setTranscriptionData] =
    useState<SessionNoteTranscriptionResponse | null>(null);
  const [isLibraryModalOpen, setLibraryModalOpen] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [smartFillAlert, setSmartFillAlert] = useState<string | null>(null);
  const [activeField, setActiveField] = useState<SessionNoteLibraryField | null>(null);
  const [isAiTemplateModalOpen, setIsAiTemplateModalOpen] = useState(false);
  const [aiTemplateModalFresh, setAiTemplateModalFresh] = useState(false);
  const [selectedAiTemplateId, setSelectedAiTemplateId] = useState<number | null>(null);
  const [generatedPreviewPlainText, setGeneratedPreviewPlainText] = useState("");
  const [finalNoteEditorKey, setFinalNoteEditorKey] = useState(0);
  const authUser = useAppSelector((state) => state.auth.user);
  const { data: practiceConfig } = useGetPracticeConfigurationQuery(undefined, {
    skip: !isOpen,
  });
  const [createSessionNote, { isLoading: isCreatingNote }] = useCreateSessionNoteMutation();
  const [updateSessionNote, { isLoading: isUpdatingNote }] = useUpdateSessionNoteMutation();
  const [finalizeSessionNote, { isLoading: isFinalizingNote }] = useFinalizeSessionNoteMutation();
  const [createSessionNoteAmendment, { isLoading: isSavingAmendment }] =
    useCreateSessionNoteAmendmentMutation();
  const [unfinalizeSessionNote, { isLoading: isUnfinalizingNote }] =
    useUnfinalizeSessionNoteMutation();
  const [deleteSessionNote, { isLoading: isDeletingNote }] = useDeleteSessionNoteMutation();
  const [getSessionNotePdf, { isFetching: isDownloadingPdf }] = useLazyGetSessionNotePdfQuery();
  const [triggerGetSessionTranscript, { isFetching: isFetchingTranscript }] =
    useLazyGetSessionTranscriptQuery();
  const [smartFillSessionTranscript, { isLoading: isSmartFilling }] =
    useSmartFillSessionTranscriptMutation();
  const {
    data: aiTemplates = [],
    refetch: refetchAiTemplates,
  } = useGetSessionNoteAiTemplatesQuery(undefined, {
    skip: !isOpen,
    refetchOnMountOrArgChange: true,
  });
  const [createAiTemplate, { isLoading: isCreatingAiTemplate }] =
    useCreateSessionNoteAiTemplateMutation();
  const [updateAiTemplate, { isLoading: isUpdatingAiTemplate }] =
    useUpdateSessionNoteAiTemplateMutation();
  const [deleteAiTemplate, { isLoading: isDeletingAiTemplate }] =
    useDeleteSessionNoteAiTemplateMutation();
  const [generateFinalNote, { isLoading: isGeneratingFinalNote }] =
    useGenerateSessionNoteFinalContentMutation();
  const shouldLoadLibrary = isOpen && !hideLibraryButtons;
  const { data: libraryEntries = [], isLoading: isLoadingLibrary, refetch: refetchLibraryEntries } =
    useGetLibraryEntriesQuery(undefined, { skip: !shouldLoadLibrary });
  const { data: libraryCategories = [] } = useGetLibraryCategoriesQuery(undefined, {
    skip: !shouldLoadLibrary,
  });
  const [incrementLibraryEntryUsage] = useIncrementLibraryEntryUsageMutation();
  const categoryIdByField = useMemo(
    () => buildSessionNoteCategoryIdMap(libraryCategories),
    [libraryCategories],
  );
  const { data: bySessionNotes = [] } = useGetSessionNotesBySessionIdQuery(
    sessionData.sessionId,
    { skip: !isOpen || mode !== "view" || !sessionData.sessionId },
  );
  const resolvedNoteId = noteId ?? bySessionNotes[0]?.id ?? null;
  const {
    data: noteById,
    isLoading: isLoadingNote,
    isFetching: isFetchingNote,
    isError: isNoteError,
    error: noteError,
    refetch: refetchNoteById,
  } = useGetSessionNoteByIdQuery(resolvedNoteId ?? 0, {
    skip: !isOpen || mode !== "view" || !resolvedNoteId,
  });



  // Amendments only exist on a signed note, so they are only fetched for one.
  const { data: amendments = [] } = useGetSessionNoteAmendmentsQuery(resolvedNoteId ?? 0, {
    skip: !isOpen || mode !== "view" || !resolvedNoteId || !advancedData.isFinalized,
  });

  if (isOpen && selectedAiTemplateId !== null && !aiTemplates.some(template => template.id === selectedAiTemplateId)) {
    setSelectedAiTemplateId(null);
  }

  // With a single template there is no choice to make, so making the clinician
  // make it is just a step that blocks Generate for no reason.
  if (isOpen && selectedAiTemplateId === null && aiTemplates.length === 1) {
    setSelectedAiTemplateId(aiTemplates[0].id);
  }

  const canGenerateFinalNote = hasClinicalFieldsForGenerate(formData);

  const riskSummary = useMemo(
    () => summarizeRiskAssessment(RISK_ASSESSMENT_ITEMS, riskAssessments),
    [riskAssessments],
  );

  const riskScorePayload = useMemo(
    () => buildRiskScorePayload(RISK_ASSESSMENT_ITEMS, riskAssessments),
    [riskAssessments],
  );

  const selectedAiTemplate = useMemo(
    () => aiTemplates.find((template) => template.id === selectedAiTemplateId) ?? null,
    [aiTemplates, selectedAiTemplateId],
  );

  const openAiTemplateModal = (options?: { fresh?: boolean }) => {
    setAiTemplateModalFresh(Boolean(options?.fresh));
    setIsAiTemplateModalOpen(true);
  };

  const handleSaveAiTemplate = async (payload: {
    id?: number;
    name: string;
    instructions: string;
  }) => {
    try {
      if (payload.id) {
        const updated = await updateAiTemplate({
          id: payload.id,
          name: payload.name,
          instructions: payload.instructions,
        }).unwrap();
        setSelectedAiTemplateId(updated.id);
      } else {
        const created = await createAiTemplate({
          name: payload.name,
          instructions: payload.instructions,
        }).unwrap();
        setSelectedAiTemplateId(created.id);
      }
      onToast?.("Template saved successfully.", "success");
      setIsAiTemplateModalOpen(false);
    } catch (error) {
      onToast?.(getApiErrorMessage(error), "error");
    }
  };

  const handleUseStarterTemplate = (starter: SessionNoteStarterTemplate) => {
    void createAiTemplate({ name: starter.name, instructions: starter.instructions })
      .unwrap()
      .then((created) => {
        setSelectedAiTemplateId(created.id);
        onToast?.(`"${created.name}" added. Edit it any time.`, "success");
      })
      .catch((error) => onToast?.(getApiErrorMessage(error), "error"));
  };

  const handleDeleteAiTemplate = () => {
    if (!selectedAiTemplateId) return;

    void deleteAiTemplate(selectedAiTemplateId)
      .unwrap()
      .then(() => {
        setShowDeleteTemplateConfirm(false);
        setSelectedAiTemplateId(null);
        void refetchAiTemplates();
        onToast?.("Template deleted successfully.", "success");
      })
      .catch((error) => {
        onToast?.(getApiErrorMessage(error), "error");
      });
  };

  const handleGenerateFinalNote = () => {
    if (!canGenerateFinalNote) return;

    if (!selectedAiTemplateId) {
      onToast?.("Please create or select a template first.", "error");
      openAiTemplateModal({ fresh: true });
      return;
    }

    if (!sessionData.sessionId || !sessionData.clientId) {
      onToast?.("Please select a session first.", "error");
      return;
    }

    void generateFinalNote({
      clientId: sessionData.clientId,
      sessionId: sessionData.sessionId,
      templateId: selectedAiTemplateId,
      formData: buildSessionNoteGenerateFormData(formData),
    })
      .unwrap()
      .then((response) => {
        const plainText = response.generatedContent.trim();
        setGeneratedPreviewPlainText(plainText);
        setAdvancedData((previous) => ({
          ...previous,
          draftContent: plainText ? plainTextToHtml(plainText) : "",
        }));
        setFinalNoteEditorKey((previous) => previous + 1);
        setSelectedTemplate("final");
        onToast?.("Final note generated successfully.", "success");
      })
      .catch((error) => {
        onToast?.(
          getSessionNoteGenerateErrorMessage(error),
          isAiProcessingConsentRequiredError(error) ? "info" : "error",
        );
      });
  };


  const isNoteFinalized = Boolean(advancedData.isFinalized);
  // Finalized notes cannot enter edit mode; reopen first (same as ClientHub).
  const canEditNote = mode === "view" && !isNoteFinalized;
  const isReadOnly = mode === "view" && (!isEditingView || isNoteFinalized);

  useEffect(() => {
    if (mode !== "view") return;
    if (isNoteFinalized) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setIsEditingView(false);
      return;
    }
    if (initialEditMode) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setIsEditingView(true);
    }
  }, [mode, isNoteFinalized, initialEditMode]);

  const openLibrary = (field: SessionNoteLibraryField) => {
    if (isReadOnly || hideLibraryButtons) return;
    void refetchLibraryEntries();
    setActiveField(field);
    setLibraryModalOpen(true);
  };

  const renderLibraryTrigger = (field: SessionNoteLibraryField) => {
    if (hideLibraryButtons) return null;
    return (
      <button
        type="button"
        disabled={isReadOnly}
        className={`mb-2 flex w-fit items-center gap-2 ${
          isReadOnly ? "cursor-not-allowed opacity-50" : "cursor-pointer"
        }`}
        onClick={() => openLibrary(field)}
      >
        <BookOpen
          size={16}
          className={
            isReadOnly
              ? "text-(--text-neutral-400)"
              : "text-(--text-primary-500)"
          }
        />
        <span
          className={`text-sm font-medium ${
            isReadOnly
              ? "text-(--text-neutral-400)"
              : "text-(--text-primary-500)"
          }`}
        >
          Library
        </span>
      </button>
    );
  };

  const handleLibrarySelect = (entry: LibraryEntry) => {
    if (!activeField) return;

    setFormData((prev) => ({
      ...prev,
      [activeField]: appendLibraryContent(prev[activeField], entry.content),
    }));

    void incrementLibraryEntryUsage(entry.id).catch(() => {
      // Usage tracking is best-effort; field text is already inserted.
    });
  };

  const handleApplyTranscription = (data: Partial<TranscribedData>) => {
    setFormData((prev) => ({
      ...prev,
      ...data,
    }));
    setIsReviewModalOpen(false);
    setSubmitError(null);
  };

  const openSmartFillReview = (data: SessionNoteTranscriptionResponse) => {
    setTranscriptionData(data);
    if (canEditNote) setIsEditingView(true);
    setSelectedTemplate("clinical");
    setIsReviewModalOpen(true);
  };

  const handleSmartFillClick = () => {
    const runSmartFill = (rawTranscription: string) => {
      setSmartFillAlert(null);
      void smartFillSessionTranscript({ sessionId: sessionData.sessionId })
        .unwrap()
        .then((response) => {
          const transcriptText = response.transcript?.trim() || rawTranscription;
          const extractedFields = response.mappedFields ?? {};

          if (!hasExtractedSmartFillFields(extractedFields)) {
            setSmartFillAlert(getSmartFillEmptyMessage(response.message));
            return;
          }

          openSmartFillReview({
            success: true,
            rawTranscription: transcriptText,
            mappedFields: extractedFields,
          });
        })
        .catch((error) => {
          const message = getApiErrorMessage(error);
          setSmartFillAlert(message);
          onToast?.(message, "error");
        });
    };

    const existingTranscript =
      transcriptionData?.rawTranscription?.trim() ||
      getStoredSessionTranscript(sessionData.sessionId)?.rawTranscription?.trim() ||
      "";

    if (existingTranscript) {
      runSmartFill(existingTranscript);
      return;
    }

    void triggerGetSessionTranscript(sessionData.sessionId)
      .unwrap()
      .then((response) => {
        const finalTranscript =
          response.content?.trim() || response.finalTranscript?.trim() || "";
        if (!finalTranscript) {
          onToast?.("Record the session first.", "info");
          return;
        }
        runSmartFill(finalTranscript);
      })
      .catch(() => {
        onToast?.("Record the session first.", "info");
      });
  };

  useEffect(() => {
    if (isNoteError) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSubmitError(getApiErrorMessage(noteError));
    }
  }, [isNoteError, noteError]);



  useEffect(() => {
    if (!isOpen) return;
    if (initialTranscriptionData) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setTranscriptionData(initialTranscriptionData);
      if (mode === "view" && !advancedData.isFinalized) {
        setIsEditingView(true);
      }
      if (autoOpenTranscriptReview) {
        setIsReviewModalOpen(true);
      }
      return;
    }

    const cachedTranscript = getStoredSessionTranscript(sessionData.sessionId);
    if (cachedTranscript?.rawTranscription?.trim()) {

      setTranscriptionData({
        success: true,
        rawTranscription: cachedTranscript.rawTranscription,
        mappedFields:
          Object.keys(cachedTranscript.mappedFields ?? {}).length > 0
            ? cachedTranscript.mappedFields
            : buildTranscriptFieldMapping(cachedTranscript.rawTranscription),
      });
      return;
    }


    setTranscriptionData(null);
    void triggerGetSessionTranscript(sessionData.sessionId)
      .unwrap()
      .then((response) => {
        const finalTranscript =
          response.content?.trim() || response.finalTranscript?.trim() || "";
        if (!finalTranscript) return;
        setTranscriptionData({
          success: true,
          rawTranscription: finalTranscript,
          mappedFields: buildTranscriptFieldMapping(finalTranscript),
        });
      })
      .catch(() => {
        setTranscriptionData(null);
      });
  }, [
    autoOpenTranscriptReview,
    initialTranscriptionData,
    isOpen,
    mode,
    sessionData.sessionId,
    triggerGetSessionTranscript,
  ]);

  useEffect(() => {
    if (!noteById) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setFormData({
      sessionFocus: noteById.sessionFocus ?? "",
      symptoms: noteById.symptoms ?? "",
      shortTermGoals: noteById.shortTermGoals ?? "",
      intervention: noteById.intervention ?? "",
      progress: noteById.progress ?? "",
      remarks: noteById.remarks ?? "",
      recommendations: noteById.recommendations ?? "",
    });
    setRatingsData({
      clientRating: noteById.clientRating?.toString() ?? "",
      therapistRating: noteById.therapistRating?.toString() ?? "",
      progressTowardGoals: noteById.progressTowardGoals?.toString() ?? "",
      moodBefore: noteById.moodBefore?.toString() ?? "",
      moodAfter: noteById.moodAfter?.toString() ?? "",
    });
    const rawDraftSource =
      noteById.draftContent?.trim() || noteById.generatedContent?.trim() || "";
    const draftContent =
      rawDraftSource && !rawDraftSource.includes("<")
        ? plainTextToHtml(rawDraftSource)
        : noteById.draftContent ?? noteById.generatedContent ?? "";
    setAdvancedData({
      isDraft: noteById.isFinalized ? Boolean(noteById.isDraft) : true,
      isFinalized: Boolean(noteById.isFinalized),
      draftContent,
      aiProcessingStatus: noteById.aiProcessingStatus ?? "",
    });
    const previewSource =
      noteById.generatedContent?.trim() || noteById.draftContent?.trim() || "";
    setGeneratedPreviewPlainText(
      previewSource.includes("<")
        ? htmlToPlainPreviewText(previewSource)
        : previewSource,
    );
    if (previewSource) {
      setFinalNoteEditorKey((previous) => previous + 1);
    }

    const riskValueByItemId: Record<string, number | null | undefined> = {
      suicidal_ideation: noteById.riskSuicidalIdeation,
      homicidal_ideation: noteById.riskHomicidalIdeation,
      substance_abuse: noteById.riskSubstanceUse,
      self_harm: noteById.riskSelfHarm,
      danger_to_others: noteById.riskAggression,
      psychosis: noteById.riskPsychosis,
      impulse_control: noteById.riskImpulsivity,
      medication_compliance: noteById.riskNonAdherence,
    };
    setRiskAssessments((prev) => {
      const next: RiskAssessment = { ...prev };
      RISK_ASSESSMENT_ITEMS.forEach((item) => {
        const score = riskValueByItemId[item.id];
        if (typeof score === "number" && score >= 0 && score < item.options.length) {
          next[item.id] = item.options[score];
        }
      });
      return next;
    });
  }, [noteById]);

  useEffect(() => {
    if (hideRatingsTab && selectedTemplate === "ratings") {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedTemplate("clinical");
    }
  }, [hideRatingsTab, selectedTemplate]);

  if (!isOpen) return null;

  const handleInputChange = (field: keyof NoteFormData, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };
  const handleRatingChange = (field: keyof RatingsData, value: string) => {
    const trimmed = value.trim();

    if (!trimmed) {
      setRatingsData((prev) => ({ ...prev, [field]: "" }));
      return;
    }

    if (!/^\d+$/.test(trimmed)) return;

    const parsed = Number.parseInt(trimmed, 10);
    if (!Number.isFinite(parsed) || parsed < 1 || parsed > 10) return;

    setRatingsData((prev) => ({ ...prev, [field]: String(parsed) }));
  };
  // Finalizing signs the note, so every risk item has to carry a real answer
  // first — including an explicit "none", which the no-risk-factors action sets.
  const blockFinalizeWhileRiskIncomplete = () => {
    if (riskSummary.isComplete) return false;
    setSubmitError(RISK_INCOMPLETE_MESSAGE);
    setSelectedTemplate("risk");
    onToast?.(RISK_INCOMPLETE_MESSAGE, "error");
    return true;
  };

  const requestFinalize = () => {
    if (blockFinalizeWhileRiskIncomplete()) return;
    setShowFinalizeConfirm(true);
  };

  const confirmFinalize = () => {
    setShowFinalizeConfirm(false);
    setPendingFinalize(true);
    if (mode === "create") {
      handleCreateNote(true);
    } else {
      handleUpdateNote(true);
    }
  };

  const handleNoRiskFactors = () => {
    if (isReadOnly) return;
    setRiskAssessments(buildNoRiskAssessments());
    setSubmitError(null);
  };

  const handleClearRiskAssessment = () => {
    if (isReadOnly) return;
    setRiskAssessments(buildEmptyRiskAssessments());
  };

  const handleCreateNote = (finalize: boolean) => {

    const resolvedTherapistId = sessionData.therapistId ?? authUser?.id ?? null;
    if (!sessionData.sessionId || !sessionData.clientId || !resolvedTherapistId || !sessionData.dateIso) {
      setSubmitError("Missing required session details. Please open this note from a valid session.");
      return;
    }

    setSubmitError(null);
    // The note is always saved as a draft first: finalContent is written by the
    // finalize endpoint from the content that is already stored, so the content
    // has to reach the server before the note is signed.
    const wantsFinalized = finalize;
    void createSessionNote({
      sessionId: sessionData.sessionId,
      clientId: sessionData.clientId,
      therapistId: resolvedTherapistId,
      date: new Date(sessionData.dateIso).toISOString(),
      sessionFocus: formData.sessionFocus || undefined,
      symptoms: formData.symptoms || undefined,
      shortTermGoals: formData.shortTermGoals || undefined,
      intervention: formData.intervention || undefined,
      progress: formData.progress || undefined,
      remarks: formData.remarks || undefined,
      recommendations: formData.recommendations || undefined,
      ...(hideRatingsTab
        ? {}
        : {
            clientRating: parseOptionalRating(ratingsData.clientRating),
            therapistRating: parseOptionalRating(ratingsData.therapistRating),
            progressTowardGoals: parseOptionalRating(ratingsData.progressTowardGoals),
            moodBefore: parseOptionalRating(ratingsData.moodBefore),
            moodAfter: parseOptionalRating(ratingsData.moodAfter),
          }),
      ...riskScorePayload,
      isDraft: true,
      isFinalized: false,
      draftContent: advancedData.draftContent || undefined,
      aiProcessingStatus: advancedData.aiProcessingStatus || undefined,
      aiEnabled: false,
    })
      .unwrap()
      .then(async (created) => {
        if (wantsFinalized && created?.id) {
          await finalizeSessionNote(created.id).unwrap();
        }
        onCreated?.();
        onToast?.(
          wantsFinalized
            ? "Session note finalized successfully."
            : "Session note saved successfully.",
          "success",
        );
        onClose();
        setFormData({
          sessionFocus: "",
          symptoms: "",
          shortTermGoals: "",
          intervention: "",
          progress: "",
          remarks: "",
          recommendations: "",
        });
        setRatingsData({
          clientRating: "",
          therapistRating: "",
          progressTowardGoals: "",
          moodBefore: "",
          moodAfter: "",
        });
        setRiskAssessments(buildEmptyRiskAssessments());
      })
      .catch((error) => {
        setSubmitError(getApiErrorMessage(error));
      });
  };

  const handleUpdateNote = (finalize: boolean) => {
    if (!resolvedNoteId) return;
    const wantsFinalized = finalize;
    const wasFinalized = Boolean(noteById?.isFinalized);
    void updateSessionNote({
      id: resolvedNoteId,
      body: {
        date: sessionData.dateIso ? new Date(sessionData.dateIso).toISOString() : undefined,
        sessionFocus: formData.sessionFocus || undefined,
        symptoms: formData.symptoms || undefined,
        shortTermGoals: formData.shortTermGoals || undefined,
        intervention: formData.intervention || undefined,
        progress: formData.progress || undefined,
        remarks: formData.remarks || undefined,
        recommendations: formData.recommendations || undefined,
        ...(hideRatingsTab
          ? {}
          : {
              clientRating: parseOptionalRating(ratingsData.clientRating),
              therapistRating: parseOptionalRating(ratingsData.therapistRating),
              progressTowardGoals: parseOptionalRating(ratingsData.progressTowardGoals),
              moodBefore: parseOptionalRating(ratingsData.moodBefore),
              moodAfter: parseOptionalRating(ratingsData.moodAfter),
            }),
        ...riskScorePayload,
        // isFinalized is deliberately not sent: signing runs through the
        // finalize endpoint below so finalContent is written and audited.
        isDraft: true,
        draftContent: advancedData.draftContent || undefined,
        aiProcessingStatus: advancedData.aiProcessingStatus || undefined,
        aiEnabled: false,
      },
    })
      .unwrap()
      .then(async () => {
        if (wantsFinalized && !wasFinalized) {
          await finalizeSessionNote(resolvedNoteId).unwrap();
        } else if (!wantsFinalized && wasFinalized) {
          await unfinalizeSessionNote(resolvedNoteId).unwrap();
        }
        setIsEditingView(false);
        await refetchNoteById();
        onChanged?.();
        onToast?.(
          wantsFinalized && !wasFinalized
            ? "Session note finalized successfully."
            : "Session note updated successfully.",
          "success",
        );
      })
      .catch((error) => setSubmitError(getApiErrorMessage(error)));
  };

  const handleDownloadPdf = () => {
    if (!resolvedNoteId) return;
    void getSessionNotePdf(resolvedNoteId)
      .unwrap()
      .then((response) => {
        if (!response) return;
        const trimmed = response.trim();
        const isHtml =
          trimmed.startsWith("<!DOCTYPE html") ||
          trimmed.startsWith("<html") ||
          trimmed.includes("<body");

        if (isHtml) {
          const printWindow = window.open("", "_blank");
          if (!printWindow) {
            setSubmitError("Popup blocked. Please allow popups and try again.");
            return;
          }
          printWindow.document.open();
          printWindow.document.write(response);
          printWindow.document.close();
          printWindow.focus();
          setTimeout(() => {
            printWindow.print();
          }, 250);
          return;
        }

        window.open(response, "_blank", "noopener,noreferrer");
      })
      .catch((error) => setSubmitError(getApiErrorMessage(error)));
  };

  const handleDeleteNote = () => {
    if (!resolvedNoteId) return;
    void deleteSessionNote(resolvedNoteId)
      .unwrap()
      .then(() => {
        setShowDeleteConfirm(false);
        onChanged?.();
        onClose();
      })
      .catch((error) => setSubmitError(getApiErrorMessage(error)));
  };

  const filledClinicalCount = Object.values(formData).filter((value) => value.trim()).length;
  const filledRatingsCount = Object.values(ratingsData).filter((value) => value.trim()).length;

  const noteSteps: SessionNoteStep[] = [
    {
      key: "clinical",
      label: "Clinical",
      // Every clinical field is optional, so anything written counts as done.
      state: filledClinicalCount > 0 ? "complete" : "empty",
    },
    {
      key: "risk",
      label: "Risk",
      required: true,
      state: riskSummary.isComplete
        ? "complete"
        : riskSummary.answeredCount > 0
          ? "partial"
          : "empty",
    },
    {
      key: "final",
      label: "Final note",
      state: hasRichTextContent(advancedData.draftContent) ? "complete" : "empty",
    },
    ...(hideRatingsTab
      ? []
      : [
          {
            key: "ratings",
            label: "Ratings",
            state: (filledRatingsCount > 0
              ? "complete"
              : "empty") as SessionNoteStep["state"],
          },
        ]),
  ];

  const activeStepIndex = Math.max(
    0,
    noteSteps.findIndex((step) => step.key === selectedTemplate),
  );

  const isSavingNote =
    isCreatingNote || isUpdatingNote || isFinalizingNote || isUnfinalizingNote;

  const isFormEmpty =
    Object.values(formData).every((val) => !val.trim()) &&
    (hideRatingsTab || Object.values(ratingsData).every((val) => !val.trim())) &&
    !hasRichTextContent(advancedData.draftContent);
  const hasSessionTranscript = Boolean(transcriptionData?.rawTranscription?.trim());
  const transcriptWordCount = transcriptionData?.rawTranscription
    ? transcriptionData.rawTranscription.split(/\s+/).filter(Boolean).length
    : 0;

  const hasFinalSessionNote = Boolean(
    hasRichTextContent(advancedData.draftContent) || generatedPreviewPlainText.trim(),
  );
  // Each blocker is listed on screen rather than hidden in a tooltip on the
  // disabled button, so it is clear what is still needed and where to do it.
  const generateFinalNoteBlockers: { key: string; label: string }[] = [
    ...(selectedAiTemplateId
      ? []
      : [{ key: "template", label: "Choose a template" }]),
    ...(canGenerateFinalNote
      ? []
      : [{ key: "fields", label: "Fill in a clinical field" }]),
  ];

  const handleFinalNoteContentChange = (content: string) => {
    setAdvancedData((previous) => ({
      ...previous,
      draftContent: content,
    }));
    setGeneratedPreviewPlainText(htmlToPlainPreviewText(content));
  };

  // A signed note is a record to read, not a form to fill, so it gets its own
  // document view instead of the editor with every control disabled.
  const showSignedView = mode === "view" && isNoteFinalized && !isEditingView && Boolean(noteById);

  const handleCreateAmendment = (payload: { amendmentText: string; reason: string }) => {
    if (!resolvedNoteId) return;
    void createSessionNoteAmendment({ id: resolvedNoteId, ...payload })
      .unwrap()
      .then(() => {
        setShowAmendmentModal(false);
        onChanged?.();
        onToast?.("Amendment saved.", "success");
      })
      .catch((error) => onToast?.(getApiErrorMessage(error), "error"));
  };

  const modalTitle =
    mode === "create"
      ? "Add Session Note"
      : isEditingView
        ? "Session Note Edit"
        : showSignedView
          ? "Signed Session Note"
          : "Session Note View";

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-9999 p-4">
      <div className="bg-white rounded-2xl w-full max-w-3xl max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header - Fixed */}
        <div className="flex items-center justify-between gap-4 p-6 border-b border-(--neutral-200) shrink-0">
          <div className="flex min-w-0 flex-1 flex-wrap items-baseline gap-x-4 gap-y-1">
            <h2 className="shrink-0 text-xl font-semibold text-(--text-primary-dark)">
              {modalTitle}
            </h2>
            <p
              className="min-w-0 flex-1 truncate text-sm text-(--text-neutral-600)"
              title={`${sessionData.dateTime} · ${sessionData.sessionType} · ${sessionData.clientName}`}
            >
              {sessionData.dateTime} · {sessionData.sessionType}
              <span className="mx-2 text-(--text-neutral-400)">|</span>
              <span className="font-medium text-(--text-primary-dark)">
                {sessionData.clientName}
              </span>
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="p-1 hover:bg-(--neutral-50) rounded-lg transition-colors"
          >
            <X size={24} className="text-(--text-neutral-600)" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto overscroll-contain pb-8">
          {mode === "view" && (isLoadingNote || isFetchingNote) ? (
            <div className="mx-6 mt-6 rounded-lg border border-(--neutral-200) p-4 text-sm text-(--text-neutral-600)">
              Loading session note...
            </div>
          ) : null}
          {showSignedView && noteById ? (
            <SessionNoteSignedView
              note={noteById}
              practiceTimezone={practiceConfig?.timezone}
              amendments={amendments}
            />
          ) : (
          <>
          {transcriptionData?.rawTranscription ? (
            <div className="mx-6 mt-6 rounded-lg bg-(--neutral-50) p-4">
              <p className="text-sm font-medium text-(--status-completed-dark)">
                Transcript ready
                {transcriptWordCount ? ` · ${transcriptWordCount} words` : ""}
              </p>
              <p className="mt-0.5 text-xs text-(--text-neutral-600)">
                Review AI suggestions before applying anything to the note.
              </p>
            </div>
          ) : null}

          {smartFillAlert ? (
            <div className="mx-6 mt-4 flex items-start gap-3 rounded-[1.125rem] border border-blue-200 bg-blue-50 px-4 py-3">
              <AlertCircle size={18} className="mt-0.5 shrink-0 text-blue-600" />
              <p className="text-sm leading-5 text-blue-800 break-words [overflow-wrap:anywhere]">
                {smartFillAlert}
              </p>
            </div>
          ) : null}

          {/* Template Selection */}
          <div className="px-6 mt-6">
            <SessionNoteSteps
              steps={noteSteps}
              activeKey={selectedTemplate}
              onSelect={(key) => setSelectedTemplate(key as TabType)}
            />

            {/* Conditional Content Based on Tab */}
            {selectedTemplate === "clinical" ? (
              // Clinical Documentation Tab
              <div className="space-y-6 pb-6">
                <div className="flex min-w-0 items-center justify-between gap-3">
                  <p className="text-sm text-(--text-neutral-600)">
                    Record what happened in the session. Every field is optional.
                  </p>
                  <SessionNoteClinicalActionsMenu
                    disabled={isReadOnly}
                    isSmartFilling={isSmartFilling || isFetchingTranscript}
                    hasTranscript={hasSessionTranscript}
                    onSmartFill={handleSmartFillClick}
                  />
                </div>

                <div>
                  {renderLibraryTrigger("sessionFocus")}
                  <CustomTextarea
                    label="Session Focus"
                    value={formData.sessionFocus}
                    disabled={isReadOnly}
                    onChange={(e) =>
                      handleInputChange("sessionFocus", e.target.value)
                    }
                    hint="Main topics or issues addressed during the session"
                    rows={4}
                    className="min-h-40"
                    textareaClassName="min-h-28 resize-y"
                  />
                </div>

                <div>
                  {renderLibraryTrigger("symptoms")}
                  <CustomTextarea
                    label="Symptoms"
                    value={formData.symptoms}
                    disabled={isReadOnly}
                    onChange={(e) =>
                      handleInputChange("symptoms", e.target.value)
                    }
                    hint="Observed or reported symptoms"
                    rows={4}
                    className="min-h-40"
                    textareaClassName="min-h-28 resize-y"
                  />
                </div>

                <div>
                  {renderLibraryTrigger("progress")}
                  <CustomTextarea
                    label="Progress"
                    value={formData.progress}
                    disabled={isReadOnly}
                    onChange={(e) => handleInputChange("progress", e.target.value)}
                    hint="Progress made during this session"
                    rows={4}
                    className="min-h-40"
                    textareaClassName="min-h-28 resize-y"
                  />
                </div>

                <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
                  <div>
                    {renderLibraryTrigger("shortTermGoals")}
                    <CustomTextarea
                      label="Short-term Goals"
                      value={formData.shortTermGoals}
                      disabled={isReadOnly}
                      onChange={(e) =>
                        handleInputChange("shortTermGoals", e.target.value)
                      }
                      hint="Goals worked on during this session"
                      rows={4}
                      className="min-h-40"
                      textareaClassName="min-h-28 resize-y"
                    />
                  </div>
                  <div>
                    {renderLibraryTrigger("intervention")}
                    <CustomTextarea
                      label="Intervention"
                      value={formData.intervention}
                      disabled={isReadOnly}
                      onChange={(e) =>
                        handleInputChange("intervention", e.target.value)
                      }
                      hint="Therapeutic techniques/interventions used"
                      rows={4}
                      className="min-h-40"
                      textareaClassName="min-h-28 resize-y"
                    />
                  </div>
                </div>

                {/* Remarks */}
                <div>
                  <CustomTextarea
                    label="Remarks"
                    value={formData.remarks}
                    disabled={isReadOnly}
                    onChange={(e) =>
                      handleInputChange("remarks", e.target.value)
                    }
                    hint="Additional clinical observations"
                    rows={4}
                    className="min-h-40"
                    textareaClassName="min-h-28 resize-y"
                  />
                </div>

                {/* Recommendations */}
                <div>
                  <CustomTextarea
                    label="Recommendations"
                    value={formData.recommendations}
                    disabled={isReadOnly}
                    onChange={(e) =>
                      handleInputChange("recommendations", e.target.value)
                    }
                    hint="Future treatment recommendations"
                    rows={4}
                    className="min-h-40"
                    textareaClassName="min-h-28 resize-y"
                  />
                </div>

              </div>
            ) : selectedTemplate === "final" ? (
              <div className="space-y-6 pb-6">
                <div className="space-y-4">
                  {/* Step one of this screen: which format the note is written in. */}
                  <div className="space-y-2">
                    <label className="block text-sm font-medium text-(--text-primary-dark)">
                      AI template
                    </label>
                    {aiTemplates.length === 0 ? (
                      <p className="text-sm text-(--text-neutral-600)">
                        Pick a format to start with, or create your own.
                      </p>
                    ) : (
                      <div className="flex min-w-0 flex-wrap items-center gap-2">
                        <div className="min-w-0 flex-1 basis-56">
                          <SessionNoteAiTemplateSelect
                            templates={aiTemplates}
                            value={selectedAiTemplateId}
                            onChange={setSelectedAiTemplateId}
                            disabled={isReadOnly}
                          />
                        </div>
                        {!isReadOnly ? (
                          <Button
                            type="button"
                            variant="outline"
                            onClick={() =>
                              openAiTemplateModal({ fresh: selectedAiTemplateId === null })
                            }
                            className="h-10 shrink-0 gap-2 rounded-full border-(--neutral-200) px-4 text-sm font-normal"
                          >
                            <Eye size={16} />
                            View template
                          </Button>
                        ) : null}
                      </div>
                    )}
                  </div>

                  {aiTemplates.length === 0 && !isReadOnly ? (
                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
                      {SESSION_NOTE_STARTER_TEMPLATES.map((starter) => (
                        <button
                          key={starter.key}
                          type="button"
                          disabled={isCreatingAiTemplate}
                          onClick={() => handleUseStarterTemplate(starter)}
                          className="flex cursor-pointer flex-col gap-1 rounded-xl border border-(--neutral-200) bg-white px-4 py-3 text-left transition-colors hover:border-(--bg-primary-dark) disabled:opacity-50"
                        >
                          <span className="text-sm font-semibold text-(--text-primary-dark)">
                            {starter.name}
                          </span>
                          <span className="text-xs text-(--text-neutral-600)">
                            {starter.summary}
                          </span>
                        </button>
                      ))}
                    </div>
                  ) : null}

                </div>

                <div>
                  <div className="mb-2 flex flex-wrap items-center justify-between gap-x-3 gap-y-2">
                    <label className="text-sm font-medium text-(--text-primary-dark)">
                      Final Session Note
                    </label>
                    {!isReadOnly ? (
                      <div className="flex min-w-0 flex-wrap items-center justify-end gap-x-3 gap-y-1">
                        {/* What is missing reads just before the button it blocks. */}
                        {generateFinalNoteBlockers.length > 0 ? (
                          <p className="min-w-0 text-[0.8125rem] text-(--status-pending-dark)">
                            {generateFinalNoteBlockers.map((b) => b.label).join(" · ")}
                            {generateFinalNoteBlockers.some((b) => b.key === "fields") ? (
                              <>
                                {" — "}
                                <button
                                  type="button"
                                  onClick={() => setSelectedTemplate("clinical")}
                                  className="font-medium underline underline-offset-2 hover:no-underline"
                                >
                                  go to Clinical
                                </button>
                              </>
                            ) : null}
                          </p>
                        ) : null}
                        <Button
                          type="button"
                          variant="outline"
                          onClick={() => setShowNotePreview(true)}
                          disabled={!hasRichTextContent(advancedData.draftContent)}
                          className="h-9 shrink-0 gap-2 rounded-full border-(--neutral-200) bg-white px-4 text-[0.8125rem] font-medium text-(--text-neutral-800) hover:bg-(--neutral-50) disabled:opacity-50"
                        >
                          <Eye size={15} />
                          Preview
                        </Button>
                        <Button
                          type="button"
                          onClick={handleGenerateFinalNote}
                          disabled={
                            isGeneratingFinalNote || generateFinalNoteBlockers.length > 0
                          }
                          loading={isGeneratingFinalNote}
                          loadingLabel={
                            hasFinalSessionNote ? "Regenerating..." : "Generating..."
                          }
                          className="h-9 shrink-0 gap-2 rounded-full bg-(--bg-primary-dark) px-4 text-[0.8125rem] font-semibold text-white hover:bg-(--bg-primary-dark)/90 disabled:opacity-50"
                        >
                          <Sparkles size={15} />
                          {hasFinalSessionNote ? "Regenerate" : "Generate"}
                        </Button>
                      </div>
                    ) : null}
                  </div>
                  {hasFinalSessionNote || !isReadOnly ? (
                    <div className="overflow-hidden rounded-xl border border-(--neutral-200) bg-white">
                      <CustomJoditEditor
                        key={`final-note-editor-${finalNoteEditorKey}`}
                        content={advancedData.draftContent}
                        setContent={handleFinalNoteContentChange}
                        readonly={isReadOnly}
                        placeholder="Generate a final note from your clinical fields and selected template. You can edit the result here before saving."
                        contentVariant="report"
                        minHeight={360}
                      />
                    </div>
                  ) : (
                    <p className="rounded-xl border border-dashed border-(--neutral-200) bg-(--neutral-50) px-4 py-8 text-center text-sm text-(--text-neutral-600)">
                      No final note yet.
                    </p>
                  )}
                </div>
              </div>
            ) : selectedTemplate === "risk" ? (
              // Risk Assessment Tab
              <div className="pb-6">
                {/* Overall Risk Assessment Header */}
                {/* Same summary-card colours as the session card in the recording modal. */}
                <div className="mb-6 rounded-xl border border-(--light-blue-3) bg-(--light-blue-2) px-4 py-3">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-3">
                      <span className="text-[0.875rem] font-semibold text-(--text-primary-dark)">
                        Overall Risk Assessment
                      </span>
                      <span
                        className={cn(
                          "px-2.5 py-0.5 text-[0.75rem] font-medium rounded-full",
                          riskSummary.answeredCount === 0
                            ? RISK_UNASSESSED_BADGE_CLASSES
                            : RISK_BAND_BADGE_CLASSES[riskSummary.band],
                        )}
                      >
                        {riskSummary.answeredCount === 0
                          ? "Not assessed"
                          : RISK_BAND_LABELS[riskSummary.band]}
                      </span>
                    </div>
                    <span className="text-[0.8125rem] text-(--text-neutral-600)">
                      {riskSummary.answeredCount === 0
                        ? `0 of ${riskSummary.totalCount} answered`
                        : `Score: ${riskSummary.score}/${riskSummary.maxScore}`}
                    </span>
                  </div>

                  {!riskSummary.isComplete ? (
                    <div className="mt-3 flex flex-wrap items-center justify-between gap-3 border-t border-(--light-blue-3) pt-3">
                      <p className="text-[0.8125rem] text-(--status-pending-dark)">
                        {riskSummary.answeredCount === 0
                          ? "No items answered yet. Unanswered items are left blank in the record."
                          : `${riskSummary.answeredCount} of ${riskSummary.totalCount} answered — the rest stay blank in the record.`}
                      </p>
                      {/* Only offered on an untouched assessment — it answers every
                          item at once and must never overwrite real answers. */}
                      {!isReadOnly && riskSummary.answeredCount === 0 ? (
                        <Button
                          type="button"
                          onClick={handleNoRiskFactors}
                          className="h-9 shrink-0 gap-2 rounded-full bg-(--bg-primary-dark) px-4 text-[0.8125rem] font-semibold text-white hover:bg-(--bg-primary-dark)/90"
                        >
                          <Check size={15} strokeWidth={2.5} />
                          No risk factors identified
                        </Button>
                      ) : null}
                    </div>
                  ) : !isReadOnly ? (
                    <div className="mt-3 flex flex-wrap items-center justify-between gap-3 border-t border-(--light-blue-3) pt-3">
                      <p className="text-[0.8125rem] text-(--status-completed-dark)">
                        All {riskSummary.totalCount} items answered.
                      </p>
                      <button
                        type="button"
                        onClick={handleClearRiskAssessment}
                        className="shrink-0 text-xs font-medium text-(--text-primary-500) hover:underline"
                      >
                        Clear answers
                      </button>
                    </div>
                  ) : null}
                </div>

                {/* Risk Assessment Items - Mapped from data */}
                {RISK_ASSESSMENT_ITEMS.map((item, index) => (
                  <RiskAssessmentItem
                    key={item.id}
                    number={index + 1}
                    title={item.title}
                    description={item.description}
                    options={item.options}
                    selectedOption={riskAssessments[item.id]}
                    onSelect={(option) =>
                      !isReadOnly &&
                      setRiskAssessments((prev) => ({
                        ...prev,
                        [item.id]: option,
                      }))
                    }
                  />
                ))}
              </div>
            ) : !hideRatingsTab ? (
              <div className="pb-6 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <CustomInput
                    label="Client Rating (1-10)"
                    type="number"
                    min={1}
                    max={10}
                    value={ratingsData.clientRating}
                    disabled={isReadOnly}
                    onChange={(e) => handleRatingChange("clientRating", e.target.value)}
                  />
                  <CustomInput
                    label="Therapist Rating (1-10)"
                    type="number"
                    min={1}
                    max={10}
                    value={ratingsData.therapistRating}
                    disabled={isReadOnly}
                    onChange={(e) => handleRatingChange("therapistRating", e.target.value)}
                  />
                  <CustomInput
                    label="Progress Toward Goals (1-10)"
                    type="number"
                    min={1}
                    max={10}
                    value={ratingsData.progressTowardGoals}
                    disabled={isReadOnly}
                    onChange={(e) => handleRatingChange("progressTowardGoals", e.target.value)}
                  />
                  <CustomInput
                    label="Mood Before (1-10)"
                    type="number"
                    min={1}
                    max={10}
                    value={ratingsData.moodBefore}
                    disabled={isReadOnly}
                    onChange={(e) => handleRatingChange("moodBefore", e.target.value)}
                  />
                  <CustomInput
                    label="Mood After (1-10)"
                    type="number"
                    min={1}
                    max={10}
                    value={ratingsData.moodAfter}
                    disabled={isReadOnly}
                    onChange={(e) => handleRatingChange("moodAfter", e.target.value)}
                  />
                </div>
              </div>
            ) : null}
          </div>
          </>
          )}
        </div>

        {/* Footer - Fixed */}
        <div className="flex flex-wrap items-center justify-end gap-3 p-6 border-t border-(--neutral-200) shrink-0">
          {submitError ? (
            <p className="w-full text-sm text-(--status-denied)">{submitError}</p>
          ) : null}
          {!showSignedView ? (
            <div className="mr-auto flex items-center gap-2">
              <Button
                variant="outline"
                onClick={() => setSelectedTemplate(noteSteps[activeStepIndex - 1].key as TabType)}
                disabled={activeStepIndex === 0}
                className="h-11 px-5 border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-800) rounded-full text-sm font-normal disabled:opacity-40"
              >
                Back
              </Button>
              <Button
                variant="outline"
                onClick={() => setSelectedTemplate(noteSteps[activeStepIndex + 1].key as TabType)}
                disabled={activeStepIndex >= noteSteps.length - 1}
                className="h-11 px-5 border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-800) rounded-full text-sm font-normal disabled:opacity-40"
              >
                Next
              </Button>
            </div>
          ) : null}
          {showSignedView ? (
            // A signed note cannot be edited or deleted, so the only actions
            // offered are the ones that actually work on it.
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                onClick={handleDownloadPdf}
                disabled={isDownloadingPdf || !resolvedNoteId}
                loading={isDownloadingPdf}
                loadingLabel="Downloading..."
                className="h-11 px-6 border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-800) rounded-full text-sm font-normal"
              >
                Download PDF
              </Button>
              <Button
                onClick={() => setShowAmendmentModal(true)}
                disabled={!resolvedNoteId}
                className="h-11 px-6 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full text-sm font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Amend
              </Button>
            </div>
          ) : mode === "create" ? (
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                onClick={() => handleCreateNote(false)}
                disabled={isSavingNote || isFormEmpty}
                loading={isSavingNote && !pendingFinalize}
                loadingLabel="Saving..."
                className="h-11 px-6 border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-800) rounded-full text-sm font-normal disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Save draft
              </Button>
              <Button
                onClick={requestFinalize}
                disabled={isSavingNote || isFormEmpty}
                loading={isSavingNote && pendingFinalize}
                loadingLabel="Finalizing..."
                className="h-11 px-6 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full text-sm font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Save &amp; finalize
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Button
                onClick={handleDownloadPdf}
                disabled={isDownloadingPdf || !resolvedNoteId}
                loading={isDownloadingPdf}
                loadingLabel="Downloading..."
                className="h-11 px-6 border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-800) rounded-full text-sm font-normal"
              >
                Download PDF
              </Button>
              {isEditingView && canEditNote ? (
                <>
                  <Button
                    variant="outline"
                    onClick={() => handleUpdateNote(false)}
                    disabled={isSavingNote || isFormEmpty}
                    loading={isSavingNote && !pendingFinalize}
                    loadingLabel="Saving..."
                    className="h-11 px-6 border-(--neutral-200) bg-white hover:bg-(--neutral-50) text-(--text-neutral-800) rounded-full text-sm font-normal disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Save draft
                  </Button>
                  <Button
                    onClick={requestFinalize}
                    disabled={isSavingNote || isFormEmpty}
                    loading={isSavingNote && pendingFinalize}
                    loadingLabel="Finalizing..."
                    className="h-11 px-6 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full text-sm font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Save &amp; finalize
                  </Button>
                </>
              ) : (
                <Button
                  onClick={() => {
                    if (!canEditNote) return;
                    setIsEditingView(true);
                  }}
                  disabled={!canEditNote}
                  title={
                    isNoteFinalized
                      ? "Reopen the session note before editing."
                      : undefined
                  }
                  className="h-11 px-6 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full text-sm font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Edit Note
                </Button>
              )}
              <Button
                onClick={() => setShowDeleteConfirm(true)}
                disabled={isNoteFinalized}
                title={
                  isNoteFinalized
                    ? "Finalized session notes cannot be deleted. Reopen first or create an amendment."
                    : undefined
                }
                className="h-11 px-6 bg-red-600 hover:bg-red-700 text-white rounded-full text-sm font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Delete
              </Button>
            </div>
          )}
        </div>
      </div>

      <ReviewTranscriptionModal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        onApply={handleApplyTranscription}
        transcriptionData={transcriptionData}
      />

      {!hideLibraryButtons ? (
      <SelectLibraryModal
        isOpen={isLibraryModalOpen}
        onClose={() => setLibraryModalOpen(false)}
        onSelect={handleLibrarySelect}
        fieldName={activeField ? SESSION_NOTE_FIELD_LABELS[activeField] : ""}
        categoryId={activeField ? categoryIdByField[activeField] : 0}
        entries={libraryEntries}
        isLoading={isLoadingLibrary}
      />
      ) : null}

      <ConfirmationModal
        type="confirm"
        isOpen={showFinalizeConfirm}
        onClose={() => setShowFinalizeConfirm(false)}
        onConfirm={confirmFinalize}
        title="Finalize this session note?"
        description="Finalizing signs and locks the note. It can no longer be edited or deleted, and any change after this needs the note reopened or an amendment."
        items={[]}
        confirmButtonText="Finalize note"
        confirmButtonLoading={isSavingNote}
        overlayClassName="z-[10001]"
      />

      <ConfirmationModal
        type="delete"
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDeleteNote}
        title="Delete session note?"
        description="Are you sure you want to delete this session note? This action cannot be undone."
        items={[]}
        confirmButtonText={isDeletingNote ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeletingNote}
      />

      <ConfirmationModal
        type="delete"
        isOpen={showDeleteTemplateConfirm}
        onClose={() => setShowDeleteTemplateConfirm(false)}
        onConfirm={handleDeleteAiTemplate}
        title={
          selectedAiTemplate
            ? `Delete template "${truncateTemplateNameForConfirm(selectedAiTemplate.name)}"?`
            : "Delete template?"
        }
        description="Are you sure you want to delete this AI template? This action cannot be undone."
        items={[]}
        confirmButtonText="Delete template"
        confirmButtonLoading={isDeletingAiTemplate}
        overlayClassName="z-[10001]"
      />

      <SessionNotePreviewModal
        isOpen={showNotePreview}
        onClose={() => setShowNotePreview(false)}
        subtitle={`${sessionData.dateTime} · ${sessionData.sessionType} | ${sessionData.clientName}`}
        practiceTimezone={practiceConfig?.timezone}
        note={{
          ...formData,
          ...riskScorePayload,
          draftContent: advancedData.draftContent,
          therapistName: noteById?.therapistName ?? authUser?.fullName ?? null,
        }}
      />
      <SessionNoteAmendmentModal
        isOpen={showAmendmentModal}
        onClose={() => setShowAmendmentModal(false)}
        onSubmit={handleCreateAmendment}
        isSaving={isSavingAmendment}
      />

      <SessionNoteAiTemplateModal
        isOpen={isAiTemplateModalOpen}
        onClose={() => {
          setIsAiTemplateModalOpen(false);
          setAiTemplateModalFresh(false);
          // The picker behind the modal has to reflect whatever just happened in it.
          void refetchAiTemplates();
        }}
        onRequestDelete={(templateId) => {
          setSelectedAiTemplateId(templateId);
          setIsAiTemplateModalOpen(false);
          setShowDeleteTemplateConfirm(true);
        }}
        templates={aiTemplates}
        selectedTemplateId={selectedAiTemplateId}
        isSaving={isCreatingAiTemplate || isUpdatingAiTemplate}
        startFresh={aiTemplateModalFresh}
        onSave={handleSaveAiTemplate}
      />
    </div>
  );
};

const AddSessionNoteModal = (props: AddSessionNoteModalProps) => props.isOpen ? <AddSessionNoteModalContent key={`${props.sessionData.sessionId}:${props.noteId}:${props.mode}`} {...props} /> : null;

export default AddSessionNoteModal;
