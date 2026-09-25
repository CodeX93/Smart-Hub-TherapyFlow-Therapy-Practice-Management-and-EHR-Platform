import type { AssessmentQuestion } from "../../../../pages/therapist/therapist.static";
import CustomDatePicker from "@/components/form/CustomDatePicker";

interface DateQuestionProps {
  question: AssessmentQuestion;
  value: Date | undefined;
  onChange: (date: Date | undefined) => void;
}

const DateQuestion = ({ question, value, onChange }: DateQuestionProps) => {
  return (
    <div className="min-w-0 space-y-3">
      <span className="block text-base font-medium text-(--text-primary-dark) break-words [overflow-wrap:anywhere]">
        {question.label}
      </span>
      {/* Match text/textarea width: leave room for the voice mic column (gap-3 + size-6). */}
      <div className="flex gap-3">
        <div className="min-w-0 flex-1">
          <CustomDatePicker
            label="Select date"
            date={value}
            onDateChange={(d) => onChange(d || undefined)}
          />
        </div>
        <div className="size-6 shrink-0" aria-hidden />
      </div>
    </div>
  );
};

export default DateQuestion;
