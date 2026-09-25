import { useMemo, useRef, useState } from "react";
import { ArrowDown } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { cn } from "@/lib/utils";
import {
  useApplySuperAdminInvoiceCreditMutation,
  useApplySuperAdminInvoiceRefundMutation,
  useGetSuperAdminBillingInvoicesQuery,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import RefundDisputeModal, {
  type RefundDisputeModalData,
} from "./RefundDisputeModal";
import InvoiceActionsMenu from "./InvoiceActionsMenu";

interface InvoiceRow {
  id: string;
  invoiceNumericId?: number;
  organisation: string;
  invoiceId: string;
  amount: string;
  currentBalance: string;
  dueDate: string;
  status: string;
}

interface PastDueInvoicesTableProps {
  onViewAllInvoices?(): void;
}

function getHeaderItems(): string[] {
  return ["Organization", "Invoice ID", "Amount", "Due Date", "Status", "Actions"];
}

function getStatusClassName(status: string): string {
  const normalized = status.toLowerCase();
  if (normalized.includes("retry")) {
    return "bg-[#f4f8fb] text-[#8da1b0]";
  }

  return "bg-[#fff1f1] text-[#ff5f57]";
}

function StatusPill(props: { status: string }) {
  return (
    <span
      className={cn(
        "inline-flex h-5 items-center rounded-full px-2",
        "text-[0.625rem] font-medium leading-4",
        getStatusClassName(props.status)
      )}
    >
      {props.status}
    </span>
  );
}

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

function formatCurrency(amount: number): string {
  return currencyFormatter.format(Number.isFinite(amount) ? amount : 0);
}

function PastDueInvoicesTable(props: PastDueInvoicesTableProps) {
  const navigate = useNavigate();
  const ignoreRowClickRef = useRef(false);
  const [selectedInvoice, setSelectedInvoice] =
    useState<RefundDisputeModalData | null>(null);
  const [adjustmentMode, setAdjustmentMode] = useState<"refund" | "credit">(
    "refund"
  );
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const {
    data: invoicesResult,
    isLoading,
    isError,
    error,
  } = useGetSuperAdminBillingInvoicesQuery({
    status: "past_due",
    sort: "duedate_asc",
    page: 0,
    pageSize: 3,
  });

  const [applySuperAdminInvoiceRefund, { isLoading: isApplyingRefund }] =
    useApplySuperAdminInvoiceRefundMutation();
  const [applySuperAdminInvoiceCredit, { isLoading: isApplyingCredit }] =
    useApplySuperAdminInvoiceCreditMutation();

  const rows: InvoiceRow[] = useMemo(
    () =>
      (invoicesResult?.items || []).map((row) => ({
        id: row.id,
        invoiceNumericId: row.invoiceNumericId,
        organisation: row.organisation,
        invoiceId: row.invoiceId,
        amount: formatCurrency(row.amount),
        currentBalance: formatCurrency(row.currentBalance),
        dueDate: row.dueDate,
        status: row.status,
      })),
    [invoicesResult]
  );

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
      setToastMessage("Unable to apply adjustment: invalid invoice ID.");
      return;
    }

    const amountUsd = Number.parseFloat(refundAmount);
    if (!Number.isFinite(amountUsd) || amountUsd <= 0) {
      setToastType("error");
      setToastMessage("Please enter a valid amount.");
      return;
    }

    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setToastType("error");
      setToastMessage("Please enter a reason.");
      return;
    }

    try {
      if (adjustmentMode === "credit") {
        await applySuperAdminInvoiceCredit({
          invoiceId: selectedInvoice.invoiceNumericId,
          body: { amountUsd, reason: trimmedReason },
        }).unwrap();
      } else {
        await applySuperAdminInvoiceRefund({
          invoiceId: selectedInvoice.invoiceNumericId,
          body: { amountUsd, reason: trimmedReason },
        }).unwrap();
      }

      setSelectedInvoice(null);
      setToastType("success");
      setToastMessage(
        adjustmentMode === "credit"
          ? "Credit applied successfully."
          : "Refund applied successfully."
      );
    } catch (adjustmentError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(adjustmentError));
    }
  }

  return (
    <div className="w-full">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="rounded-[1rem] border border-[#e6edf3] bg-white p-4 shadow-[0_2px_2px_0_var(--shadow)]">
        <div className="flex items-center justify-between gap-4 px-2 pb-4">
          <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
            Past Due Invoices
          </div>
          <Button
            type="button"
            variant="tertiary"
            size="sm"
            onClick={props.onViewAllInvoices}
          >
            View All Invoices
          </Button>
        </div>

        <div className="overflow-hidden rounded-[0.875rem] border border-[#e9eef5]">
          <div className="grid grid-cols-[1.25fr_1fr_0.9fr_0.9fr_0.85fr_0.5fr] items-center bg-[#f5f8fb] px-3 py-3.5">
            {getHeaderItems().map(function (header, index) {
              return (
                <div
                  key={header}
                  className={cn(
                    "text-[0.875rem] font-medium leading-5 text-[#1f2d38]",
                    index === getHeaderItems().length - 1 ? "text-center" : ""
                  )}
                >
                  {index === 0 || index === 1 ? (
                    <div className="flex items-center gap-1.5">
                      <span>{header}</span>
                      <ArrowDown size={13} aria-hidden="true" />
                    </div>
                  ) : (
                    header
                  )}
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
          ) : rows.length === 0 ? (
            <div className="px-4 py-8 text-center text-sm font-medium text-[#667483]">
              No past due invoices found.
            </div>
          ) : (
            rows.map(function (row, index) {
              const rowBorderClassName = index === 0 ? "" : "border-t border-[#edf2f7]";

              return (
                <div
                  key={row.id + "-" + index}
                  className={cn(
                    "grid grid-cols-[1.25fr_1fr_0.9fr_0.9fr_0.85fr_0.5fr] items-center bg-white px-3 py-5.5 transition-colors hover:bg-[#fafcfe]",
                    rowBorderClassName
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
                  <div className="text-[0.875rem] font-normal leading-6 text-[#3b4856]">
                    {row.organisation}
                  </div>
                  <div className="text-[0.875rem] font-normal leading-6 text-[#556372]">
                    {row.invoiceId}
                  </div>
                  <div className="text-[0.875rem] font-normal leading-6 text-[#3b4856]">
                    {row.amount}
                  </div>
                  <div className="text-[0.875rem] font-normal leading-6 text-[#556372]">
                    {row.dueDate}
                  </div>
                  <div>
                    <StatusPill status={row.status} />
                  </div>
                  <div
                    className="flex items-center justify-center"
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

export default PastDueInvoicesTable;
