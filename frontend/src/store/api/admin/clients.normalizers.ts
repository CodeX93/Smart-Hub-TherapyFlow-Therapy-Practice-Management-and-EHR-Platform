import type {
  AdminClientSummary,
  AdminCreatedClient,
  AdminClientDetails,
  AdminClientListSummary,
  AdminClientSessionSummary,
  AdminClientSession,
  AdminAssessmentSection,
  AdminAssessmentTemplate,
  AdminAssessmentTemplatesListParams,
  AdminAssessmentTemplatesListResponse,
  AdminClientAssessment,
  AdminAssessmentAnalytics,
  AdminAssessmentAssignmentsListParams,
  AdminAssessmentAssignmentsListResponse,
  AdminAssessmentGeneratedReport,
  ClientStageDurationsResponse,
  AdminClientHistoryEvent,
  AdminClientHistoryListResponse,
  AdminClientEmailHistoryResponse,
  AdminClientSmsLogItem,
  AdminClientSmsLogResponse,
  AdminClientNote,
  AdminDuplicateRecommendation,
  AdminDuplicateGroup,
  AdminDuplicateDetectionResponse,
  AdminFormTemplate,
  AdminFormTemplatesListParams,
  AdminFormTemplatesListResponse,
  AdminFormAssignment,
  AdminClientsListResponse,
  AdminClientDocument,
  AdminClientDocumentsListResponse,
} from "./clients.types";

export function normalizeAdminClientSmsLogResponse(
  payload: unknown,
  page: number,
  pageSize: number,
): AdminClientSmsLogResponse {
  if (!payload || typeof payload !== "object") {
    return {
      items: [],
      totalCount: 0,
      page,
      pageSize,
      totalPages: 0,
    };
  }

  const record = payload as Record<string, unknown>;
  const rawItems = Array.isArray(record.items) ? record.items : [];

  const items = rawItems.reduce<AdminClientSmsLogItem[]>((accumulator, entry) => {
    if (!entry || typeof entry !== "object") return accumulator;
    const row = entry as Record<string, unknown>;

    accumulator.push({
      id: Number(row.id ?? 0),
      action: typeof row.action === "string" ? row.action : "",
      result: typeof row.result === "string" ? row.result : "",
      resourceId:
        typeof row.resourceId === "string"
          ? row.resourceId
          : row.resourceId != null
            ? String(row.resourceId)
            : undefined,
      timestamp: typeof row.timestamp === "string" ? row.timestamp : "",
      details: typeof row.details === "string" ? row.details : null,
    });

    return accumulator;
  }, []);

  return {
    items,
    totalCount: typeof record.totalCount === "number" ? record.totalCount : items.length,
    page: typeof record.page === "number" ? record.page : page,
    pageSize: typeof record.pageSize === "number" ? record.pageSize : pageSize,
    totalPages:
      typeof record.totalPages === "number"
        ? record.totalPages
        : Math.ceil(
            (typeof record.totalCount === "number" ? record.totalCount : items.length) / pageSize,
          ) || 1,
  };
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

export function asNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  return 0;
}

export function asOptionalNumber(value: unknown): number | undefined {
  const parsed = asNumber(value);
  return parsed > 0 ? parsed : undefined;
}

export function asOptionalString(value: unknown): string | undefined {
  const parsed = asString(value).trim();
  return parsed || undefined;
}

export function asOptionalBoolean(value: unknown): boolean | undefined {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") {
    if (value === "true") return true;
    if (value === "false") return false;
  }
  return undefined;
}

