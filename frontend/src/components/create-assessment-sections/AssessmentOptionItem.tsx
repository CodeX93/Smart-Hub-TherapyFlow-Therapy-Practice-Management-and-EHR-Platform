import { GripVertical, X } from "lucide-react";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import type { AssessmentOption } from "../../types/create-assessment";

interface AssessmentOptionItemProps {
  option: AssessmentOption;
  index: number;
  questionType: string;
  enableScoring: boolean;
  onUpdate: (updates: Partial<AssessmentOption>) => void;
  onDelete: () => void;
}

const AssessmentOptionItem = ({
  option,
  index,
  questionType,
  enableScoring,
  onUpdate,
  onDelete,
}: AssessmentOptionItemProps) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: option.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isDragging ? 50 : "auto",
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="flex items-center gap-3 group/option bg-white"
    >
      <div
        {...attributes}
        {...listeners}
        className="flex items-center gap-2 opacity-0 group-hover/option:opacity-100 transition-opacity p-1 hover:bg-(--neutral-100) rounded cursor-grab active:cursor-grabbing"
      >
        <GripVertical size={16} className="text-(--text-neutral-300)" />
      </div>

      {questionType !== "Rating Scale" && (
        <div className="flex items-center gap-2">
          {questionType === "Multiple Choice" ? (
            <div className="size-5 border-2 border-(--neutral-200) rounded-sm" />
          ) : (
            <div className="size-5 border-2 border-(--neutral-200) rounded-full" />
          )}
        </div>
      )}

      <div className="flex-1">
        <input
          type="text"
          value={option.text}
          onChange={(e) => onUpdate({ text: e.target.value })}
          placeholder={`Option ${index + 1}`}
          className="w-full text-sm font-medium text-(--text-primary-dark) border-b-2 border-transparent focus:border-(--text-primary-500) outline-none py-1 transition-colors"
        />
      </div>

      {enableScoring && (
        <div className="flex items-center gap-2">
          <span className="text-sm text-(--text-primary-dark)">Score:</span>
          <input
            type="number"
            value={option.score}
            onChange={(e) => onUpdate({ score: Number(e.target.value) })}
            className="w-16 h-8 text-sm text-center text-(--text-primary-dark) border border-(--neutral-100) rounded-lg outline-none focus:border-(--text-primary-500)"
          />
        </div>
      )}

      <button
        onClick={onDelete}
        className="p-1.5 text-(--text-neutral-300) hover:bg-(--neutral-100) cursor-pointer rounded-full transition-all duration-200"
      >
        <X size={16} />
      </button>
    </div>
  );
};

export default AssessmentOptionItem;
