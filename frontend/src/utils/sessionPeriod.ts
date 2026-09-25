export type SessionPeriod =
  | "all_time"
  | "this_week"
  | "last_week"
  | "this_month"
  | "last_month"
  | "this_year"
  | "last_year";

export const DEFAULT_SESSION_PERIOD: SessionPeriod = "this_month";

export const SESSION_PERIOD_OPTIONS: Array<{ value: SessionPeriod; label: string }> = [
  { value: "all_time", label: "All Time" },
  { value: "this_week", label: "This Week" },
  { value: "last_week", label: "Last Week" },
  { value: "this_month", label: "This Month" },
  { value: "last_month", label: "Last Month" },
  { value: "this_year", label: "This Year" },
  { value: "last_year", label: "Last Year" },
];

function startOfLocalDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
}

function endOfLocalDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999);
}

function mondayOfWeek(date: Date): Date {
  const start = startOfLocalDay(date);
  const day = start.getDay(); // 0=Sun … 6=Sat
  const mondayOffset = day === 0 ? -6 : 1 - day;
  start.setDate(start.getDate() + mondayOffset);
  return start;
}

/**
 * Calendar windows for the dashboard Sessions Period filter.
 * Week is Monday–Sunday; month/year use local calendar boundaries.
 */
export function getSessionPeriodRange(period: SessionPeriod): {
  startDate?: string;
  endDate?: string;
} {
  if (period === "all_time") return {};

  const now = new Date();
  let start: Date;
  let end: Date;

  switch (period) {
    case "this_week": {
      start = mondayOfWeek(now);
      end = endOfLocalDay(
        new Date(start.getFullYear(), start.getMonth(), start.getDate() + 6),
      );
      break;
    }
    case "last_week": {
      const thisMonday = mondayOfWeek(now);
      start = startOfLocalDay(
        new Date(thisMonday.getFullYear(), thisMonday.getMonth(), thisMonday.getDate() - 7),
      );
      end = endOfLocalDay(
        new Date(start.getFullYear(), start.getMonth(), start.getDate() + 6),
      );
      break;
    }
    case "this_month": {
      start = startOfLocalDay(new Date(now.getFullYear(), now.getMonth(), 1));
      end = endOfLocalDay(new Date(now.getFullYear(), now.getMonth() + 1, 0));
      break;
    }
    case "last_month": {
      start = startOfLocalDay(new Date(now.getFullYear(), now.getMonth() - 1, 1));
      end = endOfLocalDay(new Date(now.getFullYear(), now.getMonth(), 0));
      break;
    }
    case "this_year": {
      start = startOfLocalDay(new Date(now.getFullYear(), 0, 1));
      end = endOfLocalDay(new Date(now.getFullYear(), 11, 31));
      break;
    }
    case "last_year": {
      start = startOfLocalDay(new Date(now.getFullYear() - 1, 0, 1));
      end = endOfLocalDay(new Date(now.getFullYear() - 1, 11, 31));
      break;
    }
    default:
      return {};
  }

  return {
    startDate: start.toISOString(),
    endDate: end.toISOString(),
  };
}
