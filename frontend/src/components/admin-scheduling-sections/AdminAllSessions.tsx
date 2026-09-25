import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";
import OverviewCard from "../shared/OverviewCard";
import {
  SCHEDULING_STATIC_CONTENT,
  SCHEDULING_FILTERS,
} from "../../pages/therapist/therapist.static";
import { useEffect, useMemo, useState, useRef } from "react";
import type { Appointment } from "@/types/scheduling";
import type { SchedulingFilters } from "../scheduling-sections/SchedulingFilterDropdown";
import SchedulingControls from "../scheduling-sections/SchedulingControls";
import AllSessionsTable from "../scheduling-sections/AllSessionsTable";
import ScrollToTopButton from "../shared/ScrollToTopButton";
import {
  useGetAdminSessionsListQuery,
  useGetSessionOverviewStatsQuery,
  useUpdateSessionStatusMutation,
} from "@/store/api/admin/dashboard.api";
import { useLazyGetAdminUsersQuery } from "@/store/api/admin/users.api";
import type { CustomSelectOption } from "../form/CustomSelect";
import { buildOverviewStatsArgs, getOverviewCardValue } from "../scheduling-sections/sessionOverviewStats";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "../shared/Toast";
import CreateSessionBillingModal from "@/components/billing-sections/CreateSessionBillingModal";
import { isBillableSessionStatus } from "@/utils/sessionBillingUi";
import { useLazyGetSessionBillingQuery } from "@/store/api/admin/billing.api";
import { useSessionCompletedPayNow } from "@/hooks/useSessionCompletedPayNow";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";

interface AllSessionsProps {
  handleEditScheduleClick?: (appointment: Appointment) => void;
  staffMode?: boolean;
  hideEditActions?: boolean;
  initialFilters?: Partial<SchedulingFilters>;
}