export function normalizeAdminClient(payload: unknown): AdminClientSummary | null {
  if (!isRecord(payload)) return null;

  return {
    id: asNumber(payload.id),
    clientId: asString(payload.clientId),
    fullName: asString(payload.fullName),
    email: asString(payload.email),
    phone: asOptionalString(payload.phone),
    dateOfBirth: asOptionalString(payload.dateOfBirth),
    gender: asOptionalString(payload.gender),
    maritalStatus: asOptionalString(payload.maritalStatus),
    preferredLanguage: asOptionalString(payload.preferredLanguage),
    pronouns: asOptionalString(payload.pronouns),
    timezone: asOptionalString(payload.timezone),
    status: asString(payload.status),
    stage: asOptionalString(payload.stage),
    clientType: asOptionalString(payload.clientType),
    assignedTherapistId: asOptionalNumber(payload.assignedTherapistId),
    assignedTherapistName: asOptionalString(payload.assignedTherapistName),
    streetAddress1: asOptionalString(payload.streetAddress1),
    streetAddress2: asOptionalString(payload.streetAddress2),
    city: asOptionalString(payload.city),
    province: asOptionalString(payload.province),
    postalCode: asOptionalString(payload.postalCode),
    country: asOptionalString(payload.country),
    emergencyContactName: asOptionalString(payload.emergencyContactName),
    emergencyContactPhone: asOptionalString(payload.emergencyContactPhone),
    emergencyContactRelationship: asOptionalString(payload.emergencyContactRelationship),
    insuranceProvider: asOptionalString(payload.insuranceProvider),
    insuranceType: asOptionalString(payload.insuranceType),
    treatmentModality: asOptionalString(payload.treatmentModality),
    policyNumber: asOptionalString(payload.policyNumber),
    groupNumber: asOptionalString(payload.groupNumber),
    insurancePhone: asOptionalString(payload.insurancePhone),
    copayAmount: asOptionalNumber(payload.copayAmount),
    deductible: asOptionalNumber(payload.deductible),
    referrerName: asOptionalString(payload.referrerName),
    referralDate: asOptionalString(payload.referralDate),
    startDate: asOptionalString(payload.startDate),
    referenceNumber: asOptionalString(payload.referenceNumber),
    clientSource: asOptionalString(payload.clientSource),
    referralNotes: asOptionalString(payload.referralNotes),
    hasPortalAccess: asOptionalBoolean(payload.hasPortalAccess),
    portalEmail: asOptionalString(payload.portalEmail),
    emailNotifications: asOptionalBoolean(payload.emailNotifications),
    lastLogin: asOptionalString(payload.lastLogin) ?? null,
    employmentStatus: asOptionalString(payload.employmentStatus),
    educationLevel: asOptionalString(payload.educationLevel),
    numberOfDependents:
      typeof payload.numberOfDependents === "number" && Number.isFinite(payload.numberOfDependents)
        ? payload.numberOfDependents
        : undefined,
    notes: asOptionalString(payload.notes),
    needsFollowUp: asOptionalBoolean(payload.needsFollowUp),
    priority: asOptionalString(payload.priority),
    followUpDate: asOptionalString(payload.followUpDate),
    followUpNotes: asOptionalString(payload.followUpNotes),
    serviceType: asOptionalString(payload.serviceType),
    serviceFrequency: asOptionalString(payload.serviceFrequency),
    checklistCount: asNumber(payload.checklistCount),
    documentCount: asNumber(payload.documentCount),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
    lastSessionDate: asOptionalString(payload.lastSessionDate),
    nextAppointmentDate: asOptionalString(payload.nextAppointmentDate),
  };
}

export function normalizeAdminClientSummary(payload: unknown): AdminClientListSummary | null {
  if (!isRecord(payload)) return null;

  return {
    id: asNumber(payload.id),
    clientId: asString(payload.clientId),
    fullName: asString(payload.fullName),
    status: asString(payload.status),
    stage: asOptionalString(payload.stage),
    assignedTherapistId: asOptionalNumber(payload.assignedTherapistId),
    assignedTherapistName: asOptionalString(payload.assignedTherapistName),
    referenceNumber: asOptionalString(payload.referenceNumber),
    checklistCount: asNumber(payload.checklistCount),
    documentCount: asNumber(payload.documentCount),
    lastSessionDate: asOptionalString(payload.lastSessionDate),
    nextAppointmentDate: asOptionalString(payload.nextAppointmentDate),
  };
}

export function normalizeAdminClientsResponse(payload: unknown): AdminClientsListResponse {
  const root = isRecord(payload) ? payload : {};
  const rawItems = Array.isArray(root.items) ? root.items : [];
  const items = rawItems
    .map((entry) => normalizeAdminClientSummary(entry))
    .filter(Boolean) as AdminClientListSummary[];

  return {
    items,
    totalCount: asNumber(root.totalCount),
    page: Math.max(1, asNumber(root.page) || 1),
    pageSize: Math.max(1, asNumber(root.pageSize) || 25),
    totalPages: Math.max(0, asNumber(root.totalPages)),
  };
}

export function normalizeAdminCreatedClient(payload: unknown): AdminCreatedClient {
  const normalized = normalizeAdminClient(payload);
  if (normalized) return normalized;

  return {
    id: 0,
    clientId: "",
    fullName: "",
    email: "",
    status: "",
  };
}

export function normalizeAdminClientDetails(payload: unknown): AdminClientDetails {
  const normalized = normalizeAdminClient(payload);
  if (normalized) return normalized;

  return {
    id: 0,
    clientId: "",
    fullName: "",
    email: "",
    status: "",
  };
}

