export interface DashboardOverviewItem {
  label: string;
  value: string | number;
  subtext?: string;
  icon?: string;
  badge?: {
    text: string;
    tone?: "urgent" | "success" | "neutral";
  };
}

export type SessionStatus = string;

export interface DashboardSessionItem {
  id: string;
  patientName: string;
  date: string;
  time?: string;
  sessionId: string;
  status: SessionStatus;
  scheduledAt?: string;
  hasInvoice?: boolean;
  overdueDays?: string;
}

export interface DashboardSessions {
  previous: DashboardSessionItem[];
  upcoming: DashboardSessionItem[];
  overdue: DashboardSessionItem[];
  previousTotal?: number;
  upcomingTotal?: number;
  overdueTotal?: number;
}

export interface DeadlineItem {
  id: string;
  title: string;
  date: string;
  clientName: string;
  priority: string;
  status: string;
}

export interface RecentTaskItem {
  id: string;
  title: string;
  priority: string;
  clientName: string;
  time: string;
  status: string;
}

export interface DashboardStaticContent {
  overviewItems: DashboardOverviewItem[];
  sessions: DashboardSessions;
  deadlines: DeadlineItem[];
  recentTasks: RecentTaskItem[];
}
