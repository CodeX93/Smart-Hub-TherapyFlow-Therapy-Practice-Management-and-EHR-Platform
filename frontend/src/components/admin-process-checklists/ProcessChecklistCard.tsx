import { ContentLoader } from "@/components/shared/ContentLoader";
import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { ChevronDown, Edit, Copy } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";
import ProcessChecklistItem from "./ProcessChecklistItem";
import { type ProcessChecklistTemplate } from "@/pages/admin/content/content.static";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  SortableContext,
  verticalListSortingStrategy,
  arrayMove,
  sortableKeyboardCoordinates,
} from "@dnd-kit/sortable";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import { useGetChecklistTemplateByIdQuery } from "@/store/api/admin/checklists.api";

interface ProcessChecklistCardProps {
  checklist: ProcessChecklistTemplate;
  onEdit?: () => void;
  onDuplicate?: () => void;
  onDelete?: () => void;
  onDeleteItem?: (itemId: string) => void;
  onReorderItems?: (newItems: ProcessChecklistTemplate["items"]) => void;
  isExpanded?: boolean;
  onToggle?: () => void;
  readOnly?: boolean;
}

const ProcessChecklistCard = ({
  checklist,
  onEdit,
  onDuplicate,
  onDelete,
  onDeleteItem,
  onReorderItems,
  isExpanded,
  onToggle,
  readOnly = false,
}: ProcessChecklistCardProps) => {
  const [showAll, setShowAll] = useState(false);

  const { data: detailedTemplate, isFetching } = useGetChecklistTemplateByIdQuery(
    Number(checklist.id),
    { skip: !isExpanded },
  );

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const handleDragEnd = (event: DragEndEvent) => {
    if (readOnly) return;

    const { active, over } = event;

    if (over && active.id !== over.id) {
      const oldIndex = currentItems.findIndex((item) => item.id === active.id);
      const newIndex = currentItems.findIndex((item) => item.id === over.id);

      if (oldIndex !== -1 && newIndex !== -1) {
        const newItems = arrayMove(currentItems, oldIndex, newIndex);
        onReorderItems?.(newItems);
      }
    }
  };

  const mapCategory = (cat: string): ProcessChecklistTemplate["items"][number]["category"] => {
    const c = cat.toUpperCase();
    if (c === "INTAKE") return "Intake";
    if (c === "ASSESSMENT") return "Assessment";
    if (c === "ONGOING") return "Ongoing";
    if (c === "DISCHARGE") return "Discharge";
    return "Intake";
  };

  const currentItems = detailedTemplate
    ? (detailedTemplate.items || []).map((item) => ({
        id: String(item.id),
        title: item.title,
        description: item.description,
        category: mapCategory(item.category),
        required: item.isRequired,
      }))
    : checklist.items;

  const firstFourItems = currentItems.slice(0, 4);
  const remainingItems = currentItems.slice(4);
  const remainingCount = remainingItems.length;

  return (
    <div className="bg-white border border-(--neutral-100) rounded-2xl overflow-hidden shadow-xs mb-4">
      {/* Header */}
      <div className="flex items-center justify-between gap-4 p-4 cursor-pointer hover:bg-(--neutral-50)/50 transition-colors">
        <div className="flex min-w-0 flex-1 items-center gap-4">
          <button
            onClick={onToggle}
            className="shrink-0 text-(--text-neutral-400) hover:text-(--text-neutral-600) transition-colors"
          >
            <ChevronDown
              size={20}
              className={cn(
                "transition-transform duration-200 cursor-pointer",
                isExpanded ? "rotate-180" : "",
              )}
            />
          </button>
          <div className="min-w-0 flex-1" onClick={onToggle}>
            <h3
              className="mb-1 truncate font-medium text-lg text-(--text-primary-dark)"
              title={checklist.title}
            >
              {checklist.title}
            </h3>
            <p
              className="line-clamp-2 text-sm text-(--text-neutral-600) break-words [overflow-wrap:anywhere]"
              title={checklist.description}
            >
              {checklist.description}
            </p>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-4">
          <span className="text-sm text-(--text-primary-dark)">
            {checklist.itemCount} items
          </span>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                type="button"
                disabled={readOnly}
                className={cn(
                  "text-(--text-primary-dark) transition-colors",
                  readOnly ? "cursor-not-allowed opacity-40" : "cursor-pointer",
                )}
              >
                <MenuDotsIcon size={20} />
              </button>
            </DropdownMenuTrigger>
            {!readOnly ? (
              <DropdownMenuContent
                align="end"
                className="w-fit rounded-xl shadow-lg"
              >
                <DropdownMenuItem
                  onClick={onEdit}
                  className="gap-2 cursor-pointer font-medium text-(--text-primary-dark) py-2.5 border-b border-(--neutral-100) hover:bg-(--neutral-50) rounded-md"
                >
                  <Edit size={16} className="text-(--text-primary-dark)" />
                  Edit
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={onDuplicate}
                  className="gap-2 cursor-pointer font-medium text-(--text-primary-dark) py-2.5 border-b border-(--neutral-100) hover:bg-(--neutral-50) rounded-md"
                >
                  <Copy size={16} className="text-(--text-primary-dark)" />
                  Duplicate
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={onDelete}
                  className="gap-2 cursor-pointer font-medium text-(--text-primary-dark) py-2.5 hover:bg-(--neutral-50) rounded-md"
                >
                  <TrashIcon size={16} className="text-(--text-primary-dark)" />
                  Delete
                </DropdownMenuItem>
              </DropdownMenuContent>
            ) : null}
          </DropdownMenu>
        </div>
      </div>

      <div
        className={cn(
          "grid transition-all duration-500 ease-in-out border-t border-(--neutral-100)",
          isExpanded ? "grid-rows-[1fr]" : "grid-rows-[0fr]",
        )}
      >
        <div className="overflow-hidden">
          {isFetching ? (
            <div className="py-12 flex justify-center">
              <ContentLoader variant="inline" size="md" />
            </div>
          ) : (
            <DndContext
              sensors={sensors}
              collisionDetection={closestCenter}
              onDragEnd={handleDragEnd}
            >
              <div>
                {/* First 4 items */}
                <SortableContext
                  items={firstFourItems.map((item) => item.id)}
                  strategy={verticalListSortingStrategy}
                >
                  {firstFourItems.map((item, index) => (
                    <ProcessChecklistItem
                      key={item.id}
                      item={item}
                      index={index}
                      readOnly={readOnly}
                      onDelete={() => onDeleteItem?.(item.id)}
                    />
                  ))}
                </SortableContext>

                {/* Remaining items with smooth transition */}
                {remainingCount > 0 && (
                  <div
                    className={cn(
                      "grid transition-all duration-500 ease-in-out",
                      showAll ? "grid-rows-[1fr]" : "grid-rows-[0fr]",
                    )}
                  >
                    <div className="overflow-hidden">
                      <SortableContext
                        items={remainingItems.map((item) => item.id)}
                        strategy={verticalListSortingStrategy}
                      >
                        {remainingItems.map((item, index) => (
                          <ProcessChecklistItem
                            key={item.id}
                            item={item}
                            index={index + 4}
                            readOnly={readOnly}
                            onDelete={() => onDeleteItem?.(item.id)}
                          />
                        ))}
                      </SortableContext>
                    </div>
                  </div>
                )}

                {/* View more/less button */}
                {remainingCount > 0 && (
                  <button
                    onClick={() => setShowAll(!showAll)}
                    className="w-full py-4 text-sm text-(--text-primary-dark) hover:bg-(--neutral-50) transition-colors cursor-pointer"
                  >
                    {showAll ? "View less" : `View ${remainingCount} more`}
                  </button>
                )}
              </div>
            </DndContext>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProcessChecklistCard;
