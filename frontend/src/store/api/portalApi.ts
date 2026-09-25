import { baseApi } from "./baseApi";

export interface PortalMeResponse {
  id: number;
  clientId: string;
  fullName: string;
  email: string;
  phone: string;
  assignedTherapistId: number | null;
  timezone: string | null;
  avatarUrl: string | null;
  /** False when the assigned therapist has no Zoom integration, so the portal must not offer online booking. */
  onlineBookingAvailable: boolean;
  /** Set once the client has asked the therapist to connect Zoom. Drives the sent state of that button. */
  onlineBookingRequestedAt: string | null;
}

export interface PortalOnlineBookingRequestResponse {
  onlineBookingAvailable: boolean;
  requestedAt: string | null;
  nextRequestAllowedAt: string | null;
}

export interface PortalService {
  id: number;
  serviceCode: string;
  serviceName: string;
  description: string;
  duration: number;
  baseRate: number;
}

export interface PortalAppointment {
  id: number;
  sessionDate: string;
  sessionTime: string;
  duration: number | null;
  sessionType: string | null;
  sessionMode: string | null;
  status: string | null;
  location: string | null;
  roomName?: string | null;
  referenceNumber?: string | null;
  serviceCode?: string | null;
  serviceName?: string | null;
  serviceRate?: number | null;
  therapistName?: string | null;
  /** Present when the client has already submitted a rating (0–10). */
  clientRating?: number | null;
  clientRatingComment?: string | null;
}

export interface RateSessionRequest {
  rating: number;
  comment?: string;
}

export interface RateSessionResponse {
  sessionId: number;
  rating: number;
  taskCreated: boolean;
  taskId: number;
  sessionUrl: string;
  taskUrl: string | null;
  taskListUrl: string;
}

export type PortalSessionHistoryScope = "upcoming" | "past";

export interface PortalSessionHistoryParams {
  scope: PortalSessionHistoryScope;
}

