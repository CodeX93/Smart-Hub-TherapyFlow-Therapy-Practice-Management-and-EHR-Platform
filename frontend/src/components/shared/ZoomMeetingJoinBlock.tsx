import { ExternalLink, Video } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { shouldShowZoomMeetingJoin } from "@/utils/zoomMeeting";

type ZoomMeetingJoinBlockProps = {
  joinUrl?: string | null;
  password?: string | null;
  zoomEnabled?: boolean | null;
  sessionMode?: string | null;
  status?: string | null;
  /** Compact layout for list cards */
  compact?: boolean;
  className?: string;
};

/**
 * Shown only when a virtual/online session has a generated Zoom join URL
 * and the session is not cancelled / no-show.
 */
export function ZoomMeetingJoinBlock({
  joinUrl,
  password,
  zoomEnabled,
  sessionMode,
  status,
  compact = false,
  className,
}: ZoomMeetingJoinBlockProps) {
  if (
    !shouldShowZoomMeetingJoin({
      joinUrl,
      zoomEnabled,
      sessionMode,
      status,
    })
  ) {
    return null;
  }

  const url = joinUrl!.trim();

  return (
    <div
      className={cn(
        "rounded-xl border border-(--neutral-100) bg-(--bg-primary-light)",
        compact ? "p-3" : "p-3.5",
        className,
      )}
    >
      <div className="flex items-start gap-2.5">
        <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white text-(--bg-primary-dark)">
          <Video className={compact ? "h-4 w-4" : "h-4.5 w-4.5"} />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-(--text-primary-dark)">
            Zoom meeting
          </p>
          <p
            className="mt-0.5 truncate text-xs text-(--text-neutral-600)"
            title={url}
          >
            {url}
          </p>
          {password ? (
            <p className="mt-1 text-xs text-(--text-neutral-600)">
              Password:{" "}
              <span className="font-medium text-(--text-primary-dark)">{password}</span>
            </p>
          ) : null}
          <div className="mt-2.5 flex flex-wrap items-center gap-2">
            <Button
              type="button"
              size="sm"
              className="h-8 gap-1.5 px-3 text-xs"
              onClick={() => window.open(url, "_blank", "noopener,noreferrer")}
            >
              <ExternalLink className="h-3.5 w-3.5" />
              Join Zoom
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
