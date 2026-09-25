/**
 * Starter formats offered when a practice has no AI templates yet.
 *
 * Without these the first final note is blocked behind writing AI instructions
 * from scratch. Picking one creates a normal template the practice then owns
 * and can edit or delete like any other.
 */

export interface SessionNoteStarterTemplate {
  key: string;
  name: string;
  summary: string;
  instructions: string;
}

export const SESSION_NOTE_STARTER_TEMPLATES: SessionNoteStarterTemplate[] = [
  {
    key: "soap",
    name: "SOAP progress note",
    summary: "Subjective · Objective · Assessment · Plan",
    instructions: [
      "Write the session note in SOAP format with four headed sections:",
      "",
      "Subjective — what the client reported in their own terms, including mood, symptoms and any events since the last session.",
      "Objective — what was observed: presentation, affect, engagement, and anything measurable.",
      "Assessment — clinical impression, progress against the stated goals, and any risk considerations.",
      "Plan — interventions to continue, homework agreed, and what the next session will address.",
      "",
      "Use the clinical fields provided. Write in the third person, past tense, in plain professional language. Do not invent details that are not in the fields, and where a field is empty say nothing rather than speculating.",
    ].join("\n"),
  },
  {
    key: "dap",
    name: "DAP progress note",
    summary: "Data · Assessment · Plan",
    instructions: [
      "Write the session note in DAP format with three headed sections:",
      "",
      "Data — what the client reported and what was observed during the session, combined into one factual account.",
      "Assessment — clinical interpretation of that data, progress against goals, and any risk considerations.",
      "Plan — interventions to continue, homework agreed, and the focus of the next session.",
      "",
      "Use the clinical fields provided. Write in the third person, past tense, in plain professional language. Do not invent details that are not in the fields, and where a field is empty say nothing rather than speculating.",
    ].join("\n"),
  },
  {
    key: "narrative",
    name: "Narrative summary",
    summary: "Flowing prose, no headed sections",
    instructions: [
      "Write the session note as a short narrative summary in flowing prose, without headed sections.",
      "",
      "Cover, in this order: what the session focused on, what the client reported, what was observed, the interventions used, progress against the stated goals, and the agreed next steps.",
      "",
      "Aim for three to five paragraphs. Use the clinical fields provided, write in the third person and past tense, and do not invent details that are not in the fields.",
    ].join("\n"),
  },
];
