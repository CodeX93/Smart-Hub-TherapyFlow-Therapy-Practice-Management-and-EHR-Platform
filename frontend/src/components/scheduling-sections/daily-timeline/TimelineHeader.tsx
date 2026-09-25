import React from "react";
import { formatPracticeGmtOffset } from "@/utils/scheduleTimezone";

interface TimelineHeaderProps {
  weekday: string;
  dayNumber: number;
  timezone?: string | null;
}

const TimelineHeader: React.FC<TimelineHeaderProps> = ({
  weekday,
  dayNumber,
  timezone,
}) => {
  const gmtString = formatPracticeGmtOffset(timezone);

  return (
    <div className="flex items-center mb-1 md:mb-0">
      <div className="md:w-20 pr-3">
        <div className="h-16 flex items-end justify-end">
          <span className="text-sm font-bold text-(--text-neutral-600) uppercase tracking-wider">
            {gmtString}
          </span>
        </div>
      </div>

      <div className="hidden md:flex flex-1">
        <div className="h-16 flex items-center ml-2">
          <div className="flex flex-col items-center">
            <span className="text-[0.625rem] font-bold text-(--text-neutral-400) uppercase tracking-wider">
              {weekday}
            </span>
            <div className="w-8 h-8 rounded-full bg-(--bg-primary-dark) text-white flex items-center justify-center font-bold mt-1">
              {dayNumber}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TimelineHeader;
