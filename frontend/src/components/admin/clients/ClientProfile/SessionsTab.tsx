import { ContentLoader } from "@/components/shared/ContentLoader";
import { CalendarIcon } from "@/components/icons/commonIcons";
import { useState, useCallback, useEffect, useRef, useMemo } from "react";
import { Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../ui/button";
import { SessionCard, SummaryCard } from "../../../sessions";
import AddSessionNoteModal from "../../../sessions/AddSessionNoteModal";
import RecordSessionModal from "../../../sessions/recording/RecordSessionModal";
import SessionTranscriptModal from "../../../sessions/recording/SessionTranscriptModal";
import FilterDropdown from "../../../shared/FilterDropdown";
import AddSessionModal from "@/components/scheduling-sections/add-session-modal";
import AddSessionSuccessModal from "@/components/scheduling-sections/AddSessionSuccessModal";
import { SCHEDULING_STATIC_CONTENT } from "../../../../pages/therapist/therapist.static";
import type { Client } from "../../../../types/client.type";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import CreateSessionBillingModal from "@/components/billing-sections/CreateSessionBillingModal";
import PreviewInvoiceModal from "@/components/billing-sections/PreviewInvoiceModal";
import { isSessionEligibleForManualBilling, canShowSessionPayNow } from "@/utils/sessionBillingUi";
import { getClientSchedulingBlockMessage } from "@/utils/clientStatus";
import { useSessionCompletedPayNow } from "@/hooks/useSessionCompletedPayNow";
import { useClientOverviewLabels } from "@/hooks/useSystemOptionCatalog";
import {
  type AdminDashboardSession,
  useDeleteSessionMutation,
  useGetAdminSessionsListQuery,
  useUpdateSessionStatusMutation,
} from "@/store/api/admin/dashboard.api";
import {
  useDeleteSessionTranscriptMutation,
  useLazyDownloadSessionTranscriptQuery,
  useLazyGetSessionTranscriptQuery,
  useDiarizeSessionTranscriptMutation,
  useSmartFillSessionTranscriptMutation,
} from "@/store/api/admin/sessionTranscripts.api";
import { useGetAdminClientSessionSummaryQuery } from "@/store/api/admin/clients.api";
import {
  useGetClientSessionNotesQuery,
  useLazyGetSessionNotePdfQuery,
  useUnfinalizeSessionNoteMutation,
} from "@/store/api/admin/sessionNotes.api";
import { useCancelRecurringSeriesMutation } from "@/store/api/admin/recurringSessions.api";
import { useLazyPreviewInvoiceQuery } from "@/store/api/admin/billing.api";
import type { SessionNoteTranscriptionResponse } from "@/store/api/admin/sessionNotes.api";
import {
  buildStoredSessionTranscript,
  getStoredSessionTranscript,
  removeStoredSessionTranscript,
  saveStoredSessionTranscript,
  type StoredSessionTranscript,
} from "@/components/sessions/recording/sessionTranscriptStore";
import {
  getSmartFillEmptyMessage,
  hasExtractedSmartFillFields,
} from "@/components/sessions/recording/transcriptFieldMapping";
import {
  type SchedulingFormValues,
  type SchedulingSuccessData,
} from "@/schemas/scheduling.schema";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  isAllowedBackendSessionStatusTransition,
  type BackendSessionStatus,
} from "@/utils/sessionStatusTransitions";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import {
  formatDateInScheduleTimezone,
  formatTimeInScheduleTimezone,
} from "@/utils/scheduleTimezone";

interface SessionsTabProps {
  client: Client;
  scrollRef: React.RefObject<HTMLDivElement | null>;
  schedulingPath?: string;
  isAdmin?: boolean;
  isActive?: boolean;
  openCreateTrigger?: number;
  readOnly?: boolean;
  staffMode?: boolean;
  hideSessionManageActions?: boolean;
}

