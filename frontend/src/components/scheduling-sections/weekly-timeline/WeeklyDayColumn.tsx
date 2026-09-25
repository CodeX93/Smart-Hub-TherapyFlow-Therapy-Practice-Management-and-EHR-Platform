import React from "react";
import { createPortal } from "react-dom";
import { cn } from "../../../lib/utils";
import type { Appointment } from "../../../types/scheduling";
import AppointmentDetailBox from "../AppointmentDetailBox";
import {
  getSessionStatusBackgroundClass,
  getSessionStatusTextClass,
} from "@/utils/sessionStatusPresentation";
import {
  getTimelineSessionHeight,
  getTimelineSessionTop,
  isShortTimelineSession,
  TIMELINE_GRID_TOP_PAD_REM,
} from "../timeline.constants";

interface WeeklyDayColumnProps {
  day: Date; // Keep for potential future use or debugging
  hours: string[];
  appointments: Appointment[];
  selectedAppointment?: Appointment | null;
  onAppointmentClick?: (appointment: Appointment) => void;
  currentTimePos?: string | null;
  onCloseModal?: () => void;
  onStatusChange?: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  handleEditScheduleClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  showTherapistName?: boolean;
  isLast: boolean;
  hoursHeight: string;
}

const WeeklyDayColumn: React.FC<WeeklyDayColumnProps> = ({
  hours,
  appointments,
  selectedAppointment,
  onAppointmentClick,
  currentTimePos,
  onCloseModal,
  onStatusChange,
  isStatusUpdating = false,
  handleEditScheduleClick,
  hideEditActions = false,
  showTherapistName = false,
  isLast,
  hoursHeight,
}) => {
  return (
    <div
      className={cn(
        "border-r border-(--neutral-100) relative",
        isLast && "border-r-0"
      )}
    >
      {/* horizontal lines */}
      <div className="pt-2">
        {hours.map((_, idx) => (
          <div
            key={idx}
            className="h-12 border-t border-(--neutral-100) w-full relative"
          />
        ))}
        <div className="border-t border-(--neutral-100) w-full" />
      </div>

      {currentTimePos !== null && currentTimePos !== undefined ? (
        <div
          className="pointer-events-none absolute left-0 right-0 z-10 flex items-center"
          style={{
            top: `calc(${currentTimePos} + ${TIMELINE_GRID_TOP_PAD_REM}rem)`,
          }}
        >
          <div className="relative flex w-full items-center">
            <div className="absolute -left-1.5 h-2.5 w-2.5 rounded-full bg-(--text-primary-dark) shadow-sm" />
            <div className="h-0.5 flex-1 bg-(--text-primary-dark)" />
          </div>
        </div>
      ) : null}

      {/* appointments for this day */}
      <div
        className="absolute top-2 left-0 right-0 z-20"
        style={{
          height: hoursHeight,
        }}
      >
        {appointments.map((apt) => {
          const top = getTimelineSessionTop(apt.startHour);
          const height = getTimelineSessionHeight(apt.duration);
          const isShort = isShortTimelineSession(apt.duration);
          const isSelected = selectedAppointment?.id === apt.id;
          const therapistLabel =
            showTherapistName && apt.therapistName
              ? apt.therapistName
              : undefined;
          return (
            <div
              key={apt.id}
              id={`appointment-${apt.id}`}
              onClick={(e) => {
                e.stopPropagation();
                onAppointmentClick?.(apt);
              }}
            className={cn(
              "absolute z-20 flex min-w-0 flex-col rounded-md border px-1.5 cursor-pointer hover:shadow-sm transition-shadow overflow-hidden",
              isShort ? "justify-center py-0" : "py-0.5",
              isSelected && "z-50 overflow-visible",
              getSessionStatusBackgroundClass(apt.status),
              "border-transparent",
            )}
              style={{
                top,
                height,
                left: "0.5rem",
                right: "0.5rem",
              }}
            >
              <span
                className={cn(
                  "min-w-0 w-full truncate text-[0.625rem] font-bold leading-tight",
                  getSessionStatusTextClass(apt.status),
                )}
                title={[`${apt.time} ${apt.name}`, therapistLabel]
                  .filter(Boolean)
                  .join(" · ")}
              >
                {apt.time} {apt.name}
              </span>
              {!isShort && therapistLabel ? (
                <span
                  className={cn(
                    "min-w-0 w-full truncate text-[0.5rem] font-medium leading-tight opacity-80",
                    getSessionStatusTextClass(apt.status),
                  )}
                >
                  {therapistLabel}
                </span>
              ) : null}

              {isSelected &&
                createPortal(
                  <div
                    onClick={(e) => e.stopPropagation()}
                    style={{
                      position: "fixed",
                      top: 0,
                      left: 0,
                      right: 0,
                      bottom: 0,
                      zIndex: 1000,
                      pointerEvents: "none",
                    }}
                  >
                    <AppointmentDetailBox
                      isOpen={true}
                      onClose={onCloseModal || (() => {})}
                      appointment={apt}
                      onStatusChange={(id, newStatus) => {
                        onStatusChange?.(id, newStatus);
                      }}
                      isStatusUpdating={isStatusUpdating}
                      handleEditScheduleClick={
                        handleEditScheduleClick || (() => {})
                      }
                      hideEditActions={hideEditActions}
                      showTherapistName={showTherapistName}
                      anchorElementId={`appointment-${apt.id}`}
                    />
                  </div>,
                  document.body
                )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default WeeklyDayColumn;
