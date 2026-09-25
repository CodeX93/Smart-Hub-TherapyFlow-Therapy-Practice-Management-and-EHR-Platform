import { ContentLoader } from "@/components/shared/ContentLoader";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  type AssessmentSection,
  type AssessmentQuestion,
  type AssessmentAnswer,
} from "../../../pages/therapist/therapist.static";

import AssessmentHeader from "./AssessmentHeader";
import AssessmentSidebar from "./AssessmentSidebar";
import AssessmentSectionRenderer from "./AssessmentSectionRenderer";
import AssessmentReportStep from "./AssessmentReportStep";
import SessionNoteSteps, { type SessionNoteStepState } from "@/components/sessions/SessionNoteSteps";
import { Button } from "@/components/ui/button";
import {
  useGetAssessmentAssignmentResponsesQuery,
  useGetAssessmentTemplateSectionsQuery,
  useSubmitAssessmentResponsesMutation,
  type AdminAssessmentSection,
} from "@/store/api/admin/clients.api";
import Toast from "@/components/shared/Toast";
import { getApiErrorMessage } from "@/utils/apiError";
import { countAnsweredQuestions, hasAssessmentAnswer } from "@/utils/assessmentAnswers";
import {
  getAssessmentCardState,
  getAssessmentStartStep,
  hasAssessmentReport,
  type AssessmentStep,
} from "@/utils/assessmentCardState";

interface TakeAssessmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  assessmentId: string;
  templateId?: number | null;
  clientIdValue?: number;
  clientNameValue?: string;
  assessmentNameValue?: string;
  assignedDateValue?: string;
  assessmentStatusValue?: string;
  /** A read-only viewer can read the answers and the report but change neither. */
  readOnly?: boolean;
  onAssessmentSaved?: () => Promise<void> | void;
}

function mapQuestionType(questionType?: string): AssessmentQuestion["type"] {
  // Normalize API keys (`multiple_choice`, `long_text`) and UI labels the same way.
  const normalized = questionType
    ?.trim()
    .toLowerCase()
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  if (!normalized) return "text";

  if (
    normalized.includes("multiple choice") ||
    normalized.includes("multi choice") ||
    normalized.includes("single choice") ||
    normalized === "radio" ||
    normalized === "yes no" ||
    normalized === "yes/no"
  ) {
    return "radio";
  }
  if (normalized.includes("checkbox") || normalized.includes("multi select")) {
    return "checkbox";
  }
  if (normalized.includes("rating") || normalized.includes("scale")) {
    return "radio";
  }
  if (normalized.includes("date")) return "date";
  if (normalized.includes("voice")) return "voice";
  if (
    normalized.includes("long text") ||
    normalized.includes("long answer") ||
    normalized.includes("text area") ||
    normalized.includes("textarea") ||
    normalized.includes("paragraph")
  ) {
    return "textarea";
  }
  if (
    normalized.includes("short text") ||
    normalized.includes("short answer") ||
    normalized === "text" ||
    normalized.includes("number")
  ) {
    return "text";
  }
  return "text";
}

function bySortOrderAsc<T extends { sortOrder?: number | null }>(a: T, b: T): number {
  return (a.sortOrder ?? 0) - (b.sortOrder ?? 0);
}

function mapApiSectionsToUiSections(sections: AdminAssessmentSection[]): AssessmentSection[] {
  return [...sections]
    .sort(bySortOrderAsc)
    .map((section) => ({
      id: String(section.id),
      title: section.title || "Untitled Section",
      questions: [...(section.questions ?? [])]
        .sort(bySortOrderAsc)
        .map((question) => ({
          id: String(question.id),
          type: mapQuestionType(question.questionType),
          label: question.questionText || "Untitled Question",
          required: Boolean(question.isRequired),
          options: [...(question.options ?? [])]
            .sort(bySortOrderAsc)
            .map((option, optionIndex) => {
              const label = option.optionText?.trim() || `Option ${optionIndex + 1}`;
              return `${label}__opt_${option.id || optionIndex}`;
            }),
        })),
    }));
}


/**
 * One modal, two steps — answer the questions, then write and finalize the
 * report — the same shape as the session note. Answers save as they are
 * typed, so there is no separate submit; the report is where the assessment
 * is signed off.
 */
