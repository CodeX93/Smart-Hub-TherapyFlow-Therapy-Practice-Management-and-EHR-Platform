import React from "react";
import { Clock, ShoppingBag, DoorOpen, Repeat, Video } from "lucide-react";
import type { Appointment, AppointmentStatus } from "../../types/scheduling";
import SessionActionsMenuHost from "@/components/sessions/SessionActionsMenuHost";
import { isRecurringSeriesSession } from "@/utils/recurringSessions";
import { isSessionEligibleForManualBilling } from "@/utils/sessionBillingUi";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import {
  isOnlineOrVirtualMode,
  shouldShowZoomMeetingJoin,
} from "@/utils/zoomMeeting";

interface AppointmentDetailsProps {
  appointment: Appointment;
  onEditClick?: () => void;
  onStatusChange: (statusKey: string) => void;
  currentStatus: AppointmentStatus;
  modalPosition: { left?: string; right?: string };
  isMobile?: boolean;
  isStatusUpdating?: boolean;
  hideEditActions?: boolean;
  onToast?: (message: string, type: "success" | "error" | "info") => void;
  onSeriesCancelled?: () => void;
  onCreateInvoice?: () => void;
  invoiceAlreadyCreated?: boolean;
}

function formatAppointmentDateTime(appointment: Appointment): string {
  if (appointment.dateTime?.trim()) return appointment.dateTime;
  const parts = [appointment.date, appointment.time].filter(Boolean);
  if (parts.length === 0) return "";
  if (appointment.date) {
    const parsed = new Date(appointment.date);
    if (!Number.isNaN(parsed.getTime())) {
      return parsed.toLocaleString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
      });
    }
  }
  return parts.join(" ");
}

export const AppointmentDetails: React.FC<AppointmentDetailsProps> = ({
  appointment,
  onEditClick,
  onStatusChange,
  currentStatus,
  isStatusUpdating,
  hideEditActions = false,
  onToast,
  onSeriesCancelled,
  onCreateInvoice,
  invoiceAlreadyCreated = false,
}) => {
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const showCreateInvoice =
    Boolean(onCreateInvoice) &&
    !invoiceAlreadyCreated &&
    isSessionEligibleForManualBilling({
      status: currentStatus,
      sessionDate: appointment.sessionDateIso ?? appointment.date,
      billingId: appointment.billingId,
    }, new Date(), practiceConfig?.timezone);

  const isVirtual = isOnlineOrVirtualMode(
    appointment.sessionMode,
    appointment.zoomEnabled,
  );
  const canJoinZoom = shouldShowZoomMeetingJoin({
    joinUrl: appointment.zoomJoinUrl,
    zoomEnabled: appointment.zoomEnabled,
    sessionMode: appointment.sessionMode,
    status: currentStatus,
  });
  const joinUrl = appointment.zoomJoinUrl?.trim() || "";

  const openZoomMeeting = () => {
    if (!joinUrl) return;
    window.open(joinUrl, "_blank", "noopener,noreferrer");
  };

  return (
    <div className="mt-2 w-full min-w-0 space-y-2">
      <div className="flex min-w-0 items-start gap-3 text-sm">
        <Clock className="h-4 w-4 shrink-0 text-(--text-neutral-400)" />
        <span className="w-16 shrink-0 text-(--text-neutral-600)">Session:</span>
        <span
          className="min-w-0 truncate font-bold text-(--text-primary-dark)"
          title={appointment.session}
        >
          {appointment.session}
        </span>
      </div>
      {isRecurringSeriesSession(appointment.recurrenceGroupId) ? (
        <div className="inline-flex w-fit items-center gap-1 rounded-full bg-indigo-50 px-2 py-1 text-xs font-medium text-indigo-600">
          <Repeat size={12} />
          Recurring series
        </div>
      ) : null}
      <div className="flex min-w-0 items-start gap-3 text-sm">
        <ShoppingBag className="h-4 w-4 shrink-0 text-(--text-neutral-400)" />
        <span className="w-16 shrink-0 text-(--text-neutral-600)">Service:</span>
        <span
          className="min-w-0 truncate font-bold text-(--text-primary-dark)"
          title={appointment.service}
        >
          {appointment.service}
        </span>
      </div>
      <div className="flex w-full min-w-0 flex-col items-stretch justify-between gap-2 text-sm md:flex-row md:items-center">
        {isVirtual ? (
          <div className="flex min-w-0 flex-1 items-center gap-3 text-sm">
            <Video className="h-4 w-4 shrink-0 text-(--text-neutral-400)" />
            <span className="shrink-0 text-(--text-neutral-600)">Zoom Meeting:</span>
            {canJoinZoom ? (
              <button
                type="button"
                onClick={openZoomMeeting}
                className="min-w-0 truncate font-bold text-(--text-primary-dark) underline decoration-(--neutral-200) underline-offset-4 transition-colors hover:text-(--status-billed)"
                title={joinUrl}
              >
                Join
              </button>
            ) : (
              <span className="min-w-0 truncate font-bold text-(--text-primary-dark)">
                —
              </span>
            )}
          </div>
        ) : (
          <div className="flex min-w-0 flex-1 items-start gap-3 text-sm">
            <DoorOpen className="h-4 w-4 shrink-0 text-(--text-neutral-400)" />
            <span className="w-16 shrink-0 text-(--text-neutral-600)">Room:</span>
            <span
              className="min-w-0 truncate font-bold text-(--text-primary-dark)"
              title={appointment.room}
            >
              {appointment.room}
            </span>
          </div>
        )}
        <div className="flex shrink-0 items-center gap-2 self-end">
          <SessionActionsMenuHost
            session={{
              id: appointment.id,
              status: currentStatus,
              clientName: appointment.name,
              sessionType: appointment.session,
              dateTime: formatAppointmentDateTime(appointment),
              dateIso: appointment.sessionDateIso ?? appointment.date,
              clientId: appointment.clientId,
              therapistId: appointment.therapistId,
              recurrenceGroupId: appointment.recurrenceGroupId,
              sessionMode: appointment.sessionMode,
              zoomEnabled: appointment.zoomEnabled,
              zoomJoinUrl: appointment.zoomJoinUrl,
              hasSubmittedNote: appointment.hasSubmittedNote,
              canRecordSession:
                appointment.canRecordSession !== false &&
                !appointment.hasSubmittedNote &&
                !appointment.hasTranscript,
              hasTranscript: Boolean(appointment.hasTranscript),
              billingId: appointment.billingId,
              remainingDue: appointment.remainingDue,
              invoicePaid: appointment.invoicePaid,
            }}
            hideEditAction={hideEditActions}
            showCreateInvoice={showCreateInvoice}
            isLoading={isStatusUpdating}
            onEdit={onEditClick}
            onStatusChange={onStatusChange}
            onCreateInvoice={onCreateInvoice}
            onToast={onToast}
            onSeriesCancelled={onSeriesCancelled}
          />
        </div>
      </div>
    </div>
  );
};