export function normalizeAdminClientSessionSummary(payload: unknown): AdminClientSessionSummary {
  const root = isRecord(payload) ? payload : {};

  return {
    clientId: asNumber(root.clientId),
    totalSessions: asNumber(root.totalSessions),
    completed: asNumber(root.completed),
    scheduled: asNumber(root.scheduled),
    missedCancelled: asNumber(root.missedCancelled),
    conflicts: asNumber(root.conflicts),
  };
}

export function normalizeAdminClientSession(payload: unknown): AdminClientSession | null {
  if (!isRecord(payload)) return null;

  return {
    id: asNumber(payload.id),
    clientId: asNumber(payload.clientId),
    clientName: asOptionalString(payload.clientName),
    therapistId: asOptionalNumber(payload.therapistId),
    therapistName: asOptionalString(payload.therapistName),
    sessionDate: asOptionalString(payload.sessionDate),
    duration: asOptionalNumber(payload.duration),
    sessionType: asOptionalString(payload.sessionType),
    sessionMode: asOptionalString(payload.sessionMode),
    status: asOptionalString(payload.status),
    serviceId: asOptionalNumber(payload.serviceId),
    serviceName: asOptionalString(payload.serviceName),
    roomId: asOptionalNumber(payload.roomId),
    roomName: asOptionalString(payload.roomName),
    notes: asOptionalString(payload.notes),
    zoomEnabled: asOptionalBoolean(payload.zoomEnabled),
    zoomMeetingId: asOptionalString(payload.zoomMeetingId),
    zoomJoinUrl: asOptionalString(payload.zoomJoinUrl),
    zoomPassword: asOptionalString(payload.zoomPassword),
    recurrenceGroupId: asOptionalString(payload.recurrenceGroupId),
    billingId: asOptionalNumber(payload.billingId),
    hasInvoice: asOptionalBoolean(payload.hasInvoice),
    remainingDue: asOptionalNumber(payload.remainingDue),
    invoicePaid: asOptionalBoolean(payload.invoicePaid),
    hasTranscript: asOptionalBoolean(payload.hasTranscript),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

export function normalizeAdminClientSessions(payload: unknown): AdminClientSession[] {
  if (!Array.isArray(payload)) return [];

  return payload
    .map((entry) => normalizeAdminClientSession(entry))
    .filter(Boolean) as AdminClientSession[];
}

export function normalizeAdminAssessmentTemplate(payload: unknown): AdminAssessmentTemplate | null {
  if (!isRecord(payload)) return null;

  const rawSections = Array.isArray(payload.sections) ? payload.sections : [];
  const sections: AdminAssessmentSection[] = rawSections.map((s) => {
    const rs = isRecord(s) ? s : {};
    const rawQs = Array.isArray(rs.questions) ? rs.questions : [];
    return {
      id: asNumber(rs.id),
      title: asString(rs.title),
      description: asOptionalString(rs.description),
      accessLevel: asOptionalString(rs.accessLevel),
      isScoring: asOptionalBoolean(rs.isScoring),
      sortOrder: asNumber(rs.sortOrder),
      reportMapping: asOptionalString(rs.reportMapping),
      aiReportPrompt: asOptionalString(rs.aiReportPrompt),
      questions: rawQs.map((q) => {
        const rq = isRecord(q) ? q : {};
        const rawOpts = Array.isArray(rq.options) ? rq.options : [];
        return {
          id: asNumber(rq.id),
          questionText: asString(rq.questionText),
          questionType: asString(rq.questionType),
          isRequired: Boolean(rq.isRequired),
          sortOrder: asNumber(rq.sortOrder),
          ratingMin: asOptionalNumber(rq.ratingMin),
          ratingMax: asOptionalNumber(rq.ratingMax),
          ratingLabels: asOptionalString(rq.ratingLabels),
          contributesToScore: asOptionalBoolean(rq.contributesToScore),
          options: rawOpts.map((o) => {
            const ro = isRecord(o) ? o : {};
            return {
              id: asNumber(ro.id),
              optionText: asString(ro.optionText),
              optionValue: asNumber(ro.optionValue),
              sortOrder: asNumber(ro.sortOrder),
            };
          }),
        };
      }),
    };
  });

  return {
    id: asNumber(payload.id),
    name: asOptionalString(payload.name),
    title: asOptionalString(payload.title),
    description: asOptionalString(payload.description),
    category: asOptionalString(payload.category),
    isStandardized: asOptionalBoolean(payload.isStandardized),
    version: asOptionalNumber(payload.version),
    isActive: asOptionalBoolean(payload.isActive),
    sections,
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
    sectionsCount: sections.length,
  };
}

export function normalizeAdminAssessmentTemplates(payload: unknown): AdminAssessmentTemplate[] {
  if (!Array.isArray(payload)) return [];

  return payload
    .map((entry) => normalizeAdminAssessmentTemplate(entry))
    .filter(Boolean) as AdminAssessmentTemplate[];
}

export function normalizeAdminAssessmentSections(payload: unknown): AdminAssessmentSection[] {
  if (!Array.isArray(payload)) return [];

  return payload
    .map((section) => {
      const rs = isRecord(section) ? section : {};
      const rawQs = Array.isArray(rs.questions) ? rs.questions : [];
      return {
        id: asNumber(rs.id),
        title: asString(rs.title),
        description: asOptionalString(rs.description),
        accessLevel: asOptionalString(rs.accessLevel),
        isScoring: asOptionalBoolean(rs.isScoring),
        sortOrder: asNumber(rs.sortOrder),
        reportMapping: asOptionalString(rs.reportMapping),
        aiReportPrompt: asOptionalString(rs.aiReportPrompt),
        questions: rawQs.map((q) => {
          const rq = isRecord(q) ? q : {};
          const rawOpts = Array.isArray(rq.options) ? rq.options : [];
          return {
            id: asNumber(rq.id),
            questionText: asString(rq.questionText),
            questionType: asString(rq.questionType),
            isRequired: Boolean(rq.isRequired),
            sortOrder: asNumber(rq.sortOrder),
            ratingMin: asOptionalNumber(rq.ratingMin),
            ratingMax: asOptionalNumber(rq.ratingMax),
            ratingLabels: asOptionalString(rq.ratingLabels),
            contributesToScore: asOptionalBoolean(rq.contributesToScore),
            options: rawOpts.map((o) => {
              const ro = isRecord(o) ? o : {};
              return {
                id: asNumber(ro.id),
                optionText: asString(ro.optionText),
                optionValue: asNumber(ro.optionValue),
                sortOrder: asNumber(ro.sortOrder),
              };
            }),
          };
        }),
      };
    })
    .filter((section) => section.id > 0);
}

export function normalizeAdminAssessmentTemplatesResponse(
  payload: unknown,
  filters: AdminAssessmentTemplatesListParams,
): AdminAssessmentTemplatesListResponse {
  if (Array.isArray(payload)) {
    const items = normalizeAdminAssessmentTemplates(payload);
    const page = Math.max(1, filters.page ?? 1);
    const pageSize = Math.max(1, filters.pageSize ?? (items.length || 20));
    const totalCount = items.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));

    return {
      items,
      totalCount,
      page,
      pageSize,
      totalPages,
    };
  }

  if (!isRecord(payload)) {
    return {
      items: [],
      totalCount: 0,
      page: Math.max(1, filters.page ?? 1),
      pageSize: Math.max(1, filters.pageSize ?? 20),
      totalPages: 1,
    };
  }

  const rawItems = Array.isArray(payload.items)
    ? payload.items
    : Array.isArray(payload.data)
      ? payload.data
      : Array.isArray(payload.results)
        ? payload.results
        : [];

  const items = normalizeAdminAssessmentTemplates(rawItems);
  const page = Math.max(1, asNumber(payload.page) || filters.page || 1);
  const pageSize = Math.max(1, asNumber(payload.pageSize) || filters.pageSize || items.length || 20);
  const totalCount = Math.max(asNumber(payload.totalCount) || asNumber(payload.total) || items.length, items.length);
  const totalPages = Math.max(
    asNumber(payload.totalPages) || Math.ceil(totalCount / Math.max(pageSize, 1)) || 1,
    1,
  );

  return {
    items,
    totalCount,
    page,
    pageSize,
    totalPages,
  };
}

