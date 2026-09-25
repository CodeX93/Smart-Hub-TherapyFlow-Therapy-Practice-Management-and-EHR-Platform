import { DateTime } from "luxon";
import { isValidIanaTimezone } from "./timezoneValidation.ts";

export type WorkingHoursEntry = {
  mode?: string;
  start?: string;
  end?: string;
  day?: string;
  enabled?: boolean;
};

export type TherapistAvailabilityProfile = {
  workingHours?: string | null;
  sessionDuration?: number | null;
  timezone?: string | null;
  availablePhysicalRoomIds?: number[];
};

export function resolveTherapistTimezone(timezone?: string | null): string {
  const trimmed = timezone?.trim();
  if (trimmed && isValidIanaTimezone(trimmed)) return trimmed;
  return "UTC";
}

export function calendarDateParts(date: Date): {
  year: number;
  month: number;
  day: number;
} {
  return {
    year: date.getFullYear(),
    month: date.getMonth() + 1,
    day: date.getDate(),
  };
}

export function parse12hTimeTo24h(
  time12h: string,
): { hours: number; minutes: number } | null {
  const [clock, suffix] = time12h.trim().split(" ");
  const [hoursString, minutes = "00"] = clock.split(":");
  let hours = Number.parseInt(hoursString, 10);
  const minutesNum = Number.parseInt(minutes, 10);
  if (Number.isNaN(hours) || Number.isNaN(minutesNum)) return null;
  if (suffix === "AM" && hours === 12) hours = 0;
  if (suffix === "PM" && hours !== 12) hours += 12;
  return { hours, minutes: minutesNum };
}

export function format24hTo12h(hours24: number, minutes: number): string {
  const suffix = hours24 >= 12 ? "PM" : "AM";
  const hour12 = hours24 % 12 === 0 ? 12 : hours24 % 12;
  return `${hour12.toString().padStart(2, "0")}:${minutes
    .toString()
    .padStart(2, "0")} ${suffix}`;
}

/** Convert API 24h `HH:mm` to 12h display string. */
export function formatApiTimeTo12h(value: string): string {
  const match = value.match(/^(\d{1,2}):(\d{2})$/);
  if (!match) return value;
  const hours24 = Number.parseInt(match[1], 10);
  const minutes = Number.parseInt(match[2], 10);
  if (Number.isNaN(hours24) || Number.isNaN(minutes)) return value;
  return format24hTo12h(hours24, minutes);
}

/** Convert 12h display string to API 24h `HH:mm`. */
export function format12hToApiTime(value: string): string {
  const parsed = parse12hTimeTo24h(value);
  if (!parsed) return "09:00";
  return `${parsed.hours.toString().padStart(2, "0")}:${parsed.minutes
    .toString()
    .padStart(2, "0")}`;
}

export function formatTimezoneDisplayLabel(timezone: string): string {
  const zone = resolveTherapistTimezone(timezone);
  const dt = DateTime.now().setZone(zone);
  if (!dt.isValid) return zone;
  const offset = dt.toFormat("ZZ");
  const city = zone.split("/").pop()?.replace(/_/g, " ") ?? zone;
  return `${city} (UTC${offset})`;
}

/** Combine calendar date + 12h slot into UTC ISO using therapist timezone. */
export function toSessionIsoDateTime(
  date: Date,
  time12h: string,
  timezone?: string | null,
): string {
  const zone = resolveTherapistTimezone(timezone);
  const parsed = parse12hTimeTo24h(time12h);
  if (!parsed) {
    throw new Error("Invalid time slot format.");
  }

  const { year, month, day } = calendarDateParts(date);
  const dt = DateTime.fromObject(
    {
      year,
      month,
      day,
      hour: parsed.hours,
      minute: parsed.minutes,
      second: 0,
      millisecond: 0,
    },
    { zone },
  );

  if (!dt.isValid) {
    throw new Error("Invalid session date/time for the selected timezone.");
  }

  return dt.toUTC().toISO() ?? new Date().toISOString();
}

export function formatTime12hInTimezone(
  isoOrDate: string | Date,
  timezone?: string | null,
): string {
  const zone = resolveTherapistTimezone(timezone);
  const dt =
    typeof isoOrDate === "string"
      ? DateTime.fromISO(isoOrDate, { zone: "utc" })
      : DateTime.fromJSDate(isoOrDate, { zone: "utc" });

  if (!dt.isValid) return "";
  return dt.setZone(zone).toFormat("hh:mm a");
}

/** Map a UTC instant to a local Date picker value (calendar day in therapist TZ). */
export function instantToCalendarDateInTimezone(
  isoOrDate: string | Date,
  timezone?: string | null,
): Date | null {
  const zone = resolveTherapistTimezone(timezone);
  const dt =
    typeof isoOrDate === "string"
      ? DateTime.fromISO(isoOrDate, { zone: "utc" })
      : DateTime.fromJSDate(isoOrDate, { zone: "utc" });

  if (!dt.isValid) return null;
  const local = dt.setZone(zone);
  return new Date(local.year, local.month - 1, local.day);
}

