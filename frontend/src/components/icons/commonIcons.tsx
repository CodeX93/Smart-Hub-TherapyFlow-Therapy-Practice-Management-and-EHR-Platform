import { forwardRef, type ComponentPropsWithoutRef } from "react";
import { MenuDots } from "@solar-icons/react-perf/category/ui/Bold/MenuDots";
import { TrashBinTrash } from "@solar-icons/react-perf/category/ui/Linear/TrashBinTrash";
import { CalendarMinimalistic } from "@solar-icons/react-perf/category/time/Linear/CalendarMinimalistic";
import { cn } from "@/lib/utils";

/**
 * Shared icons used site-wide for consistent UI.
 *
 * Page/tab/modal loading → `@/components/shared/ContentLoader`
 * Button loading → `@/components/ui/loading-dots` (`LoadingDots`)
 */

export const TrashIcon = TrashBinTrash;
export const CalendarIcon = CalendarMinimalistic;

type MenuDotsIconProps = ComponentPropsWithoutRef<typeof MenuDots>;

/**
 * Vertical filled action-menu dots (Solar Bold MenuDots rotated).
 * Use this for overflow / action menus — not for loaders.
 */
export const MenuDotsIcon = forwardRef<SVGSVGElement, MenuDotsIconProps>(
  function MenuDotsIcon({ className, ...props }, ref) {
    return (
      <MenuDots
        ref={ref}
        {...props}
        className={cn("rotate-90", className)}
      />
    );
  },
);

type AiNoteTakerIconProps = ComponentPropsWithoutRef<"svg"> & {
  size?: number | string;
};

/**
 * AI Note Taker: a note page with sparkles, used for the session recording
 * action that transcribes and drafts the note.
 */
export const AiNoteTakerIcon = forwardRef<SVGSVGElement, AiNoteTakerIconProps>(
  function AiNoteTakerIcon({ size = 24, ...props }, ref) {
    return (
      <svg
        ref={ref}
        xmlns="http://www.w3.org/2000/svg"
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={1.8}
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
        {...props}
      >
        <path d="M13 4H6a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2v-6" />
        <path d="M4 8h2M4 12h2M4 16h2" />
        <path d="M9 11h5M9 14.5h5M9 18h3" />
        <path
          d="M18.5 1.5l1.1 2.9 2.9 1.1-2.9 1.1-1.1 2.9-1.1-2.9-2.9-1.1 2.9-1.1z"
          fill="currentColor"
          stroke="none"
        />
        <path
          d="M21 10.2l.6 1.2 1.2.6-1.2.6-.6 1.2-.6-1.2-1.2-.6 1.2-.6z"
          fill="currentColor"
          stroke="none"
        />
      </svg>
    );
  },
);
