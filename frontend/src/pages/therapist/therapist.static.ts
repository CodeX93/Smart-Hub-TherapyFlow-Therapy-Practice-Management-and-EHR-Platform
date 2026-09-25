import { CalendarIcon } from "@/components/icons/commonIcons";
import type { AppointmentStatus } from "../../types/scheduling";
import { X, CheckCircle2, RotateCcw, AlertCircle, Loader2 } from "lucide-react";
import type { DashboardStaticContent } from "../../types/therapist-dashboard.type";
import {
  getSessionStatusBackgroundClass,
  getSessionStatusLabel,
  getSessionStatusTextClass,
} from "@/utils/sessionStatusPresentation";

export const Billings_STATIC_CONTENT = {
  overview: {
    totalInvoices: 6,
    totalBilled: "$450.00",
    totalPaid: "$300.00",
  },
  invoices: Array.from({ length: 100 }).map((_, i) => ({
    id: String(i + 1),
    client: [
      "Faizan Dilbaz",
      "Sarah Johnson",
      "Michael Chen",
      "Jessica Williams",
      "David Thompson",
    ][i % 5],
    therapist: "Ali Anjum",
    clientType: ["individual", "refugee", "couple", "mva", "xqrp"][i % 5] as
      | "individual"
      | "refugee"
      | "couple"
      | "mva"
      | "xqrp",
    date: "Dec 01, 2025",
    service: [
      "Physiotherapy (PR) session 60 minutes PSY-01",
      "Psychotherapy (SC) session 60 minutes Psy04",
      "Psychotherapy (SC) session 60 minutes Psy02",
    ][i % 3],
    amount: "150.00",
    paid: (i % 2 === 0 ? 150.0 : 0.0).toFixed(2),
    status: ["paid", "pending", "billed", "denied", "follow_up"][i % 5] as
      | "paid"
      | "pending"
      | "billed"
      | "denied"
      | "follow_up"
      | "refunded",
  })),
  overviewItems: [
    {
      label: "Outstanding Balance",
      value: "$350.00",
      subtext: "3 pending payments",
      icon: "dollar",
    },
    {
      label: "Total Collected",
      value: "$0.00",
      subtext: "0 paid invoices · 0 partial invoices",
      icon: "check",
    },
    {
      label: "Active Clients",
      value: "2",
      subtext: "With billing records",
      icon: "users",
    },
    {
      label: "Total Records",
      value: "3",
      subtext: "Billing records",
      icon: "list",
    },
  ],
  totalInvoices: 120,
  itemsPerPage: 12,
  totalPages: 10,
};
import type { Client, ClientFilters } from "../../types/client.type";

interface MockLibraryEntry {
  id: string;
  code: string;
  content: string;
  usedCount: number;
}

