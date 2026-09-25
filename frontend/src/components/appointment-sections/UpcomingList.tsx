import { CalendarIcon } from "@/components/icons/commonIcons";
import { Clock } from "lucide-react";
import { cn } from "@/lib/utils";
import SessionStatusBadge from "@/components/shared/SessionStatusBadge";
import { ZoomMeetingJoinBlock } from "@/components/shared/ZoomMeetingJoinBlock";
import { getSessionModalityBadgeClass } from "@/utils/portalSessionDisplay";

export type AppointmentListItem = {
  id: number;
  date: string;
  time: string;
  type: string;
  therapistName?: string;
  modality: string;
  status: string;
  sessionMode?: string;
  zoomEnabled?: boolean;
  zoomJoinUrl?: string;
  zoomPassword?: string;
};

type Props = { upcoming: AppointmentListItem[] };

export default function UpcomingList({ upcoming }: Props) {
  return (
    <div className="space-y-3">
      {upcoming?.length > 0 ? (
        upcoming.map((appointment) => {
          return (
            <div
              key={appointment.id}
              className="rounded-lg bg-white p-3 shadow-xs shadow-[#1E282E0A] transition"
            >
              <div className="flex min-w-0 items-start gap-3">
                <div className="flex h-fit w-fit shrink-0 items-center justify-center rounded-[0.625rem] bg-[#F3F7F8] p-2">
                  <CalendarIcon className="h-5 w-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p
                    className="truncate font-medium text-[#1B1C20]"
                    title={appointment.date}
                  >
                    {appointment.date}
                  </p>
                  <div className="mt-1 flex min-w-0 items-center gap-1">
                    <Clock
                      className="h-4 w-4 shrink-0 text-[#5B616E]"
                     
                    />
                    <p
                      className="truncate text-sm text-[#5B616E]"
                      title={appointment.time}
                    >
                      {appointment.time}
                    </p>
                  </div>
                  <p
                    className="mt-1 truncate text-xs text-[#33363D]"
                    title={appointment.type}
                  >
                    {appointment.type}
                  </p>
                  {appointment.therapistName ? (
                    <p
                      className="mt-0.5 truncate text-xs text-[#5B616E]"
                      title={appointment.therapistName}
                    >
                      {appointment.therapistName}
                    </p>
                  ) : null}
                </div>
              </div>

              <div className="mt-3 flex flex-wrap justify-end gap-2">
                <span
                  className={cn(
                    "max-w-full truncate rounded-[1.875rem] px-2 py-0.5 text-xs font-medium",
                    getSessionModalityBadgeClass(
                      appointment.sessionMode || appointment.modality,
                    ),
                  )}
                  title={appointment.modality}
                >
                  {appointment.modality}
                </span>
                <SessionStatusBadge
                  status={appointment.status}
                  className="max-w-full truncate"
                />
              </div>

              <ZoomMeetingJoinBlock
                joinUrl={appointment.zoomJoinUrl}
                password={appointment.zoomPassword}
                zoomEnabled={appointment.zoomEnabled}
                sessionMode={appointment.sessionMode}
                status={appointment.status}
                compact
                className="mt-3"
              />
            </div>
          );
        })
      ) : (
        <h2 className="px-2 font-medium">No Appointments Found!</h2>
      )}
    </div>
  );
}
