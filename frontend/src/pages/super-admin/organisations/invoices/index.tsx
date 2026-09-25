import { ArrowLeft, ArrowDown } from "lucide-react";
import { useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
  useApplySuperAdminInvoiceCreditMutation,
  useApplySuperAdminInvoiceRefundMutation,
  useGetOrganisationInvoicesQuery,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import InvoiceActionsMenu from "@/pages/super-admin/billings-and-plans/components/InvoiceActionsMenu";
import RefundDisputeModal, {
  type RefundDisputeModalData,
} from "@/pages/super-admin/billings-and-plans/components/RefundDisputeModal";
import StatusBadge from "../components/StatusBadge";

function getInvoiceHeaders(): string[] {
  return ["Invoice ID", "Period", "Due Date", "Amount", "Status", "Paid At", "Action"];
}

function InvoicesListScreen() {
  const navigate = useNavigate();
  const { slug } = useParams();
  const location = useLocation();
  const ignoreRowClickRef = useRef(false);
  const organisationId =
    (location.state as { organisationId?: number | null } | null)?.organisationId ?? null;
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [selectedInvoice, setSelectedInvoice] =
    useState<RefundDisputeModalData | null>(null);
  const [adjustmentMode, setAdjustmentMode] = useState<"refund" | "credit">("refund");
  const { data: invoiceRows = [], isLoading, isError, error } = useGetOrganisationInvoicesQuery(
    {
      id: organisationId ?? 0,
      page: 0,
      pageSize: 50,
    },
    { skip: !organisationId }
  );
  const [applySuperAdminInvoiceRefund, { isLoading: isApplyingRefund }] =
    useApplySuperAdminInvoiceRefundMutation();
  const [applySuperAdminInvoiceCredit, { isLoading: isApplyingCredit }] =
    useApplySuperAdminInvoiceCreditMutation();
  const headers = getInvoiceHeaders();

  function suppressNextRowClick() {
    ignoreRowClickRef.current = true;
    window.setTimeout(() => {
      ignoreRowClickRef.current = false;
    }, 0);
  }

  function showToast(type: "success" | "error", message: string) {
    setToastType(type);
    setToastMessage(message);
  }

  function handleViewInvoice(invoiceId: string) {
    navigate("/super-admin/billings-and-plans/invoices/" + invoiceId);
  }

  function handleApplyCredit(invoiceId: string, invoiceNumericId: number | undefined, currentBalance: number) {
    setAdjustmentMode("credit");
    setSelectedInvoice({
      invoiceId,
      currentBalance: `$${currentBalance.toFixed(2)}`,
      invoiceNumericId,
    });
  }

  function handleProcessRefund(invoiceId: string, invoiceNumericId: number | undefined, currentBalance: number) {
    setAdjustmentMode("refund");
    setSelectedInvoice({
      invoiceId,
      currentBalance: `$${currentBalance.toFixed(2)}`,
      invoiceNumericId,
    });
  }

  async function handleSubmitRefund(refundAmount: string, reason: string) {
    if (!selectedInvoice?.invoiceNumericId) {
      showToast("error", "Unable to apply adjustment: invalid invoice ID.");
      return;
    }
    const amountUsd = Number.parseFloat(refundAmount);
    if (!Number.isFinite(amountUsd) || amountUsd <= 0) {
      showToast("error", "Please enter a valid amount.");
      return;
    }
    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      showToast("error", "Please enter a reason.");
      return;
    }

    try {
      if (adjustmentMode === "credit") {
        await applySuperAdminInvoiceCredit({
          invoiceId: selectedInvoice.invoiceNumericId,
          body: { amountUsd, reason: trimmedReason },
        }).unwrap();
        showToast("success", "Credit applied successfully.");
      } else {
        await applySuperAdminInvoiceRefund({
          invoiceId: selectedInvoice.invoiceNumericId,
          body: { amountUsd, reason: trimmedReason },
        }).unwrap();
        showToast("success", "Refund applied successfully.");
      }
      setSelectedInvoice(null);
    } catch (adjustmentError) {
      showToast("error", getApiErrorMessage(adjustmentError));
    }
  }

  return (
    <div className="flex min-h-full w-full flex-col gap-4 bg-[#FAFAFB] pb-6">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="flex items-center justify-between gap-4">
        <Button
          type="button"
          variant="secondary"
          size="sm"
          onClick={function () {
            navigate("/super-admin/organisations/" + slug, {
              state: organisationId !== null ? { organisationId, initialTab: "billing" } : undefined,
            });
          }}
        >
          <ArrowLeft size={15} aria-hidden="true" />
          Back
        </Button>
      </div>

      <div className="w-full rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) px-4 py-4 shadow-[0_1px_2px_0px_var(--shadow)] md:px-5">
        <div className="text-(--text-gray-900) text-[1rem] font-semibold leading-6">
          Invoices &amp; Payment History
        </div>

        <div className="mt-4 overflow-hidden rounded-[0.875rem] border border-(--neutral-100)">
          <div className="grid grid-cols-[1.15fr_1fr_1fr_1.1fr_1fr_1.45fr_4.5rem] items-center bg-(--bg-primary-50) px-4 py-4">
            {headers.map(function (header, index) {
              return (
                <div key={header + "-" + index} className="text-(--text-primary-dark) text-[0.8125rem] font-medium leading-4.5">
                  {index === 0 ? (
                    <div className="flex items-center gap-1.5">
                      <span>{header}</span>
                      <ArrowDown size={13} strokeWidth={1.8} aria-hidden="true" />
                    </div>
                  ) : (
                    header
                  )}
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
          ) : invoiceRows.length === 0 ? (
            <div className="px-4 py-8 text-center text-sm font-medium text-[#667483]">No invoices found.</div>
          ) : (
            invoiceRows.map(function (row, index) {
              return (
                <div
                  key={row.id + "-" + index}
                  className="grid grid-cols-[1.15fr_1fr_1fr_1.1fr_1fr_1.45fr_4.5rem] items-center border-t border-(--neutral-100) px-4 py-4 transition-colors hover:bg-[#fafcfe]"
                  role="button"
                  tabIndex={0}
                  onClick={() => {
                    if (ignoreRowClickRef.current) {
                      ignoreRowClickRef.current = false;
                      return;
                    }
                    handleViewInvoice(row.invoiceId);
                  }}
                >
                  <div className="text-(--text-neutral-600) text-sm font-normal leading-5.5">{row.invoiceId}</div>
                  <div className="text-(--text-neutral-600) text-sm font-normal leading-5.5">{row.period}</div>
                  <div className="text-(--text-neutral-600) text-sm font-normal leading-5.5">{row.dueDate}</div>
                  <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                    ${row.amount.toFixed(2)}
                  </div>
                  <div>
                    <StatusBadge variant="green">{row.status}</StatusBadge>
                  </div>
                  <div className="text-(--text-neutral-600) text-sm font-normal leading-5.5">{row.paidAt}</div>
                  <div className="flex justify-center" onClick={(event) => event.stopPropagation()}>
                    <InvoiceActionsMenu
                      onActionStart={suppressNextRowClick}
                      onViewInvoice={() => handleViewInvoice(row.invoiceId)}
                      onApplyCredit={() =>
                        handleApplyCredit(row.invoiceId, row.invoiceNumericId, row.currentBalance)
                      }
                      onProcessRefund={() =>
                        handleProcessRefund(row.invoiceId, row.invoiceNumericId, row.currentBalance)
                      }
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
        onClose={() => setSelectedInvoice(null)}
        isSubmitting={isApplyingRefund || isApplyingCredit}
        onSubmit={handleSubmitRefund}
      />
    </div>
  );
}

export default InvoicesListScreen;
