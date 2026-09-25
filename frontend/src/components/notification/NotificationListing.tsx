
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState, type ReactNode } from "react";
import { Check, CheckCheck } from "lucide-react";
import type { Notification } from "../../types/notification";
import { cn } from "../../lib/utils";

const DESCRIPTION_COLLAPSE_THRESHOLD = 120;

const NotificationListing = ({
  notification,
  onMarkRead,
  isMarkingRead = false,
  onAction,
  endActions,
}: {
  notification: Notification;
  onMarkRead?: (id: string) => void;
  isMarkingRead?: boolean;
  onAction?: (notification: Notification) => void;
  endActions?: ReactNode;
}) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const isLongDescription =
    notification.description.length > DESCRIPTION_COLLAPSE_THRESHOLD;

  return (
    <div
      key={notification.id}
      className={cn(
        "md:px-5 px-2 border-b border-(--neutral-100) relative bg-transparent transition-colors py-4",
        !notification.isRead && "bg-(--bg-primary-50)",
      )}
    >
      {notification.dateGroup && (
        <div className="mb-3">
          <span className="inline-block px-3 py-0.5 rounded-full bg-(--neutral-100) text-xs font-medium text-(--text-primary-dark)">
            {notification.dateGroup}
          </span>
        </div>
      )}

      <div className="flex items-start gap-3">
        <div className="w-2 shrink-0 mt-2 flex justify-center">
          {!notification.isRead && (
            <div className="w-2 h-2 rounded-full bg-(--text-primary-dark)" />
          )}
        </div>

        <div className="min-w-0 flex-1">
          <h3
            className="mb-1 line-clamp-2 font-semibold text-(--text-primary-dark) break-words [overflow-wrap:anywhere]"
            title={notification.title}
          >
            {notification.title}
          </h3>

          <p
            className={cn(
              "mb-1 text-sm text-(--text-primary-600) break-words [overflow-wrap:anywhere]",
              !isExpanded && isLongDescription && "line-clamp-3",
            )}
          >
            {notification.description}
          </p>

          {isLongDescription ? (
            <button
              type="button"
              onClick={() => setIsExpanded((previous) => !previous)}
              className="mb-1 text-xs font-medium text-(--text-primary-500) hover:underline cursor-pointer"
            >
              {isExpanded ? "Show less" : "Show more"}
            </button>
          ) : null}

          <div className="flex items-center gap-2 justify-between">
            <span className="text-(--text-secondary-light) text-xs">
              {notification.timestamp}
            </span>
            <div className="flex items-center gap-2">
              {notification.actionUrl && onAction ? (
                <button
                  type="button"
                  onClick={() => onAction(notification)}
                  className="text-xs font-medium text-(--text-primary-500) hover:underline cursor-pointer"
                >
                  {notification.actionLabel || "View"}
                </button>
              ) : null}
              {endActions}
              {!notification.isRead ? (
                <span
                  className={cn("cursor-pointer", isMarkingRead && "cursor-wait")}
                  onClick={() => {
                    if (!isMarkingRead && onMarkRead) onMarkRead(notification.id);
                  }}
                  title="Mark as read"
                >
                  {isMarkingRead ? (
                    <ContentLoader variant="inline" size="md" />
                  ) : (
                    <Check
                      size={18}
                      className="text-(--text-primary-500)"
                    />
                  )}
                </span>
              ) : (
                <span title="Read">
                  <CheckCheck
                    size={18}
                    className="text-(--text-primary-300)"
                  />
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default NotificationListing;
