import React from "react";
import { Loader2 } from "lucide-react";

interface Props {
  date: Date | null;
  timeSlots: string[];
  selected: string;
  onSelect: (t: string) => void;
  timezoneLabel?: string;
  isLoading?: boolean;
  emptyMessage?: string;
}

const TimeSlots: React.FC<Props> = ({
  date,
  timeSlots,
  selected,
  onSelect,
  timezoneLabel,
  isLoading = false,
  emptyMessage = "No available time slots",
}) => {
  if (!date) return null;
  return (
    <div>
      <label className="text-sm font-medium text-(--text-primary-dark) mb-1 block">
        Select time slot <span className="text-red-500">*</span>
      </label>
      {timezoneLabel ? (
        <p className="text-xs text-(--text-neutral-600) mb-3">
          Times shown in{" "}
          <span className="font-medium text-(--text-primary-dark)">
            {timezoneLabel}
          </span>
        </p>
      ) : (
        <div className="mb-3" />
      )}
      <div className="flex flex-wrap p-4 gap-2.5 bg-(--bg-upload-container) rounded-xl border border-(--bg-primary-100)">
        {isLoading ? (
          <div className="flex w-full items-center justify-center gap-2 py-2">
            <Loader2 className="h-4 w-4 animate-spin text-(--bg-primary-dark)" />
            <span className="text-sm text-(--text-neutral-600)">
              Loading available times…
            </span>
          </div>
        ) : timeSlots.length > 0 ? (
          timeSlots.map((slot) => (
            <button
              key={slot}
              type="button"
              onClick={() => onSelect(slot)}
              className={`px-3.5 py-2 rounded-lg text-sm font-medium transition-all border border-(--text-neutral-100) duration-300 w-fit cursor-pointer ${
                selected === slot
                  ? "bg-(--bg-primary-dark) text-white"
                  : "bg-white text-(--text-primary-dark) hover:bg-(--bg-primary-100)"
              }`}
            >
              {slot}
            </button>
          ))
        ) : (
          <p className="text-sm text-red-500 font-medium">{emptyMessage}</p>
        )}
      </div>
    </div>
  );
};

export default TimeSlots;