export interface PortalSessionHistoryItem {
  id: number;
  clientId: number;
  clientName: string;
  therapistId: number;
  therapistName: string;
  sessionDate: string;
  duration: number;
  sessionType: string;
  sessionMode: string;
  status: string;
  serviceId: number;
  serviceName: string;
  roomId: number | null;
  roomName: string | null;
  notes: string;
  zoomEnabled: boolean;
  zoomMeetingId?: string;
  zoomJoinUrl?: string;
  zoomPassword?: string;
  recurrenceGroupId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PortalAvailableSlot {
  date: string;
  start: string;
  end: string;
  startUtc: string;
  endUtc: string;
  /**
   * IANA zone the API says this client reads times in — their own setting, or the
   * clinic default. Display uses this, never the device's zone.
   */
  timezone: string;
}

export interface PortalAvailableSlotsParams {
  startDate: string;
  endDate: string;
  sessionType: "online" | "in-person";
  serviceId: number;
}

export interface PortalBookAppointmentPayload {
  sessionStartUtc: string;
  serviceId: number;
  sessionType: "online" | "in-person";
  duration?: number;
  location?: string;
}

export interface PortalBookAppointmentResponse {
  message: string;
  appointment: {
    id: number;
    sessionDate: string;
    sessionTime: string;
    duration: number;
    sessionType: string;
    status: string;
    location: string;
    zoomEnabled: boolean;
    zoomJoinUrl?: string;
    zoomPassword?: string;
  };
}

export interface PortalTimezoneResponse {
  timezone: string | null;
}

export interface PortalAvatarUploadResponse {
  avatarUrl: string;
}

export interface PortalInvoice {
  id: number;
  sessionId: number | null;
  serviceCode: string;
  serviceName: string;
  sessionType: string;
  sessionMode: string;
  sessionDate: string | null;
  units: number;
  ratePerUnit: number;
  originalSubtotalAmount: number | null;
  totalAmount: number;
  insuranceCovered: boolean;
  copayAmount: number | null;
  billingDate: string;
  paymentStatus: string | null;
  billingStatus: string | null;
  paymentAmount: number | null;
  amountDue: number | null;
  outstandingAmount: number | null;
  paymentDate: string | null;
  paymentMethod: string | null;
  discountType: string | null;
  discountValue: number | null;
  discountAmount: number | null;
  createdAt: string;
}

export interface PortalInvoiceStats {
  totalInvoices: number;
  totalBilled: number;
  totalPaid: number;
}

export interface PortalInvoicesQueryParams {
  page?: number;
  pageSize?: number;
  paymentStatus?: string;
  insuranceCovered?: boolean;
  startDate?: string;
  endDate?: string;
  search?: string;
}

export interface PortalInvoicesPage {
  items: PortalInvoice[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface PortalPayInvoiceResponse {
  stripeSessionId: string;
  checkoutUrl: string;
}

export interface PortalPaymentConfig {
  onlinePaymentsEnabled: boolean;
  stripePaymentsEnabled?: boolean;
  stripeConnectReady?: boolean;
  disabledReason?: string | null;
}

export interface PortalDocument {
  id: number;
  fileName?: string;
  originalName?: string;
  fileSize?: number;
  mimeType?: string;
  documentType?: string;
  category?: string;
  description?: string;
  uploadedAt?: string;
  createdAt?: string;
  updatedAt?: string;
  previewUrl?: string;
  downloadUrl?: string;
}

export interface PortalDocumentsQueryParams {
  page?: number;
  pageSize?: number;
}

export interface PortalDocumentsPage {
  items: PortalDocument[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export const PORTAL_DOCUMENTS_PAGE_SIZE = 20;

export interface PortalAppointmentsQueryParams {
  page?: number;
  pageSize?: number;
  /** Session status filter (e.g. scheduled, cancelled). Omit for all statuses. */
  status?: string;
}

export interface PortalAppointmentsPage {
  items: PortalAppointment[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export const PORTAL_APPOINTMENTS_PAGE_SIZE = 20;

export const PORTAL_DOCUMENT_MAX_BYTES = 50 * 1024 * 1024;

export function validatePortalDocumentFile(file: File): string | null {
  const lowerName = file.name.toLowerCase();
  if (lowerName.endsWith(".doc") && !lowerName.endsWith(".docx")) {
    return ".doc files are not supported. Please upload .docx instead.";
  }

  if (file.size > PORTAL_DOCUMENT_MAX_BYTES) {
    return "Document must be 50 MB or smaller.";
  }

  return null;
}

const ACCEPTED_AVATAR_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/gif",
  "image/webp",
]);

export const PORTAL_AVATAR_MAX_BYTES = 5 * 1024 * 1024;

export function validatePortalAvatarFile(file: File): string | null {
  if (!ACCEPTED_AVATAR_TYPES.has(file.type)) {
    return "Please upload a JPEG, PNG, GIF, or WebP image.";
  }

  if (file.size > PORTAL_AVATAR_MAX_BYTES) {
    return "Image must be 5 MB or smaller.";
  }

  return null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number.parseFloat(value);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

function asNullableNumber(value: unknown): number | null {
  if (value === null || value === undefined) return null;
  return asNumber(value);
}

function asNullableString(value: unknown): string | null {
  if (value === null || value === undefined) return null;
  const normalized = asString(value);
  return normalized || null;
}

function normalizePortalInvoice(entry: unknown): PortalInvoice | null {
  if (!isRecord(entry)) return null;

  const sessionIdRaw = entry.sessionId;
  const copayRaw = entry.copayAmount;
  const paymentAmountRaw = entry.paymentAmount;
  const outstandingAmountRaw = entry.outstandingAmount;
  const amountDueRaw = entry.amountDue;
  const originalSubtotalRaw = entry.originalSubtotalAmount;
  const discountValueRaw = entry.discountValue;
  const discountAmountRaw = entry.discountAmount;

  return {
    id: asNumber(entry.id),
    sessionId:
      sessionIdRaw === null || sessionIdRaw === undefined
        ? null
        : asNumber(sessionIdRaw),
    serviceCode: asString(entry.serviceCode),
    serviceName: asString(entry.serviceName),
    sessionType: asString(entry.sessionType),
    sessionMode: asString(entry.sessionMode),
    sessionDate: asNullableString(entry.sessionDate),
    units: asNumber(entry.units),
    ratePerUnit: asNumber(entry.ratePerUnit),
    originalSubtotalAmount:
      originalSubtotalRaw === null || originalSubtotalRaw === undefined
        ? null
        : asNumber(originalSubtotalRaw),
    totalAmount: asNumber(entry.totalAmount),
    insuranceCovered: Boolean(entry.insuranceCovered),
    copayAmount:
      copayRaw === null || copayRaw === undefined ? null : asNumber(copayRaw),
    billingDate: asString(entry.billingDate),
    paymentStatus: asNullableString(entry.paymentStatus),
    billingStatus: asNullableString(entry.billingStatus),
    paymentAmount:
      paymentAmountRaw === null || paymentAmountRaw === undefined
        ? null
        : asNumber(paymentAmountRaw),
    amountDue:
      amountDueRaw === null || amountDueRaw === undefined
        ? null
        : asNumber(amountDueRaw),
    outstandingAmount:
      outstandingAmountRaw === null || outstandingAmountRaw === undefined
        ? null
        : asNumber(outstandingAmountRaw),
    paymentDate: asNullableString(entry.paymentDate),
    paymentMethod: asNullableString(entry.paymentMethod),
    discountType: asNullableString(entry.discountType),
    discountValue:
      discountValueRaw === null || discountValueRaw === undefined
        ? null
        : asNumber(discountValueRaw),
    discountAmount:
      discountAmountRaw === null || discountAmountRaw === undefined
        ? null
        : asNumber(discountAmountRaw),
    createdAt: asString(entry.createdAt),
  };
}

function normalizePortalInvoicesResponse(payload: unknown): PortalInvoice[] {
  const rows = Array.isArray(payload) ? payload : [];
  return rows
    .map((entry) => normalizePortalInvoice(entry))
    .filter((entry): entry is PortalInvoice => entry !== null);
}

function normalizePortalInvoicesPageResponse(
  payload: unknown,
): PortalInvoicesPage {
  if (Array.isArray(payload)) {
    const items = normalizePortalInvoicesResponse(payload);
    return {
      items,
      totalCount: items.length,
      page: 1,
      pageSize: items.length || 20,
      totalPages: 1,
    };
  }

  const root = isRecord(payload) ? payload : {};
  const itemsRaw = Array.isArray(root.items) ? root.items : [];

  return {
    items: itemsRaw
      .map((entry) => normalizePortalInvoice(entry))
      .filter((entry): entry is PortalInvoice => entry !== null),
    totalCount: asNumber(root.totalCount),
    page: asNumber(root.page) || 1,
    pageSize: asNumber(root.pageSize) || 20,
    totalPages: asNumber(root.totalPages) || 1,
  };
}

function normalizePortalInvoiceStatsResponse(
  payload: unknown,
): PortalInvoiceStats {
  const root = isRecord(payload) ? payload : {};

  return {
    totalInvoices: asNumber(root.totalInvoices),
    totalBilled: asNumber(root.totalBilled),
    totalPaid: asNumber(root.totalPaid),
  };
}

function normalizePortalPaymentConfig(payload: unknown): PortalPaymentConfig {
  const root = isRecord(payload) ? payload : {};
  const stripePaymentsEnabled = Boolean(
    root.stripePaymentsEnabled ?? root.stripePaymentsFeatureEnabled,
  );
  const stripeConnectReady = Boolean(
    root.stripeConnectReady ??
      (root.onboardingStatus === "CONNECTED" && root.chargesEnabled),
  );
  const onlinePaymentsEnabled =
    typeof root.onlinePaymentsEnabled === "boolean"
      ? root.onlinePaymentsEnabled
      : stripePaymentsEnabled && stripeConnectReady;

  return {
    onlinePaymentsEnabled,
    stripePaymentsEnabled,
    stripeConnectReady,
    disabledReason: asNullableString(root.disabledReason),
  };
}

function normalizePortalPayInvoiceResponse(
  payload: unknown,
): PortalPayInvoiceResponse {
  const root = isRecord(payload) ? payload : {};

  return {
    stripeSessionId: asString(root.sessionId),
    checkoutUrl: asString(root.checkoutUrl),
  };
}

function normalizePortalDocument(entry: unknown): PortalDocument | null {
  if (!isRecord(entry)) return null;

  return {
    id: asNumber(entry.id),
    fileName: asNullableString(entry.fileName) ?? undefined,
    originalName: asNullableString(entry.originalName) ?? undefined,
    fileSize: asNullableNumber(entry.fileSize) ?? undefined,
    mimeType: asNullableString(entry.mimeType) ?? undefined,
    documentType: asNullableString(entry.documentType) ?? undefined,
    category: asNullableString(entry.category) ?? undefined,
    description: asNullableString(entry.description) ?? undefined,
    uploadedAt: asNullableString(entry.uploadedAt) ?? undefined,
    createdAt: asNullableString(entry.createdAt) ?? undefined,
    updatedAt: asNullableString(entry.updatedAt) ?? undefined,
    previewUrl:
      asNullableString(entry.previewUrl) ??
      asNullableString(entry.url) ??
      undefined,
    downloadUrl: asNullableString(entry.downloadUrl) ?? undefined,
  };
}

function normalizePortalDocumentsPageResponse(
  payload: unknown,
): PortalDocumentsPage {
  if (Array.isArray(payload)) {
    const items = payload
      .map((entry) => normalizePortalDocument(entry))
      .filter((entry): entry is PortalDocument => entry !== null);

    return {
      items,
      totalCount: items.length,
      page: 1,
      pageSize: items.length || PORTAL_DOCUMENTS_PAGE_SIZE,
      totalPages: 1,
    };
  }

  const root = isRecord(payload) ? payload : {};
  const itemsRaw = Array.isArray(root.items) ? root.items : [];

  return {
    items: itemsRaw
      .map((entry) => normalizePortalDocument(entry))
      .filter((entry): entry is PortalDocument => entry !== null),
    totalCount: asNumber(root.totalCount),
    page: asNumber(root.page) || 1,
    pageSize: asNumber(root.pageSize) || PORTAL_DOCUMENTS_PAGE_SIZE,
    totalPages: asNumber(root.totalPages) || 1,
  };
}

function normalizePortalAppointment(entry: unknown): PortalAppointment | null {
  if (!isRecord(entry)) return null;

  const id = asNumber(entry.id);
  if (!id) return null;

  return {
    id,
    sessionDate: asString(entry.sessionDate),
    sessionTime: asString(entry.sessionTime),
    duration: asNullableNumber(entry.duration),
    sessionType: asNullableString(entry.sessionType),
    sessionMode: asNullableString(entry.sessionMode),
    status: asNullableString(entry.status),
    location: asNullableString(entry.location),
    roomName: asNullableString(entry.roomName),
    referenceNumber: asNullableString(entry.referenceNumber),
    serviceCode: asNullableString(entry.serviceCode),
    serviceName: asNullableString(entry.serviceName),
    serviceRate: asNullableNumber(entry.serviceRate),
    therapistName: asNullableString(entry.therapistName),
    clientRating: asNullableNumber(entry.clientRating),
    clientRatingComment: asNullableString(entry.clientRatingComment),
  };
}

function normalizePortalAppointmentsResponse(payload: unknown): PortalAppointment[] {
  const rows = Array.isArray(payload) ? payload : [];
  return rows
    .map((entry) => normalizePortalAppointment(entry))
    .filter((entry): entry is PortalAppointment => entry !== null);
}

function normalizePortalAppointmentsPageResponse(
  payload: unknown,
): PortalAppointmentsPage {
  if (Array.isArray(payload)) {
    const items = normalizePortalAppointmentsResponse(payload);
    return {
      items,
      totalCount: items.length,
      page: 1,
      pageSize: items.length || PORTAL_APPOINTMENTS_PAGE_SIZE,
      totalPages: 1,
    };
  }

  const root = isRecord(payload) ? payload : {};
  const itemsRaw = Array.isArray(root.items) ? root.items : [];

  return {
    items: itemsRaw
      .map((entry) => normalizePortalAppointment(entry))
      .filter((entry): entry is PortalAppointment => entry !== null),
    totalCount: asNumber(root.totalCount),
    page: asNumber(root.page) || 1,
    pageSize: asNumber(root.pageSize) || PORTAL_APPOINTMENTS_PAGE_SIZE,
    totalPages: asNumber(root.totalPages) || 1,
  };
}

function normalizeRateSessionResponse(payload: unknown): RateSessionResponse {
  const root = isRecord(payload) ? payload : {};

  return {
    sessionId: asNumber(root.sessionId),
    rating: asNumber(root.rating),
    taskCreated: Boolean(root.taskCreated),
    taskId: asNumber(root.taskId),
    sessionUrl: asString(root.sessionUrl),
    taskUrl: asNullableString(root.taskUrl),
    taskListUrl: asString(root.taskListUrl),
  };
}

function normalizePortalSessionHistoryItem(
  entry: unknown,
): PortalSessionHistoryItem | null {
  if (!isRecord(entry)) return null;

  return {
    id: asNumber(entry.id),
    clientId: asNumber(entry.clientId),
    clientName: asString(entry.clientName),
    therapistId: asNumber(entry.therapistId),
    therapistName: asString(entry.therapistName),
    sessionDate: asString(entry.sessionDate),
    duration: asNumber(entry.duration),
    sessionType: asString(entry.sessionType),
    sessionMode: asString(entry.sessionMode),
    status: asString(entry.status),
    serviceId: asNumber(entry.serviceId),
    serviceName: asString(entry.serviceName),
    roomId: asNullableNumber(entry.roomId),
    roomName: asNullableString(entry.roomName),
    notes: asString(entry.notes),
    zoomEnabled: Boolean(entry.zoomEnabled),
    zoomMeetingId: asString(entry.zoomMeetingId) || undefined,
    zoomJoinUrl: asString(entry.zoomJoinUrl) || undefined,
    zoomPassword: asString(entry.zoomPassword) || undefined,
    recurrenceGroupId: asNullableString(entry.recurrenceGroupId),
    createdAt: asString(entry.createdAt),
    updatedAt: asString(entry.updatedAt),
  };
}

function normalizePortalSessionHistoryResponse(
  payload: unknown,
): PortalSessionHistoryItem[] {
  const rows = Array.isArray(payload) ? payload : [];
  return rows
    .map((entry) => normalizePortalSessionHistoryItem(entry))
    .filter(Boolean) as PortalSessionHistoryItem[];
}

function normalizePortalMeResponse(payload: unknown): PortalMeResponse {
  const root = isRecord(payload) ? payload : {};
  const timezoneRaw = root.timezone;
  const timezone =
    timezoneRaw === null || timezoneRaw === undefined
      ? null
      : (() => {
          const value = asString(timezoneRaw);
          return value && value !== "null" ? value : null;
        })();

  return {
    id: asNumber(root.id),
    clientId: asString(root.clientId),
    fullName: asString(root.fullName),
    email: asString(root.email),
    phone: asString(root.phone),
    assignedTherapistId:
      root.assignedTherapistId === null || root.assignedTherapistId === undefined
        ? null
        : asNumber(root.assignedTherapistId),
    timezone,
    avatarUrl: asString(root.avatarUrl) || null,
    // Absent means an older API that cannot answer the question. Stay open: the booking
    // endpoints reject an online slot on their own, so this only ever costs a clear error.
    onlineBookingAvailable: root.onlineBookingAvailable === undefined
      ? true
      : Boolean(root.onlineBookingAvailable),
    onlineBookingRequestedAt: asNullableString(root.onlineBookingRequestedAt),
  };
}

function normalizePortalOnlineBookingRequestResponse(
  payload: unknown,
): PortalOnlineBookingRequestResponse {
  const root = isRecord(payload) ? payload : {};
  return {
    onlineBookingAvailable: Boolean(root.onlineBookingAvailable),
    requestedAt: asNullableString(root.requestedAt),
    nextRequestAllowedAt: asNullableString(root.nextRequestAllowedAt),
  };
}

function normalizePortalTimezoneResponse(payload: unknown): PortalTimezoneResponse {
  const root = isRecord(payload) ? payload : {};
  const timezoneRaw = root.timezone;

  if (timezoneRaw === null || timezoneRaw === undefined) {
    return { timezone: null };
  }

  const timezone = asString(timezoneRaw);
  if (!timezone || timezone === "null") {
    return { timezone: null };
  }

  return { timezone };
}

function normalizePortalAvailableSlotsResponse(
  payload: unknown,
): PortalAvailableSlot[] {
  let root = isRecord(payload) ? payload : {};

  if (isRecord(root.data) && isRecord(root.data.slotsByDate)) {
    root = root.data;
  }

  const slotsByDate = isRecord(root.slotsByDate) ? root.slotsByDate : {};
  const timezone = asString(root.timezone) || "UTC";
  const slots: PortalAvailableSlot[] = [];

  for (const [date, dateSlots] of Object.entries(slotsByDate)) {
    if (!Array.isArray(dateSlots)) continue;

    for (const entry of dateSlots) {
      if (!isRecord(entry)) continue;

      const start = asString(entry.start);
      if (!start) continue;

      slots.push({
        date,
        start,
        end: asString(entry.end) || start,
        startUtc: asString(entry.startUtc) || "",
        endUtc: asString(entry.endUtc) || "",
        timezone,
      });
    }
  }

  return slots.sort((left, right) => {
    if (left.date !== right.date) {
      return left.date.localeCompare(right.date);
    }

    return left.start.localeCompare(right.start);
  });
}

export const portalApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getPortalMe: builder.query<PortalMeResponse, void>({
      query: () => "/api/v1/portal/me",
      transformResponse: (payload) => normalizePortalMeResponse(payload),
    }),
    getPortalTimezone: builder.query<PortalTimezoneResponse, void>({
      query: () => "/api/v1/portal/me/timezone",
      transformResponse: (payload) => normalizePortalTimezoneResponse(payload),
    }),
    updatePortalTimezone: builder.mutation<PortalMeResponse, { timezone: string }>({
      query: (body) => ({
        url: "/api/v1/portal/me/timezone",
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => normalizePortalMeResponse(payload),
      async onQueryStarted(_arg, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(
            portalApi.util.updateQueryData("getPortalMe", undefined, () => data),
          );
          dispatch(
            portalApi.util.updateQueryData("getPortalTimezone", undefined, () => ({
              timezone: data.timezone,
            })),
          );
        } catch {
          // Keep existing cache when the update fails.
        }
      },
    }),
    getPortalAppointments: builder.query<
      PortalAppointmentsPage,
      PortalAppointmentsQueryParams | void
    >({
      query: (params) => ({
        url: "/api/v1/portal/appointments",
        params: {
          page: params?.page ?? 1,
          pageSize: params?.pageSize ?? PORTAL_APPOINTMENTS_PAGE_SIZE,
          ...(params?.status ? { status: params.status } : {}),
        },
      }),
      transformResponse: (payload) => normalizePortalAppointmentsPageResponse(payload),
      providesTags: ["PortalAppointments"],
    }),
    getPortalAppointmentById: builder.query<PortalAppointment, number>({
      query: (id) => `/api/v1/portal/appointments/${id}`,
      transformResponse: (payload) => {
        const appointment = normalizePortalAppointment(payload);
        if (!appointment) {
          throw new Error("Appointment not found.");
        }
        return appointment;
      },
      providesTags: (_result, _error, id) => [{ type: "PortalAppointments", id }],
    }),
    ratePortalSession: builder.mutation<
      RateSessionResponse,
      { sessionId: number; body: RateSessionRequest }
    >({
      query: ({ sessionId, body }) => ({
        url: `/api/v1/portal/me/sessions/${sessionId}/rating`,
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizeRateSessionResponse(payload),
      invalidatesTags: ["PortalAppointments"],
    }),
    getPortalServices: builder.query<PortalService[], void>({
      query: () => "/api/v1/portal/services",
      transformResponse: (payload) => {
        const rows = Array.isArray(payload) ? payload : [];
        return rows
          .map((entry) => {
            if (!isRecord(entry)) return null;
            return {
              id: asNumber(entry.id),
              serviceCode: asString(entry.serviceCode),
              serviceName: asString(entry.serviceName),
              description: asString(entry.description),
              duration: asNumber(entry.duration),
              baseRate: asNumber(entry.baseRate),
            } as PortalService;
          })
          .filter(Boolean) as PortalService[];
      },
    }),
    getPortalSessionHistory: builder.query<
      PortalSessionHistoryItem[],
      PortalSessionHistoryParams
    >({
      query: ({ scope }) => ({
        url: "/api/v1/portal/me/sessions-history",
        params: { scope },
      }),
      transformResponse: (payload) => normalizePortalSessionHistoryResponse(payload),
    }),
    getPortalAvailableSlots: builder.query<PortalAvailableSlot[], PortalAvailableSlotsParams>({
      query: ({ startDate, endDate, sessionType, serviceId }) => ({
        url: "/api/v1/portal/available-slots",
        params: { startDate, endDate, sessionType, serviceId },
      }),
      transformResponse: (payload) => normalizePortalAvailableSlotsResponse(payload),
      keepUnusedDataFor: 0,
    }),
    getPortalInvoices: builder.query<PortalInvoicesPage, PortalInvoicesQueryParams>({
      query: (params) => ({
        url: "/api/v1/portal/invoices",
        params,
      }),
      transformResponse: (payload) => normalizePortalInvoicesPageResponse(payload),
      providesTags: [{ type: "PortalInvoices", id: "LIST" }],
    }),
    getPortalInvoiceStats: builder.query<PortalInvoiceStats, void>({
      query: () => "/api/v1/portal/invoices/stats",
      transformResponse: (payload) => normalizePortalInvoiceStatsResponse(payload),
      providesTags: [{ type: "PortalInvoices", id: "STATS" }],
    }),
    getPortalPaymentConfig: builder.query<PortalPaymentConfig, void>({
      query: () => "/api/v1/portal/invoices/payment-config",
      transformResponse: (payload) => normalizePortalPaymentConfig(payload),
    }),
    getPortalInvoiceReceipt: builder.query<Blob, number>({
      query: (invoiceId) => ({
        url: `/api/v1/portal/invoices/${invoiceId}/receipt`,
        responseHandler: async (response) => response.blob(),
      }),
    }),
    getPortalInvoiceReceiptHtml: builder.query<string, number>({
      query: (invoiceId) => ({
        url: `/api/v1/portal/invoices/${invoiceId}/receipt-html`,
        responseHandler: "text",
      }),
    }),
    payPortalInvoice: builder.mutation<PortalPayInvoiceResponse, number>({
      query: (invoiceId) => ({
        url: `/api/v1/portal/invoices/${invoiceId}/pay`,
        method: "POST",
      }),
      transformResponse: (payload) => normalizePortalPayInvoiceResponse(payload),
      invalidatesTags: [
        { type: "PortalInvoices", id: "LIST" },
        { type: "PortalInvoices", id: "STATS" },
      ],
    }),
    requestOnlineBooking: builder.mutation<PortalOnlineBookingRequestResponse, void>({
      query: () => ({
        url: "/api/v1/portal/online-booking-request",
        method: "POST",
      }),
      transformResponse: (payload) =>
        normalizePortalOnlineBookingRequestResponse(payload),
      async onQueryStarted(_arg, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(
            portalApi.util.updateQueryData("getPortalMe", undefined, (draft) => {
              draft.onlineBookingAvailable = data.onlineBookingAvailable;
              // A repeat call inside the cooldown returns the original timestamp, so this
              // keeps the button in its sent state rather than resetting it.
              draft.onlineBookingRequestedAt =
                data.requestedAt ?? draft.onlineBookingRequestedAt;
            }),
          );
        } catch {
          // Keep existing cache when the request fails.
        }
      },
    }),
    bookPortalAppointment: builder.mutation<
      PortalBookAppointmentResponse,
      PortalBookAppointmentPayload
    >({
      query: (body) => ({
        url: "/api/v1/portal/book-appointment",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => {
        const root = isRecord(payload) ? payload : {};
        const appointment = isRecord(root.appointment) ? root.appointment : {};
        return {
          message: asString(root.message),
          appointment: {
            id: asNumber(appointment.id),
            sessionDate: asString(appointment.sessionDate),
            sessionTime: asString(appointment.sessionTime),
            duration: asNumber(appointment.duration),
            sessionType: asString(appointment.sessionType),
            status: asString(appointment.status),
            location: asString(appointment.location),
            zoomEnabled: Boolean(appointment.zoomEnabled),
            zoomJoinUrl: asString(appointment.zoomJoinUrl) || undefined,
            zoomPassword: asString(appointment.zoomPassword) || undefined,
          },
        };
      },
      invalidatesTags: ["PortalAppointments"],
    }),
    uploadPortalAvatar: builder.mutation<PortalAvatarUploadResponse, FormData>({
      query: (body) => ({
        url: "/api/v1/portal/me/upload-avatar",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => {
        const root = isRecord(payload) ? payload : {};
        return {
          avatarUrl: asString(root.avatarUrl),
        };
      },
      async onQueryStarted(_arg, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(
            portalApi.util.updateQueryData("getPortalMe", undefined, (draft) => {
              draft.avatarUrl = data.avatarUrl || draft.avatarUrl;
            }),
          );
          dispatch(
            portalApi.endpoints.getPortalMe.initiate(undefined, {
              forceRefetch: true,
            }),
          );
        } catch {
          // Keep existing cache when the upload fails.
        }
      },
    }),
    getPortalDocuments: builder.query<
      PortalDocumentsPage,
      PortalDocumentsQueryParams
    >({
      query: (params) => ({
        url: "/api/v1/portal/documents",
        params,
      }),
      transformResponse: (payload) => normalizePortalDocumentsPageResponse(payload),
      providesTags: ["PortalDocuments"],
    }),
    uploadPortalDocument: builder.mutation<
      PortalDocument,
      { file: File; documentType?: string }
    >({
      query: ({ file, documentType }) => {
        const formData = new FormData();
        formData.append("file", file);
        if (documentType?.trim()) {
          formData.append("documentType", documentType.trim());
        }

        return {
          url: "/api/v1/portal/upload-document",
          method: "POST",
          body: formData,
        };
      },
      transformResponse: (payload) =>
        normalizePortalDocument(payload) ?? { id: 0 },
      invalidatesTags: ["PortalDocuments"],
    }),
    deletePortalDocument: builder.mutation<{ success?: boolean; message?: string }, number>({
      query: (id) => ({
        url: `/api/v1/portal/documents/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["PortalDocuments"],
    }),
  }),
});

export const {
  useGetPortalMeQuery,
  useGetPortalTimezoneQuery,
  useUpdatePortalTimezoneMutation,
  useUploadPortalAvatarMutation,
  useGetPortalAppointmentsQuery,
  useGetPortalAppointmentByIdQuery,
  useRatePortalSessionMutation,
  useGetPortalServicesQuery,
  useGetPortalSessionHistoryQuery,
  useGetPortalAvailableSlotsQuery,
  useGetPortalInvoicesQuery,
  useGetPortalInvoiceStatsQuery,
  useGetPortalPaymentConfigQuery,
  useLazyGetPortalInvoiceReceiptQuery,
  useLazyGetPortalInvoiceReceiptHtmlQuery,
  usePayPortalInvoiceMutation,
  useBookPortalAppointmentMutation,
  useRequestOnlineBookingMutation,
  useGetPortalDocumentsQuery,
  useUploadPortalDocumentMutation,
  useDeletePortalDocumentMutation,
} = portalApi;
