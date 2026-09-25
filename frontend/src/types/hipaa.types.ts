import type { ReactNode } from "react";

export interface HIPAAOverviewData {
  label: string;
  value: string | number;
  icon: ReactNode;
  iconClassName?: string;
}

export interface AuditRecord {
  id: string;
  timestamp: string;
  user: string;
  action: string;
  client: string;
  details: string;
  riskLevel: "Low" | "Medium" | "High" | "Critical";
  result: "Success" | "Failure";
  ipAddress: string;
}

export interface HIPAAFilters {
  startDate: Date | null;
  endDate: Date | null;
  actionType: string | null;
  riskLevel: string | null;
  phiAccessOnly: boolean | null;
}

export interface HIPAAUserActivityItem {
  name: string;
  count: number;
}

export interface HIPAARiskDistributionBreakdownItem {
  label: string;
  count: string;
  percentage: number;
  color: string;
}

export interface HIPAARiskDistributionData {
  total: string;
  breakdown: HIPAARiskDistributionBreakdownItem[];
}
