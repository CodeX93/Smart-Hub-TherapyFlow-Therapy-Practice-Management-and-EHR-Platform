import type { ComponentPropsWithoutRef, ComponentType } from "react";
import { CalendarMinimalistic } from "@solar-icons/react-perf/category/time/Linear/CalendarMinimalistic";
import { CheckCircle } from "@solar-icons/react-perf/category/ui/Linear/CheckCircle";
import { CloseCircle } from "@solar-icons/react-perf/category/ui/Linear/CloseCircle";
import { DangerCircle } from "@solar-icons/react-perf/category/ui/Linear/DangerCircle";
import { PlayCircle } from "@solar-icons/react-perf/category/video/Linear/PlayCircle";
import { RefreshCircle } from "@solar-icons/react-perf/category/arrows/Linear/RefreshCircle";

/**
 * Session status action-menu icons — Solar Linear (same family as CalendarIcon /
 * View Calendar) so every status uses a consistent round, premium outline style.
 */

type SolarIconProps = ComponentPropsWithoutRef<typeof CheckCircle>;

type SessionStatusIcon = ComponentType<
  SolarIconProps & { size?: number | string }
>;

export const SessionStatusScheduledIcon: SessionStatusIcon = CalendarMinimalistic;
export const SessionStatusConfirmedIcon: SessionStatusIcon = CheckCircle;
export const SessionStatusInProgressIcon: SessionStatusIcon = PlayCircle;
export const SessionStatusCompletedIcon: SessionStatusIcon = CheckCircle;
export const SessionStatusCancelledIcon: SessionStatusIcon = CloseCircle;
export const SessionStatusRescheduledIcon: SessionStatusIcon = RefreshCircle;
export const SessionStatusNoShowIcon: SessionStatusIcon = DangerCircle;
