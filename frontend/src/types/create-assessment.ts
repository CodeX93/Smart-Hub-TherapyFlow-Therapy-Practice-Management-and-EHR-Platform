export type AccessLevel = "Shared" | "Therapist Only" | "Client Only";

export type ReportSectionType =
  | "None (Regular Section)"
  | "Referral Reason"
  | "Presenting Symptoms"
  | "Background History"
  | "Mental Status Exam"
  | "Risk Assessment"
  | "Treatment Recommendations"
  | "Goals & Objectives"
  | "Summary & Impressions"
  | "Objective Findings";

export type QuestionType =
  | "Short Answer Text"
  | "Long Answer Text"
  | "Multiple Choice"
  | "Single Choice"
  | "Rating Scale"
  | "Date"
  | "Number";

export interface AssessmentOption {
  id: string;
  text: string;
  score?: number;
}

export interface AssessmentQuestion {
  id: string;
  text: string;
  type: QuestionType;
  required: boolean;
  options?: AssessmentOption[];
}

export interface AssessmentSection {
  id: string;
  title: string;
  accessLevel: AccessLevel;
  description: string;
  reportSectionType: ReportSectionType;
  enableScoring: boolean;
  aiReportInstructions: string;
  questions: AssessmentQuestion[];
  isExpanded?: boolean;
}
