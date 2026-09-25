import type { FormSection } from "@/components/admin-consent-sections/ConsentSectionCard";

export interface LibraryEntry {
  id: string;
  code: string;
  title: string;
  usageCount: number;
  connectedCount: number;
  connections: ConnectedEntry[];
}

export interface ConnectedEntry {
  id: string;
  code: string;
  category: "Symptoms" | "Interventions" | "Goals";
}

export const LIBRARY_TABS = [
  "Session Focus",
  "Symptoms",
  "Short-term goals",
  "Interventions",
  "Progress",
];

export const MOCK_LIBRARY_ENTRIES: LibraryEntry[] = [
  {
    id: "1",
    code: "AN",
    title: "Anxiety",
    usageCount: 20,
    connectedCount: 24,
    connections: [
      { id: "c1", code: "AXS16", category: "Symptoms" },
      { id: "c2", code: "AXS11", category: "Symptoms" },
      { id: "c3", code: "AXS6", category: "Symptoms" },
      { id: "c4", code: "AXS21", category: "Symptoms" },
      { id: "c5", code: "AXS7", category: "Symptoms" },
      { id: "c6", code: "AXS1", category: "Symptoms" },
      { id: "c7", code: "AXS17", category: "Symptoms" },
      { id: "c8", code: "AXS12", category: "Symptoms" },
      { id: "c9", code: "AXS24", category: "Symptoms" },
      { id: "c10", code: "AXS8", category: "Symptoms" },
      { id: "c11", code: "AXS22", category: "Symptoms" },
      { id: "c12", code: "AXS14", category: "Symptoms" },
      { id: "c13", code: "AXS18", category: "Symptoms" },
      { id: "c14", code: "AXS2", category: "Symptoms" },
      { id: "c15", code: "AXS4", category: "Symptoms" },
      { id: "c16", code: "AXS9", category: "Symptoms" },
      { id: "c17", code: "AXS13", category: "Symptoms" },
      { id: "c18", code: "AXS25", category: "Symptoms" },
      { id: "c19", code: "AXS19", category: "Symptoms" },
      { id: "c20", code: "AXS23", category: "Symptoms" },
      { id: "c21", code: "AXS15", category: "Symptoms" },
      { id: "c22", code: "AXS10", category: "Symptoms" },
      { id: "c23", code: "AXS5", category: "Symptoms" },
      { id: "c24", code: "AXS20", category: "Symptoms" },
    ],
  },
  {
    id: "2",
    code: "DEP",
    title: "Depression",
    usageCount: 0,
    connectedCount: 0,
    connections: [],
  },
  {
    id: "3",
    code: "PTSD",
    title: "PTSD",
    usageCount: 0,
    connectedCount: 0,
    connections: [],
  },
];

export interface AssessmentTemplate {
  id: string;
  title: string;
  category: string;
  sections: number;
  createdBy: string;
  version: string;
  type: "Custom" | "Standard";
}

export interface ActiveAssignment {
  id: string;
  title: string;
  status: "Completed" | "Pending" | "In Progress";
  assignedTo: {
    name: string;
    id: string;
  };
  assignedBy: string;
  dueDate: string;
}

export const ASSESSMENT_TABS = ["Templates", "Active Assignments"];

export const ASSESSMENT_CATEGORIES = [
  "All Categories",
  "Clinical",
  "Psychological",
  "Behavioral",
  "Cognitive",
  "Custom",
];

export const ASSESSMENT_STATUS_OPTIONS = [
  { value: "Completed", label: "Completed" },
  { value: "Pending", label: "Pending" },
  { value: "In Progress", label: "In Progress" },
];

export const MOCK_TEMPLATES: AssessmentTemplate[] = Array(100)
  .fill(null)
  .map((_, i) => ({
    id: `template-${i + 1}`,
    title: "Mental Health Assessment",
    category: i % 2 === 0 ? "Psychological" : "Clinical",
    sections: 14,
    createdBy: "Abi cherian, MSW",
    version: "1.0",
    type: i % 3 === 0 ? "Standard" : "Custom",
  }));

export const MOCK_ASSIGNMENTS: ActiveAssignment[] = Array(100)
  .fill(null)
  .map((_, i) => ({
    id: `assignment-${i + 1}`,
    title: "Mental Health Assessment",
    status: i % 3 === 0 ? "In Progress" : i % 3 === 1 ? "Pending" : "Completed",
    assignedTo: {
      name: "Zeeshan Khan",
      id: "CL-2025-1464",
    },
    assignedBy: "Faizan",
    dueDate: "Dec 12, 2026",
  }));

export interface FormEntry {
  id: string;
  name: string;
  category: string;
  signature: "Required" | "Optional";
  status: "Active" | "Inactive";
  createdAt?: string;
}

export const MOCK_FORMS: FormEntry[] = Array(100)
  .fill(null)
  .map((_, i) => ({
    id: (i + 1).toString(),
    name: `Consent Form ${i + 1} for Patients`,
    category: i % 2 === 0 ? "Informed Consent" : "Intake",
    signature: i % 3 === 0 ? "Optional" : "Required",
    status: i % 4 === 0 ? "Inactive" : "Active",
  }));

