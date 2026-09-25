import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";

import { ContentLoader } from "@/components/shared/ContentLoader";
import { useCallback, useMemo, useRef, useState } from "react";
import { Download } from "lucide-react";
import CustomDatePicker from "@/components/form/CustomDatePicker";
import CustomSelect, { type CustomSelectOption } from "@/components/form/CustomSelect";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import {
  useGetAdminClientsQuery,
  useGetClientSmsLogQuery,
  useLazyExportClientSmsLogQuery,
} from "@/store/api/admin/clients.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";

type SmsLogDetails = {
  messageSid?: string;
  eventType?: string;
  reason?: string;
  error?: string;
  errorCode?: string;
};

function parseSmsLogDetails(raw?: string | null): SmsLogDetails {
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw) as SmsLogDetails;
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function humanizeLabel(value?: string): string {
  if (!value) return "—";
  return value
    .replace(/_/g, " ")
    .trim()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function getSmsOutcomeLabel(action: string, result: string): string {
  const normalizedAction = action.toLowerCase();
  const normalizedResult = result.toLowerCase();
  if (normalizedAction === "sms_notification_sent") return "Sent";
  if (normalizedAction === "sms_notification_blocked") return "Blocked";
  if (normalizedAction === "sms_notification_failed") return "Failed";
  if (normalizedAction === "sms_notification_skipped") return "Skipped";
  return humanizeLabel(normalizedResult || normalizedAction);
}

function getOutcomeClasses(label: string): string {
  switch (label.toLowerCase()) {
    case "sent":
      return "bg-green-50 text-green-700";
    case "blocked":
      return "bg-amber-50 text-amber-700";
    case "failed":
      return "bg-red-50 text-red-700";
    case "skipped":
      return "bg-slate-100 text-slate-700";
    default:
      return "bg-slate-100 text-slate-700";
  }
}

function getFriendlySmsReason(details: SmsLogDetails): string {
  const normalizedReason = details.reason?.toLowerCase() ?? "";
  const normalizedErrorCode = details.errorCode?.toUpperCase() ?? "";

  if (details.error) return details.error;

  if (normalizedReason === "sms_not_configured" || normalizedErrorCode === "NOT_CONFIGURED") {
    return "SMS is not configured for this practice yet.";
  }
  if (normalizedReason === "missing_or_withdrawn_consent") {
    return "Client SMS consent is missing or has been withdrawn.";
  }
  if (normalizedReason === "missing_or_invalid_phone") {
    return "Client primary phone is missing or invalid.";
  }
  if (normalizedErrorCode === "INVALID_CONFIGURATION") {
    return "SMS configuration is invalid. Contact an administrator.";
  }
  if (normalizedErrorCode === "AUTHENTICATION_FAILED") {
    return "SMS provider authentication failed. Contact an administrator.";
  }
  if (normalizedErrorCode === "INSUFFICIENT_CREDITS") {
    return "SMS provider balance is too low.";
  }
  if (normalizedErrorCode === "ACCOUNT_SUSPENDED") {
    return "SMS provider account is suspended.";
  }
  if (normalizedErrorCode === "INVALID_DESTINATION") {
    return "The client phone number is not a valid SMS destination.";
  }
  if (normalizedErrorCode === "PROVIDER_ERROR") {
    return "The SMS provider returned an error. Please try again later.";
  }

  return details.reason || details.errorCode || "—";
}

const AdminSmsNotificationLogs = () => {
  const [selectedClientId, setSelectedClientId] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const listScope = JSON.stringify([selectedClientId, fromDate, toDate]);
  const [page, setPage] = useScopedPage(listScope);

  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("success");

  const scrollRootRef = useRef<HTMLDivElement | null>(null);

  const { data: clientsResponse, isFetching: isFetchingClients } = useGetAdminClientsQuery({
    page: 1,
    pageSize: 100,
  });

  const clientOptions = useMemo<CustomSelectOption[]>(() => {
    const items = clientsResponse?.items ?? [];
    if (isFetchingClients && items.length === 0) {
      return [{ value: "", label: "Loading clients...", disabled: true }];
    }
    if (items.length === 0) {
      return [{ value: "", label: "No clients found", disabled: true }];
    }
    return items.map((client) => ({
      value: String(client.id),
      label: client.fullName?.trim() || client.clientId || `Client #${client.id}`,
    }));
  }, [clientsResponse?.items, isFetchingClients]);

  const selectedClientName =
    clientsResponse?.items.find((client) => String(client.id) === selectedClientId)?.fullName ||
    "client";



  const {
    currentData: smsLogResponse,
    isFetching: isFetchingSmsLog,
    error: smsLogError,
  } = useGetClientSmsLogQuery(
    {
      id: Number(selectedClientId),
      page,
      pageSize: 25,
      from: fromDate ? `${fromDate}T00:00:00Z` : undefined,
      to: toDate ? `${toDate}T23:59:59Z` : undefined,
    },
    { skip: !selectedClientId },
  );

  const [triggerExportSmsLog, { isFetching: isExportingSmsLog }] = useLazyExportClientSmsLogQuery();

  const { items: allSmsLogs, isReadyToLoadMore } = usePagedItems(smsLogResponse?.items, page, listScope);
  const canLoadMoreLogs = isReadyToLoadMore && page < (smsLogResponse?.totalPages ?? 1);

  const handleExport = () => {
    if (!selectedClientId) return;
    void triggerExportSmsLog({
      id: Number(selectedClientId),
      from: fromDate ? `${fromDate}T00:00:00Z` : undefined,
      to: toDate ? `${toDate}T23:59:59Z` : undefined,
    })
      .unwrap()
      .then((content) => {
        const blob = new Blob([content], { type: "text/csv;charset=utf-8" });
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = `sms-log-${selectedClientName.toLowerCase().replace(/[^a-z0-9]+/g, "-") || "client"}.csv`;
        anchor.click();
        window.URL.revokeObjectURL(url);
        setToastType("success");
        setToastMessage("SMS log export started.");
      })
      .catch((error) => {
        setToastType("error");
        setToastMessage(getApiErrorMessage(error));
      });
  };

  const items = allSmsLogs;
  const totalPages = smsLogResponse?.totalPages ?? 1;
  const hasMoreLogs = page < totalPages;
  const isLoadingMoreLogs = isFetchingSmsLog && page > 1;
  const loadMoreLogs = useCallback(() => {
    if (isFetchingSmsLog || !hasMoreLogs || !canLoadMoreLogs) return;

    setPage((previous) => previous + 1);
  }, [canLoadMoreLogs, hasMoreLogs, isFetchingSmsLog, setPage]);
  const { observerTarget } = useInfiniteScroll({
    onLoadMore: loadMoreLogs,
    hasMore: hasMoreLogs,
    isLoading: isFetchingSmsLog,
    rootMargin: "0px 0px 120px 0px",
    threshold: 0.8,
    scrollRootRef,
  });

  return (
    <div className="flex h-[calc(100vh-10.5rem)] min-h-0 flex-col overflow-hidden rounded-2xl border border-(--neutral-100) bg-white">
      <div className="shrink-0 border-b border-(--neutral-100) bg-white px-5 py-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-(--text-primary-dark)">SMS Notifications</h2>
          <Button
            type="button"
            variant="outline"
            onClick={handleExport}
            disabled={!selectedClientId || isExportingSmsLog}
            loading={isExportingSmsLog}
            loadingLabel="Exporting CSV..."
            className="h-9 rounded-full px-4"
          >
            <Download className="mr-2 h-4 w-4" />
            Export CSV
          </Button>
        </div>
      </div>

      <div className="shrink-0 border-b border-(--neutral-100) bg-white px-5 py-4">
        <div className="grid grid-cols-1 gap-3 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_minmax(0,1fr)_auto]">
          <CustomSelect
            label="Client"
            value={selectedClientId}
            onChange={setSelectedClientId}
            options={clientOptions}
            placeholder="Search client..."
            isSearch
          />
          <CustomDatePicker
            label="From"
            date={fromDate || null}
            onDateChange={(date) => {
              if (!date) {
                setFromDate("");
                return;
              }
              const year = date.getFullYear();
              const month = String(date.getMonth() + 1).padStart(2, "0");
              const day = String(date.getDate()).padStart(2, "0");
              const nextFromDate = `${year}-${month}-${day}`;
              setFromDate(nextFromDate);
              if (toDate && toDate < nextFromDate) {
                setToDate("");
              }
            }}
          />
          <CustomDatePicker
            label="To"
            date={toDate || null}
            minDate={fromDate || null}
            onDateChange={(date) => {
              if (!date) {
                setToDate("");
                return;
              }
              const year = date.getFullYear();
              const month = String(date.getMonth() + 1).padStart(2, "0");
              const day = String(date.getDate()).padStart(2, "0");
              setToDate(`${year}-${month}-${day}`);
            }}
          />
          <div className="flex items-end">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setFromDate("");
                setToDate("");
              }}
              disabled={!fromDate && !toDate}
              className="h-15 rounded-xl px-4"
            >
              Clear
            </Button>
          </div>
        </div>
      </div>

      <div ref={scrollRootRef} className="min-h-0 flex-1 overflow-y-auto p-5">
        {!selectedClientId ? (
          <div className="flex h-full items-center justify-center rounded-2xl border border-dashed border-(--neutral-100) bg-(--neutral-50)">
            <p className="text-sm text-(--text-neutral-600)">
              Select a client to view SMS delivery history.
            </p>
          </div>
        ) : isFetchingSmsLog && items.length === 0 ? (
          <ContentLoader />
        ) : smsLogError ? (
          <div className="flex h-full items-center justify-center rounded-2xl border border-(--status-denied)/20 bg-(--status-denied)/5 p-6">
            <p className="text-sm text-(--status-denied)">{getApiErrorMessage(smsLogError)}</p>
          </div>
        ) : items.length === 0 ? (
          <div className="flex h-full items-center justify-center rounded-2xl border border-dashed border-(--neutral-100) bg-(--neutral-50)">
            <p className="text-sm text-(--text-neutral-600)">No SMS logs found.</p>
          </div>
        ) : (
          <div className="overflow-hidden rounded-2xl border border-(--neutral-100)">
            <table className="min-w-full divide-y divide-(--neutral-100)">
              <thead className="bg-(--neutral-50)">
                <tr className="text-left text-sm font-medium text-(--text-neutral-600)">
                  <th className="px-4 py-3">Date / Time</th>
                  <th className="px-4 py-3">Outcome</th>
                  <th className="px-4 py-3">Event</th>
                  <th className="px-4 py-3">Twilio SID</th>
                  <th className="px-4 py-3">Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-(--neutral-100) bg-white">
                {items.map((item) => {
                  const details = parseSmsLogDetails(item.details);
                  const outcomeLabel = getSmsOutcomeLabel(item.action, item.result);
                  return (
                    <tr key={item.id} className="align-top text-sm text-(--text-primary-dark)">
                      <td className="px-4 py-3 whitespace-nowrap">
                        {item.timestamp ? new Date(item.timestamp).toLocaleString() : "—"}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${getOutcomeClasses(outcomeLabel)}`}>
                          {outcomeLabel}
                        </span>
                      </td>
                      <td className="px-4 py-3">{humanizeLabel(details.eventType)}</td>
                      <td className="px-4 py-3 break-all">{details.messageSid || "—"}</td>
                      <td className="px-4 py-3 break-words">
                        {getFriendlySmsReason(details)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {selectedClientId && items.length > 0 ? (
          <div className="flex flex-col items-center gap-3 py-4">
            {isLoadingMoreLogs ? (
              <div className="flex items-center gap-2 text-sm text-(--text-neutral-600)">
                <ContentLoader variant="inline" size="sm" />
              </div>
            ) : null}
            {hasMoreLogs ? <div ref={observerTarget} className="h-1 w-full" /> : null}
          </div>
        ) : null}
      </div>

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

export default AdminSmsNotificationLogs;
