import { baseApi } from "../baseApi";

export type RoomType = "PHYSICAL" | "VIRTUAL";

export interface AdminRoom {
  id: number;
  roomNumber: string;
  roomName: string;
  capacity?: number;
  equipment?: string;
  isActive: boolean;
  roomType: RoomType;
  createdAt?: string;
  updatedAt?: string;
}

export interface RoomRequestPayload {
  roomNumber: string;
  roomName: string;
  capacity?: number;
  equipment?: string;
  isActive: boolean;
  roomType: RoomType;
}

export interface GetAvailableRoomsParams {
  therapistId: number;
  sessionDate: string;
  sessionType: "online" | "in-person";
  serviceId?: number;
  duration?: number;
  /** When editing, exclude this session so its current room stays available. */
  excludeSessionId?: number;
}

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

function asOptionalNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === "") return undefined;
  const parsed = asNumber(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function normalizeRoomType(value: unknown): RoomType {
  return asString(value).toUpperCase() === "VIRTUAL" ? "VIRTUAL" : "PHYSICAL";
}

function normalizeRoom(entry: unknown): AdminRoom | null {
  if (!isRecord(entry)) return null;

  return {
    id: asNumber(entry.id),
    roomNumber: asString(entry.roomNumber),
    roomName: asString(entry.roomName),
    capacity: asOptionalNumber(entry.capacity),
    equipment: asString(entry.equipment) || undefined,
    isActive: Boolean(entry.isActive),
    roomType: normalizeRoomType(entry.roomType),
    createdAt: asString(entry.createdAt) || undefined,
    updatedAt: asString(entry.updatedAt) || undefined,
  };
}

function normalizeRooms(payload: unknown): AdminRoom[] {
  if (!Array.isArray(payload)) return [];
  return payload.map((entry) => normalizeRoom(entry)).filter(Boolean) as AdminRoom[];
}

export const adminRoomsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminRooms: builder.query<AdminRoom[], void>({
      query: () => "/api/v1/rooms",
      providesTags: ["Rooms"],
      transformResponse: (payload) => normalizeRooms(payload),
    }),
    getAdminRoomById: builder.query<AdminRoom, number>({
      query: (roomId) => `/api/v1/rooms/${roomId}`,
      providesTags: (_result, _error, roomId) => [{ type: "Rooms", id: roomId }],
      transformResponse: (payload) =>
        normalizeRoom(payload) ?? {
          id: 0,
          roomNumber: "",
          roomName: "",
          isActive: true,
          roomType: "PHYSICAL",
        },
    }),
    getAvailableRooms: builder.query<AdminRoom[], GetAvailableRoomsParams>({
      query: ({
        therapistId,
        sessionDate,
        sessionType,
        serviceId,
        duration,
        excludeSessionId,
      }) => {
        const params = new URLSearchParams();
        params.set("therapistId", String(therapistId));
        params.set("sessionDate", sessionDate);
        params.set("sessionType", sessionType);
        if (typeof serviceId === "number" && Number.isFinite(serviceId)) {
          params.set("serviceId", String(serviceId));
        }
        if (typeof duration === "number" && Number.isFinite(duration)) {
          params.set("duration", String(duration));
        }
        if (typeof excludeSessionId === "number" && Number.isFinite(excludeSessionId)) {
          params.set("excludeSessionId", String(excludeSessionId));
        }
        return `/api/v1/rooms/available?${params.toString()}`;
      },
      transformResponse: (payload) => normalizeRooms(payload),
    }),
    createAdminRoom: builder.mutation<AdminRoom, RoomRequestPayload>({
      query: (body) => ({
        url: "/api/v1/rooms",
        method: "POST",
        body,
      }),
      invalidatesTags: ["Rooms"],
      transformResponse: (payload) =>
        normalizeRoom(payload) ?? {
          id: 0,
          roomNumber: "",
          roomName: "",
          isActive: true,
          roomType: "PHYSICAL",
        },
    }),
    updateAdminRoom: builder.mutation<
      AdminRoom,
      { roomId: number; body: RoomRequestPayload }
    >({
      query: ({ roomId, body }) => ({
        url: `/api/v1/rooms/${roomId}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (_result, _error, { roomId }) => [
        "Rooms",
        { type: "Rooms", id: roomId },
      ],
      transformResponse: (payload) =>
        normalizeRoom(payload) ?? {
          id: 0,
          roomNumber: "",
          roomName: "",
          isActive: true,
          roomType: "PHYSICAL",
        },
    }),
    deleteAdminRoom: builder.mutation<void, number>({
      query: (roomId) => ({
        url: `/api/v1/rooms/${roomId}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, roomId) => [
        "Rooms",
        { type: "Rooms", id: roomId },
      ],
    }),
  }),
});

export const {
  useGetAdminRoomsQuery,
  useLazyGetAvailableRoomsQuery,
  useLazyGetAdminRoomByIdQuery,
  useCreateAdminRoomMutation,
  useUpdateAdminRoomMutation,
  useDeleteAdminRoomMutation,
} = adminRoomsApi;
