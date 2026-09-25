
import { ContentLoader } from "@/components/shared/ContentLoader";
import { MoveDown, Repeat } from "lucide-react";
import type { Appointment } from "../../types/scheduling";
import type { AdminDashboardSession } from "@/store/api/admin/dashboard.api";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { SCHEDULING_STATIC_CONTENT } from "../../pages/therapist/therapist.static";
import { normalizeAppointmentStatus } from "@/utils/sessionStatusTransitions";
import { isRecurringSeriesSession } from "@/utils/recurringSessions";
import { isSessionEligibleForManualBilling } from "@/utils/sessionBillingUi";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import { isOnlineOrVirtualMode } from "@/utils/zoomMeeting";
import SessionStatusBadge from "@/components/shared/SessionStatusBadge";
import SessionActionsMenuHost from "@/components/sessions/SessionActionsMenuHost";

function capitalizeFirstLetter(value?: string | null): string {
  const trimmed = (value || "").trim();
  if (!trimmed) return "";
  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1);
}

interface AllSessionCardsProps {
  handleEditScheduleClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  onStatusChange?: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  updatingSessionId?: string | null;
  isAdmin?: boolean;
  sessions?: AdminDashboardSession[];
  hasMore?: boolean;
  isLoading?: boolean;
  isLoadingMore?: boolean;
  onLoadMore?: () => void;
  onCreateInvoice?: (sessionId: number) => void;
  billedSessionIds?: ReadonlySet<number>;
}

