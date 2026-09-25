import React, { useMemo } from "react";
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
} from "../timeline.constants";

interface DailyAppointmentsProps {
  appointments: Appointment[];
  selectedDate: Date;
  selectedAppointment: Appointment | null;
  onAppointmentClick: (appointment: Appointment) => void;
  onCloseModal: () => void;
  onStatusChange: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  handleEditScheduleClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  showTherapistName?: boolean;
  containerHeight: string;
}

type PositionedAppointment = Appointment & {
  layout: {
    top: string;
    left: string;
    width: string;
    height: string;
    isShort: boolean;
  };
};

const COLUMN_GAP_PERCENT = 0.6;
const EDGE_INSET_PERCENT = 0.5;

function appointmentsOverlap(a: Appointment, b: Appointment): boolean {
  return (
    Math.max(a.startHour, b.startHour) <
    Math.min(a.startHour + a.duration, b.startHour + b.duration)
  );
}

/**
 * Pack overlapping sessions into columns scoped to each overlap cluster
 * so later slots are not indented by earlier concurrent groups.
 */
function layoutDailyAppointments(
  appointments: Appointment[],
): PositionedAppointment[] {
  if (appointments.length === 0) return [];

  const sorted = [...appointments].sort((a, b) => {
    if (a.startHour !== b.startHour) return a.startHour - b.startHour;
    return b.duration - a.duration;
  });

  const clusters: Appointment[][] = [];
  let currentCluster: Appointment[] = [];
  let clusterEnd = -Infinity;

  for (const apt of sorted) {
    const aptEnd = apt.startHour + apt.duration;
    if (currentCluster.length === 0 || apt.startHour < clusterEnd) {
      currentCluster.push(apt);
      clusterEnd = Math.max(clusterEnd, aptEnd);
    } else {
      clusters.push(currentCluster);
      currentCluster = [apt];
      clusterEnd = aptEnd;
    }
  }
  if (currentCluster.length > 0) clusters.push(currentCluster);

  const layoutById = new Map<
    string,
    { colIndex: number; colCount: number }
  >();

  for (const cluster of clusters) {
    const columns: Appointment[][] = [];

    for (const apt of cluster) {
      let placed = false;
      for (let i = 0; i < columns.length; i++) {
        const column = columns[i];
        const overlapsColumn = column.some((existing) =>
          appointmentsOverlap(existing, apt),
        );
        if (!overlapsColumn) {
          column.push(apt);
          placed = true;
          break;
        }
      }
      if (!placed) columns.push([apt]);
    }

    const colCount = columns.length;
    columns.forEach((column, colIndex) => {
      column.forEach((apt) => {
        layoutById.set(apt.id, { colIndex, colCount });
      });
    });
  }

  return appointments.map((apt) => {
    const placement = layoutById.get(apt.id) ?? { colIndex: 0, colCount: 1 };
    const { colIndex, colCount } = placement;
    const usableWidth = 100 - EDGE_INSET_PERCENT * 2;
    const totalGap = COLUMN_GAP_PERCENT * Math.max(colCount - 1, 0);
    const colWidth = (usableWidth - totalGap) / colCount;
    const left =
      EDGE_INSET_PERCENT + colIndex * (colWidth + COLUMN_GAP_PERCENT);

    return {
      ...apt,
      layout: {
        top: getTimelineSessionTop(apt.startHour),
        left: `${left}%`,
        width: `${colWidth}%`,
        height: getTimelineSessionHeight(apt.duration),
        isShort: isShortTimelineSession(apt.duration),
      },
    };
  });
}

const DailyAppointments: React.FC<DailyAppointmentsProps> = ({
  appointments,
  selectedDate,
  selectedAppointment,
  onAppointmentClick,
  onCloseModal,
  onStatusChange,
  isStatusUpdating = false,
  handleEditScheduleClick,
  hideEditActions = false,
  showTherapistName = false,
  containerHeight,
}) => {
  const filteredAppointments = useMemo(() => {
    return appointments.filter((a) => {
      if (a.date) {
        const aptDate = new Date(a.date);
        return (
          aptDate.getFullYear() === selectedDate.getFullYear() &&
          aptDate.getMonth() === selectedDate.getMonth() &&
          aptDate.getDate() === selectedDate.getDate()
        );
      }
      return true;
    });
  }, [appointments, selectedDate]);

  const positionedAppointments = useMemo(
    () => layoutDailyAppointments(filteredAppointments),
    [filteredAppointments],
  );

  return (
    <div
      className="absolute top-2 left-0 right-0 z-20"
      style={{ height: containerHeight, pointerEvents: "none" }}
    >
      {positionedAppointments.map((apt) => {
        const isSelected = selectedAppointment?.id === apt.id;
        const therapistLabel =
          showTherapistName && apt.therapistName
            ? apt.therapistName
            : undefined;
        return (
          <div
            key={apt.id}
            id={`appointment-${apt.id}`}
            onClick={() => onAppointmentClick(apt)}
            className={cn(
              "absolute z-20 flex min-w-0 flex-col rounded-md border px-1.5 pointer-events-auto cursor-pointer hover:shadow-sm transition-shadow",
              apt.layout.isShort ? "justify-center py-0" : "py-1",
              isSelected ? "overflow-visible z-50" : "overflow-hidden",
              getSessionStatusBackgroundClass(apt.status),
              "border-transparent",
            )}
            style={{
              top: apt.layout.top,
              height: apt.layout.height,
              left: apt.layout.left,
              width: apt.layout.width,
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
            {!apt.layout.isShort && therapistLabel ? (
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
                    onClose={onCloseModal}
                    appointment={apt}
                    onStatusChange={onStatusChange}
                    isStatusUpdating={isStatusUpdating}
                    handleEditScheduleClick={handleEditScheduleClick}
                    hideEditActions={hideEditActions}
                    showTherapistName={showTherapistName}
                    anchorElementId={`appointment-${apt.id}`}
                  />
                </div>,
                document.body,
              )}
          </div>
        );
      })}
    </div>
  );
};

export default DailyAppointments;
