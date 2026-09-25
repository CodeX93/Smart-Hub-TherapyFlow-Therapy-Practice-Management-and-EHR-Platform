/**
 * What a session card should say about its note.
 *
 * A note is only "missing" once the session has actually happened — a session
 * three weeks out is not overdue documentation, and asking for a note on it is
 * how every card ended up looking the same.
 */

export type SessionNoteCardState =
  | "not-expected"
  | "missing"
  | "draft"
  | "finalized";

export interface SessionNoteCardInput {
  status?: string | null;
  hasNote?: boolean;
  isFinalized?: boolean;
}

/** Statuses where documentation is owed. */
const DOCUMENTABLE_STATUSES = new Set(["completed", "in_progress", "no_show"]);

function normalizeStatus(status?: string | null): string {
  return (status ?? "").trim().toLowerCase().replace(/[\s-]+/g, "_");
}

export function getSessionNoteCardState(
  session: SessionNoteCardInput,
): SessionNoteCardState {
  if (session.hasNote) {
    return session.isFinalized ? "finalized" : "draft";
  }

  return DOCUMENTABLE_STATUSES.has(normalizeStatus(session.status))
    ? "missing"
    : "not-expected";
}

export const SESSION_NOTE_CARD_LABELS: Record<SessionNoteCardState, string | null> = {
  "not-expected": null,
  missing: "Note missing",
  draft: "Note in draft",
  finalized: "Note signed",
};

/** The wording on the card's note button for each state. */
export const SESSION_NOTE_CARD_ACTIONS: Record<SessionNoteCardState, string> = {
  "not-expected": "Add note",
  missing: "Write note",
  draft: "Finish note",
  finalized: "View note",
};

/** Only a session that is owed a note gets the emphasised button. */
export function isSessionNoteActionPrimary(state: SessionNoteCardState): boolean {
  return state === "missing" || state === "draft";
}
