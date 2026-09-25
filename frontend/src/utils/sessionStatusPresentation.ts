function normalizeSessionStatus(status?: string | null): string {
  const normalized = (status || "scheduled")
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, "_");

  const aliases: Record<string, string> = {
    canceled: "cancelled",
    no_show: "noshow",
    no_showed: "noshow",
    rescheduling: "rescheduled",
  };

  return aliases[normalized] ?? normalized;
}

const SESSION_STATUS_LABELS: Record<string, string> = {
  scheduled: "Scheduled",
  confirmed: "Confirmed",
  in_progress: "In Progress",
  completed: "Completed",
  cancelled: "Cancelled",
  rescheduled: "Rescheduled",
  noshow: "No-Show",
  overdue: "Overdue",
  pending: "Pending",
};

const SESSION_STATUS_BADGE_CLASSES: Record<string, string> = {
  scheduled: "bg-(--status-info-light) text-(--status-info-dark)",
  confirmed: "bg-[#ECFEFF] text-[#0E7490]",
  in_progress: "bg-[#F3E8FF] text-[#9333EA]",
  completed:
    "bg-(--status-completed-light) text-(--dark-green)",
  cancelled:
    "bg-(--status-overdue-light) text-(--status-denied)",
  rescheduled: "bg-(--pink-light) text-(--status-followup)",
  noshow:
    "bg-(--dashboard-status-pending-light) text-(--dashboard-status-pending-dark)",
  overdue:
    "bg-(--status-overdue-light) text-(--status-overdue-dark)",
  pending:
    "bg-(--dashboard-status-pending-light) text-(--dashboard-status-pending-dark)",
};

const SESSION_STATUS_BACKGROUND_CLASSES: Record<string, string> = {
  scheduled: "bg-(--status-info-light)",
  confirmed: "bg-[#ECFEFF]",
  in_progress: "bg-[#F3E8FF]",
  completed: "bg-(--status-completed-light)",
  cancelled: "bg-(--status-overdue-light)",
  rescheduled: "bg-(--pink-light)",
  noshow: "bg-(--dashboard-status-pending-light)",
  overdue: "bg-(--status-overdue-light)",
  pending: "bg-(--dashboard-status-pending-light)",
};

const SESSION_STATUS_TEXT_CLASSES: Record<string, string> = {
  scheduled: "text-(--status-info-dark)",
  confirmed: "text-[#0E7490]",
  in_progress: "text-[#9333EA]",
  completed: "text-(--dark-green)",
  cancelled: "text-(--status-denied)",
  rescheduled: "text-(--status-followup)",
  noshow: "text-(--dashboard-status-pending-dark)",
  overdue: "text-(--status-overdue-dark)",
  pending: "text-(--dashboard-status-pending-dark)",
};

function toTitleCase(value: string): string {
  return value
    .replace(/_/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

export function getSessionStatusLabel(status?: string | null): string {
  const normalized = normalizeSessionStatus(status);
  return SESSION_STATUS_LABELS[normalized] ?? toTitleCase(normalized);
}

export function getSessionStatusBadgeClass(status?: string | null): string {
  return (
    SESSION_STATUS_BADGE_CLASSES[normalizeSessionStatus(status)] ??
    "bg-(--neutral-100) text-(--text-neutral-600)"
  );
}

export function getSessionStatusBackgroundClass(
  status?: string | null,
): string {
  return (
    SESSION_STATUS_BACKGROUND_CLASSES[normalizeSessionStatus(status)] ??
    "bg-(--neutral-100)"
  );
}

export function getSessionStatusTextClass(status?: string | null): string {
  return (
    SESSION_STATUS_TEXT_CLASSES[normalizeSessionStatus(status)] ??
    "text-(--text-neutral-600)"
  );
}

export { normalizeSessionStatus };
