import { ContentLoader } from "@/components/shared/ContentLoader";
import { AiNoteTakerIcon, MenuDotsIcon } from "@/components/icons/commonIcons";
import {
  SessionStatusCancelledIcon,
  SessionStatusCompletedIcon,
  SessionStatusConfirmedIcon,
  SessionStatusInProgressIcon,
  SessionStatusNoShowIcon,
  SessionStatusRescheduledIcon,
  SessionStatusScheduledIcon,
} from "@/components/icons/sessionStatusMenuIcons";
import { useState, type ComponentType, type SVGProps } from "react";
import { Pencil, Monitor, X, FileText, Repeat, Receipt, Eye, CircleDollarSign } from "lucide-react";
import {
  getAllowedBackendSessionStatuses,
  isSessionStatusFinal,
} from "@/utils/sessionStatusTransitions";
import { isRecurringSeriesSession } from "@/utils/recurringSessions";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from "../ui/dropdown-menu";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../ui/tooltip";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";

interface SessionActionsMenuProps {
  onAction?: (action: string) => void;
  hasSubmittedNote?: boolean;
  canRecordSession?: boolean;
  currentStatus?: string;
  recurrenceGroupId?: string | null;
  hideStatusActions?: boolean;
  isVirtualSession?: boolean;
  hideEditAction?: boolean;
  showCreateInvoice?: boolean;
  showPayNow?: boolean;
  /** Hide record / transcript actions (e.g. scheduling calendar popup). */
  hideRecordingActions?: boolean;
  /** Hide cancel-upcoming-series action. */
  hideCancelSeries?: boolean;
  /** Hide note PDF / reopen / delete actions tied to submitted notes. */
  hideNoteManageActions?: boolean;
  hasInvoice?: boolean;
  scheduledAt?: string | null;
  hasTranscript?: boolean;
  isLoading?: boolean;
}

type SessionMenuIcon = ComponentType<
  SVGProps<SVGSVGElement> & { size?: number | string }
>;

type SessionMenuItem = {
  icon: SessionMenuIcon;
  label: string;
  action: string;
  iconColor: string;
  iconBg: string;
  /** Tailwind size class; defaults to size-4.5. */
  iconSize?: string;
  hidden?: boolean;
  disabled?: boolean;
  /** Explains the action on hover, shown beside the menu. */
  tooltip?: string;
};

const isVisibleMenuItem = (item: SessionMenuItem) => !item.hidden;

// Kept to what the flow actually does: the note is only drafted after the
// clinician runs Smart Fill, applies the suggestions and generates the note.
const AI_NOTE_TAKER_TOOLTIP = [
  "Records the whole session and AI turns it into a transcript.",
  "From the transcript, Smart Fill suggests your session note fields (focus, symptoms, goals, interventions, progress, remarks, recommendations). Review, edit and apply them, then generate the final note from your template.",
  "Saved notes are then used in the client's reports.",
  "Needs the client's AI processing consent.",
].join("\n\n");

const AI_NOTE_TAKER_DISABLED_TOOLTIP =
  "A note has already been submitted for this session. Use AI Note Taker before submitting the note.";

const NO_ACTIONS_TOOLTIP =
  "You cannot perform any action on this session.";

