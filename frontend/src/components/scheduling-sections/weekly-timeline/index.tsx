import React, { useEffect, useMemo, useRef } from "react";
import { calendarDateKey } from "../../../utils/scheduleTimezone";
import type { Appointment } from "../../../types/scheduling";
import { generateHours } from "../../../utils/transformer/dates.transformer";
import { useWeeklyTimePosition } from "../../../hooks/useWeeklyTimePosition";
import WeeklyHeader from "../weekly-timeline/WeeklyHeader";
import WeeklyDayColumn from "../weekly-timeline/WeeklyDayColumn";
import TimeLabelsColumn from "../daily-timeline/TimeLabelsColumn";
import {
  getTimelineHoursAreaHeight,
  getTimelineScrollTopForHour,
  TIMELINE_INITIAL_VISIBLE_HOUR,
} from "../timeline.constants";

interface WeeklyTimelineProps {
  weekStart: Date;
  appointments?: Appointment[];
  selectedFullDate?: Date;
  onDaySelect?: (date: Date) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
  selectedAppointment?: Appointment | null;
  onCloseModal?: () => void;
  onStatusChange?: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  handleEditScheduleClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  showTherapistName?: boolean;
  timezone?: string | null;
}

const WeeklyTimeline: React.FC<WeeklyTimelineProps> = ({
  weekStart,
  appointments,
  selectedFullDate,
  onDaySelect,
  onAppointmentClick,
  selectedAppointment,
  onCloseModal,
  onStatusChange,
  isStatusUpdating = false,
  handleEditScheduleClick,
  hideEditActions = false,
  showTherapistName = false,
  timezone,
}) => {
  const hours = generateHours();
  const hoursHeight = getTimelineHoursAreaHeight(hours.length);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // build 7 day labels starting from weekStart (assumed Sunday)
  const days = useMemo(() => {
    return Array.from({ length: 7 }).map((_, i) => {
      const d = new Date(weekStart);
      d.setDate(weekStart.getDate() + i);
      return d;
    });
  }, [weekStart]);

  const { currentTimePos, currentDayIndex } = useWeeklyTimePosition(
    weekStart,
    timezone,
  );

  useEffect(() => {
    const container = scrollContainerRef.current;
    if (!container) return;
    container.scrollTop = getTimelineScrollTopForHour(TIMELINE_INITIAL_VISIBLE_HOUR);
  }, [weekStart]);

  const apptsByDay = useMemo(() => {
    const result: Record<number, Appointment[]> = {};

    (appointments ?? []).forEach((apt) => {
      let dayIndex = -1;
      if (apt.date) {
        if (/^\d{4}-\d{2}-\d{2}$/.test(apt.date)) {
          dayIndex = days.findIndex((d) => calendarDateKey(d) === apt.date);
        } else {
          const aptDate = new Date(apt.date);
          dayIndex = days.findIndex(
            (d) =>
              d.getFullYear() === aptDate.getFullYear() &&
              d.getMonth() === aptDate.getMonth() &&
              d.getDate() === aptDate.getDate(),
          );
        }
        // If appointment has a date but it doesn't belong to the current week, skip it
        if (dayIndex === -1) {
          return;
        }
      } else {
        return;
      }

      result[dayIndex] = result[dayIndex] || [];
      result[dayIndex].push(apt);
    });
    return result;
  }, [appointments, days]);

  return (
    <div className="flex-1 flex flex-col overflow-x-auto">
      <div className="min-w-250 md:min-w-0 flex flex-col flex-1 h-full">
        <WeeklyHeader
          days={days}
          selectedFullDate={selectedFullDate}
          onDaySelect={onDaySelect}
          timezone={timezone}
        />

        {/* Body: times + columns */}
        <div
          ref={scrollContainerRef}
          className="flex overflow-auto h-full"
          style={{ maxHeight: "calc(100vh - 350px)" }}
          onScroll={() => {
            if (selectedAppointment) onCloseModal?.();
          }}
        >
          {/* Time labels */}
          <TimeLabelsColumn hours={hours} />

          {/* Days grid */}
          <div className="flex-1 relative">
            <div style={{ minHeight: hoursHeight }}>
              <div className="grid grid-cols-7">
                {days.map((day, dayIdx) => (
                  <WeeklyDayColumn
                    key={dayIdx}
                    day={day}
                    hours={hours}
                    appointments={apptsByDay[dayIdx] || []}
                    selectedAppointment={selectedAppointment}
                    onAppointmentClick={onAppointmentClick}
                    currentTimePos={
                      dayIdx === currentDayIndex ? currentTimePos : null
                    }
                    onCloseModal={onCloseModal}
                    onStatusChange={onStatusChange}
                    isStatusUpdating={isStatusUpdating}
                    handleEditScheduleClick={handleEditScheduleClick}
                    hideEditActions={hideEditActions}
                    showTherapistName={showTherapistName}
                    isLast={dayIdx === 6}
                    hoursHeight={hoursHeight}
                  />
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default WeeklyTimeline;
