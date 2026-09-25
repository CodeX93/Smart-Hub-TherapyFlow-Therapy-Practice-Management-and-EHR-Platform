import type { AssessmentQuestion } from "../../../../pages/therapist/therapist.static";
import CustomRadio from "@/components/form/CustomRadio";

interface RadioQuestionProps {
  question: AssessmentQuestion;
  value: string;
  onChange: (value: string) => void;
  orientation?: "vertical" | "horizontal";
}

const RadioQuestion = ({
  question,
  value,
  onChange,
  orientation = "vertical",
}: RadioQuestionProps) => {
  return (
    <CustomRadio
      label={question.label}
      options={question.options || []}
      value={value}
      onChange={onChange}
      orientation={orientation}
    />
  );
};

export default RadioQuestion;
