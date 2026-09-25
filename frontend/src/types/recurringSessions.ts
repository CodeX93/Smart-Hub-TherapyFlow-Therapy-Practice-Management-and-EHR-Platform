import type { AdminDashboardSession } from "@/store/api/admin/dashboard.api";

export type RecurrenceType = "weekly" | "monthly";
export type RecurrenceEndMode = "count" | "until";
export type SessionModeApi = string;

export interface RecurrenceRuleRequest {
  clientId: number;
  therapistId: number;
  serviceId: number;
  roomId?: number;
  sessionMode: SessionModeApi;
  sessionType?: string;
  notes?: string;
  zoomEnabled?: boolean;
  sessionDate: string;
  timezone?: string;
  recurrenceType: RecurrenceType;
  daysOfWeek?: number[];
  monthsOfYear?: number[];
  interval?: number;
  endMode: RecurrenceEndMode;
  count?: number;
  untilDate?: string;
}

export interface RecurrencePreviewSession {
  sessionDate: string;
  localDate: string;
  sessionTime: string;
  hasConflict: boolean;
  reasons: string[];
}

export interface RecurrencePreviewResponse {
  sessions: RecurrencePreviewSession[];
  totalRequested: number;
  freeCount: number;
  conflictCount: number;
}

export interface RecurrenceSkippedSession {
  sessionDate: string;
  localDate: string;
  sessionTime: string;
  reasons: string[];
}

export interface CreateRecurringSessionsResponse {
  groupId: string;
  created: AdminDashboardSession[];
  createdCount: number;
  skipped: RecurrenceSkippedSession[];
  skippedCount: number;
  warning?: string | null;
}

export interface UpdateRecurringFutureRequest {
  anchorId: number;
  sessionDate: string;
  roomId?: number;
  notes?: string;
  serviceId?: number;
  therapistId?: number;
  sessionType?: string;
  sessionMode?: SessionModeApi;
  zoomEnabled?: boolean;
  ignoreConflicts?: boolean;
}

export interface CancelRecurringSeriesResponse {
  groupId: string;
  cancelledCount: number;
}

export interface RecurrenceFormState {
  isRecurring: boolean;
  recurrenceType: RecurrenceType;
  daysOfWeek: number[];
  monthsOfYear: number[];
  interval: number;
  endMode: RecurrenceEndMode;
  count: number;
  untilDate: Date | null;
}

export const DEFAULT_RECURRENCE_FORM: RecurrenceFormState = {
  isRecurring: false,
  recurrenceType: "weekly",
  daysOfWeek: [],
  monthsOfYear: [],
  interval: 1,
  endMode: "count",
  count: 4,
  untilDate: null,
};
