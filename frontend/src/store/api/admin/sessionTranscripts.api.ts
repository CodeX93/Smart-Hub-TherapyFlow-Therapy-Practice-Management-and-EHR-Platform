import { baseApi } from "../baseApi";
import {
  getTranscriptContent,
  TRANSCRIPT_PLANNING_MAX_CHUNKS,
  TRANSCRIPT_RETENTION_DAYS,
} from "@/utils/recording/transcriptionApi";
import { uploadTranscriptChunk } from "@/utils/recording/uploadTranscriptChunk";

export interface SessionTranscriptStartRequest {
  sessionId: number;
  language?: string;
  translateToEnglish?: boolean;
  expectedChunks?: number;
  retentionDays?: number;
}

export interface SessionTranscriptStartResponse {
  uploadId: string;
}

export interface AppendSessionTranscriptChunkRequest {
  sessionId: number;
  uploadId: string;
  chunkIndex: number;
  chunkDurationSeconds: number;
  language?: string;
  audio: Blob;
}

export interface AppendSessionTranscriptChunkResponse {
  uploadId: string;
  chunkIndex: number;
  chunkText?: string;
  chunkStatus?: string;
  chunksReceived?: number;
  receivedChunks?: number;
}

export interface SilentChunkMarker {
  index: number;
  durationSeconds: number;
}

export interface FinalizeSessionTranscriptRequest {
  sessionId: number;
  uploadId: string;
  expectedChunks: number;
  totalChunks?: number;
  silentChunks?: SilentChunkMarker[];
}

export interface FinalizeSessionTranscriptResponse {
  id: number;
  sessionId: number;
  clientId: number;
  uploadId: string;
  status: string;
  language: string;
  content: string;
  diarizedTranscript?: string | null;
  durationSeconds: number;
  wordCount: number;
}

export interface SmartFillSessionTranscriptResponse {
  sessionId: number;
  uploadId: string;
  transcript: string;
  mappedFields: Record<string, string>;
  message?: string | null;
}

export interface SessionTranscriptChunkDto {
  chunkIndex: number;
  chunkStatus: string;
  chunkText: string;
  failureReason?: string;
  receivedAt?: string;
}

export interface GetSessionTranscriptResponse {
  transcriptId: number;
  sessionId: number;
  clientId: number;
  clientName: string;
  uploadId: string;
  status: string;
  language: string;
  expectedChunks: number;
  receivedChunks: number;
  finalTranscript: string;
  diarizedTranscript?: string | null;
  content: string;
  durationSeconds: number | null;
  wordCount: number | null;
  failureReason?: string;
  startedAt?: string;
  finalizedAt?: string;
  expiresAt?: string;
  updatedAt?: string;
  chunks: SessionTranscriptChunkDto[];
}

function normalizeGetSessionTranscriptResponse(
  payload: Record<string, unknown>,
): GetSessionTranscriptResponse {
  const finalTranscript = getTranscriptContent({
    content: typeof payload.content === "string" ? payload.content : null,
    finalTranscript:
      typeof payload.finalTranscript === "string" ? payload.finalTranscript : null,
  });
  const diarizedTranscript =
    typeof payload.diarizedTranscript === "string" && payload.diarizedTranscript.trim()
      ? payload.diarizedTranscript.trim()
      : null;

  return {
    ...(payload as unknown as GetSessionTranscriptResponse),
    transcriptId:
      typeof payload.transcriptId === "number"
        ? payload.transcriptId
        : typeof payload.id === "number"
          ? payload.id
          : 0,
    finalTranscript,
    diarizedTranscript,
    content: finalTranscript,
    durationSeconds:
      typeof payload.durationSeconds === "number" ? payload.durationSeconds : null,
    wordCount: typeof payload.wordCount === "number" ? payload.wordCount : null,
    chunks: Array.isArray(payload.chunks)
      ? (payload.chunks as SessionTranscriptChunkDto[])
      : [],
  };
}

