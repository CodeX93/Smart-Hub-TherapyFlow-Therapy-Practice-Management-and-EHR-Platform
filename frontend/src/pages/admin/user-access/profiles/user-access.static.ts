import type {
  SupervisorAssignment,
  UserProfile,
} from "@/types/user-access-profiles.type";

export const USER_PROFILES_MOCK_DATA: UserProfile[] = Array(100)
  .fill(null)
  .map((_, i) => ({
    id: (i + 1).toString(),
    name: i % 2 === 0 ? "Abi cherian, MSW" : "Brooklyn Simmons",
    username: i % 2 === 0 ? "abi.cheria" : "brooklyn.simmons",
    email: i % 2 === 0 ? "aby@yahoo.com" : "michelle.rivera@example.com",
    roles: [["Therapist"], ["Supervisor"], ["Admin"]][i % 3],
    status: i % 4 === 0 ? "Inactive" : "Active",
    lastLogin: `${(i % 5) + 1} day ago`,
  }));

export const SUPERVISOR_ASSIGNMENTS_MOCK_DATA: SupervisorAssignment[] = Array(
  100,
)
  .fill(null)
  .map((_, i) => ({
    id: (i + 1).toString(),
    supervisor: ["Adnan Azad", "Dianne Russell", "Theresa Webb"][i % 3],
    therapist: ["Abi cherian, MSW", "Albert Flores", "Esther Howard"][i % 3],
    meetingFrequency: ["Biweekly", "Weekly", "Monthly"][i % 3] as
      | "Biweekly"
      | "Weekly"
      | "Monthly",
    assignmentType: "PRIMARY",
    lastMeeting: "Dec 18, 2025",
    nextMeeting: "Not set",
    assignedDate: "Dec 10, 2025",
    startDate: "Dec 10, 2025",
    endDate: "-",
    notes: "-",
    isActive: true,
  }));

export const FREQUENCY_OPTIONS = [
  { value: "All", label: "All frequencies" },
  { value: "Weekly", label: "Weekly" },
  { value: "Bi-weekly", label: "Bi-weekly" },
  { value: "Monthly", label: "Monthly" },
];

export const ROLE_OPTIONSS = [
  { value: "All", label: "All Roles" },
  { value: "Admin", label: "Admin" },
  { value: "Supervisor", label: "Supervisor" },
  { value: "Therapist", label: "Therapist" },
];

export const ROLE_OPTIONS = [
  {
    value: "Therapist",
    label: "Therapist",
    description:
      "Therapist can manage own clients, lorem ipsum dolor sit deliulte consedtucty",
    helpText: [
      "Manage own clients",
      "Session scheduling",
      "Clinical documentation",
      "Assessment tools",
      "Progress tracking",
    ],
  },
  {
    value: "Supervisor",
    label: "Supervisor",
    description:
      "Supervisor can supervise therapists, lorem ipsum dolor sit deliulte consedtucty",
    helpText: [
      "Supervise therapists",
      "Review clinical notes",
      "Assign cases",
      "Performance metrics",
    ],
  },
  {
    value: "Administrator",
    label: "Administrator",
    description:
      "Administrator have Full system access, lorem ipsum dolor sit deliulte consedtucty",
    helpText: [
      "System configuration",
      "User management",
      "Billing oversight",
      "Audit logs",
    ],
  },
  {
    value: "Marketing Manage",
    label: "Marketing Manager",
    description: "Lorem ipsum dolor sit deliulte consedtucty",
    helpText: ["Marketing campaigns", "Lead management", "Content moderation"],
  },
];

export const USER_ACCESS_TABS = [
  "User Profiles",
  "Supervisor Assignments",
] as const;
export type TabType = (typeof USER_ACCESS_TABS)[number];

export const timeOptions = Array.from({ length: 48 }).map((_, i) => {
  const hour = Math.floor(i / 2);
  const minute = i % 2 === 0 ? "00" : "30";
  const ampm = hour >= 12 ? "PM" : "AM";
  const displayHour = hour % 12 === 0 ? 12 : hour % 12;
  const time = `${displayHour.toString().padStart(2, "0")}:${minute} ${ampm}`;
  return { value: time, label: time };
});

export const roomOptions = [
  { id: "101", value: "101", label: "101 - Psych-A" },
  { id: "102", value: "102", label: "102 - Psych-B" },
  { id: "111", value: "111", label: "111 - Psych-C" },
];

export const initialDays = [
  {
    id: "monday",
    label: "Monday",
    active: true,
    slots: [
      {
        id: "1",
        startTime: "09:00 AM",
        endTime: "05:00 PM",
        type: "in-person" as const,
        roomIds: [],
      },
    ],
  },
  {
    id: "tuesday",
    label: "Tuesday",
    active: true,
    slots: [
      {
        id: "2",
        startTime: "09:00 AM",
        endTime: "05:00 PM",
        type: "in-person" as const,
        roomIds: [],
      },
    ],
  },
  {
    id: "wednesday",
    label: "Wednesday",
    active: true,
    slots: [
      {
        id: "3",
        startTime: "09:00 AM",
        endTime: "05:00 PM",
        type: "in-person" as const,
        roomIds: [],
      },
    ],
  },
  {
    id: "thursday",
    label: "Thursday",
    active: true,
    slots: [
      {
        id: "4",
        startTime: "09:00 AM",
        endTime: "05:00 PM",
        type: "in-person" as const,
        roomIds: [],
      },
    ],
  },
  {
    id: "friday",
    label: "Friday",
    active: true,
    slots: [
      {
        id: "5",
        startTime: "09:00 AM",
        endTime: "05:00 PM",
        type: "in-person" as const,
        roomIds: [],
      },
    ],
  },
  {
    id: "saturday",
    label: "Saturday",
    active: false,
    slots: [],
  },
  {
    id: "sunday",
    label: "Sunday",
    active: false,
    slots: [],
  },
];

export const frequencyOptions = [
  { value: "weekly", label: "Weekly" },
  { value: "bi-weekly", label: "Bi-weekly" },
  { value: "monthly", label: "Monthly" },
];

export const mockSupervisors = [
  { value: "1", label: "Dr. Jane Smith" },
  { value: "2", label: "Michael Brown, LCSW" },
  { value: "3", label: "Sarah Wilson, PhD" },
];

export const mockTherapists = [
  { value: "t1", label: "Abi cherian, MSW" },
  { value: "t2", label: "Brooklyn Simmons" },
  { value: "t3", label: "Guy Hawkins" },
];
