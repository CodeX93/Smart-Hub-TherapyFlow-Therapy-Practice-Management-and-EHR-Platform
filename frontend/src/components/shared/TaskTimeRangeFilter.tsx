import { cn } from "@/lib/utils";
import type { TaskTimeRange } from "@/utils/taskTimeRange";

interface TaskTimeRangeFilterProps {
  value: TaskTimeRange | null;
  onChange: (value: TaskTimeRange) => void;
}

const OPTIONS: Array<{ value: TaskTimeRange; label: string }> = [
  { value: "today", label: "Today" },
  { value: "week", label: "Week" },
  { value: "month", label: "Month" },
  { value: "all", label: "All time" },
];

const TaskTimeRangeFilter = ({
  value,
  onChange,
}: TaskTimeRangeFilterProps) => (
  <div
    className="flex h-11 max-w-full items-center gap-1 overflow-x-auto rounded-full bg-(--neutral-100) p-1"
    aria-label="Filter tasks by created period"
  >
    {OPTIONS.map((option) => (
      <button
        key={option.value}
        type="button"
        onClick={() => onChange(option.value)}
        aria-pressed={value === option.value}
        className={cn(
          "h-9 shrink-0 cursor-pointer rounded-full px-4 text-sm font-medium transition-colors",
          value === option.value
            ? "bg-white text-(--text-primary-dark) shadow-sm"
            : "text-(--text-neutral-600) hover:text-(--text-primary-dark)",
        )}
      >
        {option.label}
      </button>
    ))}
  </div>
);

export default TaskTimeRangeFilter;
