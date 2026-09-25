import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";
import { useCallback, useMemo, useState } from "react";
import { Search, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import {
  type AuditLogEntry,
  useGetOrganisationAuditLogsQuery,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import {
  buildAuditChangeRows,
  formatActionOptionLabel,
  formatAuditDateTime,
  formatAuditValue,
  getActionBadgeClass,
  getAuditListActionLabel,
  getChangePillClassName,
  getLogLevelBadgeClass,
  parseAuditDetails,
} from "@/pages/super-admin/audit-logs/auditLogDisplay.utils";

function toValidIsoString(value: string): string | undefined {
  if (!value.trim()) return undefined;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return undefined;
  return date.toISOString();
}

function AuditLogDetailModal(props: {
  entry: AuditLogEntry;
  onClose(): void;
}) {
  const { entry, onClose } = props;
  const detailItems = parseAuditDetails(entry.details);
  const changeRows = buildAuditChangeRows(entry.before, entry.after);
  const actionLabel = getAuditListActionLabel(entry);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4 py-6"
      onClick={onClose}
    >
      <div
        className="flex max-h-[90vh] w-full max-w-[51.25rem] flex-col overflow-hidden rounded-[1rem] border border-[#e3ebf3] bg-white shadow-[0_20px_60px_rgba(15,23,42,0.22)]"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4 border-b border-[#edf2f7] px-5 py-4">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-[0.75rem] font-medium text-[#8a96a3]">#{entry.id}</span>
              {entry.logLevel ? (
                <span
                  className={cn(
                    "inline-flex rounded-full px-2 py-0.5 text-[0.625rem] font-semibold uppercase",
                    getLogLevelBadgeClass(entry.logLevel),
                  )}
                >
                  {entry.logLevel}
                </span>
              ) : null}
              <span
                className={cn(
                  "inline-flex max-w-full items-center rounded-full px-2.5 py-0.5 text-[0.6875rem] font-semibold",
                  getActionBadgeClass(entry.action),
                )}
                title={formatActionOptionLabel(entry.action)}
              >
                {formatActionOptionLabel(entry.action)}
              </span>
            </div>
            <h3
              className="mt-2 break-words text-[1rem] font-semibold leading-6 text-[#1f2d38]"
              title={actionLabel}
            >
              {actionLabel}
            </h3>
            <p className="mt-1 text-[0.75rem] text-[#667483]">
              {formatAuditDateTime(entry.createdAt)}
            </p>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="h-8 w-8 shrink-0 rounded-full text-[#667483]"
            onClick={onClose}
            aria-label="Close audit log details"
          >
            <X size={16} />
          </Button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 py-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {[
              ["Resource Type", entry.resourceType || "—"],
              ["Resource ID", entry.resourceId || "—"],
              ["Auth ID", entry.authId != null ? `#${entry.authId}` : "System"],
              [
                "Organisation",
                entry.organisationName ||
                  (entry.organisationId != null ? `#${entry.organisationId}` : "—"),
              ],
            ].map(([label, value]) => (
              <div
                key={label}
                className="rounded-[0.75rem] border border-[#e3ebf3] bg-[#f8fbfd] px-3.5 py-3"
              >
                <div className="text-[0.625rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                  {label}
                </div>
                <div
                  className="mt-1 break-all text-[0.8125rem] leading-5 text-[#334155]"
                  title={value}
                >
                  {value}
                </div>
              </div>
            ))}
          </div>

          {entry.actionSummary ? (
            <div className="mt-4">
              <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                Summary
              </div>
              <p className="mt-1 break-words text-[0.8125rem] leading-5 text-[#465563]">
                {entry.actionSummary}
              </p>
            </div>
          ) : null}

          <div className="mt-4">
            <div className="text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
              Details
            </div>
            {detailItems.length === 0 ? (
              <div className="mt-1 text-[0.8125rem] text-[#465563]">—</div>
            ) : (
              <div className="mt-2 overflow-hidden rounded-[0.75rem] border border-[#e3ebf3] bg-white">
                {detailItems.map((item, index) => (
                  <div
                    key={`${entry.id}-detail-${item.key}-${index}`}
                    className={cn(
                      "grid grid-cols-1 gap-1 px-3.5 py-3 sm:grid-cols-[9.375rem_1fr] sm:gap-3",
                      index === 0 ? "" : "border-t border-[#edf2f7]",
                    )}
                  >
                    <div className="text-[0.75rem] font-semibold text-[#475569]">{item.key}</div>
                    <pre className="max-h-56 overflow-auto whitespace-pre-wrap break-words font-sans text-[0.75rem] leading-5 text-[#465563]">
                      {item.value}
                    </pre>
                  </div>
                ))}
              </div>
            )}
          </div>

          {changeRows.length > 0 ? (
            <div className="mt-4">
              <div className="mb-2 text-[0.6875rem] font-semibold uppercase tracking-[0.015rem] text-[#8a96a3]">
                Changes
              </div>
              <div className="overflow-hidden rounded-[0.75rem] border border-[#e3ebf3] bg-white">
                <div className="hidden grid-cols-[1.1fr_1.3fr_1.3fr_5.625rem] border-b border-[#edf2f7] bg-[#f7fafc] px-3 py-2 text-[0.625rem] font-semibold uppercase tracking-[0.0125rem] text-[#7b8794] sm:grid">
                  <div>Field</div>
                  <div>Before</div>
                  <div>After</div>
                  <div className="text-right">Type</div>
                </div>
                <div className="max-h-72 overflow-auto">
                  {changeRows.map((change) => (
                    <div
                      key={change.key}
                      className="grid grid-cols-1 gap-2 border-b border-[#f3f6f9] px-3 py-3 text-[0.75rem] last:border-b-0 sm:grid-cols-[1.1fr_1.3fr_1.3fr_5.625rem] sm:items-start sm:gap-0"
                    >
                      <div className="break-all font-mono text-[#334155]">
                        <span className="mr-2 font-sans text-[0.625rem] font-semibold uppercase text-[#8a96a3] sm:hidden">
                          Field
                        </span>
                        {change.key}
                      </div>
                      <div className="break-all text-[#64748b]">
                        <span className="mr-2 font-sans text-[0.625rem] font-semibold uppercase text-[#8a96a3] sm:hidden">
                          Before
                        </span>
                        {formatAuditValue(change.beforeValue)}
                      </div>
                      <div className="break-all text-[#1f2937]">
                        <span className="mr-2 font-sans text-[0.625rem] font-semibold uppercase text-[#8a96a3] sm:hidden">
                          After
                        </span>
                        {formatAuditValue(change.afterValue)}
                      </div>
                      <div className="sm:text-right">
                        <span
                          className={cn(
                            "inline-flex rounded-full px-2 py-0.5 text-[0.625rem] font-semibold capitalize",
                            getChangePillClassName(change.state),
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
}

function OrganisationAuditLogsTab(props: { organisationId: number | null }) {
  const [search, setSearch] = useState("");
  const [createdFrom, setCreatedFrom] = useState("");
  const [createdTo, setCreatedTo] = useState("");
  const [sort, setSort] = useState("createdAt");
  const [order, setOrder] = useState<"asc" | "desc">("desc");
  const listScope = JSON.stringify([props.organisationId, search, createdFrom, createdTo, sort, order]);
  const [page, setPage] = useScopedPage(listScope, 0);
  const size = 50;

  const [selectedEntry, setSelectedEntry] = useState<AuditLogEntry | null>(null);

  const filters = useMemo(
    () => ({
      id: props.organisationId ?? 0,
      page,
      size,
      q: search.trim() || undefined,
      createdFrom: toValidIsoString(createdFrom),
      createdTo: toValidIsoString(createdTo),
      sort,
      order,
    }),
    [createdFrom, createdTo, order, page, props.organisationId, search, size, sort],
  );


  const { currentData: data, isLoading, isError, error, isFetching } = useGetOrganisationAuditLogsQuery(
    filters,
    { skip: !props.organisationId, refetchOnMountOrArgChange: true },
  );
  const isFreshLoading = isLoading || (isFetching && page === 0);
  const { items: allRows, isReadyToLoadMore } = usePagedItems(data?.rows, page, listScope);
  const rows = data?.rows ?? [];
  const hasMore = isReadyToLoadMore && rows.length === size;





  const onLoadMore = useCallback(() => {
    if (!hasMore || isFetching) return;
    setPage((previous) => previous + 1);
  }, [hasMore, isFetching, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore,
    hasMore,
    isLoading: isFetching,
  });

  function handleCreatedFromChange(value: string) {
    if (createdTo && value && value > createdTo) {
      setCreatedFrom(createdTo);
    } else {
      setCreatedFrom(value);
    }
    setPage(0);
  }

  function handleCreatedToChange(value: string) {
    if (createdFrom && value && value < createdFrom) {
      setCreatedTo(createdFrom);
    } else {
      setCreatedTo(value);
    }
    setPage(0);
  }

  return (
    <div className="flex w-full flex-col gap-4 overflow-hidden">
      <div className="w-full overflow-x-auto pb-3 -mb-3 hide-scrollbar">
        <div className="flex min-w-[max-content] flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
            <div className="relative w-full lg:w-[16.25rem]">
              <Search
 className="size-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-[#667483]"
 aria-hidden="true" />
              <Input
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(0);
                }}
                placeholder="Search logs"
                className={cn(
                  "h-11 rounded-full border-[#dce5ee] bg-white pl-10 pr-4 shadow-none",
                  "placeholder:text-[#97a4b0] text-[#21303d]",
                )}
              />
            </div>
            <Input
              type="datetime-local"
              value={createdFrom}
              max={createdTo || undefined}
              onChange={(event) => handleCreatedFromChange(event.target.value)}
              className="h-11 w-[12.5rem] rounded-full border-[#dce5ee] bg-white px-4"
              aria-label="Created from"
            />
            <Input
              type="datetime-local"
              value={createdTo}
              min={createdFrom || undefined}
              onChange={(event) => handleCreatedToChange(event.target.value)}
              className="h-11 w-[12.5rem] rounded-full border-[#dce5ee] bg-white px-4"
              aria-label="Created to"
            />
          </div>
          <div className="flex items-center gap-2">
            <Select
              value={sort}
              onValueChange={(value) => {
                setSort(value);
                setPage(0);
              }}
            >
              <SelectTrigger className="h-11 w-[8.125rem] rounded-full border-[#dce5ee] bg-white px-4 [&[data-size=default]]:h-11">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="createdAt">createdAt</SelectItem>
                <SelectItem value="action">action</SelectItem>
              </SelectContent>
            </Select>
            <Select
              value={order}
              onValueChange={(value: "asc" | "desc") => {
                setOrder(value);
                setPage(0);
              }}
            >
              <SelectTrigger className="h-11 w-[6.25rem] rounded-full border-[#dce5ee] bg-white px-4 [&[data-size=default]]:h-11">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="desc">desc</SelectItem>
                <SelectItem value="asc">asc</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-[1rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        <div className="grid grid-cols-[4.5rem_1.6fr_0.7fr_0.7fr_1.1fr] items-center bg-[#f5f8fb] px-5 py-4 xl:grid-cols-[4.5rem_1.8fr_0.7fr_0.7fr_0.7fr_1.1fr]">
          {["ID", "Action", "Level", "Type", "Auth", "When"].map((col) => (
            <div
              key={col}
              className={cn(
                "text-[0.75rem] font-semibold leading-[1.125rem] text-[#23313d]",
                col === "Auth" && "hidden xl:block",
              )}
            >
              {col}
            </div>
          ))}
        </div>

        <div className="w-full divide-y divide-[#edf2f7]">
          {isFreshLoading ? (
            <div className="px-5 py-10 text-center text-sm text-[#667483]">
              Loading audit logs...
            </div>
          ) : isError ? (
            <div className="px-5 py-10 text-center text-sm text-[#b42318]">
              {getApiErrorMessage(error)}
            </div>
          ) : allRows.length === 0 ? (
            <div className="px-5 py-10 text-center text-sm text-[#667483]">
              No audit logs found.
            </div>
          ) : (
            allRows.map((row) => {
              const actionLabel = getAuditListActionLabel(row);
              return (
                <button
                  key={row.id}
                  type="button"
                  onClick={() => setSelectedEntry(row)}
                  className="grid w-full grid-cols-[4.5rem_1.6fr_0.7fr_0.7fr_1.1fr] items-start gap-x-2 px-5 py-3.5 text-left transition-colors hover:bg-[#f8fbfd] xl:grid-cols-[4.5rem_1.8fr_0.7fr_0.7fr_0.7fr_1.1fr]"
                >
                  <div className="pt-0.5 text-[0.6875rem] font-medium leading-5 text-[#8a96a3]">
                    #{row.id}
                  </div>
                  <div className="min-w-0 pr-2">
                    <span
                      title={actionLabel}
                      className={cn(
                        "inline-flex max-w-full items-center rounded-full px-2.5 py-0.5 text-[0.6875rem] font-semibold leading-4",
                        "truncate",
                        getActionBadgeClass(row.action),
                      )}
                    >
                      {actionLabel}
                    </span>
                    {row.actionSummary && row.actionSummary !== actionLabel ? (
                      <div
                        className="mt-1 line-clamp-1 text-[0.6875rem] leading-4 text-[#8a96a3]"
                        title={row.actionSummary}
                      >
                        {row.actionSummary}
                      </div>
                    ) : null}
                  </div>
                  <div>
                    {row.logLevel ? (
                      <span
                        className={cn(
                          "inline-flex rounded-full px-2 py-0.5 text-[0.625rem] font-semibold uppercase",
                          getLogLevelBadgeClass(row.logLevel),
                        )}
                      >
                        {row.logLevel}
                      </span>
                    ) : (
                      <span className="text-[0.75rem] text-[#b0bac5]">—</span>
                    )}
                  </div>
                  <div
                    className="truncate text-[0.8125rem] leading-5 text-[#465563]"
                    title={row.resourceType || "—"}
                  >
                    {row.resourceType || "—"}
                  </div>
                  <div className="hidden text-[0.8125rem] leading-5 text-[#465563] xl:block">
                    {row.authId != null ? (
                      `#${row.authId}`
                    ) : (
                      <span className="text-[0.6875rem] text-[#b0bac5]">System</span>
                    )}
                  </div>
                  <div className="text-[0.75rem] leading-5 text-[#667483]">
                    {formatAuditDateTime(row.createdAt)}
                  </div>
                </button>
              );
            })
          )}
          {!isFreshLoading && allRows.length > 0 ? (
            <div ref={observerTarget} className="h-4 w-full" aria-hidden="true" />
          ) : null}
        </div>
      </div>

      {selectedEntry ? (
        <AuditLogDetailModal
          entry={selectedEntry}
          onClose={() => setSelectedEntry(null)}
        />
      ) : null}
    </div>
  );
}

export default OrganisationAuditLogsTab;
