import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { PencilLine } from "lucide-react";
import Pagination from "../shared/Pagination";
import type { Appointment, SessionData } from "../../types/scheduling";
import { SCHEDULING_STATIC_CONTENT } from "../../pages/therapist/therapist.static";
import SessionStatusBadge from "@/components/shared/SessionStatusBadge";

interface AllSessionCardsProps {
  handleEditScheduleClick: (appointment: Appointment) => void;
}

const AllSessionCards = ({ handleEditScheduleClick }: AllSessionCardsProps) => {
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPageState, setItemsPerPageState] = useState(12);

  const sessions = SCHEDULING_STATIC_CONTENT.allSessionsData;
  const totalPages = Math.ceil(sessions.length / itemsPerPageState);

  const mapSessionToAppointment = (session: SessionData): Appointment => {
    // Attempt to parse out a predictable status or default to "scheduled"
    // Since SessionData["status"] is "Scheduled" | "Completed" | "Cancelled" | "Pending"
    // and AppointmentStatus is lowercase, we adjust accordingly.
    const statusLower = session.status.toLowerCase();
    const validStatuses = [
      "scheduled",
      "completed",
      "cancelled",
      "rescheduled",
      "noshow",
    ];
    const status = validStatuses.includes(statusLower)
      ? (statusLower as Appointment["status"])
      : "scheduled";

    return {
      id: session.id,
      name: session.clientName,
      time: session.dateTime,
      // Defaulting startHour/duration as they aren't in SessionData
      startHour: 9,
      duration: 1,
      status,
      session: session.sessionType,
      service: session.service,
      room: session.room,
      date: new Date().toISOString().split("T")[0], // Fallback date
      therapistName: session.therapist,
    };
  };

  const startIndex = (currentPage - 1) * itemsPerPageState;
  const endIndex = startIndex + itemsPerPageState;
  const currentSessions = sessions.slice(startIndex, endIndex);

  return (
    <div className="flex flex-col h-full">
      {/* Cards Grid */}
      <div className="grid md:grid-cols-3 grid-cols-1 gap-4 mb-6">
        {currentSessions.map((session) => (
          <div
            key={session.id}
            className="bg-white border border-(--neutral-100) rounded-xl shadow-sm shadow-(--shadow) transition-shadow duration-200"
          >
            {/* Header: Client Name and Status */}
            <div className="flex min-w-0 items-start justify-between gap-3 p-4">
              <h3 className="min-w-0 flex-1 truncate pr-2 text-[0.9375rem] font-semibold text-(--text-primary-dark)" title={session.clientName}>
                {session.clientName}
              </h3>
              <SessionStatusBadge status={session.status} className="py-1" />
            </div>

            {/* Session Details */}
            <div className="space-y-2 mb-4 px-4">
              <div className="flex items-center gap-2">
                <span className="text-[0.8125rem] text-(--text-neutral-600)">
                  Session:
                </span>
                <span className="text-[0.8125rem] font-medium text-(--text-primary-dark)">
                  {session.sessionType}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-[0.8125rem] text-(--text-neutral-600)">
                  Service:
                </span>
                <span className="text-[0.8125rem] font-medium text-(--text-primary-dark)">
                  {session.service} - {session.amount}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <span className="text-[0.8125rem] text-(--text-neutral-600)">
                  Room:
                </span>
                <span className="text-[0.8125rem] font-medium text-(--text-primary-dark)">
                  {session.room} - {session.roomCode}
                </span>
              </div>
            </div>

            {/* Footer: Date/Time and Actions */}
            <div className="flex items-center justify-between rounded-bl-xl rounded-br-xl bg-(--bg-primary-50) p-4">
              <div className="flex items-center gap-2 text-(--text-primary-dark)">
                <img
                  src="/assets/calendar.png"
                  alt="calendar"
                  className="w-4 h-4"
                />
                <span className="text-sm font-medium">{session.dateTime}</span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  className="cursor-pointer"
                  onClick={() =>
                    handleEditScheduleClick(mapSessionToAppointment(session))
                  }
                >
                  <PencilLine
                    size={18}
                    className="text-(--text-primary-dark)"
                    strokeWidth={2}
                  />
                </button>
                <button
                  className="cursor-pointer"
                  onClick={() => console.log("More options:", session.id)}
                >
                  <MenuDotsIcon
                    size={18}
                    className="text-(--text-primary-dark)"
                    strokeWidth={2}
                  />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination */}

      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
        totalItems={sessions.length}
        itemsPerPage={itemsPerPageState}
        onPageChange={setCurrentPage}
        variant="clinical-forms"
        itemsPerPageOptions={[12, 24]}
        onItemsPerPageChange={(v: number) => {
          setItemsPerPageState(v);
          setCurrentPage(1);
        }}
      />
    </div>
  );
};

export default AllSessionCards;
