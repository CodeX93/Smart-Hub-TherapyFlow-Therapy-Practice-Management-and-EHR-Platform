/**
 * Helpers for showing Zoom join links only on virtual/online sessions
 * that actually have a generated meeting URL.
 */

const CANCELLED_STATUSES = new Set([
  "cancelled",
  "canceled",
  "noshow",
  "no-show",
  "no_show",
]);

export function isOnlineOrVirtualMode(
  sessionMode?: string | null,
  zoomEnabled?: boolean | null,
): boolean {
  const normalized = (sessionMode || "")
    .trim()
    .toLowerCase()
    .replace(/_/g, "-")
    .replace(/\s+/g, "-");

  // Explicit mode wins: in-person must never look virtual because of stale zoom flag/url.
  if (
    normalized === "in-person" ||
    normalized === "inperson" ||
    normalized.includes("in-person")
  ) {
    return false;
  }

  if (
    normalized === "online" ||
    normalized === "virtual" ||
    normalized === "video" ||
    normalized === "telehealth" ||
    normalized.includes("online") ||
    normalized.includes("virtual")
  ) {
    return true;
  }

  // Only fall back to zoomEnabled when mode is unknown/missing
  return zoomEnabled === true;
}

export function isZoomJoinAllowedStatus(status?: string | null): boolean {
  if (!status) {
    return true;
  }
  const normalized = status.trim().toLowerCase().replace(/\s+/g, "_");
  return !CANCELLED_STATUSES.has(normalized) && !CANCELLED_STATUSES.has(
    normalized.replace(/_/g, "-"),
  );
}

export function shouldShowZoomMeetingJoin(input: {
  joinUrl?: string | null;
  zoomEnabled?: boolean | null;
  sessionMode?: string | null;
  status?: string | null;
}): boolean {
  const url = input.joinUrl?.trim();
  if (!url) {
    return false;
  }
  if (!isZoomJoinAllowedStatus(input.status)) {
    return false;
  }
  // Mode-only for display of join; do not use lone zoomEnabled against in-person mode.
  return isOnlineOrVirtualMode(input.sessionMode, false) || (
    isOnlineOrVirtualMode(input.sessionMode, input.zoomEnabled) && !!url
  );
}
