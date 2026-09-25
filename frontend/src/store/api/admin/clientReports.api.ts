import { baseApi } from "../baseApi";
import type { ReportTemplate } from "./reportTemplates.api";

export interface ClientReport {
  id: number;
  clientId: number;
  templateId?: number | null;
  templateName?: string | null;
  generatedContent?: string | null;
  draftContent?: string | null;
  finalContent?: string | null;
  isDraft?: boolean;
  isFinalized?: boolean;
  generatedAt?: string | null;
  editedAt?: string | null;
  finalizedAt?: string | null;
  createdById?: number | null;
  createdByName?: string | null;
  finalizedById?: number | null;
  template?: ReportTemplate | null;
}

export interface ReportSupportingFile {
  id: number;
  clientId: number;
  originalName: string;
  mimeType: string;
  fileSize: number;
  documentType?: string | null;
  createdById?: number | null;
  createdAt?: string;
}

export interface GenerateClientReportPayload {
  clientId: number;
  templateId: number;
  sources?: {
    includeProfile?: boolean;
    includeNotes?: boolean;
    includeAssessments?: boolean;
  };
  supportingFileIds?: number[];
}

export interface UploadSupportingFilePayload {
  clientId: number;
  fileContent: string;
  originalName: string;
  mimeType: string;
  documentType?: string;
  templateId?: number;
}

const normalizeClientReport = (item: unknown): ClientReport => {
  const raw = item as Record<string, unknown>;
  return {
    id: Number(raw.id),
    clientId: Number(raw.clientId),
    templateId: raw.templateId != null ? Number(raw.templateId) : null,
    templateName: (raw.templateName as string) ?? null,
    generatedContent: (raw.generatedContent as string) ?? null,
    draftContent: (raw.draftContent as string) ?? null,
    finalContent: (raw.finalContent as string) ?? null,
    isDraft: Boolean(raw.isDraft ?? true),
    isFinalized: Boolean(raw.isFinalized ?? false),
    generatedAt: (raw.generatedAt as string) ?? null,
    editedAt: (raw.editedAt as string) ?? null,
    finalizedAt: (raw.finalizedAt as string) ?? null,
    createdById: raw.createdById != null ? Number(raw.createdById) : null,
    createdByName: (raw.createdByName as string) ?? null,
    finalizedById: raw.finalizedById != null ? Number(raw.finalizedById) : null,
  };
};

const normalizeSupportingFile = (item: unknown): ReportSupportingFile => {
  const raw = item as Record<string, unknown>;
  return {
    id: Number(raw.id),
    clientId: Number(raw.clientId),
    originalName: String(raw.originalName ?? ""),
    mimeType: String(raw.mimeType ?? ""),
    fileSize: Number(raw.fileSize ?? 0),
    documentType: (raw.documentType as string) ?? null,
    createdById: raw.createdById != null ? Number(raw.createdById) : null,
    createdAt: raw.createdAt as string | undefined,
  };
};

export const clientReportsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getClientReports: builder.query<ClientReport[], number>({
      query: (clientId) => `/api/v1/clients/${clientId}/reports`,
      providesTags: (_result, _error, clientId) => [
        { type: "ClientReports", id: `LIST-${clientId}` },
      ],
      transformResponse: (response: unknown) => {
        if (!Array.isArray(response)) return [];
        return response.map(normalizeClientReport);
      },
    }),
    getClientReport: builder.query<ClientReport, number>({
      query: (reportId) => `/api/v1/reports/${reportId}`,
      providesTags: (_result, _error, reportId) => [
        { type: "ClientReports", id: reportId },
      ],
      transformResponse: (response: unknown) => normalizeClientReport(response),
    }),
    generateClientReport: builder.mutation<ClientReport, GenerateClientReportPayload>({
      query: ({ clientId, ...body }) => ({
        url: `/api/v1/clients/${clientId}/reports/generate`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { clientId }) => [
        { type: "ClientReports", id: `LIST-${clientId}` },
      ],
      transformResponse: (response: unknown) => normalizeClientReport(response),
    }),
    updateClientReportDraft: builder.mutation<
      ClientReport,
      { reportId: number; draftContent: string; clientId: number }
    >({
      query: ({ reportId, draftContent }) => ({
        url: `/api/v1/reports/${reportId}`,
        method: "PUT",
        body: { draftContent },
      }),
      invalidatesTags: (_result, _error, { reportId, clientId }) => [
        { type: "ClientReports", id: reportId },
        { type: "ClientReports", id: `LIST-${clientId}` },
      ],
      transformResponse: (response: unknown) => normalizeClientReport(response),
    }),
    finalizeClientReport: builder.mutation<
      ClientReport,
      { reportId: number; clientId: number }
    >({
      query: ({ reportId }) => ({
        url: `/api/v1/reports/${reportId}/finalize`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, { reportId, clientId }) => [
        { type: "ClientReports", id: reportId },
        { type: "ClientReports", id: `LIST-${clientId}` },
      ],
      transformResponse: (response: unknown) => normalizeClientReport(response),
    }),
    unfinalizeClientReport: builder.mutation<
      ClientReport,
      { reportId: number; clientId: number }
    >({
      query: ({ reportId }) => ({
        url: `/api/v1/reports/${reportId}/unfinalize`,
        method: "POST",
      }),
      invalidatesTags: (_result, _error, { reportId, clientId }) => [
        { type: "ClientReports", id: reportId },
        { type: "ClientReports", id: `LIST-${clientId}` },
      ],
      transformResponse: (response: unknown) => normalizeClientReport(response),
    }),
    deleteClientReport: builder.mutation<void, { reportId: number; clientId: number }>({
      query: ({ reportId }) => ({
        url: `/api/v1/reports/${reportId}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, { reportId, clientId }) => [
        { type: "ClientReports", id: reportId },
        { type: "ClientReports", id: `LIST-${clientId}` },
      ],
    }),
    getReportSupportingFiles: builder.query<ReportSupportingFile[], number>({
      query: (clientId) => `/api/v1/clients/${clientId}/supporting-files`,
      providesTags: (_result, _error, clientId) => [
        { type: "ReportSupportingFiles", id: `LIST-${clientId}` },
      ],
      transformResponse: (response: unknown) => {
        if (!Array.isArray(response)) return [];
        return response.map(normalizeSupportingFile);
      },
    }),
    uploadReportSupportingFile: builder.mutation<
      ReportSupportingFile,
      UploadSupportingFilePayload
    >({
      query: ({ clientId, ...body }) => ({
        url: `/api/v1/clients/${clientId}/supporting-files`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { clientId }) => [
        { type: "ReportSupportingFiles", id: `LIST-${clientId}` },
      ],
      transformResponse: (response: unknown) => normalizeSupportingFile(response),
    }),
    deleteReportSupportingFile: builder.mutation<void, { fileId: number; clientId: number }>({
      query: ({ fileId }) => ({
        url: `/api/v1/supporting-files/${fileId}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, { clientId }) => [
        { type: "ReportSupportingFiles", id: `LIST-${clientId}` },
      ],
    }),
  }),
});

export const {
  useGetClientReportsQuery,
  useGetClientReportQuery,
  useGenerateClientReportMutation,
  useUpdateClientReportDraftMutation,
  useFinalizeClientReportMutation,
  useUnfinalizeClientReportMutation,
  useDeleteClientReportMutation,
  useGetReportSupportingFilesQuery,
  useUploadReportSupportingFileMutation,
  useDeleteReportSupportingFileMutation,
} = clientReportsApi;