const TakeAssessmentModal = ({
  isOpen,
  onClose,
  assessmentId,
  templateId,
  clientIdValue,
  clientNameValue,
  assessmentNameValue,
  assignedDateValue,
  assessmentStatusValue,
  readOnly = false,
  onAssessmentSaved,
}: TakeAssessmentModalProps) => {
  const [status, setStatus] = useState(assessmentStatusValue);
  const cardState = getAssessmentCardState(status);
  const isFinalized = cardState === "finalized";
  const isLocked = readOnly || isFinalized;

  const [step, setStep] = useState<AssessmentStep>(() => getAssessmentStartStep(cardState));
  const [activeSectionIndex, setActiveSectionIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, AssessmentAnswer>>({});
  const [isSavingAssessment, setIsSavingAssessment] = useState(false);
  const [hasSavedOnce, setHasSavedOnce] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const answersRef = useRef<Record<string, AssessmentAnswer>>({});
  const sectionsRef = useRef<AssessmentSection[]>([]);
  const isDirtyRef = useRef(false);
  const reportDirtyRef = useRef(false);
  const debounceSaveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isSavingRef = useRef(false);
  const contentScrollRef = useRef<HTMLDivElement>(null);

  const { data: templateSections = [], isLoading: isSectionsLoading } =
    useGetAssessmentTemplateSectionsQuery(templateId ?? 0, {
      skip: !isOpen || !templateId,
    });
  const assignmentIdNumeric = Number(assessmentId);
  const hasValidAssignment = Number.isFinite(assignmentIdNumeric) && assignmentIdNumeric > 0;
  const {
    data: assignmentResponses = [],
    isLoading: isAssignmentResponsesLoading,
    isError: isAssignmentResponsesError,
    error: assignmentResponsesError,
  } = useGetAssessmentAssignmentResponsesQuery(assignmentIdNumeric, {
    skip: !isOpen || !hasValidAssignment,
    refetchOnMountOrArgChange: true,
  });
  const [submitAssessmentResponses] = useSubmitAssessmentResponsesMutation();

  const sections = useMemo(
    () => mapApiSectionsToUiSections(templateSections),
    [templateSections],
  );

  useEffect(() => {
    sectionsRef.current = sections;
  }, [sections]);

  useEffect(() => {
    contentScrollRef.current?.scrollTo({ top: 0, behavior: "auto" });
  }, [activeSectionIndex]);

  useEffect(() => {
    if (!hasValidAssignment) return;
    if (!sections.length || !assignmentResponses.length) return;

    const prefills: Record<string, AssessmentAnswer> = {};

    for (const response of assignmentResponses) {
      const questionId = String(response.questionId);
      const question = sections
        .flatMap((section) => section.questions)
        .find((q) => q.id === questionId);
      if (!question) continue;

      if (Array.isArray(response.selectedOptionIds) && response.selectedOptionIds.length > 0) {
        const selected = question.options?.filter((option) => {
          const match = option.match(/__opt_(\d+)$/);
          const optionId = match?.[1] ? Number(match[1]) : null;
          return optionId !== null && response.selectedOptionIds?.includes(optionId);
        });
        if (selected?.length) {
          prefills[questionId] =
            question.type === "radio" ? selected[0] : selected;
          continue;
        }
      }

      if (response.ratingValue !== undefined && response.ratingValue !== null) {
        prefills[questionId] = String(response.ratingValue);
        continue;
      }

      if (response.responseText) {
        if (question.type === "date") {
          const dateValue = new Date(response.responseText);
          prefills[questionId] = Number.isNaN(dateValue.getTime())
            ? response.responseText
            : dateValue;
          continue;
        }
        prefills[questionId] = response.responseText;
      }
    }

    if (Object.keys(prefills).length > 0) {
      setAnswers((prev) => {
        const next = { ...prev, ...prefills };
        answersRef.current = next;
        return next;
      });
      isDirtyRef.current = false;
    }
  }, [assignmentResponses, sections, hasValidAssignment]);

  useEffect(() => {
    if (!isAssignmentResponsesError) return;
    setToastType("error");
    setToastMessage(getApiErrorMessage(assignmentResponsesError));
  }, [isAssignmentResponsesError, assignmentResponsesError]);

  const parsedAssignedDate = assignedDateValue ? new Date(assignedDateValue) : null;
  const formattedAssignedDate =
    parsedAssignedDate && !Number.isNaN(parsedAssignedDate.getTime())
      ? parsedAssignedDate.toLocaleDateString("en-US", {
          month: "short",
          day: "2-digit",
          year: "numeric",
        })
      : null;

  const currentSection = sections[activeSectionIndex];
  const isLastSection = activeSectionIndex >= sections.length - 1;
  const totalQuestions = sections.reduce((acc, sec) => acc + sec.questions.length, 0);
  const answeredCount = countAnsweredQuestions(answers);

  const completedSections = sections
    .map((sec, idx) => {
      const secAnswered = sec.questions.filter((q) => hasAssessmentAnswer(answers[q.id])).length;
      return sec.questions.length > 0 && secAnswered === sec.questions.length ? idx : -1;
    })
    .filter((idx) => idx !== -1);

  const buildResponseItems = (
    sourceAnswers: Record<string, AssessmentAnswer> = answersRef.current,
  ) =>
    sectionsRef.current.flatMap((section) =>
      section.questions
        .filter((question) => sourceAnswers[question.id] !== undefined && sourceAnswers[question.id] !== "")
        .map((question) => {
          const answer = sourceAnswers[question.id];
          const base = {
            questionId: Number(question.id),
          };

          if (typeof answer === "string") {
            if (question.type === "radio") {
              const optionIdMatch = answer.match(/__opt_(\d+)$/);
              const selectedOptionId = optionIdMatch?.[1]
                ? Number(optionIdMatch[1])
                : undefined;
              return {
                ...base,
                responseText: answer.split("__opt_")[0],
                selectedOptionId,
              };
            }
            return {
              ...base,
              responseText: answer,
            };
          }

          if (answer instanceof Date) {
            return {
              ...base,
              responseText: answer.toISOString(),
            };
          }

          if (Array.isArray(answer)) {
            const selectedOptionIds = answer
              .map((item) => {
                const match = String(item).match(/__opt_(\d+)$/);
                return match?.[1] ? Number(match[1]) : null;
              })
              .filter((id): id is number => typeof id === "number" && Number.isFinite(id));
            return {
              ...base,
              selectedOptionIds,
              responseText: answer.map((item) => String(item).split("__opt_")[0]).join(", "),
            };
          }

          return {
            ...base,
            responseText: String(answer),
          };
        }),
    );

  /** Saves the answers on screen. Silent: errors surface as a toast only. */
  const saveResponses = async () => {
    if (isLocked || !hasValidAssignment) return;
    if (debounceSaveTimerRef.current) {
      clearTimeout(debounceSaveTimerRef.current);
      debounceSaveTimerRef.current = null;
    }
    if (!isDirtyRef.current) return;

    const responseItems = buildResponseItems();
    if (responseItems.length === 0) return;
    if (isSavingRef.current) return;
    isSavingRef.current = true;

    try {
      setIsSavingAssessment(true);
      await submitAssessmentResponses({
        id: assignmentIdNumeric,
        body: {
          assignmentId: assignmentIdNumeric,
          responses: responseItems,
        },
      }).unwrap();
      isDirtyRef.current = false;
      setHasSavedOnce(true);
      await onAssessmentSaved?.();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      isSavingRef.current = false;
      setIsSavingAssessment(false);
    }
  };

  const handleAnswerChange = (questionId: string, value: AssessmentAnswer) => {
    if (isLocked) return;
    setAnswers((prev) => {
      const next = { ...prev, [questionId]: value };
      answersRef.current = next;
      return next;
    });
    isDirtyRef.current = true;

    if (debounceSaveTimerRef.current) {
      clearTimeout(debounceSaveTimerRef.current);
    }
    debounceSaveTimerRef.current = setTimeout(() => {
      void saveResponses();
    }, 1000);
  };

  const goToSection = async (index: number) => {
    if (index === activeSectionIndex || index < 0 || index >= sections.length) return;
    await saveResponses();
    setActiveSectionIndex(index);
  };

  const goToStep = async (next: string) => {
    if (next === step) return;
    await saveResponses();
    setStep(next as AssessmentStep);
  };

  const handleRequestClose = async () => {
    if (
      reportDirtyRef.current &&
      !window.confirm("You have unsaved changes to the report. Discard them?")
    ) {
      return;
    }
    await saveResponses();
    onClose();
  };

  const handleReportDirtyChange = useCallback((dirty: boolean) => {
    reportDirtyRef.current = dirty;
  }, []);

  useEffect(() => {
    if (!isOpen || isLocked) return;

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!isDirtyRef.current) return;
      void saveResponses();
      event.preventDefault();
      event.returnValue = "";
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
    // saveResponses reads the latest answers through refs.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, isLocked, assessmentId]);

  useEffect(() => {
    return () => {
      if (debounceSaveTimerRef.current) {
        clearTimeout(debounceSaveTimerRef.current);
      }
    };
  }, []);

  if (!isOpen) return null;

  // Like the note's steps, done means everything required is in, not that
  // every optional question was answered.
  const requiredAnswered = sections.every((section) =>
    section.questions.every((q) => !q.required || hasAssessmentAnswer(answers[q.id])),
  );
  const questionsState: SessionNoteStepState =
    answeredCount > 0 && requiredAnswered
      ? "complete"
      : answeredCount > 0
        ? "partial"
        : "empty";
  const reportState: SessionNoteStepState = isFinalized
    ? "complete"
    : hasAssessmentReport(cardState)
      ? "partial"
      : "empty";

  const saveStatus = isLocked
    ? `${answeredCount} of ${totalQuestions} answered`
    : isSavingAssessment
      ? "Saving..."
      : hasSavedOnce
        ? `All changes saved · ${answeredCount} of ${totalQuestions} answered`
        : `${answeredCount} of ${totalQuestions} answered · saves as you go`;

  const subtitle = [
    clientNameValue?.trim(),
    clientIdValue ? `#${clientIdValue}` : null,
    formattedAssignedDate ? `Assigned ${formattedAssignedDate}` : null,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-black/40 p-4 md:p-8 animate-in fade-in duration-200">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="mx-auto flex h-full w-full min-w-0 max-w-7xl flex-col overflow-hidden rounded-2xl border border-(--neutral-100) bg-white shadow-2xl">
        <AssessmentHeader
          title={assessmentNameValue?.trim() || "Assessment"}
          subtitle={subtitle}
          onClose={() => void handleRequestClose()}
        >
          <SessionNoteSteps
            steps={[
              { key: "questions", label: "Answer questions", state: questionsState },
              { key: "report", label: "Report", state: reportState },
            ]}
            activeKey={step}
            onSelect={(key) => void goToStep(key)}
          />
        </AssessmentHeader>

        {isSectionsLoading || isAssignmentResponsesLoading ? (
          <div className="flex flex-1 items-center justify-center gap-2 text-sm text-(--text-neutral-600)">
            <ContentLoader variant="inline" size="sm" />
            {isSectionsLoading ? "Loading assessment..." : "Loading responses..."}
          </div>
        ) : sections.length === 0 ? (
          <div className="flex flex-1 items-center justify-center text-sm text-(--text-neutral-600)">
            No data found
          </div>
        ) : step === "report" ? (
          <AssessmentReportStep
            assignmentId={assignmentIdNumeric}
            hasReport={hasAssessmentReport(cardState)}
            readOnly={readOnly}
            answeredCount={answeredCount}
            totalQuestions={totalQuestions}
            saveAnswers={saveResponses}
            onGenerated={() => {
              setStatus("waiting_for_therapist");
              void onAssessmentSaved?.();
            }}
            onFinalized={() => {
              setStatus("completed");
              void onAssessmentSaved?.();
            }}
            onBackToQuestions={() => void goToStep("questions")}
            onDirtyChange={handleReportDirtyChange}
            onNotify={(message, type) => {
              setToastType(type);
              setToastMessage(message);
            }}
          />
        ) : (
          <div className="flex min-h-0 flex-1 overflow-hidden">
            <AssessmentSidebar
              sections={sections}
              activeSectionIndex={activeSectionIndex}
              onSectionChange={(index) => void goToSection(index)}
              completedSections={completedSections}
            />

            <div className="relative flex h-full min-w-0 flex-1 flex-col overflow-hidden bg-white">
              <div
                ref={contentScrollRef}
                className="min-w-0 flex-1 overflow-x-hidden overflow-y-auto px-12 pb-8"
              >
                {isLocked ? (
                  <p className="mt-4 rounded-xl bg-(--neutral-50) px-4 py-3 text-sm text-(--text-neutral-600)">
                    {isFinalized
                      ? "The report is finalized, so these answers are locked."
                      : "You can view these answers but not change them."}
                  </p>
                ) : null}
                <fieldset
                  disabled={isLocked}
                  className={isLocked ? "pointer-events-none min-w-0" : "min-w-0"}
                >
                  <AssessmentSectionRenderer
                    section={currentSection}
                    answers={answers}
                    onAnswerChange={handleAnswerChange}
                  />
                </fieldset>
              </div>

              <div className="flex shrink-0 flex-wrap items-center justify-between gap-3 border-t border-(--neutral-100) bg-white px-8 py-4">
                <span className="text-sm text-(--text-neutral-600)">{saveStatus}</span>
                <div className="flex items-center gap-3">
                  {activeSectionIndex > 0 ? (
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => void goToSection(activeSectionIndex - 1)}
                      disabled={isSavingAssessment}
                      className="h-11 rounded-full px-6 text-sm font-medium"
                    >
                      Back
                    </Button>
                  ) : null}
                  <Button
                    type="button"
                    onClick={() =>
                      void (isLastSection
                        ? goToStep("report")
                        : goToSection(activeSectionIndex + 1))
                    }
                    disabled={isSavingAssessment}
                    className="h-11 rounded-full bg-(--bg-primary-dark) px-8 text-sm font-semibold text-white"
                  >
                    {isLastSection ? "Next: Report" : "Next"}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default TakeAssessmentModal;
