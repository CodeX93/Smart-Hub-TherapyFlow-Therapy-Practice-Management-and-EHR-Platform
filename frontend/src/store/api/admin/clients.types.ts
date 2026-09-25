export interface AdminClientSummary {
  id: number;
  clientId: string;
  fullName: string;
  email: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: string;
  maritalStatus?: string;
  preferredLanguage?: string;
  pronouns?: string;
  /** Resolved by the API: the client's own setting, else the clinic's. */
  timezone?: string;
  status: string;
  stage?: string;
  clientType?: string;
  employmentStatus?: string;
  educationLevel?: string;
  numberOfDependents?: number;
  assignedTherapistId?: number;
  assignedTherapistName?: string;
  streetAddress1?: string;
  streetAddress2?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  country?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  insuranceProvider?: string;
  insuranceType?: string;
  treatmentModality?: string;
  policyNumber?: string;
  groupNumber?: string;
  insurancePhone?: string;
  copayAmount?: number;
  deductible?: number;
  referrerName?: string;
  referralDate?: string;
  startDate?: string;
  referenceNumber?: string;
  clientSource?: string;
  /** Referral-tab note. Stored apart from `notes`, which holds the clinical note. */
  referralNotes?: string;
  hasPortalAccess?: boolean;
  portalEmail?: string;
  emailNotifications?: boolean;
  /** Portal identity last successful login ISO timestamp; null/undefined if never logged in. */
  lastLogin?: string | null;
  notes?: string;
  needsFollowUp?: boolean;
  priority?: string;
  followUpDate?: string;
  followUpNotes?: string;
  serviceType?: string;
  serviceFrequency?: string;
  checklistCount?: number;
  documentCount?: number;
  createdAt?: string;
  updatedAt?: string;
  lastSessionDate?: string;
  nextAppointmentDate?: string;
}

export type AdminCreatedClient = AdminClientSummary;
export type AdminClientDetails = AdminClientSummary;

/**
 * Slim shape returned by the paginated clients list (GET /clients). Deliberately excludes
 * insurance/emergency-contact/address/notes demographics fields, which require additional
 * per-row lookups on the backend and are only needed on the client detail view
 * (GET /clients/{id}, which still returns the full `AdminClientSummary`).
 *
 * Mirrors `ClientSummaryResponse` on the backend.
 */
export interface AdminClientListSummary {
  id: number;
  clientId: string;
  fullName: string;
  status: string;
  stage?: string;
  assignedTherapistId?: number;
  assignedTherapistName?: string;
  referenceNumber?: string;
  checklistCount?: number;
  documentCount?: number;
  lastSessionDate?: string;
  nextAppointmentDate?: string;
}

export interface UpdateAdminClientPayload {
  id: number;
  body: Record<string, unknown>;
}
export interface UpdateAdminClientPortalAccessPayload {
  id: number;
  enable: boolean;
  email: string;
}

export interface PortalActivationResponse {
  success?: boolean;
  message?: string;
  timestamp?: string;
}

export interface AdminClientSessionSummary {
  clientId: number;
  totalSessions: number;
  completed: number;
  scheduled: number;
  missedCancelled: number;
  conflicts: number;
}

