export interface DuplicateRecord {
  id: string;
  clientId: string;
  name: string;
  phone: string;
  email: string;
  dob: string;
  status: "Pending" | "Active" | "Inactive";
  created: string;
  stats: {
    sessions: number;
    documents: number;
    billing: number;
  };
  isRecommendedToKeep: boolean;
  details?: {
    stage?: string;
    assignedTherapistName?: string;
    assignedTherapistId?: number;
    preferredLanguage?: string;
    pronouns?: string;
    gender?: string;
    maritalStatus?: string;
    clientType?: string;
    serviceType?: string;
    serviceFrequency?: string;
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
    policyNumber?: string;
    groupNumber?: string;
    insurancePhone?: string;
    copayAmount?: number;
    deductible?: number;
    referrerName?: string;
    referralDate?: string;
    referenceNumber?: string;
    clientSource?: string;
    hasPortalAccess?: boolean;
    portalEmail?: string;
    emailNotifications?: boolean;
    notes?: string;
    updatedAt?: string;
    lastSessionDate?: string;
    nextAppointmentDate?: string;
  };
}

export interface DuplicateGroup {
  id: string;
  groupNumber: number;
  matchReasons: string[];
  recommendation: {
    keepName: string;
    keepId: string;
    reason: string;
  };
  records: DuplicateRecord[];
}
