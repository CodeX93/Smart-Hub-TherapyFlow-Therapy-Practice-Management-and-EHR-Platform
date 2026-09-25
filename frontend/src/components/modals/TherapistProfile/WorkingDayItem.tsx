import React from "react";
import { useFormContext } from "react-hook-form";
import { Plus, Copy } from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Checkbox } from "../../ui/checkbox";
import { FormControl, FormField, FormItem } from "@/components/ui/form";
import type { ScheduleFormValues } from "@/schemas/general-profile-modal-schemas";
import TimeSlotItem from "./TimeSlotItem";

interface WorkingDayItemProps {
  /**
   * Namespaces the checkbox id so two schedule sections mounted at once
   * ("schedule" and "consultation") never share an id, which would make each
   * label point at the first section's day instead of its own.
   */
  idPrefix: string;
  dayIndex: number;
  handleDayToggle: (index: number, active: boolean) => void;
  addSlot: (index: number) => void;
  removeSlot: (dayIndex: number, slotIndex: number) => void;
  copyToAll: (index: number) => void;
}

const WorkingDayItem: React.FC<WorkingDayItemProps> = ({
  idPrefix,
  dayIndex,
  handleDayToggle,
  addSlot,
  removeSlot,
  copyToAll,
}) => {
  const { control, watch } = useFormContext<ScheduleFormValues>();
  const day = watch(`workingHours.${dayIndex}`);
  const checkboxId = `${idPrefix}-${day.id}`;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <FormField
            control={control}
            name={`workingHours.${dayIndex}.active`}
            render={({ field }) => (
              <FormItem className="flex items-center space-y-0 gap-2">
                <FormControl>
                  <Checkbox
                    id={checkboxId}
                    checked={field.value}
                    onCheckedChange={(checked) =>
                      handleDayToggle(dayIndex, !!checked)
                    }
                  />
                </FormControl>
                <label
                  htmlFor={checkboxId}
                  className="text-(--neutral-950) text-[1rem] font-medium cursor-pointer"
                >
                  {day.label}
                </label>
              </FormItem>
            )}
          />
        </div>

        {day.active ? (
          <div className="flex items-center gap-4">
            <TooltipProvider>
              <Tooltip delayDuration={300}>
                <TooltipTrigger asChild>
                  <button
                    type="button"
                    onClick={() => addSlot(dayIndex)}
                    className="p-1 hover:bg-gray-100 rounded-md transition-colors cursor-pointer text-(--text-neutral-400) hover:text-(--neutral-950)"
                  >
                    <Plus size={20} />
                  </button>
                </TooltipTrigger>
                <TooltipContent>
                  <p>Add another time slot to this day</p>
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>

            <TooltipProvider>
              <Tooltip delayDuration={300}>
                <TooltipTrigger asChild>
                  <button
                    type="button"
                    onClick={() => copyToAll(dayIndex)}
                    className="p-1 hover:bg-gray-100 rounded-md transition-colors cursor-pointer text-(--text-neutral-400) hover:text-(--neutral-950)"
                  >
                    <Copy size={20} />
                  </button>
                </TooltipTrigger>
                <TooltipContent>
                  <p>Copy time to all</p>
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </div>
        ) : (
          <span className="pr-1 text-[0.875rem] text-(--text-neutral-400)">
            Not available
          </span>
        )}
      </div>

      {day.active ? (
        <div className="flex flex-col gap-4">
          {day.slots.map((slot, slotIndex) => (
            <TimeSlotItem
              key={slot.id}
              dayIndex={dayIndex}
              slotIndex={slotIndex}
              onRemove={() => removeSlot(dayIndex, slotIndex)}
              showRemove={day.slots.length > 1}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
};

export default WorkingDayItem;
