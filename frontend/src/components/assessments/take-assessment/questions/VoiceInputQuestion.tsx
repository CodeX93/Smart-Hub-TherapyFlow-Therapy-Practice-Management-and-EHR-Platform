import type { AssessmentQuestion } from "../../../../pages/therapist/therapist.static";
import CustomInput from "@/components/form/CustomInput";
import AssessmentVoiceField from "./AssessmentVoiceField";

interface VoiceInputQuestionProps {
  question: AssessmentQuestion;
  value: string;
  onChange: (value: string) => void;
}

const VoiceInputQuestion = ({
  question,
  value,
  onChange,
}: VoiceInputQuestionProps) => {
  return (
    <AssessmentVoiceField value={value || ""} onChange={onChange}>
      <CustomInput
        label={question.label}
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
      />
    </AssessmentVoiceField>
  );
};

export default VoiceInputQuestion;
