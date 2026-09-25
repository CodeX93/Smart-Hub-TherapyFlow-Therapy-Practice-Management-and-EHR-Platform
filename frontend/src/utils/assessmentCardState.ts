/**
 * What an assigned assessment's card should say, and where opening it lands.
 *
 * The backend tracks six statuses, but a clinician only ever needs to know one
 * of four things: nobody has started, the questions are under way, the AI
 * report is waiting to be finalized, or the report is signed off. Collapsing
 * them here is what lets the card name the next step instead of a status code.
 */

export type AssessmentCardState =
  | "not-started"
  | "in-progress"
  | "report-draft"
  | "finalized";

/** The two steps of the assessment modal. */
export type AssessmentStep = "questions" | "report";

function normalizeStatus(status?: string | null): string {
  return (status ?? "").trim().toLowerCase().replace(/[\s-]+/g, "_");
}

export function getAssessmentCardState(status?: string | null): AssessmentCardState {
  switch (normalizeStatus(status)) {
    case "completed":
      return "finalized";
    case "waiting_for_therapist":
      return "report-draft";
    case "client_in_progress":
    case "waiting_for_review":
    case "therapist_in_progress":
    case "therapist_completed":
    case "in_progress":
      return "in-progress";
    default:
      return "not-started";
  }
}

/** A report exists once it has been generated, whether or not it is finalized. */
export function hasAssessmentReport(state: AssessmentCardState): boolean {
  return state === "report-draft" || state === "finalized";
}

export const ASSESSMENT_CARD_LABELS: Record<AssessmentCardState, string> = {
  "not-started": "Not started",
  "in-progress": "In progress",
  "report-draft": "Report in draft",
  finalized: "Finalized",
};

/** The wording on the card's button: what pressing it does. */
export const ASSESSMENT_CARD_ACTIONS: Record<AssessmentCardState, string> = {
  "not-started": "Start",
  "in-progress": "Continue",
  "report-draft": "Finish report",
  finalized: "View report",
};

/** One line under the title saying what is left to do. */
export const ASSESSMENT_CARD_NEXT_STEP: Record<AssessmentCardState, string> = {
  "not-started": "Next: answer the questions",
  "in-progress": "Next: finish the questions, then generate the report",
  "report-draft": "Next: review the report and finalize it",
  finalized: "Report finalized and locked",
};

/** Only outstanding work gets the emphasised button. */
export function isAssessmentActionPrimary(state: AssessmentCardState): boolean {
  return state !== "finalized";
}

/** Opening a card lands on the step that still needs doing. */
export function getAssessmentStartStep(state: AssessmentCardState): AssessmentStep {
  return hasAssessmentReport(state) ? "report" : "questions";
}
