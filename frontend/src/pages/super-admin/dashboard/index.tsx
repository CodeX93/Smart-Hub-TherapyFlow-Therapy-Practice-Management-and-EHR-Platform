import { RefreshCw } from "lucide-react";
import { useMemo } from "react";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import StatCard from "./components/StatCard";
import OrganizationsCard from "./components/OrganizationsCard";
import RevenueOverviewCard from "./components/RevenueOverviewCard";
import TenantInsightsSection from "./components/TenantInsightsSection";
import { SuperAdminStatIcon } from "./dashboard.utils";
import { getAuthSession } from "@/utils/authStorage";
import {
  useGetDashboardKpisQuery,
  useGetDashboardOrganisationsQuery,
  useGetDashboardRevenueOverviewQuery,
} from "@/store/api/superAdminApi";
import type {
  SuperAdminOrgStatusItem,
  SuperAdminRevenueMiniMetricItem,
  SuperAdminRevenuePlanItem,
  SuperAdminStatCardItem,
  SuperAdminTopTenantItem,
} from "./dashboard.types";

const DASHBOARD_STATS_TEMPLATE: SuperAdminStatCardItem[] = [
  { label: "Active Tenants", value: "0", icon: "active-tenants" },
  { label: "Total End Users", value: "0", icon: "total-end-users" },
  { label: "MRR (Monthly Revenue)", value: "$0", icon: "mrr" },
  { label: "Churn This Month", value: "0%", icon: "churn-month" },
  { label: "Platform Uptime", value: "99.9%", icon: "platform-uptime" },
];

function formatLastUpdated(value: string | null | undefined): string {
  if (!value) {
    return "Last updated: -";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return `Last updated: ${value}`;
  }
  return `Last updated: ${parsed.toLocaleString()}`;
}

function getCurrentUserMeta() {
  const session = getAuthSession();
  const user = (session?.user ?? {}) as { fullName?: string; username?: string; email?: string };
  const fullName = user.fullName?.trim() || user.username?.trim() || user.email?.trim() || "Super Admin";
  const initials = fullName
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");

  return {
    userFullName: fullName,
    userInitials: initials || "SA",
  };
}

function getStatusDotClassName(label: string): string {
  const normalized = label.toLowerCase();
  if (normalized.includes("active")) return "bg-(--success-green)";
  if (normalized.includes("trial")) return "bg-(--border-primary-light)";
  if (normalized.includes("past") || normalized.includes("due")) return "bg-(--status-pending)";
  if (normalized.includes("suspend")) return "bg-(--status-denied)";
  return "bg-(--text-neutral-200)";
}

