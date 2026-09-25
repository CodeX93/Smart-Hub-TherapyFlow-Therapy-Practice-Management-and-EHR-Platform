import React from "react";
import { Info, Video } from "lucide-react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

interface Props {
  sessionMode: string;
  options: Array<{ optionKey: string; optionLabel: string }>;
  isInPersonMode: (optionKey: string) => boolean;
  onChange: (optionKey: string) => void;
  /** False when the hosting therapist has no Zoom credentials, so no meeting can be created. */
  onlineAvailable?: boolean;
  /** Shown in the tooltip. Differs for your own account and someone else's. */
  onlineUnavailableReason?: string;
  /** Only passed when the caller can actually reach the setup, i.e. it is their own account. */
  onOpenZoomSettings?: () => void;
}

const SessionModeSelector: React.FC<Props> = ({
  sessionMode,
  options,
  isInPersonMode,
  onChange,
  onlineAvailable = true,
  onlineUnavailableReason = "Zoom is not connected, so online sessions can't be created.",
  onOpenZoomSettings,
}) => {
  const selectedIsVirtual = !isInPersonMode(sessionMode);

  return (
    <div>
      <label className="text-sm font-semibold text-(--text-primary-dark) mb-2 block">
        Session Mode
      </label>
      <div
        className={`grid gap-3 ${
          options.length <= 2 ? "grid-cols-2" : "grid-cols-1 sm:grid-cols-2"
        }`}
      >
        {options.map((option) => {
          const isSelected = sessionMode === option.optionKey;
          const inPerson = isInPersonMode(option.optionKey);
          const isDisabled = !inPerson && !onlineAvailable;

          return (
            // The tooltip trigger sits outside the button on purpose: a disabled button
            // dispatches no pointer events, so an icon nested inside it never hovers.
            <div key={option.optionKey} className="relative">
            <button
              type="button"
              disabled={isDisabled}
              onClick={() => {
                if (isDisabled) return;
                onChange(option.optionKey);
              }}
              className={`flex w-full items-center gap-3 p-4 rounded-xl border transition-all ${
                isDisabled
                  ? "cursor-not-allowed border-(--neutral-100) opacity-60"
                  : isSelected
                    ? "cursor-pointer border-(--bg-primary-dark)"
                    : "cursor-pointer border-(--neutral-100) hover:border-(--neutral-200)"
              }`}
            >
              <div className="mt-0.5">
                <div
                  className={`w-5 h-5 rounded-full border-6 flex items-center justify-center ${
                    isSelected && !isDisabled
                      ? "border-(--bg-primary-dark)"
                      : "border-(--neutral-200)"
                  }`}
                />
              </div>
              <div className="flex-1 text-left">
                <div className="font-medium text-(--text-primary-dark) text-sm">
                  {option.optionLabel}
                </div>
                <div className="text-xs text-(--text-neutral-600) mt-0.5">
                  {inPerson ? "Face to face at clinic" : "Online video session"}
                </div>
              </div>
            </button>

            {isDisabled && (
              <div className="absolute right-3 top-3">
                <TooltipProvider>
                  <Tooltip delayDuration={200}>
                    <TooltipTrigger asChild>
                      <button
                        type="button"
                        aria-label="Why is this unavailable?"
                        onClick={(event) => event.preventDefault()}
                        className="inline-flex cursor-help text-(--text-neutral-400) hover:text-(--neutral-950)"
                      >
                        <Info size={16} />
                      </button>
                    </TooltipTrigger>
                    <TooltipContent className="max-w-64">
                      <p>{onlineUnavailableReason}</p>
                      {onOpenZoomSettings && (
                        <button
                          type="button"
                          onClick={onOpenZoomSettings}
                          className="mt-1 underline underline-offset-2 cursor-pointer"
                        >
                          Go to Zoom settings
                        </button>
                      )}
                    </TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              </div>
            )}
            </div>
          );
        })}
      </div>

      {selectedIsVirtual && onlineAvailable && (
        <div className="flex items-start gap-3 p-3 bg-(--neutral-50) rounded-lg mt-3">
          <Video />
          <div>
            <div className="text-sm font-medium text-(--text-primary-dark)">
              Secure video call will be created automatically
            </div>
            <div className="text-xs text-(--text-neutral-600) mt-0.5">
              Meeting link will be generated after booking
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SessionModeSelector;
