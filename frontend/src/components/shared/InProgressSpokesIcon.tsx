import { cn } from "@/lib/utils";

const SPOKE_COUNT = 8;

interface InProgressSpokesIconProps {
  className?: string;
}

/**
 * Static 8-spoke icon for task "In Progress" summary cards.
 * Not animated — keep ContentLoader for actual loading states.
 * Glyph only; parent supplies the 48×48 icon container.
 */
function InProgressSpokesIcon({ className }: InProgressSpokesIconProps) {
  return (
    <span
      aria-hidden="true"
      className={cn("relative block size-6 shrink-0 text-current", className)}
    >
      {Array.from({ length: SPOKE_COUNT }, (_, index) => (
        <span
          key={index}
          className="absolute inset-0"
          style={{ transform: `rotate(${index * (360 / SPOKE_COUNT)}deg)` }}
        >
          <span
            className="absolute left-1/2 top-0 h-[28%] w-[16%] -translate-x-1/2 rounded-full bg-current"
            style={{ opacity: (SPOKE_COUNT - index) / SPOKE_COUNT }}
          />
        </span>
      ))}
    </span>
  );
}

export default InProgressSpokesIcon;
