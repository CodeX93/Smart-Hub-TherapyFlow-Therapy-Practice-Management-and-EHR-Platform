import React from "react";
import { cn } from "../../../lib/utils";
import { getWeekdayShort } from "../../../utils/transformer/dates.transformer";
import { formatPracticeGmtOffset } from "@/utils/scheduleTimezone";

interface WeeklyHeaderProps {
  days: Date[];
  selectedFullDate?: Date;
  onDaySelect?: (date: Date) => void;
  timezone?: string | null;
}

const WeeklyHeader: React.FC<WeeklyHeaderProps> = ({
  days,
  selectedFullDate,
  onDaySelect,
  timezone,
}) => {
  const gmtString = formatPracticeGmtOffset(timezone);

  return (
    <div className="flex items-center px-3 py-2 sticky top-0 z-20 mb-2">
      <div className="relative w-10 md:w-20" />
      <div className="flex-1 grid grid-cols-7 gap-0 w-full">
        {days.map((day, dayIdx) => {
          const isActive =
            !!selectedFullDate &&
            selectedFullDate.getFullYear() === day.getFullYear() &&
            selectedFullDate.getMonth() === day.getMonth() &&
            selectedFullDate.getDate() === day.getDate();

          return (
            <div
              key={dayIdx}
              className="py-4 text-center cursor-pointer"
              onClick={() => onDaySelect?.(day)}
            >
              <div className="text-[0.625rem] font-medium text-(--text-neutral-400)">
                {getWeekdayShort(day)}
              </div>
              <div
                className={cn(
                  "w-8 h-8 rounded-full mx-auto flex items-center justify-center font-bold mt-2",
                  isActive
                    ? "bg-(--bg-primary-dark) text-white"
                    : "bg-transparent border border-(--neutral-100) text-(--text-neutral-600)"
                )}
              >
                {day.getDate()}
              </div>
            </div>
          );
        })}
        <div className="absolute -bottom-4 left-2 text-sm font-bold text-(--text-neutral-600) uppercase tracking-wider bg-white mb-2 md:mb-0">
          {gmtString}
        </div>
      </div>
    </div>
  );
};

export default WeeklyHeader;
