import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type {
  AssessmentSection,
  AssessmentQuestion,
} from "../../types/create-assessment";
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
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";

import AssessmentQuestionItem from "./AssessmentQuestionItem";
import SectionHeader from "./SectionHeader";
import SectionConfiguration from "./SectionConfiguration";

interface AssessmentSectionCardProps {
  section: AssessmentSection;
  index: number;
  onUpdate: (updates: Partial<AssessmentSection>) => void;
  onDuplicate: () => void;
  onDelete: () => void;
  onAddQuestion: () => void;
  onUpdateQuestion: (
    qIndex: number,
    updates: Partial<AssessmentQuestion>,
  ) => void;
  onDuplicateQuestion: (qIndex: number) => void;
  onDeleteQuestion: (qIndex: number) => void;
  onOpenReorder: () => void;
  isMultipleSections: boolean;
}

const AssessmentSectionCard = ({
  section,
  index,
  onUpdate,
  onDuplicate,
  onOpenReorder,
  onDelete,
  onAddQuestion,
  onUpdateQuestion,
  onDuplicateQuestion,
  onDeleteQuestion,
  isMultipleSections,
}: AssessmentSectionCardProps) => {
  const [isExpanded, setIsExpanded] = useState(false);

  // For Section reordering (parent context)
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging: isSectionDragging,
  } = useSortable({ id: section.id });

  const sectionStyle = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isSectionDragging ? 50 : "auto",
    opacity: isSectionDragging ? 0.5 : 1,
  };

  // For Question reordering (internal context)
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      const oldIndex = section.questions.findIndex((q) => q.id === active.id);
      const newIndex = section.questions.findIndex((q) => q.id === over.id);

      onUpdate({
        questions: arrayMove(section.questions, oldIndex, newIndex),
      });
    }
  };

  return (
    <div
      ref={setNodeRef}
      style={sectionStyle}
      className="bg-white rounded-3xl shadow-[0_0_10px_0_#1E282E0A] overflow-hidden transition-all duration-300 border-l-4 border-l-(--text-primary-500)"
    >
      <SectionHeader
        section={section}
        index={index}
        isExpanded={isExpanded}
        onToggleExpand={() => setIsExpanded(!isExpanded)}
        onDuplicate={onDuplicate}
        onDelete={onDelete}
        onOpenReorder={onOpenReorder}
        dragAttributes={attributes}
        dragListeners={listeners}
        isMultipleSections={isMultipleSections}
      />

      <div
        className={cn(
          "transition-all duration-300 ease-in-out",
          isExpanded
            ? "max-h-5000 opacity-100"
            : "max-h-0 opacity-0 overflow-hidden",
        )}
      >
        <div className="px-6 pb-4 flex flex-col gap-6 pt-6">
          <SectionConfiguration section={section} onUpdate={onUpdate} />

          {/* Questions Section */}
          <div className="flex flex-col gap-6 pt-4 border-t border-(--neutral-50)">
            <h3 className="text-lg font-bold text-(--text-primary-dark)">
              Questions
            </h3>

            {section.questions.length > 0 && (
              <DndContext
                sensors={sensors}
                collisionDetection={closestCenter}
                onDragEnd={handleDragEnd}
              >
                <SortableContext
                  items={section.questions.map((q) => q.id)}
                  strategy={verticalListSortingStrategy}
                >
                  <div className="flex flex-col gap-8">
                    {section.questions.map((question, qIdx) => (
                      <AssessmentQuestionItem
                        key={question.id}
                        question={question}
                        index={qIdx}
                        onUpdate={(updates) => onUpdateQuestion(qIdx, updates)}
                        onDuplicate={() => onDuplicateQuestion(qIdx)}
                        onDelete={() => onDeleteQuestion(qIdx)}
                        enableScoring={section.enableScoring}
                      />
                    ))}
                  </div>
                </SortableContext>
              </DndContext>
            )}

            <Button
              variant="link"
              onClick={onAddQuestion}
              className="fle items-center gap-2 text-(--text-primary-500) hover:text-(--bg-primary-dark)  w-fit p-0! h-5 mb-2 cursor-pointer font-semibold"
            >
              <Plus size={18} className="cursor-pointer" />
              {section.questions.length === 0
                ? "Add Question"
                : "Add Another Question"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AssessmentSectionCard;
