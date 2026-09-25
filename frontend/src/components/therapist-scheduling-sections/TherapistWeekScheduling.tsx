import OverviewCard from "../shared/OverviewCard";
import {
  SCHEDULING_STATIC_CONTENT,
  SCHEDULING_FILTERS,
} from "../../pages/therapist/therapist.static";
import { useEffect, useMemo, useState } from "react";
import WeeklyTimeline from "../scheduling-sections/weekly-timeline/index";
import type { Appointment } from "../../types/scheduling";
import {
  formatDateHeader,
  getRelativeDay,
  constructDate,
  formatWeekRange,
  formatMonthOnly,
} from "../../utils/transformer/dates.transformer";
import SchedulingControls from "../scheduling-sections/SchedulingControls";
import { type SchedulingFilters } from "../scheduling-sections/SchedulingFilterDropdown";
import {
  useGetSessionOverviewStatsQuery,
  useUpdateSessionStatusMutation,
} from "@/store/api/admin/dashboard.api";
import {
  buildSessionsQueryArgs,
  mapSessionToAppointment,
} from "../admin-scheduling-sections/sessionAppointments";
import {
  buildOverviewStatsArgs,
  getOverviewCardValue,
} from "../scheduling-sections/sessionOverviewStats";
import { getApiErrorMessage } from "@/utils/apiError";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import Toast from "../shared/Toast";
import { mapBackendSessionStatusToAppointment } from "@/utils/sessionStatusTransitions";
import {
  CALENDAR_SESSIONS_PAGE_SIZE,
  useCalendarSessions,
} from "@/hooks/useCalendarSessions";

interface WeekSchedulingProps {
  handleEditScheduleClick: (appointment: Appointment) => void;
}

