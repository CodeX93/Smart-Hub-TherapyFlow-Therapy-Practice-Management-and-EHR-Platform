export type AppointmentStatus =
  | "scheduled"
  | "confirmed"
  | "in_progress"
  | "completed"
  | "cancelled"
  | "rescheduled"
  | "noshow"
  | "overdue";

export interface Appointment {
  id: string;
  name: string;
  time?: string;
  startHour: number;
  duration: number;
  status: AppointmentStatus;
  session: string;
  service: string;
  room: string;
  // ISO date string (e.g. '2026-01-05') — optional for backward compatibility
  date?: string;
  dateTime?: string;
  recurrenceGroupId?: string;
  clientId?: number;
  therapistId?: number;
  therapistName?: string;
  sessionMode?: string;
  zoomEnabled?: boolean;
  zoomJoinUrl?: string;
  zoomPassword?: string;
  sessionDateIso?: string;
  hasSubmittedNote?: boolean;
  canRecordSession?: boolean;
  hasTranscript?: boolean;
  billingId?: number | null;
  remainingDue?: number | null;
  invoicePaid?: boolean;
}

export type SessionStatus =
  | "Scheduled"
  | "Completed"
  | "Cancelled"
  | "Pending"
  | "Rescheduled"
  | "No Show";

export interface SessionData {
  id: string;
  clientName: string;
  status: SessionStatus;
  sessionType: string;
  service: string;
  serviceCode: string;
  room: string;
  // New fields from static data updates
  ref?: string;
  date: string;
  time: string;
  duration?: string;
  therapist?: string;
  // Legacy fields (optional)
  amount?: string;
  roomCode?: string;
  dateTime?: string;
}

export interface SchedulingFormData {
  sessionType?: string;
  client?: string;
  service?: string;
  therapist?: string;
  sessionMode: "virtual" | "in-person" | string;
  date: Date | null;
  selectedTimeSlot: string;
  room?: string;
  notes?: string;

  serviceCode?: string;
  amount?: string;
  roomCode?: string;
  dateTime?: string;
}