export function normalizeAdminClientAssessment(payload: unknown): AdminClientAssessment | null {
  if (!isRecord(payload)) return null;

  return {
    id: asNumber(payload.id),
    templateId: asNumber(payload.templateId),
    templateName: asOptionalString(payload.templateName),
    clientId: asNumber(payload.clientId),
    clientName: asOptionalString(payload.clientName),
    status: asOptionalString(payload.status),
    assignedById: asOptionalNumber(payload.assignedById),
    assignedByName: asOptionalString(payload.assignedByName),
    assignedDate: asOptionalString(payload.assignedDate),
    dueDate: asOptionalString(payload.dueDate),
    completedAt: asOptionalString(payload.completedAt),
    totalScore: asOptionalNumber(payload.totalScore),
    notes: asOptionalString(payload.notes),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

export function normalizeAdminClientAssessments(payload: unknown): AdminClientAssessment[] {
  if (!Array.isArray(payload)) return [];

  return payload
    .map((entry) => normalizeAdminClientAssessment(entry))
    .filter(Boolean) as AdminClientAssessment[];
}

export function normalizeAdminAssessmentAssignmentsResponse(
  payload: unknown,
  filters: AdminAssessmentAssignmentsListParams,
): AdminAssessmentAssignmentsListResponse {
  if (Array.isArray(payload)) {
    const items = normalizeAdminClientAssessments(payload);
    const page = Math.max(1, filters.page ?? 1);
    const pageSize = Math.max(1, filters.pageSize ?? (items.length || 20));
    const totalCount = items.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));

    return { items, totalCount, page, pageSize, totalPages };
  }

  if (!isRecord(payload)) {
    return {
      items: [],
      totalCount: 0,
      page: Math.max(1, filters.page ?? 1),
      pageSize: Math.max(1, filters.pageSize ?? 20),
      totalPages: 1,
    };
  }

  const rawItems = Array.isArray(payload.items)
    ? payload.items
    : Array.isArray(payload.data)
      ? payload.data
      : Array.isArray(payload.results)
        ? payload.results
        : [];

  const items = normalizeAdminClientAssessments(rawItems);
  const page = Math.max(1, asNumber(payload.page) || filters.page || 1);
  const pageSize = Math.max(1, asNumber(payload.pageSize) || filters.pageSize || items.length || 20);
  const totalCount = Math.max(asNumber(payload.totalCount) || asNumber(payload.total) || items.length, items.length);
  const totalPages = Math.max(
    asNumber(payload.totalPages) || Math.ceil(totalCount / Math.max(pageSize, 1)) || 1,
    1,
  );

  return { items, totalCount, page, pageSize, totalPages };
}

