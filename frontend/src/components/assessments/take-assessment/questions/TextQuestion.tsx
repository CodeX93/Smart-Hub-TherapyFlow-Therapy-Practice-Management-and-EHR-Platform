import type { AssessmentQuestion } from "../../../../pages/therapist/therapist.static";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomInput from "@/components/form/CustomInput";
import AssessmentVoiceField from "./AssessmentVoiceField";

interface TextQuestionProps {
  question: AssessmentQuestion;
  value: string;
  onChange: (value: string) => void;
  /** When false, renders without mic (e.g. static previews). Default true. */
  enableVoice?: boolean;
}

const TextQuestion = ({
  question,
  value,
  onChange,
  enableVoice = true,
}: TextQuestionProps) => {
  const isTextarea = question.type === "textarea";

  const field = isTextarea ? (
    <div className="min-w-0">
      <CustomTextarea
        label={question.label}
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
        className="min-w-0"
      />
    </div>
  ) : (
    <div className="min-w-0">
      <CustomInput
        label={question.label}
        type="text"
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
        className="min-w-0"
      />
    </div>
  );

  if (!enableVoice) {
    return <div className="min-w-0 space-y-3">{field}</div>;
  }

  return (
    <AssessmentVoiceField
      value={value || ""}
      onChange={onChange}
      align={isTextarea ? "start" : "center"}
    >
      {field}
    </AssessmentVoiceField>
  );
};

export default TextQuestion;
