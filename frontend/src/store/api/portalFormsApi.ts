import { baseApi } from "./baseApi";

export interface PortalFormAssignment {
  id: number;
  templateId: number;
  templateName: string;
  templateCategory: string;
  status: string;
  assignedAt: string | null;
  dueDate: string | null;
  completedAt: string | null;
  submittedAt: string | null;
  instructions: string | null;
  requiresSignature: boolean;
}

export interface PortalFormAssignmentField {
  id: number;
  fieldId: number | null;
  fieldType: string;
  label: string;
  placeholder: string | null;
  helpText: string | null;
  isRequired: boolean;
  options: string | null;
  sortOrder: number;
  sectionTitle: string | null;
}

export interface PortalFormPersonData {
  fullName: string | null;
  clientId?: string | null;
  email: string | null;
  phone: string | null;
  dateOfBirth?: string | null;
}

export interface PortalFormPracticeData {
  name: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  website: string | null;
}

export interface PortalFormContextData {
  clientData: PortalFormPersonData | null;
  therapistData: PortalFormPersonData | null;
  practiceData: PortalFormPracticeData | null;
}

export interface PortalFormAssignmentDetail extends PortalFormAssignment {
  templateDescription: string | null;
  templateInstructions: string | null;
  fields: PortalFormAssignmentField[];
  context: PortalFormContextData;
}

export interface PortalFormResponse {
  id: number;
  assignmentId: number;
  assignmentFieldId: number;
  value: string | null;
}

export interface PortalFormSignature {
  assignmentId: number;
  signatureData: string | null;
  signerName: string | null;
  signedAt: string | null;
}

export interface PortalFormSaveResponsePayload {
  assignmentId: number;
  assignmentFieldId: number;
  value?: string;
}

export interface PortalFormSaveSignaturePayload {
  assignmentId: number;
  signatureData: string;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) return parsed;
  }
  return 0;
}

function asNullableNumber(value: unknown): number | null {
  if (value === null || value === undefined) return null;
  return asNumber(value);
}

function asNullableString(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed || null;
}

function asBoolean(value: unknown): boolean {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    return normalized === "true" || normalized === "1" || normalized === "yes";
  }
  return Boolean(value);
}

function normalizePortalFormAssignmentField(
  entry: unknown,
): PortalFormAssignmentField | null {
  if (!isRecord(entry)) return null;

  const id = asNumber(entry.assignmentFieldId ?? entry.id);
  if (!id) return null;

  return {
    id,
    fieldId: asNullableNumber(entry.fieldId ?? entry.templateFieldId),
    fieldType: asNullableString(entry.fieldType) ?? "text",
    label: asNullableString(entry.label) ?? "Field",
    placeholder: asNullableString(entry.placeholder),
    helpText: asNullableString(entry.helpText),
    isRequired: asBoolean(entry.isRequired),
    options: asNullableString(entry.options),
    sortOrder: asNumber(entry.sortOrder),
    sectionTitle: asNullableString(entry.sectionTitle),
  };
}

function normalizePortalFormAssignment(
  payload: unknown,
): PortalFormAssignment | null {
  if (!isRecord(payload)) return null;

  const id = asNumber(payload.id);
  if (!id) return null;

  return {
    id,
    templateId: asNumber(payload.templateId),
    templateName:
      asNullableString(payload.templateName) ??
      asNullableString(payload.formName) ??
      asNullableString(payload.name) ??
      "Clinical Form",
    templateCategory:
      asNullableString(payload.templateCategory) ??
      asNullableString(payload.category) ??
      "Form",
    status: asNullableString(payload.status) ?? "pending",
    assignedAt:
      asNullableString(payload.assignedAt) ??
      asNullableString(payload.createdAt),
    dueDate: asNullableString(payload.dueDate),
    completedAt:
      asNullableString(payload.completedAt) ??
      asNullableString(payload.submittedAt),
    submittedAt: asNullableString(payload.submittedAt),
    instructions: asNullableString(payload.instructions),
    requiresSignature: asBoolean(payload.requiresSignature),
  };
}

function extractAssignmentRoot(payload: unknown): Record<string, unknown> {
  if (!isRecord(payload)) return {};

  if (isRecord(payload.assignment)) {
    return payload.assignment;
  }

  return payload;
}