export function normalizeAdminAssessmentAnalytics(payload: unknown): AdminAssessmentAnalytics {
  const root = isRecord(payload) ? payload : {};

  return {
    totalAssignments: asNumber(root.totalAssignments),
    completedAssignments: asNumber(root.completedAssignments),
    pendingAssignments: asNumber(root.pendingAssignments),
    inProgressAssignments: asNumber(root.inProgressAssignments),
  };
}

export function normalizeReportDataField(value: unknown): string | undefined {
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed || undefined;
  }
  if (value === null || value === undefined) return undefined;
  if (typeof value === "object") {
    try {
      return JSON.stringify(value);
    } catch {
      return undefined;
    }
  }
  return String(value);
}

export function normalizeAdminAssessmentGeneratedReport(
  payload: unknown,
): AdminAssessmentGeneratedReport {
  const report = isRecord(payload) ? payload : {};
  return {
    id: asNumber(report.id),
    generatedContent: asOptionalString(report.generatedContent),
    draftContent: asOptionalString(report.draftContent),
    finalContent: asOptionalString(report.finalContent),
    reportData: normalizeReportDataField(report.reportData),
    editorContent: asOptionalString(report.editorContent),
    generatedAt: asOptionalString(report.generatedAt),
    isDraft: typeof report.isDraft === "boolean" ? report.isDraft : undefined,
    isFinalized: typeof report.isFinalized === "boolean" ? report.isFinalized : undefined,
    finalizedAt: asOptionalString(report.finalizedAt),
  };
}

export function normalizeClientStageDurationsResponse(
  payload: unknown,
): ClientStageDurationsResponse {
  const root = isRecord(payload) ? payload : {};
  const rawDurations = isRecord(root.durations) ? root.durations : {};
  const durations: Record<string, number> = {};

  Object.entries(rawDurations).forEach(([key, value]) => {
    const numericValue = asNumber(value);
    durations[key] = Number.isFinite(numericValue) ? numericValue : 0;
  });

  return {
    clientId: asNumber(root.clientId),
    currentStage: asOptionalString(root.currentStage),
    durations,
    totalEvents: asOptionalNumber(root.totalEvents),
  };
}

export function normalizeAdminClientHistoryEvent(payload: unknown): AdminClientHistoryEvent | null {
  if (!isRecord(payload)) return null;
  return {
    id: asNumber(payload.id),
    clientId: asNumber(payload.clientId),
    eventType: asOptionalString(payload.eventType),
    eventSource: asOptionalString(payload.eventSource),
    fromValue: asOptionalString(payload.fromValue),
    toValue: asOptionalString(payload.toValue),
    description: asOptionalString(payload.description),
    changeSummary: asOptionalString(payload.changeSummary),
    createdByUserId: asOptionalNumber(payload.createdByUserId),
    createdByName: asOptionalString(payload.createdByName),
    createdAt: asOptionalString(payload.createdAt),
  };
}

