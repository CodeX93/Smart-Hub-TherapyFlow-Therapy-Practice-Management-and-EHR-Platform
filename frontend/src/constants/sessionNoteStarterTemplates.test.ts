import assert from "node:assert/strict";
import test from "node:test";

import { SESSION_NOTE_STARTER_TEMPLATES } from "./sessionNoteStarterTemplates.ts";

// Mirrors SESSION_NOTE_AI_TEMPLATE_NAME_MAX; a starter that exceeds it would be
// rejected by the same validation a hand-written template goes through.
const NAME_MAX = 50;

test("a starter is offered for each of the three common formats", () => {
  const keys = SESSION_NOTE_STARTER_TEMPLATES.map((starter) => starter.key);

  assert.deepEqual(keys, ["soap", "dap", "narrative"]);
});

test("every starter name fits the template name limit", () => {
  for (const starter of SESSION_NOTE_STARTER_TEMPLATES) {
    assert.ok(
      starter.name.length <= NAME_MAX,
      `${starter.key} name is ${starter.name.length} characters`,
    );
    assert.equal(starter.name.trim(), starter.name);
  }
});

test("every starter carries instructions the AI can actually act on", () => {
  for (const starter of SESSION_NOTE_STARTER_TEMPLATES) {
    assert.ok(starter.instructions.trim().length > 200, starter.key);
    assert.ok(starter.summary.trim().length > 0, starter.key);
    // Each one has to forbid inventing content, since the note is a record.
    assert.match(starter.instructions, /do not invent/i);
  }
});

test("keys and names are unique so the picker cannot show duplicates", () => {
  const keys = new Set(SESSION_NOTE_STARTER_TEMPLATES.map((s) => s.key));
  const names = new Set(SESSION_NOTE_STARTER_TEMPLATES.map((s) => s.name));

  assert.equal(keys.size, SESSION_NOTE_STARTER_TEMPLATES.length);
  assert.equal(names.size, SESSION_NOTE_STARTER_TEMPLATES.length);
});

test("the structured formats name their own sections", () => {
  const byKey = Object.fromEntries(
    SESSION_NOTE_STARTER_TEMPLATES.map((s) => [s.key, s.instructions]),
  );

  for (const heading of ["Subjective", "Objective", "Assessment", "Plan"]) {
    assert.match(byKey.soap, new RegExp(heading));
  }
  for (const heading of ["Data", "Assessment", "Plan"]) {
    assert.match(byKey.dap, new RegExp(heading));
  }
  // The narrative format is defined by having no headed sections.
  assert.match(byKey.narrative, /without headed sections/i);
});
