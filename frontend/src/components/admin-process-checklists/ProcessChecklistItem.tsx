import { TrashIcon } from "@/components/icons/commonIcons";
import { GripVertical } from "lucide-react";
import { cn } from "@/lib/utils";
import { type ProcessChecklistItem as ItemType } from "@/pages/admin/content/content.static";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";

interface ProcessChecklistItemProps {
  item: ItemType;
  index: number;
  onDelete?: () => void;
  readOnly?: boolean;
}

const categoryStyles = {
  Intake: "bg-(--light-blue) text-(--status-billed)",
  Assessment: "bg-(--neutral-100) text-(--text-primary-dark)",
  Ongoing: "bg-(--status-completed-light) text-(--dark-green)",
  Discharge: "bg-(--dashboard-status-pending-light) text-(--dashboard-status-pending-dark)",
};

const ProcessChecklistItem = ({
  item,
  index,
  onDelete,
  readOnly = false,
}: ProcessChecklistItemProps) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: item.id, disabled: readOnly });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        "flex items-center justify-between p-4 bg-white border-b border-(--neutral-50) last:border-0 hover:bg-(--neutral-50)/50 transition-colors group",
        isDragging && "opacity-50"
      )}
    >
      <div className="flex items-center gap-4">
        {!readOnly ? (
          <div
            {...attributes}
            {...listeners}
            className="text-(--text-neutral-400) cursor-grab active:cursor-grabbing opacity-0 group-hover:opacity-100 transition-opacity"
          >
            <GripVertical size={18} />
          </div>
        ) : null}
        <div className="font-medium text-(--text-primary-dark) min-w-0">
          <span
            className="inline-block max-w-[32.5rem] truncate align-bottom"
            title={item.title}
          >
            {index + 1}. {item.title}
          </span>{" "}
          {item.required && <span className="text-red-500">*</span>}
        </div>
      </div>

      <div className="flex items-center gap-6">
        <span
          className={cn(
            "px-3 py-1 rounded-full text-xs font-medium",
            categoryStyles[item.category]
          )}
        >
          {item.category}
        </span>
        {!readOnly ? (
          <button
            type="button"
            onClick={onDelete}
            className="text-(--text-neutral-400) transition-colors cursor-pointer"
          >
            <TrashIcon size={18} />
          </button>
        ) : null}
      </div>
    </div>
  );
};

export default ProcessChecklistItem;
