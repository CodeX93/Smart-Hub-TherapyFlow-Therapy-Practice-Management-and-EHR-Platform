import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

type SemanticStatusTone =
  | "success"
  | "warning"
  | "danger"
  | "info"
  | "neutral";

const STATUS_TONES: Record<string, SemanticStatusTone> = {
  active: "success",
  enabled: "success",
  paid: "success",
  completed: "success",
  success: "success",
  successful: "success",
  converted: "success",

  pending: "warning",
  trial: "warning",
  past_due: "warning",
  pending_activation: "warning",
  queued: "warning",
  retry: "warning",
  retrying: "warning",

  overdue: "danger",
  failed: "danger",
  failure: "danger",
  denied: "danger",
  suspended: "danger",
  cancelled: "danger",
  canceled: "danger",
  void: "danger",
  error: "danger",
  rejected: "danger",

  billed: "info",
  in_progress: "info",
  open: "info",
  reviewed: "info",

  draft: "neutral",
  inactive: "neutral",
  unknown: "neutral",
  ended: "neutral",
  archived: "neutral",
  terminated: "neutral",
  termination_scheduled: "neutral",
};

const TONE_CLASS_NAMES: Record<SemanticStatusTone, string> = {
  success:
    "border-(--status-completed-light) bg-(--status-completed-light) text-(--status-completed-dark)",
  warning:
    "border-(--dashboard-status-pending-light) bg-(--dashboard-status-pending-light) text-(--dashboard-status-pending-dark)",
  danger:
    "border-(--status-overdue-light) bg-(--status-overdue-light) text-(--status-overdue-dark)",
  info:
    "border-(--status-info-light) bg-(--status-info-light) text-(--status-info-dark)",
  neutral:
    "border-(--neutral-100) bg-(--neutral-100) text-(--badge-gray-text)",
};

function normalizeSemanticStatusKey(status: string): string {
  return status.trim().toLowerCase().replace(/[\s_-]+/g, "_");
}

function getSemanticStatusTone(status: string): SemanticStatusTone {
  return STATUS_TONES[normalizeSemanticStatusKey(status)] ?? "neutral";
}

function getSemanticStatusToneClassName(
  tone: SemanticStatusTone,
): string {
  return TONE_CLASS_NAMES[tone];
}

function getSemanticStatusClassName(status: string): string {
  return getSemanticStatusToneClassName(getSemanticStatusTone(status));
}

interface SemanticStatusBadgeProps {
  status: string;
  children?: ReactNode;
  className?: string;
}

function SemanticStatusBadge({
  status,
  children,
  className,
}: SemanticStatusBadgeProps) {
  return (
    <span className={cn(getSemanticStatusClassName(status), className)}>
      {children ?? status}
    </span>
  );
}

export default SemanticStatusBadge;