export function getWeekdayKeyInTimezone(
  date: Date,
  timezone?: string | null,
): string {
  const zone = resolveTherapistTimezone(timezone);
  const { year, month, day } = calendarDateParts(date);
  const dt = DateTime.fromObject({ year, month, day }, { zone });
  return dt.toFormat("cccc").toLowerCase();
}

export function getWeekdayIndexInTimezone(
  date: Date,
  timezone?: string | null,
): number {
  const zone = resolveTherapistTimezone(timezone);
  const { year, month, day } = calendarDateParts(date);
  const dt = DateTime.fromObject({ year, month, day }, { zone });
  return dt.weekday % 7;
}

function parseWorkingHours(
  rawWorkingHours: string | null | undefined,
): WorkingHoursEntry[] {
  if (!rawWorkingHours) return [];
  try {
    const parsed = JSON.parse(rawWorkingHours);
    return Array.isArray(parsed)
      ? parsed.filter(
          (entry): entry is WorkingHoursEntry =>
            typeof entry === "object" && entry !== null,
        )
      : [];
  } catch {
    return [];
  }
}

function normalizeWorkingMode(mode?: string): string {
  return (mode || "")
    .trim()
    .toLowerCase()
    .replace(/[_\s]+/g, "-");
}

function normalizeWorkingDay(day?: string): string {
  return (day || "").trim().toLowerCase();
}

export function buildAvailableTimeSlots(
  date: Date | null,
  sessionMode: "virtual" | "in-person",
  sessionDuration: number,
  profile: TherapistAvailabilityProfile | null,
  timezone?: string | null,
): string[] {
  if (!date || !profile) return [];

  const zone = resolveTherapistTimezone(timezone ?? profile.timezone);
  const dayKey = getWeekdayKeyInTimezone(date, zone);
  const entries = parseWorkingHours(profile.workingHours).filter((entry) => {
    const normalizedMode = normalizeWorkingMode(entry.mode);
    const normalizedDay = normalizeWorkingDay(entry.day);
    const modeMatches =
      normalizedMode === "both" ||
      (sessionMode === "virtual" &&
        (normalizedMode === "virtual" || normalizedMode === "online")) ||
      (sessionMode === "in-person" &&
        (normalizedMode === "in-person" || normalizedMode === "inperson"));

    return entry.enabled !== false && normalizedDay === dayKey && modeMatches;
  });

  const { year, month, day } = calendarDateParts(date);
  const slots: string[] = [];

  entries.forEach((entry) => {
    if (!entry.start || !entry.end) return;
    const [startHour, startMinute = 0] = entry.start.split(":").map(Number);
    const [endHour, endMinute = 0] = entry.end.split(":").map(Number);

    let cursor = DateTime.fromObject(
      { year, month, day, hour: startHour, minute: startMinute },
      { zone },
    );
    const end = DateTime.fromObject(
      { year, month, day, hour: endHour, minute: endMinute },
      { zone },
    );

    while (
      cursor.plus({ minutes: sessionDuration }).toMillis() <= end.toMillis()
    ) {
      slots.push(cursor.toFormat("hh:mm a"));
      cursor = cursor.plus({ minutes: sessionDuration });
    }
  });

  const uniqueSlots = Array.from(new Set(slots));
  const nowInZone = DateTime.now().setZone(zone);
  const selectedDay = DateTime.fromObject({ year, month, day }, { zone });
  const isToday =
    nowInZone.year === selectedDay.year &&
    nowInZone.month === selectedDay.month &&
    nowInZone.day === selectedDay.day;

  if (!isToday) {
    return uniqueSlots;
  }

  return uniqueSlots.filter((slot) => {
    const parsed = parse12hTimeTo24h(slot);
    if (!parsed) return false;
    const slotStart = DateTime.fromObject(
      {
        year,
        month,
        day,
        hour: parsed.hours,
        minute: parsed.minutes,
      },
      { zone },
    );
    return slotStart > nowInZone;
  });
}

/**
 * Which timezone the Schedule form shows for a therapist.
 *
 * The clinic's Administration timezone is the default a profile starts from, but a
 * therapist may sit in another zone, so their own saved value wins once it exists.
 * Anything else discards a setting the profile still persists and the booking screens
 * still honour. The saved value has to be a real zone to win: a junk one would leave
 * the picker blank rather than fall back to something usable.
 */
export function resolveScheduleFormTimezone(
  savedTimezone: string | null | undefined,
  practiceTimezone: string | null | undefined,
): string {
  const saved = savedTimezone?.trim();
  if (saved && saved.toLowerCase() !== "null" && isValidIanaTimezone(saved)) {
    return saved;
  }
  return practiceTimezone?.trim() || "";
}
