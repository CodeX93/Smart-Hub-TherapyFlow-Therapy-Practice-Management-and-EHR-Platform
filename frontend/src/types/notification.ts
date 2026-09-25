export interface Notification {
  id: string;
  title: string;
  description: string;
  timestamp: string;
  isRead: boolean;
  dateGroup?: string;
  actionUrl?: string;
  actionLabel?: string;
}

export interface NotificationTemplate {
  id: string;
  templateKey?: string;
  title: string;
  tag: string;
  description: string;
  createdDate: string;
  priority?: "Low" | "Medium" | "High" | "Urgent";
  subject?: string;
  isActive?: boolean;
}
