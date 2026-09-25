import type { PortalAppointment } from "@/store/api/portalApi";
import {
  formatPortalSlotStartTime,
  getSessionModalityLabel,
} from "@/utils/portalSessionDisplay";
import {
  getSessionStatusLabel,
  normalizeSessionStatus,
} from "@/utils/sessionStatusPresentation";

export function formatBookedSessionDate(sessionDate: string): string {
  const parsed = new Date(`${sessionDate}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) return sessionDate || "—";
  return parsed.toLocaleDateString(undefined, {
    weekday: "short",
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export function formatBookedSessionTime(
  sessionDate: string,
  sessionTime: string,
): string {
  if (!sessionTime) return "—";
  return formatPortalSlotStartTime(sessionDate, sessionTime);
}

export function formatBookedSessionRate(rate?: number | null): string {
  if (rate === null || rate === undefined || Number.isNaN(rate)) return "—";
  return new Intl.NumberFormat(undefined, {
    style: "currency",
    currency: "USD",
  }).format(rate);
}

export function normalizeBookedSessionStatus(status?: string | null): string {
  return normalizeSessionStatus(status);
}

export function isCompletedBookedSession(status?: string | null): boolean {
  return normalizeBookedSessionStatus(status) === "completed";
}

export function hasSubmittedSessionRating(
  session: Pick<PortalAppointment, "clientRating">,
): boolean {
  return (
    session.clientRating !== null &&
    session.clientRating !== undefined &&
    Number.isFinite(session.clientRating)
  );
}

export function filterBookedSessions(
  sessions: PortalAppointment[],
  searchQuery: string,
  statusFilter: string,
): PortalAppointment[] {
  const normalizedSearch = searchQuery.trim().toLowerCase();
  const normalizedStatus = statusFilter.trim()
    ? normalizeSessionStatus(statusFilter)
    : "";

  return sessions.filter((session) => {
    const matchesStatus =
      !normalizedStatus ||
      normalizeBookedSessionStatus(session.status) === normalizedStatus;

    if (!matchesStatus) return false;
    if (!normalizedSearch) return true;

    const haystack = [
      session.serviceName,
      session.serviceCode,
      session.sessionType,
      session.therapistName,
      session.location,
      session.roomName,
      session.referenceNumber,
      getSessionStatusLabel(session.status),
      getSessionModalityLabel(session.sessionMode),
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();

    return haystack.includes(normalizedSearch);
  });
}

export const BOOKED_SESSION_STATUS_OPTIONS = [
  { value: "", label: "All statuses" },
  { value: "scheduled", label: getSessionStatusLabel("scheduled") },
  { value: "confirmed", label: getSessionStatusLabel("confirmed") },
  { value: "in-progress", label: getSessionStatusLabel("in_progress") },
  { value: "completed", label: getSessionStatusLabel("completed") },
  { value: "cancelled", label: getSessionStatusLabel("cancelled") },
  { value: "rescheduling", label: getSessionStatusLabel("rescheduled") },
  { value: "no-show", label: getSessionStatusLabel("noshow") },
  { value: "overdue", label: getSessionStatusLabel("overdue") },
] as const;
