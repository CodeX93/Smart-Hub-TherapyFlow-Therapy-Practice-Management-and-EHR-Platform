import { AlertTriangle, CheckCircle2, ClipboardList, Clock } from "lucide-react";
import InProgressSpokesIcon from "@/components/shared/InProgressSpokesIcon";
import { cn } from "@/lib/utils";

export interface TaskSummaryCounts {
  total: number;
  pending: number;
  inProgress: number;
  completed: number;
  needsAttention: number;
}

interface TaskSummaryCardsProps {
  counts: TaskSummaryCounts;
  className?: string;
}

const cards = [
  { key: "total", label: "Total Tasks", icon: "list" },
  { key: "pending", label: "Pending", icon: "clock" },
  { key: "inProgress", label: "In Progress", icon: "loader" },
  { key: "completed", label: "Completed", icon: "check-circle" },
  { key: "needsAttention", label: "Needs Attention", icon: "alert-triangle" },
] as const;

function SummaryIcon({
  name,
}: {
  name: (typeof cards)[number]["icon"];
}) {
  const isAttention = name === "alert-triangle";

  return (
    <div
      className={cn(
        "flex size-12 shrink-0 items-center justify-center rounded-xl",
        isAttention
          ? "border border-[#fff7c5] bg-[#fffdea] text-amber-500"
          : "bg-(--bg-primary-50) text-(--text-primary-500)",
      )}
    >
      {name === "list" ? (
        <ClipboardList className="size-6" strokeWidth={1.75} />
      ) : name === "clock" ? (
        <Clock className="size-6" strokeWidth={1.75} />
      ) : name === "loader" ? (
        <InProgressSpokesIcon className="size-6" />
      ) : name === "check-circle" ? (
        <CheckCircle2 className="size-6" strokeWidth={1.75} />
      ) : (
        <AlertTriangle className="size-6" strokeWidth={1.75} />
      )}
    </div>
  );
}

/**
 * Task management summary row — matches Figma Header Section (922:16340).
 */
function TaskSummaryCards({ counts, className }: TaskSummaryCardsProps) {
  const values: Record<(typeof cards)[number]["key"], number> = {
    total: counts.total,
    pending: counts.pending,
    inProgress: counts.inProgress,
    completed: counts.completed,
    needsAttention: counts.needsAttention,
  };

  return (
    <div
      className={cn(
        "flex w-full items-center gap-4 overflow-x-auto pb-2 lg:pb-0 [&>*]:min-w-[12rem]",
        className,
      )}
    >
      {cards.map((card) => (
        <div
          key={card.key}
          className="flex flex-1 flex-col items-start rounded-xl border border-(--neutral-100) bg-white p-4 shadow-[0_2px_1px_var(--shadow)]"
        >
          <div className="flex w-full items-center justify-between">
            <div className="flex min-w-0 flex-col gap-2">
              <span className="whitespace-nowrap text-base leading-6 font-normal text-(--text-neutral-600)">
                {card.label}
              </span>
              <span className="text-xl leading-7 font-bold text-(--neutral-950)">
                {values[card.key]}
              </span>
            </div>
            <SummaryIcon name={card.icon} />
          </div>
        </div>
      ))}
    </div>
  );
}

export default TaskSummaryCards;
