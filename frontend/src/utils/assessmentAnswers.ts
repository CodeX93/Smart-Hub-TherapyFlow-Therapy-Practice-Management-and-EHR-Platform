import type { AssessmentAnswer } from "@/pages/therapist/therapist.static";

export function hasAssessmentAnswer(answer: AssessmentAnswer | undefined): boolean {
  if (answer === undefined || answer === null) return false;
  if (typeof answer === "string") return answer.trim().length > 0;
  if (answer instanceof Date) return !Number.isNaN(answer.getTime());
  if (Array.isArray(answer)) return answer.length > 0;
  return true;
}

export function countAnsweredQuestions(
  answers: Record<string, AssessmentAnswer>,
): number {
  return Object.values(answers).filter(hasAssessmentAnswer).length;
}