export const sessionTranscriptsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    startSessionTranscription: builder.mutation<
      SessionTranscriptStartResponse,
      SessionTranscriptStartRequest
    >({
      query: ({
        sessionId,
        language,
        translateToEnglish,
        expectedChunks = TRANSCRIPT_PLANNING_MAX_CHUNKS,
        retentionDays = TRANSCRIPT_RETENTION_DAYS,
      }) => ({
        url: `/api/v1/sessions/${sessionId}/transcribe-start`,
        method: "POST",
        body: {
          language,
          translateToEnglish,
          expectedChunks,
          retentionDays,
        },
      }),
    }),
    appendSessionTranscriptChunk: builder.mutation<
      AppendSessionTranscriptChunkResponse,
      AppendSessionTranscriptChunkRequest
    >({
      async queryFn(args) {
        try {
          const data = await uploadTranscriptChunk(args);
          return { data };
        } catch (error) {
          return {
            error: {
              status: "CUSTOM_ERROR",
              error: error instanceof Error ? error.message : "Chunk upload failed",
              data: error,
            },
          };
        }
      },
    }),
    finalizeSessionTranscription: builder.mutation<
      FinalizeSessionTranscriptResponse,
      FinalizeSessionTranscriptRequest
    >({
      query: ({ sessionId, uploadId, expectedChunks, totalChunks, silentChunks }) => ({
        url: `/api/v1/sessions/${sessionId}/transcribe-finalize`,
        method: "POST",
        body: {
          uploadId,
          expectedChunks,
          ...(totalChunks !== undefined ? { totalChunks } : {}),
          ...(silentChunks?.length ? { silentChunks } : {}),
        },
      }),
      invalidatesTags: (_result, _error, { sessionId }) => [
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: sessionId },
      ],
      transformResponse: (payload: unknown): FinalizeSessionTranscriptResponse => {
        const root =
          typeof payload === "object" && payload !== null
            ? ((payload as Record<string, unknown>).data as Record<string, unknown> | undefined) ??
              (payload as Record<string, unknown>)
            : {};
        return {
          id: typeof root.id === "number" ? root.id : 0,
          sessionId: typeof root.sessionId === "number" ? root.sessionId : 0,
          clientId: typeof root.clientId === "number" ? root.clientId : 0,
          uploadId: typeof root.uploadId === "string" ? root.uploadId : "",
          status: typeof root.status === "string" ? root.status : "",
          language: typeof root.language === "string" ? root.language : "",
          content: typeof root.content === "string" ? root.content : "",
          diarizedTranscript:
            typeof root.diarizedTranscript === "string" && root.diarizedTranscript.trim()
              ? root.diarizedTranscript
              : null,
          durationSeconds: typeof root.durationSeconds === "number" ? root.durationSeconds : 0,
          wordCount: typeof root.wordCount === "number" ? root.wordCount : 0,
        };
      },
    }),
    smartFillSessionTranscript: builder.mutation<
      SmartFillSessionTranscriptResponse,
      { sessionId: number }
    >({
      query: ({ sessionId }) => ({
        url: `/api/v1/sessions/${sessionId}/transcript/smart-fill`,
        method: "POST",
      }),
      transformResponse: (payload: unknown) => {
        const root =
          typeof payload === "object" && payload !== null
            ? ((payload as Record<string, unknown>).data as Record<string, unknown> | undefined) ??
              (payload as Record<string, unknown>)
            : {};

        return {
          sessionId: typeof root.sessionId === "number" ? root.sessionId : 0,
          uploadId: typeof root.uploadId === "string" ? root.uploadId : "",
          transcript: typeof root.transcript === "string" ? root.transcript : "",
          mappedFields:
            typeof root.mappedFields === "object" && root.mappedFields !== null
              ? (root.mappedFields as Record<string, string>)
              : {},
          message:
            typeof root.message === "string"
              ? root.message
              : root.message === null
                ? null
                : undefined,
        };
      },
    }),
    diarizeSessionTranscript: builder.mutation<
      GetSessionTranscriptResponse,
      { sessionId: number }
    >({
      query: ({ sessionId }) => ({
        url: `/api/v1/sessions/${sessionId}/transcript/diarize`,
        method: "POST",
      }),
      transformResponse: (payload: unknown) =>
        normalizeGetSessionTranscriptResponse(
          typeof payload === "object" && payload !== null
            ? ((payload as Record<string, unknown>).data as Record<string, unknown> | undefined) ??
              (payload as Record<string, unknown>)
            : {},
        ),
    }),
    getSessionTranscript: builder.query<GetSessionTranscriptResponse, number>({
      query: (sessionId) => `/api/v1/sessions/${sessionId}/transcript`,
      transformResponse: (payload: unknown) =>
        normalizeGetSessionTranscriptResponse(
          typeof payload === "object" && payload !== null
            ? (payload as Record<string, unknown>)
            : {},
        ),
    }),
    deleteSessionTranscript: builder.mutation<void, number>({
      query: (sessionId) => ({
        url: `/api/v1/sessions/${sessionId}/transcript`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, sessionId) => [
        { type: "Sessions", id: "LIST" },
        { type: "Sessions", id: sessionId },
      ],
    }),
    downloadSessionTranscript: builder.query<string, number>({
      query: (sessionId) => ({
        url: `/api/v1/sessions/${sessionId}/transcript/download`,
        responseHandler: "text",
      }),
      transformResponse: (payload: unknown) =>
        typeof payload === "string" ? payload : "",
    }),
  }),
});

export const {
  useStartSessionTranscriptionMutation,
  useAppendSessionTranscriptChunkMutation,
  useFinalizeSessionTranscriptionMutation,
  useSmartFillSessionTranscriptMutation,
  useDiarizeSessionTranscriptMutation,
  useLazyGetSessionTranscriptQuery,
  useDeleteSessionTranscriptMutation,
  useLazyDownloadSessionTranscriptQuery,
} = sessionTranscriptsApi;
