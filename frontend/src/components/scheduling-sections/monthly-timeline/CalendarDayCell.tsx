import React, { useMemo, useRef } from "react";
import { cn } from "../../../lib/utils";
import type { Appointment } from "../../../types/scheduling";
import DayAppointmentPill from "./DayAppointmentPill";
import SeeMorePopover from "./SeeMorePopover";

interface CalendarDayCellProps {
  date: Date;
  isCurrentMonth: boolean;
  isToday: boolean;
  appointments: Appointment[];
  selectedAppointment: Appointment | null;
  onSelectAppointment: (
    appointment: Appointment | null,
    anchorId?: string,
  ) => void;
  onStatusChange: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  onEditClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  isPopoverOpen: boolean;
  onPopoverOpen: () => void;
  onPopoverClose: () => void;
  isLastCol: boolean;
  isLastRow: boolean;
  showTherapistName?: boolean;
}

const CalendarDayCell: React.FC<CalendarDayCellProps> = ({
  date,
  isCurrentMonth,
  isToday,
  appointments,
  selectedAppointment,
  onSelectAppointment,
  onStatusChange,
  isStatusUpdating = false,
  onEditClick,
  hideEditActions = false,
  isPopoverOpen,
  onPopoverOpen,
  onPopoverClose,
  isLastCol,
  isLastRow,
  showTherapistName = false,
}) => {
  const displayLimit = 2;
  const cellRef = useRef<HTMLDivElement>(null);
  const dayAnchorId = `calendar-day-${date.getTime()}`;
  const visibleAppointments = useMemo(
    () => appointments.slice(0, displayLimit),
    [appointments]
  );
  const remainingCount = Math.max(0, appointments.length - displayLimit);

  return (
    <div
      ref={cellRef}
      id={dayAnchorId}
      className={cn(
        "border-b border-r border-(--neutral-100) p-1 flex flex-col gap-1 min-h-40",
        isLastCol && "border-r-0",
        isLastRow && "border-b-0"
      )}
    >
      {isPopoverOpen && (
        <SeeMorePopover
          date={date}
          appointments={appointments}
          anchorRef={cellRef}
          onClose={onPopoverClose}
          showTherapistName={showTherapistName}
          onSelectAppointment={(apt) => {
            // Use calendar day as anchor since popover will close
            onSelectAppointment(apt, dayAnchorId);
            setTimeout(() => {
              onPopoverClose();
            }, 10);
          }}
        />
      )}

      <div className="flex justify-center mb-1">
        <span
          className={cn(
            "text-sm font-medium w-7 h-7 flex items-center justify-center rounded-full",
            isToday
              ? "bg-(--bg-primary-dark) text-white"
              : isCurrentMonth
              ? "text-(--text-primary-dark)"
              : "text-(--text-neutral-400)"
          )}
        >
          {date.getDate()}
        </span>
      </div>

      {/* Appointment Pills */}
      <div className="flex flex-col gap-1.5 flex-1 overflow-visible">
        {visibleAppointments.map((apt) => (
          <DayAppointmentPill
            key={apt.id}
            appointment={apt}
            isSelected={selectedAppointment?.id === apt.id}
            onSelect={(e) => {
              e.stopPropagation();
              onSelectAppointment(apt, `appointment-${apt.id}`);
            }}
            onStatusChange={onStatusChange}
            isStatusUpdating={isStatusUpdating}
            onEditClick={onEditClick}
            hideEditActions={hideEditActions}
            showTherapistName={showTherapistName}
            onCloseModal={() => onSelectAppointment(null)}
          />
        ))}

        {remainingCount > 0 && (
          <button
            className="text-xs text-left font-medium text-(--text-neutral-950) w-fit px-1 hover:text-(--text-primary-dark) hover:underline cursor-pointer"
            onClick={(e) => {
              e.stopPropagation();
              onPopoverOpen();
            }}
          >
            {remainingCount} more
          </button>
        )}
      </div>
    </div>
  );
};

export default CalendarDayCell;