export function normalizeAdminClientHistoryListResponse(
  payload: unknown,
  page: number,
  pageSize: number,
): AdminClientHistoryListResponse {
  if (Array.isArray(payload)) {
    const items = payload
      .map((entry) => normalizeAdminClientHistoryEvent(entry))
      .filter(Boolean) as AdminClientHistoryEvent[];
    const totalCount = items.length;
    return {
      items,
      totalCount,
      page,
      pageSize,
      totalPages: Math.max(1, Math.ceil(totalCount / Math.max(pageSize, 1))),
    };
  }

  if (!isRecord(payload)) {
    return { items: [], totalCount: 0, page, pageSize, totalPages: 1 };
  }

  const rawItems = Array.isArray(payload.items)
    ? payload.items
    : Array.isArray(payload.data)
      ? payload.data
      : Array.isArray(payload.results)
        ? payload.results
        : [];

  const items = rawItems
    .map((entry) => normalizeAdminClientHistoryEvent(entry))
    .filter(Boolean) as AdminClientHistoryEvent[];
  const totalCount = Math.max(asNumber(payload.totalCount) || asNumber(payload.total) || items.length, items.length);
  const normalizedPage = Math.max(1, asNumber(payload.page) || page);
  const normalizedPageSize = Math.max(1, asNumber(payload.pageSize) || pageSize);
  const totalPages = Math.max(
    asNumber(payload.totalPages) || Math.ceil(totalCount / Math.max(normalizedPageSize, 1)) || 1,
    1,
  );

  return {
    items,
    totalCount,
    page: normalizedPage,
    pageSize: normalizedPageSize,
    totalPages,
  };
}

export function normalizeAdminClientEmailHistoryResponse(
  payload: unknown,
): AdminClientEmailHistoryResponse {
  const root = isRecord(payload) ? payload : {};
  const historyRaw = Array.isArray(root.history) ? root.history : [];
  const history = historyRaw
    .map((entry) => normalizeAdminClientHistoryEvent(entry))
    .filter(Boolean) as AdminClientHistoryEvent[];
  return {
    history,
    message: asOptionalString(root.message),
    count: Math.max(asNumber(root.count), history.length),
  };
}

export function normalizeAdminClientNote(payload: unknown): AdminClientNote | null {
  if (!isRecord(payload)) return null;
  return {
    id: asNumber(payload.id),
    clientId: asNumber(payload.clientId),
    authorId: asOptionalNumber(payload.authorId),
    authorName: asOptionalString(payload.authorName),
    title: asOptionalString(payload.title),
    content: asString(payload.content),
    noteType: asOptionalString(payload.noteType),
    eventDate: asOptionalString(payload.eventDate),
    isPrivate: asOptionalBoolean(payload.isPrivate),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
  };
}

export function normalizeAdminClientNotes(payload: unknown): AdminClientNote[] {
  if (!Array.isArray(payload)) return [];
  return payload
    .map((entry) => normalizeAdminClientNote(entry))
    .filter(Boolean) as AdminClientNote[];
}

export function normalizeDuplicateRecommendation(payload: unknown): AdminDuplicateRecommendation {
  const root = isRecord(payload) ? payload : {};
  return {
    keepClientId: asOptionalNumber(root.keepClientId),
    deleteClientId: asOptionalNumber(root.deleteClientId),
    reasons: Array.isArray(root.reasons)
      ? root.reasons.filter((reason): reason is string => typeof reason === "string")
      : [],
  };
}

export function normalizeDuplicateGroup(payload: unknown): AdminDuplicateGroup | null {
  if (!isRecord(payload)) return null;
  const rawClients = Array.isArray(payload.clients) ? payload.clients : [];
  const clients = rawClients
    .map((entry) => normalizeAdminClientDetails(entry))
    .filter(Boolean) as AdminClientSummary[];
  return {
    clients,
    confidenceLevel: asOptionalString(payload.confidenceLevel),
    confidenceScore: asOptionalNumber(payload.confidenceScore),
    matchType: asOptionalString(payload.matchType),
    recommendation: normalizeDuplicateRecommendation(payload.recommendation),
  };
}

export function normalizeDuplicateDetectionResponse(
  payload: unknown,
): AdminDuplicateDetectionResponse {
  const root = isRecord(payload) ? payload : {};
  const rawGroups = Array.isArray(root.duplicateGroups) ? root.duplicateGroups : [];
  const duplicateGroups = rawGroups
    .map((entry) => normalizeDuplicateGroup(entry))
    .filter(Boolean) as AdminDuplicateGroup[];
  return {
    duplicateGroups,
    totalDuplicates: Math.max(asNumber(root.totalDuplicates), duplicateGroups.length),
  };
}

