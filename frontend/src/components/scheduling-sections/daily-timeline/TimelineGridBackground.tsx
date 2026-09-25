import React from "react";

interface TimelineGridBackgroundProps {
  hours: string[];
}

const TimelineGridBackground: React.FC<TimelineGridBackgroundProps> = ({
  hours,
}) => {
  return (
    <div className="pt-2">
      {hours.map((_, idx) => (
        <div
          key={idx}
          className="h-12 border-t border-(--neutral-100) w-full relative"
        />
      ))}
      <div className="border-t border-(--neutral-100) w-full" />
    </div>
  );
};

export default TimelineGridBackground;
