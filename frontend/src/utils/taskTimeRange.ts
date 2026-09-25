export type TaskTimeRange = "today" | "week" | "month" | "all";

function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

/**
 * Calendar windows for the Today / Week / Month chips.
 * Week is Monday–Sunday of the current week; month is the 1st through last day.
 */
export function getTaskTimeRangeDates(range: TaskTimeRange | null): {
  fromDate?: string;
  toDate?: string;
} {
  if (!range || range === "all") return {};

  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const end = new Date(start);

  if (range === "week") {
    const day = start.getDay(); // 0=Sun … 6=Sat
    const mondayOffset = day === 0 ? -6 : 1 - day;
    start.setDate(start.getDate() + mondayOffset);
    end.setTime(start.getTime());
    end.setDate(start.getDate() + 6);
  } else if (range === "month") {
    start.setDate(1);
    end.setMonth(start.getMonth() + 1, 0);
  }

  return {
    fromDate: formatLocalDate(start),
    toDate: formatLocalDate(end),
  };
}

/** Preset chips filter by createdAt; Filters panel Due Date keeps dueDate. */
export function getTaskTimeRangeDateField(
  range: TaskTimeRange | null,
): "createdAt" | undefined {
  if (!range || range === "all") return undefined;
  return "createdAt";
}
