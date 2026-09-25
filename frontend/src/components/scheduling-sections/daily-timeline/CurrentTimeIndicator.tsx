import React from "react";

interface CurrentTimeIndicatorProps {
  top: string;
}

const CurrentTimeIndicator: React.FC<CurrentTimeIndicatorProps> = ({ top }) => {
  return (
    <div
      className="pointer-events-none absolute left-0 right-0 z-10 flex items-center"
      style={{ top }}
    >
      <div className="w-2.5 h-2.5 rounded-full bg-(--text-primary-dark) -ml-1 shadow-sm" />
      <div className="flex-1 h-0.5 bg-(--text-primary-dark)" />
    </div>
  );
};

export default CurrentTimeIndicator;
