import { TrashIcon } from "@/components/icons/commonIcons";
import { Edit3, GripVertical } from "lucide-react";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { cn } from "@/lib/utils";

interface SortableOptionItemProps {
  option: { id: string; label: string; value: string; isSystem?: boolean; isActive?: boolean };
  isLast: boolean;
  onEdit?: () => void;
  onDelete?: (e: React.MouseEvent) => void;
}

const SortableOptionItem = ({
  option,
  isLast,
  onEdit,
  onDelete,
}: SortableOptionItemProps) => {
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
      className={cn(
        "group flex items-center justify-between py-5 border-b border-(--neutral-100) transition-all duration-300 bg-white",
        isLast && "border-none",
        isDragging && "shadow-lg rounded-xl px-4 z-50",
      )}
    >
      <div className="flex min-w-0 flex-1 items-center gap-4">
        <div
          {...attributes}
          {...listeners}
          className="shrink-0 text-(--text-neutral-300) cursor-grab active:cursor-grabbing hover:text-(--text-primary-dark) transition-colors p-1"
        >
          <GripVertical size={20} />
        </div>
        <div className={cn("min-w-0 flex-1", option.isActive === false && "opacity-50")}>
          <div className="flex items-center gap-2">
            <h5 className="truncate text-sm font-bold text-(--text-primary-dark)">
              {option.label}
            </h5>
            {option.isSystem && (
              <span className="shrink-0 inline-flex items-center px-2 py-0.5 rounded-full text-[0.625rem] font-semibold bg-blue-50 text-blue-600 border border-blue-100">
                System
              </span>
            )}
            {option.isActive === false && (
              <span className="shrink-0 inline-flex items-center px-2 py-0.5 rounded-full text-[0.625rem] font-semibold bg-(--neutral-100) text-(--text-neutral-600) border border-(--neutral-200)">
                Inactive
              </span>
            )}
          </div>
          <p className="truncate text-sm text-(--text-neutral-600) mt-0.5">
            Key: {option.value}
          </p>
        </div>
      </div>
      <div className="ml-3 flex shrink-0 items-center gap-1">
        <button
          onClick={onEdit}
          className="p-2.5 opacity-0 group-hover:opacity-100 rounded-lg transition-all duration-300 hover:bg-(--neutral-100) cursor-pointer"
        >
          <Edit3 size={16} className="text-(--text-neutral-600)" />
        </button>
        {onDelete && (
          <button
            onClick={onDelete}
            className="p-2.5 opacity-0 group-hover:opacity-100 rounded-lg transition-all duration-300 hover:bg-red-50 hover:text-red-500 cursor-pointer"
          >
            <TrashIcon size={16} className="text-(--text-neutral-600) hover:text-red-500" />
          </button>
        )}
      </div>
    </div>
  );
};

export default SortableOptionItem;
