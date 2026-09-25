import { baseApi } from "../baseApi";

export interface SessionNoteAmendment {
  id: number;
  sessionNoteId: number;
  amendmentText: string;
  reason: string;
  createdByUserId?: number;
  createdByUserName?: string;
  signedAt?: string;
  createdAt?: string;
}

export interface CreateSessionNoteRequest {
  sessionId: number;
  clientId: number;
  therapistId: number;
  date: string;
  sessionFocus?: string;
  symptoms?: string;
  shortTermGoals?: string;
  intervention?: string;
  progress?: string;
  remarks?: string;
  recommendations?: string;
  clientRating?: number;
  therapistRating?: number;
  progressTowardGoals?: number;
  moodBefore?: number;
  moodAfter?: number;
  riskSuicidalIdeation?: number;
  riskSelfHarm?: number;
  riskHomicidalIdeation?: number;
  riskPsychosis?: number;
  riskSubstanceUse?: number;
  riskImpulsivity?: number;
  riskAggression?: number;
  riskTraumaSymptoms?: number;
  riskNonAdherence?: number;
  riskSupportSystem?: number;
  aiEnabled?: boolean;
  customAiPrompt?: string;
  draftContent?: string;
  isDraft?: boolean;
  isFinalized?: boolean;
  aiProcessingStatus?: string;
}

