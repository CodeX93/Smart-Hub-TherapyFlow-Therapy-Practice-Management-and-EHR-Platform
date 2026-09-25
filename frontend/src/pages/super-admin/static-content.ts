import type {
  SuperAdminOrgStatusItem,
  SuperAdminRevenueMiniMetricItem,
  SuperAdminRevenuePlanItem,
  SuperAdminStatCardItem,
  SuperAdminTopTenantItem,
} from "./dashboard/dashboard.types";

export const SUPER_ADMIN_DASHBOARD_STATIC: {
  header: {
    breadcrumbLeft: string;
    breadcrumbRight: string;
    userInitials: string;
    userFullName: string;
    lastUpdatedText: string;
  };
  stats: SuperAdminStatCardItem[];
  organisations: {
    total: number;
    statuses: SuperAdminOrgStatusItem[];
    tenants: SuperAdminTopTenantItem[];
  };
  revenue: {
    miniMetrics: SuperAdminRevenueMiniMetricItem[];
    totalLabel: string;
    plans: SuperAdminRevenuePlanItem[];
    upgradesText: string;
    downgradesText: string;
  };
} = {
  header: {
    breadcrumbLeft: "Super Admin",
    breadcrumbRight: "Dashboard",
    userInitials: "JS",
    userFullName: "Jordan Smith",
    lastUpdatedText: "Last updated: 2026-02-27 14:03 UTC",
  },
  stats: [
    { label: "Active Tenants", value: "286", icon: "active-tenants" },
    { label: "Total End Users", value: "4,821", icon: "total-end-users" },
    { label: "MRR (Monthly Revenue)", value: "$124,350", icon: "mrr" },
    { label: "Churn This Month", value: "2.1%", icon: "churn-month" },
    { label: "Platform Uptime", value: "99.98%", icon: "platform-uptime" },
  ],
  organisations: {
    total: 312,
    statuses: [
      { label: "Active", count: 286, dotClassName: "bg-(--success-green)" },
      {
        label: "Trial",
        count: 18,
        dotClassName: "bg-(--border-primary-light)",
      },
      { label: "Past Due", count: 6, dotClassName: "bg-(--status-pending)" },
      { label: "Suspended", count: 2, dotClassName: "bg-(--status-denied)" },
    ],
    tenants: [
      { name: "Harbor Wellness", planLabel: "Pro", usersLabel: "86 users" },
      { name: "Blue Shore Therapy", planLabel: "Pro", usersLabel: "64 users" },
      {
        name: "East Ridge Counseling",
        planLabel: "Enterprise",
        usersLabel: "120 users",
      },
    ],
  },
  revenue: {
    miniMetrics: [
      {
        key: "mrr",
        label: "Monthly Recurring Revenue",
        value: "$142,300",
      },
      {
        key: "arr",
        label: "Annual Recurring Revenue",
        value: "$1,707,600",
      },
      {
        key: "churn",
        label: "Churn (30d)",
        value: "2.1%",
        valueClassName: "text-(--status-denied)",
        trendIcon: "down",
      },
    ],
    totalLabel: "Total: $124,350",
    plans: [
      {
        label: "Enterprise",
        value: "$78,400",
        percent: "63%",
        dotClassName: "bg-(--text-neutral-600)",
      },
      {
        label: "Professional",
        value: "$38,600",
        percent: "31%",
        dotClassName: "bg-(--border-primary-light)",
      },
      {
        label: "Trial / Other",
        value: "$7,350",
        percent: "6%",
        dotClassName: "bg-(--text-neutral-200)",
      },
    ],
    upgradesText: "+7 this month",
    downgradesText: "-2 this month",
  },
};
