import { TrashIcon } from "@/components/icons/commonIcons";
import { GripVertical, ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import CustomSelect from "@/components/form/CustomSelect";
import { Checkbox } from "@/components/ui/checkbox";
import { CHECKLIST_CATEGORIES } from "@/pages/admin/content/content.static";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import {
  FormField,
  FormItem,
  FormControl,
  FormLabel,
} from "@/components/ui/form";
import { useFormState, useWatch, type Control } from "react-hook-form";
import type { CreateTemplateValues } from "@/schemas/admin-checklist.schemas";

interface DraggableChecklistItemProps {
  control: Control<CreateTemplateValues>;
  index: number;
  item: { id: string }; // Needed for dnd-kit
  isFirst: boolean;
  isLast: boolean;
  isExpanded: boolean;
  onToggleExpanded: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onRemove: () => void;
}

const DraggableChecklistItem = ({
  control,
  index,
  item,
  isExpanded,
  onToggleExpanded,
  onRemove,
}: DraggableChecklistItemProps) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: item.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isDragging ? 50 : "auto",
    opacity: isDragging ? 0.5 : 1,
  };

  const watchedTitle = useWatch({
    control,
    name: `customItems.${index}.title`,
  });
  const { errors } = useFormState({ control, name: `customItems.${index}` });
  const itemErrors = Array.isArray(errors.customItems)
    ? errors.customItems[index]
    : undefined;
  const hasItemErrors = Boolean(itemErrors);
  const itemTitle =
    typeof watchedTitle === "string" && watchedTitle.trim().length > 0
      ? watchedTitle.trim()
      : `Item ${index + 1}`;

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        "overflow-hidden rounded-xl border px-3",
        hasItemErrors
          ? "border-red-200 bg-red-50/40"
          : "border-(--neutral-100) bg-white",
      )}
    >
      {/* Item Header */}
      <div className="flex items-center justify-between border-b pb-4 border-(--neutral-100) cursor-pointer py-4">
        <div className="flex min-w-0 items-center gap-2 flex-1">
          {/* Drag Handle */}
          <div
            {...attributes}
            {...listeners}
            className="text-(--text-neutral-400) cursor-grab active:cursor-grabbing hover:text-(--text-primary-dark) transition-colors p-1"
          >
            <GripVertical size={18} />
          </div>

          {/* Expand Toggle */}
          <button
            type="button"
            onClick={onToggleExpanded}
            className="flex min-w-0 items-center gap-2 text-left cursor-pointer flex-1"
          >
            <ChevronDown
              size={18}
              className={cn(
                "text-(--text-neutral-400) transition-transform duration-300",
                isExpanded && "rotate-180",
              )}
            />
            <span
              className="min-w-0 max-w-[13.75rem] break-all line-clamp-2 text-sm font-medium text-(--text-primary-dark)"
              title={itemTitle}
            >
              {itemTitle}
            </span>
            {hasItemErrors ? (
              <span className="shrink-0 rounded-full bg-red-100 px-2 py-0.5 text-[0.6875rem] font-semibold text-red-700">
                Fix required
              </span>
            ) : null}
          </button>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onRemove}
            className="text-(--text-neutral-600) transition-colors duration-200 cursor-pointer"
          >
            <TrashIcon size={16} />
          </button>
        </div>
      </div>

      {/* Item Content */}
      <div
        className={cn(
          "overflow-hidden transition-[max-height,opacity] duration-300 ease-in-out",
          isExpanded ? "max-h-125 opacity-100" : "max-h-0 opacity-0",
        )}
      >
        <div className="pt-4 space-y-4 pb-4">
          <CustomInput
            control={control}
            name={`customItems.${index}.title`}
            label="Item Title"
            required
            maxLength={120}
            hint="e.g., Client Contacted"
          />

          <CustomSelect
            control={control}
            name={`customItems.${index}.category`}
            label="Select Category"
            options={CHECKLIST_CATEGORIES.filter(
              (cat) => cat !== "All Categories",
            ).map((cat) => ({ value: cat, label: cat }))}
            required
          />

          <div className="space-y-1.5">
            <CustomTextarea
              control={control}
              name={`customItems.${index}.description`}
              label="Description"
              rows={3}
            />
            <p className="text-xs text-(--text-neutral-500)">
              Detailed description of the required action
            </p>
          </div>

          <FormField
            control={control}
            name={`customItems.${index}.required`}
            render={({ field }) => (
              <FormItem className="flex flex-row items-center space-x-2 space-y-0">
                <FormControl>
                  <Checkbox
                    id={`required-${item.id}`}
                    checked={field.value}
                    onCheckedChange={field.onChange}
                    className="rounded border-(--neutral-200) data-[state=checked]:bg-(--bg-primary-dark) data-[state=checked]:border-(--bg-primary-dark)"
                  />
                </FormControl>
                <FormLabel
                  htmlFor={`required-${item.id}`}
                  className="text-sm font-medium text-(--text-neutral-800) cursor-pointer"
                >
                  Required checklist item
                </FormLabel>
              </FormItem>
            )}
          />
        </div>
      </div>
    </div>
  );
};

export default DraggableChecklistItem;
