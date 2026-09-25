import React from "react";

interface TimeLabelsColumnProps {
  hours: string[];
}

/**
 * Labels sit on the top border of each hour row (same Y as grid lines /
 * appointment tops). Shares `pt-2` with the day/week grids.
 */
const TimeLabelsColumn: React.FC<TimeLabelsColumnProps> = ({ hours }) => {
  return (
    <div className="md:w-20 flex flex-col border-r border-(--neutral-100) px-2 md:px-3">
      <div className="flex flex-col pt-2">
        {hours.map((hour, idx) => (
          <div
            key={idx}
            className="relative h-12 text-xs font-medium text-(--text-neutral-600)"
          >
            <span className="absolute right-0 top-0 -translate-y-1/2 leading-none">
              {hour}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default TimeLabelsColumn;
