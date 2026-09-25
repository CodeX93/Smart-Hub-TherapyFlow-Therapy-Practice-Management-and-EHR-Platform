import { cn } from "@/lib/utils";

type ContentLoaderSize = "sm" | "md" | "lg" | "xl";
type ContentLoaderVariant = "block" | "inline";

interface ContentLoaderProps {
  /** Visual size of the spinner. */
  size?: ContentLoaderSize;
  /** Accessible label. */
  label?: string;
  /**
   * `block` — centered for pages, tabs, and modals (default).
   * `inline` — spinner only for load-more rows / compact spots.
   */
  variant?: ContentLoaderVariant;
  className?: string;
}

/** Compact sizes so page loaders don’t dominate dashboards / task grids. */
const sizeClasses: Record<ContentLoaderSize, string> = {
  sm: "size-3.5",
  md: "size-4.5",
  lg: "size-5",
  xl: "size-6",
};

const SPOKE_COUNT = 8;

/**
 * Reusable 8-spoke rounded spinner for pages, tabs, and modals.
 * Button loading stays on `LoadingDots` separately — update this file
 * to change page/tab/modal loaders site-wide.
 */
function ContentLoader({
  size = "md",
  label = "Loading",
  variant = "block",
  className,
}: ContentLoaderProps) {
  const indicator = (
    <span
      role="status"
      aria-label={label}
      className={cn(
        "relative inline-block shrink-0 text-(--text-primary-dark) motion-safe:animate-[content-loader-spin_0.8s_steps(8)_infinite]",
        sizeClasses[size],
      )}
    >
      {Array.from({ length: SPOKE_COUNT }, (_, index) => (
        <span
          key={index}
          aria-hidden="true"
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

  if (variant === "inline") {
    return (
      <span className={cn("inline-flex items-center justify-center", className)}>
        {indicator}
      </span>
    );
  }

  return (
    <div
      className={cn(
        "flex min-h-64 w-full items-center justify-center",
        className,
      )}
    >
      {indicator}
    </div>
  );
}

export { ContentLoader };
export type { ContentLoaderProps, ContentLoaderSize, ContentLoaderVariant };
