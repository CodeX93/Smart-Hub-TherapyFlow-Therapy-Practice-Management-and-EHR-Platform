import { useMemo, useState } from "react";
import {
  Database,
  Globe,
  HardDriveDownload,
  Link2,
  Mail,
  Server,
  ShieldCheck,
} from "lucide-react";
import type {
  PlanDistributionItem,
  TenantGrowthPoint,
  TenantInsightsData,
  TenantInsightsPeriod,
} from "../dashboard.types";
import {
  useGetDashboardMrrBreakdownQuery,
  useGetDashboardPlanDistributionQuery,
  useGetDashboardSystemHealthQuery,
  useGetDashboardTenantGrowthQuery,
} from "@/store/api/superAdminApi";

const PERIOD_OPTIONS: Array<{ key: TenantInsightsPeriod; label: string }> = [
  { key: "7d", label: "7 Days" },
  { key: "30d", label: "1 Month" },
  { key: "90d", label: "3 Months" },
];

interface TenantInsightsSectionProps {
  data?: TenantInsightsData;
  initialPeriod?: TenantInsightsPeriod;
  onPeriodChange?: (period: TenantInsightsPeriod) => void;
}

function formatMetricDelta(value: number): string {
  if (value > 0) return `+${value.toLocaleString()}`;
  if (value < 0) return `-${Math.abs(value).toLocaleString()}`;
  return "0";
}

function formatDateLabel(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat("en-US", { month: "short", day: "2-digit" }).format(parsed);
}

function formatCurrencyAmount(value: number, currency: string): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(value);
}

function getGraphPoints(
  values: number[],
  plotWidth: number,
  plotHeight: number,
  maxValue: number
): Array<{ x: number; y: number }> {
  return values.map(function (value, index) {
    const x = values.length > 1 ? (index / (values.length - 1)) * plotWidth : plotWidth / 2;
    const y = maxValue > 0 ? plotHeight - (value / maxValue) * plotHeight : plotHeight;
    return { x, y };
  });
}

function mapPeriodToRange(period: TenantInsightsPeriod): "7w" | "1m" | "3m" {
  if (period === "7d") return "7w";
  if (period === "30d") return "1m";
  return "3m";
}

function getSystemIcon(iconKey?: string) {
  if (iconKey === "api") return Link2;
  if (iconKey === "database") return Database;
  if (iconKey === "storage") return HardDriveDownload;
  if (iconKey === "cdn") return Globe;
  if (iconKey === "email") return Mail;
  if (iconKey === "auth") return ShieldCheck;
  return Server;
}

function inferSystemIconKey(
  name: string
): "api" | "database" | "storage" | "cdn" | "email" | "auth" | undefined {
  const normalized = name.toLowerCase();
  if (normalized.includes("api")) return "api";
  if (normalized.includes("database")) return "database";
  if (normalized.includes("storage")) return "storage";
  if (normalized.includes("cdn") || normalized.includes("edge")) return "cdn";
  if (normalized.includes("email") || normalized.includes("smtp")) return "email";
  if (normalized.includes("auth")) return "auth";
  return undefined;
}

function getSystemStatusTheme(status: string) {
  if (status === "degraded" || status === "maintenance") {
    return {
      badgeClassName: "bg-[#fff7ed] text-[#f59e0b]",
      iconWrapClassName: "bg-[#fff7ed] text-[#f59e0b]",
      label: status === "maintenance" ? "Maintenance" : "Degraded",
    };
  }
  if (status === "down") {
    return {
      badgeClassName: "bg-[#fef2f2] text-[#ef4444]",
      iconWrapClassName: "bg-[#fef2f2] text-[#ef4444]",
      label: "Down",
    };
  }
  return {
    badgeClassName: "bg-[#ecfdf5] text-[#10b981]",
    iconWrapClassName: "bg-[#ecfdf5] text-[#10b981]",
    label: "Operational",
  };
}

function getOverallHealth(overallStatus: string) {
  const normalized = overallStatus.toLowerCase();
  if (normalized.includes("down")) {
    return { label: "Issues", className: "bg-[#fef2f2] text-[#ef4444]" };
  }
  if (normalized.includes("degraded") || normalized.includes("maintenance")) {
    return { label: "Attention", className: "bg-[#fff7ed] text-[#f59e0b]" };
  }
  return { label: "All OK", className: "bg-[#ecfdf5] text-[#10b981]" };
}

