import { Check } from "lucide-react";

import { cn } from "@/lib/utils";

export type SessionNoteStepState = "empty" | "partial" | "complete";

export interface SessionNoteStep {
  key: string;
  label: string;
  state: SessionNoteStepState;
  required?: boolean;
}

interface SessionNoteStepsProps {
  steps: SessionNoteStep[];
  activeKey: string;
  onSelect: (key: string) => void;
}

/**
 * The note is a sequence, not a set of interchangeable tabs: the steps are
 * numbered and each carries whether it has been filled in, so a clinician can
 * see what is still outstanding without opening every one.
 */
const SessionNoteSteps = ({ steps, activeKey, onSelect }: SessionNoteStepsProps) => (
  // Same segmented pill as the app's other tab bars (SettingsTabs); a finished
  // step's marker takes the brand's selected colour, like a chosen option pill.
  <div className="mb-6 flex w-full flex-wrap items-center gap-1 rounded-full border border-(--neutral-100) bg-(--neutral-100) p-1 shadow-xs">
    {steps.map((step, index) => {
      const isActive = step.key === activeKey;
      const isComplete = step.state === "complete";
      const needsAttention = step.required && step.state !== "complete";

      return (
        <button
          key={step.key}
          type="button"
          onClick={() => onSelect(step.key)}
          aria-current={isActive ? "step" : undefined}
          className={cn(
            "flex flex-1 basis-40 cursor-pointer items-center justify-center gap-2 rounded-full px-4 py-2.5 text-sm font-medium transition-all duration-300",
            isActive
              ? "bg-white text-(--text-primary-dark) shadow-xs"
              : "text-(--text-neutral-600) hover:text-(--text-primary-dark)",
          )}
        >
          <span
            className={cn(
              "flex size-5 shrink-0 items-center justify-center rounded-full text-[0.6875rem] font-semibold",
              isComplete
                ? "bg-(--bg-primary-dark) text-white"
                : isActive
                  ? "bg-(--neutral-100) text-(--text-primary-dark)"
                  : "bg-white text-(--text-neutral-600)",
            )}
          >
            {isComplete ? <Check size={12} strokeWidth={3} aria-hidden="true" /> : index + 1}
          </span>
          <span className="truncate">{step.label}</span>
          {needsAttention ? (
            <span className="shrink-0 rounded-full bg-(--status-pending-light) px-2 py-0.5 text-[0.6875rem] font-medium text-(--status-pending-dark)">
              Required
            </span>
          ) : null}
        </button>
      );
    })}
  </div>
);

export default SessionNoteSteps;
