import type { Appointment } from "../../types/scheduling";
import type {
  AdminDashboardSession,
  AdminSessionsListParams,
} from "@/store/api/admin/dashboard.api";
import type { SchedulingFilters } from "../scheduling-sections/SchedulingFilterDropdown";
import { normalizeAppointmentStatus } from "@/utils/sessionStatusTransitions";
import { isOnlineOrVirtualMode } from "@/utils/zoomMeeting";
import { DateTime } from "luxon";
import {
  resolveScheduleTimezone,
  toIsoRangeEndInTimezone,
  toIsoRangeStartInTimezone,
} from "@/utils/scheduleTimezone";

function capitalizeFirstLetter(value?: string | null): string {
  const trimmed = (value || "").trim();
  if (!trimmed) return "";
  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1);
}

function normalizeStatus(status: string): Appointment["status"] {
  return normalizeAppointmentStatus(status);
}

export function buildSessionsQueryArgs({
  pageSize = 500,
  rangeStart,
  rangeEnd,
  filters,
  searchQuery,
  mySessionsOnly,
  timezone,
  view = "calendar",
}: {
  pageSize?: number;
  rangeStart?: Date;
  rangeEnd?: Date;
  filters: SchedulingFilters;
  searchQuery: string;
  mySessionsOnly: boolean;
  timezone?: string | null;
  view?: "summary" | "calendar";
}): AdminSessionsListParams {
  const params: AdminSessionsListParams = {
    page: 1,
    pageSize,
    mySessionsOnly,
    includeHiddenServices: Boolean(filters.includeHiddenServices),
    view,
  };

  const effectiveStartDate = filters.startDate ?? rangeStart;
  const effectiveEndDate = filters.endDate ?? rangeEnd;
  if (effectiveStartDate) {
    params.startDate = toIsoRangeStartInTimezone(effectiveStartDate, timezone);
  }
  if (effectiveEndDate) {
    params.endDate = toIsoRangeEndInTimezone(effectiveEndDate, timezone);
  }

  if (searchQuery.trim()) params.clientSearch = searchQuery.trim();
  if (filters.status) {
    params.status = filters.status === "noshow" ? "no-show" : filters.status;
  }
  if (filters.serviceCode) params.serviceCode = filters.serviceCode;
  if (filters.therapist) {
    const therapistId = Number.parseInt(filters.therapist, 10);
    if (Number.isFinite(therapistId)) params.therapistId = therapistId;
  }

  return params;
}

export function mapSessionToAppointment(
  session: AdminDashboardSession,
  timezone?: string | null,
): Appointment {
  const zone = resolveScheduleTimezone(timezone);
  const dt = session.sessionDate
    ? DateTime.fromISO(session.sessionDate, { zone: "utc" }).setZone(zone)
    : null;
  const valid = dt?.isValid ? dt : null;
  const minutes = valid ? valid.minute : 0;
  const durationInHours = Math.max((session.duration ?? 60) / 60, 0.5);

  return {
    id: String(session.id),
    name: session.clientName,
    time: valid ? valid.toFormat("h:mm a") : "",
    startHour: valid ? valid.hour + minutes / 60 : 0,
    duration: durationInHours,
    status: normalizeStatus(session.status),
    session: capitalizeFirstLetter(session.sessionType) || "Session",
    service: session.serviceName || "---",
    room: isOnlineOrVirtualMode(session.sessionMode, session.zoomEnabled)
      ? "Online"
      : session.roomName || "---",
    // Calendar day key in practice timezone (YYYY-MM-DD), not browser-local ISO.
    date: valid ? valid.toISODate() ?? undefined : undefined,
    dateTime: valid ? valid.toFormat("MMM d, yyyy, h:mm a") : undefined,
    recurrenceGroupId: session.recurrenceGroupId,
    clientId: session.clientId,
    therapistId: session.therapistId,
    therapistName: session.therapistName || undefined,
    sessionMode: session.sessionMode,
    zoomEnabled: session.zoomEnabled,
    zoomJoinUrl: session.zoomJoinUrl || undefined,
    zoomPassword: session.zoomPassword || undefined,
    sessionDateIso: session.sessionDate,
    canRecordSession: !session.hasTranscript,
    hasSubmittedNote: false,
    hasTranscript: Boolean(session.hasTranscript),
    billingId: session.billingId ?? null,
    remainingDue: session.remainingDue ?? null,
    invoicePaid: session.invoicePaid,
  };
}
