import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";
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
import AssessmentSectionCard from "@/components/create-assessment-sections/AssessmentSectionCard";
import type {
  AssessmentSection,
  AssessmentQuestion,
} from "../../types/create-assessment";

interface AssessmentSectionsListProps {
  sections: AssessmentSection[];
  onAddSection: () => void;
  onUpdateSection: (sIdx: number, updates: Partial<AssessmentSection>) => void;
  onDuplicateSection: (sIdx: number) => void;
  onDeleteSection: (sIdx: number) => void;
  onOpenReorder: () => void;
  onAddQuestion: (sIdx: number) => void;
  onUpdateQuestion: (
    sIdx: number,
    qIdx: number,
    updates: Partial<AssessmentQuestion>,
  ) => void;
  onDuplicateQuestion: (sIdx: number, qIdx: number) => void;
  onDeleteQuestion: (sIdx: number, qIdx: number) => void;
  setSections: (sections: AssessmentSection[]) => void;
}

const AssessmentSectionsList = ({
  sections,
  onAddSection,
  onUpdateSection,
  onDuplicateSection,
  onDeleteSection,
  onOpenReorder,
  onAddQuestion,
  onUpdateQuestion,
  onDuplicateQuestion,
  onDeleteQuestion,
  setSections,
}: AssessmentSectionsListProps) => {
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      const oldIndex = sections.findIndex((s) => s.id === active.id);
      const newIndex = sections.findIndex((s) => s.id === over.id);

      setSections(arrayMove(sections, oldIndex, newIndex));
    }
  };

  return (
    <div className="flex flex-col gap-6 mt-4">
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext
          items={sections.map((s) => s.id)}
          strategy={verticalListSortingStrategy}
        >
          {sections.map((section, sIdx) => (
            <AssessmentSectionCard
              key={section.id}
              section={section}
              index={sIdx}
              onUpdate={(updates) => onUpdateSection(sIdx, updates)}
              onDuplicate={() => onDuplicateSection(sIdx)}
              onDelete={() => onDeleteSection(sIdx)}
              onOpenReorder={onOpenReorder}
              onAddQuestion={() => onAddQuestion(sIdx)}
              onUpdateQuestion={(qIdx, updates) =>
                onUpdateQuestion(sIdx, qIdx, updates)
              }
              onDuplicateQuestion={(qIdx) => onDuplicateQuestion(sIdx, qIdx)}
              onDeleteQuestion={(qIdx) => onDeleteQuestion(sIdx, qIdx)}
              isMultipleSections={sections?.length > 1}
            />
          ))}
        </SortableContext>
      </DndContext>

      <Button
        variant="outline"
        onClick={onAddSection}
        className="border border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--bg-primary-dark) hover:text-white transition-all duration-300 ease-in-out cursor-pointer rounded-full px-8 font-semibold h-10 w-fit"
      >
        <Plus size={18} />
        Add Another Section
      </Button>
    </div>
  );
};

export default AssessmentSectionsList;