const AllSessionsTable = ({
  handleEditScheduleClick,
  hideEditActions = false,
  onStatusChange,
  isStatusUpdating = false,
  updatingSessionId = null,
  isAdmin,
  sessions: providedSessions,
  hasMore: providedHasMore,
  isLoading = false,
  isLoadingMore = false,
  onLoadMore,
  onCreateInvoice,
  billedSessionIds,
}: AllSessionCardsProps) => {
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const fallbackSessions: AdminDashboardSession[] = SCHEDULING_STATIC_CONTENT.allSessionsData.map(
    (session) => ({
      id: Number.parseInt(session.id, 10),
      clientId: undefined,
      clientName: session.clientName,
      therapistId: undefined,
      therapistName: session.therapist || "",
      sessionDate: `${session.date} ${session.time}`,
      duration: session.duration ? Number.parseInt(session.duration, 10) : undefined,
      sessionType: session.sessionType,
      status: session.status,
      serviceId: undefined,
      serviceName: session.service,
      roomId: undefined,
      roomName: session.room,
      notes: undefined,
      zoomEnabled: false,
    }),
  );

  const sessions = providedSessions ?? fallbackSessions;
  const hasMore = providedHasMore ?? false;

  const mapSessionToAppointment = (session: AdminDashboardSession): Appointment => {
    const sessionDate = session.sessionDate ? new Date(session.sessionDate) : null;
    const dateLabel =
      sessionDate && !Number.isNaN(sessionDate.getTime())
        ? sessionDate.toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric",
          })
        : "---";
    const timeLabel =
      sessionDate && !Number.isNaN(sessionDate.getTime())
        ? sessionDate.toLocaleTimeString("en-US", {
            hour: "2-digit",
            minute: "2-digit",
          })
        : "---";

    return {
      id: String(session.id),
      name: session.clientName,
      time: timeLabel,
      startHour: 9, // Default/Placeholder as before
      duration: 1, // Default/Placeholder
      status: normalizeAppointmentStatus(session.status),
      session: capitalizeFirstLetter(session.sessionType) || "",
      service: session.serviceName || "",
      room: isOnlineOrVirtualMode(session.sessionMode, session.zoomEnabled)
        ? "Online"
        : session.roomName || "",
      date: dateLabel,
      dateTime: `${dateLabel}, ${timeLabel}`,
      recurrenceGroupId: session.recurrenceGroupId,
      clientId: session.clientId,
      therapistId: session.therapistId,
      therapistName: session.therapistName || undefined,
      sessionMode: session.sessionMode,
      zoomEnabled: session.zoomEnabled,
      zoomJoinUrl: session.zoomJoinUrl,
      zoomPassword: session.zoomPassword,
      sessionDateIso: session.sessionDate,
      canRecordSession: !session.hasTranscript,
      hasSubmittedNote: false,
      hasTranscript: Boolean(session.hasTranscript),
    };
  };

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: onLoadMore ?? (() => undefined),
    hasMore,
    isLoading: isLoadingMore,
  });

  const cellTextClass =
    "text-[0.875rem] text-(--text-primary-dark) break-words [overflow-wrap:anywhere] line-clamp-2";
  const subTextClass =
    "text-[0.75rem] text-(--text-neutral-500) break-words [overflow-wrap:anywhere] line-clamp-1";

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-(--neutral-100) bg-white shadow-sm">
        <div className="min-h-0 flex-1 overflow-auto overscroll-contain">
          <table className="w-full min-w-[60rem] table-fixed">
            <colgroup>
              <col className={isAdmin ? "w-[14%]" : "w-[16%]"} />
              <col className={isAdmin ? "w-[13%]" : "w-[14%]"} />
              <col className={isAdmin ? "w-[18%]" : "w-[20%]"} />
              <col className={isAdmin ? "w-[12%]" : "w-[14%]"} />
              {isAdmin ? <col className="w-[13%]" /> : null}
              <col className={isAdmin ? "w-[12%]" : "w-[14%]"} />
              <col className={isAdmin ? "w-[10%]" : "w-[12%]"} />
              <col className={isAdmin ? "w-[8%]" : "w-[10%]"} />
            </colgroup>
            <thead className="sticky top-0 z-10 bg-(--bg-primary-50)">
              <tr className="border-b border-(--neutral-100)">
                {/* Client Column with Sort */}
                <th className="px-6 py-4 text-left align-top">
                  <div className="flex items-center gap-2 cursor-pointer group w-fit">
                    <span className="text-sm font-semibold text-(--text-primary-dark)">
                      Client
                    </span>
                    <MoveDown
                      size={14}
                      className="text-(--text-neutral-600) group-hover:text-(--text-neutral-600)"
                    />
                  </div>
                </th>

                {/* Date & Time */}
                <th className="px-6 py-4 text-left align-top text-sm font-semibold text-(--text-primary-dark)">
                  Date & Time
                </th>

                {/* Service */}
                <th className="px-6 py-4 text-left align-top text-sm font-semibold text-(--text-primary-dark)">
                  Service
                </th>

                {/* Session Type */}
                <th className="px-6 py-4 text-left align-top text-sm font-semibold text-(--text-primary-dark)">
                  Session Type
                </th>

                {/* Therapist */}
                {isAdmin && (
                  <th className="px-6 py-4 text-left align-top text-sm font-semibold text-(--text-primary-dark)">
                    Therapist
                  </th>
                )}

                {/* Room */}
                <th className="px-6 py-4 text-left align-top text-sm font-semibold text-(--text-primary-dark)">
                  Room
                </th>

                {/* Status */}
                <th className="px-6 py-4 text-left align-top text-sm font-semibold text-(--text-primary-dark)">
                  Status
                </th>

                {/* Actions */}
                <th className="px-6 py-4 text-right align-top text-sm font-semibold text-(--text-primary-dark)">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={isAdmin ? 8 : 7} className="px-6 py-12 text-center">
                    <div className="flex flex-col items-center justify-center text-(--text-neutral-500)">
                      <ContentLoader size="lg" className="mb-2" />
                      <span className="text-sm">Loading sessions...</span>
                    </div>
                  </td>
                </tr>
              ) : sessions.length === 0 ? (
                <tr>
                  <td colSpan={isAdmin ? 8 : 7} className="px-6 py-12 text-center">
                    <span className="text-sm text-(--text-neutral-500)">No sessions found.</span>
                  </td>
                </tr>
              ) : (
                sessions.map((session) => {
                  const sessionDate = session.sessionDate
                  ? new Date(session.sessionDate)
                  : null;
                const dateLabel =
                  sessionDate && !Number.isNaN(sessionDate.getTime())
                    ? sessionDate.toLocaleDateString("en-US", {
                        month: "short",
                        day: "numeric",
                        year: "numeric",
                      })
                    : "---";
                const timeLabel =
                  sessionDate && !Number.isNaN(sessionDate.getTime())
                    ? sessionDate.toLocaleTimeString("en-US", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })
                    : "---";

                const serviceLabel = `${session.serviceName || capitalizeFirstLetter(session.sessionType) || "---"}${session.duration ? ` ${session.duration}` : ""}`;
                const serviceMeta = session.serviceId ? `Service #${session.serviceId}` : "---";
                const sessionTypeLabel = capitalizeFirstLetter(session.sessionType) || "---";
                const isOnlineSession = isOnlineOrVirtualMode(
                  session.sessionMode,
                  session.zoomEnabled,
                );
                const roomLabel = isOnlineSession
                  ? "Online"
                  : session.roomName?.trim() || "---";
                const showCreateInvoice =
                  Boolean(onCreateInvoice) &&
                  isSessionEligibleForManualBilling({
                    status: session.status,
                    sessionDate: session.sessionDate,
                    billingId: session.billingId,
                    hasInvoice: billedSessionIds?.has(session.id),
                  }, new Date(), practiceConfig?.timezone);
                const appointment = mapSessionToAppointment(session);

                return (
                <tr
                  key={session.id}
                  className="group border-b border-(--neutral-50) hover:bg-(--neutral-50) transition-colors"
                >
                  {/* Client */}
                  <td className="px-6 py-4 align-top">
                    <div className="flex min-w-0 flex-col gap-0.5">
                      <span
                        className={`${cellTextClass} font-semibold border-b border-transparent hover:border-(--text-primary-dark) cursor-pointer transition-all`}
                        title={session.clientName}
                      >
                        {session.clientName}
                      </span>
                      <span className={subTextClass} title={`Ref# ${session.clientId ?? "---"}`}>
                        Ref# {session.clientId ?? "---"}
                      </span>
                      {isRecurringSeriesSession(session.recurrenceGroupId) ? (
                        <span className="inline-flex w-fit items-center gap-1 rounded-full bg-indigo-50 px-2 py-0.5 text-[0.6875rem] font-medium text-indigo-600">
                          <Repeat size={10} />
                          Recurring
                        </span>
                      ) : null}
                    </div>
                  </td>

                  {/* Date & Time */}
                  <td className="px-6 py-4 align-top">
                    <div className="flex min-w-0 flex-col gap-0.5">
                      <span className={cellTextClass} title={dateLabel}>
                        {dateLabel}
                      </span>
                      <span className={subTextClass} title={timeLabel}>
                        {timeLabel}
                      </span>
                    </div>
                  </td>

                  {/* Service */}
                  <td className="px-6 py-4 align-top">
                    <div className="flex min-w-0 flex-col gap-0.5">
                      <span className={cellTextClass} title={serviceLabel}>
                        {serviceLabel}
                      </span>
                      <span className={subTextClass} title={serviceMeta}>
                        {serviceMeta}
                      </span>
                    </div>
                  </td>

                  {/* Session Type */}
                  <td className="px-6 py-4 align-top">
                    <span className={cellTextClass} title={sessionTypeLabel}>
                      {sessionTypeLabel}
                    </span>
                  </td>

                  {/* Therapist (Mocked) */}
                  {isAdmin && (
                    <td className="px-6 py-4 align-top">
                      <span
                        className={cellTextClass}
                        title={session.therapistName || "---"}
                      >
                        {session.therapistName || "---"}
                      </span>
                    </td>
                  )}

                  {/* Room */}
                  <td className="px-6 py-4 align-top">
                    {isOnlineSession ? (
                      <span
                        className="inline-flex w-fit items-center rounded-full bg-(--neutral-100) px-2.5 py-0.5 text-[0.75rem] font-medium text-(--text-primary-dark)"
                        title="Online"
                      >
                        Online
                      </span>
                    ) : (
                      <span className={cellTextClass} title={roomLabel}>
                        {roomLabel}
                      </span>
                    )}
                  </td>

                  {/* Status */}
                  <td className="px-6 py-4 align-top">
                    <SessionStatusBadge
                      status={session.status}
                      className="whitespace-nowrap py-1"
                    />
                  </td>

                  {/* Actions */}
                  <td className="px-5 py-4 align-top">
                    <div className="flex items-start justify-end">
                      <SessionActionsMenuHost
                          session={{
                            id: String(session.id),
                            status: session.status,
                            clientName: session.clientName,
                            sessionType: session.sessionType || "Session",
                            dateTime: appointment.dateTime || `${dateLabel}, ${timeLabel}`,
                            dateIso: session.sessionDate,
                            clientId: session.clientId,
                            therapistId: session.therapistId,
                            recurrenceGroupId: session.recurrenceGroupId,
                            sessionMode: session.sessionMode,
                            zoomEnabled: session.zoomEnabled,
                            zoomJoinUrl: session.zoomJoinUrl,
                            canRecordSession: !session.hasTranscript,
                            hasSubmittedNote: false,
                            billingId: session.billingId,
                            remainingDue: session.remainingDue,
                            invoicePaid: session.invoicePaid,
                            hasTranscript: Boolean(session.hasTranscript),
                          }}
                          hideEditAction={hideEditActions}
                          hideStatusActions={!onStatusChange}
                          showCreateInvoice={showCreateInvoice}
                          isLoading={
                            Boolean(isStatusUpdating) &&
                            updatingSessionId === String(session.id)
                          }
                          onEdit={() => handleEditScheduleClick?.(appointment)}
                          onStatusChange={(statusKey) =>
                            onStatusChange?.(String(session.id), statusKey)
                          }
                          onCreateInvoice={() => onCreateInvoice?.(session.id)}
                        />
                    </div>
                  </td>
                </tr>
                );
              }))}
            </tbody>
          </table>

          <div
            ref={observerTarget}
            className="flex h-10 w-full items-center justify-center"
          >
            {isLoadingMore && (
              <ContentLoader variant="inline" size="md" />
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AllSessionsTable;
