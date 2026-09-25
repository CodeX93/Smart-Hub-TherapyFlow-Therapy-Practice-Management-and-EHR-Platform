import React from "react";

const WEEKDAYS = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];

const MonthlyHeader: React.FC = () => {
  return (
    <div className="grid grid-cols-7">
      {WEEKDAYS.map((day) => (
        <div
          key={day}
          className="pt-4 text-center text-xs font-semibold text-(--text-primary-dark) uppercase border-r border-(--neutral-100) last:border-r-0"
        >
          {day}
        </div>
      ))}
    </div>
  );
};

export default MonthlyHeader;
