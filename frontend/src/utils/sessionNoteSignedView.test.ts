import assert from "node:assert/strict";
import test from "node:test";

import { RISK_ASSESSMENT_ITEMS } from "../constants/riskAssessmentItems.ts";
import {
  formatSignedAt,
  NOT_ASSESSED,
  NOT_RECORDED,
  toSignedRiskRows,
  toSignedSections,
} from "./sessionNoteSignedView.ts";

test("a blank field is reported as not recorded rather than shown empty", () => {
  const sections = toSignedSections([
    { label: "Session focus", value: "Reviewed sleep hygiene." },
    { label: "Remarks", value: "" },
    { label: "Recommendations", value: "   " },
    { label: "Progress", value: null },
    { label: "Symptoms", value: undefined },
  ]);

  assert.equal(sections[0].value, "Reviewed sleep hygiene.");
  // null is the signal the view turns into the words, so a reader can tell a
  // blank field from a short one.
  assert.equal(sections[1].value, null);
  assert.equal(sections[2].value, null);
  assert.equal(sections[3].value, null);
  assert.equal(sections[4].value, null);
  assert.equal(NOT_RECORDED, "Not recorded");
});

test("surrounding whitespace does not survive into the record", () => {
  const [section] = toSignedSections([
    { label: "Symptoms", value: "  Improved sleep onset.  " },
  ]);

  assert.equal(section.value, "Improved sleep onset.");
});

test("stored scores read back as the wording the clinician picked", () => {
  const rows = toSignedRiskRows(RISK_ASSESSMENT_ITEMS, {
    suicidal_ideation: 0,
    homicidal_ideation: 3,
    medication_compliance: 4,
  });

  const byTitle = Object.fromEntries(rows.map((row) => [row.title, row.answer]));

  assert.equal(byTitle["Suicidal Ideation"], "None");
  assert.equal(byTitle["Homicidal Ideation"], "Active with plan");
  assert.equal(byTitle["Medication Compliance"], "Non-compliant");
});

test("a score that was never recorded stays unanswered", () => {
  const rows = toSignedRiskRows(RISK_ASSESSMENT_ITEMS, {
    suicidal_ideation: null,
    homicidal_ideation: undefined,
  });

  const byTitle = Object.fromEntries(rows.map((row) => [row.title, row.answer]));

  // The whole point: a missing score must not read back as "None".
  assert.equal(byTitle["Suicidal Ideation"], null);
  assert.equal(byTitle["Homicidal Ideation"], null);
  assert.equal(byTitle["Substance Abuse"], null);
  assert.equal(NOT_ASSESSED, "Not assessed");
});

test("an out of range score is not shown as an answer", () => {
  const rows = toSignedRiskRows(RISK_ASSESSMENT_ITEMS, {
    suicidal_ideation: 9,
    self_harm: -1,
  });

  const byTitle = Object.fromEntries(rows.map((row) => [row.title, row.answer]));

  assert.equal(byTitle["Suicidal Ideation"], null);
  assert.equal(byTitle["Self-Harm Behaviors"], null);
});

test("every risk item gets a row, answered or not", () => {
  const rows = toSignedRiskRows(RISK_ASSESSMENT_ITEMS, {});

  assert.equal(rows.length, RISK_ASSESSMENT_ITEMS.length);
  assert.equal(rows.every((row) => row.answer === null), true);
});

test("the signed timestamp is rendered in the practice timezone", () => {
  const signed = formatSignedAt("2026-09-18T22:10:00Z", "America/New_York");

  // 22:10 UTC is 6:10 PM in New York on the same day.
  assert.equal(signed, "18 Sep 2026, 6:10 PM");
});

test("a timezone east of UTC can land on the next day", () => {
  const signed = formatSignedAt("2026-09-18T22:10:00Z", "Asia/Karachi");

  assert.equal(signed, "19 Sep 2026, 3:10 AM");
});

test("a missing or unparseable timestamp yields nothing to render", () => {
  assert.equal(formatSignedAt(null, "UTC"), null);
  assert.equal(formatSignedAt(undefined, "UTC"), null);
  assert.equal(formatSignedAt("", "UTC"), null);
  assert.equal(formatSignedAt("not a date", "UTC"), null);
});

test("an unusable timezone falls back to UTC rather than blanking the date", () => {
  const signed = formatSignedAt("2026-09-18T22:10:00Z", "Not/AZone");

  assert.equal(signed, "18 Sep 2026, 10:10 PM");
});

test("with no timezone configured the date is shown in UTC", () => {
  assert.equal(formatSignedAt("2026-09-18T22:10:00Z", null), "18 Sep 2026, 10:10 PM");
});
