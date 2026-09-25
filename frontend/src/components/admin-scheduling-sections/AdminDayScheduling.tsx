import OverviewCard from "../shared/OverviewCard";
import {
  SCHEDULING_STATIC_CONTENT,
  SCHEDULING_FILTERS,
} from "../../pages/therapist/therapist.static";
import { useEffect, useMemo, useState } from "react";
import Calendar from "../appointment-sections/Calendar";
import type { Appointment } from "../../types/scheduling";
import {
  formatDateHeader,
  getRelativeDay,
  constructDate,
  formatDateFull,
  formatMonthOnly,
} from "../../utils/transformer/dates.transformer";
import type { SchedulingFilters } from "../scheduling-sections/SchedulingFilterDropdown";
import SchedulingControls from "../scheduling-sections/SchedulingControls";
import DailyTimeline from "../scheduling-sections/daily-timeline";
import {
  useGetSessionOverviewStatsQuery,
  useUpdateSessionStatusMutation,
} from "@/store/api/admin/dashboard.api";
import { useLazyGetAdminUsersQuery } from "@/store/api/admin/users.api";
import type { CustomSelectOption } from "../form/CustomSelect";
import {
  buildSessionsQueryArgs,
  mapSessionToAppointment,
} from "./sessionAppointments";
import {
  buildOverviewStatsArgs,
  getOverviewCardValue,
} from "../scheduling-sections/sessionOverviewStats";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "../shared/Toast";
import { mapBackendSessionStatusToAppointment } from "@/utils/sessionStatusTransitions";
import { useSessionCompletedPayNow } from "@/hooks/useSessionCompletedPayNow";
import {
  CALENDAR_SESSIONS_PAGE_SIZE,
  useCalendarSessions,
} from "@/hooks/useCalendarSessions";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import { DateTime } from "luxon";
import { resolveScheduleTimezone } from "@/utils/scheduleTimezone";

interface DaySchedulingProps {
  handleEditScheduleClick?: (appointment: Appointment) => void;
  staffMode?: boolean;
  hideEditActions?: boolean;
}

