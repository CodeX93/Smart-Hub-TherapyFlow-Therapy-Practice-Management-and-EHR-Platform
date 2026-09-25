import { useState } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "@/components/form/CustomInput";
import CustomTextarea from "@/components/form/CustomTextarea";
import type { PortalAppointment } from "@/store/api/portalApi";
import {
  formatBookedSessionDate,
  formatBookedSessionTime,
  hasSubmittedSessionRating,
} from "@/utils/bookedSessionDisplay";

interface RateSessionModalProps {
  isOpen: boolean;
  session: PortalAppointment | null;
  isSubmitting?: boolean;
  onClose: () => void;
  onSubmit: (rating: number, comment?: string) => void;
}

const RateSessionModalContent = ({
  isOpen,
  session,
  isSubmitting = false,
  onClose,
  onSubmit,
}: RateSessionModalProps) => {
  const [rating, setRating] = useState(() => session && hasSubmittedSessionRating(session) ? String(session.clientRating) : "");
  const [comment, setComment] = useState(() => session?.clientRatingComment?.trim() || "");
  const [error, setError] = useState<string | null>(null);

  const isViewMode = Boolean(session && hasSubmittedSessionRating(session));



  if (!isOpen || !session) return null;

  const handleSubmit = () => {
    if (isViewMode) return;

    const parsedRating = Number.parseInt(rating, 10);
    if (
      rating.trim() === "" ||
      Number.isNaN(parsedRating) ||
      parsedRating < 0 ||
      parsedRating > 10
    ) {
      setError("Please enter a rating between 0 and 10.");
      return;
    }

    setError(null);
    onSubmit(parsedRating, comment.trim() || undefined);
  };

  return (
    <div className="fixed inset-0 z-100 flex items-center justify-center bg-black/40 p-4 backdrop-blur-[0.125rem]">
      <div className="relative w-full max-w-lg min-w-0 overflow-hidden rounded-2xl bg-white p-6 shadow-xl">
        <button
          type="button"
          onClick={onClose}
          disabled={isSubmitting}
          className="absolute right-4 top-4 rounded-full p-1 text-(--text-neutral-500) transition hover:bg-(--neutral-100)"
          aria-label="Close"
        >
          <X size={20} />
        </button>

        <h2 className="pr-8 text-xl font-bold text-(--text-primary-dark)">
          {isViewMode ? "Your rating" : "Rate your session"}
        </h2>
        <p className="mt-1 break-words text-sm text-(--text-neutral-600)">
          {session.serviceName || session.sessionType || "Therapy Session"} ·{" "}
          {formatBookedSessionDate(session.sessionDate)} at{" "}
          {formatBookedSessionTime(session.sessionDate, session.sessionTime)}
        </p>

        <div className="mt-6 min-w-0 space-y-4">
          <div>
            <CustomInput
              id="session-rating"
              name="sessionRating"
              label="Rating (0–10)"
              type="number"
              min={0}
              max={10}
              step={1}
              value={rating}
              onChange={(event) => setRating(event.target.value)}
              disabled={isViewMode || isSubmitting}
              hasError={Boolean(error)}
              aria-describedby={error ? "session-rating-error" : undefined}
            />
          </div>

          {isViewMode ? (
            comment ? (
              <div className="min-w-0 overflow-hidden rounded-xl border border-(--neutral-100) bg-(--bg-primary-light) px-4 py-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                  Your comment
                </p>
                <p className="mt-1 max-w-full whitespace-pre-wrap break-all text-sm text-(--text-primary-dark)">
                  {comment}
                </p>
              </div>
            ) : (
              <p className="text-sm text-(--text-neutral-600)">
                No comment was submitted with this rating.
              </p>
            )
          ) : (
            <div className="min-w-0">
              <CustomTextarea
                id="session-rating-comment"
                name="sessionRatingComment"
                label="Comment (optional)"
                value={comment}
                onChange={(event) => setComment(event.target.value)}
                placeholder="Share what went well or what could be improved..."
                className="min-h-28 max-w-full rounded-xl break-all"
              />
            </div>
          )}

          {error ? (
            <p id="session-rating-error" role="alert" className="text-sm text-(--status-denied)">
              {error}
            </p>
          ) : null}
        </div>

        <div className="mt-6 flex justify-end gap-2">
          {isViewMode ? (
            <Button
              type="button"
              onClick={onClose}
              className="h-10 rounded-full bg-(--bg-primary-dark) px-5 text-white hover:bg-(--bg-primary-dark)/90"
            >
              Close
            </Button>
          ) : (
            <>
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isSubmitting}
                className="h-10 rounded-full px-5"
              >
                Cancel
              </Button>
              <Button
                type="button"
                onClick={handleSubmit}
                disabled={isSubmitting}
                loading={isSubmitting}
                loadingLabel="Submitting..."
                className="h-10 rounded-full bg-(--bg-primary-dark) px-5 text-white hover:bg-(--bg-primary-dark)/90"
              >
                Submit rating
              </Button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};

const RateSessionModal = (props: RateSessionModalProps) => props.isOpen && Boolean(props.session) ? <RateSessionModalContent key={props.session?.id} {...props} /> : null;

export default RateSessionModal;
