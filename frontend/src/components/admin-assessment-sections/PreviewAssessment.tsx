import { ChevronDown } from "lucide-react";
import {
  DateQuestion,
  RadioQuestion,
  TextQuestion,
  VoiceInputQuestion,
  MultiCheckboxQuestion,
} from "../assessments/take-assessment/questions";
import type { AccessLevel, AssessmentSection } from "../../types/create-assessment";
import { cn } from "../../lib/utils";

function formatAccessLevelLabel(value: string): AccessLevel {
  const normalized = value.trim().toLowerCase().replace(/[\s-]+/g, "_");
  if (normalized === "therapist_only") return "Therapist Only";
  if (normalized === "client_only") return "Client Only";
  if (normalized === "shared") return "Shared";
  if (value === "Therapist Only" || value === "Client Only" || value === "Shared") {
    return value;
  }
  return "Shared";
}

interface AssessmentPreviewProps {
  sections: AssessmentSection[];
  expandedSectionId: string | null;
  toggleSection: (sectionId: string) => void;
}

const PreviewAssessment = ({
  sections,
  expandedSectionId,
  toggleSection,
}: AssessmentPreviewProps) => {
  return (
    <div className="min-w-0">
      <div className="bg-(--light-blue) p-4 rounded-xl border border-(--light-blue-4) mb-5">
        <h1 className="font-semibold text-(--text-primary-dark)">
          Assessment Preview
        </h1>
        <p className="text-sm text-(--text-neutral-600)">
          This is how your assessment will appear to users. You can see the
          layout and test the functionality.
        </p>
      </div>

      <div className="flex min-w-0 flex-col" onClick={(e) => e.stopPropagation()}>
        {/* List Content */}
        <div className="space-y-4 min-w-0">
          <div className="space-y-3 min-w-0">
            {sections.map((section) => {
              const isExpanded = expandedSectionId === section.id;
              const accessLevelLabel = formatAccessLevelLabel(section.accessLevel);

              return (
                <div
                  key={section.id}
                  className={cn(
                    "bg-white rounded-xl border transition-all duration-200 overflow-hidden",
                    isExpanded
                      ? "border-(--neutral-100)"
                      : "border-(--neutral-100) hover:border-(--neutral-200)",
                  )}
                >
                  <div
                    onClick={() => toggleSection(section.id)}
                    className="flex min-w-0 cursor-pointer flex-col gap-2 p-4"
                  >
                    <div className="flex min-w-0 items-start justify-between gap-4">
                      <div className="flex min-w-0 flex-1 items-start gap-2">
                        <ChevronDown
                          size={18}
                          className={cn(
                            "mt-0.5 shrink-0 text-(--text-primary-dark) transition-transform duration-200",
                            isExpanded && "rotate-180",
                          )}
                        />
                        <span
                          className="font-medium text-(--text-primary-dark) break-words [overflow-wrap:anywhere]"
                          title={section.title || "Untitled Section"}
                        >
                          {section.title || "Untitled Section"}
                        </span>
                      </div>
                      <div className="flex shrink-0 items-center gap-3">
                        <span
                          className={cn(
                            "px-3 py-1 text-xs font-bold rounded-full border border-(--neutral-100)",
                            accessLevelLabel === "Therapist Only" &&
                              "bg-(--light-blue) text-(--status-billed)",
                            accessLevelLabel === "Client Only" &&
                              "bg-(--status-completed-light) text-(--dark-green)",
                            accessLevelLabel === "Shared" &&
                              "bg-(--neutral-100) text-(--text-primary-dark)",
                          )}
                        >
                          {accessLevelLabel}
                        </span>
                        <div className="border border-(--neutral-100) h-4" />
                        <span className="text-xs text-(--text-primary-dark) font-bold border border-(--neutral-100) px-2 py-1 rounded-md bg-white">
                          {section.questions.length} Questions
                        </span>
                      </div>
                    </div>
                    <span className="w-full min-w-0 text-sm text-(--text-neutral-600) break-words [overflow-wrap:anywhere]">
                      {section.description || "Untitled Section"}
                    </span>
                  </div>

                  <div
                    className={cn(
                      "grid transition-all duration-300 ease-in-out",
                      isExpanded
                        ? "grid-rows-[1fr] opacity-100"
                        : "grid-rows-[0fr] opacity-0",
                    )}
                  >
                    <div className="overflow-hidden">
                      <div className="px-5 pb-5 space-y-8">
                        {section.questions.map((question, qIndex) => {
                          // Map Create Assessment types to Take Assessment types
                          const mappedQuestion = {
                            ...question,
                            label: question.text,
                            options: question.options?.map((o) => o.text),
                            type: (() => {
                              switch (question.type) {
                                case "Short Answer Text":
                                  return "text";
                                case "Long Answer Text":
                                  return "textarea";
                                case "Single Choice":
                                  return "radio";
                                case "Multiple Choice":
                                  return "checkbox";
                                case "Rating Scale":
                                  return "rating";
                                case "Date":
                                  return "date";
                                default:
                                  return "text";
                              }
                              // eslint-disable-next-line @typescript-eslint/no-explicit-any
                            })() as any, // Cast to any because of complex mapping, but components use strict question types
                          };

                          return (
                            <div
                              key={question.id}
                              className="pointer-events-none flex w-full min-w-0 gap-2 rounded-2xl border border-(--neutral-100) bg-(--bg-primary-light) p-4"
                            >
                              <div className="mt-1.5 shrink-0 text-xs font-bold text-(--text-neutral-600)">
                                {qIndex + 1}.
                              </div>
                              <div className="min-w-0 flex-1">
                                {mappedQuestion.type === "text" ||
                                mappedQuestion.type === "textarea" ? (
                                  <TextQuestion
                                    question={mappedQuestion}
                                    value=""
                                    onChange={() => {}}
                                  />
                                ) : mappedQuestion.type === "date" ? (
                                  <DateQuestion
                                    question={mappedQuestion}
                                    value={undefined}
                                    onChange={() => {}}
                                  />
                                ) : mappedQuestion.type === "radio" ||
                                  mappedQuestion.type === "rating" ? (
                                  <RadioQuestion
                                    question={mappedQuestion}
                                    value=""
                                    onChange={() => {}}
                                  />
                                ) : mappedQuestion.type === "voice" ? (
                                  <RadioQuestion
                                    question={mappedQuestion}
                                    value=""
                                    onChange={() => {}}
                                  />
                                ) : mappedQuestion.type === "checkbox" ? (
                                  <MultiCheckboxQuestion
                                    question={mappedQuestion}
                                    value={[]}
                                    onChange={() => {}}
                                  />
                                ) : mappedQuestion.type === "voice" ? (
                                  <VoiceInputQuestion
                                    question={mappedQuestion}
                                    value=""
                                    onChange={() => {}}
                                  />
                                ) : (
                                  <div className="text-sm text-(--text-neutral-400) italic">
                                    Question type "{question.type}" preview not
                                    available.
                                  </div>
                                )}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PreviewAssessment;
