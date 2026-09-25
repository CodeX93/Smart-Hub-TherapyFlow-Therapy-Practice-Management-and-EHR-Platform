import assert from "node:assert/strict";
import test from "node:test";

import {
  ASSESSMENT_CARD_ACTIONS,
  getAssessmentCardState,
  getAssessmentStartStep,
  hasAssessmentReport,
  isAssessmentActionPrimary,
} from "./assessmentCardState.ts";

test("a freshly assigned assessment has not been started", () => {
  assert.equal(getAssessmentCardState("pending"), "not-started");
  assert.equal(getAssessmentCardState(undefined), "not-started");
  assert.equal(getAssessmentCardState(""), "not-started");
});

test("every answering status reads as in progress", () => {
  for (const status of [
    "client_in_progress",
    "waiting_for_review",
    "therapist_completed",
    "Therapist In Progress",
  ]) {
    assert.equal(getAssessmentCardState(status), "in-progress", status);
  }
});

test("a generated report waits to be finalized; a completed one is finalized", () => {
  assert.equal(getAssessmentCardState("waiting_for_therapist"), "report-draft");
  assert.equal(getAssessmentCardState("COMPLETED"), "finalized");
});

test("the card opens on the step that still needs doing", () => {
  assert.equal(getAssessmentStartStep("not-started"), "questions");
  assert.equal(getAssessmentStartStep("in-progress"), "questions");
  assert.equal(getAssessmentStartStep("report-draft"), "report");
  assert.equal(getAssessmentStartStep("finalized"), "report");
  assert.equal(hasAssessmentReport("in-progress"), false);
});

test("only outstanding work gets the emphasised button, and each names its action", () => {
  assert.equal(isAssessmentActionPrimary("report-draft"), true);
  assert.equal(isAssessmentActionPrimary("finalized"), false);
  assert.equal(ASSESSMENT_CARD_ACTIONS["not-started"], "Start");
  assert.equal(ASSESSMENT_CARD_ACTIONS.finalized, "View report");
});
