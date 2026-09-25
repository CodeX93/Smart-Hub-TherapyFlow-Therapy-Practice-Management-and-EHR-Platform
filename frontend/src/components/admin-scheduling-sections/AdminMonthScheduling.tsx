import OverviewCard from "../shared/OverviewCard";
import {
  SCHEDULING_STATIC_CONTENT,
  SCHEDULING_FILTERS,
} from "../../pages/therapist/therapist.static";

import type { Appointment } from "../../types/scheduling";
import {
  constructDate,
  formatDateFull,
  formatMonthOnly,
} from "../../utils/transformer/dates.transformer";
import { useEffect, useMemo, useState } from "react";
import type { SchedulingFilters } from "../scheduling-sections/SchedulingFilterDropdown";
import SchedulingControls from "../scheduling-sections/SchedulingControls";
import MonthlyTimeline from "../scheduling-sections/monthly-timeline";
import {
  useGetSessionOverviewStatsQuery,
  useUpdateSessionStatusMutation,
} from "@/store/api/admin/dashboard.api";
import { useLazyGetAdminUsersQuery } from "@/store/api/admin/users.api";
import type { CustomSelectOption } from "../form/CustomSelect";
import {
  mapSessionToAppointment,
} from "./sessionAppointments";
import type { AdminSessionsListParams } from "@/store/api/admin/dashboard.api";
import {
  buildOverviewStatsArgs,
  getOverviewCardValue,
} from "../scheduling-sections/sessionOverviewStats";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "../shared/Toast";
import { useSessionCompletedPayNow } from "@/hooks/useSessionCompletedPayNow";
import {
  CALENDAR_SESSIONS_PAGE_SIZE,
  useCalendarSessions,
} from "@/hooks/useCalendarSessions";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import {
  toIsoRangeEndInTimezone,
  toIsoRangeStartInTimezone,
} from "@/utils/scheduleTimezone";

interface MonthSchedulingProps {
  handleEditScheduleClick?: (appointment: Appointment) => void;
  staffMode?: boolean;
  hideEditActions?: boolean;
}