function TenantInsightsSection(props: TenantInsightsSectionProps) {
  const [period, setPeriod] = useState<TenantInsightsPeriod>(props.initialPeriod ?? "7d");
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const { data: tenantGrowthData } = useGetDashboardTenantGrowthQuery({
    range: mapPeriodToRange(period),
  });
  const { data: planDistributionApiData } = useGetDashboardPlanDistributionQuery();
  const { data: mrrBreakdownApiData } = useGetDashboardMrrBreakdownQuery();
  const { data: systemHealthApiData } = useGetDashboardSystemHealthQuery();

  const growthData = useMemo<TenantGrowthPoint[]>(() => {
    return (tenantGrowthData?.points ?? []).map((point) => ({
      date: point.label,
      signups: point.newTenants,
      churned: point.churnedTenants,
    }));
  }, [tenantGrowthData]);
  const distributionData = useMemo<PlanDistributionItem[]>(() => {
    const colors = ["#f59e0b", "#14b8a6", "#ef4444", "#94a3b8", "#6366f1", "#22c55e"];
    const plans = planDistributionApiData?.plans ?? [];
    const topPercent = plans.reduce((max, item) => Math.max(max, item.percentage), 0);
    return plans.map((plan, index) => ({
      planName: plan.planName || plan.tier || plan.planCode || "Unknown Plan",
      label: plan.planName || plan.tier || plan.planCode || "Unknown Plan",
      tenants: plan.tenants,
      percent: plan.percentage,
      color: colors[index % colors.length],
      isTopPlan: plan.percentage === topPercent && topPercent > 0,
    }));
  }, [planDistributionApiData]);
  const breakdownData = useMemo(
    () => [
      {
        value: formatCurrencyAmount(
          mrrBreakdownApiData?.enterpriseAndPro ?? 0,
          mrrBreakdownApiData?.currency ?? "USD"
        ),
        label: "Enterprise + Pro",
        tone: "success" as const,
      },
      {
        value: formatCurrencyAmount(
          mrrBreakdownApiData?.growthAndStarter ?? 0,
          mrrBreakdownApiData?.currency ?? "USD"
        ),
        label: "Growth + Starter",
        tone: "neutral" as const,
      },
    ],
    [mrrBreakdownApiData]
  );

  const growthMetrics = useMemo(function () {
    return {
      newTenants: tenantGrowthData?.totalNewTenants ?? 0,
      churned: -(tenantGrowthData?.totalChurnedTenants ?? 0),
      netGrowth: tenantGrowthData?.netGrowth ?? 0,
    };
  }, [tenantGrowthData]);

  const graphViewModel = useMemo(function () {
    const chartWidth = 560;
    const chartHeight = 220;
    const paddingLeft = 20;
    const paddingRight = 10;
    const paddingTop = 10;
    const paddingBottom = 10;
    const plotWidth = chartWidth - paddingLeft - paddingRight;
    const plotHeight = chartHeight - paddingTop - paddingBottom;
    const signupsValues = growthData.map((item) => item.signups);
    const churnValues = growthData.map((item) => item.churned);
    const maxSeriesValue = Math.max(...signupsValues, ...churnValues, 0);
    const maxValue = maxSeriesValue > 0 ? Math.ceil(maxSeriesValue / 10) * 10 : 0;
    const yTicks = [0, maxValue / 3, (maxValue / 3) * 2, maxValue].map((tick) =>
      Math.round(tick)
    );
    const signupsPoints = getGraphPoints(signupsValues, plotWidth, plotHeight, maxValue);
    const churnPoints = getGraphPoints(churnValues, plotWidth, plotHeight, maxValue);
    return {
      chartWidth,
      chartHeight,
      paddingLeft,
      paddingRight,
      paddingTop,
      paddingBottom,
      plotWidth,
      plotHeight,
      maxValue,
      yTicks,
      signupsPoints,
      churnPoints,
    };
  }, [growthData]);

  const systemHealthServices = useMemo(
    () =>
      (systemHealthApiData?.services ?? []).map((service, index) => ({
        id: `${service.name}-${index}`,
        label: service.name,
        status: service.status.toLowerCase(),
        iconKey: inferSystemIconKey(service.name),
      })),
    [systemHealthApiData]
  );
  const overallHealth = useMemo(
    () => getOverallHealth(systemHealthApiData?.overallStatus ?? "Operational"),
    [systemHealthApiData]
  );

  function handlePeriodChange(nextPeriod: TenantInsightsPeriod) {
    setPeriod(nextPeriod);
    setHoveredIndex(null);
    props.onPeriodChange?.(nextPeriod);
  }

  const hoveredPoint = hoveredIndex === null ? null : growthData[hoveredIndex] ?? null;
  const hoveredX =
    hoveredIndex === null ? null : graphViewModel.signupsPoints[hoveredIndex]?.x ?? null;

  return (
    <div className="grid min-w-0 grid-cols-1 gap-3.5 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)_minmax(0,1fr)]">
      <section className="min-w-0 rounded-[0.875rem] border border-[#e3ebf3] bg-white p-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-[0.8125rem] font-semibold leading-5 text-[#1e2934]">Tenant Growth</h3>
            <p className="mt-1 text-[0.6875rem] font-medium text-[#8a96a3]">New signups vs churned tenants</p>
          </div>
          <div className="inline-flex rounded-full bg-[#f6f9fc] p-1 text-[0.6875rem] font-medium text-[#7c8a97]">
            {PERIOD_OPTIONS.map(function (option) {
              const isActive = option.key === period;
              return (
                <button
                  key={option.key}
                  type="button"
                  onClick={() => handlePeriodChange(option.key)}
                  className={`rounded-full px-2.5 py-1 transition-colors ${isActive ? "bg-white text-[#1e2934] shadow-[0_1px_2px_rgba(15,23,42,0.08)]" : "text-[#7c8a97] hover:text-[#4b5563]"}`}
                >
                  {option.label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="mt-4 overflow-x-auto">
          <div className="relative min-w-[38.75rem]">
            {hoveredPoint ? (
              <div
                className="pointer-events-none absolute top-2 z-10 -translate-x-1/2 rounded-[0.625rem] border border-[#e3ebf3] bg-white/95 px-2.5 py-1.5 text-[0.625rem] font-medium text-[#5b6977] shadow-[0_6px_20px_rgba(15,23,42,0.08)]"
                style={{
                  left: (hoveredX ?? 0) + graphViewModel.paddingLeft,
                }}
              >
                <div className="font-semibold text-[#1f2d38]">{formatDateLabel(hoveredPoint.date)}</div>
                <div className="mt-0.5 text-[#10b981]">New: {hoveredPoint.signups}</div>
                <div className="text-[#ef4444]">Churned: {hoveredPoint.churned}</div>
              </div>
            ) : null}

            <svg
              viewBox={`0 0 ${graphViewModel.chartWidth} ${graphViewModel.chartHeight}`}
              className="h-[13.75rem] w-full"
              onMouseLeave={() => setHoveredIndex(null)}
            >
              {graphViewModel.yTicks.map(function (tick) {
                const y =
                  graphViewModel.paddingTop +
                  (graphViewModel.plotHeight -
                    (graphViewModel.maxValue > 0
                      ? (tick / graphViewModel.maxValue) * graphViewModel.plotHeight
                      : 0));
                return (
                  <g key={tick}>
                    <line
                      x1={graphViewModel.paddingLeft}
                      y1={y}
                      x2={graphViewModel.chartWidth - graphViewModel.paddingRight}
                      y2={y}
                      stroke="#ecf1f6"
                      strokeWidth="1"
                    />
                    <text
                      x={graphViewModel.paddingLeft - 10}
                      y={y + 3}
                      textAnchor="end"
                      className="fill-[#9aa7b3] text-[0.6875rem]"
                    >
                      {tick}
                    </text>
                  </g>
                );
              })}

              <polyline
                fill="none"
                stroke="#34d399"
                strokeWidth="2.5"
                style={{ transition: "all 320ms ease" }}
                points={graphViewModel.signupsPoints
                  .map(
                    (point) =>
                      `${point.x + graphViewModel.paddingLeft},${point.y + graphViewModel.paddingTop}`
                  )
                  .join(" ")}
              />
              <polyline
                fill="none"
                stroke="#ef4444"
                strokeWidth="2.5"
                style={{ transition: "all 320ms ease" }}
                points={graphViewModel.churnPoints
                  .map(
                    (point) =>
                      `${point.x + graphViewModel.paddingLeft},${point.y + graphViewModel.paddingTop}`
                  )
                  .join(" ")}
              />

              {hoveredIndex !== null ? (
                <line
                  x1={graphViewModel.signupsPoints[hoveredIndex].x + graphViewModel.paddingLeft}
                  y1={graphViewModel.paddingTop}
                  x2={graphViewModel.signupsPoints[hoveredIndex].x + graphViewModel.paddingLeft}
                  y2={graphViewModel.chartHeight - graphViewModel.paddingBottom}
                  stroke="#d5dee8"
                  strokeDasharray="4 4"
                />
              ) : null}

              {graphViewModel.signupsPoints.map(function (point, index) {
                const isHovered = hoveredIndex === index;
                return (
                  <circle
                    key={`signup-${index}`}
                    cx={point.x + graphViewModel.paddingLeft}
                    cy={point.y + graphViewModel.paddingTop}
                    r={isHovered ? "5.2" : "3.8"}
                    fill="#fff"
                    stroke="#34d399"
                    strokeWidth="2.4"
                    className="transition-all duration-300 ease-out"
                  />
                );
              })}
              {graphViewModel.churnPoints.map(function (point, index) {
                const isHovered = hoveredIndex === index;
                return (
                  <circle
                    key={`churn-${index}`}
                    cx={point.x + graphViewModel.paddingLeft}
                    cy={point.y + graphViewModel.paddingTop}
                    r={isHovered ? "5.2" : "3.8"}
                    fill="#fff"
                    stroke="#ef4444"
                    strokeWidth="2.4"
                    className="transition-all duration-300 ease-out"
                  />
                );
              })}

              {graphViewModel.signupsPoints.map(function (point, index) {
                const previousPoint = graphViewModel.signupsPoints[index - 1];
                const nextPoint = graphViewModel.signupsPoints[index + 1];
                const leftBoundary =
                  index === 0
                    ? graphViewModel.paddingLeft
                    : (previousPoint.x + point.x) / 2 + graphViewModel.paddingLeft;
                const rightBoundary =
                  index === graphViewModel.signupsPoints.length - 1
                    ? graphViewModel.chartWidth - graphViewModel.paddingRight
                    : (nextPoint.x + point.x) / 2 + graphViewModel.paddingLeft;

                return (
                  <rect
                    key={`hover-zone-${index}`}
                    x={leftBoundary}
                    y={graphViewModel.paddingTop}
                    width={Math.max(rightBoundary - leftBoundary, 8)}
                    height={graphViewModel.plotHeight}
                    fill="transparent"
                    onMouseEnter={() => setHoveredIndex(index)}
                    style={{ cursor: "pointer" }}
                  />
                );
              })}
            </svg>

            <div className="mt-2 flex items-center justify-between pl-5 pr-2 text-[0.6875rem] font-medium text-[#9aa7b3]">
              {growthData.map(function (point) {
                return <span key={point.date}>{formatDateLabel(point.date)}</span>;
              })}
            </div>
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-start justify-between gap-6 border-t border-[#edf2f7] pb-0.5 pl-3 pr-5 pt-4">
          <div className="min-w-[8.75rem]">
            <div className="text-[1.75rem] font-semibold leading-none text-[#1f2d38]">
              {formatMetricDelta(growthMetrics.newTenants)}
            </div>
            <div className="mt-1 text-[0.6875rem] font-medium text-[#8a96a3]">
              <span className="mr-1 text-[#34d399]">&bull;</span>
              New Tenants
            </div>
          </div>
          <div className="min-w-[8.75rem]">
            <div className="text-[1.75rem] font-semibold leading-none text-[#ef4444]">
              {formatMetricDelta(growthMetrics.churned)}
            </div>
            <div className="mt-1 text-[0.6875rem] font-medium text-[#8a96a3]">
              <span className="mr-1 text-[#ef4444]">&bull;</span>
              Churned
            </div>
          </div>
          <div className="min-w-[8.75rem]">
            <div className="text-[1.75rem] font-semibold leading-none text-[#1f2d38]">
              {formatMetricDelta(growthMetrics.netGrowth)}
            </div>
            <div className="mt-1 text-[0.6875rem] font-medium text-[#8a96a3]">
              <span className="mr-1 text-[#34d399]">&bull;</span>
              Net Growth
            </div>
          </div>
        </div>
      </section>

      <section className="min-w-0 rounded-[0.875rem] border border-[#e3ebf3] bg-white p-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        <h3 className="text-[0.8125rem] font-semibold leading-5 text-[#1e2934]">Plan Distribution</h3>
        <p className="mt-1 text-[0.6875rem] font-medium text-[#8a96a3]">Subscribers per tier</p>

        <div className="mt-4 space-y-3.5">
          {distributionData.map(function (plan) {
            return (
              <div key={plan.label}>
                <div className="flex items-center justify-between text-[0.6875rem]">
                  <div className="flex items-center gap-2">
                    <span className="h-2.5 w-2.5 rounded-[0.1875rem]" style={{ backgroundColor: plan.color }} />
                    <span className="font-semibold text-[#334155]">{plan.planName}</span>
                    {plan.isTopPlan ? (
                      <span className="rounded-full bg-[#fef3c7] px-1.5 py-0.5 text-[0.5625rem] font-semibold text-[#d97706]">
                        Top
                      </span>
                    ) : null}
                  </div>
                  <div className="font-medium text-[#64748b]">
                    {plan.tenants.toLocaleString()} tenants
                    <span className="ml-1.5 text-[1rem] font-semibold text-[#334155]">{plan.percent}%</span>
                  </div>
                </div>
                <div className="mt-2 h-2 rounded-full bg-[#eef2f7]">
                  <div className="h-full rounded-full" style={{ width: `${plan.percent}%`, backgroundColor: plan.color }} />
                </div>
              </div>
            );
          })}
        </div>

        <div className="mt-5 border-t border-[#edf2f7] pt-4">
          <div className="text-[0.6875rem] font-semibold tracking-[0.02em] text-[#7c8a97]">MRR BREAKDOWN</div>
          <div className="mt-3 grid grid-cols-2 gap-2.5">
            {breakdownData.map(function (item) {
              const isSuccess = item.tone === "success";
              return (
                <div
                  key={item.label}
                  className={`rounded-[0.6875rem] p-2.5 ${isSuccess ? "bg-[#dcfce7]" : "bg-[#f8fafc]"}`}
                >
                  <div className={`text-[1rem] font-semibold ${isSuccess ? "text-[#10b981]" : "text-[#1f2d38]"}`}>
                    {item.value}
                  </div>
                  <div className={`text-[0.6875rem] font-medium ${isSuccess ? "text-[#10b981]" : "text-[#64748b]"}`}>
                    {item.label}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      <section className="min-w-0 rounded-[0.875rem] border border-[#e3ebf3] bg-white p-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h3 className="text-[0.8125rem] font-semibold leading-5 text-[#1e2934]">System Health</h3>
            <p className="mt-1 text-[0.6875rem] font-medium text-[#8a96a3]">All services status</p>
          </div>
          <span className={`rounded-full px-2.5 py-1 text-[0.625rem] font-semibold ${overallHealth.className}`}>
            {overallHealth.label}
          </span>
        </div>

        <div className="mt-4 space-y-2.5">
          {systemHealthServices.map(function (service) {
            const statusTheme = getSystemStatusTheme(service.status);
            const Icon = getSystemIcon(service.iconKey);
            return (
              <div
                key={service.id}
                className="flex items-center justify-between rounded-[0.6875rem] border border-[#edf2f7] bg-[#fcfdff] px-3 py-2.5"
              >
                <div className="flex items-center gap-2.5">
                  <span
                    className={`flex h-7 w-7 items-center justify-center rounded-[0.5rem] ${statusTheme.iconWrapClassName}`}
                  >
                    <Icon size={13} />
                  </span>
                  <span className="text-[0.75rem] font-semibold text-[#334155]">{service.label}</span>
                </div>
                <span className={`rounded-full px-2.5 py-1 text-[0.625rem] font-semibold ${statusTheme.badgeClassName}`}>
                  {statusTheme.label}
                </span>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}

export default TenantInsightsSection;
