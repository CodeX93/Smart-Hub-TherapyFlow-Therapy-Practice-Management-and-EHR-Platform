import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import {
  getSessionStatusBadgeClass,
  getSessionStatusLabel,
} from "@/utils/sessionStatusPresentation";

interface SessionStatusBadgeProps {
  status?: string | null;
  children?: ReactNode;
  className?: string;
}

function SessionStatusBadge({
  status,
  children,
  className,
}: SessionStatusBadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        getSessionStatusBadgeClass(status),
        className,
      )}
    >
      {children ?? getSessionStatusLabel(status)}
    </span>
  );
}

export default SessionStatusBadge;