const SessionsTab = ({
  client,
  scrollRef,
  schedulingPath = "/admin/scheduling",
  isAdmin = true,
  isActive = false,
  openCreateTrigger = 0,
  readOnly = false,
  staffMode = false,
  hideSessionManageActions,
}: SessionsTabProps) => {
  const hideSessionActions = hideSessionManageActions ?? staffMode;
  const navigate = useNavigate();
  const clientId = Number(client.id);
  const skip = !Number.isFinite(clientId) || clientId <= 0;
  const overviewLabels = useClientOverviewLabels(client);
  // Admin Settings → Administration "Select Timezone" (practice/system timezone).
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const systemTimezone = practiceConfig?.timezone;
  const [isAddSessionOpen, setIsAddSessionOpen] = useState(false);
  const [isEditSessionOpen, setIsEditSessionOpen] = useState(false);
  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null);
  const [deleteSessionId, setDeleteSessionId] = useState<number | null>(null);
  const [cancelSeriesGroupId, setCancelSeriesGroupId] = useState<string | null>(null);
  const [isSuccessOpen, setIsSuccessOpen] = useState(false);
  const [submittedData, setSubmittedData] =
    useState<SchedulingSuccessData | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastVariant, setToastVariant] = useState<"success" | "error" | "info">("success");
  const [statusFilter, setStatusFilter] = useState("All Statuses");
  const [notesFilter, setNotesFilter] = useState("All Notes");
  const hasMountedStatusFilterEffect = useRef(false);
  const [viewNoteSessionId, setViewNoteSessionId] = useState<number | null>(null);
  const [viewNoteSessionData, setViewNoteSessionData] = useState<{
    sessionId: number;
    clientId: number | null;
    therapistId: number | null;
    dateIso: string | null;
    dateTime: string;
    sessionType: string;
    clientName: string;
  } | null>(null);
  const [viewNoteId, setViewNoteId] = useState<number | null>(null);
  const [viewNoteInitialEditMode, setViewNoteInitialEditMode] = useState(false);
  const [reopenNoteTarget, setReopenNoteTarget] = useState<{
    sessionId: number;
    noteId: number;
  } | null>(null);
  const [recordSessionData, setRecordSessionData] = useState<{
    sessionId: number;
    clientId: number | null;
    therapistId: number | null;
    dateTime: string;
    sessionType: string;
    clientName: string;
  } | null>(null);
  const [activeTranscriptSessionId, setActiveTranscriptSessionId] = useState<number | null>(null);
  const [activeTranscript, setActiveTranscript] = useState<StoredSessionTranscript | null>(null);
  const [transcriptNoteSessionData, setTranscriptNoteSessionData] = useState<{
    sessionId: number;
    clientId: number | null;
    therapistId: number | null;
    dateIso: string | null;
    dateTime: string;
    sessionType: string;
    clientName: string;
  } | null>(null);
  const [transcriptNoteId, setTranscriptNoteId] = useState<number | null>(null);
  const [transcriptNoteData, setTranscriptNoteData] =
    useState<SessionNoteTranscriptionResponse | null>(null);

  const [previewInvoiceId, setPreviewInvoiceId] = useState<number | null>(null);
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);
  const [triggerPreview, { isFetching: isPreviewing }] = useLazyPreviewInvoiceQuery();

  const [createBillingSessionId, setCreateBillingSessionId] = useState<number | null>(null);
  const [updateSessionStatus] = useUpdateSessionStatusMutation();
  const { maybeOpenAfterStatusChange, openPayNowForSession, payNowModal } = useSessionCompletedPayNow({
    onToast: (message, type = "info") => {
      setToastVariant(type);
      setToastMessage(message);
    },
    onPaymentRecorded: () => {
      void refetchSessionsList();
    },
  });
  const [deleteSession, { isLoading: isDeletingSession }] = useDeleteSessionMutation();
  const [cancelRecurringSeries, { isLoading: isCancellingSeries }] =
    useCancelRecurringSeriesMutation();
  const [smartFillSessionTranscript, { isLoading: isSmartFillingTranscript }] =
    useSmartFillSessionTranscriptMutation();
  const [diarizeSessionTranscript, { isLoading: isDiarizingTranscript }] =
    useDiarizeSessionTranscriptMutation();
  const [unfinalizeSessionNote, { isLoading: isReopeningNote }] = useUnfinalizeSessionNoteMutation();
  const [deleteSessionTranscript, { isLoading: isDeletingTranscript }] =
    useDeleteSessionTranscriptMutation();
  const [triggerGetSessionTranscript, { isFetching: isTranscriptLoading }] =
    useLazyGetSessionTranscriptQuery();
  const [triggerDownloadSessionTranscript, { isFetching: isTranscriptDownloading }] =
    useLazyDownloadSessionTranscriptQuery();
  const [triggerGetSessionNotePdf] = useLazyGetSessionNotePdfQuery();

  const {
    data: sessionSummary,
    isLoading: isSummaryLoading,
    isError: isSummaryError,
    refetch: refetchSummary,
  } = useGetAdminClientSessionSummaryQuery(clientId, { skip });
  const [clientSessions, setClientSessions] = useState<AdminDashboardSession[]>([]);
  const [sessionsPage, setSessionsPage] = useState(1);
  const [sessionsTotalPages, setSessionsTotalPages] = useState(1);
  const [isSessionsError, setIsSessionsError] = useState(false);
  const {
    data: sessionsResponse,
    isLoading: isSessionsLoading,
    isFetching: isSessionsFetching,
    isError: isSessionsQueryError,
    refetch: refetchSessionsList,
  } = useGetAdminSessionsListQuery(
    {
      clientId,
      page: sessionsPage,
      pageSize: 25,
      status:
        statusFilter !== "All Statuses"
          ? statusFilter.trim().toUpperCase().replace(/\s+/g, "_")
          : undefined,
    },
    { skip: skip || !isActive },
  );
  const {
    data: clientSessionNotes = [],
    refetch: refetchClientSessionNotes,
  } = useGetClientSessionNotesQuery(clientId, { skip });

  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const handleLoadMore = useCallback(() => {
    if (isLoadingMore || isSessionsLoading || isSessionsFetching) return;
    if (sessionsPage >= sessionsTotalPages) return;
    setIsLoadingMore(true);
    setSessionsPage((prev) => prev + 1);
  }, [
    isLoadingMore,
    isSessionsLoading,
    isSessionsFetching,
    sessionsPage,
    sessionsTotalPages,
  ]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: sessionsPage < sessionsTotalPages,
    isLoading: isLoadingMore || isSessionsLoading || isSessionsFetching,
  });

  useEffect(() => {
    if (!isActive || skip) return;
    if (!sessionsResponse) return;
    setSessionsTotalPages(Math.max(sessionsResponse.totalPages || 1, 1));
    setClientSessions((prev) =>
      sessionsPage <= 1 ? sessionsResponse.items : [...prev, ...sessionsResponse.items],
    );
    setIsLoadingMore(false);
    setIsSessionsError(false);
  }, [isActive, skip, sessionsResponse, sessionsPage]);

  useEffect(() => {
    if (!isActive || skip) return;
    if (!isSessionsQueryError) return;
    setIsSessionsError(true);
    setIsLoadingMore(false);
    setToastVariant("error");
    setToastMessage("Failed to load sessions for selected filters.");
  }, [isActive, skip, isSessionsQueryError]);

  const totalSessions = sessionSummary?.totalSessions ?? 0;
  const completedSessions = sessionSummary?.completed ?? 0;
  const scheduledSessions = sessionSummary?.scheduled ?? 0;
  const cancelledSessions = sessionSummary?.missedCancelled ?? 0;
  const conflictSessions = sessionSummary?.conflicts ?? 0;

  function formatSessionDateTime(date?: string): string {
    if (!date) return "---";

    // Use admin system timezone — never the browser local zone.
    const datePart = formatDateInScheduleTimezone(date, systemTimezone);
    const timePart = formatTimeInScheduleTimezone(date, systemTimezone);
    if (!datePart || datePart === "-") return "---";
    return timePart ? `${datePart}, ${timePart}` : datePart;
  }

  const sessions = clientSessions
    .filter((session) => {
      const hasLinkedNote = clientSessionNotes.some((note) => note.sessionId === session.id);
      if (notesFilter === "With Notes") return hasLinkedNote;
      if (notesFilter === "Without Notes") return !hasLinkedNote;
      return true;
    })
    .map((session) => {
      const matchedNote = clientSessionNotes.find((note) => note.sessionId === session.id);
      const hasSubmittedNote = Boolean(matchedNote?.isFinalized);
      const hasTranscript = Boolean(session.hasTranscript);

      return {
        id: String(session.id),
        title: `${session.sessionType || "Session"}${session.duration ? ` ${session.duration} minutes` : ""}`,
        status: session.status || "---",
        type:
          ["online", "virtual", "video", "telehealth"].includes(
            (session.sessionMode || "").trim().toLowerCase().replace(/_/g, "-"),
          )
            ? "Virtual"
            : "In person",
        zoomEnabled:
          Boolean(session.zoomEnabled) &&
          ["online", "virtual", "video", "telehealth"].includes(
            (session.sessionMode || "").trim().toLowerCase().replace(/_/g, "-"),
          ),
        zoomJoinUrl: ["online", "virtual", "video", "telehealth"].includes(
          (session.sessionMode || "").trim().toLowerCase().replace(/_/g, "-"),
        )
          ? session.zoomJoinUrl
          : undefined,
        zoomPassword: ["online", "virtual", "video", "telehealth"].includes(
          (session.sessionMode || "").trim().toLowerCase().replace(/_/g, "-"),
        )
          ? session.zoomPassword
          : undefined,
        sessionMode: session.sessionMode,
        room: session.roomName ? `Room: ${session.roomName}` : "Room: ---",
        dateTime: formatSessionDateTime(session.sessionDate),
        clientId: session.clientId ?? clientId,
        therapistId: session.therapistId ?? undefined,
        sessionDateIso: session.sessionDate ?? undefined,
        hasSessionNote: Boolean(matchedNote),
        hasSubmittedNote,
        hasTranscript,
        canRecordSession: !hasSubmittedNote && !hasTranscript,
        noteId: matchedNote?.id ?? null,
        recurrenceGroupId: session.recurrenceGroupId,
        billingId: session.billingId,
        hasInvoice: session.hasInvoice,
        remainingDue: session.remainingDue,
        invoicePaid: session.invoicePaid,
      };
    });

  const initialSessionData: Partial<SchedulingFormValues> = useMemo(
    () => ({
      client: String(client.id),
      therapist:
        isAdmin && client.assignedTherapistId
          ? String(client.assignedTherapistId)
          : "",
    }),
    [client.assignedTherapistId, client.id, isAdmin],
  );

  const lockedClientLabel = useMemo(
    () =>
      client.clientId ? `${client.name} (${client.clientId})` : client.name,
    [client.clientId, client.name],
  );

  const resolveSessionContext = (sessionId: number) => {
    const matchedSession = sessions.find((item) => Number(item.id) === sessionId);
    if (!matchedSession) return null;

    return {
      sessionId,
      clientId: matchedSession.clientId ?? clientId,
      therapistId: matchedSession.therapistId ?? null,
      dateIso: matchedSession.sessionDateIso ?? null,
      dateTime: matchedSession.dateTime ?? "",
      sessionType: matchedSession.title ?? "Session",
      clientName: client.name,
    };
  };

  const handleSessionAction = (action: string, sessionId: string) => {
    const parsedSessionId = Number.parseInt(sessionId, 10);
    if (!Number.isFinite(parsedSessionId)) return;

    if (action === "join") {
      const matched = sessions.find((item) => Number(item.id) === parsedSessionId);
      const url = matched?.zoomJoinUrl?.trim();
      if (url) {
        window.open(url, "_blank", "noopener,noreferrer");
      } else {
        handleToast("Zoom meeting link is not available for this session.", "error");
      }
      return;
    }

    if (action === "create-invoice") {
      setCreateBillingSessionId(parsedSessionId);
      return;
    }

    if (action === "pay-now") {
      void openPayNowForSession(parsedSessionId, { notifyIfUnpayable: true });
      return;
    }

    if (action === "preview-invoice") {
      const matchedSession = clientSessions.find((s) => Number(s.id) === parsedSessionId);
      if (matchedSession?.billingId) {
        setPreviewInvoiceId(matchedSession.billingId);
        triggerPreview(matchedSession.billingId)
          .unwrap()
          .then((response) => setPreviewHtml(response.html))
          .catch((error) => {
            handleToast(getApiErrorMessage(error), "error");
            setPreviewInvoiceId(null);
          });
      }
      return;
    }

    if (action === "record-session") {
      const context = resolveSessionContext(parsedSessionId);
      if (!context) return;
      const matchedNote = clientSessionNotes.find((note) => note.sessionId === parsedSessionId);
      if (matchedNote?.isFinalized) {
        handleToast("Finalized session notes cannot be recorded again.", "error");
        return;
      }
      const apiSession = clientSessions.find((s) => s.id === parsedSessionId);
      if (apiSession?.hasTranscript) {
        handleToast("A transcript already exists for this session.", "error");
        return;
      }
      setRecordSessionData({
        sessionId: context.sessionId,
        clientId: context.clientId,
        therapistId: context.therapistId,
        dateTime: context.dateTime,
        sessionType: context.sessionType,
        clientName: context.clientName,
      });
      return;
    }

    if (action === "download-note-pdf") {
      const matchedNote = clientSessionNotes.find((note) => note.sessionId === parsedSessionId);
      if (!matchedNote?.id) {
        handleToast("No session note PDF is available for this session.", "error");
        return;
      }

      void triggerGetSessionNotePdf(matchedNote.id)
        .unwrap()
        .then((response) => {
          const trimmed = response.trim();
          const isHtml =
            trimmed.startsWith("<!DOCTYPE html") ||
            trimmed.startsWith("<html") ||
            trimmed.includes("<body");

          if (isHtml) {
            const printWindow = window.open("", "_blank");
            if (!printWindow) {
              handleToast("Popup blocked. Please allow popups and try again.", "error");
              return;
            }
            printWindow.document.open();
            printWindow.document.write(response);
            printWindow.document.close();
            printWindow.focus();
            setTimeout(() => {
              printWindow.print();
            }, 250);
            handleToast("Session note PDF opened successfully.", "success");
            return;
          }

          window.open(response, "_blank", "noopener,noreferrer");
          handleToast("Session note PDF opened successfully.", "success");
        })
        .catch((error) => {
          handleToast(getApiErrorMessage(error), "error");
        });
      return;
    }

    if (action === "reopen-note") {
      const matchedNote = clientSessionNotes.find((note) => note.sessionId === parsedSessionId);
      if (!matchedNote?.id) {
        handleToast("No session note was found for this session.", "error");
        return;
      }
      setReopenNoteTarget({
        sessionId: parsedSessionId,
        noteId: matchedNote.id,
      });
      return;
    }

    if (action === "view-transcript") {
      const cachedTranscript = getStoredSessionTranscript(parsedSessionId);
      setActiveTranscriptSessionId(parsedSessionId);
      setActiveTranscript(cachedTranscript?.rawTranscription.trim() ? cachedTranscript : null);
      void triggerGetSessionTranscript(parsedSessionId)
        .unwrap()
        .then((response) => {
          const finalTranscript =
            response.content?.trim() || response.finalTranscript?.trim() || "";
          if (!finalTranscript) {
            removeStoredSessionTranscript(parsedSessionId);
            setActiveTranscript(null);
            return;
          }

          const context = resolveSessionContext(parsedSessionId);
          const transcript = buildStoredSessionTranscript({
            transcriptId: response.transcriptId,
            sessionId: response.sessionId,
            uploadId: response.uploadId,
            clientId: response.clientId ?? context?.clientId ?? clientId,
            therapistId: context?.therapistId ?? null,
            clientName: response.clientName || context?.clientName || client.name,
            sessionType: context?.sessionType ?? "Session",
            sessionDateTime: context?.dateTime ?? "",
            durationSeconds: response.durationSeconds ?? 0,
            status: response.status,
            expectedChunks: response.expectedChunks,
            receivedChunks: response.receivedChunks,
            rawTranscription: finalTranscript,
            diarizedTranscript: response.diarizedTranscript ?? null,
            wordCount: response.wordCount ?? undefined,
            mappedFields: activeTranscript?.mappedFields ?? {},
            finalizedAt: response.finalizedAt,
            failureReason: response.failureReason,
          });
          saveStoredSessionTranscript(transcript);
          setActiveTranscript(transcript);
        })
        .catch((error) => {
          handleToast(getApiErrorMessage(error), "error");
          setActiveTranscript(null);
        });
      return;
    }

    if (action === "delete-session") {
      setDeleteSessionId(parsedSessionId);
      return;
    }

    if (action === "cancel-series") {
      const matchedSession = clientSessions.find((session) => session.id === parsedSessionId);
      if (matchedSession?.recurrenceGroupId) {
        setCancelSeriesGroupId(matchedSession.recurrenceGroupId);
      }
      return;
    }

    if (action === "edit") {
      setSelectedSessionId(parsedSessionId);
      setIsEditSessionOpen(true);
      return;
    }

    const validStatuses = [
      "SCHEDULED",
      "CONFIRMED",
      "IN_PROGRESS",
      "COMPLETED",
      "CANCELLED",
      "RESCHEDULING",
      "NO_SHOW",
      "OVERDUE",
    ] as const;
    if (!validStatuses.includes(action as typeof validStatuses[number])) {
      return;
    }

    const currentSession = clientSessions.find(
      (session) => session.id === parsedSessionId,
    );
    const nextStatus = action as BackendSessionStatus;
    if (
      currentSession &&
      !isAllowedBackendSessionStatusTransition(currentSession.status, nextStatus, {
        scheduledAt: currentSession.sessionDate,
        hasInvoice: Boolean(currentSession.billingId || currentSession.hasInvoice),
        practiceTimezone: systemTimezone,
      })
    ) {
      return;
    }

    void updateSessionStatus({
      id: parsedSessionId,
      body: {
        status: action as
          | "SCHEDULED"
          | "CONFIRMED"
          | "IN_PROGRESS"
          | "COMPLETED"
          | "CANCELLED"
          | "RESCHEDULING"
          | "NO_SHOW"
          | "OVERDUE",
      },
    })
      .unwrap()
      .then(() => {
        setToastVariant("success");
        setToastMessage("Session status updated successfully.");
        setSessionsPage(1);
        if (sessionsResponse?.items) {
          setClientSessions(sessionsResponse.items);
        }
        void refetchSessionsList();
        maybeOpenAfterStatusChange(parsedSessionId, action);
      })
      .catch((error) => {
        setToastVariant("error");
        setToastMessage(getApiErrorMessage(error));
      });
  };

  const handleSchedule = (data: SchedulingSuccessData) => {
    const wasEdit = isEditSessionOpen;
    setSubmittedData(data);
    setIsAddSessionOpen(false);
    setIsEditSessionOpen(false);
    setSelectedSessionId(null);
    setSessionsPage(1);
    if (sessionsResponse?.items) {
      setClientSessions(sessionsResponse.items);
    }
    void refetchSessionsList();
    void refetchSummary();
    void refetchClientSessionNotes();
    handleToast(
      wasEdit ? "Session updated successfully." : "Session created successfully.",
      "success",
    );
    setIsSuccessOpen(true);
  };

  useEffect(() => {
    if (!isActive || skip) return;
    setSessionsPage(1);
    setSessionsTotalPages(1);
    if (sessionsResponse?.items) {
      setClientSessions(sessionsResponse.items);
    }
    void refetchSessionsList();
    void refetchSummary();
    void refetchClientSessionNotes();
    // Intentionally scoped to tab/client activation to avoid repeated clears.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isActive, skip, clientId]);

  useEffect(() => {
    if (!hasMountedStatusFilterEffect.current) {
      hasMountedStatusFilterEffect.current = true;
      return;
    }
    if (!isActive || skip) return;
    setSessionsPage(1);
    setSessionsTotalPages(1);
    setIsSessionsError(false);
    void refetchSessionsList();
  }, [statusFilter, isActive, skip, refetchSessionsList]);

  const handleStatusFilterChange = (value: string) => {
    if (value === statusFilter) return;
    setStatusFilter(value);
    setToastVariant("success");
    setToastMessage("Status filter applied.");
  };

  const handleNotesFilterChange = (value: string) => {
    if (value === notesFilter) return;
    setNotesFilter(value);
    setToastVariant("success");
    setToastMessage("Notes filter applied.");
  };

  const handleToast = useCallback(
    (message: string, type: "success" | "error" | "info") => {
      setToastVariant(type);
      setToastMessage(message);
    },
    [],
  );

  const handleCloseTranscriptModal = useCallback(() => {
    setActiveTranscriptSessionId(null);
    setActiveTranscript(null);
  }, []);

  useEffect(() => {
    if (!isActive || !openCreateTrigger || readOnly) return;
    setIsEditSessionOpen(false);
    setSelectedSessionId(null);
    setIsAddSessionOpen(true);
  }, [isActive, openCreateTrigger]);

  return (
    <>
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastVariant}
          onClose={() => setToastMessage(null)}
          duration={
            toastVariant === "error" && (toastMessage?.length ?? 0) > 120
              ? 12000
              : 2500
          }
        />
      ) : null}
      <ScrollToTopButton containerRef={scrollRef} />

      <div className="p-6 space-y-6">
        {/* Summary Cards */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          <SummaryCard
            label="Total Sessions"
            value={String(totalSessions).padStart(2, "0")}
          />
          <SummaryCard label="Completed" value={String(completedSessions)} />
          <SummaryCard
            label="Scheduled"
            value={String(scheduledSessions).padStart(2, "0")}
          />
          <SummaryCard
            label="Missed/Cancelled"
            value={String(cancelledSessions)}
          />
          <SummaryCard label="Conflicts" value={String(conflictSessions)} />
        </div>

        {(isSummaryLoading || isSessionsLoading) && (
          <div className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
            <ContentLoader variant="inline" size="sm" />
          </div>
        )}

        {(isSummaryError || isSessionsError) && (
          <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
            Could not load client sessions.
          </div>
        )}

        {/* Session History Header */}
        <div>
          <h3 className="text-base font-semibold text-gray-900 mb-4">
            Session History
          </h3>

          <div className="flex items-center justify-between mb-6">
            {/* Left: Filters */}
            <div className="flex gap-3">
              <FilterDropdown
                options={SCHEDULING_STATIC_CONTENT.sessionFilters.statusOptions}
                defaultValue={statusFilter}
                onChange={handleStatusFilterChange}
              />
              <FilterDropdown
                options={SCHEDULING_STATIC_CONTENT.sessionFilters.notesOptions}
                defaultValue={notesFilter}
                onChange={handleNotesFilterChange}
              />
              {isSessionsFetching ? (
                <div className="flex items-center gap-2 text-sm text-(--text-neutral-600) px-2">
                  <ContentLoader variant="inline" size="sm" />
                </div>
              ) : null}
            </div>

            {/* Right: Action Buttons */}
            <div className="flex gap-3">
              <Button
                onClick={() => navigate(schedulingPath)}
                variant="outline"
                className="h-10 px-4 rounded-full cursor-pointer text-sm font-normal flex items-center gap-2"
              >
                <CalendarIcon size={16} />
                View Calendar
              </Button>
              {!readOnly && !hideSessionActions ? (
              <button
                type="button"
                onClick={() => {
                  const blockMessage = getClientSchedulingBlockMessage(
                    client,
                    overviewLabels.clientStatus !== "—"
                      ? overviewLabels.clientStatus
                      : undefined,
                  );
                  if (blockMessage) {
                    handleToast(blockMessage, "error");
                    return;
                  }
                  setIsAddSessionOpen(true);
                }}
                className="h-10 px-4 font-semibold rounded-full cursor-pointer text-sm flex items-center gap-2 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white transition duration-300"
              >
                <Plus size={16} />
                Schedule Session
              </button>
              ) : null}
            </div>
          </div>
        </div>

        {/* Session Cards Grid */}
        <div className="grid grid-cols-1 items-stretch gap-4 lg:grid-cols-2">
          {!isSessionsLoading && !isSessionsError && sessions.length > 0 ? (
            sessions.map((session) => (
              <SessionCard
                key={session.id}
                session={session}
                clientName={client.name}
                hideStatusActions={readOnly}
                hideEditAction={hideSessionActions}
                onAction={readOnly ? undefined : handleSessionAction}
                hasSessionNote={session.hasSessionNote}
                onViewNote={(sessionId) => {
                  const matchedSession = sessions.find((item) => Number(item.id) === sessionId);
                  const matchedNote = clientSessionNotes.find((note) => note.sessionId === sessionId);
                  setViewNoteSessionId(sessionId);
                  setViewNoteId(matchedNote?.id ?? null);
                  setViewNoteInitialEditMode(false);
                  setViewNoteSessionData({
                    sessionId,
                    clientId: matchedSession?.clientId ?? clientId,
                    therapistId: matchedSession?.therapistId ?? null,
                    dateIso: matchedSession?.sessionDateIso ?? null,
                    dateTime: matchedSession?.dateTime ?? "",
                    sessionType: matchedSession?.title ?? "Session",
                    clientName: client.name,
                  });
                }}
                hasSubmittedNote={session.hasSubmittedNote}
                canRecordSession={readOnly ? false : session.canRecordSession}
                onNoteCreated={() => void refetchClientSessionNotes()}
                onToast={handleToast}
                hideRatingsTab
                showCreateInvoice={
                  !readOnly && isSessionEligibleForManualBilling({
                    status: session.status,
                    sessionDate: session.sessionDateIso,
                    billingId: session.billingId,
                    hasInvoice: session.hasInvoice,
                  }, new Date(), systemTimezone)
                }
                showPayNow={!readOnly && canShowSessionPayNow(session)}
              />
            ))
          ) : !isSessionsLoading && !isSessionsError ? (
            <div className="col-span-2 text-center py-8 text-gray-500">
              No sessions found for this client
            </div>
          ) : null}
        </div>

        {/* Load More Observer */}
        <div
          ref={observerTarget}
          className="h-10 w-full flex items-center justify-center mt-4"
        >
          {isLoadingMore && (
            <ContentLoader variant="inline" size="md" />
          )}
        </div>
      </div>

      <AddSessionModal
        isOpen={isAddSessionOpen}
        onClose={() => setIsAddSessionOpen(false)}
        onSchedule={handleSchedule}
        onToast={handleToast}
        initialData={initialSessionData}
        isAdmin={isAdmin}
        lockClientSelection={true}
        lockedClientLabel={lockedClientLabel}
      />

      <AddSessionModal
        isOpen={isEditSessionOpen}
        onClose={() => {
          setIsEditSessionOpen(false);
          setSelectedSessionId(null);
        }}
        onSchedule={handleSchedule}
        onToast={handleToast}
        initialData={initialSessionData}
        isAdmin={isAdmin}
        isEditSchedule={true}
        sessionId={selectedSessionId}
        lockClientSelection={true}
        lockedClientLabel={lockedClientLabel}
      />

      <AddSessionSuccessModal
        isOpen={isSuccessOpen}
        onClose={() => setIsSuccessOpen(false)}
        data={submittedData}
      />

      {viewNoteSessionData ? (
        <AddSessionNoteModal
          isOpen={viewNoteSessionId !== null}
          onClose={() => {
            setViewNoteSessionId(null);
            setViewNoteSessionData(null);
            setViewNoteId(null);
            setViewNoteInitialEditMode(false);
          }}
          sessionData={viewNoteSessionData}
          mode="view"
          noteId={viewNoteId}
          initialEditMode={viewNoteInitialEditMode}
          onToast={handleToast}
          hideRatingsTab
          onChanged={() => {
            void refetchClientSessionNotes();
            setSessionsPage(1);
            if (sessionsResponse?.items) {
              setClientSessions(sessionsResponse.items);
            }
            void refetchSessionsList();
          }}
        />
      ) : null}

      <RecordSessionModal
        isOpen={recordSessionData !== null}
        onClose={() => setRecordSessionData(null)}
        sessionData={recordSessionData}
        onToast={handleToast}
        onTranscriptSaved={(transcript) => {
          setRecordSessionData(null);

          handleToast("Session transcript saved successfully.", "success");
          window.setTimeout(() => {
            handleSessionAction("view-transcript", String(transcript.sessionId));
          }, 350);
        }}
      />

      <SessionTranscriptModal
        isOpen={activeTranscriptSessionId !== null}
        onClose={handleCloseTranscriptModal}
        transcript={activeTranscript}
        isLoading={isTranscriptLoading}
        onDownload={() => {
          if (!activeTranscriptSessionId) return;
          void triggerDownloadSessionTranscript(activeTranscriptSessionId)
            .unwrap()
            .then((content) => {
              const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
              const url = window.URL.createObjectURL(blob);
              const anchor = document.createElement("a");
              anchor.href = url;
              anchor.download = `${client.name.toLowerCase().replace(/[^a-z0-9]+/g, "-") || "session"}-transcript.txt`;
              anchor.click();
              window.URL.revokeObjectURL(url);
              handleToast("Transcript downloaded successfully.", "success");
            })
            .catch((error) => {
              handleToast(getApiErrorMessage(error), "error");
            });
        }}
        onDelete={() => {
          if (!activeTranscriptSessionId) return;
          void deleteSessionTranscript(activeTranscriptSessionId)
            .unwrap()
            .then(() => {
              removeStoredSessionTranscript(activeTranscriptSessionId);

              handleToast("Transcript deleted successfully.", "success");
              handleCloseTranscriptModal();
            })
            .catch((error) => {
              handleToast(getApiErrorMessage(error), "error");
            });
        }}
        onIdentifySpeakers={() => {
          if (!activeTranscript) return;
          if (activeTranscript.status.toLowerCase() !== "ready") {
            handleToast("Identify speakers is available after the transcript is ready.", "info");
            return;
          }
          void diarizeSessionTranscript({ sessionId: activeTranscript.sessionId })
            .unwrap()
            .then((response) => {
              const diarized =
                response.diarizedTranscript?.trim() ||
                response.content?.trim() ||
                response.finalTranscript?.trim() ||
                "";
              if (!diarized) {
                handleToast("Speaker identification returned no content. Please try again.", "error");
                return;
              }
              const updated: StoredSessionTranscript = {
                ...activeTranscript,
                diarizedTranscript: response.diarizedTranscript ?? diarized,
                formattedTranscript: diarized,
                rawTranscription:
                  response.content?.trim() ||
                  response.finalTranscript?.trim() ||
                  activeTranscript.rawTranscription,
                updatedAt: new Date().toISOString(),
              };
              saveStoredSessionTranscript(updated);
              setActiveTranscript(updated);
              handleToast("Speakers identified successfully.", "success");
            })
            .catch((error) => {
              handleToast(getApiErrorMessage(error), "error");
            });
        }}
        isSmartFillLoading={isSmartFillingTranscript}
        isDownloadLoading={isTranscriptDownloading}
        isDeleteLoading={isDeletingTranscript}
        isDiarizeLoading={isDiarizingTranscript}
        onSmartFill={() => {
          if (!activeTranscript) return;
          if (activeTranscript.status.toLowerCase() !== "ready") {
            handleToast("Smart fill is available after the transcript is ready.", "info");
            return;
          }
          void smartFillSessionTranscript({
            sessionId: activeTranscript.sessionId,
          })
            .unwrap()
            .then((response) => {
              const context = resolveSessionContext(activeTranscript.sessionId);
              if (!context) return;

              const extractedFields = response.mappedFields ?? {};

              if (!hasExtractedSmartFillFields(extractedFields)) {
                handleToast(getSmartFillEmptyMessage(response.message), "info");
                return;
              }

              const matchedNote = clientSessionNotes.find(
                (note) => note.sessionId === activeTranscript.sessionId,
              );

              const updatedTranscript: StoredSessionTranscript = {
                ...activeTranscript,
                uploadId: response.uploadId || activeTranscript.uploadId,
                rawTranscription: response.transcript || activeTranscript.rawTranscription,
                formattedTranscript: response.transcript || activeTranscript.formattedTranscript,
                mappedFields: extractedFields,
                wordCount: response.transcript
                  ? response.transcript.split(/\s+/).filter(Boolean).length
                  : activeTranscript.wordCount,
                updatedAt: new Date().toISOString(),
              };

              saveStoredSessionTranscript(updatedTranscript);
              setTranscriptNoteSessionData(context);
              setTranscriptNoteId(matchedNote?.id ?? null);
              setTranscriptNoteData({
                success: true,
                rawTranscription: response.transcript || activeTranscript.rawTranscription,
                mappedFields: extractedFields,
              });
              handleToast("Transcript smart fill completed.", "success");
              setActiveTranscript(null);
              setActiveTranscriptSessionId(null);
            })
            .catch((error) => {
              handleToast(getApiErrorMessage(error), "error");
            });
        }}
      />

      <ConfirmationModal
        type="delete"
        isOpen={cancelSeriesGroupId !== null}
        onClose={() => setCancelSeriesGroupId(null)}
        onConfirm={() => {
          if (!cancelSeriesGroupId) return;
          void cancelRecurringSeries(cancelSeriesGroupId)
            .unwrap()
            .then((response) => {
              setCancelSeriesGroupId(null);
              handleToast(
                `${response.cancelledCount} upcoming session${response.cancelledCount === 1 ? "" : "s"} cancelled.`,
                "success",
              );
              setSessionsPage(1);
              if (sessionsResponse?.items) {
                setClientSessions(sessionsResponse.items);
              }
              void refetchSessionsList();
              void refetchSummary();
            })
            .catch((error) => {
              handleToast(getApiErrorMessage(error), "error");
            });
        }}
        title="Cancel upcoming series?"
        description="This will cancel all upcoming scheduled or confirmed sessions in this recurring series. Past sessions are not affected."
        items={[]}
        confirmButtonText="Cancel upcoming sessions"
        confirmButtonLoading={isCancellingSeries}
        confirmButtonLoadingText="Cancelling..."
      />

      <ConfirmationModal
        type="delete"
        isOpen={deleteSessionId !== null}
        onClose={() => setDeleteSessionId(null)}
        onConfirm={() => {
          if (!deleteSessionId) return;
          void deleteSession(deleteSessionId)
            .unwrap()
            .then(() => {
              setDeleteSessionId(null);
              handleToast("Session deleted successfully.", "success");
              setSessionsPage(1);
              if (sessionsResponse?.items) {
                setClientSessions(sessionsResponse.items);
              }
              void refetchSessionsList();
              void refetchSummary();
              void refetchClientSessionNotes();
            })
            .catch((error) => {
              handleToast(getApiErrorMessage(error), "error");
            });
        }}
        title="Delete session?"
        description="Are you sure you want to delete this session? This action cannot be undone."
        items={[]}
        confirmButtonText="Delete session"
        confirmButtonLoading={isDeletingSession}
        confirmButtonLoadingText="Deleting..."
      />

      <ConfirmationModal
        type="close"
        isOpen={reopenNoteTarget !== null}
        onClose={() => setReopenNoteTarget(null)}
        onConfirm={() => {
          if (!reopenNoteTarget) return;
          void unfinalizeSessionNote(reopenNoteTarget.noteId)
            .unwrap()
            .then(() => {
              const matchedSession = sessions.find(
                (item) => Number(item.id) === reopenNoteTarget.sessionId,
              );
              setReopenNoteTarget(null);
              setViewNoteSessionId(reopenNoteTarget.sessionId);
              setViewNoteId(reopenNoteTarget.noteId);
              setViewNoteInitialEditMode(true);
              setViewNoteSessionData({
                sessionId: reopenNoteTarget.sessionId,
                clientId: matchedSession?.clientId ?? clientId,
                therapistId: matchedSession?.therapistId ?? null,
                dateIso: matchedSession?.sessionDateIso ?? null,
                dateTime: matchedSession?.dateTime ?? "",
                sessionType: matchedSession?.title ?? "Session",
                clientName: client.name,
              });
              handleToast("Session note reopened as draft.", "success");
              void refetchClientSessionNotes();
            })
            .catch((error) => {
              handleToast(getApiErrorMessage(error), "error");
            });
        }}
        title="Reopen session note?"
        description="This action can undo the last saved note and move it back into draft mode. Do you want to continue?"
        items={[]}
        confirmButtonText="Reopen note"
        confirmButtonLoading={isReopeningNote}
        confirmButtonLoadingText="Reopening..."
      />

      {transcriptNoteSessionData && transcriptNoteData ? (
        <AddSessionNoteModal
          isOpen={Boolean(transcriptNoteSessionData)}
          onClose={() => {
            setTranscriptNoteSessionData(null);
            setTranscriptNoteId(null);
            setTranscriptNoteData(null);
          }}
          sessionData={transcriptNoteSessionData}
          mode={transcriptNoteId ? "view" : "create"}
          noteId={transcriptNoteId}
          initialTranscriptionData={transcriptNoteData}
          autoOpenTranscriptReview={true}
          onToast={handleToast}
          hideRatingsTab
          onCreated={() => {
            void refetchClientSessionNotes();
            setTranscriptNoteSessionData(null);
            setTranscriptNoteId(null);
            setTranscriptNoteData(null);
          }}
          onChanged={() => {
            void refetchClientSessionNotes();
            setTranscriptNoteSessionData(null);
            setTranscriptNoteId(null);
            setTranscriptNoteData(null);
          }}
        />
      ) : null}

      {createBillingSessionId ? (
        <CreateSessionBillingModal
          isOpen={Boolean(createBillingSessionId)}
          onClose={() => setCreateBillingSessionId(null)}
          sessionId={createBillingSessionId}
          onCreated={() => {
            handleToast("Invoice created. View it in Billings.", "success");
            setCreateBillingSessionId(null);
          }}
        />
      ) : null}

      <PreviewInvoiceModal
        isOpen={previewInvoiceId !== null}
        onClose={() => {
          setPreviewInvoiceId(null);
          setPreviewHtml(null);
        }}
        invoice={
          previewInvoiceId
            ? ({
                id: String(previewInvoiceId),
                client: client.name,
                service: "Session",
                date: new Date().toISOString(),
                amount: "$0.00",
                status: "pending",
              })
            : null
        }
        htmlContent={previewHtml}
        isLoading={isPreviewing}
      />
      {payNowModal}
    </>
  );
};

export default SessionsTab;
