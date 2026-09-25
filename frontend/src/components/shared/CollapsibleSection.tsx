import { useState, useRef, useEffect, type ReactNode } from "react";
import { AltArrowDown } from "@solar-icons/react-perf/category/arrows/Linear/AltArrowDown";
import { AltArrowUp } from "@solar-icons/react-perf/category/arrows/Linear/AltArrowUp";
import { ClockCircle } from "@solar-icons/react-perf/category/time/Linear/ClockCircle";
import { cn } from "@/lib/utils.ts";

interface CollapsibleSectionProps {
  title: string;
  children: ReactNode;
  defaultExpanded?: boolean;
  className?: string;
  headerClassName?: string;
  contentClassName?: string;
  titleClassName?: string;
  showStatus?: string; // e.g., "Enabled" text shown next to title
  statusMetaText?: string;
  isExpanded?: boolean; // Controlled state
  onToggle?: () => void; // Controlled toggle handler
}

const CollapsibleSection = ({
  title,
  children,
  defaultExpanded = false,
  className = "",
  headerClassName = "",
  contentClassName = "",
  titleClassName = "",
  showStatus,
  statusMetaText,
  isExpanded: controlledIsExpanded,
  onToggle,
}: CollapsibleSectionProps) => {
  const [internalIsExpanded, setInternalIsExpanded] = useState(defaultExpanded);
  const sectionRef = useRef<HTMLDivElement>(null);

  // Use controlled state if provided, otherwise use internal state
  const isExpanded =
    controlledIsExpanded !== undefined
      ? controlledIsExpanded
      : internalIsExpanded;

  const handleToggle = () => {
    if (onToggle) {
      onToggle();
    } else {
      setInternalIsExpanded(!internalIsExpanded);
    }
  };

  // Scroll to section when it expands
  useEffect(() => {
    if (
      isExpanded &&
      sectionRef.current &&
      controlledIsExpanded !== undefined
    ) {
      // Delay scroll to allow for animation
      setTimeout(() => {
        sectionRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "nearest",
        });
      }, 100);
    }
  }, [isExpanded, controlledIsExpanded]);

  return (
    <div
      ref={sectionRef}
      className={cn(
        "bg-white border border-gray-200 rounded-lg overflow-hidden mb-4",
        className,
      )}
    >
      <button
        onClick={handleToggle}
        className={cn(
          "w-full flex items-center justify-between py-4 px-6 text-left cursor-pointer hover:bg-gray-50 transition-colors",
          headerClassName,
        )}
      >
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center gap-4">
            <span className={cn("text-sm font-semibold text-gray-900", titleClassName)}>
              {title}
            </span>
            {showStatus && statusMetaText && (
              <div className="flex items-center gap-1.5">
                <ClockCircle
                  size={20}
                  color="#5B616E"
                />
                <p className="text-sm leading-[1.375rem] text-(--text-neutral-600)">
                  {statusMetaText}
                </p>
              </div>
            )}
          </div>
          {showStatus && (
            <span
              className={cn(
                "mr-3 inline-flex h-6 items-center rounded-full px-2.5 text-xs font-medium leading-[1.125rem]",
                showStatus === "Enabled"
                  ? "bg-[#D0FBE3] text-[#007C54]"
                  : "bg-(--neutral-50) text-(--text-neutral-600)",
              )}
            >
              {showStatus}
            </span>
          )}
        </div>
        {isExpanded ? (
          <AltArrowUp size={20} color="#1B1C20" />
        ) : (
          <AltArrowDown size={20} color="#1B1C20" />
        )}
      </button>
      {isExpanded && (
        <div className={cn("min-w-0 px-6 pb-6", contentClassName)}>{children}</div>
      )}
    </div>
  );
};

export default CollapsibleSection;
