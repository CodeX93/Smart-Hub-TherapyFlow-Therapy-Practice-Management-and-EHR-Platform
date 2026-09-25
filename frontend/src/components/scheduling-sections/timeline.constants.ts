/**
 * Timeline geometry must use rem (matching Tailwind `h-12` = 3rem).
 * Dashboard density scales `html` font-size below 16px; fixed px tops then
 * drift below the hour labels (e.g. 8 AM chips landing on the 10 AM row).
 *
 * Hour rows start at midnight with no leading blank spacer. Labels sit on the
 * top border of each hour cell so they share the same Y as appointment tops.
 */
export const TIMELINE_HOUR_HEIGHT_REM = 3;
export const TIMELINE_MIN_SESSION_HEIGHT_REM = 1.25;
export const TIMELINE_GRID_TOP_PAD_REM = 0.5; // pt-2 / top-2
export const TIMELINE_NOW_NUDGE_REM = 0.3125;
export const TIMELINE_SHORT_SESSION_HOURS = 0.5;
/** Default vertical scroll target so business hours are visible first. */
export const TIMELINE_INITIAL_VISIBLE_HOUR = 8;

/** @deprecated Use rem helpers; kept for any leftover px math at 16px root. */
export const TIMELINE_HOUR_HEIGHT_PX = 48;

export const getTimelineSessionTop = (startHour: number): string =>
  `${startHour * TIMELINE_HOUR_HEIGHT_REM}rem`;

export const getTimelineSessionHeight = (durationHours: number): string =>
  `${Math.max(
    durationHours * TIMELINE_HOUR_HEIGHT_REM,
    TIMELINE_MIN_SESSION_HEIGHT_REM,
  )}rem`;

export const isShortTimelineSession = (durationHours: number): boolean =>
  durationHours < TIMELINE_SHORT_SESSION_HOURS;

/** Absolute height covering all hour rows (+ small pad). */
export const getTimelineHoursAreaHeight = (hourCount: number): string =>
  `${hourCount * TIMELINE_HOUR_HEIGHT_REM + 1}rem`;

/** Now-line offset inside the appointments layer. */
export const getTimelineNowIndicatorTop = (
  hour: number,
  minute: number,
): string => {
  const hoursFromMidnight = hour + minute / 60;
  return `${hoursFromMidnight * TIMELINE_HOUR_HEIGHT_REM - TIMELINE_NOW_NUDGE_REM}rem`;
};

/** Pixel height of one hour row at the current root font-size (for scrollTop). */
export function getTimelineHourHeightPx(): number {
  if (typeof window === "undefined") {
    return TIMELINE_HOUR_HEIGHT_REM * 16;
  }
  const rootPx = Number.parseFloat(
    getComputedStyle(document.documentElement).fontSize,
  );
  const safeRoot = Number.isFinite(rootPx) && rootPx > 0 ? rootPx : 16;
  return TIMELINE_HOUR_HEIGHT_REM * safeRoot;
}

/** Scroll offset that places the given hour near the top of the timeline viewport. */
export const getTimelineScrollTopForHour = (hour: number): number =>
  Math.max(0, hour * getTimelineHourHeightPx());
