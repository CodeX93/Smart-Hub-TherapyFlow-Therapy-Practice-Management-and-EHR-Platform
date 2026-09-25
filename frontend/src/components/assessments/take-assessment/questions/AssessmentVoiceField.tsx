import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { ContentLoader } from "@/components/shared/ContentLoader";
import { cn } from "@/lib/utils";
import { useSpeechRecognition } from "@/hooks/useSpeechRecognition";
import { AssessmentMicrophoneIcon } from "./AssessmentMicrophoneIcon";

interface AssessmentVoiceFieldProps {
  value: string;
  onChange: (value: string) => void;
  children: ReactNode;
  /** Align mic with single-line inputs vs taller textareas */
  align?: "center" | "start";
  className?: string;
}

const AssessmentVoiceField = ({
  value,
  onChange,
  children,
  align = "center",
  className,
}: AssessmentVoiceFieldProps) => {
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [translationEnabled, setTranslationEnabled] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const baselineValueRef = useRef(value);
  const valueRef = useRef(value);

  useEffect(() => {
    valueRef.current = value;
  }, [value]);

  const applySpeechText = useCallback(
    (spokenText: string) => {
      const baseline = baselineValueRef.current.trim();
      const next = spokenText.trim();
      if (!next) {
        onChange(baselineValueRef.current);
        return;
      }
      onChange(baseline ? `${baseline} ${next}` : next);
    },
    [onChange],
  );

  const { isListening, isSupported, start, stop } = useSpeechRecognition({
    lang: translationEnabled ? "en-US" : navigator.language || "en-US",
    onTranscript: applySpeechText,
    onError: (message) => setErrorMessage(message),
  });

  const handleTogglePanel = () => {
    if (isPanelOpen) {
      stop();
      setIsPanelOpen(false);
      setErrorMessage(null);
      return;
    }
    setErrorMessage(null);
    setIsPanelOpen(true);
  };

  const handleStartRecording = () => {
    setErrorMessage(null);
    baselineValueRef.current = valueRef.current;
    start();
  };

  const handleCancelRecording = () => {
    stop();
    onChange(baselineValueRef.current);
    setErrorMessage(null);
    setIsPanelOpen(false);
  };

  const handleConfirmRecording = () => {
    stop();
    setErrorMessage(null);
    setIsPanelOpen(false);
  };

  return (
    <div className={cn("space-y-4", className)}>
      <div
        className={cn(
          "flex gap-3",
          align === "center" ? "items-center" : "items-start",
        )}
      >
        <div className="min-w-0 flex-1">{children}</div>

        <button
          type="button"
          data-testid="assessment-voice-mic"
          onClick={handleTogglePanel}
          aria-label={isPanelOpen ? "Close voice input" : "Start voice input"}
          aria-pressed={isPanelOpen}
          className={cn(
            "flex size-6 shrink-0 items-center justify-center transition-colors cursor-pointer",
            align === "start" && "mt-4",
            isPanelOpen || isListening
              ? "text-(--text-primary-dark)"
              : "text-[#5B616E] hover:text-(--text-primary-dark)",
          )}
        >
          <AssessmentMicrophoneIcon className="size-6" />
        </button>
      </div>

      {isPanelOpen && (
        <div className="space-y-4 rounded-xl border border-(--neutral-200) bg-(--bg-primary-50) p-4 animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center justify-between gap-3">
            <span className="text-base font-medium text-(--text-primary-dark)">
              Voice Recording
            </span>
            <div className="flex items-center gap-3">
              <Switch
                checked={translationEnabled}
                onCheckedChange={setTranslationEnabled}
                disabled={isListening}
                className="scale-90 data-[state=checked]:bg-primary"
              />
              <span className="text-sm text-muted-foreground">
                Translate to English
              </span>
            </div>
          </div>

          {!isSupported ? (
            <p className="text-sm text-destructive">
              Speech recognition is not supported in this browser. Try Chrome or
              Edge.
            </p>
          ) : !isListening ? (
            <Button
              type="button"
              onClick={handleStartRecording}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-full text-sm font-medium shadow-sm"
            >
              <AssessmentMicrophoneIcon className="size-5" />
              Start Recording
            </Button>
          ) : (
            <div className="flex h-8 items-center justify-between px-2">
              <div className="mr-4 flex h-full w-full flex-1 items-center justify-between gap-0.5">
                {[...Array(40)].map((_, i) => (
                  <div
                    key={i}
                    className="w-0.5 animate-pulse rounded-full bg-gray-900"
                    style={{
                      height: `${((i * 7) % 40) + 30}%`,
                      animationDelay: `${i * 0.05}s`,
                      animationDuration: "0.8s",
                    }}
                  />
                ))}
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handleCancelRecording}
                  aria-label="Cancel recording"
                  className="p-2 text-muted-foreground transition-colors hover:text-(--text-primary-dark)"
                >
                  <X size={24} />
                </button>
                <button
                  type="button"
                  onClick={handleConfirmRecording}
                  aria-label="Confirm recording"
                  className="p-2 text-gray-900 transition-colors hover:text-black"
                >
                  <Check size={24} />
                </button>
              </div>
            </div>
          )}

          {isListening && (
            <div className="flex items-center gap-2 text-sm text-(--text-primary-dark)">
              <ContentLoader variant="inline" size="sm" />
              <span>Listening… speak now. Text will fill this field.</span>
            </div>
          )}

          {errorMessage && (
            <p className="text-sm text-destructive">{errorMessage}</p>
          )}

          <p className="text-sm text-muted-foreground">
            Record in any language, and it will be transcribed as-is.
          </p>
        </div>
      )}
    </div>
  );
};

export default AssessmentVoiceField;
