export interface Comment {
  id: string;
  userId: string;
  userName: string;
  userInitials: string;
  text: string;
  timestamp: string;
  createdAt?: string;
  isInternal: boolean;
}

export interface Task {
  id: string;
  title: string;
  clientName: string;
  clientId?: string;
  assignee?: string;
  assigneeId?: string;
  priority: string;
  status: string;
  titleKey?: string | null;
  taskType?: string | null;
  createdDate: string;
  dueDate: string;
  rawDueDate?: string;
  description?: string;
  commentsCount: number;
  comments?: Comment[];
}

export const TASKS_STATIC_CONTENT = {
  priorityOptions: ["Low", "Medium", "High", "Urgent"] as const,
  assigneeOptions: [
    "Cameron Williamson",
    "Devon Lane",
    "Arlene McCoy",
    "Kristin Watson",
  ] as const,
  statusOptions: ["Pending", "In Progress", "Completed", "Overdue"] as const,
  taskTitleOptions: [
    "Complete Initial Assessment",
    "Develop Treatment Plan",
    "Review Client Progress",
    "Update Care Plan",
    "Schedule Follow-up Session",
    "Complete Documentation",
    "Insurance Verification",
    "Client Intake Process",
    "Progress Note Review",
    "Treatment Plan Update",
    "Assessment Review",
    "Client Goal Setting",
  ],
  summaryCards: [
    {
      label: "Total Tasks",
      value: "3",
      iconSrc: "", // Will use default icon or special handling
      icon: "list", // Placeholder for icon name logic if needed
      className: "bg-white",
    },
    {
      label: "Pending",
      value: "3",
      iconSrc: "",
      icon: "clock",
    },
    {
      label: "In Progress",
      value: "3",
      iconSrc: "",
      icon: "loader",
    },
    {
      label: "Completed",
      value: "0",
      iconSrc: "",
      icon: "check-circle",
    },
    {
      label: "Needs Attention",
      value: "3",
      iconSrc: "",
      icon: "alert-triangle",
      className: "bg-white",
      valueClassName: "",
      iconClassName: "bg-(--bg-warning-light) text-(--text-warning-dark)",
    },
  ],
  tasks: Array.from({ length: 100 }).map((_, i) => ({
    id: String(i + 1),
    title: [
      "Complete Initial Assessment",
      "Develop Treatment Plan",
      "Review Client Progress",
      "Update Care Plan",
      "Schedule Follow-up Session",
    ][i % 5],
    clientName: [
      "Ahtisham Khan",
      "Usman",
      "Sarah Johnson",
      "Michael Chen",
      "Jessica Williams",
    ][i % 5],
    clientId: `CL-2025-${1485 + i}`,
    assignee: [
      "Eleanor Pena",
      "Cody Fisher",
      "Devon Lane",
      "Kristin Watson",
      "Arlene McCoy",
    ][i % 5],
    priority: ["Low", "Medium", "High", "Urgent"][i % 4] as
      | "Low"
      | "Medium"
      | "High"
      | "Urgent",
    status: ["Pending", "In Progress", "Completed", "Overdue"][i % 4] as
      | "Pending"
      | "In Progress"
      | "Completed"
      | "Overdue",
    createdDate: "Dec 18, 2025",
    dueDate: "Dec 22, 2025",
    description:
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
    commentsCount: (i % 5) + 1,
    comments: [],
  })),
};
