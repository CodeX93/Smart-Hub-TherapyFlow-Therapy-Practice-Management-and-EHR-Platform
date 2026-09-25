import type { CSSProperties } from "react";
import { cn } from "@/lib/utils";

type LoadingDotsSize = "sm" | "md" | "lg" | "xl";
type LoadingDotsTone = "neutral" | "inverse";

interface LoadingDotsProps {
  className?: string;
  label?: string;
  size?: LoadingDotsSize;
  tone?: LoadingDotsTone;
}

/**
 * Three-dot loader used by Button (and similar compact controls).
 * Do not use this for page/tab/modal loading — use `ContentLoader` instead.
 */
const sizeClasses: Record<LoadingDotsSize, string> = {
  sm: "size-[1.375rem] gap-0.5 [&>span]:size-1.5",
  md: "size-5 gap-0.5 [&>span]:size-[0.3125rem]",
  lg: "size-5 gap-0.5 [&>span]:size-[0.3125rem]",
  xl: "size-6 gap-0.75 [&>span]:size-1.5",
};

const toneColors: Record<LoadingDotsTone, [string, string, string]> = {
  neutral: [
    "var(--loader-neutral-start)",
    "var(--loader-neutral-middle)",
    "var(--loader-neutral-end)",
  ],
  inverse: [
    "var(--loader-inverse-start)",
    "var(--loader-inverse-middle)",
    "var(--loader-inverse-end)",
  ],
};

function LoadingDots({
  className,
  label = "Loading",
  size = "md",
  tone = "neutral",
}: LoadingDotsProps) {
  return (
    <span
      role="status"
      aria-label={label}
      className={cn(
        "inline-flex shrink-0 items-center justify-center",
        sizeClasses[size],
        className,
      )}
    >
      {toneColors[tone].map((color, index) => (
        <span
          key={color}
          aria-hidden="true"
          className="block shrink-0 rounded-full motion-safe:animate-[loading-dot-pulse_1.2s_ease-in-out_infinite]"
          style={
            {
              backgroundColor: color,
              animationDelay: `${index * 160}ms`,
            } as CSSProperties
          }
        />
      ))}
    </span>
  );
}

export { LoadingDots };
export type { LoadingDotsProps, LoadingDotsSize, LoadingDotsTone };
