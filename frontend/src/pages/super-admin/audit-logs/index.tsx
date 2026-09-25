import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";
import { useCallback, useMemo, useState } from "react";
import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import { cn } from "@/lib/utils";
import {
  useGetAuditLogsQuery, type AuditLogFilters
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import {
  buildAuditChangeRows,
  formatActionOptionLabel,
  formatAuditDateTime,
  formatAuditValue,
  getActionBadgeClass,
  getChangePillClassName,
  normalizeBackendTokens,
  parseAuditDetails,
} from "./auditLogDisplay.utils";

const ACTION_OPTIONS = [
  "All Actions",
  "PLAN_CREATED",
  "PLAN_UPDATED",
  "PLAN_ARCHIVED",
  "ORGANISATION_ONBOARDED",
  "ORGANISATION_SUSPENDED",
  "ORGANISATION_REACTIVATED",
  "TENANT_SCHEMA_DRIFT",
  "IMPERSONATION_STARTED",
  "IMPERSONATION_ENDED",
];

const RESOURCE_TYPE_OPTIONS = [
  "All Types",
  "Organisation",
  "SubscriptionPlan",
  "User",
  "Feature",
];

function AuditLogs() {
  const [search, setSearch] = useState("");
  const [actionFilter, setActionFilter] = useState("All Actions");
  const [resourceTypeFilter, setResourceTypeFilter] = useState("All Types");
  const listScope = JSON.stringify([actionFilter, resourceTypeFilter]);
  const [page, setPage] = useScopedPage(listScope, 0);
  const pageSize = 50;

  const [hoveredRowId, setHoveredRowId] = useState<number | null>(null);

  const filters = useMemo<AuditLogFilters>(
    () => ({
      page,
      size: pageSize,
      sort: "createdAt",
      order: "desc",
      mine: false,
      action: actionFilter === "All Actions" ? undefined : actionFilter,
      resourceType: resourceTypeFilter === "All Types" ? undefined : resourceTypeFilter,
      q: undefined,
    }),
    [page, pageSize, actionFilter, resourceTypeFilter]
  );


  const { currentData: data, isLoading, isError, error, isFetching } =
    useGetAuditLogsQuery(filters);

  const { items: allRows, isReadyToLoadMore } = usePagedItems(data?.rows, page, listScope);
  const rows = data?.rows ?? [];
  const hasMore = isReadyToLoadMore && rows.length === pageSize;
  const isLoadingMore = isFetching && page > 0 && allRows.length > 0;





  const onLoadMore = useCallback(() => {
    if (!hasMore || isFetching) return;
    setPage((previous) => previous + 1);
  }, [hasMore, isFetching, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore,
    hasMore,
    isLoading: isFetching,
  });

  function handleSearchChange(e: React.ChangeEvent<HTMLInputElement>) {
    setSearch(e.target.value);
    setHoveredRowId(null);
  }

  function handleActionChange(value: string) {
    setActionFilter(value);
    setPage(0);
  }

  function handleResourceTypeChange(value: string) {
    setResourceTypeFilter(value);
    setPage(0);
  }

  const filteredRows = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return allRows;

    return allRows.filter((row) => {
      const haystack = [
        String(row.id ?? ""),
        formatActionOptionLabel(row.action ?? ""),
        row.action ?? "",
        row.organisationName ?? "",
        row.resourceId ?? "",
        row.resourceType ?? "",
        row.details ? normalizeBackendTokens(row.details) : "",
      ]
        .join(" ")
        .toLowerCase();
      return haystack.includes(query);
    });
  }, [allRows, search]);

  return (
    <SuperAdminPageShell
      title="Audit Logs"
      description="Read-only platform audit trail. Track all administrative actions across the system."
    >
      {/* Filters Row */}
      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
          {/* Search */}
          <div className="relative w-full lg:w-[17.5rem]">
            <Search
 className="size-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-[#667483]"
 aria-hidden="true" />
            <Input
              value={search}
              onChange={handleSearchChange}
              placeholder="Search by ID, details..."
              className={cn(
                "h-11 rounded-full border-[#dce5ee] bg-white pl-10 pr-4 shadow-none",
                "placeholder:text-[#97a4b0] text-[#21303d]"
              )}
            />
          </div>

          {/* Action filter */}
          <Select value={actionFilter} onValueChange={handleActionChange}>
            <SelectTrigger
              className={cn(
                "h-11 w-full rounded-full border-[#dce5ee] bg-white px-4 shadow-none sm:w-[12.5rem]",
                "text-[#52606d] [&_svg]:text-[#97a4b0]"
              )}
            >
              <SelectValue placeholder="Action: All" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {ACTION_OPTIONS.map((option) => (
                <SelectItem
                  key={option}
                  value={option}
                  className="text-[#1f2d38] focus:bg-[#f4f7fa] focus:text-[#1f2d38]"
                >
                  {formatActionOptionLabel(option)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          {/* Resource type filter */}
          <Select value={resourceTypeFilter} onValueChange={handleResourceTypeChange}>
            <SelectTrigger
              className={cn(
                "h-11 w-full rounded-full border-[#dce5ee] bg-white px-4 shadow-none sm:w-[10rem]",
                "text-[#52606d] [&_svg]:text-[#97a4b0]"
              )}
            >
              <SelectValue placeholder="Type: All" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {RESOURCE_TYPE_OPTIONS.map((option) => (
                <SelectItem
                  key={option}
                  value={option}
                  className="text-[#1f2d38] focus:bg-[#f4f7fa] focus:text-[#1f2d38]"
                >
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="text-xs text-[#667483]">Infinite loading enabled</div>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-[1rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        {/* Header */}
        <div className="grid grid-cols-[5rem_1.4fr_1fr_1fr_0.8fr_1.5fr] items-center bg-[#f5f8fb] px-5 py-4">
          {["ID", "Action", "Organisation", "Resource ID", "Auth ID", "Timestamp"].map((col) => (
            <div key={col} className="text-[#23313d] text-[0.75rem] font-semibold leading-[1.125rem]">
              {col}
            </div>
          ))}
        </div>

        {/* Body */}
        <div className="w-full">
          {isLoading || (isFetching && allRows.length === 0) ? (
            <div className="px-5 py-10 text-center text-sm text-[#667483]">
              Loading audit logs...
            </div>
          ) : isError ? (
            <div className="px-5 py-10 text-center text-sm text-[#b42318]">
              {getApiErrorMessage(error)}
            </div>
          ) : filteredRows.length === 0 ? (
            <div className="px-5 py-10 text-center text-sm text-[#667483]">
              No audit logs found for the current filters.
            </div>
          ) : (
            filteredRows.map((row) => {
              const changeRows = buildAuditChangeRows(row.before, row.after);
              const detailItems = parseAuditDetails(row.details);

              return (
                <div
                  key={row.id}
                  className="border-b border-[#edf2f7] last:border-b-0"
                  onMouseEnter={() => setHoveredRowId(row.id)}
                  onMouseLeave={() => setHoveredRowId((previous) => (previous === row.id ? null : previous))}
                >
                <div className="grid grid-cols-[5rem_1.4fr_1fr_1fr_0.8fr_1.5fr] items-start px-5 py-3.5">
                  {/* ID */}
                  <div className="text-[#8a96a3] text-[0.6875rem] font-medium leading-5 pt-0.5">
                    #{row.id}
                  </div>

                  {/* Action */}
                  <div>
                    <span
                      title={formatActionOptionLabel(row.action)}
                      className={cn(
                        "inline-flex max-w-[13.75rem] items-center rounded-full px-2.5 py-0.5 text-[0.6875rem] font-semibold leading-4",
                        "truncate whitespace-nowrap",
                        getActionBadgeClass(row.action)
                      )}
                    >
                      {formatActionOptionLabel(row.action)}
                    </span>
                  </div>

                  {/* Organisation */}
                  <div
                    className="min-w-0 truncate pr-2 text-[#465563] text-[0.8125rem] font-normal leading-5"
                    title={row.organisationName || "—"}
                  >
                    {row.organisationName || "—"}
                  </div>

                  {/* Resource ID */}
                  <div className="text-[#465563] text-[0.8125rem] font-mono leading-5">
                    {row.resourceId || "—"}
                  </div>

                  {/* Auth ID */}
                  <div className="text-[#465563] text-[0.8125rem] font-normal leading-5">
                    {row.authId != null ? `#${row.authId}` : <span className="text-[#b0bac5] text-[0.6875rem]">System</span>}
                  </div>

                  {/* Timestamp */}
                  <div className="text-[#667483] text-[0.75rem] leading-5">
                    {formatAuditDateTime(row.createdAt)}
                  </div>
                </div>
                  <div
                    className={cn(
                      "overflow-hidden px-5 transition-all duration-200",
                      hoveredRowId === row.id
                        ? "max-h-[80vh] overflow-y-auto pb-4 opacity-100"
                        : "max-h-0 pb-0 opacity-0"
                    )}
                  >
                    <div className="rounded-[0.75rem] border border-[#e3ebf3] bg-[#f8fbfd] p-4">
                      <div className="grid grid-cols-1 gap-3">
                        <div>
                          <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                            Details
                          </div>
                          {detailItems.length === 0 ? (
                            <div className="mt-1 text-[0.75rem] leading-5 text-[#465563]">—</div>
                          ) : (
                            <div className="mt-2 overflow-hidden rounded-[0.625rem] border border-[#e3ebf3] bg-white">
                              {detailItems.map((item, index) => (
                                <div
                                  key={`${row.id}-detail-${item.key}-${index}`}
                                  className={cn(
                                    "grid grid-cols-[10rem_1fr] gap-3 px-3 py-2.5 text-[0.75rem]",
                                    index === 0 ? "" : "border-t border-[#edf2f7]"
                                  )}
                                >
                                  <div className="font-semibold text-[#475569]">{item.key}</div>
                                  <pre className="max-h-40 overflow-auto whitespace-pre-wrap break-words font-sans leading-5 text-[#465563]">
                                    {item.value}
                                  </pre>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                        <div>
                          <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                            Context
                          </div>
                          <div className="mt-1 text-[0.75rem] leading-5 text-[#465563]">
                            <div>Action: {formatActionOptionLabel(row.action)}</div>
                            <div>Resource Type: {row.resourceType || "—"}</div>
                            <div>Organisation ID: {row.organisationId ?? "—"}</div>
                          </div>
                        </div>
                      </div>

                      {changeRows.length > 0 ? (
                        <div className="mt-4">
                          <div className="mb-2 text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                            Changes
                          </div>
                          <div className="overflow-hidden rounded-[0.625rem] border border-[#e3ebf3] bg-white">
                            <div className="grid grid-cols-[1.2fr_1.4fr_1.4fr_5.625rem] border-b border-[#edf2f7] bg-[#f7fafc] px-3 py-2 text-[0.625rem] font-semibold uppercase tracking-[0.0125rem] text-[#7b8794]">
                              <div>Field</div>
                              <div>Before</div>
                              <div>After</div>
                              <div className="text-right">Type</div>
                            </div>
                            <div className="max-h-48 overflow-auto">
                              {changeRows.map((change) => (
                                <div
                                  key={change.key}
                                  className="grid grid-cols-[1.2fr_1.4fr_1.4fr_5.625rem] items-start border-b border-[#f3f6f9] px-3 py-2.5 text-[0.75rem] last:border-b-0"
                                >
                                  <div className="font-mono text-[#334155] break-all">{change.key}</div>
                                  <div className="text-[#64748b] break-all">
                                    {formatAuditValue(change.beforeValue)}
                                  </div>
                                  <div className="text-[#1f2937] break-all">
                                    {formatAuditValue(change.afterValue)}
                                  </div>
                                  <div className="text-right">
                                    <span
                                      className={cn(
                                        "inline-flex rounded-full px-2 py-0.5 text-[0.625rem] font-semibold capitalize",
                                        getChangePillClassName(change.state)
                                      )}
                                    >
                                      {change.state}
                                    </span>
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>
                        </div>
                      ) : null}
                    </div>
                  </div>
                </div>
              );
            })
          )}
          {!isLoading && filteredRows.length > 0 ? (
            <div ref={observerTarget} className="h-4 w-full" aria-hidden="true" />
          ) : null}
          {isLoadingMore ? (
            <div className="px-5 py-4 text-center">
              <Button
                type="button"
                variant="secondary"
                size="sm"
                disabled
                loading
                loadingLabel="Loading more audit logs"
              >
                Loading more...
              </Button>
            </div>
          ) : null}
        </div>
      </div>
    </SuperAdminPageShell>
  );
}

export default AuditLogs;
