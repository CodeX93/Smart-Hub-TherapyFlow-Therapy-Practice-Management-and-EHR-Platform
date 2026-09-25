import { TrashIcon } from "@/components/icons/commonIcons";
import React from "react";
import { useFormContext } from "react-hook-form";

import CustomSelect from "@/components/form/CustomSelect";
import {
  timeOptions,
} from "@/pages/admin/user-access/profiles/user-access.static";
import { FormField } from "@/components/ui/form";
import type { ScheduleFormValues } from "@/schemas/user-access-profiles.schema";
import TypeButton from "./TypeButton";

interface TimeSlotItemProps {
  dayIndex: number;
  slotIndex: number;
  onRemove: () => void;
  showRemove: boolean;
}

const TimeSlotItem: React.FC<TimeSlotItemProps> = ({
  dayIndex,
  slotIndex,
  onRemove,
  showRemove,
}) => {
  const { control, setValue } = useFormContext<ScheduleFormValues>();

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-1.5">
        <CustomSelect
          control={control}
          name={`workingHours.${dayIndex}.slots.${slotIndex}.startTime`}
          options={timeOptions}
          className="pt-1.5 h-1 min-h-10 w-25"
          isSearch={false}
          isIcon={false}
        />
        <span className="text-(--text-neutral-400)">—</span>
        <CustomSelect
          control={control}
          name={`workingHours.${dayIndex}.slots.${slotIndex}.endTime`}
          options={timeOptions}
          className="pt-1.5 h-1 min-h-10 w-25"
          isSearch={false}
          isIcon={false}
        />

        <div className="h-6 w-px bg-(--neutral-100) mx-1" />

        {/* Virtual / In-person Toggle */}
        <div className="flex items-center gap-2">
          <FormField
            control={control}
            name={`workingHours.${dayIndex}.slots.${slotIndex}.type`}
            render={({ field }) => (
              <>
                <TypeButton
                  active={field.value === "in-person"}
                  onClick={() => field.onChange("in-person")}
                  label="In-person"
                />
                <TypeButton
                  active={field.value === "virtual"}
                  onClick={() => {
                    field.onChange("virtual");
                    setValue(
                      `workingHours.${dayIndex}.slots.${slotIndex}.roomIds`,
                      [],
                      { shouldDirty: true, shouldValidate: true },
                    );
                  }}
                  label="Virtual"
                />
              </>
            )}
          />
        </div>

        {showRemove && (
          <button
            type="button"
            onClick={onRemove}
            className="ml-auto text-(--text-neutral-400) hover:text-red-500 transition-colors cursor-pointer"
          >
            <TrashIcon size={20} />
          </button>
        )}
      </div>
    </div>
  );
};

export default TimeSlotItem;
