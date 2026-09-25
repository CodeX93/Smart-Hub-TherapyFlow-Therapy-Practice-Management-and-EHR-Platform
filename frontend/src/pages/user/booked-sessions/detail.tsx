
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import RateSessionModal from "@/components/booked-sessions/RateSessionModal";
import Toast from "@/components/shared/Toast";
import SessionStatusBadge from "@/components/shared/SessionStatusBadge";
import { Button } from "@/components/ui/button";
import {
  useGetPortalAppointmentByIdQuery,
  useRatePortalSessionMutation,
} from "@/store/api/portalApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  formatBookedSessionDate,
  formatBookedSessionRate,
  formatBookedSessionTime,
  hasSubmittedSessionRating,
  isCompletedBookedSession,
} from "@/utils/bookedSessionDisplay";
import {
  getSessionModalityLabel,
} from "@/utils/portalSessionDisplay";

const BookedSessionDetail = () => {
  const navigate = useNavigate();
  const { sessionId } = useParams<{ sessionId: string }>();
  const parsedSessionId = Number.parseInt(sessionId || "", 10);
  const isValidSessionId = !Number.isNaN(parsedSessionId) && parsedSessionId > 0;

  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("error");
  const [isRateModalOpen, setIsRateModalOpen] = useState(false);

  const {
    data: session,
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useGetPortalAppointmentByIdQuery(parsedSessionId, {
    skip: !isValidSessionId,
    refetchOnMountOrArgChange: true,
  });

  const [rateSession, { isLoading: isRating }] = useRatePortalSessionMutation();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3200);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const [reportedError, setReportedError] = useState<unknown>(null);
  if (isError && error !== reportedError) {
    setReportedError(error);
    setToastType("error");
    setToastMessage(getApiErrorMessage(error));
  }

  const handleSubmitRating = async (rating: number, comment?: string) => {
    if (!session || hasSubmittedSessionRating(session)) return;

    try {
      const response = await rateSession({
        sessionId: session.id,
        body: { rating, comment },
      }).unwrap();

      setIsRateModalOpen(false);
      setToastType("success");
      setToastMessage(
        response.taskCreated
          ? "Thank you for your feedback. Your therapist has been notified."
          : "Thank you for rating your session.",
      );
      await refetch();
    } catch (rateError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(rateError));
    }
  };

  if (!isValidSessionId) {
    return (
      <div className="rounded-xl border border-(--neutral-100) bg-white px-5 py-12 text-center text-sm text-(--text-neutral-600)">
        Invalid session link.
      </div>
    );
  }

  const isInitialLoading = (isLoading || isFetching) && !session;

  return (
    <div className="mx-auto w-full max-w-3xl">
      <button
        type="button"
        onClick={() => navigate("/user/booked-sessions")}
        className="mb-4 inline-flex items-center gap-2 text-sm font-medium text-(--text-primary-500) transition hover:opacity-80"
      >
        <ArrowLeft size={18} />
        Back to booked sessions
      </button>

      {isInitialLoading ? (
        <ContentLoader size="md" className="gap-2 rounded-xl border border-(--neutral-100) bg-white py-16 text-sm text-(--text-neutral-600)" />
      ) : !session ? (
        <div className="rounded-xl border border-(--neutral-100) bg-white px-5 py-12 text-center text-sm text-(--text-neutral-600)">
          Appointment not found.
        </div>
      ) : (
        <div className="rounded-2xl border border-(--neutral-100) bg-white p-6 shadow-xs">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h1 className="text-2xl font-bold text-(--text-primary-dark)">
                {session.serviceName || session.sessionType || "Therapy Session"}
              </h1>
              <p className="mt-1 text-sm text-(--text-neutral-600)">
                {formatBookedSessionDate(session.sessionDate)} at{" "}
                {formatBookedSessionTime(session.sessionDate, session.sessionTime)}
              </p>
            </div>
            <SessionStatusBadge
              status={session.status}
              className="px-3 py-1 font-semibold"
            />
          </div>

          <div className="mt-6 grid gap-4 sm:grid-cols-2">
            <DetailField label="Therapist" value={session.therapistName || "—"} />
            <DetailField
              label="Session mode"
              value={getSessionModalityLabel(session.sessionMode)}
            />
            <DetailField
              label="Duration"
              value={session.duration ? `${session.duration} minutes` : "—"}
            />
            <DetailField label="Location" value={session.location || "—"} />
            <DetailField label="Room" value={session.roomName || "—"} />
            <DetailField label="Service code" value={session.serviceCode || "—"} />
            <DetailField label="Reference" value={session.referenceNumber || "—"} />
            <DetailField
              label="Service rate"
              value={formatBookedSessionRate(session.serviceRate)}
            />
          </div>

          {isCompletedBookedSession(session.status) ? (
            <div className="mt-6 flex justify-end border-t border-(--neutral-100) pt-5">
              <Button
                type="button"
                variant={hasSubmittedSessionRating(session) ? "outline" : "default"}
                onClick={() => setIsRateModalOpen(true)}
                className={
                  hasSubmittedSessionRating(session)
                    ? "h-10 rounded-full px-5"
                    : "h-10 rounded-full bg-(--bg-primary-dark) px-5 text-white hover:bg-(--bg-primary-dark)/90"
                }
              >
                {hasSubmittedSessionRating(session)
                  ? "View rating"
                  : "Rate this session"}
              </Button>
            </div>
          ) : null}
        </div>
      )}

      <RateSessionModal
        isOpen={isRateModalOpen}
        session={session ?? null}
        isSubmitting={isRating}
        onClose={() => {
          if (isRating) return;
          setIsRateModalOpen(false);
        }}
        onSubmit={(rating, comment) => void handleSubmitRating(rating, comment)}
      />

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

function DetailField({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-(--bg-primary-light) px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
        {label}
      </p>
      <p className="mt-1 text-sm font-medium text-(--text-primary-dark)">{value}</p>
    </div>
  );
}

export default BookedSessionDetail;
