import assert from "node:assert/strict";
import test from "node:test";

import { RISK_ASSESSMENT_ITEMS } from "../constants/riskAssessmentItems.ts";
import {
  buildRiskScorePayload,
  RISK_BAND_LABELS,
  RISK_ITEM_API_FIELDS,
  summarizeRiskAssessment,
  type RiskScoreItem,
} from "./sessionNoteRiskScore.ts";

const ITEMS: RiskScoreItem[] = RISK_ASSESSMENT_ITEMS.map((item) => ({
  id: item.id,
  options: item.options,
}));

function selectAll(optionIndex: number): Record<string, string> {
  return Object.fromEntries(
    ITEMS.map((item) => [
      item.id,
      item.options[Math.min(optionIndex, item.options.length - 1)],
    ]),
  );
}

function selectFirstOption(): Record<string, string> {
  return selectAll(0);
}

test("denominator is derived from the items, not hardcoded to 40", () => {
  const summary = summarizeRiskAssessment(ITEMS, selectFirstOption());

  // 8 items x 5 options each => worst possible total is 8 * 4 = 32.
  assert.equal(ITEMS.length, 8);
  assert.equal(summary.maxScore, 32);
  assert.notEqual(summary.maxScore, 40);
});

test("least concerning answer on every item scores zero and bands as low", () => {
  const summary = summarizeRiskAssessment(ITEMS, selectFirstOption());

  assert.equal(summary.score, 0);
  assert.equal(summary.band, "low");
  assert.equal(RISK_BAND_LABELS[summary.band], "Low");
  assert.equal(summary.answeredCount, 8);
  assert.equal(summary.isComplete, true);
});

test("worst answer on every item scores the maximum and bands as severe", () => {
  const summary = summarizeRiskAssessment(ITEMS, selectAll(4));

  assert.equal(summary.score, 32);
  assert.equal(summary.maxScore, 32);
  assert.equal(summary.band, "severe");
  assert.equal(summary.highestItemScore, 4);
});

test("score is the sum of the selected option indexes", () => {
  const selections = {
    ...selectFirstOption(),
    substance_abuse: "Moderate", // index 2
    impulse_control: "Fair", // index 1
  };

  const summary = summarizeRiskAssessment(ITEMS, selections);

  assert.equal(summary.score, 3);
  assert.equal(summary.maxScore, 32);
});

test("the badge moves up as selections get more concerning", () => {
  const low = summarizeRiskAssessment(ITEMS, selectFirstOption());
  const moderate = summarizeRiskAssessment(ITEMS, {
    ...selectFirstOption(),
    substance_abuse: "Moderate", // 2
    self_harm: "Occasional thoughts", // 2
    psychosis: "Moderate", // 2
    impulse_control: "Poor", // 2
    medication_compliance: "Fair", // 2
  });

  assert.equal(low.band, "low");
  // 10/32 is 31%, above the moderate threshold and below high, with no single
  // item high enough to escalate.
  assert.equal(moderate.score, 10);
  assert.equal(moderate.highestItemScore, 2);
  assert.equal(moderate.band, "moderate");
  assert.equal(RISK_BAND_LABELS[moderate.band], "Moderate");
});

test("one alarming item is not averaged away by calm ones", () => {
  const summary = summarizeRiskAssessment(ITEMS, {
    ...selectFirstOption(),
    suicidal_ideation: "Active with intent", // index 4
  });

  // 4/32 is only 12.5% of the total, which would read as low on ratio alone.
  assert.equal(summary.score, 4);
  assert.equal(summary.highestItemScore, 4);
  assert.equal(summary.band, "severe");
});

test("a single item at 3 escalates the band to at least high", () => {
  const summary = summarizeRiskAssessment(ITEMS, {
    ...selectFirstOption(),
    homicidal_ideation: "Active with plan", // index 3
  });

  assert.equal(summary.highestItemScore, 3);
  assert.equal(summary.band, "high");
});

test("missing, null and unknown selections count as unanswered", () => {
  const selections: Record<string, string | null> = {
    suicidal_ideation: "Passive", // index 1
    homicidal_ideation: null,
    substance_abuse: "Not a real option",
    // remaining items are absent entirely
  };

  const summary = summarizeRiskAssessment(ITEMS, selections);

  assert.equal(summary.score, 1);
  assert.equal(summary.answeredCount, 1);
  assert.equal(summary.totalCount, 8);
  assert.equal(summary.isComplete, false);
  // The denominator still reflects the full assessment.
  assert.equal(summary.maxScore, 32);
});

test("the denominator follows a changed item set", () => {
  const items: RiskScoreItem[] = [
    { id: "a", options: ["None", "Some", "A lot"] },
    { id: "b", options: ["None", "Some", "A lot"] },
  ];

  const summary = summarizeRiskAssessment(items, { a: "A lot", b: "Some" });

  assert.equal(summary.maxScore, 4);
  assert.equal(summary.score, 3);
  assert.equal(summary.totalCount, 2);
});

test("an empty item set does not divide by zero", () => {
  const summary = summarizeRiskAssessment([], {});

  assert.equal(summary.score, 0);
  assert.equal(summary.maxScore, 0);
  assert.equal(summary.band, "low");
  assert.equal(summary.isComplete, false);
});

test("an untouched assessment sends no risk scores at all", () => {
  const payload = buildRiskScorePayload(ITEMS, {});

  // The whole point of the fix: nothing is sent, so the record stores null
  // rather than a score of 0 that reads as "assessed, no risk".
  assert.deepEqual(payload, {});
  assert.equal(Object.keys(payload).length, 0);
});

test("null selections send no risk scores", () => {
  const selections = Object.fromEntries(ITEMS.map((item) => [item.id, null]));

  assert.deepEqual(buildRiskScorePayload(ITEMS, selections), {});
});

test("an explicit least concerning answer does send a zero", () => {
  const payload = buildRiskScorePayload(ITEMS, selectFirstOption());

  // "No risk factors identified" is an assertion, and must reach the record.
  assert.equal(Object.keys(payload).length, 8);
  assert.equal(payload.riskSuicidalIdeation, 0);
  assert.equal(payload.riskNonAdherence, 0);
});

test("a partly answered assessment sends only the answered items", () => {
  const payload = buildRiskScorePayload(ITEMS, {
    suicidal_ideation: "Active with plan", // 3
    substance_abuse: "Minimal", // 1
  });

  assert.deepEqual(payload, {
    riskSuicidalIdeation: 3,
    riskSubstanceUse: 1,
  });
  assert.equal("riskSelfHarm" in payload, false);
});

test("every risk item maps to an API field", () => {
  const mapped = Object.keys(RISK_ITEM_API_FIELDS).sort();
  const actual = ITEMS.map((item) => item.id).sort();

  assert.deepEqual(mapped, actual);
});

test("an unknown selection is not sent", () => {
  const payload = buildRiskScorePayload(ITEMS, {
    suicidal_ideation: "Not a real option",
  });

  assert.deepEqual(payload, {});
});
