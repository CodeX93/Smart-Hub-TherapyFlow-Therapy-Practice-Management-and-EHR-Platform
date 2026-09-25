type Props = {
  timeSlots: string[];
  selectedTime: string | null;
  setSelectedTime: (t: string) => void;
  showEmptyState?: boolean;
};

export default function TimeSlots({
  timeSlots,
  selectedTime,
  setSelectedTime,
  showEmptyState = true,
}: Props) {
  if (!timeSlots.length) {
    if (!showEmptyState) return null;

    return (
      <p className="mb-8 text-sm text-(--text-neutral-600)">
        No available slots for this date.
      </p>
    );
  }

  return (
    <div className="mb-8 w-full">
      <div className="grid grid-cols-3 gap-2 max-h-64 overflow-y-auto">
        {timeSlots.map((time, index) => (
          <button
            key={`${time}-${index}`}
            onClick={() => setSelectedTime(time)}
            className={`p-3 rounded-lg text-xs  font-medium transition cursor-pointer ${
              selectedTime === time
                ? "bg-(--text-primary-500) text-white border border-(--border-primary-light)"
                : "text-(--text-primary-dark) hover:bg-gray-100 border border-(--neutral-100)"
            }`}
          >
            {time}
          </button>
        ))}
      </div>
    </div>
  );
}
