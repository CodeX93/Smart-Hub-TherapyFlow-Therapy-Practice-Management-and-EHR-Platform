import { baseApi } from "../baseApi";

export interface SessionNoteAiTemplate {
  id: number;
  name: string;
  instructions: string;
  lastUsedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateSessionNoteAiTemplatePayload {
  name: string;
  instructions: string;
}

export interface UpdateSessionNoteAiTemplatePayload {
  id: number;
  name?: string;
  instructions?: string;
}

export interface GenerateSessionNoteFinalContentPayload {
  clientId: number;
  sessionId: number;
  templateId?: number;
  customInstructions?: string;
  formData: Record<string, string>;
}

export interface GenerateSessionNoteFinalContentResponse {
  generatedContent: string;
}

const normalizeTemplate = (item: unknown): SessionNoteAiTemplate => {
  const raw = item as Record<string, unknown>;
  return {
    id: Number(raw.id),
    name: String(raw.name ?? ""),
    instructions: String(raw.instructions ?? ""),
    lastUsedAt: (raw.lastUsedAt as string) ?? null,
    createdAt: raw.createdAt as string | undefined,
    updatedAt: raw.updatedAt as string | undefined,
  };
};

export const sessionNoteAiTemplatesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSessionNoteAiTemplates: builder.query<SessionNoteAiTemplate[], void>({
      query: () => "/api/v1/session-note-ai-templates",
      transformResponse: (payload: unknown) => {
        if (!Array.isArray(payload)) return [];
        return payload.map(normalizeTemplate);
      },
      providesTags: [{ type: "SessionNoteAiTemplate", id: "LIST" }],
    }),
    createSessionNoteAiTemplate: builder.mutation<
      SessionNoteAiTemplate,
      CreateSessionNoteAiTemplatePayload
    >({
      query: (body) => ({
        url: "/api/v1/session-note-ai-templates",
        method: "POST",
        body,
      }),
      transformResponse: (payload: unknown) => normalizeTemplate(payload),
      invalidatesTags: [{ type: "SessionNoteAiTemplate", id: "LIST" }],
    }),
    updateSessionNoteAiTemplate: builder.mutation<
      SessionNoteAiTemplate,
      UpdateSessionNoteAiTemplatePayload
    >({
      query: ({ id, ...body }) => ({
        url: `/api/v1/session-note-ai-templates/${id}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload: unknown) => normalizeTemplate(payload),
      invalidatesTags: [{ type: "SessionNoteAiTemplate", id: "LIST" }],
    }),
    deleteSessionNoteAiTemplate: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/session-note-ai-templates/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: [{ type: "SessionNoteAiTemplate", id: "LIST" }],
    }),
    markSessionNoteAiTemplateLastUsed: builder.mutation<SessionNoteAiTemplate, number>({
      query: (id) => ({
        url: `/api/v1/session-note-ai-templates/${id}/mark-last-used`,
        method: "POST",
      }),
      transformResponse: (payload: unknown) => normalizeTemplate(payload),
      invalidatesTags: [{ type: "SessionNoteAiTemplate", id: "LIST" }],
    }),
    generateSessionNoteFinalContent: builder.mutation<
      GenerateSessionNoteFinalContentResponse,
      GenerateSessionNoteFinalContentPayload
    >({
      query: (body) => ({
        url: "/api/v1/ai/generate-template",
        method: "POST",
        body,
      }),
      transformResponse: (payload: unknown) => {
        const raw = payload as Record<string, unknown>;
        const data =
          typeof raw.data === "object" && raw.data !== null
            ? (raw.data as Record<string, unknown>)
            : raw;
        return {
          generatedContent: String(data.generatedContent ?? ""),
        };
      },
    }),
  }),
});

export const {
  useGetSessionNoteAiTemplatesQuery,
  useCreateSessionNoteAiTemplateMutation,
  useUpdateSessionNoteAiTemplateMutation,
  useDeleteSessionNoteAiTemplateMutation,
  useMarkSessionNoteAiTemplateLastUsedMutation,
  useGenerateSessionNoteFinalContentMutation,
} = sessionNoteAiTemplatesApi;