function extractAssignmentFields(payload: unknown): unknown[] {
  if (!isRecord(payload)) return [];

  const directFields = [
    payload.fields,
    payload.assignmentFields,
    payload.templateFields,
  ];

  for (const candidate of directFields) {
    if (Array.isArray(candidate)) return candidate;
  }

  if (isRecord(payload.template) && Array.isArray(payload.template.fields)) {
    return payload.template.fields;
  }

  if (
    isRecord(payload.activeVersion) &&
    Array.isArray(payload.activeVersion.fields)
  ) {
    return payload.activeVersion.fields;
  }

  if (isRecord(payload.assignment) && Array.isArray(payload.assignment.fields)) {
    return payload.assignment.fields;
  }

  return [];
}

function normalizePortalFormPersonData(
  entry: unknown,
): PortalFormPersonData | null {
  if (!isRecord(entry)) return null;
  return {
    fullName: asNullableString(entry.fullName),
    clientId: asNullableString(entry.clientId),
    email: asNullableString(entry.email),
    phone: asNullableString(entry.phone),
    dateOfBirth: asNullableString(entry.dateOfBirth),
  };
}

function normalizePortalFormPracticeData(
  entry: unknown,
): PortalFormPracticeData | null {
  if (!isRecord(entry)) return null;
  return {
    name: asNullableString(entry.name),
    address: asNullableString(entry.address),
    phone: asNullableString(entry.phone),
    email: asNullableString(entry.email),
    website: asNullableString(entry.website),
  };
}

function normalizePortalFormContext(payload: unknown): PortalFormContextData {
  const root = isRecord(payload) ? payload : {};
  return {
    clientData: normalizePortalFormPersonData(root.clientData),
    therapistData: normalizePortalFormPersonData(root.therapistData),
    practiceData: normalizePortalFormPracticeData(root.practiceData),
  };
}

function normalizePortalFormAssignmentDetail(
  payload: unknown,
): PortalFormAssignmentDetail | null {
  const root = isRecord(payload) ? payload : {};
  const assignmentRoot = extractAssignmentRoot(payload);
  const assignment = normalizePortalFormAssignment({
    ...assignmentRoot,
    templateName:
      assignmentRoot.templateName ??
      (isRecord(root.template) ? root.template.name : undefined),
    templateCategory:
      assignmentRoot.templateCategory ??
      (isRecord(root.template) ? root.template.category : undefined),
    requiresSignature:
      assignmentRoot.requiresSignature ??
      (isRecord(root.template) ? root.template.requiresSignature : undefined),
    instructions:
      assignmentRoot.instructions ??
      (isRecord(root.template) ? root.template.instructions : undefined),
  });

  if (!assignment) return null;

  const template = isRecord(root.template) ? root.template : root;

  return {
    ...assignment,
    templateDescription: asNullableString(template.description),
    templateInstructions:
      asNullableString(template.instructions) ?? assignment.instructions,
    fields: extractAssignmentFields(payload)
      .map((entry) => normalizePortalFormAssignmentField(entry))
      .filter((entry): entry is PortalFormAssignmentField => entry !== null)
      .sort((left, right) => left.sortOrder - right.sortOrder),
    context: normalizePortalFormContext(payload),
  };
}

function normalizePortalFormAssignments(payload: unknown): PortalFormAssignment[] {
  if (Array.isArray(payload)) {
    return payload
      .map((entry) => normalizePortalFormAssignment(entry))
      .filter((entry): entry is PortalFormAssignment => entry !== null);
  }

  if (isRecord(payload) && Array.isArray(payload.items)) {
    return payload.items
      .map((entry) => normalizePortalFormAssignment(entry))
      .filter((entry): entry is PortalFormAssignment => entry !== null);
  }

  const single = normalizePortalFormAssignment(payload);
  return single ? [single] : [];
}

function normalizePortalFormResponse(entry: unknown): PortalFormResponse | null {
  if (!isRecord(entry)) return null;

  const assignmentFieldId = asNumber(
    entry.assignmentFieldId ?? entry.fieldId,
  );
  const assignmentId = asNumber(entry.assignmentId);

  if (!assignmentFieldId || !assignmentId) return null;

  return {
    id: asNumber(entry.id),
    assignmentId,
    assignmentFieldId,
    value: asNullableString(entry.value),
  };
}

function normalizePortalFormResponses(payload: unknown): PortalFormResponse[] {
  const rows = Array.isArray(payload) ? payload : [];
  return rows
    .map((entry) => normalizePortalFormResponse(entry))
    .filter((entry): entry is PortalFormResponse => entry !== null);
}

