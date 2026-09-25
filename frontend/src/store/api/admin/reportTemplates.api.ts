import { baseApi } from "../baseApi";

export interface ReportTemplate {
  id: number;
  name: string;
  description?: string | null;
  aiInstructions?: string | null;
  originalName?: string | null;
  mimeType?: string | null;
  fileSize?: number | null;
  structureText?: string | null;
  defaultIncludeProfile?: boolean;
  defaultIncludeNotes?: boolean;
  defaultIncludeAssessments?: boolean;
  supportingFilesGuidance?: string | null;
  supportingFilesExpected?: boolean;
  supportingFileTypes?: string[];
  isActive?: boolean;
  createdById?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateReportTemplatePayload {
  name: string;
  description?: string;
  aiInstructions?: string;
  fileContent: string;
  originalName: string;
  mimeType: string;
  defaultIncludeProfile?: boolean;
  defaultIncludeNotes?: boolean;
  defaultIncludeAssessments?: boolean;
  supportingFilesGuidance?: string;
  supportingFilesExpected?: boolean;
  supportingFileTypes?: string[];
}

export interface UpdateReportTemplatePayload {
  id: number;
  name?: string;
  description?: string;
  aiInstructions?: string;
  structureText?: string;
  isActive?: boolean;
  defaultIncludeProfile?: boolean;
  defaultIncludeNotes?: boolean;
  defaultIncludeAssessments?: boolean;
  supportingFilesGuidance?: string;
  supportingFilesExpected?: boolean;
  supportingFileTypes?: string[];
}

const normalizeTemplate = (item: unknown): ReportTemplate => {
  const raw = item as Record<string, unknown>;
  return {
    id: Number(raw.id),
    name: String(raw.name ?? ""),
    description: (raw.description as string) ?? null,
    aiInstructions: (raw.aiInstructions as string) ?? null,
    originalName: (raw.originalName as string) ?? null,
    mimeType: (raw.mimeType as string) ?? null,
    fileSize: raw.fileSize != null ? Number(raw.fileSize) : null,
    structureText: (raw.structureText as string) ?? null,
    defaultIncludeProfile: Boolean(raw.defaultIncludeProfile ?? true),
    defaultIncludeNotes: Boolean(raw.defaultIncludeNotes ?? true),
    defaultIncludeAssessments: Boolean(raw.defaultIncludeAssessments ?? true),
    supportingFilesGuidance: (raw.supportingFilesGuidance as string) ?? null,
    supportingFilesExpected: Boolean(raw.supportingFilesExpected ?? false),
    supportingFileTypes: Array.isArray(raw.supportingFileTypes)
      ? raw.supportingFileTypes.map(String)
      : [],
    isActive: Boolean(raw.isActive ?? true),
    createdById: raw.createdById != null ? Number(raw.createdById) : null,
    createdAt: raw.createdAt as string | undefined,
    updatedAt: raw.updatedAt as string | undefined,
  };
};

export const reportTemplatesApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getReportTemplates: builder.query<ReportTemplate[], boolean | void>({
      query: (includeInactive) => ({
        url: "/api/v1/report-templates",
        params: includeInactive ? { includeInactive: true } : undefined,
      }),
      providesTags: ["ReportTemplates"],
      transformResponse: (response: unknown) => {
        if (!Array.isArray(response)) return [];
        return response.map(normalizeTemplate);
      },
    }),
    getReportTemplateById: builder.query<ReportTemplate, number>({
      query: (id) => `/api/v1/report-templates/${id}`,
      providesTags: (_result, _error, id) => [{ type: "ReportTemplates", id }],
      transformResponse: (response: unknown) => normalizeTemplate(response),
    }),
    createReportTemplate: builder.mutation<ReportTemplate, CreateReportTemplatePayload>({
      query: (body) => ({
        url: "/api/v1/report-templates",
        method: "POST",
        body,
      }),
      invalidatesTags: ["ReportTemplates"],
      transformResponse: (response: unknown) => normalizeTemplate(response),
    }),
    updateReportTemplate: builder.mutation<ReportTemplate, UpdateReportTemplatePayload>({
      query: ({ id, ...body }) => ({
        url: `/api/v1/report-templates/${id}`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        "ReportTemplates",
        { type: "ReportTemplates", id },
      ],
      transformResponse: (response: unknown) => normalizeTemplate(response),
    }),
    deleteReportTemplate: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/report-templates/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["ReportTemplates"],
    }),
  }),
});

export const {
  useGetReportTemplatesQuery,
  useGetReportTemplateByIdQuery,
  useCreateReportTemplateMutation,
  useUpdateReportTemplateMutation,
  useDeleteReportTemplateMutation,
} = reportTemplatesApi;
