import type { AssessmentQuestion } from "../../../../pages/therapist/therapist.static";
import { Checkbox } from "@/components/ui/checkbox";

interface MultiCheckboxQuestionProps {
  question: AssessmentQuestion;
  value: string[];
  onChange: (value: string[]) => void;
}

const MultiCheckboxQuestion = ({
  question,
  value = [],
  onChange,
}: MultiCheckboxQuestionProps) => {
  const handleToggle = (option: string) => {
    const newValue = value.includes(option)
      ? value.filter((v) => v !== option)
      : [...value, option];
    onChange(newValue);
  };

  return (
    <div className="min-w-0 space-y-3">
      <span className="block text-base font-medium text-(--text-primary-dark) break-words [overflow-wrap:anywhere]">
        {question.label}{" "}
        {question.required && <span className="text-red-500">*</span>}
      </span>
      <div className="space-y-3">
        {(question.options || []).map((option) => (
          <label
            key={option}
            className="group flex w-full min-w-0 cursor-pointer items-start gap-3"
          >
            <Checkbox
              id={`${question.id}-${option}`}
              checked={value.includes(option)}
              onCheckedChange={() => handleToggle(option)}
              className="mt-0.5 shrink-0"
            />
            <span className="min-w-0 flex-1 break-words text-base text-[--text-neutral-600] transition-colors [overflow-wrap:anywhere] group-hover:text-[--text-primary-dark]">
              {option.split("__opt_")[0]}
            </span>
          </label>
        ))}
      </div>
    </div>
  );
};

export default MultiCheckboxQuestion;
