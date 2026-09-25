import { ContentLoader } from "@/components/shared/ContentLoader";
import { MenuDotsIcon } from "@/components/icons/commonIcons";
import React from "react";
import { Check } from "lucide-react";
import { cn } from "../../lib/utils";
import type { AppointmentStatus } from "../../types/scheduling";
import { statusConfig } from "../../pages/therapist/therapist.static";
import {
  getAllowedAppointmentStatuses,
  normalizeAppointmentStatus,
} from "@/utils/sessionStatusTransitions";
import {
  getSessionStatusLabel,
  getSessionStatusTextClass,
} from "@/utils/sessionStatusPresentation";
import { useSessionSystemOptions } from "@/hooks/useSystemOptionCatalog";
import { resolveOptionKey } from "@/utils/systemOptions";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../ui/tooltip";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";

interface StatusDropdownProps {
  currentStatus: AppointmentStatus;
  currentStatusKey?: string;
  onStatusChange: (statusKey: string) => void;
  modalPosition?: { left?: string; right?: string };
  isMobile?: boolean;
  isLoading?: boolean;
  scheduledAt?: string | null;
  hasInvoice?: boolean;
}

export const StatusDropdown: React.FC<StatusDropdownProps> = ({
  currentStatus,
  currentStatusKey,
  onStatusChange,
  isLoading = false,
  scheduledAt = null,
  hasInvoice = false,
}) => {
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const sessionCatalog = useSessionSystemOptions(true);
  const resolvedCurrentKey =
    currentStatusKey ||
    resolveOptionKey(sessionCatalog.sessionStatusOptions, currentStatus) ||
    currentStatus;

  const allowedStatuses = new Set(
    getAllowedAppointmentStatuses(resolvedCurrentKey || currentStatus, {
      scheduledAt,
      hasInvoice,
      practiceTimezone: practiceConfig?.timezone,
    }).map(
      (status) => status,
    ),
  );

  const selectableOptions = sessionCatalog.sessionStatusOptions.filter((option) => {
    if (option.optionKey === resolvedCurrentKey) return false;
    const appointmentStatus = normalizeAppointmentStatus(option.optionKey);
    return allowedStatuses.has(appointmentStatus);
  });

  const hasAvailableTransitions = selectableOptions.length > 0;
  const isFinalStatus = !hasAvailableTransitions && !isLoading;
  const isTriggerDisabled = isLoading || !hasAvailableTransitions;
  const currentLabel =
    getSessionStatusLabel(resolvedCurrentKey || currentStatus);
  const finalStatusTooltip = isFinalStatus
    ? `This session is ${currentLabel.toLowerCase()}. Its status is final and can't be changed.`
    : null;

  const triggerButton = (
    <button
      type="button"
      className={cn(
        "outline-none",
        isTriggerDisabled ? "opacity-60" : "cursor-pointer",
      )}
      disabled={isTriggerDisabled}
      aria-disabled={isTriggerDisabled}
      aria-label={
        finalStatusTooltip ?? (isLoading ? "Updating session status" : "Change session status")
      }
    >
      {isLoading ? (
        <ContentLoader variant="inline" size="md" />
      ) : (
        <MenuDotsIcon className="w-5 h-5 text-(--text-neutral-600)" />
      )}
    </button>
  );

  if (isTriggerDisabled) {
    if (finalStatusTooltip) {
      return (
        <TooltipProvider>
          <Tooltip delayDuration={200}>
            <TooltipTrigger asChild>
              <span className="inline-flex cursor-not-allowed rounded">
                {triggerButton}
              </span>
            </TooltipTrigger>
            <TooltipContent
              side="top"
              className="max-w-[13.75rem] text-center leading-snug"
            >
              {finalStatusTooltip}
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>
      );
    }

    return triggerButton;
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>{triggerButton}</DropdownMenuTrigger>

      <DropdownMenuContent
        align="end"
        sideOffset={4}
        className="w-56 p-1 rounded-xl shadow-lg border border-(--neutral-100) bg-white z-[1000000]"
      >
        {selectableOptions.map((option) => {
          const appointmentStatus = normalizeAppointmentStatus(option.optionKey);
          const StatusIcon = statusConfig[appointmentStatus]?.icon;
          const optionLabel = getSessionStatusLabel(option.optionKey);
          if (!StatusIcon) {
            return (
              <DropdownMenuItem
                key={option.optionKey}
                disabled={isLoading}
                onClick={() => onStatusChange(option.optionKey)}
                className="w-full flex items-center justify-between px-3 py-2.5 hover:bg-(--neutral-50) focus:bg-(--neutral-50) rounded-lg transition-colors cursor-pointer outline-none border-b border-(--neutral-100) last:border-0"
              >
                <span className="text-sm font-medium text-(--text-neutral-600)">
                  Mark as {optionLabel}
                </span>
              </DropdownMenuItem>
            );
          }

          return (
            <DropdownMenuItem
              key={option.optionKey}
              disabled={isLoading}
              onClick={() => onStatusChange(option.optionKey)}
              className="w-full flex items-center justify-between px-3 py-2.5 hover:bg-(--neutral-50) focus:bg-(--neutral-50) rounded-lg transition-colors cursor-pointer outline-none border-b border-(--neutral-100) last:border-0"
            >
              <div className="flex items-center gap-3">
                <StatusIcon
                  className={cn(
                    "w-5 h-5",
                    getSessionStatusTextClass(option.optionKey),
                  )}
                />
                <span className="text-sm font-medium text-(--text-neutral-600)">
                  Mark as {optionLabel}
                </span>
              </div>
              {resolvedCurrentKey === option.optionKey && (
                <Check className="w-4 h-4 text-(--status-billed)" />
              )}
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
