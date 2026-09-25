import { useState } from "react";
import SessionActionsMenu from "@/components/shared/SessionActionsMenu";
import RecordSessionModal from "@/components/sessions/recording/RecordSessionModal";
import SessionTranscriptModal from "@/components/sessions/recording/SessionTranscriptModal";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import PreviewInvoiceModal from "@/components/billing-sections/PreviewInvoiceModal";
import Toast from "@/components/shared/Toast";
import {
  buildStoredSessionTranscript,
  getStoredSessionTranscript,
  removeStoredSessionTranscript,
  saveStoredSessionTranscript,
  type StoredSessionTranscript,
} from "@/components/sessions/recording/sessionTranscriptStore";
import { useCancelRecurringSeriesMutation } from "@/store/api/admin/recurringSessions.api";
import {
  useDeleteSessionTranscriptMutation,
  useDiarizeSessionTranscriptMutation,
  useLazyDownloadSessionTranscriptQuery,
  useLazyGetSessionTranscriptQuery,
} from "@/store/api/admin/sessionTranscripts.api";
import { useLazyPreviewInvoiceQuery } from "@/store/api/admin/billing.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { isVirtualSession } from "@/utils/portalSessionDisplay";
import { canShowSessionPayNow } from "@/utils/sessionBillingUi";
import { useSessionCompletedPayNow } from "@/hooks/useSessionCompletedPayNow";

export type SessionActionsHostSession = {
  id: string;
  status: string;
  clientName: string;
  sessionType: string;
  dateTime: string;
  dateIso?: string | null;
  clientId?: number | null;
  therapistId?: number | null;
  recurrenceGroupId?: string | null;
  sessionMode?: string | null;
  zoomEnabled?: boolean;
  zoomJoinUrl?: string | null;
  hasSubmittedNote?: boolean;
  canRecordSession?: boolean;
  billingId?: number | null;
  remainingDue?: number | null;
  invoicePaid?: boolean;
  hasTranscript?: boolean;
};

type ToastState = {
  message: string;
  type: "success" | "error" | "info";
} | null;

interface SessionActionsMenuHostProps {
  session: SessionActionsHostSession;
  hideEditAction?: boolean;
  hideStatusActions?: boolean;
  showCreateInvoice?: boolean;
  isLoading?: boolean;
  onEdit?: () => void;
  onStatusChange?: (statusKey: string) => void;
  onCreateInvoice?: () => void;
  onSeriesCancelled?: () => void;
  onToast?: (message: string, type: "success" | "error" | "info") => void;
  onPaymentRecorded?: () => void;
}

