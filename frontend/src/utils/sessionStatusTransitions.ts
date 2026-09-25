import type { AppointmentStatus } from "@/types/scheduling";
import { DateTime } from "luxon";

export type BackendSessionStatus =
  | "SCHEDULED"
  | "CONFIRMED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED"
  | "RESCHEDULING"
  | "NO_SHOW"
  | "OVERDUE";

export type SessionTransitionContext = {
  scheduledAt?: string | Date | null;
  hasInvoice?: boolean;
  practiceTimezone?: string | null;
  now?: Date;
};

const appointmentToBackendMap: Record<AppointmentStatus, BackendSessionStatus> = {
  scheduled: "SCHEDULED",
  confirmed: "CONFIRMED",
  in_progress: "IN_PROGRESS",
  completed: "COMPLETED",
  cancelled: "CANCELLED",
  rescheduled: "RESCHEDULING",
  noshow: "NO_SHOW",
  overdue: "OVERDUE",
};

const backendToAppointmentMap: Record<BackendSessionStatus, AppointmentStatus> = {
  SCHEDULED: "scheduled",
  CONFIRMED: "confirmed",
  IN_PROGRESS: "in_progress",
  COMPLETED: "completed",
  CANCELLED: "cancelled",
  RESCHEDULING: "rescheduled",
  NO_SHOW: "noshow",
  OVERDUE: "overdue",
};

const workflowOrder: Record<AppointmentStatus, number> = {
  scheduled: 1,
  rescheduled: 1,
  confirmed: 2,
  overdue: 2,
  in_progress: 3,
  noshow: 4,
  completed: 4,
  cancelled: Number.POSITIVE_INFINITY,
};

const orderedStatuses: AppointmentStatus[] = [
  "scheduled",
  "rescheduled",
  "confirmed",
  "overdue",
  "in_progress",
  "noshow",
  "completed",
  "cancelled",
];

export function normalizeAppointmentStatus(status?: string | null): AppointmentStatus {
  const normalized = (status ?? "")
    .trim()
    .toLowerCase()
    .replace(/[\s_]+/g, "-");

  if (normalized === "confirmed") return "confirmed";
  if (normalized === "in-progress") return "in_progress";
  if (normalized === "completed") return "completed";
  if (normalized === "cancelled" || normalized === "canceled") return "cancelled";
  if (normalized === "rescheduling" || normalized === "rescheduled") return "rescheduled";
  if (normalized === "no-show" || normalized === "noshow") return "noshow";
  if (normalized === "overdue") return "overdue";
  return "scheduled";
}

export function normalizeBackendSessionStatus(
  status?: string | null,
): BackendSessionStatus {
  return appointmentToBackendMap[normalizeAppointmentStatus(status)];
}

export function mapAppointmentStatusToBackend(
  status: AppointmentStatus,
): BackendSessionStatus {
  return appointmentToBackendMap[status];
}

export function mapBackendSessionStatusToAppointment(
  status?: string | null,
): AppointmentStatus {
  return backendToAppointmentMap[normalizeBackendSessionStatus(status)];
}

export function isSessionStatusFinal(status?: string | null): boolean {
  const currentStatus = normalizeAppointmentStatus(status);
  return currentStatus === "completed" || currentStatus === "noshow" || currentStatus === "cancelled";
}

export function hasSessionScheduledTimePassed(
  scheduledAt?: string | Date | null,
  now: Date = new Date(),
  practiceTimezone?: string | null,
): boolean {
  if (!scheduledAt) return false;
  if (scheduledAt instanceof Date) {
    return !Number.isNaN(scheduledAt.getTime()) && now.getTime() > scheduledAt.getTime();
  }

  const hasExplicitOffset = /(?:z|[+-]\d{2}:?\d{2})$/i.test(scheduledAt.trim());
  const parsed = DateTime.fromISO(scheduledAt, hasExplicitOffset
    ? { setZone: true }
    : { zone: practiceTimezone?.trim() || "UTC" });
  return parsed.isValid && now.getTime() > parsed.toMillis();
}

export function getAllowedAppointmentStatuses(
  currentStatusInput?: string | null,
  context: SessionTransitionContext = {},
): AppointmentStatus[] {
  const currentStatus = normalizeAppointmentStatus(currentStatusInput);

  if (isSessionStatusFinal(currentStatus)) {
    return [];
  }

  let allowedStatuses: AppointmentStatus[];

  if (currentStatus === "scheduled" || currentStatus === "rescheduled") {
    allowedStatuses = [
      ...orderedStatuses.filter((status) => status !== "cancelled"),
      "cancelled",
    ];
  } else {
    const currentOrder = workflowOrder[currentStatus];
    allowedStatuses = orderedStatuses.filter((status) => {
      if (status === "cancelled") {
        return true;
      }
      if (status === "completed") {
        return currentOrder <= workflowOrder.completed;
      }
      return workflowOrder[status] >= currentOrder;
    });
  }

  const scheduledTimePassed = hasSessionScheduledTimePassed(
    context.scheduledAt,
    context.now,
    context.practiceTimezone,
  );

  return allowedStatuses.filter((status) => {
    if (status === currentStatus) return false;
    if ((status === "completed" || status === "noshow") && !scheduledTimePassed) {
      return false;
    }
    if (status === "cancelled" && context.hasInvoice) return false;
    return true;
  });
}

export function isAllowedAppointmentStatusTransition(
  currentStatusInput: string | null | undefined,
  nextStatus: AppointmentStatus,
  context: SessionTransitionContext = {},
): boolean {
  return getAllowedAppointmentStatuses(currentStatusInput, context).includes(nextStatus);
}

export function getAllowedBackendSessionStatuses(
  currentStatusInput?: string | null,
  context: SessionTransitionContext = {},
): BackendSessionStatus[] {
  return getAllowedAppointmentStatuses(currentStatusInput, context).map(
    mapAppointmentStatusToBackend,
  );
}

export function isAllowedBackendSessionStatusTransition(
  currentStatusInput: string | null | undefined,
  nextStatus: BackendSessionStatus,
  context: SessionTransitionContext = {},
): boolean {
  return getAllowedBackendSessionStatuses(currentStatusInput, context).includes(nextStatus);
}
