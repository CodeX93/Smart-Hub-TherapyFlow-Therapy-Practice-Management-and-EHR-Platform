import { GripHorizontal } from "lucide-react";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "@/components/form/CustomSelect";
import type {
  AssessmentQuestion,
  QuestionType,
} from "../../types/create-assessment";
import { QUESTION_TYPE_OPTIONS } from "@/pages/admin/content/content.static";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import QuestionOptionsSection from "./QuestionOptionsSection";
import QuestionFooter from "./QuestionFooter";

interface AssessmentQuestionItemProps {
  question: AssessmentQuestion;
  index: number;
  onUpdate: (updates: Partial<AssessmentQuestion>) => void;
  onDuplicate: () => void;
  onDelete: () => void;
  enableScoring?: boolean;
}

const ICON_MAPPING: Record<string, React.ReactNode> = {
  "Short Answer Text": <img src="/assets/short.png" alt="short" />,
  "Long Answer Text": <img src="/assets/long.png" alt="long" />,
  "Multiple Choice": <img src="/assets/multi.png" alt="multiple" />,
  "Single Choice": <img src="/assets/single.png" alt="single" />,
  "Rating Scale": <img src="/assets/rating.png" alt="rating" />,
  Date: <img src="/assets/calendar.png" alt="date" />,
  Number: <img src="/assets/number.png" alt="number" />,
};

const AssessmentQuestionItem = ({
  question,
  index,
  onUpdate,
  onDuplicate,
  onDelete,
  enableScoring = false,
}: AssessmentQuestionItemProps) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging: isQuestionDragging,
  } = useSortable({ id: question.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isQuestionDragging ? 50 : "auto",
    opacity: isQuestionDragging ? 0.5 : 1,
  };
  const showOptions = [
    "Multiple Choice",
    "Single Choice",
    "Rating Scale",
  ].includes(question.type);

  const questionTypeOptions = QUESTION_TYPE_OPTIONS.map((opt) => ({
    ...opt,
    icon: ICON_MAPPING[opt.value],
  }));

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="flex flex-col gap-2 group/question bg-white rounded-xl border border-(--neutral-100) overflow-hidden shadow-xs"
    >
      <div className="flex items-center relative px-4 pt-4">
        <span className="text-sm font-semibold text-(--text-primary-dark)">
          Question {index + 1}
        </span>
        <div
          {...attributes}
          {...listeners}
          className="absolute left-1/2 -translate-x-1/2 opacity-0 group-hover/question:opacity-100 transition-opacity duration-300 p-1 hover:bg-(--neutral-100) rounded cursor-grab active:cursor-grabbing"
        >
          <GripHorizontal size={18} className="text-(--text-neutral-400)" />
        </div>
      </div>
      <div>
        <div className="flex flex-col gap-4 px-4">
          <div className="flex gap-4">
            <div className="flex-1">
              <CustomInput
                label="Question Text"
                value={question.text}
                onChange={(e) => onUpdate({ text: e.target.value })}
                className="bg-white"
              />
            </div>
            <div className="w-64">
              <CustomSelect
                label="Question Type"
                options={questionTypeOptions}
                value={question.type}
                onChange={(val) => onUpdate({ type: val as QuestionType })}
                className="bg-white"
                isSearch={false}
              />
            </div>
          </div>

          {showOptions && (
            <QuestionOptionsSection
              question={question}
              enableScoring={enableScoring}
              onUpdate={onUpdate}
            />
          )}
        </div>

        <QuestionFooter
          required={question.required}
          onUpdateRequired={(checked) => onUpdate({ required: checked })}
          onDuplicate={onDuplicate}
          onDelete={onDelete}
        />
      </div>
    </div>
  );
};

export default AssessmentQuestionItem;
