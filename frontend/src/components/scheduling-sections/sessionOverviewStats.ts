import type {
  SessionOverviewStats,
  SessionOverviewStatsParams,
} from "@/store/api/admin/dashboard.api";
import type { SchedulingFilters } from "./SchedulingFilterDropdown";
import {
  resolveScheduleTimezone,
  toIsoRangeEndInTimezone,
  toIsoRangeStartInTimezone,
} from "@/utils/scheduleTimezone";

/**
 * Build overview-stats query args for a visible calendar range.
 * Uses practice timezone (not browser) so day boundaries match the calendar.
 */
export function buildOverviewStatsArgs(
  rangeStart: Date | null,
  rangeEnd: Date | null,
  filters?: SchedulingFilters,
  timezone?: string | null,
): SessionOverviewStatsParams {
  const zone = resolveScheduleTimezone(timezone);
  const args: SessionOverviewStatsParams = {
    timezone: zone,
  };

  const startDate = filters?.startDate ?? rangeStart;
  const endDate = filters?.endDate ?? rangeEnd;

  if (startDate instanceof Date) {
    args.startDate = toIsoRangeStartInTimezone(startDate, zone);
  }

  if (endDate instanceof Date) {
    args.endDate = toIsoRangeEndInTimezone(endDate, zone);
  }

  if (filters?.therapist) {
    const therapistId = Number.parseInt(filters.therapist, 10);
    if (Number.isFinite(therapistId)) {
      args.therapistId = therapistId;
    }
  }

  return args;
}

/**
 * Card values for a stats payload fetched for the matching visible range.
 * "Today" / "This Week" / "This Month" / "Total Sessions" use totalSessions of that
 * range — never the backend's absolute today/week/month fields.
 *
 * Prefer `rangeTotalOverride` (sessions list totalCount) when provided so the
 * card matches the calendar/table query even if overview is still loading.
 */
export function getOverviewCardValue(
  label: string,
  stats: SessionOverviewStats | undefined,
  rangeTotalOverride?: number | null,
): string {
  switch (label) {
    case "Today":
    case "This Week":
    case "This Month":
    case "Total Sessions": {
      if (typeof rangeTotalOverride === "number" && Number.isFinite(rangeTotalOverride)) {
        return String(rangeTotalOverride);
      }
      return String(stats?.totalSessions ?? 0);
    }
    case "Completed":
      return String(stats?.completedSessions ?? 0);
    case "Upcoming":
      return String(stats?.upcomingSessions ?? 0);
    default:
      return "0";
  }
}
