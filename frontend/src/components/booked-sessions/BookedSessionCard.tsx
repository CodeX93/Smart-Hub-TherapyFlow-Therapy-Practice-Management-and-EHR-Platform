import { CalendarIcon } from "@/components/icons/commonIcons";
import { ChevronRight, Clock, MapPin, UserRound } from "lucide-react";
import type { PortalAppointment } from "@/store/api/portalApi";
import SessionStatusBadge from "@/components/shared/SessionStatusBadge";
import {
  formatBookedSessionDate,
  formatBookedSessionRate,
  formatBookedSessionTime,
  hasSubmittedSessionRating,
  isCompletedBookedSession,
} from "@/utils/bookedSessionDisplay";
import {
  getSessionModalityBadgeClass,
  getSessionModalityLabel,
} from "@/utils/portalSessionDisplay";
import { cn } from "@/lib/utils";

interface BookedSessionCardProps {
  session: PortalAppointment;
  onOpen: (sessionId: number) => void;
  onRate?: (session: PortalAppointment) => void;
}

const BookedSessionCard = ({ session, onOpen, onRate }: BookedSessionCardProps) => {
  const modalityLabel = getSessionModalityLabel(session.sessionMode);
  const canRate = isCompletedBookedSession(session.status) && onRate;
  const hasRating = hasSubmittedSessionRating(session);

  return (
    <div className="rounded-xl border border-(--neutral-100) bg-white p-4 shadow-xs">
      <button
        type="button"
        onClick={() => onOpen(session.id)}
        className="flex w-full items-start gap-3 text-left"
      >
        <div className="flex h-fit shrink-0 items-center justify-center rounded-[0.625rem] bg-[#F3F7F8] p-2">
          <CalendarIcon className="h-5 w-5 text-(--text-primary-dark)" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="truncate font-semibold text-(--text-primary-dark)">
                {session.serviceName || session.sessionType || "Therapy Session"}
              </p>
              <p className="mt-0.5 truncate text-sm text-(--text-neutral-600)">
                {formatBookedSessionDate(session.sessionDate)}
              </p>
            </div>
            <ChevronRight className="mt-0.5 h-5 w-5 shrink-0 text-(--text-neutral-400)" />
          </div>

          <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-(--text-neutral-600)">
            <span className="inline-flex items-center gap-1">
              <Clock className="h-4 w-4" />
              {formatBookedSessionTime(session.sessionDate, session.sessionTime)}
              {session.duration ? ` · ${session.duration} min` : ""}
            </span>
            {session.therapistName ? (
              <span className="inline-flex min-w-0 items-center gap-1">
                <UserRound className="h-4 w-4 shrink-0" />
                <span className="truncate">{session.therapistName}</span>
              </span>
            ) : null}
            {session.location ? (
              <span className="inline-flex min-w-0 items-center gap-1">
                <MapPin className="h-4 w-4 shrink-0" />
                <span className="truncate">
                  {session.location}
                  {session.roomName ? ` · ${session.roomName}` : ""}
                </span>
              </span>
            ) : null}
          </div>

          <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
            <div className="flex flex-wrap gap-2">
              <SessionStatusBadge status={session.status} />
              <span
                className={cn(
                  "rounded-full px-2.5 py-0.5 text-xs font-medium",
                  getSessionModalityBadgeClass(session.sessionMode),
                )}
              >
                {modalityLabel}
              </span>
            </div>
            <span className="text-sm font-semibold text-(--text-primary-dark)">
              {formatBookedSessionRate(session.serviceRate)}
            </span>
          </div>
        </div>
      </button>

      {canRate ? (
        <div className="mt-3 flex justify-end border-t border-(--neutral-100) pt-3">
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              onRate(session);
            }}
            className={
              hasRating
                ? "rounded-full border border-(--neutral-200) bg-white px-4 py-2 text-sm font-semibold text-(--text-primary-dark) transition hover:bg-(--bg-primary-light)"
                : "rounded-full bg-(--bg-primary-dark) px-4 py-2 text-sm font-semibold text-white transition hover:bg-(--bg-primary-dark)/90"
            }
          >
            {hasRating ? "View rating" : "Rate session"}
          </button>
        </div>
      ) : null}
    </div>
  );
};

export default BookedSessionCard;
