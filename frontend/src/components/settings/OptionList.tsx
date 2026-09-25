
import { ContentLoader } from "@/components/shared/ContentLoader";
import { Plus } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  DndContext,
  closestCenter,
  type DragEndEvent,
  type SensorDescriptor,
  type SensorOptions,
} from "@dnd-kit/core";
import {
  SortableContext,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import SortableOptionItem from "./SortableOptionItem";
import type { OptionCategory, OptionValue } from "@/types/settings.types";

interface OptionListProps {
  selectedCategory: OptionCategory | null;
  isLoading?: boolean;
  onAddOption: () => void;
  onEditOption: (option: OptionValue) => void;
  onDeleteOption: (option: OptionValue, e: React.MouseEvent) => void;
  sensors: SensorDescriptor<SensorOptions>[];
  handleDragEnd: (event: DragEndEvent) => void;
}

const OptionList = ({
  selectedCategory,
  isLoading = false,
  onAddOption,
  onEditOption,
  onDeleteOption,
  sensors,
  handleDragEnd,
}: OptionListProps) => {
  if (!selectedCategory) {
    return (
      <div className="flex-1 text-center">
        <div className="space-y-4 mt-40">
          <div className="flex items-center justify-center mx-auto mb-4">
            <img src="/assets/setting.png" alt="placeholder" />
          </div>
          <h3 className="text-xl font-semibold text-(--text-primary-dark) mb-1">
            Select a category
          </h3>
          <p className="text-(--text-neutral-600) max-w-85 mx-auto">
            Choose a category from the left panel to view and manage its options
            here.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      {/* Category Details Header */}
      <div className="p-4 pb-4 flex items-center justify-between gap-4">
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-xl font-bold text-(--text-primary-dark)">
            {selectedCategory.name} Options
          </h3>
          <p className="truncate text-sm text-(--text-neutral-600) mt-1">
            {selectedCategory.description?.trim() ||
              "No description available for this category."}
          </p>
        </div>
        <Button
          onClick={onAddOption}
          className="shrink-0 rounded-full bg-white border border-(--neutral-200) hover:bg-transparent text-(--bg-primary-dark) px-5 h-10 flex items-center gap-2 cursor-pointer font-semibold"
        >
          <Plus size={18} />
          Add Option
        </Button>
      </div>

      {/* Options List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <div className="space-y-0.5">
          {isLoading ? (
            <div className="flex items-center justify-center py-20 text-sm text-(--text-neutral-500)">
              <ContentLoader variant="inline" size="md" />
              <span className="ml-2">Loading options...</span>
            </div>
          ) : selectedCategory.options.length > 0 ? (
            <DndContext
              sensors={sensors}
              collisionDetection={closestCenter}
              onDragEnd={handleDragEnd}
            >
              <SortableContext
                items={selectedCategory.options.map((o) => o.id)}
                strategy={verticalListSortingStrategy}
              >
                {selectedCategory.options.map((option, idx) => (
                  <SortableOptionItem
                    key={option.id}
                    option={option}
                    isLast={idx === selectedCategory.options.length - 1}
                    onEdit={() => onEditOption(option)}
                    onDelete={!option.isSystem ? (e: React.MouseEvent) => onDeleteOption(option, e) : undefined}
                  />
                ))}
              </SortableContext>
            </DndContext>
          ) : (
            <div className="flex flex-col items-center justify-center py-20 text-center">
              <p className="text-(--text-neutral-500)">
                No options found for this category.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default OptionList;
