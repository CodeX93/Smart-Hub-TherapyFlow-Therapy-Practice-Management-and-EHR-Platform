import { formatDateOnly } from "@/utils/transformer/dates.transformer";
import {
  getWeekdayIndexInTimezone,
  toSessionIsoDateTime,
} from "@/utils/therapistTimezone";
import type {
  RecurrenceFormState,
  RecurrenceRuleRequest,
  RecurrenceType,
} from "@/types/recurringSessions";

export function isRecurringSeriesSession(
  recurrenceGroupId?: string | null,
): boolean {
  return Boolean(recurrenceGroupId?.trim().startsWith("rec-"));
}

export function timeSlotTo24Hour(time: string): string {
  const trimmed = time.trim();
  const match = trimmed.match(/^(\d{1,2}):(\d{2})\s*(AM|PM)?$/i);
  if (!match) return trimmed;

  let hours = Number.parseInt(match[1], 10);
  const minutes = match[2];
  const suffix = match[3]?.toUpperCase();

  if (suffix === "AM" && hours === 12) hours = 0;
  if (suffix === "PM" && hours !== 12) hours += 12;

  return `${String(hours).padStart(2, "0")}:${minutes}`;
}

export function formatRecurrenceSummary(state: RecurrenceFormState): string {
  if (!state.isRecurring) return "One-time session";

  const intervalLabel = state.interval > 1 ? `every ${state.interval} ` : "every ";
  if (state.recurrenceType === "weekly") {
    const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
    const days = state.daysOfWeek.map((day) => dayNames[day]).join(", ");
    const end =
      state.endMode === "count"
        ? `${state.count} session${state.count === 1 ? "" : "s"}`
        : `until ${state.untilDate ? formatDateOnly(state.untilDate) : "—"}`;
    return `Weekly ${intervalLabel}week on ${days || "selected days"} · ${end}`;
  }

  const months =
    state.monthsOfYear.length > 0
      ? ` in months ${state.monthsOfYear.join(", ")}`
      : "";
  const end =
    state.endMode === "count"
      ? `${state.count} session${state.count === 1 ? "" : "s"}`
      : `until ${state.untilDate ? formatDateOnly(state.untilDate) : "—"}`;
  return `Monthly ${intervalLabel}month${months} · ${end}`;
}

export function buildRecurrenceRuleRequest(input: {
  clientId: number;
  therapistId: number;
  serviceId: number;
  roomId?: number;
  sessionMode: string;
  sessionModeKey?: string;
  isInPersonMode?: (mode: string) => boolean;
  sessionType?: string;
  notes?: string;
  sessionDate: Date;
  sessionTime12h: string;
  timezone?: string;
  recurrence: RecurrenceFormState;
}): RecurrenceRuleRequest | null {
  if (!input.recurrence.isRecurring) return null;

  const inPerson = input.isInPersonMode
    ? input.isInPersonMode(input.sessionMode)
    : input.sessionMode.toLowerCase().replace(/[\s_-]+/g, "").includes("person");

  const body: RecurrenceRuleRequest = {
    clientId: input.clientId,
    therapistId: input.therapistId,
    serviceId: input.serviceId,
    sessionMode: input.sessionModeKey ?? input.sessionMode,
    sessionType: input.sessionType?.trim() || undefined,
    notes: input.notes?.trim() || undefined,
    zoomEnabled: !inPerson,
    sessionDate: toSessionIsoDateTime(
      input.sessionDate,
      input.sessionTime12h,
      input.timezone,
    ),
    timezone: input.timezone?.trim() || undefined,
    recurrenceType: input.recurrence.recurrenceType,
    interval: input.recurrence.interval,
    endMode: input.recurrence.endMode,
  };

  if (inPerson && input.roomId) {
    body.roomId = input.roomId;
  }

  if (input.recurrence.recurrenceType === "weekly") {
    body.daysOfWeek = [...input.recurrence.daysOfWeek].sort((a, b) => a - b);
  } else if (input.recurrence.monthsOfYear.length > 0) {
    body.monthsOfYear = [...input.recurrence.monthsOfYear].sort((a, b) => a - b);
  }

  if (input.recurrence.endMode === "count") {
    body.count = input.recurrence.count;
  } else if (input.recurrence.untilDate) {
    body.untilDate = formatDateOnly(input.recurrence.untilDate);
  }

  return body;
}

export function canPreviewRecurrence(
  recurrence: RecurrenceFormState,
  startDate: Date | null,
  sessionTime: string,
): boolean {
  if (!recurrence.isRecurring || !startDate || !sessionTime.trim()) return false;
  if (recurrence.recurrenceType === "weekly" && recurrence.daysOfWeek.length === 0) {
    return false;
  }
  if (recurrence.endMode === "count" && (!recurrence.count || recurrence.count < 1)) {
    return false;
  }
  if (recurrence.endMode === "until" && !recurrence.untilDate) return false;
  return true;
}

export function getDefaultWeeklyDay(
  startDate: Date | null,
  timezone?: string | null,
): number[] {
  if (!startDate) return [];
  return [getWeekdayIndexInTimezone(startDate, timezone)];
}

export function toggleDayOfWeek(days: number[], day: number): number[] {
  return days.includes(day) ? days.filter((value) => value !== day) : [...days, day];
}

export function toggleMonthOfYear(months: number[], month: number): number[] {
  return months.includes(month)
    ? months.filter((value) => value !== month)
    : [...months, month];
}

export const WEEKDAY_OPTIONS = [
  { value: 0, label: "Sun" },
  { value: 1, label: "Mon" },
  { value: 2, label: "Tue" },
  { value: 3, label: "Wed" },
  { value: 4, label: "Thu" },
  { value: 5, label: "Fri" },
  { value: 6, label: "Sat" },
] as const;

export const MONTH_OPTIONS = Array.from({ length: 12 }, (_, index) => ({
  value: index + 1,
  label: new Date(2000, index, 1).toLocaleString("en-US", { month: "short" }),
}));

export function isValidRecurrenceType(value: string): value is RecurrenceType {
  return value === "weekly" || value === "monthly";
}
