import { ContentLoader } from "@/components/shared/ContentLoader";
import { CalendarIcon } from "@/components/icons/commonIcons";
import { cn } from "../../../../../lib/utils";
import type { RefObject } from "react";

interface TimelineEvent {
  id: string;
  title: string;
  description: string;
  author?: string;
  date: string;
  type: "portal" | "file" | "general";
}

interface ClientTimelineProps {
  events: TimelineEvent[];
  isLoading?: boolean;
  isLoadingMore?: boolean;
  observerTarget?: RefObject<HTMLDivElement | null>;
}

/**
 * Figma: History → Timeline panel (1559:99468).
 * Heading lives inside the bordered card; date is icon + text (no pill).
 */
const ClientTimeline = ({
  events,
  isLoading = false,
  isLoadingMore = false,
  observerTarget,
}: ClientTimelineProps) => {
  return (
    <div className="flex w-full flex-col gap-3 rounded-xl border border-(--neutral-100) bg-white p-3 sm:p-4">
      <h3 className="text-base leading-6 font-semibold text-(--neutral-950)">
        Timeline
      </h3>

      <div className="custom-scrollbar flex max-h-[20rem] flex-col gap-3 overflow-y-auto pr-1">
        {isLoading ? (
          <ContentLoader size="sm" className="min-h-0 py-8 text-sm text-(--text-neutral-600)" />
        ) : events.length === 0 ? (
          <div className="py-8 text-center text-sm text-(--text-neutral-600)">
            No data found
          </div>
        ) : (
          events.map((event) => (
            <div
              key={event.id}
              className={cn(
                "flex flex-col gap-2 rounded-xl border p-3 sm:flex-row sm:items-start sm:gap-2",
                event.type === "file"
                  ? "border-[#D0FBE3] bg-(--bg-success-light)"
                  : "border-(--neutral-100) bg-(--bg-primary-light)",
              )}
            >
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <h4 className="text-sm leading-[1.375rem] font-semibold text-(--neutral-950)">
                  {event.title}
                </h4>
                <div className="flex flex-col gap-1 text-(--text-neutral-600)">
                  <p className="text-sm leading-[1.375rem] font-normal break-words">
                    {event.description}
                  </p>
                  {event.author ? (
                    <p className="text-xs leading-[1.125rem] font-normal">
                      by {event.author}
                    </p>
                  ) : null}
                </div>
              </div>

              <div className="flex shrink-0 items-center gap-2 self-start text-xs leading-[1.125rem] font-normal whitespace-nowrap text-(--text-neutral-600)">
                <CalendarIcon size={16} className="size-4 shrink-0 text-(--text-neutral-600)" />
                <span>{event.date}</span>
              </div>
            </div>
          ))
        )}
        {observerTarget ? <div ref={observerTarget} className="h-1 w-full" /> : null}
        {isLoadingMore ? (
          <ContentLoader size="sm" className="min-h-0 py-2 text-xs text-(--text-neutral-600)" />
        ) : null}
      </div>
    </div>
  );
};

export default ClientTimeline;
