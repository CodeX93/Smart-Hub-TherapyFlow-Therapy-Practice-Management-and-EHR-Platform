import { useState, useEffect } from "react";
import { X, GripVertical, MoveUp, MoveDown } from "lucide-react";
import { Button } from "../ui/button";
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
import type { AssessmentSection } from "../../types/create-assessment";
import { cn } from "@/lib/utils";

interface ReorderSectionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  sections: AssessmentSection[];
  onSave: (sections: AssessmentSection[]) => void;
}

const SortableSectionItem = ({
  section,
  index,
  total,
  onMoveUp,
  onMoveDown,
}: {
  section: AssessmentSection;
  index: number;
  total: number;
  onMoveUp: () => void;
  onMoveDown: () => void;
}) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: section.id });

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
      className={cn(
        "flex items-center justify-between p-3 bg-white border-b border-(--neutral-100)",
        isDragging && "shadow-lg rounded-lg border-0",
      )}
    >
      <div className="flex items-center gap-4">
        <div
          {...attributes}
          {...listeners}
          className="p-1 hover:bg-(--neutral-100) rounded cursor-grab active:cursor-grabbing text-(--text-neutral-300) hover:text-(--text-primary-dark) transition-colors"
        >
          <GripVertical size={20} />
        </div>
        <div className="flex flex-col gap-1 min-w-0">
          <span
            className="text-sm font-semibold text-(--text-primary-dark) truncate max-w-[17.5rem]"
            title={section.title || "Untitled Section"}
          >
            {section.title || "Untitled Section"}
          </span>
          <span className="text-sm text-(--text-neutral-600)">
            Section {index + 1} of {total}
          </span>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={onMoveDown}
          disabled={index === total - 1}
          className="p-1.5 text-(--text-neutral-400) hover:bg-(--neutral-50) disabled:opacity-30 disabled:hover:bg-transparent rounded-full transition-colors cursor-pointer"
        >
          <MoveDown size={18} />
        </button>
        <button
          onClick={onMoveUp}
          disabled={index === 0}
          className="p-1.5 text-(--text-neutral-400) hover:bg-(--neutral-50) disabled:opacity-30 disabled:hover:bg-transparent rounded-full transition-colors cursor-pointer"
        >
          <MoveUp size={18} />
        </button>
      </div>
    </div>
  );
};

const ReorderSectionsModal = ({
  isOpen,
  onClose,
  sections: initialSections,
  onSave,
}: ReorderSectionsModalProps) => {
  const [sections, setSections] =
    useState<AssessmentSection[]>(initialSections);

  useEffect(() => {
    if (isOpen) {
      setTimeout(() => {
        setSections(initialSections);
      }, 0);
    }
  }, [isOpen, initialSections]);

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

  const moveUp = (index: number) => {
    if (index === 0) return;
    setSections(arrayMove(sections, index, index - 1));
  };

  const moveDown = (index: number) => {
    if (index === sections.length - 1) return;
    setSections(arrayMove(sections, index, index + 1));
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 backdrop-blur-xs p-2">
      <div
        className="bg-white w-full max-w-xl rounded-2xl shadow-2xl flex flex-col max-h-[90vh]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-5">
          <h2 className="text-xl font-bold text-(--text-primary-dark)">
            Reorder Sections
          </h2>
          <button
            onClick={onClose}
            className="p-1 hover:bg-(--neutral-100) rounded-full transition-colors text-(--text-neutral-400) hover:text-(--text-primary-dark) cursor-pointer"
          >
            <X size={24} />
          </button>
        </div>

        {/* List Content */}
        <div className="flex-1 overflow-y-auto px-5">
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <SortableContext
              items={sections.map((s) => s.id)}
              strategy={verticalListSortingStrategy}
            >
              <div className="flex flex-col">
                {sections.map((section, index) => (
                  <SortableSectionItem
                    key={section.id}
                    section={section}
                    index={index}
                    total={sections.length}
                    onMoveUp={() => moveUp(index)}
                    onMoveDown={() => moveDown(index)}
                  />
                ))}
              </div>
            </SortableContext>
          </DndContext>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-5 py-6">
          <Button
            variant="outline"
            onClick={onClose}
            className="rounded-full px-8 h-12 text-sm font-semibold border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--bg-primary-light) cursor-pointer"
          >
            Cancel
          </Button>
          <Button
            onClick={() => onSave(sections)}
            className="rounded-full px-10 h-12 text-sm font-semibold bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white cursor-pointer"
          >
            Save
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ReorderSectionsModal;