export const categoryOptions = [
  { value: "Informed Consent", label: "Informed Consent" },
  { value: "Client Intake", label: "Client Intake" },
  { value: "Release of Information", label: "Release of Information" },
  { value: "Treatment Agreement", label: "Treatment Agreement" },
  { value: "Safety Plan", label: "Safety Plan" },
  { value: "Discharge Summary", label: "Discharge Summary" },
  { value: "Custom Form", label: "Custom Form" },
];

export const MOCK_SECTIONS: FormSection[] = [
  {
    id: "1",
    title: "Agreement to Participate",
    type: "Fill-in-the-Blank",
    content:
      "I, {{CLIENT_FULL_NAME}}, hereby request and agree to participate in individual psychotherapy at Resilience Psychotherapy, Counselling, Consultation, and Research Corp. I understand that psychotherapy involves discussing my problems and difficulties with a psychotherapist who will provide a supportive, empathic environment.",
  },
  {
    id: "2",
    title: "Key Understandings",
    type: "Information Text (Read-Only)",
    content:
      "Information Text (Read-Only) • <p>I acknowledge that I have been informed of the nature and purpose of psychotherapy and that I understand what participation involves. I recognize that therapy is a collaborative process between myself and <strong>{{THERAPIST_FULL_NAME}}</strong>.</p>",
  },
  {
    id: "3",
    title: "Confidentiality",
    type: "Information Text (Read-Only)",
    content:
      "Information Text (Read-Only) • <p>All information related to my treatment (verbal or written) will be kept confidential except under the following circumstances: ...</p>",
  },
  {
    id: "4",
    title: "Fees for Service",
    type: "Information Text (Read-Only)",
    content:
      "Information Text (Read-Only) • <p>The fee is <strong>CA$205 plus GST per 50-minute session</strong> and is covered by IFHP. Payment will be claimed directly from the insurer.</p>",
  },
  {
    id: "5",
    title: "Cancellations and Missed Appointments",
    type: "Information Text (Read-Only)",
    content:
      "Information Text (Read-Only) • <p>I understand that I must provide at least <strong>48 hours' notice</strong> to cancel or change an appointment.</p>",
  },
  {
    id: "6",
    title: "Therapist Information",
    type: "Information Text (Read-Only)",
    content:
      "Information Text (Read-Only) • <p>The psychotherapy services outlined in this consent form will be provided by <strong>{{THERAPIST_FULL_NAME}}</strong>.</p>",
  },
  {
    id: "7",
    title: "Signatures",
    type: "Information Text (Read-Only)",
    content:
      "Information Text (Read-Only) • <p>By signing below, I, <strong>{{CLIENT_FULL_NAME}}</strong>, confirm that I have read and understood the contents of this consent form.</p>",
  },
  {
    id: "8",
    title: "Client Full Name",
    type: "Short Text",
    content: "Short Text",
    required: true,
  },
  {
    id: "9",
    title: "Date",
    type: "Date",
    content: "Date",
    required: true,
  },
  {
    id: "10",
    title: "Signature",
    type: "Signature",
    content: "Signature",
    required: true,
  },
];

export const fieldTypeOptions = [
  { value: "Heading (Read-only)", label: "Heading (Read-only)" },
  {
    value: "Information Text (Read-only)",
    label: "Information Text (Read-only)",
  },
  { value: "Short Text", label: "Short Text" },
  { value: "Long Text", label: "Long Text" },
  { value: "Dropdown", label: "Dropdown" },
  { value: "Radio Buttons", label: "Radio Buttons" },
  { value: "Single Checkbox", label: "Single Checkbox" },
  { value: "Multiple Checkboxes", label: "Multiple Checkboxes" },
  { value: "Date", label: "Date" },
  { value: "Signature", label: "Signature" },
  { value: "File Upload", label: "File Upload" },
];

export interface ProcessChecklistItem {
  description?: string;
  id: string;
  title: string;
  category: "Intake" | "Assessment" | "Ongoing" | "Discharge";
  required: boolean;
}

export interface ProcessChecklistTemplate {
  id: string;
  title: string;
  description: string;
  itemCount: number;
  items: ProcessChecklistItem[];
}

export const CHECKLIST_CATEGORIES = [
  "All Categories",
  "Intake",
  "Assessment",
  "Ongoing",
  "Discharge",
];

