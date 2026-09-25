import { TrashIcon } from "@/components/icons/commonIcons";
import { GripVertical, Pencil, MoveUp, MoveDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { sanitizeHtml } from "@/utils/sanitizeHtml";

export interface FormSection {
  id: string;
  title: string;
  type: string;
  content: string;
  required?: boolean;
  options?: string;
}

interface ConsentSectionCardProps {
  section: FormSection;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onEdit: () => void;
  onDelete: () => void;
  isFirst: boolean;
  isLast: boolean;
}

const ConsentSectionCard = ({
  section,
  onMoveUp,
  onMoveDown,
  onEdit,
  onDelete,
  isFirst,
  isLast,
}: ConsentSectionCardProps) => {
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
      className="bg-white rounded-xl border border-(--neutral-100) shadow-xs p-6 mb-4 group transition-shadow hover:shadow-md"
    >
      <div className="flex items-start gap-4">
        {/* Drag Handle */}
        <div
          {...attributes}
          {...listeners}
          className="mt-1 text-(--text-neutral-400) cursor-grab active:cursor-grabbing hover:text-(--text-primary-dark) transition-colors p-1"
        >
          <GripVertical size={20} />
        </div>

        <div className="flex-1 min-w-0">
          {/* Header */}
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-lg font-semibold text-(--text-primary-dark) truncate">
              {section.title}
              {section.required && <span className="text-red-500 ml-1">*</span>}
            </h3>
            <div className="flex items-center gap-4 text-(--text-neutral-500)">
              <div className="flex items-center gap-3 border-r border-(--neutral-100) pr-4 mr-1">
                <button
                  onClick={onMoveUp}
                  disabled={isFirst}
                  className={cn(
                    "cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed"
                  )}
                >
                  <MoveUp size={18} />
                </button>
                <button
                  onClick={onMoveDown}
                  disabled={isLast}
                  className={cn(
                    "cursor-pointer disabled:opacity-30 disabled:cursor-not-allowed"
                  )}
                >
                  <MoveDown size={18} />
                </button>
              </div>
              <button onClick={onEdit} className="cursor-pointer">
                <Pencil size={18} />
              </button>
              <button onClick={onDelete} className="cursor-pointer">
                <TrashIcon size={18} />
              </button>
            </div>
          </div>

          {/* Content Preview */}
          <div
            className="text-(--text-neutral-800)"
            dangerouslySetInnerHTML={{
              __html: sanitizeHtml(
                section.content.replace(/({{[A-Z_]+}})/g, "<strong>$1</strong>"),
              ),
            }}
          />
        </div>
      </div>
    </div>
  );
};

export default ConsentSectionCard;
