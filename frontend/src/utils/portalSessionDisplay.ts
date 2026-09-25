import {
  getSessionStatusBadgeClass,
  getSessionStatusLabel,
} from "./sessionStatusPresentation.ts";

export function getSessionModalityLabel(sessionMode?: string | null): string {
  const normalized = (sessionMode || "").trim().toLowerCase().replace(/_/g, "-");

  if (normalized === "online" || normalized === "virtual") {
    return "Virtual";
  }

  if (normalized === "in-person" || normalized === "inperson") {
    return "In-Person";
  }

  return "In-Person";
}

/**
 * Badge colors aligned with staff SessionCard (admin client Sessions tab).
 * Virtual → purple; In-Person → green.
 */
export function getSessionModalityBadgeClass(sessionModeOrLabel?: string | null): string {
  const label = getSessionModalityLabel(sessionModeOrLabel).toLowerCase();
  if (label === "virtual") {
    return "bg-[#F3E8FF] text-[#C33EF3]";
  }
  return "bg-[#EBFEF4] text-[#0ABF7C]";
}

export function isVirtualSession(session: {
  sessionMode?: string | null;
  type?: string | null;
  zoomEnabled?: boolean;
}): boolean {
  const mode = session.sessionMode || session.type || "";
  const normalized = mode
    .trim()
    .toLowerCase()
    .replace(/_/g, "-")
    .replace(/\s+/g, "-");

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

  // Unknown mode only: stale type "Virtual" label or zoom flag
  if ((session.type || "").toLowerCase().includes("virtual")) {
    return true;
  }
  return session.zoomEnabled === true;
}

export function getBookingSessionTypeLabel(sessionType: "online" | "in-person"): string {
  return sessionType === "online" ? "Virtual" : "In-Person";
}

export function getPortalSessionStatusLabel(status?: string | null): string {
  return getSessionStatusLabel(status);
}

export function getPortalSessionStatusStyles(status?: string | null): string {
  return getSessionStatusBadgeClass(status);
}

/**
 * `timeZone` comes from the API (the client's own setting, or the clinic default).
 * It is passed explicitly so the device's timezone never decides what a client reads.
 */
export function formatPortalSlotStartTime(
  dateIso: string,
  startHHmm: string,
  startUtc?: string,
  timeZone?: string,
): string {
  const timeOptions: Intl.DateTimeFormatOptions = {
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
    ...(timeZone ? { timeZone } : {}),
  };

  if (startUtc) {
    const utcDate = new Date(startUtc);
    if (!Number.isNaN(utcDate.getTime())) {
      return utcDate.toLocaleTimeString([], timeOptions);
    }
  }

  const [hourRaw, minuteRaw] = startHHmm.split(":");
  const hours = Number.parseInt(hourRaw, 10);
  const minutes = Number.parseInt(minuteRaw, 10);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) {
    return startHHmm;
  }

  const localDate = new Date(`${dateIso}T00:00:00`);
  if (Number.isNaN(localDate.getTime())) {
    return startHHmm;
  }

  // Fallback only: no UTC instant to convert, so startHHmm is echoed back as-is.
  // Deliberately not re-zoned - localDate was built from local wall-clock fields,
  // so applying timeZone here would shift a time that was never UTC to begin with.
  localDate.setHours(hours, minutes, 0, 0);
  return localDate.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  });
}

/** Slot wall-clock times from the portal API are therapist-local; booking uses the same local interpretation. */
export function isPortalSlotInPast(
  dateIso: string,
  startHHmm: string,
  nowMs: number = Date.now(),
  startUtc?: string,
): boolean {
  if (startUtc) {
    const utcDate = new Date(startUtc);
    if (!Number.isNaN(utcDate.getTime())) {
      return utcDate.getTime() <= nowMs;
    }
  }

  const [hourRaw, minuteRaw] = startHHmm.split(":");
  const hours = Number.parseInt(hourRaw, 10);
  const minutes = Number.parseInt(minuteRaw, 10);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) {
    return true;
  }

  const slotLocal = new Date(`${dateIso}T00:00:00`);
  if (Number.isNaN(slotLocal.getTime())) {
    return true;
  }

  slotLocal.setHours(hours, minutes, 0, 0);
  return slotLocal.getTime() <= nowMs;
}
