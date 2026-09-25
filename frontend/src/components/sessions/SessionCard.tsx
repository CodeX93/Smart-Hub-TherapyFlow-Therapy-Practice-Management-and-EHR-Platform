import { CalendarIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { DoorOpen, Repeat, Video } from "lucide-react";
import { Button } from "../ui/button";
import { cn } from "../../lib/utils";
import SessionActionsMenu from "../shared/SessionActionsMenu";
import AddSessionNoteModal from "./AddSessionNoteModal";
import { isRecurringSeriesSession } from "@/utils/recurringSessions";
import { isVirtualSession } from "@/utils/portalSessionDisplay";
import SessionStatusBadge from "@/components/shared/SessionStatusBadge";
import {
  getSessionNoteCardState,
  isSessionNoteActionPrimary,
  SESSION_NOTE_CARD_ACTIONS,
  SESSION_NOTE_CARD_LABELS,
} from "@/utils/sessionNoteCardState";
import {
  isOnlineOrVirtualMode,
  shouldShowZoomMeetingJoin,
} from "@/utils/zoomMeeting";

interface SessionCardProps {
    session: {
        id: string;
        title: string;
        status: string;
        type: string;
        room: string;
        dateTime: string;
        clientId?: number;
        therapistId?: number;
        sessionDateIso?: string;
        hasSubmittedNote?: boolean;
        canRecordSession?: boolean;
        recurrenceGroupId?: string;
        zoomEnabled?: boolean;
        zoomJoinUrl?: string;
        zoomPassword?: string;
        sessionMode?: string;
        billingId?: number | null;
        hasInvoice?: boolean;
        remainingDue?: number | null;
        invoicePaid?: boolean;
        hasTranscript?: boolean;
    };
    clientName: string;
    onAction?: (action: string, sessionId: string) => void;
    hasSessionNote?: boolean;
    hasSubmittedNote?: boolean;
    canRecordSession?: boolean;
    onViewNote?: (sessionId: number) => void;
    onNoteCreated?: () => void;
    onToast?: (message: string, type: "success" | "error" | "info") => void;
    hideStatusActions?: boolean;
    hideEditAction?: boolean;
    hideLibraryButtons?: boolean;
    hideRatingsTab?: boolean;
    showCreateInvoice?: boolean;
    showPayNow?: boolean;
}

const SessionCard = ({
    session,
    clientName,
    onAction,
    hasSessionNote = false,
    hasSubmittedNote = false,
    canRecordSession = true,
    onViewNote,
    onNoteCreated,
    onToast,
    hideStatusActions = false,
    hideEditAction = false,
    hideLibraryButtons = false,
    hideRatingsTab = false,
    showCreateInvoice = false,
    showPayNow = false,
}: SessionCardProps) => {
    const [isNoteModalOpen, setIsNoteModalOpen] = useState(false);

    const noteState = getSessionNoteCardState({
        status: session.status,
        hasNote: hasSessionNote,
        isFinalized: hasSubmittedNote || Boolean(session.hasSubmittedNote),
    });
    const noteLabel = SESSION_NOTE_CARD_LABELS[noteState];
    const noteActionPrimary = isSessionNoteActionPrimary(noteState);

    const isVirtual = isOnlineOrVirtualMode(session.sessionMode, session.zoomEnabled)
        || isVirtualSession(session);
    const canJoinZoom = shouldShowZoomMeetingJoin({
        joinUrl: session.zoomJoinUrl,
        zoomEnabled: session.zoomEnabled,
        sessionMode: session.sessionMode,
        status: session.status,
    });
    const joinUrl = session.zoomJoinUrl?.trim() || "";

    const openZoomMeeting = () => {
        if (!joinUrl) return;
        window.open(joinUrl, "_blank", "noopener,noreferrer");
    };

    const getTypeColor = (type: string) => {
        switch (type.toLowerCase()) {
            case "in person":
                return "bg-[#EBFEF4] text-[#0ABF7C]";
            case "virtual":
                return "bg-[#F3E8FF] text-[#C33EF3]";
            default:
                return "bg-(--neutral-50) text-(--text-neutral-600)";
        }
    };

    return (
        <>
            <div className="flex h-full flex-col bg-white border border-(--neutral-200) rounded-xl hover:shadow-md transition-shadow overflow-hidden">
                <div className="flex flex-1 flex-col p-4 pb-3 min-h-0">
                    <h4
                        className="text-base font-semibold text-(--text-primary-dark) mb-3 line-clamp-2"
                        title={session.title}
                    >
                        {session.title}
                    </h4>

                    <div className="flex items-center gap-2 flex-wrap">
                        <SessionStatusBadge status={session.status} />
                        <span
                            className={cn(
                                "px-2 py-1 text-xs font-medium rounded-full shrink-0",
                                getTypeColor(session.type)
                            )}
                        >
                            {session.type}
                        </span>
                        {noteLabel ? (
                            <span
                                className={cn(
                                    "px-2 py-1 text-xs font-medium rounded-full shrink-0",
                                    noteState === "missing"
                                        ? "bg-[#FCF3E2] text-[#8A5A00]"
                                        : noteState === "draft"
                                          ? "bg-(--neutral-100) text-(--text-neutral-600)"
                                          : "bg-[#E8F4EE] text-[#157347]",
                                )}
                            >
                                {noteLabel}
                            </span>
                        ) : null}
                        {isRecurringSeriesSession(session.recurrenceGroupId) ? (
                            <span className="inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded-full bg-indigo-50 text-indigo-600 shrink-0">
                                <Repeat size={12} />
                                Recurring
                            </span>
                        ) : null}
                    </div>

                    {isVirtual ? (
                        <div className="mt-2 flex min-w-0 items-center gap-2 text-(--text-neutral-600)">
                            <Video size={14} className="shrink-0" />
                            <span className="shrink-0 text-xs">Zoom Meeting:</span>
                            {canJoinZoom ? (
                                <button
                                    type="button"
                                    onClick={openZoomMeeting}
                                    className="truncate text-xs font-semibold text-(--text-primary-dark) underline decoration-(--neutral-200) underline-offset-2 transition-colors hover:text-(--status-billed)"
                                    title={joinUrl}
                                >
                                    Join
                                </button>
                            ) : (
                                <span className="truncate text-xs font-medium text-(--text-primary-dark)">
                                    —
                                </span>
                            )}
                        </div>
                    ) : (
                        <div className="mt-2 flex min-w-0 items-center gap-1 text-(--text-neutral-600)">
                            <DoorOpen size={14} className="shrink-0" />
                            <span className="truncate text-xs" title={session.room}>
                                {session.room}
                            </span>
                        </div>
                    )}
                </div>

                <div className="mt-auto shrink-0 bg-(--neutral-50) px-4 py-3 rounded-b-xl flex items-center justify-between gap-3 border-t border-(--neutral-100)">
                    <div className="flex min-w-0 items-center gap-2 text-(--text-neutral-800)">
                        <CalendarIcon size={16} className="shrink-0" />
                        <span className="truncate text-sm" title={session.dateTime}>
                            {session.dateTime}
                        </span>
                    </div>

                    <div className="flex shrink-0 items-center gap-2">
                        <Button
                            variant={noteActionPrimary ? "default" : "outline"}
                            onClick={() => {
                                if (hasSessionNote) {
                                    onViewNote?.(Number(session.id));
                                    return;
                                }
                                setIsNoteModalOpen(true);
                            }}
                            className={cn(
                                "h-8 px-3 rounded-full cursor-pointer text-xs",
                                noteActionPrimary
                                    ? "bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white font-semibold"
                                    : "border-(--neutral-200) bg-transparent hover:bg-(--neutral-50) text-(--text-neutral-800) font-normal",
                            )}
                        >
                            {SESSION_NOTE_CARD_ACTIONS[noteState]}
                        </Button>
                        {onAction ? (
                        <SessionActionsMenu
                            onAction={(action) => onAction(action, session.id)}
                            hasSubmittedNote={hasSubmittedNote || Boolean(session.hasSubmittedNote)}
                            canRecordSession={session.canRecordSession ?? canRecordSession}
                            currentStatus={session.status}
                            recurrenceGroupId={session.recurrenceGroupId}
                            hideStatusActions={hideStatusActions}
                            hideEditAction={hideEditAction}
                            isVirtualSession={isVirtual}
                            showCreateInvoice={showCreateInvoice}
                            showPayNow={showPayNow}
                            hasInvoice={Boolean(session.billingId || session.hasInvoice)}
                            scheduledAt={session.sessionDateIso ?? null}
                            hasTranscript={Boolean(session.hasTranscript)}
                          />
                        ) : null}
                    </div>
                </div>
            </div>

            {/* Add Session Note Modal */}
            <AddSessionNoteModal
                isOpen={isNoteModalOpen}
                onClose={() => setIsNoteModalOpen(false)}
                sessionData={{
                    sessionId: Number(session.id),
                    clientId: session.clientId ?? null,
                    therapistId: session.therapistId ?? null,
                    dateIso: session.sessionDateIso ?? null,
                    dateTime: session.dateTime,
                    sessionType: session.title.split('session')[0].trim(),
                    clientName: clientName
                }}
                onCreated={onNoteCreated}
                onToast={onToast}
                hideLibraryButtons={hideLibraryButtons}
                hideRatingsTab={hideRatingsTab}
            />
        </>
    );
};

export default SessionCard;
