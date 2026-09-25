import { baseApi } from "../baseApi";
import * as Shared from "./shared";

export const superAdminDashboardApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getDashboardKpis: builder.query<Shared.DashboardKpisResponse, { timezone?: string } | void>({
      query: (arg) => {
        const timezone = arg?.timezone;
        const resolvedTimezone =
          timezone || Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";
        const query = new URLSearchParams({
          timezone: resolvedTimezone,
        });
        return `/api/v1/super-admin/dashboard/kpis?${query.toString()}`;
      },
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          activeTenants: Shared.getNumber(root, [
            "activeTenants",
            "active_tenants",
            "tenantCount",
            "totalTenants",
          ]),
          totalEndUsers: Shared.getNumber(root, [
            "totalEndUsers",
            "total_end_users",
            "activeUsers",
            "active_users",
            "users",
            "activeUserCount",
          ]),
          mrr: Shared.getNumber(root, [
            "mrr",
            "monthlyRevenueUsd",
            "revenue",
            "revenueUsd",
            "totalRevenue",
          ]),
          churnThisMonth: Shared.getNumber(root, [
            "churnThisMonth",
            "churn_this_month",
            "churnRate",
            "churnRatePct",
          ]),
          platformUptimePercent: Shared.getNumber(root, [
            "platformUptimePercent",
            "platform_uptime_percent",
            "platformUptime",
            "uptimePercent",
          ]),
          timezone: Shared.getString(root, ["timezone"]),
          currency: Shared.getString(root, ["currency"]),
          lastUpdated: Shared.getString(root, ["generatedAt", "lastUpdated", "timestamp", "updatedAt"]),
        };
      },
    }),
    getDashboardOrganisations: builder.query<
      Shared.DashboardOrganisationsResponse,
      { period?: string; timezone?: string }
    >({
      query: ({ period = "30d", timezone }) => {
        const resolvedTimezone =
          timezone || Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";
        const query = new URLSearchParams({
          period,
          timezone: resolvedTimezone,
        });
        return `/api/v1/super-admin/dashboard/organisations?${query.toString()}`;
      },
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        const total = Shared.getNumber(root, ["totalOrganisations", "total", "count", "totalOrgs"]);
        const statusesRaw = Array.isArray(root.statusCounts)
          ? root.statusCounts
          : Array.isArray(root.statuses)
            ? root.statuses
            : [];
        const statuses =
          statusesRaw
            .map((item: unknown) => {
              if (!Shared.isRecord(item)) return null;
              return {
                label: Shared.getString(item, ["label", "status"]),
                count: Shared.getNumber(item, ["count", "total"]),
              };
            })
            .filter(Boolean) as Array<{ label: string; count: number }>;
        const tenantsRaw = Array.isArray(root.topTenants)
          ? root.topTenants
          : Array.isArray(root.tenants)
            ? root.tenants
            : [];
        const tenants =
          tenantsRaw
            .map((item: unknown) => {
              if (!Shared.isRecord(item)) return null;
              const userCount = Shared.getNumber(item, ["activeUsers", "users", "userCount"]);
              return {
                name: Shared.getString(item, ["name", "organisationName", "tenantName"]),
                planLabel:
                  Shared.getString(item, ["planLabel", "plan", "planName", "planCode"]) || "N/A",
                usersLabel: `${userCount} users`,
              };
            })
            .filter(Boolean) as Shared.DashboardOrganisationItem[];
        return { total, statuses, tenants };
      },
    }),
    getDashboardRevenueOverview: builder.query<
      Shared.DashboardRevenueOverviewResponse,
      { period?: string; timezone?: string }
    >({
      query: ({ period = "30d", timezone }) => {
        const resolvedTimezone =
          timezone || Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";
        const query = new URLSearchParams({
          period,
          timezone: resolvedTimezone,
        });
        return `/api/v1/super-admin/dashboard/revenue-overview?${query.toString()}`;
      },
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        const mrr = Shared.getNumber(root, ["mrr"]);
        const arr = Shared.getNumber(root, ["arr"]);
        const churnRate = Shared.getNumber(root, ["churnRate", "churnRatePct"]);
        const upgrades = Shared.getNumber(root, ["upgrades"]);
        const downgrades = Shared.getNumber(root, ["downgrades"]);
        const revenueByPlanRaw = Array.isArray(root.revenueByPlan) ? root.revenueByPlan : [];
        const totalPlanRevenue = revenueByPlanRaw.reduce(function (sum: number, item: unknown) {
          if (!Shared.isRecord(item)) return sum;
          return sum + Shared.getNumber(item, ["amount", "revenue", "revenueUsd", "value"]);
        }, 0);

        return {
          miniMetrics: [
            { key: "mrr", label: "Monthly Recurring Revenue", value: mrr },
            { key: "arr", label: "Annual Recurring Revenue", value: arr },
            {
              key: "churn",
              label: "Churn (30d)",
              value: churnRate,
              trend: "down" as const,
            },
          ],
          totalLabel: `Total: $${totalPlanRevenue.toLocaleString()}`,
          plans: revenueByPlanRaw
            .map(function (item: unknown, index: number) {
              if (!Shared.isRecord(item)) return null;
              const value = Shared.getNumber(item, ["amount", "revenue", "revenueUsd", "value"]);
              const label =
                Shared.getString(item, ["planName", "label", "planCode", "name"]) ||
                `Plan ${index + 1}`;
              const percent = totalPlanRevenue > 0 ? (value / totalPlanRevenue) * 100 : 0;
              return { label, value, percent };
            })
            .filter(Boolean) as Shared.DashboardRevenuePlanItem[],
          upgradesText: `+${upgrades} this month`,
          downgradesText: `-${downgrades} this month`,
          lastUpdatedText: Shared.getString(root, [
            "generatedAt",
            "lastUpdatedText",
            "lastUpdated",
            "updatedAt",
          ]),
        };
      },
    }),
    getDashboardTenantGrowth: builder.query<
      Shared.DashboardTenantGrowthResponse,
      { range: "7w" | "1m" | "3m"; timezone?: string }
    >({
      query: ({ range, timezone }) => {
        const resolvedTimezone =
          timezone || Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";
        const query = new URLSearchParams({
          range,
          timezone: resolvedTimezone,
        });
        return `/api/v1/super-admin/dashboard/tenant-growth?${query.toString()}`;
      },
      transformResponse: (payload, _meta, arg) => {
        const root = Shared.extractRoot(payload);
        const pointsRaw = Array.isArray(root.points) ? root.points : [];
        return {
          range: Shared.getString(root, ["range"]) || arg.range,
          timezone: Shared.getString(root, ["timezone"]),
          from: Shared.getString(root, ["from"]),
          to: Shared.getString(root, ["to"]),
          points: pointsRaw
            .map((item: unknown) => {
              if (!Shared.isRecord(item)) return null;
              return {
                label: Shared.getString(item, ["label"]),
                from: Shared.getString(item, ["from"]),
                to: Shared.getString(item, ["to"]),
                newTenants: Shared.getNumber(item, ["newTenants"]),
                churnedTenants: Shared.getNumber(item, ["churnedTenants"]),
                netGrowth: Shared.getNumber(item, ["netGrowth"]),
              };
            })
            .filter(Boolean) as Shared.DashboardTenantGrowthPoint[],
          totalNewTenants: Shared.getNumber(root, ["totalNewTenants"]),
          totalChurnedTenants: Shared.getNumber(root, ["totalChurnedTenants"]),
          netGrowth: Shared.getNumber(root, ["netGrowth"]),
          generatedAt: Shared.getString(root, ["generatedAt"]),
        };
      },
    }),
    getDashboardPlanDistribution: builder.query<Shared.DashboardPlanDistributionResponse, void>({
      query: () => "/api/v1/super-admin/dashboard/plan-distribution",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        const plansRaw = Array.isArray(root.plans) ? root.plans : [];
        return {
          totalTenants: Shared.getNumber(root, ["totalTenants"]),
          plans: plansRaw
            .map((item: unknown) => {
              if (!Shared.isRecord(item)) return null;
              return {
                tier: Shared.getString(item, ["tier"]),
                planName: Shared.getString(item, ["planName", "plan", "label", "name"]),
                planCode: Shared.getString(item, ["planCode", "code", "slug"]),
                tenants: Shared.getNumber(item, ["tenants"]),
                percentage: Shared.getNumber(item, ["percentage"]),
              };
            })
            .filter(Boolean) as Shared.DashboardPlanDistributionPlanItem[],
          generatedAt: Shared.getString(root, ["generatedAt"]),
        };
      },
    }),
    getDashboardMrrBreakdown: builder.query<Shared.DashboardMrrBreakdownResponse, void>({
      query: () => "/api/v1/super-admin/dashboard/mrr-breakdown",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        return {
          currency: Shared.getString(root, ["currency"]) || "USD",
          enterpriseAndPro: Shared.getNumber(root, ["enterpriseAndPro"]),
          growthAndStarter: Shared.getNumber(root, ["growthAndStarter"]),
          totalMrr: Shared.getNumber(root, ["totalMrr"]),
          generatedAt: Shared.getString(root, ["generatedAt"]),
        };
      },
    }),
    getDashboardSystemHealth: builder.query<Shared.DashboardSystemHealthResponse, void>({
      query: () => "/api/v1/super-admin/dashboard/system-health",
      transformResponse: (payload) => {
        const root = Shared.extractRoot(payload);
        const servicesRaw = Array.isArray(root.services) ? root.services : [];
        return {
          overallStatus: Shared.getString(root, ["overallStatus"]),
          openIncidents: Shared.getNumber(root, ["openIncidents"]),
          services: servicesRaw
            .map((item: unknown) => {
              if (!Shared.isRecord(item)) return null;
              return {
                name: Shared.getString(item, ["name"]),
                status: Shared.getString(item, ["status"]),
              };
            })
            .filter(Boolean) as Shared.DashboardSystemHealthService[],
          generatedAt: Shared.getString(root, ["generatedAt"]),
        };
      },
    }),
    getRevenueAnalytics: builder.query<Shared.RevenueAnalyticsResponse, { months?: number }>({
      query: ({ months = 12 }) =>
        `/api/v1/super-admin/billing/revenue-analytics?months=${months}`,
      transformResponse: (payload, _meta, arg) => {
        const root = Shared.extractRoot(payload);
        const rows = Array.isArray(root.rows) ? root.rows : [];
        return {
          months: Shared.getNumber(root, ["months"]) || arg?.months || 12,
          generatedAt: Shared.getString(root, ["generatedAt"]) || new Date().toISOString(),
          rows: rows.map(function (row: unknown) {
            if (!Shared.isRecord(row)) {
              return {
                month: "",
                mrr: 0,
                arr: 0,
                activeSubscriptions: 0,
                endedSubscriptions: 0,
                churnRatePct: 0,
              };
            }
            return {
              month: Shared.getString(row, ["month"]),
              mrr: Shared.getNumber(row, ["mrr"]),
              arr: Shared.getNumber(row, ["arr"]),
              activeSubscriptions: Shared.getNumber(row, ["activeSubscriptions"]),
              endedSubscriptions: Shared.getNumber(row, ["endedSubscriptions"]),
              churnRatePct: Shared.getNumber(row, ["churnRatePct", "churnRate"]),
            };
          }),
        };
      },
    }),
  }),
});

export const {
  useGetDashboardKpisQuery,
  useGetDashboardOrganisationsQuery,
  useGetDashboardRevenueOverviewQuery,
  useGetDashboardTenantGrowthQuery,
  useGetDashboardPlanDistributionQuery,
  useGetDashboardMrrBreakdownQuery,
  useGetDashboardSystemHealthQuery,
  useGetRevenueAnalyticsQuery,
} = superAdminDashboardApi;
