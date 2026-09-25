import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import type {
  AssessmentQuestion,
  AssessmentOption,
} from "../../types/create-assessment";
import AssessmentOptionItem from "./AssessmentOptionItem";

interface QuestionOptionsSectionProps {
  question: AssessmentQuestion;
  enableScoring: boolean;
  onUpdate: (updates: Partial<AssessmentQuestion>) => void;
}

const generateId = () => Math.random().toString(36).substr(2, 9);

const QuestionOptionsSection = ({
  question,
  enableScoring,
  onUpdate,
}: QuestionOptionsSectionProps) => {
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const onAddOption = () => {
    const newOption: AssessmentOption = {
      id: generateId(),
      text: "",
      score: 0,
    };
    onUpdate({
      options: [...(question.options || []), newOption],
    });
  };

  const onUpdateOption = (
    optId: string,
    updates: Partial<AssessmentOption>,
  ) => {
    onUpdate({
      options: question.options?.map((opt) =>
        opt.id === optId ? { ...opt, ...updates } : opt,
      ),
    });
  };

  const onDeleteOption = (optId: string) => {
    onUpdate({
      options: question.options?.filter((opt) => opt.id !== optId),
    });
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      const oldIndex =
        question.options?.findIndex((opt) => opt.id === active.id) ?? -1;
      const newIndex =
        question.options?.findIndex((opt) => opt.id === over.id) ?? -1;

      if (oldIndex !== -1 && newIndex !== -1) {
        onUpdate({
          options: arrayMove(question.options!, oldIndex, newIndex),
        });
      }
    }
  };

  return (
    <div className="flex flex-col gap-3">
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext
          items={(question.options || []).map((opt) => opt.id)}
          strategy={verticalListSortingStrategy}
        >
          <div className="flex flex-col gap-3">
            {question.options?.map((option, oIdx) => (
              <AssessmentOptionItem
                key={option.id}
                option={option}
                index={oIdx}
                questionType={question.type}
                enableScoring={enableScoring}
                onUpdate={(updates) => onUpdateOption(option.id, updates)}
                onDelete={() => onDeleteOption(option.id)}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>

      <Button
        variant="ghost"
        onClick={onAddOption}
        className="flex items-center gap-2 text-(--text-primary-500) hover:text-(--bg-primary-dark) bg-transparent hover:bg-transparent w-fit p-0! h-6 pl-8! cursor-pointer font-semibold"
      >
        <Plus size={20} />
        Add option
      </Button>
    </div>
  );
};

export default QuestionOptionsSection;
