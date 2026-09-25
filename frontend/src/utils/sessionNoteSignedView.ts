import { DateTime } from "luxon";

import type { RiskScoreItem } from "./sessionNoteRiskScore.ts";

/** Matches DEFAULT_PRACTICE_TIMEZONE; kept local so this module stays alias-free. */
const FALLBACK_TIMEZONE = "UTC";

export const NOT_RECORDED = "Not recorded";
export const NOT_ASSESSED = "Not assessed";

export interface SignedNoteSection {
  label: string;
  value: string | null;
}

/**
 * A signed note is read, not filled in, so an empty field says so in words
 * instead of rendering an empty input.
 */
export function toSignedSections(
  fields: { label: string; value?: string | null }[],
): SignedNoteSection[] {
  return fields.map(({ label, value }) => ({
    label,
    value: value && value.trim() ? value.trim() : null,
  }));
}

/** e.g. "18 Sep 2026, 3:10 PM" in the practice timezone. */
export function formatSignedAt(
  value?: string | null,
  timezone?: string | null,
): string | null {
  if (!value) return null;
  const parsed = DateTime.fromISO(value, { zone: "utc" });
  if (!parsed.isValid) return null;

  const zoned = timezone ? parsed.setZone(timezone) : parsed;
  const resolved = zoned.isValid ? zoned : parsed.setZone(FALLBACK_TIMEZONE);
  return resolved.toFormat("d LLL yyyy, h:mm a");
}

export interface SignedRiskRow {
  title: string;
  answer: string | null;
}

/**
 * Turn stored risk scores back into the wording the clinician picked. A score
 * that was never recorded stays unanswered rather than reading as the first
 * option.
 */
export function toSignedRiskRows(
  items: (RiskScoreItem & { title: string })[],
  scoreByItemId: Record<string, number | null | undefined>,
): SignedRiskRow[] {
  return items.map((item) => {
    const score = scoreByItemId[item.id];
    const answer =
      typeof score === "number" && score >= 0 && score < item.options.length
        ? item.options[score]
        : null;
    return { title: item.title, answer };
  });
}
