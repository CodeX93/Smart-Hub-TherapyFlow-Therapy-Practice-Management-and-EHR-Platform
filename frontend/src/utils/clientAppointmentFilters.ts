import type { PortalSessionHistoryItem } from "@/store/api/portalApi";
import {
  getSessionStatusLabel,
  normalizeSessionStatus,
} from "@/utils/sessionStatusPresentation";

export interface ClientAppointmentFilters {
  startDate: Date | null;
  endDate: Date | null;
  status: string | null;
  serviceId: string | null;
  sessionMode: string | null;
}

export const DEFAULT_CLIENT_APPOINTMENT_FILTERS: ClientAppointmentFilters = {
  startDate: null,
  endDate: null,
  status: null,
  serviceId: null,
  sessionMode: null,
};

export const CLIENT_APPOINTMENT_STATUS_OPTIONS = [
  { value: "", label: "All Statuses" },
  { value: "scheduled", label: getSessionStatusLabel("scheduled") },
  { value: "confirmed", label: getSessionStatusLabel("confirmed") },
  { value: "rescheduled", label: getSessionStatusLabel("rescheduled") },
  { value: "in_progress", label: getSessionStatusLabel("in_progress") },
  { value: "completed", label: getSessionStatusLabel("completed") },
  { value: "cancelled", label: getSessionStatusLabel("cancelled") },
  { value: "noshow", label: getSessionStatusLabel("noshow") },
  { value: "overdue", label: getSessionStatusLabel("overdue") },
  { value: "pending", label: getSessionStatusLabel("pending") },
];

export const CLIENT_APPOINTMENT_SESSION_MODE_OPTIONS = [
  { value: "", label: "All Types" },
  { value: "in-person", label: "In-Person" },
  { value: "online", label: "Virtual" },
];

function normalizeSessionMode(sessionMode?: string | null): string {
  const normalized = (sessionMode || "").trim().toLowerCase().replace(/_/g, "-");

  if (normalized === "online" || normalized === "virtual") {
    return "online";
  }

  if (normalized === "in-person" || normalized === "inperson") {
    return "in-person";
  }

  return normalized;
}

export function hasActiveClientAppointmentFilters(
  filters: ClientAppointmentFilters,
): boolean {
  return Boolean(
    filters.startDate ||
      filters.endDate ||
      filters.status ||
      filters.serviceId ||
      filters.sessionMode,
  );
}

export function applyClientAppointmentFilters(
  sessions: PortalSessionHistoryItem[],
  filters: ClientAppointmentFilters,
): PortalSessionHistoryItem[] {
  return sessions.filter((session) => {
    if (filters.status) {
      const sessionStatus = normalizeSessionStatus(session.status);
      const filterStatus = normalizeSessionStatus(filters.status);
      if (sessionStatus !== filterStatus) {
        return false;
      }
    }

    if (filters.serviceId && String(session.serviceId) !== filters.serviceId) {
      return false;
    }

    if (filters.sessionMode) {
      if (normalizeSessionMode(session.sessionMode) !== filters.sessionMode) {
        return false;
      }
    }

    if (filters.startDate || filters.endDate) {
      const sessionDate = new Date(session.sessionDate);
      if (Number.isNaN(sessionDate.getTime())) {
        return false;
      }

      if (filters.startDate) {
        const start = new Date(filters.startDate);
        start.setHours(0, 0, 0, 0);
        if (sessionDate < start) {
          return false;
        }
      }

      if (filters.endDate) {
        const end = new Date(filters.endDate);
        end.setHours(23, 59, 59, 999);
        if (sessionDate > end) {
          return false;
        }
      }
    }

    return true;
  });
}