const SessionActionsMenuHost = ({
  session,
  hideEditAction = false,
  hideStatusActions = false,
  showCreateInvoice = false,
  isLoading = false,
  onEdit,
  onStatusChange,
  onCreateInvoice,
  onSeriesCancelled,
  onToast,
  onPaymentRecorded,
}: SessionActionsMenuHostProps) => {
  const hasTranscript = Boolean(session.hasTranscript);
  const hasSubmittedNote = Boolean(session.hasSubmittedNote);
  const canRecordSession =
    session.canRecordSession !== false && !hasSubmittedNote && !hasTranscript;

  const [localToast, setLocalToast] = useState<ToastState>(null);
  const [recordSessionData, setRecordSessionData] = useState<{
    sessionId: number;
    clientId: number | null;
    therapistId: number | null;
    dateTime: string;
    sessionType: string;
    clientName: string;
  } | null>(null);
  const [activeTranscriptSessionId, setActiveTranscriptSessionId] = useState<number | null>(
    null,
  );
  const [activeTranscript, setActiveTranscript] = useState<StoredSessionTranscript | null>(
    null,
  );
  const [cancelSeriesGroupId, setCancelSeriesGroupId] = useState<string | null>(null);

  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);
  const [triggerPreview, { isFetching: isPreviewing }] = useLazyPreviewInvoiceQuery();

  const [cancelRecurringSeries, { isLoading: isCancellingSeries }] =
    useCancelRecurringSeriesMutation();
  const [deleteSessionTranscript, { isLoading: isDeletingTranscript }] =
    useDeleteSessionTranscriptMutation();
  const [diarizeSessionTranscript, { isLoading: isDiarizingTranscript }] =
    useDiarizeSessionTranscriptMutation();
  const [triggerGetSessionTranscript, { isFetching: isTranscriptLoading }] =
    useLazyGetSessionTranscriptQuery();
  const [triggerDownloadSessionTranscript, { isFetching: isTranscriptDownloading }] =
    useLazyDownloadSessionTranscriptQuery();

  const emitToast = (message: string, type: "success" | "error" | "info" = "info") => {
    if (onToast) {
      onToast(message, type);
      return;
    }
    setLocalToast({ message, type });
  };

  const [locallyPaid, setLocallyPaid] = useState(false);
  const { openPayNowForSession, payNowModal } = useSessionCompletedPayNow({
    onToast: emitToast,
    onPaymentRecorded: () => {
      setLocallyPaid(true);
      onPaymentRecorded?.();
    },
  });

  const showPayNow =
    !locallyPaid &&
    canShowSessionPayNow({
      status: session.status,
      billingId: session.billingId,
      remainingDue: session.remainingDue,
      invoicePaid: session.invoicePaid,
    });

  const handleCloseTranscriptModal = () => {
    setActiveTranscriptSessionId(null);
    setActiveTranscript(null);
  };

  const handleAction = (action: string) => {
    const sessionId = Number.parseInt(session.id, 10);

    if (action === "edit") {
      onEdit?.();
      return;
    }

    if (action === "join") {
      const url = session.zoomJoinUrl?.trim();
      if (url) {
        window.open(url, "_blank", "noopener,noreferrer");
      } else {
        emitToast("Zoom meeting link is not available for this session.", "error");
      }
      return;
    }

    if (action === "create-invoice") {
      onCreateInvoice?.();
      return;
    }

    if (action === "pay-now") {
      if (!Number.isFinite(sessionId)) return;
      void openPayNowForSession(sessionId, { notifyIfUnpayable: true });
      return;
    }

    if (action === "record-session") {
      if (!Number.isFinite(sessionId)) return;
      if (hasSubmittedNote) {
        emitToast("Finalized session notes cannot be recorded again.", "error");
        return;
      }
      if (hasTranscript) {
        emitToast("A transcript already exists for this session.", "error");
        return;
      }
      if (!canRecordSession) {
        return;
      }
      setRecordSessionData({
        sessionId,
        clientId: session.clientId ?? null,
        therapistId: session.therapistId ?? null,
        dateTime: session.dateTime,
        sessionType: session.sessionType,
        clientName: session.clientName,
      });
      return;
    }

    if (action === "view-transcript") {
      if (!Number.isFinite(sessionId)) return;
      const cachedTranscript = getStoredSessionTranscript(sessionId);
      setActiveTranscriptSessionId(sessionId);
      setActiveTranscript(
        cachedTranscript?.rawTranscription.trim() ? cachedTranscript : null,
      );
      void triggerGetSessionTranscript(sessionId)
        .unwrap()
        .then((response) => {
          const finalTranscript =
            response.content?.trim() || response.finalTranscript?.trim() || "";
          if (!finalTranscript) {
            removeStoredSessionTranscript(sessionId);
            setActiveTranscript(null);
            return;
          }

          const transcript = buildStoredSessionTranscript({
            transcriptId: response.transcriptId,
            sessionId: response.sessionId,
            uploadId: response.uploadId,
            clientId: response.clientId ?? session.clientId ?? null,
            therapistId: session.therapistId ?? null,
            clientName: response.clientName || session.clientName,
            sessionType: session.sessionType,
            sessionDateTime: session.dateTime,
            durationSeconds: response.durationSeconds ?? 0,
            status: response.status,
            expectedChunks: response.expectedChunks,
            receivedChunks: response.receivedChunks,
            rawTranscription: finalTranscript,
            diarizedTranscript: response.diarizedTranscript ?? null,
            wordCount: response.wordCount ?? undefined,
            mappedFields: {},
            finalizedAt: response.finalizedAt,
            failureReason: response.failureReason,
          });
          saveStoredSessionTranscript(transcript);
          setActiveTranscript(transcript);
        })
        .catch((error) => {
          emitToast(getApiErrorMessage(error), "error");
          setActiveTranscript(null);
        });
      return;
    }

    if (action === "preview-invoice") {
      if (!session.billingId) return;
      setIsPreviewOpen(true);
      triggerPreview(session.billingId)
        .unwrap()
        .then((response) => setPreviewHtml(response.html))
        .catch((error) => {
          emitToast(getApiErrorMessage(error), "error");
          setIsPreviewOpen(false);
        });
      return;
    }

    if (action === "cancel-series") {
      if (session.recurrenceGroupId) {
        setCancelSeriesGroupId(session.recurrenceGroupId);
      }
      return;
    }

    onStatusChange?.(action);
  };

  return (
    <>
      <SessionActionsMenu
        onAction={handleAction}
        currentStatus={session.status}
        recurrenceGroupId={session.recurrenceGroupId}
        hasSubmittedNote={hasSubmittedNote}
        canRecordSession={canRecordSession}
        isVirtualSession={isVirtualSession(session)}
        hideEditAction={hideEditAction}
        hideStatusActions={hideStatusActions}
        showCreateInvoice={showCreateInvoice}
        showPayNow={showPayNow}
        hasInvoice={Boolean(session.billingId)}
        scheduledAt={session.dateIso ?? null}
        hasTranscript={hasTranscript}
        isLoading={isLoading}
      />

      <RecordSessionModal
        isOpen={recordSessionData !== null}
        onClose={() => setRecordSessionData(null)}
        sessionData={recordSessionData}
        onToast={emitToast}
        onTranscriptSaved={() => {
          setRecordSessionData(null);
          emitToast("Session transcript saved successfully.", "success");
          window.setTimeout(() => {
            handleAction("view-transcript");
          }, 350);
        }}
      />

      <SessionTranscriptModal
        isOpen={activeTranscriptSessionId !== null}
        onClose={handleCloseTranscriptModal}
        transcript={activeTranscript}
        isLoading={isTranscriptLoading}
        onSmartFill={() => {
          emitToast(
            "Open this session from the client profile to smart-fill notes.",
            "info",
          );
        }}
        onIdentifySpeakers={() => {
          if (!activeTranscript) return;
          if (activeTranscript.status.toLowerCase() !== "ready") {
            emitToast("Identify speakers is available after the transcript is ready.", "info");
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
                emitToast("Speaker identification returned no content. Please try again.", "error");
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
              emitToast("Speakers identified successfully.", "success");
            })
            .catch((error) => {
              emitToast(getApiErrorMessage(error), "error");
            });
        }}
        onDownload={() => {
          if (!activeTranscriptSessionId) return;
          void triggerDownloadSessionTranscript(activeTranscriptSessionId)
            .unwrap()
            .then((content) => {
              const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
              const url = window.URL.createObjectURL(blob);
              const anchor = document.createElement("a");
              anchor.href = url;
              const safeName =
                session.clientName.toLowerCase().replace(/[^a-z0-9]+/g, "-") || "session";
              anchor.download = `${safeName}-transcript.txt`;
              anchor.click();
              window.URL.revokeObjectURL(url);
              emitToast("Transcript downloaded successfully.", "success");
            })
            .catch((error) => {
              emitToast(getApiErrorMessage(error), "error");
            });
        }}
        onDelete={() => {
          if (!activeTranscriptSessionId) return;
          void deleteSessionTranscript(activeTranscriptSessionId)
            .unwrap()
            .then(() => {
              removeStoredSessionTranscript(activeTranscriptSessionId);
              emitToast("Transcript deleted successfully.", "success");
              handleCloseTranscriptModal();
            })
            .catch((error) => {
              emitToast(getApiErrorMessage(error), "error");
            });
        }}
        isDownloadLoading={isTranscriptDownloading}
        isDeleteLoading={isDeletingTranscript}
        isDiarizeLoading={isDiarizingTranscript}
      />

      <PreviewInvoiceModal
        isOpen={isPreviewOpen}
        onClose={() => {
          setIsPreviewOpen(false);
          setPreviewHtml(null);
        }}
        invoice={
          session.billingId
            ? ({
                id: String(session.billingId),
                client: session.clientName,
                service: session.sessionType,
                date: session.dateTime,
                amount: "$0.00",
                status: "pending",
              })
            : null
        }
        htmlContent={previewHtml}
        isLoading={isPreviewing}
      />

      <ConfirmationModal
        type="close"
        isOpen={cancelSeriesGroupId !== null}
        onClose={() => setCancelSeriesGroupId(null)}
        onConfirm={() => {
          if (!cancelSeriesGroupId) return;
          void cancelRecurringSeries(cancelSeriesGroupId)
            .unwrap()
            .then(() => {
              setCancelSeriesGroupId(null);
              emitToast("Upcoming series sessions cancelled successfully.", "success");
              onSeriesCancelled?.();
            })
            .catch((error) => {
              emitToast(getApiErrorMessage(error), "error");
            });
        }}
        title="Cancel upcoming series?"
        description="This will cancel all upcoming scheduled or confirmed sessions in this recurring series. Past sessions are not affected."
        items={[]}
        confirmButtonText="Cancel upcoming sessions"
        confirmButtonLoading={isCancellingSeries}
        confirmButtonLoadingText="Cancelling..."
      />

      {localToast ? (
        <Toast
          message={localToast.message}
          type={localToast.type}
          onClose={() => setLocalToast(null)}
        />
      ) : null}
      {payNowModal}
    </>
  );
};

export default SessionActionsMenuHost;
