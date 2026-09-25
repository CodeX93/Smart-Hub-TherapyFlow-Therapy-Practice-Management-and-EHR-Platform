import { ContentLoader } from "@/components/shared/ContentLoader";
import { CalendarIcon, MenuDotsIcon } from "@/components/icons/commonIcons";
import {
  getDashboardTabs,
  getFilteredSessions,
  getOverdueSessionActions,
} from "@/utils/functions/dashboard";
import SessionStatusBadge from "@/components/shared/SessionStatusBadge";

import { ClockCircle } from "@solar-icons/react-perf/category/time/Linear/ClockCircle";
import { Button } from "../ui/button";
import ActionDropdown from "../shared/ActionDropdown";
import CustomSelect from "@/components/form/CustomSelect";
import type {
  DashboardSessionItem,
  DashboardSessions,
} from "@/types/therapist-dashboard.type";
import { useState } from "react";
import {
  DEFAULT_SESSION_PERIOD,
  SESSION_PERIOD_OPTIONS,
  type SessionPeriod,
} from "@/utils/sessionPeriod";

interface SessionsProps {
  sessions: DashboardSessions;
  period?: SessionPeriod;
  onPeriodChange?: (period: SessionPeriod) => void;
  onViewAll?: (tab: "Previous" | "Upcoming" | "Overdue") => void;
  onSessionAction?: (action: string, sessionId: string) => void;
  onScheduleSession?: () => void;
  updatingSessionId?: string | null;
}

