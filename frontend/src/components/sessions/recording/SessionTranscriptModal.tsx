import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { createPortal } from "react-dom";
import { Copy, Download, FileText, Sparkles, Users, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import type { StoredSessionTranscript } from "./sessionTranscriptStore";
import { isTranscriptReady } from "@/utils/recording/transcriptionApi";

interface SessionTranscriptModalProps {
  isOpen: boolean;
  onClose: () => void;
  transcript: StoredSessionTranscript | null;
  onSmartFill: () => void;
  onDownload: () => void;
  onDelete: () => void;
  onIdentifySpeakers?: () => void;
  isLoading?: boolean;
  isSmartFillLoading?: boolean;
  isDownloadLoading?: boolean;
  isDeleteLoading?: boolean;
  isDiarizeLoading?: boolean;
}

function TranscriptBody({ text, speakersIdentified }: { text: string; speakersIdentified: boolean }) {
  if (!speakersIdentified) {
    return (
      <pre className="whitespace-pre-wrap font-sans text-sm leading-7 text-[#1B1C20]">
        {text}
      </pre>
    );
  }

  const lines = text.split("\n");
  return (
    <div className="space-y-0.5 font-sans text-sm leading-7 text-[#1B1C20]">
      {lines.map((line, index) => {
        const trimmed = line.trimStart();
        if (trimmed.startsWith("Therapist:")) {
          return (
            <p key={index} className="whitespace-pre-wrap">
              <span className="font-semibold text-[#3C4D58]">Therapist:</span>
              <span>{line.slice(line.indexOf("Therapist:") + "Therapist:".length)}</span>
            </p>
          );
        }
        if (trimmed.startsWith("Client:")) {
          return (
            <p key={index} className="whitespace-pre-wrap">
              <span className="font-semibold text-[#5B616E]">Client:</span>
              <span>{line.slice(line.indexOf("Client:") + "Client:".length)}</span>
            </p>
          );
        }
        if (/^\[\d{2}:\d{2}:\d{2}\]$/.test(trimmed)) {
          return (
            <p key={index} className="mt-2 whitespace-pre-wrap text-xs font-medium text-[#8B919C]">
              {line}
            </p>
          );
        }
        return (
          <p key={index} className="whitespace-pre-wrap">
            {line || "\u00A0"}
          </p>
        );
      })}
    </div>
  );
}

const SessionTranscriptModal = ({
  isOpen,
  onClose,
  transcript,
  onSmartFill,
  onDownload,
  onDelete,
  onIdentifySpeakers,
  isLoading = false,
  isSmartFillLoading = false,
  isDownloadLoading = false,
  isDeleteLoading = false,
  isDiarizeLoading = false,
}: SessionTranscriptModalProps) => {
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  const [showWithoutDiarization, setShowWithoutDiarization] = useState(false);

  const resetKey = JSON.stringify([transcript?.transcriptId, transcript?.sessionId, transcript?.diarizedTranscript]);
  const [previousResetKey, setPreviousResetKey] = useState(resetKey);
  if (previousResetKey !== resetKey) {
    setPreviousResetKey(resetKey);
    setShowWithoutDiarization(false);
  }

  if (!isOpen) return null;

  const canSmartFill = transcript ? isTranscriptReady(transcript.status) : false;
  const speakersIdentified = Boolean(transcript?.diarizedTranscript?.trim());
  const canIdentifySpeakers =
    Boolean(onIdentifySpeakers) &&
    Boolean(transcript) &&
    isTranscriptReady(transcript?.status) &&
    !speakersIdentified;

  const displayText =
    speakersIdentified && !showWithoutDiarization
      ? (transcript?.diarizedTranscript ?? transcript?.formattedTranscript ?? "")
      : (transcript?.rawTranscription || transcript?.formattedTranscript || "");

  const handleCopy = () => {
    if (!displayText) return;
    void navigator.clipboard.writeText(displayText);
  };

  const handleClose = () => {
    setIsDeleteConfirmOpen(false);
    onClose();
  };

  return createPortal(
    <div
      data-scheduling-nested-modal
      className="fixed inset-0 z-[10000] flex items-center justify-center bg-[#111727]/30 p-4"
      onMouseDown={(event) => event.stopPropagation()}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="session-transcript-title"
        className="flex max-h-[calc(100dvh-2rem)] w-full max-w-[52rem] flex-col overflow-hidden rounded-3xl bg-white shadow-[0_12px_28px_rgba(0,0,0,0.14),0_6px_12px_-6px_rgba(0,0,0,0.12)]"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="flex shrink-0 items-start justify-between gap-4 px-5 py-5 sm:px-6">
          <div className="min-w-0 space-y-[0.5625rem]">
            <h2
              id="session-transcript-title"
              className="text-xl font-semibold leading-7 text-[#1B1C20]"
            >
              Session Transcript
            </h2>
            <p className="text-sm leading-[1.375rem] text-[#5B616E]">
              Review the transcript, save a copy, or use it to fill the session note.
            </p>
          </div>
          <button
            type="button"
            onClick={handleClose}
            aria-label="Close session transcript"
            className="flex size-11 shrink-0 cursor-pointer items-center justify-center rounded-full text-[#1B1C20] transition-colors hover:bg-[#F6F6F6] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#3C4D58]/30"
          >
            <X size={20} strokeWidth={2} />
          </button>
        </div>

        <div className="min-h-0 overflow-y-auto px-5 pt-3 pb-6 sm:px-6">
          <div className="rounded-xl border border-[#EDEEF1] bg-white p-4 sm:p-5">
            {isLoading ? (
              <div
                className="flex min-h-64 flex-col items-center justify-center gap-4 text-center"
                role="status"
                aria-live="polite"
              >
                <span className="flex size-12 items-center justify-center rounded-full bg-[#F5F7FF] text-[#3C4D58]">
                  <ContentLoader size="lg" />
                </span>
                <div className="space-y-1">
                  <h3 className="text-base font-semibold leading-6 text-[#1B1C20]">
                    Loading transcript
                  </h3>
                  <p className="text-sm leading-[1.375rem] text-[#5B616E]">
                    Preparing the saved recording for review.
                  </p>
                </div>
              </div>
            ) : transcript ? (
              <>
                <div className="mb-5 flex flex-col gap-4">
                  <div className="flex min-w-0 items-start gap-3">
                    <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-[#F5F7FF] text-[#3C4D58]">
                      <FileText size={20} />
                    </span>
                    <div className="min-w-0">
                      <h3 className="text-base font-semibold leading-6 text-[#1B1C20]">
                        Recorded session
                      </h3>
                      <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs leading-[1.125rem] text-[#5B616E]">
                        <span>
                          Duration{" "}
                          {Math.floor(transcript.durationSeconds / 60)
                            .toString()
                            .padStart(2, "0")}
                          :{(transcript.durationSeconds % 60).toString().padStart(2, "0")}
                        </span>
                        <span aria-hidden="true">•</span>
                        <span>{transcript.wordCount} words</span>
                        <span className="inline-flex h-6 items-center rounded-full bg-[#D0FBE3] px-2.5 font-medium text-[#007C54]">
                          Saved
                        </span>
                        {speakersIdentified && !showWithoutDiarization ? (
                          <span className="inline-flex h-6 items-center rounded-full bg-[#E8EEF2] px-2.5 font-medium text-[#3C4D58]">
                            Speakers identified
                          </span>
                        ) : null}
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <Button
                      variant="outline"
                      onClick={onDownload}
                      disabled={isDownloadLoading}
                      loading={isDownloadLoading}
                      loadingLabel="Downloading..."
                      className="h-10 rounded-xl border-[#D8DBDF] bg-white px-3 text-xs text-[#1B1C20]"
                    >
                      <Download size={16} />
                      Download
                    </Button>
                    <Button
                      variant="outline"
                      onClick={handleCopy}
                      className="h-10 rounded-xl border-[#D8DBDF] bg-white px-3 text-xs text-[#1B1C20]"
                    >
                      <Copy size={16} />
                      Copy
                    </Button>
                    {speakersIdentified ? (
                      <Button
                        variant="outline"
                        onClick={() => setShowWithoutDiarization((prev) => !prev)}
                        className="h-10 rounded-xl border-[#D8DBDF] bg-white px-3 text-xs text-[#1B1C20]"
                      >
                        <Users size={16} />
                        {showWithoutDiarization
                          ? "Show with speakers"
                          : "Show without diarization"}
                      </Button>
                    ) : null}
                    {canIdentifySpeakers ? (
                      <Button
                        variant="outline"
                        onClick={onIdentifySpeakers}
                        disabled={isDiarizeLoading}
                        loading={isDiarizeLoading}
                        loadingLabel="Identifying..."
                        className="h-10 rounded-xl border-[#D8DBDF] bg-white px-3 text-xs text-[#1B1C20]"
                      >
                        <Users size={16} />
                        Identify Speakers
                      </Button>
                    ) : null}
                    <Button
                      onClick={onSmartFill}
                      disabled={isSmartFillLoading || !canSmartFill}
                      loading={isSmartFillLoading}
                      loadingLabel="Applying transcript..."
                      className="h-10 rounded-xl bg-[#3C4D58] px-3 text-xs font-semibold text-white hover:bg-[#323E47] disabled:opacity-50"
                    >
                      <Sparkles size={16} />
                      Smart fill note
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => setIsDeleteConfirmOpen(true)}
                      disabled={isDeleteLoading}
                      loading={isDeleteLoading}
                      loadingLabel="Deleting..."
                      className="h-10 rounded-xl border-red-200 bg-white px-3 text-xs text-red-600 hover:bg-red-50"
                    >
                      <TrashIcon size={16} />
                      Delete
                    </Button>
                  </div>
                </div>

                <div className="max-h-[48vh] min-h-48 overflow-y-auto rounded-xl border border-[#EDEEF1] bg-[#FAFAFB] p-4">
                  <TranscriptBody
                    text={displayText}
                    speakersIdentified={speakersIdentified && !showWithoutDiarization}
                  />
                </div>

                <p className="mt-3 text-xs leading-[1.125rem] text-[#5B616E]">
                  {speakersIdentified
                    ? showWithoutDiarization
                      ? "Showing the original transcript before speaker labels were applied."
                      : "Speaker labels are AI-inferred from conversational patterns (not voice biometrics). Use Show without diarization to view the original."
                    : "Saved as a separate session artifact. Review the content before applying it to the session note."}
                </p>
              </>
            ) : (
              <div className="flex min-h-64 flex-col items-center justify-center gap-3 text-center">
                <div className="relative mb-1">
                  <div className="flex size-14 items-center justify-center rounded-2xl bg-[#EEF1F6] text-[#3C4D58]">
                    <FileText size={24} strokeWidth={1.5} />
                  </div>
                  <div className="absolute -bottom-1 left-1/2 h-2 w-10 -translate-x-1/2 rounded-[50%] bg-[#D8DEE6] opacity-60 blur-[2px]" />
                </div>
                <div className="flex flex-col items-center gap-1.5">
                  <h3 className="text-base font-semibold leading-6 text-(--text-primary-dark)">
                    Transcript unavailable
                  </h3>
                  <p className="max-w-80 text-sm leading-[1.375rem] text-(--text-neutral-600)">
                    No saved transcript was found for this session.
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <ConfirmationModal
        type="delete"
        isOpen={isDeleteConfirmOpen}
        onClose={() => {
          if (isDeleteLoading) return;
          setIsDeleteConfirmOpen(false);
        }}
        onConfirm={onDelete}
        title="Delete session transcript?"
        description="Are you sure you want to delete this transcript? This action cannot be undone."
        items={[]}
        confirmButtonText="Delete transcript"
        confirmButtonLoading={isDeleteLoading}
        confirmButtonLoadingText="Deleting..."
        overlayClassName="z-[10001]"
      />
    </div>,
    document.body,
  );
};

export default SessionTranscriptModal;
