export const DEFAULT_FILTER_CALENDAR_MIN_YEAR = 2010;
export const DEFAULT_CALENDAR_PAST_MIN_YEAR = 1900;
export const DEFAULT_CALENDAR_FUTURE_YEAR_OFFSET = 50;

export function getDefaultCalendarMaxYear(
  offset = DEFAULT_CALENDAR_FUTURE_YEAR_OFFSET,
): number {
  return new Date().getFullYear() + offset;
}

/** Parse a calendar date (YYYY-MM-DD or ISO string) in local time — avoids UTC off-by-one. */
export const parseDateOnly = (value?: string | null): Date | null => {
  if (!value) return null;
  const trimmed = value.trim();
  const match = trimmed.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!match) {
    const parsed = new Date(trimmed);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
  const year = Number.parseInt(match[1], 10);
  const month = Number.parseInt(match[2], 10) - 1;
  const day = Number.parseInt(match[3], 10);
  return new Date(year, month, day);
};

/** Format a Date as YYYY-MM-DD using local calendar fields. */
export const formatDateOnly = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const daysInMonth = (date: Date) =>
  new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
const firstDayOfMonth = (date: Date) =>
  new Date(date.getFullYear(), date.getMonth(), 1).getDay();

export const getDaysArray = (currentMonth: Date) => {
  const days: Array<number | null> = [];
  const prevMonthDays = (firstDayOfMonth(currentMonth) + 6) % 7;
  const totalDays = daysInMonth(currentMonth);

  for (let i = 0; i < prevMonthDays; i++) days.push(null);
  for (let i = 1; i <= totalDays; i++) days.push(i);
  return days;
};

export const formatDateHeader = (date: Date) => {
  return new Intl.DateTimeFormat("en-GB", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(date);
};

export const formatDateLong = (date: Date) => {
  return new Intl.DateTimeFormat("en-US", {
    weekday: "long",
    month: "long",
    day: "numeric",
  }).format(date);
};

export const formatDateShort = (date: Date) => {
  return new Intl.DateTimeFormat("en-US", {
    weekday: "short",
    month: "short",
    day: "numeric",
  }).format(date);
};

export const getWeekdayShort = (date: Date) => {
  return new Intl.DateTimeFormat("en-US", {
    weekday: "short",
  }).format(date);
};

export const getRelativeDay = (
  currentMonth: Date,
  selectedDate: number,
  offset: number
) => {
  const date = new Date(
    currentMonth.getFullYear(),
    currentMonth.getMonth(),
    selectedDate + offset
  );
  return {
    month: new Date(date.getFullYear(), date.getMonth()),
    day: date.getDate(),
  };
};

export const constructDate = (year: number, month: number, day: number) => {
  return new Date(year, month, day);
};

export const generateHours = () => {
  return Array.from({ length: 24 }, (_, i) => {
    const hour = i % 12 || 12;
    const period = i < 12 ? "AM" : "PM";
    return `${hour} ${period}`;
  });
};

export const formatDateFull = (date: Date) => {
  return new Intl.DateTimeFormat("en-US", {
    weekday: "long",
    year: "numeric",
    month: "short",
    day: "numeric",
  }).format(date);
};

/** e.g. "Dec 21 - Dec 27, 2025" */
export const formatWeekRange = (weekStart: Date, weekEnd: Date) => {
  const start = new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
  }).format(weekStart);
  const end = new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(weekEnd);
  return `${start} - ${end}`;
};

export const formatMonthOnly = (date: Date) => {
  return new Intl.DateTimeFormat("en-US", {
    month: "long",
  }).format(date);
};

export const getCalendarDays = (currentMonth: Date) => {
  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth();

  // First day of the month
  const firstDay = new Date(year, month, 1);
  // Day of week (0-6, Sun-Sat)
  const dayOfWeek = firstDay.getDay();

  // Start date of the grid (Sunday of the first week)
  const startDate = new Date(firstDay);
  startDate.setDate(1 - dayOfWeek);

  const days: Date[] = [];
  // Generate 42 days (6 weeks)
  for (let i = 0; i < 42; i++) {
    const day = new Date(startDate);
    day.setDate(startDate.getDate() + i);
    days.push(day);
  }

  return days;
};