export const MOCK_PROCESS_CHECKLISTS: ProcessChecklistTemplate[] = Array(5)
  .fill(null)
  .map((_, i) => ({
    id: (i + 1).toString(),
    title: "Refugee clients",
    description:
      "This checklist will help manage client administration and track what is required to obtain their approval, as well as provide th...",
    itemCount: 12,
    items: [
      {
        id: "i1",
        title: "Client Contacted",
        category: "Intake",
        required: true,
      },
      {
        id: "i2",
        title: "Waiting for Referral",
        category: "Intake",
        required: true,
      },
      {
        id: "i3",
        title: "Referred by Doctor",
        category: "Ongoing",
        required: true,
      },
      {
        id: "i4",
        title: "Assessment Ongoing",
        category: "Assessment",
        required: true,
      },
      {
        id: "i5",
        title: "Report Completed",
        category: "Assessment",
        required: true,
      },
      {
        id: "i6",
        title: "Psychotherapy Extended",
        category: "Ongoing",
        required: true,
      },
      {
        id: "i7",
        title: "Psychotherapy Submitted for Approval",
        category: "Ongoing",
        required: true,
      },
      {
        id: "i8",
        title: "Psychotherapy Ongoing",
        category: "Ongoing",
        required: true,
      },
      { id: "i9", title: "Item 9", category: "Discharge", required: true },
      { id: "i10", title: "Item 10", category: "Intake", required: true },
      { id: "i11", title: "Item 11", category: "Assessment", required: true },
      { id: "i12", title: "Item 12", category: "Ongoing", required: true },
    ],
  }));

export const MOCK_CHECKLIST_ITEMS = [
  {
    id: "item-1",
    title: "Client Contacted",
    category: "Assessment",
    description: "Contact the client to confirm attendance",
    templates: [],
    required: true,
  },
  {
    id: "item-2",
    title: "Support Letter Provided",
    category: "Intake",
    description: "Provide support letter if required",
    templates: [],
    required: true,
  },
  {
    id: "item-3",
    title: "Waiting for Referral",
    category: "Discharge",
    description: "Awaiting referral from healthcare provider",
    templates: [],
    required: false,
  },
  {
    id: "item-4",
    title: "Referred by Doctor",
    category: "Assessment",
    description: "Client was referred by their doctor",
    templates: [],
    required: true,
  },
  {
    id: "item-5",
    title: "Assessment Ongoing",
    category: "Ongoing",
    description: "Assessment is currently in progress",
    templates: [],
    required: true,
  },
  {
    id: "item-6",
    title: "Support Letter Provided",
    category: "Assessment",
    description: "Provide support letter if required",
    templates: [],
    required: false,
  },
  {
    id: "item-7",
    title: "Waiting for Referral",
    category: "Discharge",
    description: "Awaiting referral from healthcare provider",
    templates: [],
    required: false,
  },
  {
    id: "item-8",
    title: "Referred by Doctor",
    category: "Assessment",
    description: "Client was referred by their doctor",
    templates: [],
    required: false,
  },
  {
    id: "item-9",
    title: "Assessment Ongoing",
    category: "Ongoing",
    description: "Assessment is currently in progress",
    templates: [],
    required: false,
  },
  {
    id: "item-10",
    title: "Client Contacted",
    category: "Intake",
    description: "Contact the client to confirm attendance",
    templates: [],
    required: false,
  },
  {
    id: "item-11",
    title: "Referred by Doctor",
    category: "Intake",
    description: "Client was referred by their doctor",
    templates: [],
    required: false,
  },
  {
    id: "item-12",
    title: "Support Letter Provided",
    category: "Intake",
    description: "Provide support letter if required",
    templates: [],
    required: true,
  },
];

export const MOCK_CLIENTS = [
  { value: "CL-001", label: "Zeeshan Khan (CL-2025-1464)" },
  { value: "CL-002", label: "Ahmad Jalil (CL-2025-1465)" },
  { value: "CL-003", label: "Sara Ahmed (CL-2025-1466)" },
  { value: "CL-004", label: "John Doe (CL-2025-1467)" },
];

export const ACCESS_LEVEL_OPTIONS = [
  { label: "Therapist Only", value: "Therapist Only" },
  { label: "Client Only", value: "Client Only" },
  { label: "Shared", value: "Shared" },
];

export const REPORT_SECTION_TYPE_OPTIONS = [
  { label: "None (Regular Section)", value: "None (Regular Section)" },
  { label: "Referral Reason", value: "Referral Reason" },
  { label: "Presenting Symptoms", value: "Presenting Symptoms" },
  { label: "Background History", value: "Background History" },
  { label: "Mental Status Exam", value: "Mental Status Exam" },
  { label: "Risk Assessment", value: "Risk Assessment" },
  { label: "Treatment Recommendations", value: "Treatment Recommendations" },
  { label: "Goals & Objectives", value: "Goals & Objectives" },
  { label: "Summary & Impressions", value: "Summary & Impressions" },
  { label: "Objective Findings", value: "Objective Findings" },
];

export const QUESTION_TYPE_OPTIONS = [
  { label: "Short Answer Text", value: "Short Answer Text" },
  { label: "Long Answer Text", value: "Long Answer Text" },
  { label: "Multiple Choice", value: "Multiple Choice" },
  { label: "Single Choice", value: "Single Choice" },
  { label: "Rating Scale", value: "Rating Scale" },
  { label: "Date", value: "Date" },
  { label: "Number", value: "Number" },
];
