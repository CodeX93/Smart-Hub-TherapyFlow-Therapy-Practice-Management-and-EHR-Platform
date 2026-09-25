import { usePagedItems } from "@/hooks/usePagedItems";
import { useScopedPage } from "@/hooks/useScopedPage";
import { Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { useCallback, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import {
  useApplySuperAdminInvoiceCreditMutation,
  useApplySuperAdminInvoiceRefundMutation,
  useGetSuperAdminBillingInvoicesQuery,
  useLazyGetSuperAdminBillingInvoicesExportQuery,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import SemanticStatusBadge from "@/components/shared/SemanticStatusBadge";
import Toast from "@/components/shared/Toast";
import RefundDisputeModal, {
  type RefundDisputeModalData,
} from "./RefundDisputeModal";
import InvoiceActionsMenu from "./InvoiceActionsMenu";

interface InvoiceRow {
  id: string;
  invoiceNumericId?: number;
  organisation: string;
  plan?: string;
  invoiceId: string;
  period: string;
  dueDate: string;
  amount: string;
  currentBalance: string;
  status: string;
}

function getHeaderItems(): string[] {
  return ["Organization", "Invoice ID", "Period", "Due Date", "Amount", "Status", "Actions"];
}

function getSelectTriggerClassName(): string {
  return cn(
    "w-full rounded-[1.125rem] border-(--neutral-100) bg-(--surface-white) px-3 shadow-none",
    "data-[size=default]:h-10 text-(--text-gray-900)",
    "[&_svg]:text-(--text-neutral-400)"
  );
}

function getFilterOptions(values: string[]) {
  return values.map(function (value) {
    return (
      <SelectItem
        key={value}
        value={value}
        className="text-(--text-primary-dark) focus:bg-(--bg-primary-50) focus:text-(--text-primary-dark)"
      >
        <span className="block max-w-[11.25rem] truncate" title={value}>
          {value}
        </span>
      </SelectItem>
    );
  });
}

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

function formatCurrency(amount: number): string {
  return currencyFormatter.format(Number.isFinite(amount) ? amount : 0);
}

function getSortValue(dateFilter: string): string | undefined {
  if (dateFilter === "Newest") return "dueDate,desc";
  if (dateFilter === "Oldest") return "dueDate,asc";
  return undefined;
}

const INVOICE_STATUS_FILTER_OPTIONS = [
  { label: "Status: All", apiValue: undefined },
  { label: "Pending", apiValue: "PENDING" },
  { label: "Paid", apiValue: "PAID" },
  { label: "Failed", apiValue: "FAILED" },
  { label: "Past Due", apiValue: "PAST_DUE" },
] as const;

function toInvoiceStatusQueryParam(statusFilter: string): string | undefined {
  const match = INVOICE_STATUS_FILTER_OPTIONS.find((option) => option.label === statusFilter);
  return match?.apiValue;
}

function AllInvoicesPanel() {
  const navigate = useNavigate();
  const ignoreRowClickRef = useRef(false);
  const [statusFilter, setStatusFilter] = useState("Status: All");
  const [planFilter, setPlanFilter] = useState("Plan: All");
  const [dateFilter, setDateFilter] = useState("Due Date");
  const listScope = JSON.stringify([statusFilter, dateFilter]);
  const [page, setPage] = useScopedPage(listScope, 0);



  const [selectedInvoice, setSelectedInvoice] =
    useState<RefundDisputeModalData | null>(null);
  const [adjustmentMode, setAdjustmentMode] = useState<"refund" | "credit">(
    "refund"
  );
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const statusQueryValue = toInvoiceStatusQueryParam(statusFilter);

  const queryArgs = useMemo(
    () => ({
      page,
      pageSize: 50,
      status: statusQueryValue,
      sort: getSortValue(dateFilter),
    }),
    [page, statusQueryValue, dateFilter]
  );

  const { currentData: data, isLoading, isFetching, isError, error } =
    useGetSuperAdminBillingInvoicesQuery(queryArgs);
  const [triggerExportCsv, { isFetching: isExportingCsv }] =
    useLazyGetSuperAdminBillingInvoicesExportQuery();
  const [applySuperAdminInvoiceRefund, { isLoading: isApplyingRefund }] =
    useApplySuperAdminInvoiceRefundMutation();
  const [applySuperAdminInvoiceCredit, { isLoading: isApplyingCredit }] =
    useApplySuperAdminInvoiceCreditMutation();



      const mappedRows = useMemo(() => data?.items.map((item) => ({
      id: item.id,
      invoiceNumericId: item.invoiceNumericId,
      organisation: item.organisation,
      plan: item.plan,
      invoiceId: item.invoiceId,
      period: item.period,
      dueDate: item.dueDate,
      amount: formatCurrency(item.amount),
      currentBalance: formatCurrency(item.currentBalance),
      status: item.status,
    })), [data?.items]);
  const { items: rows, isReadyToLoadMore } = usePagedItems(mappedRows, page, listScope);
  const totalPages = data?.totalPages ?? 0;
  const totalItems = data?.totalItems ?? 0;

  const visibleRows = useMemo(() => {
    if (planFilter === "Plan: All") return rows;
    const normalizedPlan = planFilter.toLowerCase();
    return rows.filter((row) => row.plan?.toLowerCase().includes(normalizedPlan));
  }, [rows, planFilter]);

  const hasMore = isReadyToLoadMore && page + 1 < totalPages;
  const handleLoadMore = useCallback(() => {
    if (hasMore && !isFetching) {
      setPage((previousPage) => previousPage + 1);
    }
  }, [hasMore, isFetching, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore,
    isLoading: isFetching,
  });

  function suppressNextRowClick() {
    ignoreRowClickRef.current = true;

    window.setTimeout(function () {
      ignoreRowClickRef.current = false;
    }, 0);
  }

  function handleViewInvoice(invoiceId: string) {
    navigate("/super-admin/billings-and-plans/invoices/" + invoiceId);
  }

  function handleApplyCredit(row: InvoiceRow) {
    setAdjustmentMode("credit");
    setSelectedInvoice({
      invoiceId: row.invoiceId,
      currentBalance: row.currentBalance,
      outstandingBalance: row.currentBalance,
      invoiceNumericId: row.invoiceNumericId,
    });
  }

  function handleOpenRefundModal(row: InvoiceRow) {
    setAdjustmentMode("refund");
    setSelectedInvoice({
      invoiceId: row.invoiceId,
      currentBalance: row.currentBalance,
      outstandingBalance: row.currentBalance,
      invoiceNumericId: row.invoiceNumericId,
    });
  }

  function handleCloseRefundModal() {
    setSelectedInvoice(null);
  }

  async function handleSubmitRefund(refundAmount: string, reason: string) {
    if (!selectedInvoice?.invoiceNumericId) {
      setToastType("error");
      setToastMessage("Unable to apply refund: invalid invoice ID.");
      return;
    }

    const amountUsd = Number.parseFloat(refundAmount);
    if (!Number.isFinite(amountUsd) || amountUsd <= 0) {
      setToastType("error");
      setToastMessage("Please enter a valid refund amount.");
      return;
    }

    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setToastType("error");
      setToastMessage("Please enter a reason for the refund.");
      return;
    }

    try {
      if (adjustmentMode === "credit") {
        await applySuperAdminInvoiceCredit({
          invoiceId: selectedInvoice.invoiceNumericId,
          body: {
            amountUsd,
            reason: trimmedReason,
          },
        }).unwrap();
      } else {
        await applySuperAdminInvoiceRefund({
          invoiceId: selectedInvoice.invoiceNumericId,
          body: {
            amountUsd,
            reason: trimmedReason,
          },
        }).unwrap();
      }
      setSelectedInvoice(null);
      setToastType("success");
      setToastMessage(
        adjustmentMode === "credit"
          ? "Credit applied successfully."
          : "Refund applied successfully."
      );
    } catch (refundError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(refundError));
    }
  }

  async function handleExportCsv() {
    try {
      const blob = await triggerExportCsv({
        status: statusQueryValue,
      }).unwrap();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `super-admin-invoices-${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (exportError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(exportError));
    }
  }

  return (
    <div className="flex w-full flex-col gap-4">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap items-center gap-3">
          <div className="w-full sm:w-[7.625rem]">
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className={getSelectTriggerClassName()}>
                <SelectValue placeholder="Status: All" />
              </SelectTrigger>
              <SelectContent className="border-(--neutral-100) bg-(--surface-white) shadow-[0px_12px_24px_var(--shadow)]">
                {getFilterOptions(
                  INVOICE_STATUS_FILTER_OPTIONS.map((option) => option.label)
                )}
              </SelectContent>
            </Select>
          </div>

          <div className="w-full sm:w-[6.3125rem]">
            <Select value={planFilter} onValueChange={setPlanFilter}>
              <SelectTrigger className={getSelectTriggerClassName()}>
                <SelectValue placeholder="Plan: All" className="truncate" />
              </SelectTrigger>
              <SelectContent className="border-(--neutral-100) bg-(--surface-white) shadow-[0px_12px_24px_var(--shadow)]">
                {getFilterOptions(["Plan: All", "Enterprise", "Professional", "Trial"])}
              </SelectContent>
            </Select>
          </div>

          <div className="w-full sm:w-[6.9375rem]">
            <Select value={dateFilter} onValueChange={setDateFilter}>
              <SelectTrigger className={getSelectTriggerClassName()}>
                <SelectValue placeholder="Due Date" />
              </SelectTrigger>
              <SelectContent className="border-(--neutral-100) bg-(--surface-white) shadow-[0px_12px_24px_var(--shadow)]">
                {getFilterOptions(["Due Date", "Newest", "Oldest"])}
              </SelectContent>
            </Select>
          </div>
        </div>

        <Button
          variant="secondary"
          size="md"
          onClick={handleExportCsv}
          disabled={isExportingCsv}
          loading={isExportingCsv}
          loadingLabel="Exporting..."
        >
          <Download size={16} aria-hidden="true" />
          Export CSV
        </Button>
      </div>

      <div className="overflow-hidden rounded-[1.25rem] border border-(--neutral-100) bg-(--surface-white) shadow-[0_2px_2px_0_var(--shadow)]">
        <div className="grid grid-cols-[1.55fr_1fr_1fr_1fr_0.8fr_0.9fr_0.45fr] items-center bg-(--bg-primary-50) px-4 py-3">
          {getHeaderItems().map(function (header) {
            return (
              <div
                key={header}
                className={cn(
                  "text-(--text-gray-900) text-xs font-medium leading-4",
                  header === "Actions" ? "text-center" : ""
                )}
              >
                {header}
              </div>
            );
          })}
        </div>

        {isLoading ? (
          <div className="px-4 py-8 text-center text-sm font-medium text-[#667483]">
            Loading invoices...
          </div>
        ) : isError ? (
          <div className="px-4 py-8 text-sm font-medium text-(--status-denied)">
            {getApiErrorMessage(error)}
          </div>
        ) : visibleRows.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm font-medium text-[#667483]">
            No invoices found.
          </div>
        ) : (
          visibleRows.map(function (row, index) {
          return (
            <div
              key={row.id + "-" + index}
              className={cn(
                "grid grid-cols-[1.55fr_1fr_1fr_1fr_0.8fr_0.9fr_0.45fr] items-center px-4 py-4 transition-colors hover:bg-[#fafcfe]",
                index === 0 ? "" : "border-t border-(--neutral-100)"
              )}
              role="button"
              tabIndex={0}
              onClick={function () {
                if (ignoreRowClickRef.current) {
                  ignoreRowClickRef.current = false;
                  return;
                }

                handleViewInvoice(row.invoiceId);
              }}
              onKeyDown={function (event) {
                if (ignoreRowClickRef.current) {
                  ignoreRowClickRef.current = false;
                  return;
                }

                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  handleViewInvoice(row.invoiceId);
                }
              }}
            >
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.organisation}
              </div>
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.invoiceId}
              </div>
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.period}
              </div>
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.dueDate}
              </div>
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.amount}
              </div>
              <div>
                <SemanticStatusBadge
                  status={row.status}
                  className="inline-flex h-5 items-center rounded-full px-2 text-[0.625rem] font-medium leading-4"
                />
              </div>
              <div
                className="flex justify-center"
                onClick={function (event) {
                  event.stopPropagation();
                }}
              >
                <InvoiceActionsMenu
                  onActionStart={suppressNextRowClick}
                  onViewInvoice={function () {
                    handleViewInvoice(row.invoiceId);
                  }}
                  onApplyCredit={function () {
                    handleApplyCredit(row);
                  }}
                  onProcessRefund={function () {
                    handleOpenRefundModal(row);
                  }}
                />
              </div>
            </div>
          );
          })
        )}
      </div>

      {!isLoading && visibleRows.length > 0 ? (
        <div className="text-xs text-(--text-neutral-600)">
          Showing {visibleRows.length} of {totalItems} invoices
        </div>
      ) : null}

      <div
        ref={observerTarget}
        className="h-10 w-full flex items-center justify-center"
      >
        {null}
      </div>

      <RefundDisputeModal
        open={selectedInvoice !== null}
        invoice={selectedInvoice}
        mode={adjustmentMode}
        onClose={handleCloseRefundModal}
        isSubmitting={isApplyingRefund || isApplyingCredit}
        onSubmit={handleSubmitRefund}
      />
    </div>
  );
}

export default AllInvoicesPanel;
