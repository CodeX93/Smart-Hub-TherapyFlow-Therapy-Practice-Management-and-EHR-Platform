import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";

import { ContentLoader } from "@/components/shared/ContentLoader";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search } from "lucide-react";
import BookedSessionCard from "@/components/booked-sessions/BookedSessionCard";
import RateSessionModal from "@/components/booked-sessions/RateSessionModal";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "@/components/form/CustomSelect";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  PORTAL_APPOINTMENTS_PAGE_SIZE,
  useGetPortalAppointmentsQuery,
  useRatePortalSessionMutation,
  type PortalAppointment,
} from "@/store/api/portalApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  BOOKED_SESSION_STATUS_OPTIONS,
  filterBookedSessions,
  hasSubmittedSessionRating,
} from "@/utils/bookedSessionDisplay";
import { usePortalNotificationUnreadBootstrap } from "@/hooks/useNotificationUnreadBootstrap";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";

const BookedSessions = () => {
  usePortalNotificationUnreadBootstrap();
  const navigate = useNavigate();
  const listScrollRef = useRef<HTMLDivElement>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("error");
  const [ratingSession, setRatingSession] = useState<PortalAppointment | null>(null);
  const [isRateModalOpen, setIsRateModalOpen] = useState(false);
  const [page, setPage] = useScopedPage(statusFilter);




  const listQueryArgs = useMemo(
    () => ({
      page,
      pageSize: PORTAL_APPOINTMENTS_PAGE_SIZE,
      ...(statusFilter.trim() ? { status: statusFilter.trim() } : {}),
    }),
    [page, statusFilter],
  );

  const {
    currentData: appointmentsPage,
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useGetPortalAppointmentsQuery(listQueryArgs, {
    refetchOnMountOrArgChange: true,
  });

  const [rateSession, { isLoading: isRating }] = useRatePortalSessionMutation();



  const { items: accumulatedSessions, isReadyToLoadMore } = usePagedItems(appointmentsPage?.items, page, statusFilter);
  const totalPages = appointmentsPage?.totalPages ?? 1;
  const isLoadingMore = isFetching && page > 1;

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

  const filteredSessions = useMemo(
    () => filterBookedSessions(accumulatedSessions, searchQuery, ""),
    [searchQuery, accumulatedSessions],
  );

  const hasMore = isReadyToLoadMore && page < totalPages;
  const isInitialLoading =
    (isLoading || isFetching) && accumulatedSessions.length === 0;
  const isFetchingMore =
    isLoadingMore || (isFetching && page > 1 && accumulatedSessions.length > 0);

  const handleLoadMore = useCallback(() => {
    if (!hasMore || isLoading || isFetching || isLoadingMore) return;
    setPage((currentPage) => currentPage + 1);
  }, [hasMore, isLoading, isFetching, isLoadingMore, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore,
    isLoading: isFetchingMore || isInitialLoading,
    scrollRootRef: listScrollRef,
  });

  const handleOpenSession = (sessionId: number) => {
    navigate(`/user/booked-sessions/${sessionId}`);
  };

  const handleOpenRateModal = (session: PortalAppointment) => {
    setRatingSession(session);
    setIsRateModalOpen(true);
  };

  const resetListToFirstPage = () => {
    setPage(1);
  };

  const handleSubmitRating = async (rating: number, comment?: string) => {
    if (!ratingSession || hasSubmittedSessionRating(ratingSession)) return;

    try {
      const response = await rateSession({
        sessionId: ratingSession.id,
        body: { rating, comment },
      }).unwrap();

      setIsRateModalOpen(false);
      setRatingSession(null);
      setToastType("success");
      setToastMessage(
        response.taskCreated
          ? "Thank you for your feedback. Your therapist has been notified."
          : "Thank you for rating your session.",
      );
      resetListToFirstPage();
      if (page === 1) {
        await refetch();
      }
    } catch (rateError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(rateError));
    }
  };

  return (
    <div className="flex h-[calc(100vh-10rem)] min-h-0 flex-col overflow-hidden">
      <ScrollToTopButton containerRef={listScrollRef} />

      <div className="mb-4 flex shrink-0 flex-col gap-3 md:flex-row md:items-center">
        <div className="w-full md:max-w-md">
          <CustomInput
            placeholder="Search by service, therapist, or location..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
            className="min-h-10 rounded-full pb-0 pt-1.75"
          />
        </div>
        <div className="w-full md:max-w-56">
          <CustomSelect
            value={statusFilter}
            onChange={setStatusFilter}
            options={BOOKED_SESSION_STATUS_OPTIONS.map((option) => ({
              value: option.value,
              label: option.label,
            }))}
            className="h-10 min-h-10 max-h-10 rounded-full border border-(--neutral-100) bg-white pb-0 pt-0 shadow-xs"
            isSearch={false}
          />
        </div>
      </div>

      <div
        ref={listScrollRef}
        className="min-h-0 flex-1 overflow-y-auto pr-1 custom-scrollbar"
      >
        {isInitialLoading ? (
          <ContentLoader size="md" className="gap-2 py-16 text-sm text-(--text-neutral-600)" />
        ) : filteredSessions.length === 0 ? (
          <div className="rounded-xl border border-(--neutral-100) bg-white px-5 py-12 text-center text-sm text-(--text-neutral-600)">
            No booked sessions found.
          </div>
        ) : (
          <div className="space-y-3">
            {filteredSessions.map((session) => (
              <BookedSessionCard
                key={session.id}
                session={session}
                onOpen={handleOpenSession}
                onRate={handleOpenRateModal}
              />
            ))}
            {hasMore ? (
              <div
                ref={observerTarget}
                className="flex items-center justify-center py-4 text-sm text-(--text-neutral-600)"
              >
                {isFetchingMore ? (
                  <ContentLoader variant="inline" size="md" />
                ) : (
                  <span className="sr-only">Load more sessions</span>
                )}
              </div>
            ) : null}
          </div>
        )}
      </div>

      <RateSessionModal
        isOpen={isRateModalOpen}
        session={ratingSession}
        isSubmitting={isRating}
        onClose={() => {
          if (isRating) return;
          setIsRateModalOpen(false);
          setRatingSession(null);
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

export default BookedSessions;
