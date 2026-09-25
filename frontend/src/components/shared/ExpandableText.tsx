import { useState } from "react";
import { cn } from "@/lib/utils";

interface ExpandableTextProps {
  text?: string | null;
  emptyText?: string;
  collapseThreshold?: number;
  collapsedLineClamp?: number;
  className?: string;
  buttonClassName?: string;
}

const ExpandableText = ({
  text,
  emptyText = "-",
  collapseThreshold = 120,
  collapsedLineClamp = 3,
  className,
  buttonClassName,
}: ExpandableTextProps) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const displayText = text?.trim() ? text.trim() : emptyText;
  const isLongText = displayText.length > collapseThreshold && displayText !== emptyText;

  const lineClampClass =
    collapsedLineClamp === 1
      ? "line-clamp-1"
      : collapsedLineClamp === 2
        ? "line-clamp-2"
        : collapsedLineClamp === 3
          ? "line-clamp-3"
          : "line-clamp-3";

  return (
    <div className="min-w-0">
      <p
        className={cn(
          "break-words [overflow-wrap:anywhere]",
          !isExpanded && isLongText && lineClampClass,
          className,
        )}
        title={displayText}
      >
        {displayText}
      </p>
      {isLongText ? (
        <button
          type="button"
          onClick={() => setIsExpanded((previous) => !previous)}
          className={cn(
            "mt-1 text-xs font-medium text-(--text-primary-500) hover:underline cursor-pointer",
            buttonClassName,
          )}
        >
          {isExpanded ? "Show less" : "Show more"}
        </button>
      ) : null}
    </div>
  );
};

export default ExpandableText;
