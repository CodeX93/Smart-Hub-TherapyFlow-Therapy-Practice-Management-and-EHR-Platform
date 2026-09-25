import { baseApi } from "../baseApi";

interface AssignmentUserSummary {
  id: number;
  username: string;
  fullName: string;
  email: string;
}

export interface SupervisorAssignmentSummary {
  id: number;
  supervisor: AssignmentUserSummary;
  therapist: AssignmentUserSummary;
  assignmentType: string;
  startDate: string | null;
  endDate: string | null;
  assignedDate: string | null;
  isActive: boolean;
  notes: string | null;
  requiredMeetingFrequency: string | null;
  nextMeetingDate: string | null;
  lastMeetingDate: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface SupervisorAssignmentsListParams {
  supervisorId?: number;
  therapistId?: number;
  active?: boolean;
  search?: string;
  requiredMeetingFrequency?: string;
}

export interface CreateSupervisorAssignmentPayload {
  supervisorId: number;
  therapistId: number;
  assignmentType?: string;
  startDate?: string;
  endDate?: string | null;
  requiredMeetingFrequency: string;
  notes?: string;
}

export interface UpdateSupervisorAssignmentPayload {
  id: number;
  body: {
    assignmentType?: string;
    startDate?: string;
    endDate?: string | null;
    requiredMeetingFrequency?: string;
    notes?: string;
  };
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
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

function normalizeUser(value: unknown): AssignmentUserSummary {
  const root = isRecord(value) ? value : {};
  return {
    id: asNumber(root.id),
    username: asString(root.username),
    fullName: asString(root.fullName),
    email: asString(root.email),
  };
}

function normalizeAssignment(value: unknown): SupervisorAssignmentSummary {
  const root = isRecord(value) ? value : {};
  return {
    id: asNumber(root.id),
    supervisor: normalizeUser(root.supervisor),
    therapist: normalizeUser(root.therapist),
    assignmentType: asString(root.assignmentType),
    startDate: asString(root.startDate) || null,
    endDate: asString(root.endDate) || null,
    assignedDate: asString(root.assignedDate) || null,
    isActive: Boolean(root.isActive),
    notes: asString(root.notes) || null,
    requiredMeetingFrequency: asString(root.requiredMeetingFrequency) || null,
    nextMeetingDate: asString(root.nextMeetingDate) || null,
    lastMeetingDate: asString(root.lastMeetingDate) || null,
    createdAt: asString(root.createdAt) || null,
    updatedAt: asString(root.updatedAt) || null,
  };
}

function normalizeAssignmentsList(payload: unknown): SupervisorAssignmentSummary[] {
  if (Array.isArray(payload)) {
    return payload.map(normalizeAssignment);
  }

  if (isRecord(payload) && Array.isArray(payload.items)) {
    return payload.items.map(normalizeAssignment);
  }

  return [];
}

export const supervisorAssignmentsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSupervisorAssignments: builder.query<
      SupervisorAssignmentSummary[],
      SupervisorAssignmentsListParams | void
    >({
      query: (params) => {
        const query = new URLSearchParams();
        if (params?.supervisorId) query.set("supervisorId", String(params.supervisorId));
        if (params?.therapistId) query.set("therapistId", String(params.therapistId));
        if (typeof params?.active === "boolean") query.set("active", String(params.active));
        if (params?.search?.trim()) query.set("search", params.search.trim());
        if (params?.requiredMeetingFrequency?.trim()) {
          query.set("requiredMeetingFrequency", params.requiredMeetingFrequency.trim());
        }

        const suffix = query.toString();
        return `/api/v1/users/supervisor-assignments${suffix ? `?${suffix}` : ""}`;
      },
      transformResponse: (payload) => normalizeAssignmentsList(payload),
    }),
    getSupervisorAssignmentById: builder.query<SupervisorAssignmentSummary, number>({
      query: (id) => `/api/v1/users/supervisor-assignments/${id}`,
      transformResponse: (payload) => normalizeAssignment(payload),
    }),
    createSupervisorAssignment: builder.mutation<
      SupervisorAssignmentSummary,
      CreateSupervisorAssignmentPayload
    >({
      query: (body) => ({
        url: "/api/v1/users/supervisor-assignments",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizeAssignment(payload),
    }),
    updateSupervisorAssignment: builder.mutation<
      SupervisorAssignmentSummary,
      UpdateSupervisorAssignmentPayload
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/users/supervisor-assignments/${id}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) => normalizeAssignment(payload),
    }),
    deleteSupervisorAssignment: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/users/supervisor-assignments/${id}`,
        method: "DELETE",
      }),
    }),
  }),
});

export const {
  useGetSupervisorAssignmentsQuery,
  useLazyGetSupervisorAssignmentsQuery,
  useLazyGetSupervisorAssignmentByIdQuery,
  useCreateSupervisorAssignmentMutation,
  useUpdateSupervisorAssignmentMutation,
  useDeleteSupervisorAssignmentMutation,
} = supervisorAssignmentsApi;