export function normalizeAdminFormTemplate(payload: unknown): AdminFormTemplate | null {
  if (!isRecord(payload)) return null;

  const activeVersion = isRecord(payload.activeVersion) ? payload.activeVersion : null;
  const topLevelInstructions = asOptionalString(payload.instructions);
  const activeVersionInstructions = activeVersion
    ? asOptionalString(activeVersion.instructions)
    : undefined;
  const topLevelDescription = asOptionalString(payload.description);
  const activeVersionDescription = activeVersion
    ? asOptionalString(activeVersion.description)
    : undefined;
  const topLevelName = asOptionalString(payload.name);
  const activeVersionName = activeVersion ? asOptionalString(activeVersion.name) : undefined;

  return {
    id: asNumber(payload.id),
    name: topLevelName ?? activeVersionName,
    description: topLevelDescription ?? activeVersionDescription,
    category: asOptionalString(payload.category),
    instructions: topLevelInstructions ?? activeVersionInstructions,
    requiresSignature: asOptionalBoolean(payload.requiresSignature),
    isActive: asOptionalBoolean(payload.isActive),
    isSystemTemplate: asOptionalBoolean(payload.isSystemTemplate),
    sortOrder: asOptionalNumber(payload.sortOrder),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
    fields: Array.isArray(payload.fields)
      ? payload.fields
      : Array.isArray(activeVersion?.fields)
        ? activeVersion.fields
        : undefined,
    activeVersion: activeVersion
      ? {
          name: activeVersionName,
          description: activeVersionDescription,
          instructions: activeVersionInstructions,
          sections: Array.isArray(activeVersion.sections) ? activeVersion.sections : undefined,
          fields: Array.isArray(activeVersion.fields) ? activeVersion.fields : undefined,
        }
      : undefined,
  };
}

export function normalizeAdminFormTemplates(payload: unknown): AdminFormTemplate[] {
  if (Array.isArray(payload)) {
    return payload
      .map((entry) => normalizeAdminFormTemplate(entry))
      .filter(Boolean) as AdminFormTemplate[];
  }

  const single = normalizeAdminFormTemplate(payload);
  return single ? [single] : [];
}

export function normalizeAdminFormTemplatesResponse(
  payload: unknown,
  filters: AdminFormTemplatesListParams,
): AdminFormTemplatesListResponse {
  if (Array.isArray(payload)) {
    const items = normalizeAdminFormTemplates(payload);
    const page = Math.max(1, filters.page ?? 1);
    const pageSize = Math.max(1, filters.pageSize ?? (items.length || 20));
    const totalCount = items.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
    return { items, totalCount, page, pageSize, totalPages };
  }

  if (!isRecord(payload)) {
    return {
      items: [],
      totalCount: 0,
      page: Math.max(1, filters.page ?? 1),
      pageSize: Math.max(1, filters.pageSize ?? 20),
      totalPages: 1,
    };
  }

  const rawItems = Array.isArray(payload.items)
    ? payload.items
    : Array.isArray(payload.data)
      ? payload.data
      : Array.isArray(payload.results)
        ? payload.results
        : [];

  const items = normalizeAdminFormTemplates(rawItems);
  const page = Math.max(1, asNumber(payload.page) || filters.page || 1);
  const pageSize = Math.max(1, asNumber(payload.pageSize) || filters.pageSize || items.length || 20);
  const totalCount = Math.max(asNumber(payload.totalCount) || asNumber(payload.total) || items.length, items.length);
  const totalPages = Math.max(
    asNumber(payload.totalPages) || Math.ceil(totalCount / Math.max(pageSize, 1)) || 1,
    1,
  );

  return { items, totalCount, page, pageSize, totalPages };
}

export function normalizeAdminFormAssignment(payload: unknown): AdminFormAssignment | null {
  if (!isRecord(payload)) return null;

  return {
    id: asNumber(payload.id),
    templateId: asNumber(payload.templateId),
    templateName: asOptionalString(payload.templateName),
    templateCategory: asOptionalString(payload.templateCategory),
    clientId: asNumber(payload.clientId),
    clientName: asOptionalString(payload.clientName),
    status: asOptionalString(payload.status),
    assignedAt: asOptionalString(payload.assignedAt) ?? asOptionalString(payload.createdAt),
    dueDate: asOptionalString(payload.dueDate),
    instructions: asOptionalString(payload.instructions),
    responses: Array.isArray(payload.responses)
      ? payload.responses.map((response) => {
          const entry = isRecord(response) ? response : {};
          return {
            id: asOptionalNumber(entry.id),
            fieldLabel: asOptionalString(entry.fieldLabel),
            fieldType: asOptionalString(entry.fieldType),
            value: asOptionalString(entry.value),
          };
        })
      : [],
    signatures: Array.isArray(payload.signatures)
      ? payload.signatures.map((signature) => {
          const entry = isRecord(signature) ? signature : {};
          return {
            id: asOptionalNumber(entry.id),
            signerName: asOptionalString(entry.signerName),
            signerRole: asOptionalString(entry.signerRole),
            signedAt: asOptionalString(entry.signedAt),
            signatureData: asOptionalString(entry.signatureData),
          };
        })
      : [],
  };
}

