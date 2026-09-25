
export type MeetingFrequency = "Daily" | "Weekly" | "Biweekly" | "Monthly" | "Yearly";

export interface UserProfile {
  id: string;
  name: string;
  username: string;
  email: string;
  roles: string[];
  status: "Active" | "Inactive";
  lastLogin: string;
}

export interface SupervisorAssignment {
  id: string;
  supervisor: string;
  therapist: string;
  meetingFrequency: MeetingFrequency;
  assignmentType: string;
  lastMeeting: string;
  nextMeeting: string;
  assignedDate: string;
  startDate: string;
  endDate: string;
  notes: string;
  isActive: boolean;
}
