import type { SessionNoteTranscriptionResponse } from "@/store/api/admin/sessionNotes.api";

const SENTENCE_SPLIT_REGEX = /(?<=[.!?])\s+|\n+/;

function normalizeText(value: string) {
  return value.replace(/\s+/g, " ").trim();
}

function splitSentences(value: string) {
  return value
    .split(SENTENCE_SPLIT_REGEX)
    .map((part) => normalizeText(part))
    .filter(Boolean);
}

function pickSentencesByKeywords(sentences: string[], keywords: string[], fallbackCount = 0) {
  const matched = sentences.filter((sentence) => {
    const lower = sentence.toLowerCase();
    return keywords.some((keyword) => lower.includes(keyword));
  });

  if (matched.length > 0) {
    return matched.join(" ");
  }

  return fallbackCount > 0 ? sentences.slice(0, fallbackCount).join(" ") : "";
}

export function buildTranscriptFieldMapping(
  rawTranscription: string,
): SessionNoteTranscriptionResponse["mappedFields"] {
  const transcript = normalizeText(rawTranscription);
  const sentences = splitSentences(rawTranscription);

  const sessionFocus = pickSentencesByKeywords(
    sentences,
    ["today", "discuss", "session", "focus", "topic", "planning", "complete", "working on"],
    2,
  );
  const symptoms = pickSentencesByKeywords(sentences, [
    "anxious",
    "anxiety",
    "depressed",
    "depression",
    "stress",
    "stressed",
    "panic",
    "sleep",
    "mood",
    "angry",
    "sad",
    "worry",
    "symptom",
    "hot",
    "tired",
  ]);
  const shortTermGoals = pickSentencesByKeywords(sentences, [
    "goal",
    "plan",
    "next week",
    "next session",
    "complete",
    "finish",
    "working day",
    "monday",
    "tuesday",
  ]);
  const intervention = pickSentencesByKeywords(sentences, [
    "review",
    "record session",
    "complete",
    "plan",
    "discuss",
    "working on",
    "deployed",
  ]);
  const progress = pickSentencesByKeywords(sentences, [
    "complete",
    "completed",
    "done",
    "working",
    "progress",
    "deploy",
    "deployed",
  ]);
  const recommendations = pickSentencesByKeywords(sentences, [
    "recommend",
    "follow up",
    "next week",
    "next session",
    "plan",
    "should",
    "will be",
  ]);

  return {
    sessionFocus: sessionFocus || transcript,
    symptoms,
    shortTermGoals,
    intervention,
    progress,
    remarks: transcript,
    recommendations,
  };
}

export const EMPTY_SMART_FILL_FIELDS_MESSAGE =
  "No structured session note fields could be extracted from this transcript. The recording does not contain definable clinical content such as session focus, symptoms, interventions, progress, recommendations, mood ratings, or risk indicators. Please record an actual therapy session conversation and try again.";

export function hasExtractedSmartFillFields(
  mappedFields?: Record<string, string> | null,
): boolean {
  if (!mappedFields) return false;
  return Object.values(mappedFields).some((value) => value?.trim());
}

export function getSmartFillEmptyMessage(message?: string | null): string {
  const trimmed = message?.trim();
  return trimmed || EMPTY_SMART_FILL_FIELDS_MESSAGE;
}
