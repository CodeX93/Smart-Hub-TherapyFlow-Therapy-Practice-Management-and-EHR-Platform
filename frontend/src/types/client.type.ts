export interface Client {
  id: string;
  name: string;
  referenceNumber: string;
  therapist: string;
  assignedTherapistId?: number;
  lastSession: string;
  sinceLastSession: string;
  lastSessionDate?: string;
  attachedDocs: number;
  checklist: string; // e.g., "0/11 (0%)" or "---"
  // Profile data
  clientId?: string; // e.g., "CL-2025-1481"
  age?: number;
  serviceType?: string; // e.g., "Psychotherapy"
  isMVAClient?: boolean;
  insurance?: string; // e.g., "Blue Cross"
  insuranceProvider?: string;
  policyNumber?: string;
  groupNumber?: string;
  insurancePhone?: string;
  copayAmount?: number;
  deductible?: number;
  // Personal Info
  dateOfBirth?: string;
  gender?: string;
  maritalStatus?: string;
  preferredLanguage?: string;
  // Contact Info
  phone?: string;
  email?: string;
  portalEmail?: string;
  address?: string;
  emergencyContact?: {
    name: string;
    phone: string;
  };
  // Employment & Socioeconomic
  employmentStatus?: string;
  educationLevel?: string;
  numberOfDependents?: number;
  needsFollowUp?: boolean;
  priority?: string;
  followUpDate?: string;
  followUpNotes?: string;
  // Clinical Status
  clientStatus?: string; // e.g., "Active", "Inactive"
  clientStage?: string; // e.g., "Active", "Intake", "On Hold", "Completed"
  clientType?: string; // e.g., "Individual", "Couple", "Family", "Group"
  serviceFrequency?: string; // e.g., "Weekly", "Bi-weekly", "Monthly"
  treatmentModality?: string;
  insuranceType?: string;
  completedSessions?: number;
  // Referral Information
  referrerName?: string;
  referralNumber?: string;
  startDate?: string;
  referralDate?: string;
  referralSource?: string;
  notes?: string;
  // Portal Access
  portalAccessEnabled?: boolean;
  /** Portal last login (auth identity); undefined/null = never logged in. */
  lastLogin?: string | null;
}

export interface ClientFilters {
  clientStatus?: string[]; // Replaces allClients
  clientStage?: string[];
  clientType?: string[];
  assignedTherapist?: string[];
  hasPortalAccess?: boolean;
  hasPendingTasks?: boolean;
  hasNoSessions?: boolean;
  needsFollowUp?: boolean;
  unassigned?: boolean;
  checklistTemplate?: string[];
  reportTemplate?: string[];
  noSessions?: boolean;
}

export interface ClientsPageProps {
  clients: Client[];
  itemsPerPage: number;
}
