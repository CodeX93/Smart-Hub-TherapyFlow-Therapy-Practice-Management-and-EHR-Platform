import { ContentLoader } from "@/components/shared/ContentLoader";
import { CalendarIcon } from "@/components/icons/commonIcons";
import { useState, useCallback } from "react";
import { Plus } from "lucide-react";
import { Button } from "../../ui/button";
import { SessionCard, SummaryCard } from "../../sessions";
import FilterDropdown from "../../shared/FilterDropdown";
import { SCHEDULING_STATIC_CONTENT } from "../../../pages/therapist/therapist.static";
import type { Client } from "../../../types/client.type";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";

interface SessionsTabProps {
  client: Client;
  scrollRef: React.RefObject<HTMLDivElement | null>;
}

const SessionsTab = ({ client, scrollRef }: SessionsTabProps) => {
  // Get sessions from static data and filter by client name
  const allSessions = SCHEDULING_STATIC_CONTENT.allSessionsData;
  const clientSessions = allSessions;

  const itemsPerPage = 6;
  const [displayedItemsCount, setDisplayedItemsCount] = useState(itemsPerPage);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  const totalSessionsCount = clientSessions.length;

  const handleLoadMore = useCallback(() => {
    setIsLoadingMore(true);
    // Simulate brief loading delay
    setTimeout(() => {
      setDisplayedItemsCount((prev) =>
        Math.min(prev + itemsPerPage, totalSessionsCount),
      );
      setIsLoadingMore(false);
    }, 500);
  }, [totalSessionsCount]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: displayedItemsCount < totalSessionsCount,
    isLoading: isLoadingMore,
  });

  // Calculate summary statistics
  const totalSessions = clientSessions.length;
  const completedSessions = clientSessions.filter(
    (s) => s.status === "Completed",
  ).length;
  const scheduledSessions = clientSessions.filter(
    (s) => s.status === "Scheduled",
  ).length;
  const cancelledSessions = clientSessions.filter(
    (s) => s.status === "Cancelled",
  ).length;
  const conflictSessions = 0; // No conflict status in current data

  // Map sessions to match SessionCard interface
  const sessions = clientSessions
    .slice(0, displayedItemsCount)
    .map((session) => ({
      id: session.id,
      title: `${session.sessionType} (${session.serviceCode}) session 60 minutes`,
      status: session.status,
      type: "In person", // Default to in person, can be extended later
      room: `Room: ${session.room} - ${session.roomCode}`,
      dateTime: `${session.dateTime.split(",")[0]}, 2025 - ${session.dateTime.split(", ")[1]}`,
    }));

  return (
    <>
      <ScrollToTopButton containerRef={scrollRef} centered={false} />

      <div className="p-6 space-y-6">
        {/* Summary Cards */}
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          <SummaryCard
            label="Total Sessions"
            value={String(totalSessions).padStart(2, "0")}
          />
          <SummaryCard label="Completed" value={String(completedSessions)} />
          <SummaryCard
            label="Scheduled"
            value={String(scheduledSessions).padStart(2, "0")}
          />
          <SummaryCard
            label="Missed/Cancelled"
            value={String(cancelledSessions)}
          />
          <SummaryCard label="Conflicts" value={String(conflictSessions)} />
        </div>

        {/* Session History Header */}
        <div>
          <h3 className="text-base font-semibold text-gray-900 mb-4">
            Session History
          </h3>

          <div className="flex items-center justify-between mb-6">
            {/* Left: Filters */}
            <div className="flex gap-3">
              <FilterDropdown
                options={SCHEDULING_STATIC_CONTENT.sessionFilters.statusOptions}
              />
              <FilterDropdown
                options={SCHEDULING_STATIC_CONTENT.sessionFilters.notesOptions}
              />
            </div>

            {/* Right: Action Buttons */}
            <div className="flex gap-3">
              <Button
                variant="outline"
                className="h-10 px-4 rounded-full cursor-pointer text-sm font-normal flex items-center gap-2"
              >
                <CalendarIcon size={16} />
                View Calendar
              </Button>
              <Button className="h-10 px-4 font-semibold rounded-full cursor-pointer text-sm flex items-center gap-2">
                <Plus size={16} />
                Schedule Session
              </Button>
            </div>
          </div>
        </div>

        {/* Session Cards Grid */}
        <div className="grid grid-cols-1 items-stretch gap-4 lg:grid-cols-2">
          {sessions.length > 0 ? (
            sessions.map((session) => (
              <SessionCard
                key={session.id}
                session={session}
                clientName={client.name}
              />
            ))
          ) : (
            <div className="col-span-2 text-center py-8 text-gray-500">
              No sessions found for this client
            </div>
          )}
        </div>

        {/* Load More Observer */}
        <div
          ref={observerTarget}
          className="h-10 w-full flex items-center justify-center mt-4"
        >
          {isLoadingMore && (
            <ContentLoader variant="inline" size="md" />
          )}
        </div>
      </div>
    </>
  );
};

export default SessionsTab;
