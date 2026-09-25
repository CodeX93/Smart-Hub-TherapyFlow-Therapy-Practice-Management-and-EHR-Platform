import assert from "node:assert/strict";
import test from "node:test";

import {
  getSessionNoteCardState,
  isSessionNoteActionPrimary,
  SESSION_NOTE_CARD_ACTIONS,
} from "./sessionNoteCardState.ts";

test("a session that has not happened yet is not owed a note", () => {
  assert.equal(getSessionNoteCardState({ status: "SCHEDULED" }), "not-expected");
  assert.equal(getSessionNoteCardState({ status: "CONFIRMED" }), "not-expected");
  assert.equal(getSessionNoteCardState({ status: "CANCELLED" }), "not-expected");
  assert.equal(getSessionNoteCardState({ status: "RESCHEDULING" }), "not-expected");
});

test("a session that has happened and has no note is missing one", () => {
  assert.equal(getSessionNoteCardState({ status: "COMPLETED" }), "missing");
  assert.equal(getSessionNoteCardState({ status: "IN_PROGRESS" }), "missing");
  assert.equal(getSessionNoteCardState({ status: "NO_SHOW" }), "missing");
});

test("status wording and casing do not change the answer", () => {
  assert.equal(getSessionNoteCardState({ status: "completed" }), "missing");
  assert.equal(getSessionNoteCardState({ status: "No-Show" }), "missing");
  assert.equal(getSessionNoteCardState({ status: "in progress" }), "missing");
});

test("an existing note reports whether it is signed", () => {
  assert.equal(
    getSessionNoteCardState({ status: "COMPLETED", hasNote: true, isFinalized: false }),
    "draft",
  );
  assert.equal(
    getSessionNoteCardState({ status: "COMPLETED", hasNote: true, isFinalized: true }),
    "finalized",
  );
});

test("a note on a future session still shows as a note, not as missing", () => {
  assert.equal(
    getSessionNoteCardState({ status: "SCHEDULED", hasNote: true, isFinalized: true }),
    "finalized",
  );
});

test("only outstanding documentation gets the emphasised button", () => {
  assert.equal(isSessionNoteActionPrimary("missing"), true);
  assert.equal(isSessionNoteActionPrimary("draft"), true);
  assert.equal(isSessionNoteActionPrimary("finalized"), false);
  assert.equal(isSessionNoteActionPrimary("not-expected"), false);
});

test("the button says what pressing it does", () => {
  assert.equal(SESSION_NOTE_CARD_ACTIONS.missing, "Write note");
  assert.equal(SESSION_NOTE_CARD_ACTIONS.draft, "Finish note");
  assert.equal(SESSION_NOTE_CARD_ACTIONS.finalized, "View note");
});
