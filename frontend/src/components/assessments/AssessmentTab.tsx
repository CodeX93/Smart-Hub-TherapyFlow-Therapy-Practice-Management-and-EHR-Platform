import { useMemo, useState } from "react";
import AssessmentStats from "./AssessmentStats";
import AssessmentTemplateCard from "./AssessmentTemplateCard";
import AssessmentHistoryCard from "./AssessmentHistoryCard";
import {
  ASSESSMENT_OVERVIEW,
  type AssessmentHistoryItem,
  type AssessmentTemplate,
} from "../../pages/therapist/therapist.static";
import { X } from "lucide-react";
import ConfirmationModal from "../shared/ConfirmationModal";
import { ContentLoader } from "@/components/shared/ContentLoader";
import TakeAssessmentModal from "./take-assessment/TakeAssessmentModal";
import EmptyAssessmentsState from "./EmptyAssessmentsState";
import {
  useAssignClientAssessmentMutation,
  useDeleteClientAssessmentMutation,
  useGetAssessmentAnalyticsQuery,
  useGetAssessmentTemplatesQuery,
  useGetClientAssessmentsQuery,
  type AdminAssessmentTemplate,
  type AdminClientAssessment,
} from "@/store/api/admin/clients.api";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import CustomTextarea from "@/components/form/CustomTextarea";
import { Button } from "@/components/ui/button";
import Toast from "@/components/shared/Toast";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  downloadAssessmentReportFile,
  triggerBrowserDownload,
} from "@/utils/downloadAssessmentReport";

/** Stable fallbacks so `?? []` does not create a new array every render. */
const EMPTY_CLIENT_ASSESSMENTS: AdminClientAssessment[] = [];
const EMPTY_TEMPLATES: AdminAssessmentTemplate[] = [];

interface AssessmentTabProps {
  clientId?: number;
  readOnly?: boolean;
}

function normalizeAssessmentStatus(status?: string): AssessmentHistoryItem["status"] {
  const normalized = status?.trim().toLowerCase().replace(/[_\s]+/g, " ");
  if (normalized === "completed") return "Completed";
  if (
    normalized === "in progress" ||
    normalized === "client in progress" ||
    normalized === "therapist in progress" ||
    normalized === "waiting for therapist"
  ) {
    return "In Progress";
  }
  return "Pending";
}

function formatAssessmentDate(date?: string): string | undefined {
  if (!date) return undefined;
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return undefined;

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  }).format(parsed);
}