const AdminAllSessions = ({
  handleEditScheduleClick,
  staffMode = false,
  hideEditActions,
  initialFilters,
}: AllSessionsProps) => {
  const resolvedHideEditActions = hideEditActions ?? staffMode;
  const { allSessions: allSessionsOverview } = SCHEDULING_STATIC_CONTENT;
  const { statusOptions, serviceCodeOptions } =
    SCHEDULING_FILTERS;
  const [searchQuery, setSearchQuery] = useState("");
  const [mySessionsOnly, setMySessionsOnly] = useState(false);
  const [appliedSearchQuery, setAppliedSearchQuery] = useState("");


  const [updatingSessionId, setUpdatingSessionId] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [createBillingSessionId, setCreateBillingSessionId] = useState<number | null>(null);
  const [billedSessionIds, setBilledSessionIds] = useState<ReadonlySet<number>>(
    () => new Set(),
  );
  const billedSessionIdsRef = useRef<Set<number>>(new Set());
  const checkingBillingSessionIdsRef = useRef<Set<number>>(new Set());
  const [therapistOptions, setTherapistOptions] = useState<CustomSelectOption[]>([
    { value: "", label: "All Therapists" },
  ]);
  const [triggerGetUsers] = useLazyGetAdminUsersQuery();
  const [updateSessionStatus, { isLoading: isStatusUpdating }] = useUpdateSessionStatusMutation();
  const [triggerGetSessionBilling] = useLazyGetSessionBillingQuery();
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const scheduleTimezone = practiceConfig?.timezone;
  const { maybeOpenAfterStatusChange, payNowModal } = useSessionCompletedPayNow({
    onToast: (message, type = "info") => {
      setToastType(type);
      setToastMessage(message);
    },
  });

  const [filters, setFilters] = useState<SchedulingFilters>({
    startDate: initialFilters?.startDate ?? null,
    endDate: initialFilters?.endDate ?? null,
    status: initialFilters?.status ?? null,
    serviceCode: initialFilters?.serviceCode ?? null,
    includeHiddenServices: initialFilters?.includeHiddenServices ?? false,
    therapist: initialFilters?.therapist ?? null,
  });

  const areFiltersEqual = (left: SchedulingFilters, right: SchedulingFilters) =>
    (left.startDate?.getTime() ?? null) === (right.startDate?.getTime() ?? null) &&
    (left.endDate?.getTime() ?? null) === (right.endDate?.getTime() ?? null) &&
    (left.status ?? null) === (right.status ?? null) &&
    (left.serviceCode ?? null) === (right.serviceCode ?? null) &&
    (left.therapist ?? null) === (right.therapist ?? null) &&
    Boolean(left.includeHiddenServices) === Boolean(right.includeHiddenServices);

  const listScope = JSON.stringify([filters, appliedSearchQuery, mySessionsOnly]);
  const [page, setPage] = useScopedPage(listScope);
  const sessionsQueryArgs = useMemo(() => {
    const params: Parameters<typeof useGetAdminSessionsListQuery>[0] = {
      page,
      pageSize: 25,
      mySessionsOnly,
      includeHiddenServices: Boolean(filters.includeHiddenServices),
    };

    if (appliedSearchQuery.trim()) params.clientSearch = appliedSearchQuery.trim();
    if (filters.status) {
      params.status = filters.status === "noshow" ? "no-show" : filters.status;
    }
    if (filters.serviceCode) params.serviceCode = filters.serviceCode;
    if (filters.therapist) {
      const therapistId = Number.parseInt(filters.therapist, 10);
      if (Number.isFinite(therapistId)) params.therapistId = therapistId;
    }
    if (filters.startDate) {
      const start = new Date(filters.startDate);
      start.setHours(0, 0, 0, 0);
      params.startDate = start.toISOString();
    }
    if (filters.endDate) {
      const end = new Date(filters.endDate);
      end.setHours(23, 59, 59, 999);
      params.endDate = end.toISOString();
    }

    return params;
  }, [appliedSearchQuery, filters, mySessionsOnly, page]);


  const {
    currentData: sessionsResponse,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useGetAdminSessionsListQuery(sessionsQueryArgs, { refetchOnMountOrArgChange: true });
  const { data: overviewStats } = useGetSessionOverviewStatsQuery(
    buildOverviewStatsArgs(filters.startDate, filters.endDate, filters, scheduleTimezone),
  );

  useEffect(() => {
    void triggerGetUsers({
      page: 1,
      pageSize: 25,
      role: "THERAPIST",
    })
      .unwrap()
      .then((response) => {
        setTherapistOptions([
          { value: "", label: "All Therapists" },
          ...response.items.map((user) => ({
            value: String(user.id),
            label: user.fullName?.trim() || user.email || user.username,
          })),
        ]);
      })
      .catch(() => {
        setTherapistOptions([{ value: "", label: "All Therapists" }]);
      });
  }, [triggerGetUsers]);

  const effectiveSessionsResponse = sessionsResponse;

  const { items: allSessions, updateItems: setAllSessions, isReadyToLoadMore: isFirstPageReady } = usePagedItems(effectiveSessionsResponse?.items, page, listScope);

  useEffect(() => {
    let changed = false;

    allSessions.forEach((session) => {
      if (session.billingId != null && !billedSessionIdsRef.current.has(session.id)) {
        billedSessionIdsRef.current.add(session.id);
        changed = true;
      }
    });

    if (changed) {
      setBilledSessionIds(new Set(billedSessionIdsRef.current));
    }
  }, [allSessions]);

  useEffect(() => {
    let cancelled = false;

    const sessionsToCheck = allSessions.filter(
      (session) =>
        isBillableSessionStatus(session.status ?? "") &&
        session.billingId == null &&
        !billedSessionIdsRef.current.has(session.id) &&
        !checkingBillingSessionIdsRef.current.has(session.id),
    );

    if (!sessionsToCheck.length) return;

    sessionsToCheck.forEach((session) => {
      checkingBillingSessionIdsRef.current.add(session.id);
      void triggerGetSessionBilling(session.id)
        .unwrap()
        .then(() => {
          if (cancelled) return;
          billedSessionIdsRef.current.add(session.id);
          setBilledSessionIds(new Set(billedSessionIdsRef.current));
        })
        .catch(() => {})
        .finally(() => {
          checkingBillingSessionIdsRef.current.delete(session.id);
        });
    });

    return () => {
      cancelled = true;
    };
  }, [allSessions, triggerGetSessionBilling]);

  useEffect(() => {
    if (searchQuery === appliedSearchQuery) return;

    const timeout = window.setTimeout(() => {
      setPage(1);
      setAppliedSearchQuery(searchQuery);
    }, 300);

    return () => window.clearTimeout(timeout);
  }, [searchQuery, appliedSearchQuery, setPage]);




  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const [reportedError, setReportedError] = useState<unknown>(null);
  if (error && error !== reportedError) {
    setReportedError(error);
    setToastType("error");
    setToastMessage(getApiErrorMessage(error));
  }

  const handleApplyFilters = (f: SchedulingFilters) => {
    if (areFiltersEqual(filters, f)) return;
    setFilters(f);
    setPage(1);
  };

  const handleClearFilters = (f: SchedulingFilters) => {
    setFilters(f);
    setPage(1);
  };

  const handleStatusChange = (id: string, statusKey: string) => {
    const apiStatus = statusKey;
    setToastType("info");
    setToastMessage("Updating session status...");
    setUpdatingSessionId(id);

    void updateSessionStatus({
      id: Number.parseInt(id, 10),
      body: { status: apiStatus },
    })
      .unwrap()
      .then((updatedSession) => {
        setToastType("success");
        setToastMessage("Session status updated successfully.");
        const sessionId = updatedSession.id;
        const activeStatusFilter = filters.status
          ? filters.status === "noshow"
            ? "no-show"
            : filters.status
          : null;
        const updatedStatusKey = (updatedSession.status || apiStatus).toLowerCase();

        setAllSessions((previous) => {
          if (
            activeStatusFilter &&
            updatedStatusKey !== activeStatusFilter.toLowerCase()
          ) {
            return previous.filter((session) => session.id !== sessionId);
          }

          const hasSession = previous.some((session) => session.id === sessionId);
          if (!hasSession) return previous;

          return previous.map((session) =>
            session.id === sessionId ? { ...session, ...updatedSession } : session,
          );
        });
        void refetch();
        maybeOpenAfterStatusChange(Number.parseInt(id, 10), apiStatus);
      })
      .catch((error) => {
        setToastType("error");
        setToastMessage(getApiErrorMessage(error));
      })
      .finally(() => {
        setUpdatingSessionId(null);
      });
  };

  return (
    <div className="mt-4 flex h-full min-h-0 flex-col overflow-hidden">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
          duration={2500}
        />
      ) : null}
      <ScrollToTopButton />
      <div className="grid shrink-0 grid-cols-2 md:grid-cols-3 gap-2 md:gap-4">
        {allSessionsOverview.map((card, index) => (
          <OverviewCard
            key={index}
            label={card.label}
            value={getOverviewCardValue(
              card.label,
              overviewStats,
              effectiveSessionsResponse?.totalCount,
            )}
            labelFirst
          />
        ))}
      </div>
      <div className="shrink-0">
        <SchedulingControls
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        filters={filters}
        setFilters={setFilters}
        statusOptions={statusOptions}
        serviceCodeOptions={serviceCodeOptions}
        onApplyFilters={handleApplyFilters}
        onClearFilters={handleClearFilters}
        isSession={true}
        isAdmin={true}
        mySessionsOnly={mySessionsOnly}
        setMySessionsOnly={setMySessionsOnly}
        therapistOptions={therapistOptions}
        />
      </div>

      <div className="mt-4 flex min-h-0 flex-1 flex-col overflow-hidden">
      <AllSessionsTable
        handleEditScheduleClick={handleEditScheduleClick}
        hideEditActions={resolvedHideEditActions}
        onStatusChange={handleStatusChange}
        isStatusUpdating={isStatusUpdating}
        updatingSessionId={updatingSessionId}
        isAdmin={true}
        sessions={allSessions}
        hasMore={
          isFirstPageReady &&
          !isFetching &&
          (effectiveSessionsResponse?.page ?? page) === page &&
          page < (effectiveSessionsResponse?.totalPages ?? 0)
        }
        isLoading={isLoading || (isFetching && page === 1)}
        isLoadingMore={isFetching && page > 1}
        onLoadMore={() => {
          if (
            isFirstPageReady &&
            !isLoading &&
            !isFetching &&
            (effectiveSessionsResponse?.page ?? page) === page &&
            page < (effectiveSessionsResponse?.totalPages ?? 0)
          ) {
            setPage((previous) => previous + 1);
          }
        }}
        onCreateInvoice={(sessionId) => setCreateBillingSessionId(sessionId)}
        billedSessionIds={billedSessionIds}
      />
      </div>

      {createBillingSessionId ? (
        <CreateSessionBillingModal
          isOpen={Boolean(createBillingSessionId)}
          onClose={() => setCreateBillingSessionId(null)}
          sessionId={createBillingSessionId}
          onCreated={() => {
            if (createBillingSessionId) {
              billedSessionIdsRef.current.add(createBillingSessionId);
              setBilledSessionIds(new Set(billedSessionIdsRef.current));
            }
            setToastType("success");
            setToastMessage("Invoice created. View it in Billings.");
            setCreateBillingSessionId(null);
          }}
        />
      ) : null}
      {payNowModal}
    </div>
  );
};

export default AdminAllSessions;
