import { X } from "lucide-react";
import type { AppliedFilterChip } from "@/types/appliedFilters";
import { cn } from "@/lib/utils";

interface AppliedFiltersBarProps {
  chips: AppliedFilterChip[];
  onRemove: (chipId: string) => void;
  onClearAll: () => void;
  className?: string;
}

const AppliedFiltersBar = ({
  chips,
  onRemove,
  onClearAll,
  className,
}: AppliedFiltersBarProps) => {
  if (chips.length === 0) return null;

  return (
    <div className={cn("flex flex-wrap items-center gap-2", className)}>
      {chips.map((chip) => (
        <span
          key={chip.id}
          className="inline-flex max-w-full items-center gap-1.5 rounded-full border border-(--neutral-100) bg-(--bg-primary-50) py-1 pl-3 pr-1.5 text-xs font-medium text-(--text-primary-dark)"
        >
          <span className="truncate">{chip.label}</span>
          <button
            type="button"
            onClick={() => onRemove(chip.id)}
            className="shrink-0 rounded-full p-0.5 text-(--text-neutral-600) transition-colors hover:bg-white hover:text-(--text-primary-dark)"
            aria-label={`Remove ${chip.label} filter`}
          >
            <X className="size-3.5" />
          </button>
        </span>
      ))}
      <button
        type="button"
        onClick={onClearAll}
        className="text-xs font-medium text-(--text-primary-500) transition-colors hover:text-(--text-primary-dark) hover:underline"
      >
        Clear all
      </button>
    </div>
  );
};

export default AppliedFiltersBar;
