import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";

import { ContentLoader } from "@/components/shared/ContentLoader";
import { useCallback, useMemo, useState, type ChangeEvent } from "react";
import { AlertCircle, AlertTriangle, Eye, Shield } from "lucide-react";
import OverviewCard from "@/components/shared/OverviewCard";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import type {
  AuditRecord,
  HIPAAFilters,
  HIPAAOverviewData,
  HIPAARiskDistributionData,
  HIPAAUserActivityItem,
} from "@/types/hipaa.types";
import { actionTypes, riskLevels } from "../compliance.static";
import HipaaToolbar from "@/components/hipaa/HipaaToolbar";
import HipaaTable from "@/components/hipaa/HipaaTable";
import UserActivityChart from "@/components/hipaa/UserActivityChart";
import RiskDistributionChart from "@/components/hipaa/RiskDistributionChart";
import {
  type AdminAuditDashboardParams,
  type AdminAuditLogEntry,
  type AdminAuditLogsParams,
  type AdminAuditPeriod,
  type AdminAuditRiskFilter,
  useGetAdminAuditDashboardQuery,
  useGetAdminAuditLogsQuery,
  useLazyGetAdminAuditExportQuery,
} from "@/store/api/admin/audit.api";
import { getApiErrorMessage } from "@/utils/apiError";

const ITEMS_PER_PAGE = 50;