const Sessions = ({
  sessions,
  period = DEFAULT_SESSION_PERIOD,
  onPeriodChange,
  onViewAll,
  onSessionAction,
  onScheduleSession,
  updatingSessionId = null,
}: SessionsProps) => {
  const tabs = getDashboardTabs(sessions);
  const [selectedType, setSelectedType] = useState("Upcoming");

  const handleTypesTabChange = (type: string) => {
    setSelectedType(type);
  };

  const currentSessions = getFilteredSessions(sessions, selectedType);

  const isTerminalOverdueStatus = (status: string) => {
    const normalized = status.trim().toLowerCase().replace(/[\s_]+/g, "-");
    return normalized === "completed" || normalized === "cancelled" || normalized === "canceled";
  };

  return (
    <div className="bg-white rounded-lg border border-(--neutral-100) shadow-(--shadow) flex flex-col overflow-hidden">
      <div className="flex items-start justify-between gap-3 mb-4 px-4 pt-4">
        <div className="flex items-center gap-2 text-(--text-primary-dark) text-lg font-semibold pt-1">
          <CalendarIcon className="h-5 w-5" />
          Sessions
        </div>
        <div className="w-full max-w-52 shrink-0">
          <CustomSelect
            label="Period"
            value={period}
            onChange={(value) => onPeriodChange?.(value as SessionPeriod)}
            options={SESSION_PERIOD_OPTIONS}
            placeholder="Type to search..."
            className="h-14 rounded-xl"
          />
        </div>
      </div>
      <div className="flex-1 flex flex-col px-4 min-h-0">
        <div className="bg-(--neutral-100) p-1 rounded-full flex items-center justify-center gap-2 mb-4">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => handleTypesTabChange(tab.id)}
              aria-pressed={selectedType === tab.id}
              className={`rounded-full px-4 py-2 text-sm font-medium transition cursor-pointer duration-300 w-full ${
                selectedType === tab.id
                  ? "bg-white text-(--text-primary-dark)"
                  : "text-(--text-neutral-600)"
              }`}
            >
              {tab.label} ({tab.count})
            </button>
          ))}
        </div>

        <div className="flex-1 overflow-y-auto px-3 min-h-0 max-h-[25rem] custom-scrollbar">
          {currentSessions.length > 0 ? (
            <div className="flex flex-col">
              {currentSessions.map(
                (session: DashboardSessionItem, index: number) => (
                  <div
                    key={session.id}
                    className={`py-4 flex flex-col gap-1 ${
                      index !== currentSessions.length - 1
                        ? "border-b border-(--neutral-100)"
                        : ""
                    }`}
                  >
                    <div className="flex justify-between items-start gap-3">
                      <div className="flex flex-col min-w-0 flex-1">
                        <h3 className="text-(--text-primary-dark) font-semibold text-base truncate">
                          {session.patientName}
                        </h3>
                        <div className="flex items-center gap-2 text-(--text-neutral-400) text-sm min-w-0">
                          <div className="flex items-center gap-1">
                            <ClockCircle className="h-3.5 w-3.5" />
                            <span className="truncate">
                              {session.date}
                              {session.time ? ` - ${session.time}` : ""}
                            </span>
                          </div>
                          <span className="text-(--neutral-100)">|</span>
                          <span className="truncate">{session.sessionId}</span>
                          {session.overdueDays && (
                            <>
                              <span className="text-(--neutral-100)">|</span>
                              <span className="truncate">{session.overdueDays}</span>
                            </>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <SessionStatusBadge
                          status={session.status}
                          className="px-3 py-1"
                        />
                        {selectedType === "Overdue" && (
                          updatingSessionId === session.id ? (
                            <ContentLoader variant="inline" size="md" />
                          ) : isTerminalOverdueStatus(session.status) ? (
                            <span
                              title="That status is on highest point you cant edit it"
                              className="inline-flex"
                            >
                              <Button
                                variant="ghost"
                                size="icon"
                                disabled
                                className="cursor-not-allowed opacity-50"
                              >
                                <MenuDotsIcon size={20} />
                              </Button>
                            </span>
                          ) : (
                            <ActionDropdown
                              actions={getOverdueSessionActions(
                                session,
                                onSessionAction
                              )}
                              trigger={
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 cursor-pointer"
                                >
                                  <MenuDotsIcon size={20} />
                                </Button>
                              }
                            />
                          )
                        )}
                      </div>
                    </div>
                  </div>
                )
              )}
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center gap-2 py-10">
              <img
                src="/assets/cal.png"
                alt="empty-sessions"
                className="w-30 h-17"
              />
              <h1 className="text-(--text-primary-dark) text-xl font-semibold">
                {selectedType === "Upcoming"
                  ? "No upcoming sessions"
                  : selectedType === "Previous"
                  ? "No Previous Sessions"
                  : "No Overdue Sessions"}
              </h1>
              <p className="text-(--text-neutral-600)">
                {selectedType === "Upcoming"
                  ? "Schedule new sessions to see them here"
                  : selectedType === "Previous"
                  ? "You don’t have any previous sessions"
                  : "You don’t have any overdue sessions"}
              </p>
              {selectedType === "Upcoming" && (
                <Button
                  variant="outline"
                  className="rounded-full cursor-pointer text-(--bg-primary-dark)"
                  onClick={onScheduleSession}
                >
                  Schedule Session
                </Button>
              )}
            </div>
          )}
        </div>

        <div className="mt-auto pt-4 pb-6 text-center border-t border-(--neutral-100)">
          <Button
            variant="link"
            className="text-(--text-primary-500) font-medium text-sm cursor-pointer hover:underline h-auto p-0"
            onClick={() =>
              onViewAll?.(selectedType as "Previous" | "Upcoming" | "Overdue")
            }
          >
            View all
            {selectedType === "Overdue" &&
            (sessions.overdueTotal ?? 0) > currentSessions.length
              ? ` (${sessions.overdueTotal})`
              : selectedType === "Previous" &&
                  (sessions.previousTotal ?? 0) > currentSessions.length
                ? ` (${sessions.previousTotal})`
                : selectedType === "Upcoming" &&
                    (sessions.upcomingTotal ?? 0) > currentSessions.length
                  ? ` (${sessions.upcomingTotal})`
                  : ""}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default Sessions;
