import { baseApi } from "../baseApi";
import type {
  CancelRecurringSeriesResponse,
  CreateRecurringSessionsResponse,
  RecurrencePreviewResponse,
  RecurrenceRuleRequest,
  UpdateRecurringFutureRequest,
} from "@/types/recurringSessions";
import {
  normalizeSession,
  type AdminDashboardSession,
} from "./dashboard.api";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

function normalizePreviewSession(entry: unknown) {
  if (!isRecord(entry)) return null;
  const reasons = Array.isArray(entry.reasons)
    ? entry.reasons.filter((item): item is string => typeof item === "string")
    : [];
  return {
    sessionDate: asString(entry.sessionDate),
    localDate: asString(entry.localDate),
    sessionTime: asString(entry.sessionTime),
    hasConflict: Boolean(entry.hasConflict),
    reasons,
  };
}

function normalizePreviewResponse(payload: unknown): RecurrencePreviewResponse {
  const root = isRecord(payload) ? payload : {};
  const sessions = Array.isArray(root.sessions)
    ? root.sessions.map(normalizePreviewSession).filter(Boolean)
    : [];

  return {
    sessions: sessions as RecurrencePreviewResponse["sessions"],
    totalRequested: asNumber(root.totalRequested),
    freeCount: asNumber(root.freeCount),
    conflictCount: asNumber(root.conflictCount),
  };
}

function normalizeSkippedSession(entry: unknown) {
  if (!isRecord(entry)) return null;
  const reasons = Array.isArray(entry.reasons)
    ? entry.reasons.filter((item): item is string => typeof item === "string")
    : [];
  return {
    sessionDate: asString(entry.sessionDate),
    localDate: asString(entry.localDate),
    sessionTime: asString(entry.sessionTime),
    reasons,
  };
}

function normalizeCreateRecurringResponse(
  payload: unknown,
): CreateRecurringSessionsResponse {
  const root = isRecord(payload) ? payload : {};
  const created = Array.isArray(root.created)
    ? root.created.map(normalizeSession).filter(Boolean)
    : [];
  const skipped = Array.isArray(root.skipped)
    ? root.skipped.map(normalizeSkippedSession).filter(Boolean)
    : [];

  return {
    groupId: asString(root.groupId),
    created: created as AdminDashboardSession[],
    createdCount: asNumber(root.createdCount) || created.length,
    skipped: skipped as CreateRecurringSessionsResponse["skipped"],
    skippedCount: asNumber(root.skippedCount) || skipped.length,
    warning: asString(root.warning) || null,
  };
}

export const adminRecurringSessionsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    previewRecurringSessions: builder.mutation<
      RecurrencePreviewResponse,
      RecurrenceRuleRequest
    >({
      query: (body) => ({
        url: "/api/v1/sessions/recurring/preview",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizePreviewResponse(payload),
    }),
    createRecurringSessions: builder.mutation<
      CreateRecurringSessionsResponse,
      RecurrenceRuleRequest
    >({
      query: (body) => ({
        url: "/api/v1/sessions/recurring",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizeCreateRecurringResponse(payload),
      invalidatesTags: [
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: "STATS" },
      ],
    }),
    updateRecurringFutureSessions: builder.mutation<
      AdminDashboardSession[],
      { groupId: string; body: UpdateRecurringFutureRequest }
    >({
      query: ({ groupId, body }) => ({
        url: `/api/v1/sessions/recurring/${groupId}/future`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => {
        if (!Array.isArray(payload)) return [];
        return payload.map(normalizeSession).filter(Boolean) as AdminDashboardSession[];
      },
      invalidatesTags: [
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: "STATS" },
      ],
    }),
    cancelRecurringSeries: builder.mutation<
      CancelRecurringSeriesResponse,
      string
    >({
      query: (groupId) => ({
        url: `/api/v1/sessions/recurring/${groupId}`,
        method: "DELETE",
      }),
      transformResponse: (payload) => {
        const root = isRecord(payload) ? payload : {};
        return {
          groupId: asString(root.groupId),
          cancelledCount: asNumber(root.cancelledCount),
        };
      },
      invalidatesTags: [
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: "STATS" },
      ],
    }),
  }),
});

export const {
  usePreviewRecurringSessionsMutation,
  useCreateRecurringSessionsMutation,
  useUpdateRecurringFutureSessionsMutation,
  useCancelRecurringSeriesMutation,
} = adminRecurringSessionsApi;