function SuperAdminDashboard() {
  const {
    data: kpisData,
    isFetching: isKpisFetching,
    refetch: refetchKpis,
  } = useGetDashboardKpisQuery();
  const {
    data: orgData,
    isFetching: isOrgFetching,
    refetch: refetchOrganisations,
  } = useGetDashboardOrganisationsQuery({ period: "30d" });
  const {
    data: revenueData,
    isFetching: isRevenueFetching,
    refetch: refetchRevenueOverview,
  } = useGetDashboardRevenueOverviewQuery({ period: "30d" });
  const { userFullName, userInitials } = useMemo(() => getCurrentUserMeta(), []);
  const isRefreshing = isKpisFetching || isOrgFetching || isRevenueFetching;

  function handleSync() {
    void Promise.all([
      refetchKpis(),
      refetchOrganisations(),
      refetchRevenueOverview(),
    ]);
  }

  const statsViewModel = useMemo(() => {
    return DASHBOARD_STATS_TEMPLATE.map((item) => {
      if (item.icon === "active-tenants") {
        return { ...item, value: (kpisData?.activeTenants ?? 0).toLocaleString() };
      }
      if (item.icon === "total-end-users") {
        return { ...item, value: (kpisData?.totalEndUsers ?? 0).toLocaleString() };
      }
      if (item.icon === "mrr") {
        return { ...item, value: `$${(kpisData?.mrr ?? 0).toLocaleString()}` };
      }
      if (item.icon === "churn-month") {
        return { ...item, value: `${Number(kpisData?.churnThisMonth ?? 0).toLocaleString()}%` };
      }
      if (item.icon === "platform-uptime") {
        return {
          ...item,
          value: `${Number(kpisData?.platformUptimePercent ?? 0).toLocaleString()}%`,
        };
      }
      return item;
    });
  }, [kpisData]);

  const organisationsViewModel = useMemo(() => {
    const statuses: SuperAdminOrgStatusItem[] = (orgData?.statuses ?? []).map((status) => ({
      label: status.label,
      count: status.count,
      dotClassName: getStatusDotClassName(status.label),
    }));
    const tenants: SuperAdminTopTenantItem[] = (orgData?.tenants ?? []).map((tenant) => ({
      name: tenant.name,
      planLabel: tenant.planLabel,
      usersLabel: tenant.usersLabel,
    }));
    return {
      total: orgData?.total ?? 0,
      statuses,
      tenants,
    };
  }, [orgData]);

  const revenueViewModel = useMemo(() => {
    const miniMetrics: SuperAdminRevenueMiniMetricItem[] = (revenueData?.miniMetrics ?? []).filter((metric): metric is typeof metric & { key: "mrr" | "arr" | "churn" } => ["mrr", "arr", "churn"].includes(metric.key)).map(
      (metric) => ({
        key: metric.key,
        label: metric.label ?? "",
        value:
          metric.key === "churn"
            ? `${Number(metric.value ?? 0).toLocaleString()}%`
            : `$${Number(metric.value ?? 0).toLocaleString()}`,
        trendIcon: metric.trend,
      })
    );

    const planColorClasses = [
      "bg-(--text-neutral-600)",
      "bg-(--border-primary-light)",
      "bg-(--text-neutral-200)",
    ];
    const plans: SuperAdminRevenuePlanItem[] = (revenueData?.plans ?? []).map(
      (plan, index: number) => ({
        label: plan.label ?? "",
        value: `$${Number(plan.value ?? 0).toLocaleString()}`,
        percent: `${Number(plan.percent ?? 0).toLocaleString()}%`,
        dotClassName: planColorClasses[index] ?? "bg-(--text-neutral-200)",
      })
    );

    return {
      miniMetrics,
      totalLabel: revenueData?.totalLabel ?? "Total: $0",
      plans,
      upgradesText: revenueData?.upgradesText ?? "+0 this month",
      downgradesText: revenueData?.downgradesText ?? "-0 this month",
    };
  }, [revenueData]);

  return (
    <SuperAdminPageShell
      title="Global Dashboard"
      description="Overview of platform health, usage metrics, and recent incidents."
      meta={
        <>
          <button
            type="button"
            className="inline-flex items-center gap-1 text-[0.75rem] font-medium text-[#8a96a3] transition-colors hover:text-[#667483] disabled:cursor-not-allowed disabled:opacity-70"
            onClick={handleSync}
            disabled={isRefreshing}
          >
            <RefreshCw
              size={11}
              aria-hidden="true"
              className={isRefreshing ? "animate-spin" : undefined}
            />
            <span>Sync</span>
          </button>
          <span>{formatLastUpdated(kpisData?.lastUpdated)}</span>
        </>
      }
      userInitials={userInitials}
      userFullName={userFullName}
    >
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {statsViewModel.map(function (item) {
          return (
            <StatCard
              key={item.label}
              label={item.label}
              value={item.value}
              subtext={item.subtext}
              icon={<SuperAdminStatIcon iconKey={item.icon} />}
            />
          );
        })}
      </div>

      <TenantInsightsSection />

      <div className="grid w-full min-w-0 grid-cols-1 items-stretch gap-3.5 xl:min-h-0 xl:flex-1 xl:grid-cols-[1fr_1fr]">
        <OrganizationsCard
          total={organisationsViewModel.total}
          statuses={organisationsViewModel.statuses}
          tenants={organisationsViewModel.tenants}
          className="xl:h-full"
        />
        <RevenueOverviewCard
          miniMetrics={revenueViewModel.miniMetrics}
          totalLabel={revenueViewModel.totalLabel}
          plans={revenueViewModel.plans}
          upgradesText={revenueViewModel.upgradesText}
          downgradesText={revenueViewModel.downgradesText}
          className="xl:h-full"
        />
      </div>
    </SuperAdminPageShell>
  );
}

export default SuperAdminDashboard;
