import React from "react";
import { cn } from "../../../lib/utils";
import type { Appointment } from "../../../types/scheduling";
import {
  getSessionStatusBackgroundClass,
  getSessionStatusTextClass,
} from "@/utils/sessionStatusPresentation";

interface DayAppointmentPillProps {
  appointment: Appointment;
  isSelected: boolean;
  onSelect: (e: React.MouseEvent) => void;
  onStatusChange?: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  onEditClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  onCloseModal?: () => void;
  showTherapistName?: boolean;
}

const DayAppointmentPill: React.FC<DayAppointmentPillProps> = ({
  appointment,
  isSelected,
  onSelect,
  showTherapistName = false,
}) => {
  const therapistLabel =
    showTherapistName && appointment.therapistName
      ? appointment.therapistName
      : undefined;

  return (
    <div
      id={`appointment-${appointment.id}`}
      className={cn(
        "relative flex min-w-0 flex-col overflow-hidden rounded-lg border px-2 py-1.5 text-left cursor-pointer transition-shadow hover:shadow-sm min-h-[28px]",
        getSessionStatusBackgroundClass(appointment.status),
        "border-transparent",
        isSelected && "ring-2 ring-(--bg-primary-dark)/30 ring-offset-1",
      )}
      title={[`${appointment.time} ${appointment.name}`, therapistLabel]
        .filter(Boolean)
        .join(" · ")}
      onClick={onSelect}
    >
      <span
        className={cn(
          "min-w-0 w-full truncate text-[0.625rem] font-bold leading-tight",
          getSessionStatusTextClass(appointment.status),
        )}
      >
        {appointment.time?.split(" ")[0]} {appointment.name}
      </span>
      {therapistLabel ? (
        <span
          className={cn(
            "min-w-0 w-full truncate text-[0.5rem] font-medium leading-tight opacity-80",
            getSessionStatusTextClass(appointment.status),
          )}
        >
          {therapistLabel}
        </span>
      ) : null}
    </div>
  );
};

export default DayAppointmentPill;
