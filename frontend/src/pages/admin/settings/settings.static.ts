import type {
  OptionCategory,
  ServicePrice,
  ServiceVisibility,
  TherapyRoom,
  PracticeConfig,
} from "@/types/settings.types";

export const mockOptionCategories: OptionCategory[] = [
  {
    id: "1",
    name: "Client Sources",
    code: "client_source",
    options: [
      { id: "s1", label: "Referral", value: "referral" },
      { id: "s2", label: "Google", value: "google" },
      { id: "s3", label: "Facebook", value: "facebook" },
    ],
  },
  {
    id: "2",
    name: "Client Stage",
    code: "client_stage",
    options: [
      { id: "st1", label: "Lead", value: "lead" },
      { id: "st2", label: "Active", value: "active" },
    ],
  },
  { id: "3", name: "Client Status", code: "client_status", options: [] },
  { id: "4", name: "Client Types", code: "client_types", options: [] },
  { id: "5", name: "Education Levels", code: "education_levels", options: [] },
  {
    id: "6",
    name: "Employment Status",
    code: "employment_status",
    options: [],
  },
  { id: "7", name: "Document Categories", code: "doc_categories", options: [] },
  { id: "8", name: "Appointment Types", code: "app_types", options: [] },
  {
    id: "9",
    name: "Cancellation Reasons",
    code: "cancel_reasons",
    options: [],
  },
  { id: "10", name: "Marketing Channels", code: "marketing", options: [] },
  { id: "11", name: "Referral Partners", code: "partners", options: [] },
  { id: "12", name: "Service Areas", code: "service_areas", options: [] },
];

export const mockServicePrices: ServicePrice[] = Array.from(
  { length: 100 },
  (_, i) => ({
    id: `s-${i + 1}`,
    code: "Psy01",
    name: "Psychotherapy (PR) session",
    duration: "60 minutes",
    price: "$150.00",
  }),
);

export const mockServiceVisibility: ServiceVisibility[] = Array.from(
  { length: 100 },
  (_, i) => ({
    id: `v-${i + 1}`,
    code: "Psy01",
    name: "Psychotherapy (PR) session",
    duration: "60 minutes",
    price: "$150.00",
    therapistVisible: i % 3 !== 0,
    clientPortalVisible: true,
  }),
);

export const mockTherapyRooms: TherapyRoom[] = Array.from(
  { length: 100 },
  (_, i) => ({
    id: `r-${i + 1}`,
    number: (101 + i).toString(),
    name: `Psych-${String.fromCharCode(65 + (i % 6))}`,
    capacity: 3,
    isActive: i % 2 === 0,
  }),
);

export const mockPracticeConfig: PracticeConfig = {
  name: "",
  subtitle: "",
  description: "",
  address: "",
  phone: "",
  email: "",
  website: "",
  timezone: "Etc/GMT",
};
