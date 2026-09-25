import React, { useCallback, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import type { Appointment } from "../../../types/scheduling";
import { getCalendarDays } from "../../../utils/transformer/dates.transformer";
import { calendarDateKey } from "../../../utils/scheduleTimezone";
import MonthlyHeader from "../monthly-timeline/MonthlyHeader";
import CalendarDayCell from "../monthly-timeline/CalendarDayCell";
import AppointmentDetailBox from "../AppointmentDetailBox";

interface MonthlyTimelineProps {
  currentMonth: Date;
  appointments: Appointment[];
  handleEditScheduleClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  showTherapistName?: boolean;
  onStatusChange: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  isLoading?: boolean;
}

const MonthlyTimeline: React.FC<MonthlyTimelineProps> = ({
  currentMonth,
  appointments,
  handleEditScheduleClick,
  hideEditActions = false,
  showTherapistName = false,
  onStatusChange,
  isStatusUpdating = false,
  isLoading = false,
}) => {
  const [selectedAppointment, setSelectedAppointment] =
    useState<Appointment | null>(null);
  const [detailAnchorId, setDetailAnchorId] = useState<string | undefined>();

  const [activeDateForPopover, setActiveDateForPopover] = useState<Date | null>(
    null,
  );

  const today = new Date();

  const calendarDays = getCalendarDays(currentMonth);

  const closeOverlays = useCallback(() => {
    setSelectedAppointment(null);
    setDetailAnchorId(undefined);
    setActiveDateForPopover(null);
  }, []);

  const handleSelectAppointment = useCallback(
    (appointment: Appointment | null, anchorId?: string) => {
      setSelectedAppointment(appointment);
      setDetailAnchorId(
        appointment
          ? anchorId ?? `appointment-${appointment.id}`
          : undefined,
      );
      if (appointment) {
        setActiveDateForPopover(null);
      }
    },
    [],
  );

  useEffect(() => {
    if (!selectedAppointment && !activeDateForPopover) return;

    const handleScroll = (event: Event) => {
      const target = event.target as HTMLElement | null;
      // Keep day list / nested session modals open while scrolling inside them
      if (
        target?.closest("[data-popover-content]") ||
        target?.closest("[data-scheduling-nested-modal]")
      ) {
        return;
      }
      closeOverlays();
    };

    // Listen to scroll events on the entire document
    document.addEventListener("scroll", handleScroll, true);
    window.addEventListener("scroll", handleScroll, true);

    return () => {
      document.removeEventListener("scroll", handleScroll, true);
      window.removeEventListener("scroll", handleScroll, true);
    };
  }, [activeDateForPopover, closeOverlays, selectedAppointment]);

  const getAppointmentsForDate = (date: Date): Appointment[] => {
    const dayKey = calendarDateKey(date);
    return appointments.filter((apt) => {
      if (!apt.date) return false;
      // Prefer YYYY-MM-DD practice-timezone keys from mapSessionToAppointment.
      if (/^\d{4}-\d{2}-\d{2}$/.test(apt.date)) {
        return apt.date === dayKey;
      }
      const aptDate = new Date(apt.date);
      return (
        aptDate.getFullYear() === date.getFullYear() &&
        aptDate.getMonth() === date.getMonth() &&
        aptDate.getDate() === date.getDate()
      );
    });
  };

  const resolvedAnchorId =
    detailAnchorId &&
    document.getElementById(detailAnchorId)
      ? detailAnchorId
      : selectedAppointment
        ? `calendar-day-${new Date(selectedAppointment.date ?? "").getTime()}`
        : undefined;

  return (
    <div
      className={`relative flex-1 bg-white rounded-xl border border-(--neutral-100) flex flex-col min-h-0 overflow-auto ${
        isLoading ? "opacity-60 pointer-events-none" : ""
      }`}
      onScroll={closeOverlays}
      aria-busy={isLoading}
    >
      {isLoading ? (
        <div className="absolute inset-0 z-10 flex items-center justify-center bg-white/50">
          <p className="text-sm text-(--text-neutral-600)">Loading sessions…</p>
        </div>
      ) : null}
      <div className="min-w-250 md:min-w-0 flex flex-col flex-1 min-h-max">
        <MonthlyHeader />

        <div className="grid grid-cols-7 grid-rows-6 min-h-[48rem]">
          {calendarDays.map((date, idx) => {
            const isCurrentMonth = date.getMonth() === currentMonth.getMonth();
            const isToday =
              date.getDate() === today.getDate() &&
              date.getMonth() === today.getMonth() &&
              date.getFullYear() === today.getFullYear();

            const dayAppointments = getAppointmentsForDate(date);

            return (
              <CalendarDayCell
                key={idx}
                date={date}
                isCurrentMonth={isCurrentMonth}
                isToday={isToday}
                appointments={dayAppointments}
                selectedAppointment={selectedAppointment}
                onSelectAppointment={handleSelectAppointment}
                onStatusChange={onStatusChange}
                isStatusUpdating={isStatusUpdating}
                onEditClick={handleEditScheduleClick}
                hideEditActions={hideEditActions}
                showTherapistName={showTherapistName}
                isPopoverOpen={
                  activeDateForPopover?.getTime() === date.getTime()
                }
                onPopoverOpen={() => {
                  setSelectedAppointment(null);
                  setDetailAnchorId(undefined);
                  setActiveDateForPopover(date);
                }}
                onPopoverClose={() => setActiveDateForPopover(null)}
                isLastCol={idx % 7 === 6}
                isLastRow={idx >= 35}
              />
            );
          })}
        </div>
      </div>

      {selectedAppointment &&
        createPortal(
          <div className="pointer-events-none fixed inset-0 z-[1000]">
            <AppointmentDetailBox
              isOpen
              onClose={closeOverlays}
              appointment={selectedAppointment}
              onStatusChange={onStatusChange}
              isStatusUpdating={isStatusUpdating}
              handleEditScheduleClick={handleEditScheduleClick}
              hideEditActions={hideEditActions}
              showTherapistName={showTherapistName}
              anchorElementId={resolvedAnchorId}
            />
          </div>,
          document.body,
        )}
    </div>
  );
};

export default MonthlyTimeline;