export function normalizeAdminFormAssignments(payload: unknown): AdminFormAssignment[] {
  if (Array.isArray(payload)) {
    return payload
      .map((entry) => normalizeAdminFormAssignment(entry))
      .filter(Boolean) as AdminFormAssignment[];
  }

  if (isRecord(payload) && Array.isArray(payload.items)) {
    return payload.items
      .map((entry) => normalizeAdminFormAssignment(entry))
      .filter(Boolean) as AdminFormAssignment[];
  }

  const single = normalizeAdminFormAssignment(payload);
  return single ? [single] : [];
}

export function normalizeAdminClientDocument(payload: unknown): AdminClientDocument | null {
  if (!isRecord(payload)) return null;
  return {
    id: asNumber(payload.id),
    clientId: asNumber(payload.clientId),
    clientName: asOptionalString(payload.clientName),
    fileName: asOptionalString(payload.fileName),
    originalName: asOptionalString(payload.originalName),
    fileSize: asOptionalNumber(payload.fileSize),
    mimeType: asOptionalString(payload.mimeType),
    documentType: asOptionalString(payload.documentType),
    category: asOptionalString(payload.category),
    description: asOptionalString(payload.description),
    uploadedById: asOptionalNumber(payload.uploadedById),
    uploadedByName: asOptionalString(payload.uploadedByName),
    uploadedAt: asOptionalString(payload.uploadedAt),
    needsReview: asOptionalBoolean(payload.needsReview),
    reviewStatus: asOptionalString(payload.reviewStatus),
    reviewDueAt: asOptionalString(payload.reviewDueAt),
    reviewedById: asOptionalNumber(payload.reviewedById),
    reviewedByName: asOptionalString(payload.reviewedByName),
    reviewedAt: asOptionalString(payload.reviewedAt),
    shareWithClient: asOptionalBoolean(payload.shareWithClient),
    createdAt: asOptionalString(payload.createdAt),
    updatedAt: asOptionalString(payload.updatedAt),
    previewUrl: asOptionalString(payload.previewUrl) || asOptionalString(payload.url),
    downloadUrl: asOptionalString(payload.downloadUrl),
  };
}

export function normalizeAdminClientDocuments(payload: unknown): AdminClientDocument[] {
  if (Array.isArray(payload)) {
    return payload
      .map((entry) => normalizeAdminClientDocument(entry))
      .filter(Boolean) as AdminClientDocument[];
  }
  if (isRecord(payload) && Array.isArray(payload.items)) {
    return payload.items
      .map((entry) => normalizeAdminClientDocument(entry))
      .filter(Boolean) as AdminClientDocument[];
  }
  const single = normalizeAdminClientDocument(payload);
  return single ? [single] : [];
}

export function normalizeAdminClientDocumentsResponse(
  payload: unknown,
  filters: { page?: number; pageSize?: number },
): AdminClientDocumentsListResponse {
  if (Array.isArray(payload)) {
    const items = normalizeAdminClientDocuments(payload);
    const page = Math.max(1, filters.page ?? 1);
    const pageSize = Math.max(1, filters.pageSize ?? (items.length || 20));
    const totalCount = items.length;
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));

    return { items, totalCount, page, pageSize, totalPages };
  }

  if (!isRecord(payload)) {
    return {
      items: [],
      totalCount: 0,
      page: Math.max(1, filters.page ?? 1),
      pageSize: Math.max(1, filters.pageSize ?? 20),
      totalPages: 1,
    };
  }

  const rawItems = Array.isArray(payload.items)
    ? payload.items
    : Array.isArray(payload.data)
      ? payload.data
      : Array.isArray(payload.results)
        ? payload.results
        : [];

  const items = rawItems
    .map((entry) => normalizeAdminClientDocument(entry))
    .filter(Boolean) as AdminClientDocument[];
  const page = Math.max(1, asNumber(payload.page) || filters.page || 1);
  const pageSize = Math.max(
    1,
    asNumber(payload.pageSize) || filters.pageSize || items.length || 20,
  );
  const totalCount = Math.max(
    asNumber(payload.totalCount) || asNumber(payload.total) || items.length,
    items.length,
  );
  const totalPages = Math.max(
    asNumber(payload.totalPages) || Math.ceil(totalCount / Math.max(pageSize, 1)) || 1,
    1,
  );

  return { items, totalCount, page, pageSize, totalPages };
}