const baseClients: Client[] = [
  {
    id: "1",
    name: "Zeeshan Khan",
    referenceNumber: "123456",
    therapist: "Faizan Khan",
    lastSession: "Dec 07, 2025",
    sinceLastSession: "10 days ago",
    attachedDocs: 2,
    checklist: "---",
    clientId: "CL-2025-1481",
    age: 27,
    serviceType: "Psychotherapy",
    isMVAClient: true,
    insurance: "Blue Cross",
    dateOfBirth: "May 30, 1998",
    gender: "Male",
    maritalStatus: "Married",
    preferredLanguage: "English",
    phone: "0301-2345678",
    email: "email@example.com",
    address: "1901 Thornridge Cir. Shiloh, Hawaii 81063",
    emergencyContact: {
      name: "John Doe",
      phone: "0301-2345678",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Weekly",
    completedSessions: 12,
    referrerName: "Dr. Smith",
    referralNumber: "REF-2025-001",
    referralDate: "Jan 15, 2025",
    referralSource: "Physician",
    portalAccessEnabled: true,
  },
  {
    id: "2",
    name: "Sarah Johnson",
    referenceNumber: "123457",
    therapist: "Sarah",
    lastSession: "Dec 05, 2025",
    sinceLastSession: "12 days ago",
    attachedDocs: 3,
    checklist: "0/11 (0%)",
    clientId: "CL-2025-1482",
    age: 32,
    serviceType: "Counseling",
    isMVAClient: false,
    insurance: "Aetna",
    dateOfBirth: "Mar 15, 1992",
    gender: "Female",
    maritalStatus: "Single",
    preferredLanguage: "English",
    phone: "0301-2345679",
    email: "sarah.johnson@example.com",
    address: "123 Main St. New York, NY 10001",
    emergencyContact: {
      name: "Jane Johnson",
      phone: "0301-2345680",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Bi-weekly",
    completedSessions: 8,
    referrerName: "Dr. Brown",
    referralNumber: "REF-2025-002",
    referralDate: "Feb 01, 2025",
    referralSource: "Self-referral",
    portalAccessEnabled: true,
  },
  {
    id: "3",
    name: "Michael Chen",
    referenceNumber: "123458",
    therapist: "Michael",
    lastSession: "Dec 03, 2025",
    sinceLastSession: "14 days ago",
    attachedDocs: 1,
    checklist: "---",
    clientId: "CL-2025-1483",
    age: 45,
    serviceType: "Therapy",
    isMVAClient: true,
    insurance: "Cigna",
    dateOfBirth: "Jul 22, 1979",
    gender: "Male",
    maritalStatus: "Divorced",
    preferredLanguage: "English",
    phone: "0301-2345681",
    email: "michael.chen@example.com",
    address: "456 Oak Ave. Los Angeles, CA 90001",
    emergencyContact: {
      name: "Lisa Chen",
      phone: "0301-2345682",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Weekly",
    completedSessions: 20,
    referrerName: "Dr. Wilson",
    referralNumber: "REF-2025-003",
    referralDate: "Jan 10, 2025",
    referralSource: "Physician",
    portalAccessEnabled: false,
  },
  {
    id: "4",
    name: "Emily Rodriguez",
    referenceNumber: "123459",
    therapist: "Emily",
    lastSession: "Dec 01, 2025",
    sinceLastSession: "16 days ago",
    attachedDocs: 4,
    checklist: "---",
    clientId: "CL-2025-1484",
    age: 28,
    serviceType: "Psychotherapy",
    isMVAClient: false,
    insurance: "UnitedHealth",
    dateOfBirth: "Nov 08, 1996",
    gender: "Female",
    maritalStatus: "Married",
    preferredLanguage: "Spanish",
    phone: "0301-2345683",
    email: "emily.rodriguez@example.com",
    address: "789 Pine St. Miami, FL 33101",
    emergencyContact: {
      name: "Carlos Rodriguez",
      phone: "0301-2345684",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Weekly",
    completedSessions: 15,
    referrerName: "Dr. Martinez",
    referralNumber: "REF-2025-004",
    referralDate: "Mar 05, 2025",
    referralSource: "Physician",
    portalAccessEnabled: true,
  },
  {
    id: "5",
    name: "David Thompson",
    referenceNumber: "123460",
    therapist: "Faizan",
    lastSession: "Nov 28, 2025",
    sinceLastSession: "19 days ago",
    attachedDocs: 2,
    checklist: "---",
    clientId: "CL-2025-1485",
    age: 38,
    serviceType: "Counseling",
    isMVAClient: false,
    insurance: "Blue Cross",
    dateOfBirth: "Apr 12, 1986",
    gender: "Male",
    maritalStatus: "Married",
    preferredLanguage: "English",
    phone: "0301-2345685",
    email: "david.thompson@example.com",
    address: "321 Elm St. Chicago, IL 60601",
    emergencyContact: {
      name: "Mary Thompson",
      phone: "0301-2345686",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Bi-weekly",
    completedSessions: 6,
    referrerName: "Dr. Anderson",
    referralNumber: "REF-2025-005",
    referralDate: "Apr 20, 2025",
    referralSource: "Self-referral",
    portalAccessEnabled: true,
  },
  {
    id: "6",
    name: "Jessica Williams",
    referenceNumber: "123461",
    therapist: "Sarah",
    lastSession: "Nov 25, 2025",
    sinceLastSession: "22 days ago",
    attachedDocs: 5,
    checklist: "---",
    clientId: "CL-2025-1486",
    age: 29,
    serviceType: "Therapy",
    isMVAClient: true,
    insurance: "Aetna",
    dateOfBirth: "Sep 30, 1995",
    gender: "Female",
    maritalStatus: "Single",
    preferredLanguage: "English",
    phone: "0301-2345687",
    email: "jessica.williams@example.com",
    address: "654 Maple Dr. Seattle, WA 98101",
    emergencyContact: {
      name: "Robert Williams",
      phone: "0301-2345688",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Weekly",
    completedSessions: 10,
    referrerName: "Dr. Taylor",
    referralNumber: "REF-2025-006",
    referralDate: "May 15, 2025",
    referralSource: "Physician",
    portalAccessEnabled: false,
  },
  {
    id: "7",
    name: "Robert Martinez",
    referenceNumber: "123462",
    therapist: "Michael",
    lastSession: "Nov 22, 2025",
    sinceLastSession: "25 days ago",
    attachedDocs: 1,
    checklist: "---",
    clientId: "CL-2025-1487",
    age: 41,
    serviceType: "Psychotherapy",
    isMVAClient: false,
    insurance: "Cigna",
    dateOfBirth: "Jan 18, 1983",
    gender: "Male",
    maritalStatus: "Married",
    preferredLanguage: "English",
    phone: "0301-2345689",
    email: "robert.martinez@example.com",
    address: "987 Cedar Ln. Denver, CO 80201",
    emergencyContact: {
      name: "Patricia Martinez",
      phone: "0301-2345690",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Weekly",
    completedSessions: 18,
    referrerName: "Dr. Lee",
    referralNumber: "REF-2025-007",
    referralDate: "Jun 01, 2025",
    referralSource: "Physician",
    portalAccessEnabled: true,
  },
  {
    id: "8",
    name: "Amanda Davis",
    referenceNumber: "123463",
    therapist: "Emily",
    lastSession: "Nov 20, 2025",
    sinceLastSession: "27 days ago",
    attachedDocs: 3,
    checklist: "---",
    clientId: "CL-2025-1488",
    age: 35,
    serviceType: "Counseling",
    isMVAClient: false,
    insurance: "UnitedHealth",
    dateOfBirth: "Feb 14, 1989",
    gender: "Female",
    maritalStatus: "Divorced",
    preferredLanguage: "English",
    phone: "0301-2345691",
    email: "amanda.davis@example.com",
    address: "147 Birch St. Boston, MA 02101",
    emergencyContact: {
      name: "James Davis",
      phone: "0301-2345692",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Bi-weekly",
    completedSessions: 7,
    referrerName: "Dr. White",
    referralNumber: "REF-2025-008",
    referralDate: "Jul 10, 2025",
    referralSource: "Self-referral",
    portalAccessEnabled: true,
  },
  {
    id: "9",
    name: "James Wilson",
    referenceNumber: "123464",
    therapist: "Faizan",
    lastSession: "Nov 18, 2025",
    sinceLastSession: "29 days ago",
    attachedDocs: 2,
    checklist: "---",
    clientId: "CL-2025-1489",
    age: 52,
    serviceType: "Therapy",
    isMVAClient: true,
    insurance: "Blue Cross",
    dateOfBirth: "Dec 05, 1972",
    gender: "Male",
    maritalStatus: "Married",
    preferredLanguage: "English",
    phone: "0301-2345693",
    email: "james.wilson@example.com",
    address: "258 Spruce Ave. Phoenix, AZ 85001",
    emergencyContact: {
      name: "Susan Wilson",
      phone: "0301-2345694",
    },
    clientStage: "Active",
    clientType: "Individual",
    serviceFrequency: "Weekly",
    completedSessions: 25,
    referrerName: "Dr. Harris",
    referralNumber: "REF-2025-009",
    referralDate: "Aug 05, 2025",
    referralSource: "Physician",
    portalAccessEnabled: false,
  },
];

// Generate 90 clients for pagination testing
const generateClients = (): Client[] => {
  const clients: Client[] = [];
  const therapists = ["Faizan", "Sarah", "Michael", "Emily"];
  const checklistOptions = ["---", "0/11 (0%)", "5/11 (45%)", "11/11 (100%)"];

  const serviceTypes = ["Psychotherapy", "Counseling", "Therapy"];
  const clientTypes = ["Individual", "Couple", "Family"];
  const serviceFrequencies = ["Weekly", "Bi-weekly", "Monthly"];
  const genders = ["Male", "Female", "Other"];
  const maritalStatuses = ["Single", "Married", "Divorced", "Widowed"];
  const languages = ["English", "Spanish", "French"];
  const insuranceProviders = ["Blue Cross", "Aetna", "Cigna", "UnitedHealth"];
  const referralSources = ["Physician", "Self-referral", "Insurance", "Other"];

  for (let i = 0; i < 90; i++) {
    const baseClient = baseClients[i % baseClients.length];

    clients.push({
      ...baseClient,
      id: String(i + 1),
      referenceNumber: String(123456 + i),
      clientId: `CL-2025-${1481 + i}`,
      therapist: therapists[i % therapists.length],
      attachedDocs: Math.floor(Math.random() * 5),
      checklist: checklistOptions[i % checklistOptions.length],
      // Vary profile data for different clients
      age: 20 + (i % 50),
      serviceType: serviceTypes[i % serviceTypes.length],
      isMVAClient: i % 4 === 0, // Every 4th client is MVA
      insurance: insuranceProviders[i % insuranceProviders.length],
      dateOfBirth:
        baseClient.dateOfBirth || `Jan ${1 + (i % 28)}, ${1970 + (i % 30)}`,
      gender: genders[i % genders.length],
      maritalStatus: maritalStatuses[i % maritalStatuses.length],
      preferredLanguage: languages[i % languages.length],
      phone: `0301-${2345678 + i}`,
      email: `client${i + 1}@example.com`,
      address:
        baseClient.address || `${100 + i} Main St. City, State ${10000 + i}`,
      emergencyContact: {
        name: `Emergency Contact ${i + 1}`,
        phone: `0301-${2345678 + i + 1000}`,
      },
      clientStage: "Active",
      clientType: clientTypes[i % clientTypes.length],
      serviceFrequency: serviceFrequencies[i % serviceFrequencies.length],
      completedSessions: Math.floor(Math.random() * 50),
      referrerName: `Dr. ${
        ["Smith", "Brown", "Wilson", "Martinez", "Anderson"][i % 5]
      }`,
      referralNumber: `REF-2025-${String(1000 + i).padStart(3, "0")}`,
      referralDate: baseClient.referralDate || `Jan ${1 + (i % 28)}, 2025`,
      referralSource: referralSources[i % referralSources.length],
      portalAccessEnabled: i % 3 !== 0, // Every 3rd client has portal disabled
    });
  }

  return clients;
};

export const CLIENTS_STATIC_CONTENT = {
  clients: generateClients(),
  totalClients: 90,
  itemsPerPage: 9,
  totalPages: 10,
  availableTherapists: ["Faizan", "Sarah", "Michael", "Emily"],
};

export const SCHEDULING_STATIC_CONTENT = {
  dayOverview: [
    {
      label: "Today",
      value: "4",
      isText: "Wednesday, Dec 24, 2025",
    },
    {
      label: "This Month",
      value: "18",
      isText: "December",
    },
    {
      label: "Completed",
      value: "0",
    },
    {
      label: "Upcoming",
      value: "3",
    },
  ],
  weekOverview: [
    {
      label: "This Week",
      value: "4",
      isText: "Dec 21 - Dec 27, 2025",
    },
    {
      label: "This Month",
      value: "18",
      isText: "December",
    },
    {
      label: "Completed",
      value: "0",
    },
    {
      label: "Upcoming",
      value: "3",
    },
  ],
  monthOverview: [
    {
      label: "This Month",
      value: "4",
      isText: "December",
    },
    {
      label: "Completed",
      value: "0",
    },
    {
      label: "Upcoming",
      value: "3",
    },
  ],
  allSessions: [
    {
      label: "Total Sessions",
      value: "4",
    },
    {
      label: "Completed",
      value: "0",
    },
    {
      label: "Upcoming",
      value: "3",
    },
  ],
  allSessionsData: Array.from({ length: 100 }).map((_, i) => ({
    id: `session-${i + 1}`,
    clientName: [
      "Faizan Dilbaz",
      "Sarah Johnson",
      "Michael Chen",
      "Jessica Williams",
      "David Thompson",
    ][i % 5],
    ref: String(123456 + i),
    date: "Dec 01, 2025",
    time: "11:30 AM",
    service: [
      "Physiotherapy (PR) session",
      "Psychotherapy (SC) session",
      "Counseling session",
    ][i % 3],
    duration: "60 minutes",
    serviceCode: ["PSY-01", "Psy04", "Psy02"][i % 3],
    sessionType: ["Physiotherapy", "Psychotherapy", "Counseling"][i % 3],
    therapist: ["John Doe", "Jane Smith", "Robert Brown"][i % 3],
    room: ["102 - Psych-B", "103 - Psych-A", "104 - Couns-C"][i % 3],
    status: ["Scheduled", "Completed", "Cancelled", "Rescheduled", "No Show"][
      i % 5
    ] as "Scheduled" | "Completed" | "Cancelled" | "Rescheduled" | "No Show",

    dateTime: "Dec 01, 2025 - 11:30 AM",
    roomCode: "PSY-01",
    amount: "$100",
  })),
  dailyAppointments: [
    {
      id: "5",
      name: "Ahsan",
      time: "12:00 AM",
      startHour: 0,
      duration: 0.5,
      status: "scheduled",
      session: "Physiotherapy",
      service: "Psy04 - $100.00",
      room: "102 - Psych-B",
      // appointment dates (ISO) — adjust as needed
      date: "2026-01-04",
    },
    {
      id: "6",
      name: "Ahsan",
      time: "12:00 AM",
      startHour: 0,
      duration: 0.5,
      status: "scheduled",
      session: "Physiotherapy",
      service: "Psy04 - $100.00",
      room: "102 - Psych-B",
      // appointment dates (ISO) — adjust as needed
      date: "2026-01-04",
    },
    {
      id: "7",
      name: "Ahsan",
      time: "12:00 AM",
      startHour: 0,
      duration: 0.5,
      status: "scheduled",
      session: "Physiotherapy",
      service: "Psy04 - $100.00",
      room: "102 - Psych-B",
      // appointment dates (ISO) — adjust as needed
      date: "2026-01-04",
    },
    {
      id: "8",
      name: "Ahsan",
      time: "12:00 AM",
      startHour: 0,
      duration: 0.5,
      status: "scheduled",
      session: "Physiotherapy",
      service: "Psy04 - $100.00",
      room: "102 - Psych-B",
      // appointment dates (ISO) — adjust as needed
      date: "2026-01-04",
    },
    {
      id: "1",
      name: "Faizan Dilbaz",
      time: "10:30 AM",
      startHour: 10.5,
      duration: 1,
      status: "completed",
      session: "Physiotherapy",
      service: "Psy04 - $100.00",
      room: "102 - Psych-B",
      date: "2026-01-06",
    },
    {
      id: "2",
      name: "Ahtisham Khan",
      time: "1:00 PM",
      startHour: 13,
      duration: 1,
      status: "scheduled",
      session: "Physiotherapy",
      service: "Psy04 - $100.00",
      room: "102 - Psych-B",
      date: "2026-01-07",
    },
    {
      id: "3",
      name: "Ahtisham Khan",
      time: "1:30 PM",
      startHour: 13.5,
      duration: 1,
      status: "scheduled",
      session: "Physiotherapy",
      service: "Psy04 - $100.00",
      room: "102 - Psych-B",
      date: "2026-01-07",
    },
    {
      id: "4",
      name: "Nida",
      time: "3:30 PM",
      startHour: 15.5,
      duration: 1,
      status: "cancelled",
      session: "Physiotherapy",
      service: "Psy04 - $100.00",
      room: "102 - Psych-B",
      date: "2026-01-09",
    },
  ],
  sessionFilters: {
    statusOptions: [
      "All Statuses",
      "Scheduled",
      "Confirmed",
      "In Progress",
      "Completed",
      "Cancelled",
      "Rescheduled",
      "No-Show",
      "Overdue",
      "Pending",
    ],
    notesOptions: ["All Notes", "With Notes", "Without Notes"],
  },
};

export const sessionTypes = [
  { value: "Assessment", label: "Assessment" },
  { value: "Psychotherapy", label: "Psychotherapy" },
  { value: "Consultation", label: "Consultation" },
];

export const clients = [
  { value: "Arham (CL-2025-1485)", label: "Arham (CL-2025-1485)" },
  { value: "Sarah (CL-2025-1486)", label: "Sarah (CL-2025-1486)" },
  { value: "Michael (CL-2025-1487)", label: "Michael (CL-2025-1487)" },
];

export const therapists = [
  { value: "Ali Ahmed", label: "Ali Ahmed" },
  { value: "Ayesha Khan", label: "Ayesha Khan" },
  { value: "Hira Mani", label: "Hira Mani" },
];

export const services = [
  {
    value: "Psychotherapy (SC) session 60 Min $100.00",
    label: "Psychotherapy (SC) session",
    duration: "60 Min",
    price: "$100.00",
  },
  {
    value: "Psychotherapy (SC) session 90 Min $150.00",
    label: "Psychotherapy (SC) session",
    duration: "90 Min",
    price: "$150.00",
  },
];

export const timeSlots = [
  "09:00 AM",
  "10:00 AM",
  "11:00 AM",
  "12:00 PM",
  "01:00 PM",
  "03:00 PM",
  "04:00 PM",
];

export const rooms = [
  {
    value: "Room 101 - Main Wing",
    label: "Room 101 - Main Wing",
    available: true,
  },
  {
    value: "Room 102 - Main Wing",
    label: "Room 102 - Main Wing",
    available: true,
  },
  {
    value: "Room 103 - Main Wing",
    label: "Room 103 - Main Wing",
    available: false,
  },
  {
    value: "Room 104 - Main Wing",
    label: "Room 104 - Main Wing",
    available: true,
  },
  {
    value: "Room 105 - Main Wing",
    label: "Room 105 - Main Wing",
    available: true,
  },
  {
    value: "Room 106 - Main Wing",
    label: "Room 106 - Main Wing",
    available: false,
  },
];

export const statusConfig: Record<
  AppointmentStatus,
  {
    label: string;
    color: string;
    bgColor: string;
    icon: React.ElementType;
  }
> = {
  scheduled: {
    label: getSessionStatusLabel("scheduled"),
    color: getSessionStatusTextClass("scheduled"),
    bgColor: getSessionStatusBackgroundClass("scheduled"),
    icon: CalendarIcon,
  },
  confirmed: {
    label: getSessionStatusLabel("confirmed"),
    color: getSessionStatusTextClass("confirmed"),
    bgColor: getSessionStatusBackgroundClass("confirmed"),
    icon: CheckCircle2,
  },
  in_progress: {
    label: getSessionStatusLabel("in_progress"),
    color: getSessionStatusTextClass("in_progress"),
    bgColor: getSessionStatusBackgroundClass("in_progress"),
    icon: Loader2,
  },
  completed: {
    label: getSessionStatusLabel("completed"),
    color: getSessionStatusTextClass("completed"),
    bgColor: getSessionStatusBackgroundClass("completed"),
    icon: CheckCircle2,
  },
  cancelled: {
    label: getSessionStatusLabel("cancelled"),
    color: getSessionStatusTextClass("cancelled"),
    bgColor: getSessionStatusBackgroundClass("cancelled"),
    icon: X,
  },
  rescheduled: {
    label: getSessionStatusLabel("rescheduled"),
    color: getSessionStatusTextClass("rescheduled"),
    bgColor: getSessionStatusBackgroundClass("rescheduled"),
    icon: RotateCcw,
  },
  overdue: {
    label: getSessionStatusLabel("overdue"),
    color: getSessionStatusTextClass("overdue"),
    bgColor: getSessionStatusBackgroundClass("overdue"),
    icon: AlertCircle,
  },
  noshow: {
    label: getSessionStatusLabel("noshow"),
    color: getSessionStatusTextClass("noshow"),
    bgColor: getSessionStatusBackgroundClass("noshow"),
    icon: AlertCircle,
  },
};

// Risk Assessment Types and Data
// Defined in constants/riskAssessmentItems so scoring code and tests can import
// them without pulling in this module's UI dependencies.
export type { RiskItem } from "@/constants/riskAssessmentItems";
export { RISK_ASSESSMENT_ITEMS } from "@/constants/riskAssessmentItems";

export interface AssessmentOverview {
  totalAssigned: number;
  completed: number;
  inProgress: number;
  pending: number;
}

export interface AssessmentTemplate {
  id: string;
  title: string;
  description: string;
  category: string;
}

export interface AssessmentHistoryItem {
  id: string;
  title: string;
  description: string;
  status: "Pending" | "In Progress" | "Completed";
  /** Backend status key (e.g. completed, waiting_for_therapist). */
  rawStatus?: string;
  date?: string;
}

export const ASSESSMENT_OVERVIEW: AssessmentOverview = {
  totalAssigned: 0,
  completed: 0,
  inProgress: 0,
  pending: 0,
};

export const ASSESSMENT_TEMPLATES: AssessmentTemplate[] = [
  {
    id: "1",
    title: "Mental Health Assessment",
    description:
      "This assessment is designed to conduct a general mental health assessment to help understand the client's background and provide tr...",
    category: "clinical",
  },
  {
    id: "2",
    title: "Mental Health Assessment",
    description:
      "This assessment is designed to conduct a general mental health assessment to help understand the client's background and provide tr...",
    category: "clinical",
  },
];

export const ASSESSMENT_HISTORY: AssessmentHistoryItem[] = [
  {
    id: "1",
    title: "Mental Health Assessment",
    description:
      "This assessment is designed to conduct a general mental health assessment to help understand the client's background and provide tr...",
    status: "Pending",
  },
  {
    id: "2",
    title: "Mental Health Assessment",
    description:
      "This assessment is designed to conduct a general mental health assessment to help understand the client's background and provide tr...",
    status: "Pending",
  },
  {
    id: "3",
    title: "Mental Health Assessment",
    description:
      "This assessment is designed to conduct a general mental health assessment to help understand the client's background and provide tr...",
    status: "Pending",
  },
  {
    id: "4",
    title: "Mental Health Assessment",
    description:
      "This assessment is designed to conduct a general mental health assessment to help understand the client's background and provide tr...",
    status: "Pending",
  },
];

// Assessment Taking Flow Types

export type QuestionType =
  | "text"
  | "textarea"
  | "date"
  | "radio"
  | "voice"
  | "checkbox"
  | "rating";

export interface AssessmentQuestion {
  id: string;
  type: QuestionType;
  label: string;
  placeholder?: string;
  options?: string[]; // For radio/checkbox
  required?: boolean;
}

export interface AssessmentSection {
  id: string;
  title: string;
  questions: AssessmentQuestion[];
}

export type AssessmentAnswer = string | Date | string[] | boolean | undefined;

export const MOCK_ASSESSMENT_CONTENT: AssessmentSection[] = [
  {
    id: "referral-info",
    title: "Referral Information",
    questions: [
      {
        id: "q1",
        type: "text",
        label: "Referring professional's name",
        placeholder: "Enter name",
      },
      {
        id: "q2",
        type: "text",
        label: "Full address of referring clinic",
        placeholder: "Enter address",
      },
      {
        id: "q3",
        type: "voice",
        label: "Reason for referral",
        placeholder: "Type or record reason",
      },
      { id: "q4", type: "date", label: "Referral date" },
      { id: "q5", type: "date", label: "Assessment date" },
      {
        id: "q6",
        type: "text",
        label: "Clinical interview duration",
        placeholder: "e.g. 50 mins",
      },
      {
        id: "q7",
        type: "radio",
        label: "Assessment session format (in-person, video, phone)",
        options: ["In-person", "Video", "Phone"],
      },
    ],
  },
  {
    id: "informed-consent",
    title: "Informed Consent",
    questions: [
      {
        id: "ic1",
        type: "radio",
        label: "Consent obtained for assessment?",
        options: ["Yes", "No"],
      },
      {
        id: "ic2",
        type: "textarea",
        label: "Notes on consent",
        placeholder: "Any specific notes...",
      },
    ],
  },
  {
    id: "sources-info",
    title: "Sources of Information",
    questions: [
      {
        id: "si1",
        type: "textarea",
        label: "List all sources of information",
        placeholder: "e.g. Clinical interview, files...",
      },
    ],
  },
  {
    id: "background-info",
    title: "Relevant Background Information",
    questions: [
      {
        id: "bg1",
        type: "textarea",
        label: "Personal History",
        placeholder: "Enter details...",
      },
      {
        id: "bg2",
        type: "textarea",
        label: "Family History",
        placeholder: "Enter details...",
      },
    ],
  },
  {
    id: "medical-history",
    title: "Medical History - Chronic Conditions",
    questions: [
      {
        id: "mh1",
        type: "textarea",
        label: "Current medical conditions",
        placeholder: "List conditions...",
      },
    ],
  },
  {
    id: "trauma-history",
    title: "Trauma and Migration History",
    questions: [
      {
        id: "tm1",
        type: "textarea",
        label: "Trauma history details",
        placeholder: "Describe...",
      },
    ],
  },
  {
    id: "presenting-concerns",
    title: "Presenting Concerns",
    questions: [
      {
        id: "pc1",
        type: "textarea",
        label: "Primary concerns",
        placeholder: "Describe main issues...",
      },
      {
        id: "pc2",
        type: "textarea",
        label: "History of presenting problem",
        placeholder: "Duration, triggers...",
      },
    ],
  },
  {
    id: "bdi",
    title: "Beck Depression Inventory",
    questions: [
      {
        id: "bdi1",
        type: "radio",
        label: "Sadness",
        options: [
          "0 - I do not feel sad",
          "1 - I feel sad",
          "2 - I am sad all the time",
          "3 - I am so sad I can't stand it",
        ],
      },
      {
        id: "bdi2",
        type: "radio",
        label: "Pessimism",
        options: [
          "0 - I am not discouraged",
          "1 - I feel discouraged",
          "2 - I feel I have nothing to look forward to",
          "3 - I feel the future is hopeless",
        ],
      },
    ],
  },
  {
    id: "bai",
    title: "Beck Anxiety Inventory BAI",
    questions: [
      {
        id: "bai1",
        type: "radio",
        label: "Numbness or tingling",
        options: ["Not at all", "Mildly", "Moderately", "Severely"],
      },
      {
        id: "bai2",
        type: "radio",
        label: "Feeling hot",
        options: ["Not at all", "Mildly", "Moderately", "Severely"],
      },
    ],
  },
  {
    id: "ptsd",
    title: "PTSD CheckList - Civilian Version",
    questions: [
      {
        id: "ptsd1",
        type: "radio",
        label:
          "Repeated, disturbing memories, thoughts, or images of a stressful experience?",
        options: [
          "Not at all",
          "A little bit",
          "Moderately",
          "Quite a bit",
          "Extremely",
        ],
      },
      {
        id: "ptsd2",
        type: "radio",
        label: "Repeated, disturbing dreams of a stressful experience?",
        options: [
          "Not at all",
          "A little bit",
          "Moderately",
          "Quite a bit",
          "Extremely",
        ],
      },
    ],
  },
  {
    id: "assessment-summary",
    title: "Assessment summary",
    questions: [
      {
        id: "as1",
        type: "textarea",
        label: "Clinical Impression",
        placeholder: "Summary of findings...",
      },
    ],
  },
  {
    id: "intervention-action",
    title: "Intervention/action",
    questions: [
      {
        id: "ia1",
        type: "textarea",
        label: "Planned interventions",
        placeholder: "List interventions...",
      },
    ],
  },
  {
    id: "treatment-time",
    title: "Treatment time frame",
    questions: [
      {
        id: "ttf1",
        type: "text",
        label: "Estimated duration",
        placeholder: "e.g. 12 weeks",
      },
    ],
  },
  {
    id: "treatment-plan",
    title: "Treatment plan",
    questions: [
      {
        id: "tp1",
        type: "textarea",
        label: "Treatment goals",
        placeholder: "Goal 1...",
      },
    ],
  },
];
export const DOCUMENTS_STATIC_CONTENT = {
  documents: [
    {
      id: "1",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: false,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
    {
      id: "2",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: true,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
    {
      id: "3",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: false,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
    {
      id: "4",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: true,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
    {
      id: "5",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: false,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
    {
      id: "6",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: false,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
    {
      id: "7",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: false,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
    {
      id: "8",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: false,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
    {
      id: "9",
      name: "df742495-c1ef-494c-8818-f758c.pdf",
      shareInPortal: false,
      size: "921.09 KB",
      uploadedDate: "Dec 07, 2025",
    },
  ],
  totalDocuments: 90,
  itemsPerPage: 9,
  totalPages: 10,
};

export const CHECKLISTS_STATIC_CONTENT = {
  templates: [
    { id: "1", title: "Refugee Clients", totalItems: 11 },
    { id: "2", title: "Initial Assessment", totalItems: 8 },
    { id: "3", title: "Discharge Planning", totalItems: 15 },
    { id: "4", title: "Insurance Verification", totalItems: 5 },
    { id: "5", title: "Safety Plan", totalItems: 9 },
  ],
};

export const DASHBOARD_STATIC_CONTENT: DashboardStaticContent = {
  overviewItems: [
    {
      label: "Active Clients",
      value: "2",
      subtext: "of 4 total",
      icon: "users",
    },
    {
      label: "Today's Sessions",
      value: "2",
      subtext: "scheduled for today",
      icon: "calendar",
    },
    {
      label: "Pending Tasks",
      value: "2",
      subtext: "of 0 total tasks",
      icon: "tasks",
    },
    {
      label: "Billing Overview",
      value: "$12",
      subtext: "Collected $3 this month",
      icon: "billing",
    },
  ],
  sessions: {
    previous: [
      {
        id: "prev-1",
        patientName: "Faizan Dilbaz",
        date: "Dec 16, 2025",
        sessionId: "PSY - 04",
        status: "Completed",
      },
      {
        id: "prev-2",
        patientName: "Faizan Dilbaz",
        date: "Dec 16, 2025",
        sessionId: "PSY - 04",
        status: "Completed",
      },
      {
        id: "prev-3",
        patientName: "Faizan Dilbaz",
        date: "Dec 16, 2025",
        sessionId: "PSY - 04",
        status: "Completed",
      },
      {
        id: "prev-4",
        patientName: "Faizan Dilbaz",
        date: "Dec 16, 2025",
        sessionId: "PSY - 04",
        status: "Completed",
      },
    ],
    upcoming: [
      {
        id: "up-1",
        patientName: "Aiden Brooks",
        date: "August 15, 2023",
        time: "2:30 PM",
        sessionId: "PSY - 09",
        status: "Pending",
      },
      {
        id: "up-2",
        patientName: "Olivia Davis",
        date: "March 15, 2024",
        time: "3:45 PM",
        sessionId: "PSY - 09",
        status: "Pending",
      },
      {
        id: "up-3",
        patientName: "Mia Williams",
        date: "April 10, 2024",
        time: "12:00 PM",
        sessionId: "PSY - 09",
        status: "Pending",
      },
      {
        id: "up-4",
        patientName: "Noah Brown",
        date: "December 5, 2023",
        time: "10:15 AM",
        sessionId: "PSY - 09",
        status: "Pending",
      },
    ],
    overdue: [
      {
        id: "over-1",
        patientName: "Ahtisham Khan",
        date: "Dec 16, 2025",
        time: "10:30 AM",
        sessionId: "PSY - 04",
        status: "Overdue",
        overdueDays: "21 days overdue",
      },
      {
        id: "over-2",
        patientName: "Nida Munawar",
        date: "Dec 16, 2025",
        time: "10:30 AM",
        sessionId: "PSY - 04",
        status: "Overdue",
        overdueDays: "21 days overdue",
      },
      {
        id: "over-3",
        patientName: "John Doe",
        date: "Dec 16, 2025",
        time: "10:30 AM",
        sessionId: "PSY - 04",
        status: "Overdue",
        overdueDays: "21 days overdue",
      },
      {
        id: "over-4",
        patientName: "Faizan Dilbaz",
        date: "Dec 16, 2025",
        time: "10:30 AM",
        sessionId: "PSY - 04",
        status: "Overdue",
        overdueDays: "21 days overdue",
      },
    ],
  },
  deadlines: [
    {
      id: "dl-1",
      title: "Client Medication Review",
      date: "January 16, 2026",
      clientName: "Emily Johnson",
      priority: "Low",
      status: "Pending",
    },
    {
      id: "dl-2",
      title: "Care Coordinator",
      date: "January 17, 2026",
      clientName: "Michael Smith",
      priority: "Medium",
      status: "In Progress",
    },
    {
      id: "dl-3",
      title: "Treatment Executor",
      date: "January 18, 2026",
      clientName: "Sarah Brown",
      priority: "High",
      status: "Overdue",
    },
    {
      id: "dl-4",
      title: "Care Organizer",
      date: "January 19, 2026",
      clientName: "David Wilson",
      priority: "Low",
      status: "Completed",
    },
  ],
  recentTasks: [
    {
      id: "rt-1",
      title: "Heart Health Strategy",
      priority: "Low",
      clientName: "Alex Thompson",
      time: "11:00 AM",
      status: "Pending",
    },
    {
      id: "rt-2",
      title: "Regular Check-up",
      priority: "Medium",
      clientName: "Jamie Parker",
      time: "10:30 AM",
      status: "Pending",
    },
    {
      id: "rt-3",
      title: "Regular Check-ups: Ensure co...",
      priority: "High",
      clientName: "Taylor Morgan",
      time: "11:00 AM",
      status: "Pending",
    },
    {
      id: "rt-4",
      title: "Patient Resources: Educate o...",
      priority: "Urgent",
      clientName: "Jordan Lee",
      time: "11:00 AM",
      status: "Overdue",
    },
    {
      id: "rt-5",
      title: "Regular Check-ups: Schedule...",
      priority: "Low",
      clientName: "Casey Rivera",
      time: "11:00 AM",
      status: "Pending",
    },
    {
      id: "rt-6",
      title: "Regular Check-ups: Schedule...",
      priority: "Low",
      clientName: "Casey Rivera",
      time: "11:00 AM",
      status: "Pending",
    },
    {
      id: "rt-7",
      title: "Regular Check-ups: Schedule...",
      priority: "Low",
      clientName: "Casey Rivera",
      time: "11:00 AM",
      status: "Pending",
    },
  ],
};

export const SCHEDULING_FILTERS = {
  therapistOptions: [
    { value: "", label: "All Therapists" },
    { value: "Ahtisham Khan", label: "Ahtisham Khan" },
    { value: "Nida Munawar", label: "Nida Munawar" },
    { value: "John Doe", label: "John Doe" },
    { value: "Faizan Dilbaz", label: "Faizan Dilbaz" },
  ],
  statusOptions: [
    { value: "", label: "All Statuses" },
    { value: "scheduled", label: getSessionStatusLabel("scheduled") },
    { value: "confirmed", label: getSessionStatusLabel("confirmed") },
    { value: "in_progress", label: getSessionStatusLabel("in_progress") },
    { value: "completed", label: getSessionStatusLabel("completed") },
    { value: "cancelled", label: getSessionStatusLabel("cancelled") },
    { value: "rescheduled", label: getSessionStatusLabel("rescheduled") },
    { value: "noshow", label: getSessionStatusLabel("noshow") },
    { value: "overdue", label: getSessionStatusLabel("overdue") },
    { value: "pending", label: getSessionStatusLabel("pending") },
  ],
  serviceCodeOptions: [
    { value: "", label: "All Services" },
    { value: "Psy04", label: "Psy04 - Psychotherapy (SC) session 60 minutes" },
    { value: "Psy01", label: "Psy01 - Psychotherapy (SC) session 60 minutes" },
    { value: "Psy02", label: "Psy02 - Psychotherapy (SC) session 60 minutes" },
    { value: "Psy03", label: "Psy03 - Psychotherapy (SC) session 60 minutes" },
  ],
};

export const BILLING_FILTERS = {
  paymentStatusOptions: [
    { value: "", label: "All Payment Stages" },
    { value: "pending", label: "Pending" },
    { value: "paid", label: "Paid" },
    { value: "partial", label: "Partial" },
    { value: "failed", label: "Failed" },
    { value: "refunded", label: "Refunded" },
  ],
  billingStatusOptions: [
    { value: "", label: "All Billing Stages" },
    { value: "pending", label: "Pending" },
    { value: "partial", label: "Partial" },
    { value: "paid", label: "Paid" },
    { value: "denied", label: "Denied" },
  ],
  paymentMethodOptions: [
    { value: "", label: "All Methods" },
    { value: "cash", label: "Cash" },
    { value: "check", label: "Check" },
    { value: "credit_card", label: "Credit Card" },
    { value: "debit_card", label: "Debit Card" },
    { value: "insurance", label: "Insurance" },
    { value: "bank_transfer", label: "Bank Transfer" },
    { value: "online_payment", label: "Online Payment" },
    { value: "credit_balance", label: "Credit Balance" },
  ],
  clientTypeFilterOptions: [
    { value: "", label: "All Client Types" },
    { value: "individual", label: "Individual" },
    { value: "couple", label: "Couple" },
    { value: "family", label: "Family" },
    { value: "group", label: "Group" },
    { value: "refugee", label: "Refugee" },
    { value: "mva", label: "MVA" },
  ],
  sessionTypeFilterOptions: [
    { value: "", label: "All Session Types" },
    { value: "in-person", label: "In Person" },
    { value: "online", label: "Online" },
  ],
};

// Quick Filters Data
export const QUICK_FILTERS = [
  {
    id: "has-portal-access",
    label: "Has portal access",
    key: "hasPortalAccess" as keyof ClientFilters,
  },
  {
    id: "has-pending-tasks",
    label: "Has pending tasks",
    key: "hasPendingTasks" as keyof ClientFilters,
  },
  {
    id: "no-sessions",
    label: "No sessions",
    key: "noSessions" as keyof ClientFilters,
  },
];

// Checklist Items Mock Data
export const CHECKLIST_ITEMS = [
  { id: "client_contacted", label: "Client Contacted (intake)" },
  { id: "support_letter", label: "Support Letter Provided (intake)" },
  { id: "waiting_referral", label: "Waiting for Referral (intake)" },
  { id: "referred_doctor", label: "Referred by Doctor (intake)" },
  { id: "assessment_ongoing", label: "Assessment Ongoing (assessment)" },
  { id: "report_completed", label: "Report Completed (intake)" },
  { id: "psychotherapy_extended", label: "Psychotherapy Extended (ongoing)" },
  {
    id: "psychotherapy_submitted",
    label: "Psychotherapy Submitted for Approval (ongoing)",
  },
  { id: "psychotherapy_ongoing", label: "Psychotherapy Ongoing (ongoing)" },
];

export const allClientOptions = [
  { label: "All Clients", value: "all" },
  { label: "Active", value: "Active" },
  { label: "Inactive", value: "Inactive" },
];

export const clientStageOptions = [
  { label: "All Stages", value: "all" },
  { label: "Intake", value: "intake" },
  { label: "Assessment", value: "assessment" },
  { label: "Psychotherapy", value: "psychotherapy" },
  { label: "Closed", value: "closed" },
];

export const alltherapistOptions = [
  { label: "All Therapists", value: "all" },
  { label: "Cameron Williamson", value: "Cameron Williamson" },
  { label: "Albert Flores", value: "Albert Flores" },
  { label: "Esther Howard", value: "Esther Howard" },
  { label: "Bessie Cooper", value: "Bessie Cooper" },
];

export const clientTypeOptions = [
  { label: "All Types", value: "all" },
  { label: "Refugee", value: "Refugee" },
  { label: "VQRP", value: "VQRP" },
  { label: "MVA", value: "MVA" },
  { label: "Personal", value: "Personal" },
];

export const templateOptions = [
  { label: "All Templates", value: "all" },
  { label: "Refugee Clients", value: "Refugee Clients" },
  { label: "Standard", value: "Standard" },
];

export const paymentMethods = [
  { value: "cash", label: "Cash" },
  { value: "check", label: "Check" },
  { value: "credit_card", label: "Credit Card" },
  { value: "debit_card", label: "Debit Card" },
  { value: "insurance", label: "Insurance" },
  { value: "bank_transfer", label: "Bank Transfer" },
  { value: "online_payment", label: "Online Payment" },
  { value: "credit_balance", label: "Credit Balance" },
];

export const BILLING_STATUS_OPTIONS = [
  { value: "paid", label: "Paid" },
  { value: "denied", label: "Denied" },
];

export const discountOptions = [
  { value: "no_discount", label: "No Discount" },
  { value: "percentage", label: "Percentage (%)" },
  { value: "fixed", label: "Fixed Amount ($)" },
];

export const MOCK_LIBRARY_DATA: MockLibraryEntry[] = [
  {
    id: "1",
    code: "PTSDG1",
    content:
      "Reduce the frequency and emotional intensity of intrusive recollections.",
    usedCount: 1,
  },
  {
    id: "2",
    code: "PTSDG2",
    content:
      "Develop and implement effective grounding techniques for managing dissociation.",
    usedCount: 3,
  },
  {
    id: "3",
    code: "PTSDG3",
    content:
      "Identify and challenge maladaptive schemas related to safety and trust.",
    usedCount: 1,
  },
  {
    id: "4",
    code: "PTSDG4",
    content: "Increase engagement in safe, health-promoting social activities.",
    usedCount: 0,
  },
  {
    id: "5",
    code: "PTSDG5",
    content:
      "Improve emotional regulation skills during trauma processing sessions.",
    usedCount: 2,
  },
  {
    id: "6",
    code: "PTSDG6",
    content:
      "Additional therapeutic observation content for testing scroll and list.",
    usedCount: 5,
  },
];

export const SECTION_RESPONSES = [
  { id: 1, title: "Referral", questionCount: 4 },
  { id: 2, title: "Inform Consent", questionCount: 7 },
  { id: 3, title: "Source of Information", questionCount: 5 },
  { id: 4, title: "Behavioral Observations", questionCount: 8 },
  { id: 5, title: "Risk Factors", questionCount: 7 },
  { id: 6, title: "Relevant Background Information", questionCount: 4 },
  { id: 7, title: "Presenting Concerns", questionCount: 8 },
  { id: 8, title: "Beck Depression Inventory", questionCount: 21 },
  { id: 9, title: "Beck Anxiety Inventory", questionCount: 21 },
  { id: 10, title: "PTSD Checklist", questionCount: 17 },
  { id: 11, title: "Assessment Summary", questionCount: 1 },
  { id: 12, title: "Treatment Plan", questionCount: 1 },
];

export const staticText = `    <h1>INFORMED CONSENT</h1>
    <p>During the assessment session, Mr. Jameel was informed of the purpose of the mental health evaluation and the limits of confidentiality. He was advised that the resulting report would include personal information, the assessor’s clinical impressions, a recommended treatment plan, and an outline of key therapeutic interventions. Mr. Jameel was encouraged to ask questions about the assessment process and the release of information prior to signing the consent form. He was also informed that the completed report would be submitted to Medavie Blue Cross – Interim Federal Health Program (IFHP), although the specific purpose of the submission was not explained.</p>
    <br/>
    <h1>SOURCES OF INFORMATION</h1>
    <p>The information included in this report was derived from multiple sources. A semi-structured clinical interview was conducted to explore Mr. Jameel’s current and historical mental health functioning, as well as relevant aspects of his social background. The interview process also allowed for pertinent behavioral observations to be made. Psychological testing was administered, and the assessment battery included the Beck Depression Inventory–II (BDI-II).</p>
    <br/>
    <h1>RELEVANT BACKGROUND INFORMATION</h1>
    <p>Mr. Jameel described his early family environment as "revcsaxs," which shaped his initial sense of safety, identity, and emotional expression. Information not available regarding specific trauma or adverse experiences. The decision to leave his country was based on "eracxs." Since arriving in the host country, information not available regarding his post-arrival experience. These experiences have continued to influence his mental health and adjustment in the current context.</p>`;