export interface AdminClientSession {
  id: number;
  clientId: number;
  clientName?: string;
  therapistId?: number;
  therapistName?: string;
  sessionDate?: string;
  duration?: number;
  sessionType?: string;
  sessionMode?: string;
  status?: string;
  serviceId?: number;
  serviceName?: string;
  roomId?: number;
  roomName?: string;
  notes?: string;
  zoomEnabled?: boolean;
  zoomMeetingId?: string;
  zoomJoinUrl?: string;
  zoomPassword?: string;
  recurrenceGroupId?: string;
  billingId?: number;
  hasInvoice?: boolean;
  remainingDue?: number;
  invoicePaid?: boolean;
  hasTranscript?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminAssessmentOption {
  id: number;
  optionText: string;
  optionValue: number;
  sortOrder: number;
}

export interface AdminAssessmentQuestion {
  id: number;
  questionText: string;
  questionType: string;
  isRequired: boolean;
  sortOrder: number;
  ratingMin?: number;
  ratingMax?: number;
  ratingLabels?: string;
  contributesToScore?: boolean;
  options?: AdminAssessmentOption[];
}

export interface AdminAssessmentSection {
  id: number;
  title: string;
  description?: string;
  accessLevel?: string;
  isScoring?: boolean;
  sortOrder?: number;
  reportMapping?: string;
  aiReportPrompt?: string;
  questions?: AdminAssessmentQuestion[];
}

export interface AdminAssessmentTemplate {
  id: number;
  name?: string;
  title?: string;
  description?: string;
  category?: string;
  isStandardized?: boolean;
  version?: number;
  isActive?: boolean;
  sections?: AdminAssessmentSection[];
  createdAt?: string;
  updatedAt?: string;
  sectionsCount?: number;
}

export interface CreateAssessmentTemplatePayload {
  name: string;
  description?: string;
  category?: string;
  isStandardized?: boolean;
  version?: number;
}

export interface UpdateAssessmentTemplatePayload {
  name?: string;
  description?: string;
  category?: string;
  isStandardized?: boolean;
  version?: string | number;
  isActive?: boolean;
  sections?: {
    id?: number;
    title: string;
    description?: string;
    accessLevel?: string;
    isScoring?: boolean;
    sortOrder?: number;
    reportMapping?: string | null;
    aiReportPrompt?: string;
    questions?: {
      id?: number;
      questionText: string;
      questionType: string;
      isRequired: boolean;
      sortOrder: number;
      ratingMin?: number;
      ratingMax?: number;
      ratingLabels?: string;
      contributesToScore?: boolean;
      options?: {
        id?: number;
        optionText: string;
        optionValue: number;
        sortOrder: number;
      }[];
    }[];
  }[];
}

export interface AdminAssessmentTemplatesListParams {
  page?: number;
  pageSize?: number;
}

export interface AdminAssessmentTemplatesListResponse {
  items: AdminAssessmentTemplate[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AdminClientAssessment {
  id: number;
  templateId: number;
  templateName?: string;
  clientId: number;
  clientName?: string;
  status?: string;
  assignedById?: number;
  assignedByName?: string;
  assignedDate?: string;
  dueDate?: string;
  completedAt?: string;
  totalScore?: number;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminAssessmentAnalytics {
  totalAssignments: number;
  completedAssignments: number;
  pendingAssignments: number;
  inProgressAssignments: number;
}

export interface AdminAssessmentAssignmentsListParams {
  page?: number;
  pageSize?: number;
  search?: string;
  templateId?: number;
  clientId?: number;
  status?: string;
  from?: string;
  to?: string;
}

export interface AdminAssessmentAssignmentsListResponse {
  items: AdminClientAssessment[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AssignClientAssessmentPayload {
  clientId: number;
  templateId: number;
  dueDate: string;
  notes?: string;
}

export interface SubmitAssessmentResponseItemPayload {
  questionId: number;
  responseText?: string;
  scoreValue?: number;
  selectedOptionId?: number;
  selectedOptionIds?: number[];
  ratingValue?: number;
}

export interface SubmitAssessmentResponsesPayload {
  assignmentId: number;
  responses: SubmitAssessmentResponseItemPayload[];
}

export interface AdminAssessmentGeneratedReport {
  id: number;
  generatedContent?: string;
  draftContent?: string;
  finalContent?: string;
  reportData?: string;
  editorContent?: string;
  generatedAt?: string;
  isDraft?: boolean;
  isFinalized?: boolean;
  finalizedAt?: string;
}

export interface AdminAssessmentAssignmentResponseItem {
  id: number;
  assignmentId: number;
  questionId: number;
  questionText?: string;
  responderType?: string;
  responderUserId?: number;
  responderClientId?: number;
  responseText?: string;
  responseValue?: string;
  score?: number;
  answeredAt?: string;
  selectedOptionIds?: number[];
  ratingValue?: number;
}

export interface ClientStageDurationsResponse {
  clientId: number;
  currentStage?: string;
  durations: Record<string, number>;
  totalEvents?: number;
}

export interface AdminClientHistoryEvent {
  id: number;
  clientId: number;
  eventType?: string;
  eventSource?: string;
  fromValue?: string;
  toValue?: string;
  description?: string;
  changeSummary?: string;
  createdByUserId?: number;
  createdByName?: string;
  createdAt?: string;
}

export interface AdminClientHistoryListResponse {
  items: AdminClientHistoryEvent[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AdminClientEmailHistoryResponse {
  history: AdminClientHistoryEvent[];
  message?: string;
  count: number;
}

export interface AdminClientSmsLogItem {
  id: number;
  action: string;
  result: string;
  resourceId?: string;
  timestamp: string;
  details?: string | null;
}

export interface AdminClientSmsLogResponse {
  items: AdminClientSmsLogItem[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AdminClientNote {
  id: number;
  clientId: number;
  authorId?: number;
  authorName?: string;
  title?: string;
  content: string;
  noteType?: string;
  eventDate?: string;
  isPrivate?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminDuplicateRecommendation {
  keepClientId?: number;
  deleteClientId?: number;
  reasons?: string[];
}

export interface AdminDuplicateGroup {
  clients: AdminClientSummary[];
  confidenceLevel?: string;
  confidenceScore?: number;
  matchType?: string;
  recommendation?: AdminDuplicateRecommendation;
}

export interface AdminDuplicateDetectionResponse {
  duplicateGroups: AdminDuplicateGroup[];
  totalDuplicates: number;
}

export interface MarkClientDuplicatePayload {
  id: number;
  duplicateOfClientId: number;
}

export interface UpdateAssessmentAssignmentStatusPayload {
  status: string;
  notes?: string;
}

export interface AdminFormField extends Omit<CreateFormFieldPayload, "templateId" | "options"> {
  id: number;
  templateId?: number;
  options?: string | Array<string | { optionText?: string; label?: string; value?: string }>;
}
export interface AdminFormSection {
  id: number;
  name?: string;
  fields?: AdminFormField[];
}

export interface AdminFormTemplate {
  id: number;
  name?: string;
  description?: string;
  category?: string;
  instructions?: string;
  requiresSignature?: boolean;
  isActive?: boolean;
  isSystemTemplate?: boolean;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
  fields?: AdminFormField[];
  activeVersion?: {
    instructions?: string;
    description?: string;
    name?: string;
    sections?: AdminFormSection[];
    fields?: AdminFormField[];
  };
}

export interface AdminFormFieldPayload {
  fieldType: string;
  label: string;
  isRequired: boolean;
  placeholder?: string;
  sortOrder?: number;
}

export interface CreateFormTemplatePayload {
  name: string;
  category: string;
  requiresSignature: boolean;
  description?: string;
  instructions?: string;
  isActive?: boolean;
  isSystemTemplate?: boolean;
  sortOrder?: number;
  fields?: AdminFormFieldPayload[];
}

export interface CreateFormFieldPayload {
  templateId: number;
  fieldType: string;
  label: string;
  placeholder?: string;
  helpText?: string;
  isRequired: boolean;
  options?: string;
  validation?: string;
  defaultValue?: string;
  autoPopulate?: string;
  conditionalDisplay?: string;
  sortOrder?: number;
}

export interface AdminFormTemplatesListParams {
  page?: number;
  pageSize?: number;
  search?: string;
  category?: string;
}

export interface AdminFormTemplatesListResponse {
  items: AdminFormTemplate[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface CreateFormAssignmentPayload {
  templateId: number;
  clientId: number;
  dueDate?: string;
  instructions?: string;
}

export interface AdminFormAssignment {
  id: number;
  templateId: number;
  templateName?: string;
  templateCategory?: string;
  clientId: number;
  clientName?: string;
  status?: string;
  assignedAt?: string;
  dueDate?: string;
  instructions?: string;
  responses?: Array<{
    id?: number;
    fieldLabel?: string;
    fieldType?: string;
    value?: string;
  }>;
  signatures?: Array<{
    id?: number;
    signerName?: string;
    signerRole?: string;
    signedAt?: string;
    signatureData?: string;
  }>;
}

export interface AdminClientsListParams {
  page?: number;
  pageSize?: number;
  search?: string;
  status?: string;
  stage?: string;
  therapistId?: number;
  clientType?: string;
  hasPortalAccess?: boolean;
  hasPendingTasks?: boolean;
  hasNoSessions?: boolean;
  needsFollowUp?: boolean;
  unassigned?: boolean;
  includeUnassigned?: boolean;
  checklistTemplateId?: number;
  reportTemplateId?: number;
  sortBy?: string;
  sortOrder?: "asc" | "desc";
}

export interface AdminClientsListResponse {
  items: AdminClientListSummary[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface AdminClientDocument {
  id: number;
  clientId: number;
  clientName?: string;
  fileName?: string;
  originalName?: string;
  fileSize?: number;
  mimeType?: string;
  documentType?: string;
  category?: string;
  description?: string;
  uploadedById?: number;
  uploadedByName?: string;
  uploadedAt?: string;
  needsReview?: boolean;
  reviewStatus?: string;
  reviewDueAt?: string;
  reviewedById?: number;
  reviewedByName?: string;
  reviewedAt?: string;
  shareWithClient?: boolean;
  createdAt?: string;
  updatedAt?: string;
  previewUrl?: string;
  downloadUrl?: string;
}

export interface AdminClientDocumentsListResponse {
  items: AdminClientDocument[];
  totalCount: number;
  page: number;
  pageSize: number;
  totalPages: number;
}
