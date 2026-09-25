import { Info, Sparkles } from "lucide-react";

import { Button } from "../ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../ui/tooltip";

interface SessionNoteClinicalActionsMenuProps {
  disabled?: boolean;
  isSmartFilling?: boolean;
  /** Whether this session has a transcript to fill the fields from. */
  hasTranscript?: boolean;
  onSmartFill: () => void;
}

const NO_TRANSCRIPT_EXPLANATION =
  "Smart Fill reads this session's transcript and drafts the clinical fields from it. There is no transcript yet — record the session first, from the session's menu.";

const SessionNoteClinicalActionsMenu = ({
  disabled = false,
  isSmartFilling = false,
  hasTranscript = false,
  onSmartFill,
}: SessionNoteClinicalActionsMenuProps) => (
  <div className="flex shrink-0 items-center gap-2">
    {/* A disabled button that cannot say why is just a dead control. */}
    {!hasTranscript && !disabled ? (
      <TooltipProvider delayDuration={100}>
        <Tooltip>
          <TooltipTrigger asChild>
            <button
              type="button"
              aria-label="Why is Smart Fill unavailable?"
              className="flex size-7 items-center justify-center rounded-full text-(--text-neutral-400) transition-colors hover:bg-(--neutral-50) hover:text-(--text-neutral-600)"
            >
              <Info size={16} />
            </button>
          </TooltipTrigger>
          <TooltipContent side="bottom" className="max-w-72 text-left">
            {NO_TRANSCRIPT_EXPLANATION}
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    ) : null}
    <Button
      type="button"
      disabled={disabled || isSmartFilling || !hasTranscript}
      loading={isSmartFilling}
      loadingLabel="Smart filling..."
      onClick={onSmartFill}
      title={!hasTranscript ? NO_TRANSCRIPT_EXPLANATION : undefined}
      className="h-10 shrink-0 gap-2 rounded-full bg-(--bg-primary-dark) px-5 text-sm font-semibold text-white hover:bg-(--bg-primary-dark)/90 disabled:opacity-50"
    >
      <Sparkles size={16} />
      Smart Fill from transcript
    </Button>
  </div>
);

export default SessionNoteClinicalActionsMenu;