const AdminDayScheduling = ({
  handleEditScheduleClick,
  staffMode = false,
  hideEditActions,
}: DaySchedulingProps) => {
  const resolvedHideEditActions = hideEditActions ?? staffMode;
  const { statusOptions, serviceCodeOptions } = SCHEDULING_FILTERS;
  const { dayOverview } = SCHEDULING_STATIC_CONTENT;
  const [searchQuery, setSearchQuery] = useState("");
  const [mySessionsOnly, setMySessionsOnly] = useState(false);
  const [appliedSearchQuery, setAppliedSearchQuery] = useState("");
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
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const scheduleTimezone = practiceConfig?.timezone;

  const today = new Date();

  const [currentMonth, setCurrentMonth] = useState(
    constructDate(today.getFullYear(), today.getMonth(), 1),
  );
  const [selectedDate, setSelectedDate] = useState<number>(today.getDate());
  const [selectedAppointment, setSelectedAppointment] =
    useState<Appointment | null>(null);
  const [filters, setFilters] = useState<SchedulingFilters>({
    startDate: null,
    endDate: null,
    status: null,
    serviceCode: null,
    therapist: null,
    includeHiddenServices: false,
  });

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
      selectedDate,
    ),
  );

  const selectedFullDate = constructDate(
    currentMonth.getFullYear(),
    currentMonth.getMonth(),
    selectedDate,
  );

  const rangeStart = selectedFullDate;
  const rangeEnd = selectedFullDate;

  const calendarQueryArgs = useMemo(
    () =>
      buildSessionsQueryArgs({
        pageSize: CALENDAR_SESSIONS_PAGE_SIZE,
        rangeStart: constructDate(
          currentMonth.getFullYear(),
          currentMonth.getMonth(),
          1,
        ),
        rangeEnd: constructDate(
          currentMonth.getFullYear(),
          currentMonth.getMonth() + 1,
          0,
        ),
        filters: { ...filters, startDate: null, endDate: null },
        searchQuery: appliedSearchQuery,
        mySessionsOnly,
        timezone: scheduleTimezone,
      }),
    [appliedSearchQuery, currentMonth, filters, mySessionsOnly, scheduleTimezone],
  );

  const sessionsQueryArgs = useMemo(
    () =>
      buildSessionsQueryArgs({
        pageSize: CALENDAR_SESSIONS_PAGE_SIZE,
        rangeStart,
        rangeEnd,
        filters,
        searchQuery: appliedSearchQuery,
        mySessionsOnly,
        timezone: scheduleTimezone,
      }),
    [appliedSearchQuery, filters, mySessionsOnly, rangeEnd, rangeStart, scheduleTimezone],
  );

  const {
    items: sessionItems,
    totalCount: sessionsTotalCount,
    refetch,
  } = useCalendarSessions(sessionsQueryArgs);
  const { items: calendarSessionItems } = useCalendarSessions(calendarQueryArgs);
  const { data: overviewStats } = useGetSessionOverviewStatsQuery(
    buildOverviewStatsArgs(rangeStart, rangeEnd, filters, scheduleTimezone),
  );
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
  const { data: monthOverviewStats } = useGetSessionOverviewStatsQuery(
    buildOverviewStatsArgs(
      monthStart,
      monthEnd,
      { ...filters, startDate: null, endDate: null },
      scheduleTimezone,
    ),
  );

  const appointments = useMemo(
    () =>
      sessionItems.map((session) =>
        mapSessionToAppointment(session, scheduleTimezone),
      ),
    [scheduleTimezone, sessionItems],
  );

  const markedDates = useMemo(() => {
    const zone = resolveScheduleTimezone(scheduleTimezone);
    return Array.from(
      new Set(
        calendarSessionItems.flatMap((session) => {
          if (!session.sessionDate) return [];
          const local = DateTime.fromISO(session.sessionDate, { zone: "utc" }).setZone(
            zone,
          );
          if (
            !local.isValid ||
            local.year !== currentMonth.getFullYear() ||
            local.month !== currentMonth.getMonth() + 1
          ) {
            return [];
          }
          return [local.day];
        }),
      ),
    );
  }, [calendarSessionItems, currentMonth, scheduleTimezone]);

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

  const handleApplyFilters = (f: SchedulingFilters) => {
    setFilters(f);
  };

  const handleClearFilters = (f: SchedulingFilters) => {
    setFilters(f);
  };

  const dynamicFullDate = formatDateFull(selectedFullDate);
  const dynamicMonth = formatMonthOnly(currentMonth);

  const handleStatusChange = (id: string, statusKey: string) => {
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
        maybeOpenAfterStatusChange(Number.parseInt(id, 10), apiStatus);
      })
      .catch((error) => {
        setToastType("error");
        setToastMessage(getApiErrorMessage(error));
      });
  };

  return (
    <div className="flex w-full flex-col gap-4">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
          duration={2500}
        />
      ) : null}
      <div className="grid shrink-0 grid-cols-2 gap-2 md:grid-cols-4 md:gap-4">
        {dayOverview.map((card, index) => (
          <OverviewCard
            key={index}
            label={card.label}
            value={getOverviewCardValue(
              card.label,
              card.label === "This Month" ? monthOverviewStats : overviewStats,
              card.label === "Today"
                ? sessionsTotalCount
                : card.label === "This Month"
                  ? monthOverviewStats?.totalSessions
                  : undefined,
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
      <div className="shrink-0">
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
          therapistOptions={therapistOptions}
          onApplyFilters={handleApplyFilters}
          onClearFilters={handleClearFilters}
          isAdmin={true}
          mySessionsOnly={mySessionsOnly}
          setMySessionsOnly={setMySessionsOnly}
        />
      </div>

      {/* Two-column layout for Calendar and Timeline */}
      <div className="flex flex-col gap-4 rounded-[1.5rem] border border-(--neutral-100) bg-white px-2.5 py-4 md:flex-row md:items-start md:gap-6 md:p-4">
        {/* Left Sidebar: Calendar */}
        <div className="mx-auto w-full max-w-80 shrink-0 md:mx-0">
          <Calendar
            currentMonth={currentMonth}
            setCurrentMonth={setCurrentMonth}
            selectedDate={selectedDate}
            setSelectedDate={setSelectedDate}
            isDropdowns={false}
            markedDates={markedDates}
          />
        </div>

        {/* Right Content: Timeline */}
        <div className="min-w-0 flex-1">
          <DailyTimeline
            date={selectedDate}
            month={currentMonth}
            appointments={appointments}
            selectedAppointment={selectedAppointment}
            onAppointmentClick={setSelectedAppointment}
            onCloseModal={() => setSelectedAppointment(null)}
            onStatusChange={handleStatusChange}
            isStatusUpdating={isStatusUpdating}
            handleEditScheduleClick={handleEditScheduleClick}
            hideEditActions={resolvedHideEditActions}
            showTherapistName
            timezone={scheduleTimezone}
          />
        </div>
      </div>
      {payNowModal}
    </div>
  );
};

export default AdminDayScheduling;