const TherapistWeekScheduling = ({ handleEditScheduleClick }: WeekSchedulingProps) => {
  const { weekOverview } = SCHEDULING_STATIC_CONTENT;
  const [searchQuery, setSearchQuery] = useState("");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [updateSessionStatus, { isLoading: isStatusUpdating }] = useUpdateSessionStatusMutation();
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const scheduleTimezone = practiceConfig?.timezone;

  const today = new Date();
  const [currentMonth, setCurrentMonth] = useState(
    constructDate(today.getFullYear(), today.getMonth(), 1)
  );
  const [selectedDate, setSelectedDate] = useState<number>(today.getDate());

  const handleToday = () => {
    const now = new Date();
    setCurrentMonth(constructDate(now.getFullYear(), now.getMonth(), 1));
    setSelectedDate(now.getDate());
  };

  const handlePrevDay = () => {
    const { month, day } = getRelativeDay(currentMonth, selectedDate, -1);
    setCurrentMonth(month);
    setSelectedDate(day);
  };

  const handleNextDay = () => {
    const { month, day } = getRelativeDay(currentMonth, selectedDate, 1);
    setCurrentMonth(month);
    setSelectedDate(day);
  };

  const formattedDate = formatDateHeader(
    constructDate(
      currentMonth.getFullYear(),
      currentMonth.getMonth(),
      selectedDate
    )
  );

  const [filters, setFilters] = useState<SchedulingFilters>({
    startDate: null,
    endDate: null,
    status: null,
    serviceCode: null,
    includeHiddenServices: false,
  });
  const [appliedSearchQuery, setAppliedSearchQuery] = useState("");

  const { statusOptions, serviceCodeOptions } = SCHEDULING_FILTERS;

  const handleApplyFilters = (f: SchedulingFilters) => {
    setFilters(f);
  };

  const handleClearFilters = (f: SchedulingFilters) => {
    setFilters(f);
  };

  const selectedFullDate = constructDate(
    currentMonth.getFullYear(),
    currentMonth.getMonth(),
    selectedDate
  );

  const dynamicMonth = formatMonthOnly(currentMonth);

  const [selectedAppointment, setSelectedAppointment] =
    useState<Appointment | null>(null);

  // compute week start (Sunday) for the selectedFullDate
  const weekdayIndex = selectedFullDate.getDay();
  const weekStart = new Date(selectedFullDate);
  weekStart.setDate(selectedFullDate.getDate() - weekdayIndex);
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekStart.getDate() + 6);

  const dynamicWeekRange = formatWeekRange(weekStart, weekEnd);

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

  const sessionsQueryArgs = useMemo(
    () =>
      buildSessionsQueryArgs({
        pageSize: CALENDAR_SESSIONS_PAGE_SIZE,
        rangeStart: weekStart,
        rangeEnd: weekEnd,
        filters,
        searchQuery: appliedSearchQuery,
        mySessionsOnly: false,
        timezone: scheduleTimezone,
      }),
    // weekStart/weekEnd are derived from selectedFullDate each render; key on YMD.
    // eslint-disable-next-line react-hooks/exhaustive-deps -- intentional calendar-day deps
    [
      appliedSearchQuery,
      filters,
      scheduleTimezone,
      selectedFullDate.getFullYear(),
      selectedFullDate.getMonth(),
      selectedFullDate.getDate(),
    ],
  );
  const {
    items: sessionItems,
    totalCount: sessionsTotalCount,
    refetch,
  } = useCalendarSessions(sessionsQueryArgs);
  const { data: overviewStats } = useGetSessionOverviewStatsQuery(
    buildOverviewStatsArgs(weekStart, weekEnd, filters, scheduleTimezone),
  );
  const { data: monthOverviewStats } = useGetSessionOverviewStatsQuery(
    buildOverviewStatsArgs(
      monthStart,
      monthEnd,
      { ...filters, startDate: null, endDate: null },
      scheduleTimezone,
    ),
  );

  const appointments = useMemo(
    () => sessionItems.map((session) => mapSessionToAppointment(session, scheduleTimezone)),
    [scheduleTimezone, sessionItems],
  );

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

  const handleAppointmentClick = (apt: Appointment) => {
    setSelectedAppointment(apt);
  };

  const handleStatusMutation = (id: string, statusKey: string) => {
    const apiStatus = statusKey;
    setToastType("info");
    setToastMessage("Updating session status...");

    void updateSessionStatus({
      id: Number.parseInt(id, 10),
      body: { status: apiStatus },
    })
      .unwrap()
      .then(() => {
        if (selectedAppointment && selectedAppointment.id === id) {
          setSelectedAppointment({
            ...selectedAppointment,
            status: mapBackendSessionStatusToAppointment(statusKey),
          });
        }
        setToastType("success");
        setToastMessage("Session status updated successfully.");
        void refetch();
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
      <div className="grid grid-cols-2 md:grid-cols-4 md:gap-4 gap-2">
        {weekOverview.map((card, index) => (
          <OverviewCard
            key={index}
            label={card.label}
            value={getOverviewCardValue(
              card.label,
              card.label === "This Month" ? monthOverviewStats : overviewStats,
              card.label === "This Week"
                ? sessionsTotalCount
                : card.label === "This Month"
                  ? monthOverviewStats?.totalSessions
                  : undefined,
            )}
            labelFirst
            isText={
              card.label === "This Week"
                ? dynamicWeekRange
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
        onPrev={handlePrevDay}
        onNext={handleNextDay}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        filters={filters}
        setFilters={setFilters}
        statusOptions={statusOptions}
        serviceCodeOptions={serviceCodeOptions}
        onApplyFilters={handleApplyFilters}
        onClearFilters={handleClearFilters}
      />

      {/* Two-column layout for Calendar and Timeline */}
      <div className="flex gap-6 flex-1 bg-white rounded-tl-2xl rounded-tr-2xl border border-(--neutral-100)">
        <div className="flex-1 min-w-0 overflow-hidden flex flex-col">
          <WeeklyTimeline
            weekStart={weekStart}
            appointments={appointments}
            selectedFullDate={selectedFullDate}
            onDaySelect={(d) => {
              // Ensure parent updates selected date and month when user clicks a day in week header
              setCurrentMonth(constructDate(d.getFullYear(), d.getMonth(), 1));
              setSelectedDate(d.getDate());
            }}
            onAppointmentClick={handleAppointmentClick}
            selectedAppointment={selectedAppointment}
            onCloseModal={() => setSelectedAppointment(null)}
            onStatusChange={handleStatusMutation}
            isStatusUpdating={isStatusUpdating}
            handleEditScheduleClick={handleEditScheduleClick}
            timezone={scheduleTimezone}
          />
        </div>
      </div>
    </div>
  );
};

export default TherapistWeekScheduling;
