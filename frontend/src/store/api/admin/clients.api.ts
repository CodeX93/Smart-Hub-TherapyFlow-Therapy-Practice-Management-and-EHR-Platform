import { baseApi } from "../baseApi";
import type { AddClientFormValues } from "@/types/add-client.type";
import { mapAddClientFormToCreateClientBody } from "./createClientPayload";
import type {
  AdminCreatedClient,
  AdminClientDetails,
  UpdateAdminClientPayload,
  UpdateAdminClientPortalAccessPayload,
  PortalActivationResponse,
  AdminClientSessionSummary,
  AdminClientSession,
  AdminAssessmentSection,
  AdminAssessmentTemplate,
  CreateAssessmentTemplatePayload,
  UpdateAssessmentTemplatePayload,
  AdminAssessmentTemplatesListParams,
  AdminAssessmentTemplatesListResponse,
  AdminClientAssessment,
  AdminAssessmentAnalytics,
  AdminAssessmentAssignmentsListParams,
  AdminAssessmentAssignmentsListResponse,
  AssignClientAssessmentPayload,
  SubmitAssessmentResponsesPayload,
  AdminAssessmentGeneratedReport,
  AdminAssessmentAssignmentResponseItem,
  ClientStageDurationsResponse,
  AdminClientHistoryListResponse,
  AdminClientEmailHistoryResponse,
  AdminClientSmsLogResponse,
  AdminClientNote,
  AdminDuplicateDetectionResponse,
  MarkClientDuplicatePayload,
  UpdateAssessmentAssignmentStatusPayload,
  AdminFormField,
  AdminFormTemplate,
  CreateFormTemplatePayload,
  CreateFormFieldPayload,
  AdminFormTemplatesListParams,
  AdminFormTemplatesListResponse,
  CreateFormAssignmentPayload,
  AdminFormAssignment,
  AdminClientsListParams,
  AdminClientsListResponse,
  AdminClientDocument,
  AdminClientDocumentsListResponse,
} from "./clients.types";
import {
  normalizeAdminClientSmsLogResponse,
  isRecord,
  asNumber,
  asOptionalNumber,
  asOptionalString,
  normalizeAdminClientsResponse,
  normalizeAdminCreatedClient,
  normalizeAdminClientDetails,
  normalizeAdminClientSessionSummary,
  normalizeAdminClientSessions,
  normalizeAdminAssessmentTemplate,
  normalizeAdminAssessmentSections,
  normalizeAdminAssessmentTemplatesResponse,
  normalizeAdminClientAssessment,
  normalizeAdminClientAssessments,
  normalizeAdminAssessmentAssignmentsResponse,
  normalizeAdminAssessmentAnalytics,
  normalizeAdminAssessmentGeneratedReport,
  normalizeClientStageDurationsResponse,
  normalizeAdminClientHistoryListResponse,
  normalizeAdminClientEmailHistoryResponse,
  normalizeAdminClientNote,
  normalizeAdminClientNotes,
  normalizeDuplicateDetectionResponse,
  normalizeAdminFormTemplate,
  normalizeAdminFormTemplatesResponse,
  normalizeAdminFormAssignment,
  normalizeAdminFormAssignments,
  normalizeAdminClientDocument,
  normalizeAdminClientDocumentsResponse,
} from "./clients.normalizers";

export type * from "./clients.types";

