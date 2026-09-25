export type SuperAdminStatIconKey =
  | "active-tenants"
  | "total-end-users"
  | "mrr"
  | "churn-month"
  | "platform-uptime";

export type SuperAdminRevenueMiniMetricKey = "mrr" | "arr" | "churn";

export interface SuperAdminStatCardItem {
  label: string;
  value: string;
  icon: SuperAdminStatIconKey;
  subtext?: string;
}

export interface SuperAdminOrgStatusItem {
  label: string;
  count: number;
  dotClassName: string;
}

export interface SuperAdminTopTenantItem {
  name: string;
  planLabel: string;
  usersLabel: string;
}

export interface SuperAdminRevenueMiniMetricItem {
  key: SuperAdminRevenueMiniMetricKey;
  label: string;
  value: string;
  valueClassName?: string;
  trendIcon?: "up" | "down";
}

export interface SuperAdminRevenuePlanItem {
  label: string;
  value: string;
  percent: string;
  dotClassName: string;
}

export type TenantInsightsPeriod = "7d" | "30d" | "90d";

export interface TenantGrowthPoint {
  date: string;
  signups: number;
  churned: number;
}

export interface TenantGrowthMetrics {
  newTenants: number;
  churned: number;
  netGrowth: number;
}

export interface PlanDistributionItem {
  planName: string;
  label: string;
  tenants: number;
  percent: number;
  color: string;
  isTopPlan?: boolean;
}

export interface RevenueBreakdownItem {
  value: string;
  label: string;
  tone: "success" | "neutral";
}

export type SystemHealthStatus = "operational" | "degraded" | "down" | "maintenance";

export interface SystemHealthItem {
  id: string;
  label: string;
  status: SystemHealthStatus;
  iconKey?: "api" | "database" | "storage" | "cdn" | "email" | "auth";
}

export interface TenantInsightsData {
  growthByPeriod: Record<TenantInsightsPeriod, TenantGrowthPoint[]>;
  planDistributionByPeriod: Record<TenantInsightsPeriod, PlanDistributionItem[]>;
  revenueBreakdownByPeriod: Record<TenantInsightsPeriod, RevenueBreakdownItem[]>;
  systemHealth: SystemHealthItem[];
}
