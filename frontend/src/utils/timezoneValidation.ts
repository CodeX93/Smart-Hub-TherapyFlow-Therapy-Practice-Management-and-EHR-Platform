import { DateTime } from "luxon";

const FALLBACK_TIMEZONE = "UTC";

export function isValidIanaTimezone(timezone: string): boolean {
  const trimmed = timezone.trim();
  if (!trimmed) return false;

  return DateTime.now().setZone(trimmed).isValid;
}

/**
 * The client's device timezone, used once to seed a portal account that has none.
 * It is a first guess at where the client is, never an override: after the seed the
 * saved setting wins, and only the client changes it.
 */
export function getSafeBrowserTimezone(): string | null {
  if (typeof Intl === "undefined" || typeof Intl.DateTimeFormat !== "function") {
    return null;
  }

  let detected = "";
  try {
    detected = (Intl.DateTimeFormat().resolvedOptions().timeZone || "").trim();
  } catch {
    return null;
  }

  if (!detected) return null;
  if (isValidIanaTimezone(detected)) return detected;

  return isValidIanaTimezone(FALLBACK_TIMEZONE) ? FALLBACK_TIMEZONE : null;
}

/** Whether a portal profile already carries a timezone the client is entitled to keep. */
export function hasPortalTimezone(
  timezone: string | null | undefined,
): boolean {
  if (timezone === null || timezone === undefined) return false;

  const trimmed = timezone.trim();
  return trimmed.length > 0 && trimmed.toLowerCase() !== "null";
}