export const adminClientsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAdminClients: builder.query<AdminClientsListResponse, AdminClientsListParams>({
      query: (params) => {
        const query = new URLSearchParams();
        query.set("page", String(params.page ?? 1));
        query.set("pageSize", String(params.pageSize ?? 25));
        query.set("sortBy", params.sortBy ?? "createdAt");
        query.set("sortOrder", params.sortOrder ?? "desc");

        if (params.search?.trim()) query.set("search", params.search.trim());
        if (params.status?.trim()) query.set("status", params.status.trim());
        if (params.stage?.trim()) query.set("stage", params.stage.trim());
        if (typeof params.therapistId === "number") {
          query.set("therapistId", String(params.therapistId));
        }
        if (params.clientType?.trim()) query.set("clientType", params.clientType.trim());
        if (typeof params.hasPortalAccess === "boolean") {
          query.set("hasPortalAccess", String(params.hasPortalAccess));
        }
        if (typeof params.hasPendingTasks === "boolean") {
          query.set("hasPendingTasks", String(params.hasPendingTasks));
        }
        if (typeof params.hasNoSessions === "boolean") {
          query.set("hasNoSessions", String(params.hasNoSessions));
        }
        if (typeof params.needsFollowUp === "boolean") {
          query.set("needsFollowUp", String(params.needsFollowUp));
        }
        if (typeof params.unassigned === "boolean") {
          query.set("unassigned", String(params.unassigned));
        }
        if (typeof params.includeUnassigned === "boolean") {
          query.set("includeUnassigned", String(params.includeUnassigned));
        }
        if (typeof params.checklistTemplateId === "number") {
          query.set("checklistTemplateId", String(params.checklistTemplateId));
        }
        if (typeof params.reportTemplateId === "number") {
          query.set("reportTemplateId", String(params.reportTemplateId));
        }

        return `/api/v1/clients?${query.toString()}`;
      },
      transformResponse: (payload) => normalizeAdminClientsResponse(payload),
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((client) => ({
                type: "Clients" as const,
                id: client.id,
              })),
              { type: "Clients" as const, id: "LIST" },
            ]
          : [{ type: "Clients" as const, id: "LIST" }],
    }),
    createAdminClient: builder.mutation<AdminCreatedClient, AddClientFormValues>({
      query: (formValues) => ({
        url: "/api/v1/clients",
        method: "POST",
        body: mapAddClientFormToCreateClientBody(formValues),
      }),
      transformResponse: (payload) => normalizeAdminCreatedClient(payload),
      invalidatesTags: [{ type: "Clients", id: "LIST" }],
    }),
    updateAdminClient: builder.mutation<AdminClientDetails, UpdateAdminClientPayload>({
      query: ({ id, body }) => ({
        url: `/api/v1/clients/${id}`,
        method: "PATCH",
        body,
      }),
      transformResponse: (payload) => normalizeAdminClientDetails(payload),
      async onQueryStarted({ id }, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(
            adminClientsApi.util.updateQueryData("getAdminClientById", id, (draft) => {
              Object.assign(draft, data);
            }),
          );
        } catch {
          /* Keep existing cache on failure */
        }
      },
      // Refresh list only; detail cache is updated above to avoid edit-form flicker.
      invalidatesTags: [{ type: "Clients", id: "LIST" }],
    }),
    deleteAdminClient: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/clients/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "Clients", id },
        { type: "Clients", id: "LIST" },
      ],
    }),
    updateAdminClientPortalAccess: builder.mutation<
      AdminClientDetails,
      UpdateAdminClientPortalAccessPayload
    >({
      query: ({ id, enable, email }) => ({
        url: `/api/v1/clients/${id}/portal-access`,
        method: "PUT",
        body: {
          enable,
          email,
        },
      }),
      transformResponse: (payload) => normalizeAdminClientDetails(payload),
      invalidatesTags: (_result, _error, { id }) => [
        { type: "Clients", id },
        { type: "Clients", id: "LIST" },
      ],
    }),
    closeAdminClientFile: builder.mutation<AdminClientDetails, number>({
      query: (id) => ({
        url: `/api/v1/clients/${id}`,
        method: "PUT",
        body: {
          status: "inactive",
        },
      }),
      transformResponse: (payload) => normalizeAdminClientDetails(payload),
      invalidatesTags: (_result, _error, id) => [
        { type: "Clients", id },
        { type: "Clients", id: "LIST" },
      ],
    }),
    activateAdminClientFile: builder.mutation<AdminClientDetails, number>({
      query: (id) => ({
        url: `/api/v1/clients/${id}`,
        method: "PUT",
        body: {
          status: "active",
        },
      }),
      transformResponse: (payload) => normalizeAdminClientDetails(payload),
      invalidatesTags: (_result, _error, id) => [
        { type: "Clients", id },
        { type: "Clients", id: "LIST" },
      ],
    }),
    bulkUpdateClientStatus: builder.mutation<
      { updatedCount?: number },
      { clientIds: number[]; status: string }
    >({
      query: (body) => ({
        url: "/api/v1/clients/bulk-update-status",
        method: "POST",
        body,
      }),
      invalidatesTags: [{ type: "Clients", id: "LIST" }],
    }),
    sendClientPortalActivationEmail: builder.mutation<PortalActivationResponse, number>({
      query: (id) => ({
        url: `/api/v1/clients/${id}/send-portal-activation`,
        method: "POST",
      }),
    }),
    getAdminClientById: builder.query<AdminClientDetails, number>({
      query: (id) => `/api/v1/clients/${id}`,
      transformResponse: (payload) => normalizeAdminClientDetails(payload),
      providesTags: (_result, _error, id) => [{ type: "Clients", id }],
    }),
    getClientStageDurations: builder.query<ClientStageDurationsResponse, number>({
      query: (id) => `/api/v1/clients/${id}/stage-durations`,
      transformResponse: (payload) => normalizeClientStageDurationsResponse(payload),
    }),
    getClientHistory: builder.query<
      AdminClientHistoryListResponse,
      { id: number; page?: number; pageSize?: number }
    >({
      query: ({ id, page = 1, pageSize = 20 }) => {
        const params = new URLSearchParams();
        params.set("page", String(Math.max(1, page)));
        params.set("pageSize", String(Math.max(1, pageSize)));
        return `/api/v1/clients/${id}/history?${params.toString()}`;
      },
      transformResponse: (payload, _meta, args) =>
        normalizeAdminClientHistoryListResponse(
          payload,
          Math.max(1, args?.page ?? 1),
          Math.max(1, args?.pageSize ?? 20),
        ),
    }),
    getClientEmailHistory: builder.query<
      AdminClientEmailHistoryResponse,
      { id: number; page?: number; pageSize?: number }
    >({
      query: ({ id, page = 1, pageSize = 20 }) => {
        const params = new URLSearchParams();
        params.set("page", String(Math.max(1, page)));
        params.set("pageSize", String(Math.max(1, pageSize)));
        return `/api/v1/clients/${id}/email-history?${params.toString()}`;
      },
      transformResponse: (payload) => normalizeAdminClientEmailHistoryResponse(payload),
    }),
    getClientSmsLog: builder.query<
      AdminClientSmsLogResponse,
      { id: number; page?: number; pageSize?: number; from?: string; to?: string }
    >({
      query: ({ id, page = 1, pageSize = 25, from, to }) => {
        const params = new URLSearchParams();
        params.set("page", String(Math.max(1, page)));
        params.set("pageSize", String(Math.max(1, pageSize)));
        if (from) params.set("from", from);
        if (to) params.set("to", to);
        return `/api/v1/clients/${id}/sms-log?${params.toString()}`;
      },
      transformResponse: (payload, _meta, args) =>
        normalizeAdminClientSmsLogResponse(
          payload,
          Math.max(1, args?.page ?? 1),
          Math.max(1, args?.pageSize ?? 25),
        ),
    }),
    exportClientSmsLog: builder.query<
      string,
      { id: number; from?: string; to?: string }
    >({
      query: ({ id, from, to }) => {
        const params = new URLSearchParams();
        if (from) params.set("from", from);
        if (to) params.set("to", to);
        return {
          url: `/api/v1/clients/${id}/sms-log/export${params.toString() ? `?${params.toString()}` : ""}`,
          responseHandler: (response) => response.text(),
        };
      },
    }),
    getClientNotes: builder.query<
      AdminClientNote[],
      { clientId: number; noteType?: string; startDate?: string; endDate?: string }
    >({
      query: ({ clientId, noteType, startDate, endDate }) => {
        const params = new URLSearchParams();
        if (noteType?.trim()) params.set("noteType", noteType.trim().toLowerCase());
        if (startDate) params.set("startDate", startDate);
        if (endDate) params.set("endDate", endDate);
        return `/api/v1/notes/clients/${clientId}/notes${params.toString() ? `?${params.toString()}` : ""}`;
      },
      transformResponse: (payload) => normalizeAdminClientNotes(payload),
    }),
    getClientNoteById: builder.query<AdminClientNote, number>({
      query: (id) => `/api/v1/notes/${id}`,
      transformResponse: (payload) =>
        normalizeAdminClientNote(payload) ?? {
          id: 0,
          clientId: 0,
          content: "",
        },
    }),
    createClientNote: builder.mutation<
      AdminClientNote,
      {
        clientId: number;
        title?: string;
        content: string;
        noteType?: string;
        eventDate: string;
        isPrivate?: boolean;
      }
    >({
      query: (body) => ({
        url: "/api/v1/notes",
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminClientNote(payload) ?? {
          id: 0,
          clientId: 0,
          content: "",
        },
    }),
    updateClientNote: builder.mutation<
      AdminClientNote,
      {
        id: number;
        body: {
          title?: string;
          content: string;
          noteType?: string;
          eventDate?: string;
          isPrivate?: boolean;
        };
      }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/notes/${id}`,
        method: "PATCH",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminClientNote(payload) ?? {
          id: 0,
          clientId: 0,
          content: "",
        },
    }),
    deleteClientNote: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/notes/${id}`,
        method: "DELETE",
      }),
    }),
    getDuplicateClients: builder.query<AdminDuplicateDetectionResponse, void>({
      query: () => "/api/v1/clients/duplicates",
      transformResponse: (payload) => normalizeDuplicateDetectionResponse(payload),
    }),
    markClientAsDuplicate: builder.mutation<void, MarkClientDuplicatePayload>({
      query: ({ id, duplicateOfClientId }) => ({
        url: `/api/v1/clients/${id}/mark-duplicate`,
        method: "POST",
        body: { duplicateOfClientId },
      }),
    }),
    getAdminClientSessionSummary: builder.query<AdminClientSessionSummary, number>({
      query: (id) => `/api/v1/clients/${id}/sessions/summary`,
      transformResponse: (payload) => normalizeAdminClientSessionSummary(payload),
    }),
    getAdminClientSessions: builder.query<AdminClientSession[], number>({
      query: (id) => `/api/v1/clients/${id}/sessions`,
      transformResponse: (payload) => normalizeAdminClientSessions(payload),
    }),
    getAssessmentTemplates: builder.query<
      AdminAssessmentTemplatesListResponse,
      AdminAssessmentTemplatesListParams | void
    >({
      query: (params) => {
        const page = Math.max(1, params?.page ?? 1);
        const pageSize = Math.max(1, params?.pageSize ?? 20);
        const search = new URLSearchParams();
        search.set("page", String(page));
        search.set("pageSize", String(pageSize));
        return `/api/v1/assessments/templates?${search.toString()}`;
      },
      transformResponse: (payload, _meta, args) =>
        normalizeAdminAssessmentTemplatesResponse(payload, args ?? {}),
      providesTags: (result) => {
        const listTag = [{ type: "AssessmentTemplates" as const, id: "LIST" }];
        if (!result) return listTag;
        return [
          ...listTag,
          ...result.items.map((item) => ({ type: "AssessmentTemplates" as const, id: item.id })),
        ];
      },
    }),
    createAssessmentTemplate: builder.mutation<
      AdminAssessmentTemplate,
      CreateAssessmentTemplatePayload
    >({
      query: (body) => ({
        url: "/api/v1/assessments/templates",
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminAssessmentTemplate(payload) ?? {
          id: 0,
          name: "",
          title: "",
        },
      invalidatesTags: [{ type: "AssessmentTemplates", id: "LIST" }],
    }),
    getAssessmentTemplateById: builder.query<AdminAssessmentTemplate, number>({
      query: (id) => `/api/v1/assessments/templates/${id}`,
      transformResponse: (payload) =>
        normalizeAdminAssessmentTemplate(payload) ?? {
          id: 0,
          name: "",
          title: "",
        },
      providesTags: (_result, _error, id) => [{ type: "AssessmentTemplates", id }],
    }),
    getAssessmentTemplateSections: builder.query<AdminAssessmentSection[], number>({
      query: (id) => `/api/v1/assessments/templates/${id}/sections`,
      transformResponse: (payload) => normalizeAdminAssessmentSections(payload),
    }),
    updateAssessmentTemplate: builder.mutation<
      AdminAssessmentTemplate,
      { id: number; body: UpdateAssessmentTemplatePayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/assessments/templates/${id}`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminAssessmentTemplate(payload) ?? {
          id: 0,
          name: "",
          title: "",
        },
      invalidatesTags: (_result, _error, { id }) => [
        { type: "AssessmentTemplates", id },
        { type: "AssessmentTemplates", id: "LIST" },
      ],
    }),
    deleteAssessmentTemplate: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/assessments/templates/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "AssessmentTemplates", id },
        { type: "AssessmentTemplates", id: "LIST" },
      ],
    }),
    getClientAssessments: builder.query<AdminClientAssessment[], number>({
      query: (clientId) => `/api/v1/assessments/clients/${clientId}/assessments`,
      transformResponse: (payload) => normalizeAdminClientAssessments(payload),
    }),
    getAssessmentAssignments: builder.query<
      AdminAssessmentAssignmentsListResponse,
      AdminAssessmentAssignmentsListParams | void
    >({
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((assignment) => ({
                type: "AssessmentAssignments" as const,
                id: assignment.id,
              })),
              { type: "AssessmentAssignments" as const, id: "LIST" },
            ]
          : [{ type: "AssessmentAssignments" as const, id: "LIST" }],
      query: (params) => {
        const query = new URLSearchParams();
        const page = Math.max(1, params?.page ?? 1);
        const pageSize = Math.max(1, params?.pageSize ?? 20);
        query.set("page", String(page));
        query.set("pageSize", String(pageSize));
        if (params?.search?.trim()) query.set("search", params.search.trim());
        if (params?.templateId) query.set("templateId", String(params.templateId));
        if (params?.clientId) query.set("clientId", String(params.clientId));
        if (params?.status) query.set("status", params.status);
        if (params?.from) query.set("from", params.from);
        if (params?.to) query.set("to", params.to);

        return `/api/v1/assessments/assignments?${query.toString()}`;
      },
      transformResponse: (payload, _meta, args) =>
        normalizeAdminAssessmentAssignmentsResponse(payload, args ?? {}),
    }),
    getAssessmentAnalytics: builder.query<AdminAssessmentAnalytics, { clientId?: number }>({
      query: ({ clientId }) => {
        const params = new URLSearchParams();
        if (typeof clientId === "number" && clientId > 0) {
          params.set("clientId", String(clientId));
        }

        return `/api/v1/assessments/analytics${params.toString() ? `?${params.toString()}` : ""}`;
      },
      transformResponse: (payload) => normalizeAdminAssessmentAnalytics(payload),
    }),
    getAssessmentAssignmentResponses: builder.query<AdminAssessmentAssignmentResponseItem[], number>({
      query: (assignmentId) => `/api/v1/assessments/assignments/${assignmentId}/responses`,
      transformResponse: (payload) => {
        if (!Array.isArray(payload)) return [];
        return payload.map((entry) => {
          const row = isRecord(entry) ? entry : {};
          return {
            id: asNumber(row.id),
            assignmentId: asNumber(row.assignmentId),
            questionId: asNumber(row.questionId),
            questionText: asOptionalString(row.questionText),
            responderType: asOptionalString(row.responderType),
            responderUserId: asOptionalNumber(row.responderUserId),
            responderClientId: asOptionalNumber(row.responderClientId),
            responseText: asOptionalString(row.responseText),
            responseValue: asOptionalString(row.responseValue),
            score: asOptionalNumber(row.score),
            answeredAt: asOptionalString(row.answeredAt),
            selectedOptionIds: Array.isArray(row.selectedOptionIds)
              ? row.selectedOptionIds
                  .map((id) => asNumber(id))
                  .filter((id) => Number.isFinite(id) && id > 0)
              : [],
            ratingValue: asOptionalNumber(row.ratingValue),
          };
        });
      },
    }),
    assignClientAssessment: builder.mutation<
      AdminClientAssessment,
      AssignClientAssessmentPayload
    >({
      query: ({ clientId, templateId, dueDate, notes }) => ({
        url: `/api/v1/assessments/assignments`,
        method: "POST",
        body: {
          templateId,
          clientId,
          dueDate,
          notes,
        },
      }),
      transformResponse: (payload) =>
        normalizeAdminClientAssessment(payload) ?? {
          id: 0,
          templateId: 0,
          clientId: 0,
        },
      invalidatesTags: [{ type: "AssessmentAssignments", id: "LIST" }],
    }),
    submitAssessmentResponses: builder.mutation<
      AdminClientAssessment,
      { id: number; body: SubmitAssessmentResponsesPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/assessments/assignments/${id}/responses`,
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminClientAssessment(payload) ?? {
          id: 0,
          templateId: 0,
          clientId: 0,
        },
      invalidatesTags: [{ type: "AssessmentAssignments", id: "LIST" }],
    }),
    generateAssessmentReport: builder.mutation<AdminAssessmentGeneratedReport, number>({
      query: (assignmentId) => ({
        url: `/api/v1/assessments/assignments/${assignmentId}/generate-report`,
        method: "POST",
      }),
      transformResponse: (payload) => normalizeAdminAssessmentGeneratedReport(payload),
      invalidatesTags: (_result, _error, assignmentId) => [
        { type: "AssessmentReports", id: assignmentId },
      ],
    }),
    getAssessmentReport: builder.query<AdminAssessmentGeneratedReport, number>({
      query: (assignmentId) => `/api/v1/assessments/assignments/${assignmentId}/report`,
      transformResponse: (payload) => normalizeAdminAssessmentGeneratedReport(payload),
      providesTags: (_result, _error, assignmentId) => [
        { type: "AssessmentReports", id: assignmentId },
      ],
    }),
    updateAssessmentReportDraft: builder.mutation<
      AdminAssessmentGeneratedReport,
      { assignmentId: number; draftContent: string }
    >({
      query: ({ assignmentId, draftContent }) => ({
        url: `/api/v1/assessments/assignments/${assignmentId}/report`,
        method: "PUT",
        body: { draftContent },
      }),
      transformResponse: (payload) => normalizeAdminAssessmentGeneratedReport(payload),
      invalidatesTags: (_result, _error, { assignmentId }) => [
        { type: "AssessmentReports", id: assignmentId },
      ],
    }),
    finalizeAssessmentReport: builder.mutation<AdminAssessmentGeneratedReport, number>({
      query: (assignmentId) => ({
        url: `/api/v1/assessments/assignments/${assignmentId}/report/finalize`,
        method: "POST",
      }),
      transformResponse: (payload) => normalizeAdminAssessmentGeneratedReport(payload),
      invalidatesTags: (_result, _error, assignmentId) => [
        { type: "AssessmentReports", id: assignmentId },
        { type: "AssessmentAssignments", id: assignmentId },
        { type: "AssessmentAssignments", id: "LIST" },
      ],
    }),
    updateAssessmentAssignmentStatus: builder.mutation<
      AdminClientAssessment,
      { id: number; body: UpdateAssessmentAssignmentStatusPayload }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/assessments/assignments/${id}/status`,
        method: "PUT",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminClientAssessment(payload) ?? {
          id: 0,
          templateId: 0,
          clientId: 0,
        },
      invalidatesTags: (_result, _error, { id }) => [
        { type: "AssessmentAssignments", id },
        { type: "AssessmentAssignments", id: "LIST" },
      ],
    }),
    deleteClientAssessment: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/assessments/assignments/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "AssessmentAssignments", id },
        { type: "AssessmentAssignments", id: "LIST" },
      ],
    }),
    getFormTemplates: builder.query<
      AdminFormTemplatesListResponse,
      AdminFormTemplatesListParams | void
    >({
      query: (params) => {
        const query = new URLSearchParams();
        const page = Math.max(1, params?.page ?? 1);
        const pageSize = Math.max(1, params?.pageSize ?? 20);
        query.set("page", String(page));
        query.set("pageSize", String(pageSize));
        if (params?.search?.trim()) query.set("search", params.search.trim());
        if (params?.category?.trim()) query.set("category", params.category.trim());
        return `/api/v1/forms/templates?${query.toString()}`;
      },
      transformResponse: (payload, _meta, args) =>
        normalizeAdminFormTemplatesResponse(payload, args ?? {}),
      providesTags: (result) =>
        result
          ? [
              ...result.items.map((form) => ({
                type: "FormTemplates" as const,
                id: form.id,
              })),
              { type: "FormTemplates" as const, id: "LIST" },
            ]
          : [{ type: "FormTemplates" as const, id: "LIST" }],
    }),
    createFormTemplate: builder.mutation<AdminFormTemplate, CreateFormTemplatePayload>({
      query: (body) => ({
        url: "/api/v1/forms/templates",
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminFormTemplate(payload) ?? {
          id: 0,
          name: "",
          category: "",
          requiresSignature: false,
          isActive: true,
        },
      invalidatesTags: [{ type: "FormTemplates", id: "LIST" }],
    }),
    getFormTemplateById: builder.query<AdminFormTemplate, number>({
      query: (id) => `/api/v1/forms/templates/${id}`,
      transformResponse: (payload) =>
        normalizeAdminFormTemplate(payload) ?? {
          id: 0,
          name: "",
          category: "",
          requiresSignature: false,
          isActive: true,
        },
      providesTags: (_result, _error, id) => [{ type: "FormTemplates", id }],
    }),
    updateFormTemplate: builder.mutation<
      AdminFormTemplate,
      { id: number; body: Partial<CreateFormTemplatePayload> }
    >({
      query: ({ id, body }) => ({
        url: `/api/v1/forms/templates/${id}`,
        method: "PATCH",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminFormTemplate(payload) ?? {
          id: 0,
          name: "",
          category: "",
          requiresSignature: false,
          isActive: true,
        },
      invalidatesTags: (_result, _error, { id }) => [
        { type: "FormTemplates", id },
        { type: "FormTemplates", id: "LIST" },
      ],
    }),
    deleteFormTemplate: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/forms/templates/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: "FormTemplates", id },
        { type: "FormTemplates", id: "LIST" },
      ],
    }),
    createFormAssignment: builder.mutation<AdminFormAssignment, CreateFormAssignmentPayload>({
      query: (body) => ({
        url: "/api/v1/forms/assignments",
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizeAdminFormAssignment(payload) ?? {
          id: 0,
          templateId: 0,
          clientId: 0,
        },
    }),
    getFormAssignments: builder.query<
      AdminFormAssignment[],
      { clientId?: number; templateId?: number } | void
    >({
      query: (params) => {
        const query = new URLSearchParams();
        if (params?.clientId) query.set("clientId", String(params.clientId));
        if (params?.templateId) query.set("templateId", String(params.templateId));
        return `/api/v1/forms/assignments${query.toString() ? `?${query.toString()}` : ""}`;
      },
      transformResponse: (payload) => normalizeAdminFormAssignments(payload),
    }),
    getClientDocuments: builder.query<
      AdminClientDocumentsListResponse,
      {
        clientId: number;
        page?: number;
        pageSize?: number;
        documentType?: string;
        category?: string;
        reviewStatus?: string;
        shareWithClient?: boolean;
        search?: string;
      }
    >({
      query: ({ clientId, ...params }) => {
        const query = new URLSearchParams();
        query.set("page", String(params.page ?? 1));
        query.set("pageSize", String(params.pageSize ?? 25));
        if (params.documentType?.trim()) query.set("documentType", params.documentType.trim());
        if (params.category?.trim()) query.set("category", params.category.trim());
        if (params.reviewStatus?.trim()) query.set("reviewStatus", params.reviewStatus.trim());
        if (typeof params.shareWithClient === "boolean") {
          query.set("shareWithClient", String(params.shareWithClient));
        }
        if (params.search?.trim()) query.set("search", params.search.trim());
        const suffix = query.toString();
        return `/api/v1/clients/${clientId}/documents${suffix ? `?${suffix}` : ""}`;
      },
      transformResponse: (payload, _meta, arg) =>
        normalizeAdminClientDocumentsResponse(payload, {
          page: arg.page,
          pageSize: arg.pageSize,
        }),
      providesTags: (_result, _error, arg) => [
        { type: "ClientDocuments", id: `LIST-${arg.clientId}` },
      ],
    }),
    getClientDocumentPreview: builder.query<
      AdminClientDocument,
      { clientId: number; id: number }
    >({
      query: ({ clientId, id }) => `/api/v1/clients/${clientId}/documents/${id}/preview`,
      transformResponse: (payload) =>
        normalizeAdminClientDocument(payload) ?? {
          id: 0,
          clientId: 0,
        },
    }),
    getClientDocumentViewer: builder.query<
      Blob | string,
      { clientId: number; id: number }
    >({
      query: ({ clientId, id }) => ({
        url: `/api/v1/clients/${clientId}/documents/${id}/viewer`,
        responseHandler: async (response) => {
          const contentType = response.headers.get("content-type") || "";
          if (contentType.includes("application/json")) {
            return response.text();
          }
          return response.blob();
        },
      }),
    }),
    downloadClientDocument: builder.query<
      Blob | string,
      { clientId: number; id: number }
    >({
      query: ({ clientId, id }) => ({
        url: `/api/v1/clients/${clientId}/documents/${id}/download`,
        responseHandler: async (response) => {
          const contentType = response.headers.get("content-type") || "";
          if (contentType.includes("application/json")) {
            return response.text();
          }
          return response.blob();
        },
      }),
    }),
    shareClientDocument: builder.mutation<
      { success?: boolean; message?: string },
      { clientId: number; id: number; shareWithClient: boolean }
    >({
      query: ({ clientId, id, shareWithClient }) => ({
        url: `/api/v1/clients/${clientId}/documents/${id}/share`,
        method: "PATCH",
        body: { shareWithClient },
      }),
      invalidatesTags: (_result, _error, arg) => [
        { type: "ClientDocuments", id: `LIST-${arg.clientId}` },
      ],
    }),
    deleteClientDocument: builder.mutation<
      { success?: boolean; message?: string },
      { clientId: number; id: number }
    >({
      query: ({ clientId, id }) => ({
        url: `/api/v1/clients/${clientId}/documents/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, arg) => [
        { type: "ClientDocuments", id: `LIST-${arg.clientId}` },
      ],
    }),
    uploadClientDocument: builder.mutation<
      AdminClientDocument,
      {
        clientId: number;
        file: File;
        documentType?: string;
        category?: string;
        description?: string;
        needsReview?: boolean;
        shareWithClient?: boolean;
      }
    >({
      query: ({
        clientId,
        file,
        documentType,
        category,
        description,
        needsReview,
        shareWithClient,
      }) => {
        const formData = new FormData();
        formData.append("file", file);
        const params = new URLSearchParams();
        if (documentType?.trim()) params.set("documentType", documentType.trim());
        if (category?.trim()) params.set("category", category.trim());
        if (description?.trim()) params.set("description", description.trim());
        if (typeof needsReview === "boolean") params.set("needsReview", String(needsReview));
        if (typeof shareWithClient === "boolean") {
          params.set("shareWithClient", String(shareWithClient));
        }
        const suffix = params.toString();
        return {
          url: `/api/v1/clients/${clientId}/documents${suffix ? `?${suffix}` : ""}`,
          method: "POST",
          body: formData,
        };
      },
      transformResponse: (payload) =>
        normalizeAdminClientDocument(payload) ?? {
          id: 0,
          clientId: 0,
        },
      invalidatesTags: (_result, _error, arg) => [
        { type: "ClientDocuments", id: `LIST-${arg.clientId}` },
      ],
    }),
    getFormAssignmentById: builder.query<AdminFormAssignment, number>({
      query: (id) => `/api/v1/forms/assignments/${id}`,
      transformResponse: (payload) =>
        normalizeAdminFormAssignment(payload) ?? {
          id: 0,
          templateId: 0,
          clientId: 0,
        },
    }),
    deleteFormAssignment: builder.mutation<void, number>({
      query: (id) => ({
        url: `/api/v1/forms/assignments/${id}`,
        method: "DELETE",
      }),
    }),
    createFormField: builder.mutation<AdminFormField, CreateFormFieldPayload>({
      query: (body) => ({
        url: "/api/v1/forms/fields",
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { templateId }) => [
        { type: "FormTemplates", id: templateId },
        { type: "FormTemplates", id: "LIST" },
      ],
    }),
    updateFormField: builder.mutation<AdminFormField, { id: number; body: CreateFormFieldPayload }>({
      query: ({ id, body }) => ({
        url: `/api/v1/forms/fields/${id}`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: (_result, _error, { body }) => [
        { type: "FormTemplates", id: body.templateId },
        { type: "FormTemplates", id: "LIST" },
      ],
    }),
    deleteFormField: builder.mutation<void, { id: number; templateId?: number }>({
      query: ({ id }) => ({
        url: `/api/v1/forms/fields/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: (_result, _error, { templateId }) =>
        templateId
          ? [
              { type: "FormTemplates", id: templateId },
              { type: "FormTemplates", id: "LIST" },
            ]
          : [{ type: "FormTemplates", id: "LIST" }],
    }),
  }),
});

export const {
  useGetAdminClientsQuery,
  useLazyGetAdminClientsQuery,
  useCreateAdminClientMutation,
  useUpdateAdminClientMutation,
  useDeleteAdminClientMutation,
  useUpdateAdminClientPortalAccessMutation,
  useCloseAdminClientFileMutation,
  useActivateAdminClientFileMutation,
  useBulkUpdateClientStatusMutation,
  useSendClientPortalActivationEmailMutation,
  useGetAdminClientByIdQuery,
  useGetClientStageDurationsQuery,
  useGetClientHistoryQuery,
  useGetClientEmailHistoryQuery,
  useGetClientSmsLogQuery,
  useLazyExportClientSmsLogQuery,
  useGetClientNotesQuery,
  useLazyGetClientNoteByIdQuery,
  useCreateClientNoteMutation,
  useUpdateClientNoteMutation,
  useDeleteClientNoteMutation,
  useGetDuplicateClientsQuery,
  useMarkClientAsDuplicateMutation,
  useGetAdminClientSessionSummaryQuery,
  useGetAdminClientSessionsQuery,
  useGetAssessmentTemplatesQuery,
  useCreateAssessmentTemplateMutation,
  useLazyGetAssessmentTemplateByIdQuery,
  useGetAssessmentTemplateSectionsQuery,
  useUpdateAssessmentTemplateMutation,
  useDeleteAssessmentTemplateMutation,
  useGetClientAssessmentsQuery,
  useGetAssessmentAssignmentsQuery,
  useGetAssessmentAnalyticsQuery,
  useGetAssessmentAssignmentResponsesQuery,
  useAssignClientAssessmentMutation,
  useSubmitAssessmentResponsesMutation,
  useGenerateAssessmentReportMutation,
  useGetAssessmentReportQuery,
  useLazyGetAssessmentReportQuery,
  useUpdateAssessmentReportDraftMutation,
  useFinalizeAssessmentReportMutation,
  useUpdateAssessmentAssignmentStatusMutation,
  useDeleteClientAssessmentMutation,
  useGetFormTemplatesQuery,
  useCreateFormTemplateMutation,
  useGetFormTemplateByIdQuery,
  useLazyGetFormTemplateByIdQuery,
  useUpdateFormTemplateMutation,
  useDeleteFormTemplateMutation,
  useCreateFormAssignmentMutation,
  useGetFormAssignmentsQuery,
  useGetClientDocumentsQuery,
  useLazyGetClientDocumentPreviewQuery,
  useLazyGetClientDocumentViewerQuery,
  useLazyDownloadClientDocumentQuery,
  useDeleteClientDocumentMutation,
  useUploadClientDocumentMutation,
  useShareClientDocumentMutation,
  useLazyGetFormAssignmentByIdQuery,
  useDeleteFormAssignmentMutation,
  useCreateFormFieldMutation,
  useUpdateFormFieldMutation,
  useDeleteFormFieldMutation,
} = adminClientsApi;
