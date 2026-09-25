import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  PointerSensor,
} from "@dnd-kit/core";
import {
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import type { Control, UseFieldArrayReturn } from "react-hook-form";
import type { CreateTemplateValues } from "@/schemas/admin-checklist.schemas";
import DraggableChecklistItem from "./DraggableChecklistItem";

interface CustomItemsListProps {
  control: Control<CreateTemplateValues>;
  fieldArray: UseFieldArrayReturn<CreateTemplateValues, "customItems">;
  expandedItems: Set<string>;
  setExpandedItems: (items: Set<string>) => void;
}

const CustomItemsList = ({
  control,
  fieldArray,
  expandedItems,
  setExpandedItems,
}: CustomItemsListProps) => {
  const { fields, move, remove } = fieldArray;

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      const oldIndex = fields.findIndex((item) => item.id === active.id);
      const newIndex = fields.findIndex((item) => item.id === over.id);
      move(oldIndex, newIndex);
    }
  };

  const handleMoveUp = (index: number) => {
    if (index === 0) return;
    move(index, index - 1);
  };

  const handleMoveDown = (index: number) => {
    if (index === fields.length - 1) return;
    move(index, index + 1);
  };

  const toggleItemExpanded = (id: string) => {
    const newExpanded = new Set(expandedItems);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedItems(newExpanded);
  };

  return (
    <div>
      <h3 className="text-sm font-semibold text-(--text-primary-dark) mb-4">
        Custom Checklist Items
      </h3>
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext
          items={fields.map((item) => item.id)}
          strategy={verticalListSortingStrategy}
        >
          <div className="space-y-3">
            {fields.map((item, index) => (
              <DraggableChecklistItem
                key={item.id}
                control={control}
                item={item}
                index={index}
                isFirst={index === 0}
                isLast={index === fields.length - 1}
                isExpanded={expandedItems.has(item.id)}
                onToggleExpanded={() => toggleItemExpanded(item.id)}
                onMoveUp={() => handleMoveUp(index)}
                onMoveDown={() => handleMoveDown(index)}
                onRemove={() => remove(index)}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>
    </div>
  );
};

export default CustomItemsList;
