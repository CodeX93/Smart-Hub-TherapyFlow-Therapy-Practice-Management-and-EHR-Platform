import { baseApi } from "../baseApi";
import { formatTime12hInTimezone } from "@/utils/therapistTimezone";

export type TherapistAvailabilitySessionType = "online" | "in-person";

export interface GetTherapistAvailabilitySlotsParams {
  therapistId: number;
  date: string; // yyyy-MM-dd
  serviceId: number;
  sessionType?: TherapistAvailabilitySessionType;
}

export interface TherapistAvailabilitySlot {
  time: string;
  timezone: string;
  localTimeLabel: string;
  available: boolean;
  therapistBusy: boolean;
  roomBusy: boolean;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asBoolean(value: unknown): boolean {
  return Boolean(value);
}

function normalizeAvailabilitySlot(entry: unknown): TherapistAvailabilitySlot | null {
  if (!isRecord(entry)) return null;

  const time = asString(entry.time);
  if (!time) return null;

  const timezone = asString(entry.timezone);
  const localTimeLabel =
    formatTime12hInTimezone(time, timezone || undefined) ||
    asString(entry.localTime);

  return {
    time,
    timezone,
    localTimeLabel,
    available: asBoolean(entry.available),
    therapistBusy: asBoolean(entry.therapistBusy),
    roomBusy: asBoolean(entry.roomBusy),
  };
}

function normalizeAvailabilitySlots(payload: unknown): TherapistAvailabilitySlot[] {
  const list = Array.isArray(payload)
    ? payload
    : isRecord(payload) && Array.isArray(payload.data)
      ? payload.data
      : [];

  return list
    .map((entry) => normalizeAvailabilitySlot(entry))
    .filter((slot): slot is TherapistAvailabilitySlot => Boolean(slot));
}

export const therapistAvailabilityApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getTherapistAvailabilitySlots: builder.query<
      TherapistAvailabilitySlot[],
      GetTherapistAvailabilitySlotsParams
    >({
      query: ({ therapistId, date, serviceId, sessionType }) => ({
        url: "/api/v1/therapist-availability/availability/slots",
        params: {
          therapistId,
          date,
          serviceId,
          ...(sessionType ? { sessionType } : {}),
        },
      }),
      transformResponse: (payload) => normalizeAvailabilitySlots(payload),
      keepUnusedDataFor: 0,
    }),
  }),
});

export const { useGetTherapistAvailabilitySlotsQuery } = therapistAvailabilityApi;