const AdminMonthScheduling = ({
  handleEditScheduleClick,
  staffMode = false,
  hideEditActions,
}: MonthSchedulingProps) => {
  const resolvedHideEditActions = hideEditActions ?? staffMode;
  const { monthOverview } = SCHEDULING_STATIC_CONTENT;
  const [searchQuery, setSearchQuery] = useState("");
  const [appliedSearchQuery, setAppliedSearchQuery] = useState("");
  const [filters, setFilters] = useState<SchedulingFilters>({
    startDate: null,
    endDate: null,
    status: null,
    serviceCode: null,
    therapist: null,
    includeHiddenServices: false,
  });
  const [mySessionsOnly, setMySessionsOnly] = useState(false);
  const [therapistOptions, setTherapistOptions] = useState<CustomSelectOption[]>([
    { value: "", label: "All Therapists" },
  ]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [triggerGetUsers] = useLazyGetAdminUsersQuery();
  const [updateSessionStatus, { isLoading: isStatusUpdating }] = useUpdateSessionStatusMutation();
  const { maybeOpenAfterStatusChange, payNowModal } = useSessionCompletedPayNow({
    onToast: (message, type = "info") => {
      setToastType(type);
      setToastMessage(message);
    },
  });
  const { data: practiceConfig, isSuccess: hasPracticeConfig } = useGetPracticeConfigurationQuery();
  const scheduleTimezone = practiceConfig?.timezone;
  const today = new Date();
  const [currentMonth, setCurrentMonth] = useState(
    constructDate(today.getFullYear(), today.getMonth(), 1),
  );

  const handleToday = () => {
    const now = new Date();
    setCurrentMonth(constructDate(now.getFullYear(), now.getMonth(), 1));
  };

  const handlePrevMonth = () => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1),
    );
  };

  const handleNextMonth = () => {
    setCurrentMonth(
      new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1),
    );
  };

  const formattedDate =
    formatMonthOnly(currentMonth) + " " + currentMonth.getFullYear();

  const dynamicFullDate = formatDateFull(today);
  const dynamicMonth = formatMonthOnly(currentMonth);

  const { statusOptions, serviceCodeOptions } = SCHEDULING_FILTERS;

  const handleClearFilters = (f: SchedulingFilters) => {
    setFilters(f);
  };

  const monthStart = constructDate(
    currentMonth.getFullYear(),
    currentMonth.getMonth(),
    1,
  );
  const monthEnd = constructDate(
    currentMonth.getFullYear(),
    currentMonth.getMonth() + 1,
    0,
  );

  const sessionsQueryArgs = useMemo(() => {
    const params: AdminSessionsListParams = {
      pageSize: CALENDAR_SESSIONS_PAGE_SIZE,
      mySessionsOnly,
      includeHiddenServices: Boolean(filters.includeHiddenServices),
      startDate: toIsoRangeStartInTimezone(monthStart, scheduleTimezone),
      endDate: toIsoRangeEndInTimezone(monthEnd, scheduleTimezone),
      view: "calendar",
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

    return params;
  }, [
    appliedSearchQuery,
    filters.includeHiddenServices,
    filters.serviceCode,
    filters.status,
    filters.therapist,
    monthEnd,
    monthStart,
    mySessionsOnly,
    scheduleTimezone,
  ]);

  const {
    items: sessionItems,
    totalCount: sessionsTotalCount,
    isFetching: isSessionsFetching,
    refetch,
  } = useCalendarSessions(sessionsQueryArgs, { skip: !hasPracticeConfig });
  const { data: overviewStats } = useGetSessionOverviewStatsQuery(
    buildOverviewStatsArgs(monthStart, monthEnd, filters, scheduleTimezone),
    { skip: !hasPracticeConfig },
  );

  const appointments = useMemo(
    () =>
      sessionItems.map((session) =>
        mapSessionToAppointment(session, scheduleTimezone),
      ),
    [scheduleTimezone, sessionItems],
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

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setAppliedSearchQuery(searchQuery);
    }, 300);

    return () => window.clearTimeout(timeout);
  }, [searchQuery]);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const handleStatusChange = (
    id: string,
    statusKey: string,
  ) => {
    const apiStatus = statusKey;
    setToastType("info");
    setToastMessage("Updating session status...");

    void updateSessionStatus({
      id: Number.parseInt(id, 10),
      body: { status: apiStatus },
    })
      .unwrap()
      .then(() => {
        setToastType("success");
        setToastMessage("Session status updated successfully.");
        void refetch();
        maybeOpenAfterStatusChange(Number.parseInt(id, 10), apiStatus);
      })
      .catch((error) => {
        setToastType("error");
        setToastMessage(getApiErrorMessage(error));
      });
  };

  return (
    <div className="mt-4 flex flex-col h-full">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
          duration={2500}
        />
      ) : null}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-2 md:gap-4">
        {monthOverview.map((card, index) => (
          <OverviewCard
            key={index}
            label={card.label}
            value={getOverviewCardValue(
              card.label,
              overviewStats,
              sessionsTotalCount,
            )}
            labelFirst
            isText={
              card.label === "Today"
                ? dynamicFullDate
                : card.label === "This Month"
                  ? dynamicMonth
                  : card.isText
            }
          />
        ))}
      </div>
      <SchedulingControls
        currentDateLabel={formattedDate}
        onToday={handleToday}
        onPrev={handlePrevMonth}
        onNext={handleNextMonth}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        filters={filters}
        setFilters={setFilters}
        statusOptions={statusOptions}
        serviceCodeOptions={serviceCodeOptions}
        onClearFilters={handleClearFilters}
        isAdmin={true}
        mySessionsOnly={mySessionsOnly}
        setMySessionsOnly={setMySessionsOnly}
        therapistOptions={therapistOptions}
      />

      <MonthlyTimeline
        currentMonth={currentMonth}
        appointments={appointments}
        handleEditScheduleClick={handleEditScheduleClick}
        hideEditActions={resolvedHideEditActions}
        showTherapistName
        onStatusChange={handleStatusChange}
        isStatusUpdating={isStatusUpdating}
        isLoading={isSessionsFetching && appointments.length === 0}
      />
      {payNowModal}
    </div>
  );
};

export default AdminMonthScheduling;
