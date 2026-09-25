import type { SVGProps } from "react";
import { cn } from "@/lib/utils";

/** Exact asset from Component 18.svg — assessment voice mic. */
export function AssessmentMicrophoneIcon({
  className,
  ...props
}: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      className={cn("shrink-0", className)}
      {...props}
    >
      <path
        d="M7 8C7 5.23858 9.23858 3 12 3C14.7614 3 17 5.23858 17 8V11C17 13.7614 14.7614 16 12 16C9.23858 16 7 13.7614 7 11V8Z"
        stroke="currentColor"
        strokeWidth="1.5"
      />
      <path
        d="M11 8H13"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
      />
      <path
        d="M10 11L14 11"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
      />
      <path
        d="M20 10V11C20 15.4183 16.4183 19 12 19C7.58172 19 4 15.4183 4 11V10"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
      />
      <path
        d="M12 19V22"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
      />
    </svg>
  );
}