const SessionActionsMenu = ({
  onAction,
  hasSubmittedNote = false,
  canRecordSession = true,
  currentStatus = "scheduled",
  recurrenceGroupId = null,
  hideStatusActions = false,
  isVirtualSession = false,
  hideEditAction = false,
  showCreateInvoice = false,
  showPayNow = false,
  hideRecordingActions = false,
  hideCancelSeries = false,
  hideNoteManageActions = false,
  hasInvoice = false,
  scheduledAt = null,
  hasTranscript = false,
  isLoading = false,
}: SessionActionsMenuProps) => {
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const [isOpen, setIsOpen] = useState(false);
  const allowedStatusActions = new Set(getAllowedBackendSessionStatuses(currentStatus, {
    scheduledAt,
    hasInvoice,
    practiceTimezone: practiceConfig?.timezone,
  }));
  const normalizedStatus = currentStatus.trim().toLowerCase().replace(/[\s_-]+/g, "_");
  const isFinalStatus = isSessionStatusFinal(currentStatus);
  const isRecordEnabled = canRecordSession && !hasTranscript && !hasSubmittedNote;
  const showCancelSeries =
    !hideCancelSeries &&
    !isFinalStatus &&
    !hasInvoice &&
    isRecurringSeriesSession(recurrenceGroupId) &&
    normalizedStatus !== "cancelled";

  const recordSessionItem: SessionMenuItem = {
    icon: AiNoteTakerIcon,
    label: "AI Note Taker",
    action: "record-session",
    iconColor: isRecordEnabled ? "text-black" : "text-(--text-neutral-400)",
    iconBg: isRecordEnabled ? "bg-transparent" : "bg-(--neutral-100)",
    iconSize: "size-5",
    disabled: !isRecordEnabled,
    tooltip:
      !isRecordEnabled && hasSubmittedNote
        ? AI_NOTE_TAKER_DISABLED_TOOLTIP
        : AI_NOTE_TAKER_TOOLTIP,
    // Hide when a transcript already exists (Show Transcript covers that case).
    hidden: hideRecordingActions || isFinalStatus || hasTranscript,
  };

  const viewTranscriptItem: SessionMenuItem = {
    icon: FileText,
    label: "Show Transcript",
    action: "view-transcript",
    iconColor: "text-(--text-primary-500)",
    iconBg: "bg-(--primary-50)",
    hidden: hideRecordingActions || !hasTranscript,
  };

  const statusItems: SessionMenuItem[] = hideStatusActions
    ? []
    : [
        {
          icon: SessionStatusScheduledIcon,
          label: "Mark as Scheduled",
          action: "SCHEDULED",
          iconColor: "text-[#5878F8]",
          iconBg: "bg-[#EBEFFF]",
          hidden:
            !allowedStatusActions.has("SCHEDULED") ||
            normalizedStatus === "rescheduling",
        },
        {
          icon: SessionStatusConfirmedIcon,
          label: "Mark as Confirmed",
          action: "CONFIRMED",
          iconColor: "text-blue-600",
          iconBg: "bg-blue-50",
          hidden: !allowedStatusActions.has("CONFIRMED"),
        },
        {
          icon: SessionStatusInProgressIcon,
          label: "Mark as In Progress",
          action: "IN_PROGRESS",
          iconColor: "text-purple-600",
          iconBg: "bg-purple-50",
          hidden: !allowedStatusActions.has("IN_PROGRESS"),
        },
        {
          icon: SessionStatusCompletedIcon,
          label: "Mark as Completed",
          action: "COMPLETED",
          iconColor: "text-[#0ABF7C]",
          iconBg: "bg-[#EBFEF4]",
          hidden: !allowedStatusActions.has("COMPLETED"),
        },
        {
          icon: SessionStatusCancelledIcon,
          label: "Mark as Cancelled",
          action: "CANCELLED",
          iconColor: "text-[#EF4444]",
          iconBg: "bg-[#FEF2F2]",
          hidden:
            !allowedStatusActions.has("CANCELLED") ||
            normalizedStatus === "in_progress" ||
            normalizedStatus === "confirmed" ||
            normalizedStatus === "rescheduling",
        },
        {
          icon: SessionStatusRescheduledIcon,
          label: "Mark as Rescheduled",
          action: "RESCHEDULING",
          iconColor: "text-[#C33EF3]",
          iconBg: "bg-[#F3E8FF]",
          hidden: !allowedStatusActions.has("RESCHEDULING"),
        },
        {
          icon: SessionStatusNoShowIcon,
          label: "Mark as No-Show",
          action: "NO_SHOW",
          iconColor: "text-[#FBAC00]",
          iconBg: "bg-[#FFF7ED]",
          hidden: !allowedStatusActions.has("NO_SHOW"),
        },
      ];

  const invoiceItem: SessionMenuItem = {
    icon: hasInvoice ? Eye : Receipt,
    label: hasInvoice ? "Preview Invoice" : "Create invoice",
    action: hasInvoice ? "preview-invoice" : "create-invoice",
    iconColor: "text-(--text-primary-500)",
    iconBg: "bg-(--primary-50)",
    // Always offer preview when an invoice exists; create only when explicitly enabled.
    hidden: hasInvoice ? false : !showCreateInvoice || isFinalStatus,
  };

  const payNowItem: SessionMenuItem = {
    icon: CircleDollarSign,
    label: "Pay now",
    action: "pay-now",
    iconColor: "text-(--text-primary-500)",
    iconBg: "bg-(--primary-50)",
    hidden: !showPayNow,
  };

  const editSessionItem: SessionMenuItem = {
    icon: Pencil,
    label: "Edit Session Details",
    action: "edit",
    iconColor: "text-(--text-neutral-800)",
    iconBg: "bg-transparent",
    // A closed session can still be corrected, but not once it is invoiced:
    // editing the service, date or duration would not update the invoice.
    hidden: hideEditAction || (isFinalStatus && hasInvoice),
  };

  const menuItems: SessionMenuItem[] = (
    hasSubmittedNote
      ? [
          editSessionItem,
          invoiceItem,
          payNowItem,
          {
            icon: FileText,
            label: "Download PDF",
            action: "download-note-pdf",
            iconColor: "text-(--text-primary-500)",
            iconBg: "bg-(--primary-50)",
            hidden: hideNoteManageActions,
          },
          {
            icon: FileText,
            label: "Reopen Note",
            action: "reopen-note",
            iconColor: "text-(--text-neutral-800)",
            iconBg: "bg-transparent",
            hidden: hideNoteManageActions,
          },
          recordSessionItem,
          viewTranscriptItem,
          ...statusItems,
          {
            icon: X,
            label: "Delete Session",
            action: "delete-session",
            iconColor: "text-[#EF4444]",
            iconBg: "bg-[#FEF2F2]",
            hidden: hideNoteManageActions || isFinalStatus,
          },
        ]
      : [
          editSessionItem,
          invoiceItem,
          payNowItem,
          ...(isVirtualSession && !isFinalStatus
            ? [
                {
                  icon: Monitor,
                  label: "Join Meeting",
                  action: "join",
                  iconColor: "text-(--text-neutral-800)",
                  iconBg: "bg-transparent",
                } satisfies SessionMenuItem,
              ]
            : []),
          recordSessionItem,
          viewTranscriptItem,
          ...statusItems,
          ...(showCancelSeries
            ? [
                {
                  icon: Repeat,
                  label: "Cancel Upcoming Series",
                  action: "cancel-series",
                  iconColor: "text-[#EF4444]",
                  iconBg: "bg-[#FEF2F2]",
                } satisfies SessionMenuItem,
              ]
            : []),
        ]
  ).filter(isVisibleMenuItem);

  const handleAction = (action: string) => {
    onAction?.(action);
    setIsOpen(false);
  };

  const hasNoActions = menuItems.length === 0;
  const triggerDisabled = isLoading || hasNoActions;

  const triggerButton = (
    <button
      type="button"
      className={`p-1 rounded outline-none ${
        triggerDisabled
          ? "cursor-not-allowed opacity-60"
          : "hover:bg-(--neutral-100) cursor-pointer"
      }`}
      disabled={triggerDisabled}
      aria-label={
        isLoading
          ? "Updating session"
          : hasNoActions
            ? NO_ACTIONS_TOOLTIP
            : "Session actions"
      }
      aria-disabled={triggerDisabled}
    >
      {isLoading ? (
        <ContentLoader variant="inline" size="sm" />
      ) : (
        <MenuDotsIcon size={16} color="#5B616E" />
      )}
    </button>
  );

  if (isLoading) {
    return triggerButton;
  }

  if (hasNoActions) {
    return (
      <TooltipProvider>
        <Tooltip delayDuration={200}>
          <TooltipTrigger asChild>
            <span className="inline-flex">{triggerButton}</span>
          </TooltipTrigger>
          <TooltipContent
            side="left"
            className="max-w-[13.75rem] text-center leading-snug"
          >
            {NO_ACTIONS_TOOLTIP}
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    );
  }

  return (
    <TooltipProvider>
      <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
        <DropdownMenuTrigger asChild>{triggerButton}</DropdownMenuTrigger>

        <DropdownMenuContent
          align="end"
          sideOffset={8}
          className="w-56 bg-white rounded-xl shadow-lg border border-(--neutral-200) p-0 z-[1000000] overflow-hidden py-2"
        >
          {menuItems.map((item) => {
            const Icon = item.icon;
            const menuItem = (
              <DropdownMenuItem
                key={item.action}
                onSelect={(e) => {
                  if (item.disabled) {
                    e.preventDefault();
                    return;
                  }
                  handleAction(item.action);
                }}
                disabled={item.disabled ?? false}
                className={`w-full px-4 py-2 flex items-center gap-3 hover:bg-(--neutral-50) focus:bg-(--neutral-50) text-left transition-colors cursor-pointer rounded-none disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:bg-transparent ${
                  item.tooltip ? "" : "border-b border-(--neutral-100) last:border-none"
                }`}
              >
                <div className="flex size-5 shrink-0 items-center justify-center">
                  <Icon className={`${item.iconSize ?? "size-4.5"} ${item.iconColor}`} />
                </div>
                <span className="text-sm text-(--text-primary-dark)">
                  {item.label}
                </span>
              </DropdownMenuItem>
            );

            if (!item.tooltip) return menuItem;

            // A disabled item ignores the pointer, so the tooltip hangs off a
            // wrapper that also carries the row divider.
            return (
              <Tooltip key={item.action} delayDuration={300}>
                <TooltipTrigger asChild>
                  <div className="border-b border-(--neutral-100) last:border-none">
                    {menuItem}
                  </div>
                </TooltipTrigger>
                <TooltipContent
                  side="left"
                  sideOffset={8}
                  className="max-w-[18rem] whitespace-pre-line leading-snug"
                >
                  {item.tooltip}
                </TooltipContent>
              </Tooltip>
            );
          })}
        </DropdownMenuContent>
      </DropdownMenu>
    </TooltipProvider>
  );
};

export default SessionActionsMenu;