export interface SessionNoteResponse extends CreateSessionNoteRequest {
  id: number;
  therapistName?: string;
  generatedContent?: string;
  draftContent?: string;
  finalContent?: string;
  isDraft?: boolean;
  isFinalized?: boolean;
  finalizedAt?: string;
  aiProcessingStatus?: string;
  voiceTranscription?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateSessionNoteRequest extends Partial<CreateSessionNoteRequest> {
  date?: string;
  draftContent?: string;
  isDraft?: boolean;
  isFinalized?: boolean;
  aiEnabled?: boolean;
  customAiPrompt?: string;
  aiProcessingStatus?: string;
}

export interface SessionNoteTranscriptionResponse {
  success: boolean;
  rawTranscription: string;
  mappedFields: Record<string, string>;
  qualityScore?: number;
  message?: string;
}

export const sessionNotesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getClientSessionNotes: builder.query<SessionNoteResponse[], number>({
      query: (clientId) => `/api/v1/session-notes/clients/${clientId}/session-notes`,
      transformResponse: (payload: unknown) =>
        Array.isArray(payload) ? (payload as SessionNoteResponse[]) : [],
      providesTags: (result, _error, clientId) =>
        result
          ? [
              ...result.map((note) => ({
                type: "SessionNotes" as const,
                id: note.id,
              })),
              { type: "SessionNotes", id: `CLIENT_${clientId}` },
              { type: "SessionNotes", id: "LIST" },
            ]
          : [
              { type: "SessionNotes", id: `CLIENT_${clientId}` },
              { type: "SessionNotes", id: "LIST" },
            ],
    }),
    getSessionNotesBySessionId: builder.query<SessionNoteResponse[], number>({
      query: (sessionId) => `/api/v1/session-notes/sessions/${sessionId}/notes`,
      transformResponse: (payload: unknown) =>
        Array.isArray(payload) ? (payload as SessionNoteResponse[]) : [],
      providesTags: (result, _error, sessionId) =>
        result
          ? [
              ...result.map((note) => ({
                type: "SessionNotes" as const,
                id: note.id,
              })),
              { type: "SessionNotes", id: `SESSION_${sessionId}` },
              { type: "SessionNotes", id: "LIST" },
            ]
          : [
              { type: "SessionNotes", id: `SESSION_${sessionId}` },
              { type: "SessionNotes", id: "LIST" },
            ],
    }),
    getSessionNoteById: builder.query<SessionNoteResponse, number>({
      query: (id) => `/api/v1/session-notes/${id}`,
      providesTags: (_result, _error, id) => [{ type: "SessionNotes", id }],
    }),
    createSessionNote: builder.mutation<SessionNoteResponse, CreateSessionNoteRequest>({
      query: (body) => ({
        url: "/api/v1/session-notes",
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, arg) => [
        { type: "SessionNotes", id: "LIST" },
        { type: "SessionNotes", id: `CLIENT_${arg.clientId}` },
        { type: "SessionNotes", id: `SESSION_${arg.sessionId}` },
      ],
    }),
    updateSessionNote: builder.mutation<
      SessionNoteResponse,
      { id: number; body: UpdateSessionNoteRequest }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/session-notes/${id}`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (result, _error, { id }) => [
        { type: "SessionNotes", id },
        { type: "SessionNotes", id: "LIST" },
        ...(result
          ? [
              {
                type: "SessionNotes" as const,
                id: `CLIENT_${result.clientId}`,
              },
              {
                type: "SessionNotes" as const,
                id: `SESSION_${result.sessionId}`,
              },
            ]
          : []),
      ],
    }),
    getSessionNoteAmendments: builder.query<SessionNoteAmendment[], number>({
      query: (id) => `/api/v1/session-notes/${id}/amendments`,
      providesTags: (_result, _error, id) => [{ type: "SessionNotes", id: `AMENDMENTS_${id}` }],
    }),
    createSessionNoteAmendment: builder.mutation<
      SessionNoteAmendment,
      { id: number; amendmentText: string; reason: string }
    >({
      query: ({ id, ...body }) => ({
        url: `/api/v1/session-notes/${id}/amendments`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "SessionNotes", id },
        { type: "SessionNotes", id: `AMENDMENTS_${id}` },
      ],
    }),
    // Signing goes through the dedicated endpoint, not an isFinalized flag on
    // update: only this path writes finalContent and audits the finalization.
    finalizeSessionNote: builder.mutation<SessionNoteResponse, number>({
      query: (id) => ({
        url: `/api/v1/session-notes/${id}/finalize`,
        method: "POST",
      }),
      invalidatesTags: (result, _error, id) => [
        { type: "SessionNotes", id },
        { type: "SessionNotes", id: "LIST" },
        ...(result
          ? [
              {
                type: "SessionNotes" as const,
                id: `CLIENT_${result.clientId}`,
              },
              {
                type: "SessionNotes" as const,
                id: `SESSION_${result.sessionId}`,
              },
            ]
          : []),
      ],
    }),
    unfinalizeSessionNote: builder.mutation<SessionNoteResponse, number>({
      query: (id) => ({
        url: `/api/v1/session-notes/${id}/unfinalize`,
        method: "POST",
      }),
      invalidatesTags: (result, _error, id) => [
        { type: "SessionNotes", id },
        { type: "SessionNotes", id: "LIST" },
        ...(result
          ? [
              {
                type: "SessionNotes" as const,
                id: `CLIENT_${result.clientId}`,
              },
              {
                type: "SessionNotes" as const,
                id: `SESSION_${result.sessionId}`,
              },
            ]
          : []),
      ],
    }),
    deleteSessionNote: builder.mutation<unknown, number>({
      query: (id) => ({
        url: `/api/v1/session-notes/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "SessionNotes", id },
        { type: "SessionNotes", id: "LIST" },
      ],
    }),
    getSessionNotePdf: builder.query<string, number>({
      query: (id) => ({
        url: `/api/v1/session-notes/${id}/pdf`,
        responseHandler: "text",
      }),
      transformResponse: (payload: unknown) =>
        typeof payload === "string" ? payload : "",
    }),
    transcribeSessionNoteAudio: builder.mutation<
      SessionNoteTranscriptionResponse,
      { audioFile: File; sessionNoteId?: number }
    >({
      query: ({ audioFile, sessionNoteId }) => {
        const formData = new FormData();
        formData.append("audio", audioFile);
        const params = new URLSearchParams();
        if (sessionNoteId) params.set("sessionNoteId", String(sessionNoteId));
        return {
          url: `/api/v1/session-notes/transcribe${params.toString() ? `?${params.toString()}` : ""}`,
          method: "POST",
          body: formData,
        };
      },
      invalidatesTags: (_result, _error, { sessionNoteId }) =>
        sessionNoteId
          ? [
              { type: "SessionNotes", id: sessionNoteId },
              { type: "SessionNotes", id: "LIST" },
            ]
          : [{ type: "SessionNotes", id: "LIST" }],
    }),
  }),
});

export const {
  useCreateSessionNoteMutation,
  useGetClientSessionNotesQuery,
  useGetSessionNotesBySessionIdQuery,
  useGetSessionNoteByIdQuery,
  useUpdateSessionNoteMutation,
  useGetSessionNoteAmendmentsQuery,
  useCreateSessionNoteAmendmentMutation,
  useFinalizeSessionNoteMutation,
  useUnfinalizeSessionNoteMutation,
  useDeleteSessionNoteMutation,
  useLazyGetSessionNotePdfQuery,
  useTranscribeSessionNoteAudioMutation,
} = sessionNotesApi;
