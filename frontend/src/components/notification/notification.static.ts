import type { NotificationTemplate } from "@/types/notification";

export const NOTIFICATION_STATIC_CONTENT = {
  notifications: [
    {
      id: "1",
      title: "Lorem ipsum dolor sit amet",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: false,
    },
    {
      id: "2",
      title: "Duis aute irure dolor in reprehenderit in",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: false,
      dateGroup: "Yesterday",
    },
    {
      id: "3",
      title: "Sed ut perspiciatis unde omnis",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: true,
    },
    {
      id: "4",
      title: "Nemo enim ipsam voluptatem",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: true,
      dateGroup: "Dec 09",
    },
    {
      id: "5",
      title: "Ut enim ad minima veniam",
      description:
        "Consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
      timestamp: "12:13 PM",
      isRead: true,
    },
  ],
};

export const MOCK_TEMPLATES: NotificationTemplate[] = [
  {
    id: "1",
    title: "New Client Created",
    tag: "client_created",
    subject: "SmartHub - New Client: {{fullName}}",
    description:
      "A new client {{fullName}} has been added to the system. Please review their profile and ensure all intake requirements are met.",
    createdDate: "5 months ago",
    priority: "High",
  },
  {
    id: "2",
    title: "Client Assigned",
    tag: "client_assigned",
    subject: "SmartHub - Client Assignment: {{fullName}}",
    description:
      "Client {{fullName}} has been assigned to therapist {{assignedTherapist}}. Please coordinate with the client to schedule initial sessions.",
    createdDate: "5 months ago",
    priority: "Medium",
  },
  {
    id: "3",
    title: "Task Assigned",
    tag: "task_assigned",
    subject: "SmartHub - New Task: {{title}}",
    description:
      "You have been assigned a new task: {{title}} for client {{clientName}}. Priority: {{priority}}",
    createdDate: "5 months ago",
    priority: "Medium",
  },
  {
    id: "4",
    title: "Session Overdue",
    tag: "session_overdue",
    subject: "SmartHub - Session Status Update Required",
    description:
      "Session for client {{clientName}} on {{sessionDate}} requires status update. Please mark as completed or cancelled.",
    createdDate: "5 months ago",
    priority: "Urgent",
  },
];

export const NOTIFICATION_SCENARIOS = [
  {
    text: 'New client assignment: Event = "Client Assigned to Therapist"',
    highlight: '"Client Assigned to Therapist"',
  },
  {
    text: 'Document review needed: Event = "Document Needs Supervisor Review"',
    highlight: '"Document Needs Supervisor Review"',
  },
  {
    text: 'Task overdue: Event = "Task Overdue", Priority = "High"',
    type: "multi",
    parts: [
      { prefix: "Task overdue: Event = ", text: '"Task Overdue"' },
      { prefix: ", Priority =", text: '"High"' },
    ],
  },
  {
    text: 'Payment issues: Event = "Payment Overdue"',
    highlight: '"Payment Overdue"',
  },
  {
    text: 'Assessment completion: Event = "Assessment Completed"',
    highlight: '"Assessment Completed"',
  },
];

export const EVENT_TYPE_FALLBACK_KEYS = [
  "client_created",
  "client_assigned",
  "session_scheduled",
  "session_rescheduled",
  "session_cancelled",
  "session_overdue",
  "task_assigned",
  "task_overdue",
  "checklist_assigned",
  "checklist_completed",
  "form_assigned",
  "form_completed",
  "document_uploaded",
  "assessment_assigned",
  "assessment_completed",
];

function humanizeTriggerKey(value: string): string {
  return value
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

export const EVENT_TYPE_OPTIONS = EVENT_TYPE_FALLBACK_KEYS.map((value) => {
  let group = "Other Events";
  if (value.startsWith("client_")) group = "Client Events";
  if (value.startsWith("session_")) group = "Session Events";
  if (value.startsWith("task_")) group = "Task Events";
  if (value.startsWith("checklist_")) group = "Checklist Events";
  if (value.startsWith("form_")) group = "Form Events";
  if (value.startsWith("document_")) group = "Document Events";
  if (value.startsWith("assessment_")) group = "Assessment Events";

  return {
    label: humanizeTriggerKey(value),
    value,
    group,
  };
});

export const PRIORITY_OPTIONS = [
  { label: "High", value: "HIGH" },
  { label: "Medium", value: "MEDIUM" },
  { label: "Low", value: "LOW" },
  { label: "Urgent", value: "URGENT" },
];

export const ENTITY_TYPE_FALLBACK_KEYS = [
  "CLIENT",
  "SESSION",
  "TASK",
  "DOCUMENT",
  "BILLING",
  "PAYMENT",
  "NOTIFICATION",
  "TRIGGER",
  "PREFERENCE",
  "DELIVERY_LOG",
];

export const ENTITY_TYPE_OPTIONS = ENTITY_TYPE_FALLBACK_KEYS.map((value) => ({
  label: humanizeTriggerKey(value.toLowerCase()),
  value,
}));
