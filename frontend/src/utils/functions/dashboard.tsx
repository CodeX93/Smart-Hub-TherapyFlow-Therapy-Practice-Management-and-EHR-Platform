import { CalendarIcon } from "@/components/icons/commonIcons";
import {
  SessionStatusCancelledIcon,
  SessionStatusCompletedIcon,
  SessionStatusConfirmedIcon,
  SessionStatusInProgressIcon,
  SessionStatusNoShowIcon,
  SessionStatusRescheduledIcon,
  SessionStatusScheduledIcon,
} from "@/components/icons/sessionStatusMenuIcons";
import { PencilLine } from "lucide-react";
import { UsersGroupTwoRounded } from "@solar-icons/react-perf/category/users/Linear/UsersGroupTwoRounded";
import { ClipboardList } from "@solar-icons/react-perf/category/notes/Linear/ClipboardList";
import { BillList } from "@solar-icons/react-perf/category/money/Linear/BillList";
import type {
  DashboardSessions,
  DashboardSessionItem,
} from "@/types/therapist-dashboard.type";
import type { DropdownAction } from "@/components/shared/ActionDropdown";
import { getAllowedBackendSessionStatuses } from "@/utils/sessionStatusTransitions";
import { getSessionStatusBadgeClass } from "@/utils/sessionStatusPresentation";
import { getTaskPriorityBadgeClass } from "@/utils/taskOptionPresentation";

export const getDashboardTabs = (sessions: DashboardSessions) => [
  {
    id: "Previous",
    label: "Previous",
    count: sessions?.previousTotal ?? sessions?.previous?.length ?? 0,
  },
  {
    id: "Upcoming",
    label: "Upcoming",
    count: sessions?.upcomingTotal ?? sessions?.upcoming?.length ?? 0,
  },
  {
    id: "Overdue",
    label: "Overdue",
    count: sessions?.overdueTotal ?? sessions?.overdue?.length ?? 0,
  },
];

export const getFilteredSessions = (
  sessions: DashboardSessions,
  type: string
): DashboardSessionItem[] => {
  switch (type) {
    case "Previous":
      return sessions?.previous || [];
    case "Upcoming":
      return sessions?.upcoming || [];
    case "Overdue":
      return sessions?.overdue || [];
    default:
      return [];
  }
};

export const getOverdueSessionActions = (
  session: DashboardSessionItem,
  onSessionAction?: (action: string, sessionId: string) => void
): DropdownAction[] => {
  const allowedStatuses = new Set(getAllowedBackendSessionStatuses(session.status, {
    scheduledAt: session.scheduledAt,
    hasInvoice: session.hasInvoice,
  }));
  return [
  {
    label: "Edit Session Details",
    icon: <PencilLine />,
    iconClassName: "text-(--text-primary-dark)",
    onClick: () => onSessionAction?.("edit", session.id),
  },
  allowedStatuses.has("SCHEDULED") && {
    label: "Mark as Scheduled",
    icon: <SessionStatusScheduledIcon />,
    iconClassName: "text-[#5878F8]",
    onClick: () => onSessionAction?.("SCHEDULED", session.id),
  },
  allowedStatuses.has("CONFIRMED") && {
    label: "Mark as Confirmed",
    icon: <SessionStatusConfirmedIcon />,
    iconClassName: "text-blue-600",
    onClick: () => onSessionAction?.("CONFIRMED", session.id),
  },
  allowedStatuses.has("IN_PROGRESS") && {
    label: "Mark as In Progress",
    icon: <SessionStatusInProgressIcon />,
    iconClassName: "text-purple-600",
    onClick: () => onSessionAction?.("IN_PROGRESS", session.id),
  },
  allowedStatuses.has("COMPLETED") && {
    label: "Mark as Completed",
    icon: <SessionStatusCompletedIcon />,
    iconClassName: "text-(--status-paid)",
    onClick: () => onSessionAction?.("COMPLETED", session.id),
  },
  allowedStatuses.has("CANCELLED") && {
    label: "Mark as Cancelled",
    icon: <SessionStatusCancelledIcon />,
    iconClassName: "text-(--status-denied)",
    onClick: () => onSessionAction?.("CANCELLED", session.id),
  },
  allowedStatuses.has("RESCHEDULING") && {
    label: "Mark as Rescheduled",
    icon: <SessionStatusRescheduledIcon />,
    iconClassName: "text-[#C33EF3]",
    onClick: () => onSessionAction?.("RESCHEDULING", session.id),
  },
  allowedStatuses.has("NO_SHOW") && {
    label: "Mark as No-Show",
    icon: <SessionStatusNoShowIcon />,
    iconClassName: "text-(--status-pending)",
    onClick: () => onSessionAction?.("NO_SHOW", session.id),
  },
].filter(Boolean) as DropdownAction[];
};

export const getIcon = (iconName?: string) => {
  switch (iconName) {
    case "users":
      return (
        <UsersGroupTwoRounded className="h-10 w-10 rounded-lg bg-(--bg-primary-50) p-2 text-(--text-primary-500)" />
      );
    case "calendar":
      return (
        <CalendarIcon className="h-10 w-10 rounded-lg bg-(--bg-primary-50) p-2 text-(--text-primary-500)" />
      );
    case "tasks":
      return (
        <ClipboardList className="h-10 w-10 rounded-lg bg-(--bg-primary-50) p-2 text-(--text-primary-500)" />
      );
    case "billing":
      return (
        <BillList className="h-10 w-10 rounded-lg bg-(--bg-primary-50) p-2 text-(--text-primary-500)" />
      );
    default:
      return null;
  }
};

export const getStatusStyle = (status: string) => {
  return getSessionStatusBadgeClass(status);
};

export const getPriorityStyle = (priority: string) => {
  // Reuse the same normalized priority styles used by Recent Tasks badges.
  return getTaskPriorityBadgeClass(priority);
};
