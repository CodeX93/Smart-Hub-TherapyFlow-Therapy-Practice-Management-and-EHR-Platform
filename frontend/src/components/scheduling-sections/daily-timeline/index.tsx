import { useEffect, useRef } from "react";
import { DateTime } from "luxon";
import type { Appointment } from "../../../types/scheduling";
import {
  getWeekdayShort,
  constructDate,
  generateHours,
} from "../../../utils/transformer/dates.transformer";
import { useTimelinePosition } from "../../../hooks/useTimelinePosition";
import { resolveScheduleTimezone } from "@/utils/scheduleTimezone";
import TimelineHeader from "./TimelineHeader";
import TimeLabelsColumn from "./TimeLabelsColumn";
import TimelineGridBackground from "./TimelineGridBackground";
import DailyAppointments from "./DailyAppointments";
import CurrentTimeIndicator from "./CurrentTimeIndicator";
import {
  getTimelineHoursAreaHeight,
  getTimelineScrollTopForHour,
  TIMELINE_GRID_TOP_PAD_REM,
  TIMELINE_INITIAL_VISIBLE_HOUR,
} from "../timeline.constants";

interface DailyTimelineProps {
  date: number;
  month: Date;
  appointments?: Appointment[];
  selectedAppointment: Appointment | null;
  onAppointmentClick: (appointment: Appointment) => void;
  onCloseModal: () => void;
  onStatusChange: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  handleEditScheduleClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  showTherapistName?: boolean;
  timezone?: string | null;
}

const DailyTimeline: React.FC<DailyTimelineProps> = ({
  date,
  month,
  appointments,
  selectedAppointment,
  onAppointmentClick,
  onCloseModal,
  onStatusChange,
  isStatusUpdating = false,
  handleEditScheduleClick,
  hideEditActions = false,
  showTherapistName = false,
  timezone,
}) => {
  const currentTimePos = useTimelinePosition(timezone);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  const selectedFullDate = constructDate(
    month.getFullYear(),
    month.getMonth(),
    date
  );

  const nowInZone = DateTime.now().setZone(resolveScheduleTimezone(timezone));
  const isToday =
    nowInZone.year === month.getFullYear() &&
    nowInZone.month === month.getMonth() + 1 &&
    nowInZone.day === date;

  const weekday = getWeekdayShort(selectedFullDate);
  const dayNumber = selectedFullDate.getDate();

  const hours = generateHours();
  const hoursHeight = getTimelineHoursAreaHeight(hours.length);

  useEffect(() => {
    const container = scrollContainerRef.current;
    if (!container) return;
    container.scrollTop = getTimelineScrollTopForHour(TIMELINE_INITIAL_VISIBLE_HOUR);
  }, [date, month]);

  return (
    <div className="flex flex-col">
      <TimelineHeader
        weekday={weekday}
        dayNumber={dayNumber}
        timezone={timezone}
      />

      {/* Scrollable hours + timeline area */}
      <div
        ref={scrollContainerRef}
        className="flex max-h-[min(32rem,calc(100dvh-16rem))] overflow-y-auto md:max-h-[calc(100dvh-18rem)]"
        onScroll={() => {
          if (selectedAppointment) onCloseModal();
        }}
      >
        <TimeLabelsColumn hours={hours} />

        {/* Timeline Column */}
        <div className="flex-1 relative">
          <TimelineGridBackground hours={hours} />

          {isToday ? (
            <CurrentTimeIndicator
              top={`calc(${currentTimePos} + ${TIMELINE_GRID_TOP_PAD_REM}rem)`}
            />
          ) : null}

          <DailyAppointments
            appointments={appointments ?? []}
            selectedDate={selectedFullDate}
            selectedAppointment={selectedAppointment}
            onAppointmentClick={onAppointmentClick}
            onCloseModal={onCloseModal}
            onStatusChange={onStatusChange}
            isStatusUpdating={isStatusUpdating}
            handleEditScheduleClick={handleEditScheduleClick}
            hideEditActions={hideEditActions}
            showTherapistName={showTherapistName}
            containerHeight={hoursHeight}
          />
        </div>
      </div>
    </div>
  );
};

export default DailyTimeline;
