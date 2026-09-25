import { ArrowDown } from "lucide-react";
import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import {
  useApplySuperAdminInvoiceCreditMutation,
  useApplySuperAdminInvoiceRefundMutation,
  useGetOrganisationInvoicesQuery,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import InvoiceActionsMenu from "@/pages/super-admin/billings-and-plans/components/InvoiceActionsMenu";
import RefundDisputeModal, {
  type RefundDisputeModalData,
} from "@/pages/super-admin/billings-and-plans/components/RefundDisputeModal";
import {
  parseCurrencyDisplay,
  validateInvoiceAdjustment,
} from "@/pages/super-admin/billings-and-plans/invoiceAdjustment.utils";
import StatusBadge from "../../../components/StatusBadge";

interface InvoiceRow {
  id: string;
  invoiceNumericId?: number;
  period: string;
  dueDate: string;
  amount: string;
  currentBalance: string;
  status: string;
  paidAt: string;
}

function getInvoiceHeaders(): string[] {
  return ["Invoice ID", "Period", "Due Date", "Amount", "Status", "Paid At", "Action"];
}

function getInvoiceTableGridClassName(): string {
  return "grid min-w-[47.5rem] grid-cols-[minmax(5.5rem,1.05fr)_minmax(7.5rem,1.35fr)_minmax(5.5rem,0.95fr)_minmax(4.5rem,1fr)_minmax(4.5rem,0.9fr)_minmax(6.25rem,1.3fr)_3.5rem] items-center";
}

function renderHeaderCell(header: string, index: number) {
  if (index === 0) {
    return (
      <div className="flex items-center gap-1.5 text-(--text-primary-dark) text-[0.8125rem] font-medium leading-4.5">
        <span>{header}</span>
        <ArrowDown size={13} strokeWidth={1.8} aria-hidden="true" />
      </div>
    );
  }

  return (
    <div className="text-(--text-primary-dark) text-[0.8125rem] font-medium leading-4.5">
      {header}
    </div>
  );
}

interface InvoicesPaymentHistoryCardProps {
  organisationId: number | null;
  slug: string;
}

function InvoicesPaymentHistoryCard(props: InvoicesPaymentHistoryCardProps) {
  const navigate = useNavigate();
  const ignoreRowClickRef = useRef(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [selectedInvoice, setSelectedInvoice] =
    useState<RefundDisputeModalData | null>(null);
  const [adjustmentMode, setAdjustmentMode] = useState<"refund" | "credit">("refund");
  const { data: invoiceRows = [], isLoading, isError, error } = useGetOrganisationInvoicesQuery(
    {
      id: props.organisationId ?? 0,
      page: 0,
      pageSize: 50,
    },
    { skip: !props.organisationId }
  );
  const [applySuperAdminInvoiceRefund, { isLoading: isApplyingRefund }] =
    useApplySuperAdminInvoiceRefundMutation();
  const [applySuperAdminInvoiceCredit, { isLoading: isApplyingCredit }] =
    useApplySuperAdminInvoiceCreditMutation();
  const rows: InvoiceRow[] = invoiceRows.map((row) => ({
    id: row.invoiceId,
    invoiceNumericId: row.invoiceNumericId,
    period: row.period,
    dueDate: row.dueDate,
    amount: `$${row.amount.toFixed(2)}`,
    currentBalance: `$${row.currentBalance.toFixed(2)}`,
    status: row.status,
    paidAt: row.paidAt,
  }));
  const headers = getInvoiceHeaders();

  function showToast(type: "success" | "error", message: string) {
    setToastType(type);
    setToastMessage(message);
  }

  function suppressNextRowClick() {
    ignoreRowClickRef.current = true;
    window.setTimeout(() => {
      ignoreRowClickRef.current = false;
    }, 0);
  }

  function handleViewInvoice(invoiceId: string) {
    navigate("/super-admin/billings-and-plans/invoices/" + invoiceId);
  }

  function handleApplyCredit(row: InvoiceRow) {
    setAdjustmentMode("credit");
    setSelectedInvoice({
      invoiceId: row.id,
      currentBalance: row.currentBalance,
      invoiceNumericId: row.invoiceNumericId,
    });
  }

  function handleProcessRefund(row: InvoiceRow) {
    setAdjustmentMode("refund");
    setSelectedInvoice({
      invoiceId: row.id,
      currentBalance: row.currentBalance,
      invoiceNumericId: row.invoiceNumericId,
    });
  }

  async function handleSubmitRefund(refundAmount: string, reason: string) {
    if (!selectedInvoice?.invoiceNumericId) {
      showToast("error", "Unable to apply adjustment: invalid invoice ID.");
      return;
    }

    const amountLabel = adjustmentMode === "credit" ? "Credit amount" : "Refund amount";
    const validationError = validateInvoiceAdjustment({
      amount: refundAmount,
      reason,
      maxAmountUsd: parseCurrencyDisplay(selectedInvoice.currentBalance),
      amountLabel,
    });
    if (validationError) {
      showToast("error", validationError);
      return;
    }

    const amountUsd = Number.parseFloat(refundAmount.trim());
    const trimmedReason = reason.trim();

    try {
      if (adjustmentMode === "credit") {
        await applySuperAdminInvoiceCredit({
          invoiceId: selectedInvoice.invoiceNumericId,
          body: { amountUsd, reason: trimmedReason },
        }).unwrap();
        setSelectedInvoice(null);
        showToast("success", "Credit applied successfully.");
      } else {
        await applySuperAdminInvoiceRefund({
          invoiceId: selectedInvoice.invoiceNumericId,
          body: { amountUsd, reason: trimmedReason },
        }).unwrap();
        setSelectedInvoice(null);
        showToast("success", "Refund applied successfully.");
      }
    } catch (adjustmentError) {
      showToast("error", getApiErrorMessage(adjustmentError));
    }
  }

  return (
    <div className="w-full rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) px-4 py-4 shadow-[0_1px_2px_0px_var(--shadow)] md:px-5">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0 truncate text-(--text-gray-900) text-[1rem] font-semibold leading-6">
          Invoices &amp; Payment History
        </div>
        <button
          type="button"
          className="h-10 shrink-0 self-start rounded-full border border-(--neutral-200) px-5 text-(--text-neutral-600) text-sm font-medium leading-5 hover:bg-(--bg-primary-50) sm:self-auto"
          onClick={() => navigate("/super-admin/billings-and-plans")}
        >
          Dunning Policy
        </button>
      </div>

      <div className="mt-4 overflow-hidden rounded-[0.875rem] border border-(--neutral-100)">
        <div className="overflow-x-auto">
          <div className={getInvoiceTableGridClassName() + " bg-(--bg-primary-50) px-4 py-4"}>
            {headers.map(function (header, index) {
              return (
                <div key={header + "-" + index} className="min-w-0">
                  {renderHeaderCell(header, index)}
                </div>
              );
            })}
          </div>

        {isLoading ? (
          <div className="px-4 py-8 text-sm font-medium text-[#667483]">Loading invoices...</div>
        ) : isError ? (
          <div className="px-4 py-8 text-sm font-medium text-(--status-denied)">
            {getApiErrorMessage(error)}
          </div>
        ) : rows.length === 0 ? (
          <div className="px-4 py-8 text-center text-sm font-medium text-[#667483]">No invoices found.</div>
          ) : rows.map(function (row, index) {
          return (
            <div
              key={row.id + "-" + index}
              className={getInvoiceTableGridClassName() + " border-t border-(--neutral-100) px-4 py-4 transition-colors hover:bg-[#fafcfe]"}
              role="button"
              tabIndex={0}
              onClick={() => {
                if (ignoreRowClickRef.current) {
                  ignoreRowClickRef.current = false;
                  return;
                }
                handleViewInvoice(row.id);
              }}
            >
              <div
                className="min-w-0 truncate text-(--text-neutral-600) text-sm font-normal leading-5.5"
                title={row.id}
              >
                {row.id}
              </div>
              <div
                className="min-w-0 truncate whitespace-nowrap text-(--text-neutral-600) text-sm font-normal leading-5.5"
                title={row.period}
              >
                {row.period}
              </div>
              <div className="min-w-0 truncate text-(--text-neutral-600) text-sm font-normal leading-5.5">
                {row.dueDate}
              </div>
              <div className="min-w-0 truncate text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.amount}
              </div>
              <div className="min-w-0">
                <StatusBadge variant="green">{row.status}</StatusBadge>
              </div>
              <div className="min-w-0 truncate text-(--text-neutral-600) text-sm font-normal leading-5.5">
                {row.paidAt}
              </div>
              <div
                className="flex shrink-0 justify-center"
                onClick={(event) => event.stopPropagation()}
              >
                <InvoiceActionsMenu
                  onActionStart={suppressNextRowClick}
                  onViewInvoice={() => handleViewInvoice(row.id)}
                  onApplyCredit={() => handleApplyCredit(row)}
                  onProcessRefund={() => handleProcessRefund(row)}
                />
              </div>
            </div>
          );
        })}
        </div>
      </div>

      <div className="grid place-items-center pt-4">
        <Button
          type="button"
          variant="secondary"
          size="md"
          onClick={function () {
            navigate("/super-admin/organisations/" + props.slug + "/invoices", {
              state:
                props.organisationId !== null
                  ? { organisationId: props.organisationId }
                  : undefined,
            });
          }}
        >
          View All Invoices
        </Button>
      </div>
      <RefundDisputeModal
        open={selectedInvoice !== null}
        invoice={selectedInvoice}
        mode={adjustmentMode}
        onClose={() => setSelectedInvoice(null)}
        isSubmitting={isApplyingRefund || isApplyingCredit}
        onSubmit={handleSubmitRefund}
      />
    </div>
  );
}

export default InvoicesPaymentHistoryCard;