const Hipaa = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const [period, setPeriod] = useState<AdminAuditPeriod>("monthly");


  const [filters, setFilters] = useState<HIPAAFilters>({
    startDate: null,
    endDate: null,
    actionType: null,
    riskLevel: null,
    phiAccessOnly: null,
  });

  const listScope = JSON.stringify([filters, searchQuery]);
  const [currentPage, setCurrentPage] = useScopedPage(listScope);
  const dashboardQueryArgs = useMemo<AdminAuditDashboardParams>(() => {
    const params: AdminAuditDashboardParams = {
      period,
      page: 0,
      size: 25,
    };

    const trimmedSearch = searchQuery.trim();
    if (trimmedSearch) params.username = trimmedSearch;
    if (filters.actionType && filters.actionType !== "all") params.action = filters.actionType;
    if (filters.riskLevel && filters.riskLevel !== "all") {
      params.riskLevel = filters.riskLevel as AdminAuditRiskFilter;
    }
    if (filters.startDate) params.startDate = filters.startDate.toISOString().slice(0, 10);
    if (filters.endDate) params.endDate = filters.endDate.toISOString().slice(0, 10);
    if (filters.phiAccessOnly !== null) params.hipaaOnly = filters.phiAccessOnly;
    return params;
  }, [filters, period, searchQuery]);

  const logsQueryArgs = useMemo<AdminAuditLogsParams>(() => {
    const params: AdminAuditLogsParams = {
      page: currentPage - 1,
      size: ITEMS_PER_PAGE,
    };

    const trimmedSearch = searchQuery.trim();
    if (trimmedSearch) params.username = trimmedSearch;
    if (filters.actionType && filters.actionType !== "all") params.action = filters.actionType;
    if (filters.riskLevel && filters.riskLevel !== "all") {
      params.riskLevel = filters.riskLevel as AdminAuditRiskFilter;
    }
    if (filters.startDate) params.startDate = filters.startDate.toISOString().slice(0, 10);
    if (filters.endDate) params.endDate = filters.endDate.toISOString().slice(0, 10);
    if (filters.phiAccessOnly !== null) params.hipaaOnly = filters.phiAccessOnly;
    return params;
  }, [currentPage, filters, searchQuery]);

  const {
    data: dashboard,
    error: dashboardError,
    isLoading: isDashboardLoading,
    isFetching: isDashboardFetching,
  } = useGetAdminAuditDashboardQuery(dashboardQueryArgs);
  const {
    currentData: logsResponse,
    error: logsError,
    isLoading: isLogsLoading,
    isFetching: isLogsFetching,
  } = useGetAdminAuditLogsQuery(logsQueryArgs);
  const [triggerExport, { isFetching: isExporting }] =
    useLazyGetAdminAuditExportQuery();

  const mappedRecords = useMemo(() => logsResponse?.content.map(mapAuditRecord), [logsResponse?.content]);
  const { items: records } = usePagedItems(mappedRecords, currentPage, listScope);

  const resetPagination = useCallback(() => {
    setCurrentPage(1);
  }, [setCurrentPage]);

  const handleSearchChange = (e: ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
    resetPagination();
  };

  const handleApplyFilter = (newFilters: HIPAAFilters) => {
    setFilters(newFilters);
    resetPagination();
  };

  const handleClearFilters = (newFilters: HIPAAFilters) => {
    setFilters(newFilters);
    resetPagination();
  };

  const handleExport = useCallback(async () => {
    try {
      const blob = await triggerExport({
        username: searchQuery.trim() || undefined,
        action: filters.actionType && filters.actionType !== "all" ? filters.actionType : undefined,
        riskLevel:
          filters.riskLevel && filters.riskLevel !== "all"
            ? (filters.riskLevel as AdminAuditRiskFilter)
            : undefined,
        startDate: filters.startDate?.toISOString().slice(0, 10),
        endDate: filters.endDate?.toISOString().slice(0, 10),
        hipaaOnly: filters.phiAccessOnly ?? undefined,
        limit: 1000,
      }).unwrap();

      const objectUrl = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = `hipaa-audit-report-${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(objectUrl);
    } catch {
      // surface through current error messaging only
    }
  }, [filters, searchQuery, triggerExport]);

  const overviewData = useMemo<HIPAAOverviewData[]>(
    () => [
      {
        label: "Total Activities",
        value: dashboard?.totalActivities ?? 0,
        icon: <Shield size={20} />,
        iconClassName:
          "bg-(--bg-primary-50) text-(--bg-primary-dark) !h-12 !w-12 !flex-none rounded-xl items-center justify-center p-3.25",
      },
      {
        label: "PHI Access Events",
        value: dashboard?.phiAccessEvents ?? 0,
        icon: <Eye size={20} />,
        iconClassName:
          "bg-(--bg-primary-50) text-(--bg-primary-dark) !h-12 !w-12 !flex-none rounded-xl items-center justify-center p-3.25",
      },
      {
        label: "High Risk Events",
        value: dashboard?.highRiskEvents ?? 0,
        icon: <AlertTriangle size={20} />,
        iconClassName:
          "bg-[#fff9e6] text-[#b45309] !h-12 !w-12 !flex-none rounded-xl items-center justify-center p-3.25",
      },
      {
        label: "Failed Attempts",
        value: dashboard?.failedAttempts ?? 0,
        icon: <AlertCircle size={20} />,
        iconClassName:
          "bg-[#fef2f2] text-[#b91c1c] !h-12 !w-12 !flex-none rounded-xl items-center justify-center p-3.25",
      },
    ],
    [dashboard],
  );

  const userActivityData = useMemo<HIPAAUserActivityItem[]>(
    () =>
      (dashboard?.userActivitySummary ?? []).map((entry) => ({
        name: entry.username || "Unknown user",
        count: entry.activityCount,
      })),
    [dashboard],
  );

  const riskDistributionData = useMemo<HIPAARiskDistributionData>(() => {
    const distribution = dashboard?.riskDistribution;
    const total = distribution?.totalEvents ?? 0;
    return {
      total: total.toLocaleString(),
      breakdown: [
        {
          label: "Low Risk",
          count: (distribution?.lowRiskEvents ?? 0).toLocaleString(),
          percentage: distribution?.lowRiskPercentage ?? 0,
          color: "#009B65",
        },
        {
          label: "Medium Risk",
          count: (distribution?.mediumRiskEvents ?? 0).toLocaleString(),
          percentage: distribution?.mediumRiskPercentage ?? 0,
          color: "#FBAC00",
        },
        {
          label: "High Risk",
          count: (distribution?.highRiskEvents ?? 0).toLocaleString(),
          percentage: distribution?.highRiskPercentage ?? 0,
          color: "#EF4444",
        },
        {
          label: "Critical Risk",
          count: (distribution?.criticalRiskEvents ?? 0).toLocaleString(),
          percentage: distribution?.criticalRiskPercentage ?? 0,
          color: "#7F1D1D",
        },
      ].filter((item) => item.percentage > 0 || total === 0),
    };
  }, [dashboard]);

  const errorMessage = dashboardError
    ? getApiErrorMessage(dashboardError)
    : logsError
      ? getApiErrorMessage(logsError)
      : null;
  const hasMore = currentPage < (logsResponse?.totalPages ?? 0);
  const isLoadingMore = isLogsFetching && currentPage > 1;
  const showSpinner =
    isDashboardLoading || isDashboardFetching || isLogsLoading || isLogsFetching || isExporting;

  const handleLoadMore = useCallback(() => {
    if (!hasMore || isLogsFetching) return;
    setCurrentPage((previous) => previous + 1);
  }, [hasMore, isLogsFetching, setCurrentPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore,
    isLoading: isLoadingMore,
  });

  return (
    <div className="flex flex-col gap-6 w-full">
      <ScrollToTopButton />

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 w-full">
        {overviewData.map((item, index) => (
          <OverviewCard
            key={index}
            label={item.label}
            value={item.value}
            icon={item.icon}
            labelFirst={true}
            iconClassName={item.iconClassName}
            className="h-full"
          />
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 w-full">
        <UserActivityChart data={userActivityData} />
        <RiskDistributionChart
          filter={period}
          onFilterChange={(value) => {
            setPeriod(value);
            resetPagination();
          }}
          data={riskDistributionData}
        />
      </div>

      <HipaaToolbar
        searchQuery={searchQuery}
        handleSearchChange={handleSearchChange}
        filters={filters}
        handleClearFilters={handleClearFilters}
        handleApplyFilter={handleApplyFilter}
        actionTypes={actionTypes}
        riskLevels={riskLevels}
        handleExport={handleExport}
      />

      {errorMessage ? (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      ) : null}

      <HipaaTable data={records} />

      <div
        ref={observerTarget}
        className="h-10 w-full flex items-center justify-center"
      >
        {showSpinner && (
          <ContentLoader variant="inline" size="md" />
        )}
      </div>
    </div>
  );
};

export default Hipaa;

function mapAuditRecord(entry: AdminAuditLogEntry): AuditRecord {
  return {
    id: String(entry.id),
    timestamp: formatAuditTimestamp(entry.timestamp),
    user: entry.username || "Unknown user",
    action: normalizeAuditLabel(entry.action),
    client: entry.clientMrn?.trim() || "-",
    details: entry.details || entry.resourceType || "-",
    riskLevel: normalizeRiskLevel(entry.riskLevel),
    result: normalizeAuditResult(entry.result),
    ipAddress: entry.ipAddress || "-",
  };
}

function formatAuditTimestamp(value: string): string {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: true,
  }).format(date);
}

function normalizeAuditLabel(value: string): string {
  if (!value) return "-";
  return value
    .trim()
    .replace(/[_-]+/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function normalizeRiskLevel(value: string): AuditRecord["riskLevel"] {
  const normalized = value.trim().toLowerCase();
  if (normalized === "critical") return "Critical";
  if (normalized === "high") return "High";
  if (normalized === "medium") return "Medium";
  return "Low";
}

function normalizeAuditResult(value: string): AuditRecord["result"] {
  const normalized = value.trim().toLowerCase();
  return normalized === "success" ? "Success" : "Failure";
}