const AssessmentTab = ({ clientId, readOnly = false }: AssessmentTabProps) => {
  const minAssessmentYear = 2010;
  const maxAssessmentYear = new Date().getFullYear() + 10;
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [assessmentToDelete, setAssessmentToDelete] = useState<string | null>(
    null,
  );
  const [takeAssessmentOpen, setTakeAssessmentOpen] = useState(false);
  const [activeAssessmentId, setActiveAssessmentId] = useState<string | null>(
    null,
  );
  const [activeTemplateId, setActiveTemplateId] = useState<number | null>(null);
  const [activeAssessmentMeta, setActiveAssessmentMeta] = useState<{
    clientId?: number;
    clientName?: string;
    assessmentName?: string;
    assignedDate?: string;
    status?: string;
  } | null>(null);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<AssessmentTemplate | null>(null);
  const [assignDueDate, setAssignDueDate] = useState<Date | null>(null);
  const [assignNotes, setAssignNotes] = useState("");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [downloadingPdfId, setDownloadingPdfId] = useState<string | null>(null);
  const [downloadingWordId, setDownloadingWordId] = useState<string | null>(null);
  const skipClientQueries = !clientId || clientId <= 0;

  const { data: templatesResponse, isLoading: isTemplatesLoading } =
    useGetAssessmentTemplatesQuery();
  const templates = templatesResponse?.items ?? EMPTY_TEMPLATES;
  const {
    data: clientAssessmentsData,
    isLoading: isHistoryLoading,
    refetch: refetchClientAssessments,
  } = useGetClientAssessmentsQuery(clientId ?? 0, {
    skip: skipClientQueries,
  });
  const clientAssessments = clientAssessmentsData ?? EMPTY_CLIENT_ASSESSMENTS;
  const { data: analytics, isLoading: isAnalyticsLoading, refetch: refetchAnalytics } =
    useGetAssessmentAnalyticsQuery(
      { clientId },
      {
        skip: skipClientQueries,
      },
    );
  const [assignClientAssessment] = useAssignClientAssessmentMutation();
  const [deleteClientAssessment, { isLoading: isDeletingAssessment }] =
    useDeleteClientAssessmentMutation();
  const [isAssigningAssessment, setIsAssigningAssessment] = useState(false);

  const overviewData = useMemo(
    () => ({
      totalAssigned: analytics?.totalAssignments ?? ASSESSMENT_OVERVIEW.totalAssigned,
      completed: analytics?.completedAssignments ?? ASSESSMENT_OVERVIEW.completed,
      inProgress: analytics?.inProgressAssignments ?? ASSESSMENT_OVERVIEW.inProgress,
      pending: analytics?.pendingAssignments ?? ASSESSMENT_OVERVIEW.pending,
    }),
    [analytics],
  );

  const templateCards = useMemo<AssessmentTemplate[]>(
    () =>
      templates.map((template) => ({
        id: String(template.id),
        title: template.title || template.name || "Untitled Assessment",
        description: template.description || "No description available.",
        category: template.category || "General",
      })),
    [templates],
  );

  const history = useMemo<AssessmentHistoryItem[]>(
    () =>
      clientAssessments.map((assessment) => ({
        id: String(assessment.id),
        title: assessment.templateName || "Untitled Assessment",
        description:
          assessment.notes ||
          [
            assessment.assignedByName ? `Assigned by ${assessment.assignedByName}` : null,
            formatAssessmentDate(assessment.assignedDate),
          ]
            .filter(Boolean)
            .join(" • ") ||
          "No details available.",
        status: normalizeAssessmentStatus(assessment.status),
        rawStatus: assessment.status,
        date: formatAssessmentDate(
          normalizeAssessmentStatus(assessment.status) === "Completed"
            ? assessment.completedAt
            : assessment.dueDate,
        ),
      })),
    [clientAssessments],
  );

  const openAssessment = async (assessmentId: string) => {
    const refreshed = await refetchClientAssessments();
    const assessments = refreshed.data ?? clientAssessments;
    const assessment = assessments.find(
      (item) => String(item.id) === assessmentId,
    );
    setActiveAssessmentId(assessmentId);
    setActiveTemplateId(assessment?.templateId ?? null);
    setActiveAssessmentMeta({
      clientId: assessment?.clientId,
      clientName: assessment?.clientName,
      assessmentName: assessment?.templateName,
      assignedDate: assessment?.assignedDate,
      status: assessment?.status,
    });
    setTakeAssessmentOpen(true);
  };

  const handleAssign = (templateId: string) => {
    const nextTemplate =
      templateCards.find((template) => template.id === templateId) ?? null;
    setSelectedTemplate(nextTemplate);
    setAssignDueDate(null);
    setAssignNotes("");
    setAssignModalOpen(true);
  };

  const handleDownloadReport = async (
    assessmentId: string,
    format: "pdf" | "docx",
  ) => {
    const assignmentId = Number(assessmentId);
    if (!Number.isFinite(assignmentId) || assignmentId <= 0) {
      setToastType("error");
      setToastMessage("Invalid assessment assignment.");
      return;
    }

    const setDownloadingId =
      format === "pdf" ? setDownloadingPdfId : setDownloadingWordId;

    try {
      setDownloadingId(assessmentId);
      const { blob, filename } = await downloadAssessmentReportFile(
        assignmentId,
        format,
      );
      triggerBrowserDownload(blob, filename);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setDownloadingId(null);
    }
  };

  const refreshAssessmentData = async () => {
    await Promise.all([refetchClientAssessments(), refetchAnalytics()]);
  };

  const handleDelete = (assessmentId: string) => {
    setAssessmentToDelete(assessmentId);
    setDeleteModalOpen(true);
  };

  const confirmDelete = async () => {
    if (!assessmentToDelete) return;

    const assignmentId = Number(assessmentToDelete);
    if (!Number.isFinite(assignmentId) || assignmentId <= 0) {
      setToastType("error");
      setToastMessage("Invalid assessment assignment.");
      return;
    }

    try {
      await deleteClientAssessment(assignmentId).unwrap();
      setAssessmentToDelete(null);
      setDeleteModalOpen(false);
      await Promise.all([refetchClientAssessments(), refetchAnalytics()]);
      setToastType("success");
      setToastMessage("Assessment deleted successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const closeAssignModal = () => {
    if (isAssigningAssessment) return;
    setAssignModalOpen(false);
    setSelectedTemplate(null);
    setAssignDueDate(null);
    setAssignNotes("");
  };

  const handleAssignSubmit = async () => {
    if (!clientId || !selectedTemplate || !assignDueDate || isAssigningAssessment) return;

    try {
      setIsAssigningAssessment(true);
      const dueDate = new Date(assignDueDate);
      dueDate.setHours(23, 59, 59, 0);

      await assignClientAssessment({
        clientId,
        templateId: Number(selectedTemplate.id),
        dueDate: dueDate.toISOString(),
        notes: assignNotes.trim() || undefined,
      }).unwrap();

      await Promise.all([refetchClientAssessments(), refetchAnalytics()]);
      setToastType("success");
      setToastMessage("Assessment assigned successfully.");
      setAssignModalOpen(false);
      setSelectedTemplate(null);
      setAssignDueDate(null);
      setAssignNotes("");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setIsAssigningAssessment(false);
    }
  };

  return (
    <div className="space-y-8 p-6 bg-white min-h-full">
      {/* Overview Stats */}
      <section>
        <AssessmentStats data={overviewData} />
      </section>

      {/* Assigned assessments */}
      <section>
        <h3 className="text-sm font-semibold text-(--text-primary-dark) mb-4">
          Assigned Assessments
        </h3>
        {isHistoryLoading || isAnalyticsLoading ? (
          <ContentLoader size="md" className="min-h-40" />
        ) : history.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {history.map((assessment) => (
              <AssessmentHistoryCard
                key={assessment.id}
                assessment={assessment}
                onOpen={(id) => void openAssessment(id)}
                onDownloadPdf={(id) => void handleDownloadReport(id, "pdf")}
                onDownloadWord={(id) => void handleDownloadReport(id, "docx")}
                onDelete={readOnly ? undefined : handleDelete}
                downloadingPdfId={downloadingPdfId}
                downloadingWordId={downloadingWordId}
                isDeleting={isDeletingAssessment && assessmentToDelete === assessment.id}
                readOnly={readOnly}
              />
            ))}
          </div>
        ) : (
          <EmptyAssessmentsState
            description={
              readOnly
                ? "Assigned assessments will appear here."
                : "Pick a template below to assign one."
            }
          />
        )}
      </section>

      {/* Templates to assign from */}
      {!readOnly ? (
      <section>
        <h3 className="text-sm font-semibold text-(--text-primary-dark) mb-4">
          Assign a New Assessment
        </h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          {templateCards.map((template) => (
            <AssessmentTemplateCard
              key={template.id}
              template={template}
              onAssign={handleAssign}
            />
          ))}
        </div>
        {isTemplatesLoading && (
          <ContentLoader size="md" className="mt-4 min-h-24" />
        )}
      </section>
      ) : null}

      <ConfirmationModal
        type="delete"
        isOpen={deleteModalOpen}
        onClose={() => {
          if (isDeletingAssessment) return;
          setDeleteModalOpen(false);
          setAssessmentToDelete(null);
        }}
        onConfirm={() => void confirmDelete()}
        title="Delete Assessment"
        description="Are you sure you want to delete this assessment? This action cannot be undone."
        items={[]}
        confirmButtonText="Delete Assessment"
        confirmButtonLoading={isDeletingAssessment}
        confirmButtonLoadingText="Deleting..."
      />

      {takeAssessmentOpen && (
        <TakeAssessmentModal
          isOpen={takeAssessmentOpen}
          onClose={() => {
            setTakeAssessmentOpen(false);
            setActiveTemplateId(null);
            setActiveAssessmentMeta(null);
          }}
          assessmentId={activeAssessmentId || ""}
          templateId={activeTemplateId}
          clientIdValue={activeAssessmentMeta?.clientId}
          clientNameValue={activeAssessmentMeta?.clientName}
          assessmentNameValue={activeAssessmentMeta?.assessmentName}
          assignedDateValue={activeAssessmentMeta?.assignedDate}
          assessmentStatusValue={activeAssessmentMeta?.status}
          readOnly={readOnly}
          onAssessmentSaved={refreshAssessmentData}
        />
      )}

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      {assignModalOpen && selectedTemplate && (
        <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 backdrop-blur-[0.125rem] transition-all duration-300">
          <div className="mx-4 flex w-full max-w-135 max-h-[90vh] flex-col overflow-hidden rounded-[1.5rem] bg-white shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="flex shrink-0 items-start justify-between px-8 pt-8 pb-2">
              <div className="space-y-1">
                <h2 className="text-[1.375rem] font-bold text-(--neutral-950)">
                  Assign Assessment
                </h2>
                <p className="text-sm text-(--text-neutral-600)">
                  Assign this assessment template to the current client.
                </p>
              </div>
              <button
                onClick={closeAssignModal}
                type="button"
                className="p-1 text-(--text-neutral-400) hover:text-(--neutral-950) transition-colors cursor-pointer rounded-full hover:bg-(--neutral-50)"
                disabled={isAssigningAssessment}
              >
                <X size={24} />
              </button>
            </div>

            <div className="min-h-0 flex-1 space-y-6 overflow-y-auto overscroll-contain px-8 py-6">
              <div className="space-y-2">
                <label className="text-sm font-semibold text-(--neutral-950)">
                  Selected Template
                </label>
                <div className="p-5 rounded-2xl border border-(--neutral-100) bg-(--neutral-50)/30 space-y-3">
                  <div className="flex items-center justify-between gap-4">
                    <h3 className="text-base font-bold text-(--neutral-950)">
                      {selectedTemplate.title}
                    </h3>
                    <span className="px-3 py-1 bg-(--neutral-100) text-(--text-neutral-600) text-xs font-medium rounded-full">
                      {selectedTemplate.category}
                    </span>
                  </div>
                  <p className="text-xs text-(--text-neutral-400) leading-relaxed">
                    {selectedTemplate.description}
                  </p>
                </div>
              </div>

              <CustomDatePicker
                label="Due Date"
                date={assignDueDate}
                onDateChange={setAssignDueDate}
                disablePast
                required
                minYear={minAssessmentYear}
                maxYear={maxAssessmentYear}
              />

              <div className="space-y-1">
                <CustomTextarea
                  label="Notes"
                  value={assignNotes}
                  onChange={(event) => setAssignNotes(event.target.value)}
                  placeholder=" "
                  className="min-h-24"
                />
                <p className="text-[0.6875rem] text-(--text-neutral-400) px-1">
                  Add any additional notes or instructions
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center justify-end gap-4 border-t border-(--neutral-100) bg-white px-8 py-5">
              <Button
                type="button"
                variant="outline"
                onClick={closeAssignModal}
                disabled={isAssigningAssessment}
                className="px-10 py-3 h-auto border-(--neutral-100) text-(--neutral-950) rounded-full cursor-pointer text-base font-medium transition-all duration-300 hover:bg-(--neutral-50)"
              >
                Cancel
              </Button>
              <Button
                type="button"
                onClick={() => void handleAssignSubmit()}
                disabled={!assignDueDate || isAssigningAssessment}
                loading={isAssigningAssessment}
                loadingLabel="Assigning..."
                className="px-8 py-3 h-auto bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/95 text-white rounded-full cursor-pointer text-base font-medium transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Assign
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AssessmentTab;