function normalizePortalFormSignature(payload: unknown): PortalFormSignature {
  const root = isRecord(payload) ? payload : {};

  return {
    assignmentId: asNumber(root.assignmentId),
    signatureData:
      asNullableString(root.signatureData) ??
      asNullableString(root.signature),
    signerName: asNullableString(root.signerName),
    signedAt: asNullableString(root.signedAt),
  };
}

export const portalFormsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getPortalFormAssignments: builder.query<PortalFormAssignment[], void>({
      query: () => "/api/v1/portal/forms/assignments",
      transformResponse: (payload) => normalizePortalFormAssignments(payload),
      providesTags: ["PortalForms"],
    }),
    getPortalFormAssignmentById: builder.query<
      PortalFormAssignmentDetail,
      number
    >({
      query: (assignmentId) =>
        `/api/v1/portal/forms/assignments/${assignmentId}`,
      transformResponse: (payload) =>
        normalizePortalFormAssignmentDetail(payload) ?? {
          id: 0,
          templateId: 0,
          templateName: "Clinical Form",
          templateCategory: "Form",
          status: "pending",
          assignedAt: null,
          dueDate: null,
          completedAt: null,
          submittedAt: null,
          instructions: null,
          requiresSignature: false,
          templateDescription: null,
          templateInstructions: null,
          fields: [],
          context: {
            clientData: null,
            therapistData: null,
            practiceData: null,
          },
        },
      providesTags: (_result, _error, assignmentId) => [
        { type: "PortalForms", id: assignmentId },
      ],
    }),
    getPortalFormResponses: builder.query<PortalFormResponse[], number>({
      query: (assignmentId) =>
        `/api/v1/portal/forms/responses/${assignmentId}`,
      transformResponse: (payload) => normalizePortalFormResponses(payload),
      providesTags: (_result, _error, assignmentId) => [
        { type: "PortalForms", id: `responses-${assignmentId}` },
      ],
    }),
    getPortalFormSignature: builder.query<PortalFormSignature, number>({
      query: (assignmentId) =>
        `/api/v1/portal/forms/signature/${assignmentId}`,
      transformResponse: (payload) => normalizePortalFormSignature(payload),
      providesTags: (_result, _error, assignmentId) => [
        { type: "PortalForms", id: `signature-${assignmentId}` },
      ],
    }),
    savePortalFormResponse: builder.mutation<
      PortalFormResponse,
      PortalFormSaveResponsePayload
    >({
      query: (body) => ({
        url: "/api/v1/portal/forms/responses",
        method: "POST",
        body,
      }),
      transformResponse: (payload) =>
        normalizePortalFormResponse(payload) ?? {
          id: 0,
          assignmentId: 0,
          assignmentFieldId: 0,
          value: null,
        },
      invalidatesTags: (_result, _error, arg) => [
        { type: "PortalForms", id: arg.assignmentId },
        { type: "PortalForms", id: `responses-${arg.assignmentId}` },
      ],
    }),
    savePortalFormSignature: builder.mutation<
      PortalFormSignature,
      PortalFormSaveSignaturePayload
    >({
      query: (body) => ({
        url: "/api/v1/portal/forms/signature",
        method: "POST",
        body,
      }),
      transformResponse: (payload) => normalizePortalFormSignature(payload),
      invalidatesTags: (_result, _error, arg) => [
        { type: "PortalForms", id: arg.assignmentId },
        { type: "PortalForms", id: `signature-${arg.assignmentId}` },
        "PortalForms",
      ],
    }),
    submitPortalFormAssignment: builder.mutation<PortalFormAssignment, number>({
      query: (assignmentId) => ({
        url: `/api/v1/portal/forms/submit/${assignmentId}`,
        method: "POST",
      }),
      transformResponse: (payload) =>
        normalizePortalFormAssignment(payload) ?? {
          id: 0,
          templateId: 0,
          templateName: "Clinical Form",
          templateCategory: "Form",
          status: "completed",
          assignedAt: null,
          dueDate: null,
          completedAt: null,
          submittedAt: null,
          instructions: null,
          requiresSignature: false,
        },
      invalidatesTags: (_result, _error, assignmentId) => [
        { type: "PortalForms", id: assignmentId },
        "PortalForms",
      ],
    }),
  }),
});

export const {
  useGetPortalFormAssignmentsQuery,
  useGetPortalFormAssignmentByIdQuery,
  useGetPortalFormResponsesQuery,
  useGetPortalFormSignatureQuery,
  useSavePortalFormResponseMutation,
  useSavePortalFormSignatureMutation,
  useSubmitPortalFormAssignmentMutation,
} = portalFormsApi;
