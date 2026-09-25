import type {
  AssessmentSection,
  AssessmentAnswer,
} from "../../../pages/therapist/therapist.static";
import {
  TextQuestion,
  DateQuestion,
  RadioQuestion,
  VoiceInputQuestion,
  MultiCheckboxQuestion,
} from "./questions";

interface AssessmentSectionRendererProps {
  section: AssessmentSection;
  answers: Record<string, AssessmentAnswer>;
  onAnswerChange: (questionId: string, value: AssessmentAnswer) => void;
}

const AssessmentSectionRenderer = ({
  section,
  answers,
  onAnswerChange,
}: AssessmentSectionRendererProps) => {
  return (
    <div className="space-y-8 py-2 min-w-0">
      <div className="min-w-0">
        <h3 className="mb-6 border-b border-gray-100 pb-4 text-lg font-semibold text-gray-900 break-words [overflow-wrap:anywhere]">
          {section.title}
        </h3>
      </div>

      <div
        className={
          section.id === "referral-info"
            ? "grid grid-cols-2 gap-x-8 gap-y-6"
            : "space-y-8"
        }
      >
        {section.questions.map((question, index) => {
          // Grid layout logic for Referral Info section
          const isReferralSection = section.id === "referral-info";
          const isHalfWidth =
            isReferralSection && (question.id === "q4" || question.id === "q5");
          const colSpanClass = isReferralSection
            ? isHalfWidth
              ? "col-span-1"
              : "col-span-2"
            : "";

          const shouldShowNumber = !(isReferralSection && question.id === "q5");
          const displayNumber =
            isReferralSection && index > 4 ? index : index + 1;

          const renderQuestion = () => {
            const val = answers[question.id];
            const handleChange = (newVal: AssessmentAnswer) =>
              onAnswerChange(question.id, newVal);

            switch (question.type) {
              case "text":
              case "textarea":
                return (
                  <TextQuestion
                    question={question}
                    value={val as string}
                    onChange={handleChange}
                  />
                );
              case "date":
                return (
                  <DateQuestion
                    question={question}
                    value={val as Date | undefined}
                    onChange={handleChange}
                  />
                );
              case "radio":
              case "rating":
                return (
                  <RadioQuestion
                    question={question}
                    value={val as string}
                    onChange={handleChange}
                    orientation={
                      isReferralSection && question.type === "radio"
                        ? "horizontal"
                        : undefined
                    }
                  />
                );
              case "checkbox":
                return (
                  <MultiCheckboxQuestion
                    question={question}
                    value={Array.isArray(val) ? (val as string[]) : []}
                    onChange={handleChange}
                  />
                );
              case "voice":
                return (
                  <VoiceInputQuestion
                    question={question}
                    value={val as string}
                    onChange={handleChange}
                  />
                );
              default:
                console.warn(`Unknown question type: ${question.type}`);
                return null;
            }
          };

          return (
            <div
              key={question.id}
              className={`flex min-w-0 items-start gap-4 animate-in fade-in slide-in-from-bottom-4 duration-500 ${colSpanClass}`}
              style={{ animationDelay: `${index * 100}ms` }}
            >
              {/* Number Circle */}
              {shouldShowNumber && (
                <div
                  className={`flex-shrink-0 w-8 h-8 rounded-full bg-[#E4E7EC] flex items-center justify-center text-sm font-semibold text-[#475467] ${question.type === "radio" ? "-mt-1" : "mt-3"}`}
                >
                  {displayNumber}
                </div>
              )}

              {/* Question Content */}
              <div className="min-w-0 flex-1">{renderQuestion()}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default AssessmentSectionRenderer;
