import { DateTime } from "luxon";
import { isValidIanaTimezone } from "@/utils/timezoneValidation";

/** Safe fallback only; the configured Administration timezone is the business source of truth. */
export const DEFAULT_PRACTICE_TIMEZONE = "UTC";

/**
 * Resolve timezone for staff scheduling calendar display.
 * Prefer practice timezone — never fall back to the browser zone
 * (that made Eastern 9:00 AM render as 6:00 PM in Asia/Karachi).
 */
export function resolveScheduleTimezone(timezone?: string | null): string {
  const trimmed = timezone?.trim();
  if (trimmed && isValidIanaTimezone(trimmed)) {
    return trimmed;
  }
  return DEFAULT_PRACTICE_TIMEZONE;
}

export function toIsoRangeStartInTimezone(
  date: Date,
  timezone?: string | null,
): string {
  const zone = resolveScheduleTimezone(timezone);
  const dt = DateTime.fromObject(
    {
      year: date.getFullYear(),
      month: date.getMonth() + 1,
      day: date.getDate(),
    },
    { zone },
  ).startOf("day");
  return dt.toUTC().toISO() ?? date.toISOString();
}

export function toIsoRangeEndInTimezone(
  date: Date,
  timezone?: string | null,
): string {
  const zone = resolveScheduleTimezone(timezone);
  const dt = DateTime.fromObject(
    {
      year: date.getFullYear(),
      month: date.getMonth() + 1,
      day: date.getDate(),
    },
    { zone },
  ).endOf("day");
  return dt.toUTC().toISO() ?? date.toISOString();
}

export function calendarDateKey(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function parseInstantInScheduleTimezone(
  value?: string | null,
  timezone?: string | null,
): DateTime | null {
  if (!value) return null;
  const zone = resolveScheduleTimezone(timezone);
  const parsed = DateTime.fromISO(value, { setZone: true });
  if (!parsed.isValid) return null;
  return parsed.setZone(zone);
}

/** e.g. "Sep 01, 2025" in practice timezone (not browser local). */
export function formatDateInScheduleTimezone(
  value?: string | null,
  timezone?: string | null,
): string {
  const dt = parseInstantInScheduleTimezone(value, timezone);
  if (!dt) return "-";
  return dt.toFormat("MMM dd, yyyy");
}

/** e.g. "9:00 AM" in practice timezone (not browser local). */
export function formatTimeInScheduleTimezone(
  value?: string | null,
  timezone?: string | null,
): string | undefined {
  const dt = parseInstantInScheduleTimezone(value, timezone);
  if (!dt) return undefined;
  return dt.toFormat("h:mm a");
}

export function formatDateTimeInScheduleTimezone(
  value?: string | null,
  timezone?: string | null,
): string {
  const dt = parseInstantInScheduleTimezone(value, timezone);
  if (!dt) return "";
  return dt.toFormat("MMM d, h:mm a");
}

/** e.g. "GMT -04:00" for the practice timezone (not the browser zone). */
export function formatPracticeGmtOffset(timezone?: string | null): string {
  const zone = resolveScheduleTimezone(timezone);
  return `GMT ${DateTime.now().setZone(zone).toFormat("ZZ")}`;
}
