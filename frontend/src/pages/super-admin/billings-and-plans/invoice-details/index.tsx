import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";
import SuperAdminHeaderActions from "@/components/shared/SuperAdminHeaderActions";
import {
  useGetSuperAdminBillingInvoiceByIdQuery,
  useLazyGetSuperAdminBillingInvoicePdfQuery,
  useSendSuperAdminInvoiceReminderMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import InvoiceHeader from "./components/InvoiceHeader";
import InvoiceSummaryCard from "./components/InvoiceSummaryCard";
import CustomerDetailsCard from "./components/CustomerDetailsCard";
import LineItemsCard from "./components/LineItemsCard";
import PaymentActivityCard from "./components/PaymentActivityCard";
import AdjustmentsCard from "./components/AdjustmentsCard";
import DisputesCard from "./components/DisputesCard";
import SendReminderModal from "./components/SendReminderModal";
import {
  canSendInvoiceReminder,
  INVOICE_DETAILS_MOCK,
  mapInvoiceDetailsFromDetail,
} from "./invoiceDetails.data";

function getErrorStatus(error: unknown): number | null {
  if (!error || typeof error !== "object") return null;
  const status = (error as FetchBaseQueryError).status;
  return typeof status === "number" ? status : null;
}

function InvoiceBreadcrumb(props: { invoiceId: string }) {
  const navigate = useNavigate();

  return (
    <div className="flex min-w-0 flex-1 items-center gap-1.5 overflow-hidden text-[0.8125rem] font-medium leading-5 text-[#8a96a3]">
      <button
        type="button"
        onClick={() => navigate("/super-admin/dashboard")}
        className="shrink-0 cursor-pointer transition-colors hover:text-[#1f2d38] hover:underline"
      >
        Super Admin
      </button>
      <span className="shrink-0">/</span>
      <button
        type="button"
        onClick={() =>
          navigate("/super-admin/billings-and-plans", {
            state: { initialTab: "invoices" },
          })
        }
        className="shrink-0 cursor-pointer transition-colors hover:text-[#1f2d38] hover:underline"
      >
        Billing &amp; Plans
      </button>
      <span className="shrink-0">/</span>
      <span
        className="min-w-0 truncate text-[#1f2d38]"
        title={`Invoice ${props.invoiceId}`}
      >
        Invoice {props.invoiceId}
      </span>
    </div>
  );
}

function InvoiceDetails() {
  const navigate = useNavigate();
  const { invoiceId } = useParams();
  const [reminderOpen, setReminderOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastTone, setToastTone] = useState<"error" | "success">("error");

  const parsedInvoiceId = useMemo(() => {
    const value = Number.parseInt(invoiceId ?? "", 10);
    return Number.isFinite(value) && value > 0 ? value : null;
  }, [invoiceId]);

  const { data, isLoading, isError, error } = useGetSuperAdminBillingInvoiceByIdQuery(
    parsedInvoiceId ?? 0,
    { skip: parsedInvoiceId === null },
  );

  const [fetchInvoicePdf, { isFetching: isDownloadingPdf }] =
    useLazyGetSuperAdminBillingInvoicePdfQuery();
  const [sendReminder, { isLoading: isSendingReminder }] =
    useSendSuperAdminInvoiceReminderMutation();

  const invoice = useMemo(() => {
    if (data) return mapInvoiceDetailsFromDetail(data);
    if (invoiceId) {
      return {
        ...INVOICE_DETAILS_MOCK,
        invoiceId,
        invoiceNumericId: parsedInvoiceId,
      };
    }
    return INVOICE_DETAILS_MOCK;
  }, [data, invoiceId, parsedInvoiceId]);

  const canRemind = canSendInvoiceReminder(invoice.rawStatus || data?.status);

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 3500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  function handleOpenReminder() {
    if (!canRemind) return;
    setReminderOpen(true);
  }

  function handleCloseReminder() {
    if (isSendingReminder) return;
    setReminderOpen(false);
  }

  async function handleSendReminder() {
    if (parsedInvoiceId === null) {
      setToastTone("error");
      setToastMessage("Invoice id is missing.");
      return;
    }

    try {
      const result = await sendReminder(parsedInvoiceId).unwrap();
      setReminderOpen(false);
      setToastTone("success");
      const successMessage = `Reminder sent to ${result.emailsSent} email(s), ${result.inAppNotificationsCreated} in-app notification(s)`;
      setToastMessage(
        result.warning ? `${successMessage}. ${result.warning}` : successMessage,
      );
    } catch (reminderError) {
      const status = getErrorStatus(reminderError);
      setToastTone("error");
      if (status === 429) {
        setToastMessage("Already reminded recently — try again later");
        return;
      }
      if (status === 409) {
        setToastMessage("Reminders are not available for paid or void invoices.");
        setReminderOpen(false);
        return;
      }
      setToastMessage(getApiErrorMessage(reminderError));
    }
  }

  async function handleDownloadInvoice() {
    if (parsedInvoiceId === null) {
      setToastTone("error");
      setToastMessage("Invoice id is missing.");
      return;
    }

    try {
      const blob = await fetchInvoicePdf(parsedInvoiceId).unwrap();
      if (!(blob instanceof Blob)) {
        throw new Error("PDF download failed.");
      }

      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `subscription-invoice-${parsedInvoiceId}.pdf`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
      setToastTone("success");
      setToastMessage("Invoice PDF downloaded.");
    } catch (downloadError) {
      setToastTone("error");
      setToastMessage(getApiErrorMessage(downloadError));
    }
  }

  return (
    <div className="flex h-full min-h-0 w-full flex-col bg-[#FAFAFB]">
      {toastMessage ? (
        <div
          className={
            toastTone === "success"
              ? "fixed right-6 top-6 z-[70] rounded-[0.75rem] border border-[#d1fae5] bg-[#ecfdf5] px-4 py-3 text-sm font-medium text-[#047857] shadow-[0_12px_24px_rgba(15,23,42,0.10)]"
              : "fixed right-6 top-6 z-[70] rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm font-medium text-[#b42318] shadow-[0_12px_24px_rgba(15,23,42,0.10)]"
          }
        >
          {toastMessage}
        </div>
      ) : null}

      <div className="sticky top-0 z-20 flex shrink-0 flex-col gap-5 bg-[#FAFAFB] pb-4 pt-1">
        <div className="flex items-start justify-between gap-4">
          <InvoiceBreadcrumb invoiceId={invoice.invoiceId} />
          <SuperAdminHeaderActions />
        </div>

        <InvoiceHeader
          invoice={invoice}
          onSendReminder={handleOpenReminder}
          canSendReminder={canRemind}
          onDownload={() => void handleDownloadInvoice()}
          isDownloadingPdf={isDownloadingPdf}
          onBack={() =>
            navigate("/super-admin/billings-and-plans", {
              state: { initialTab: "invoices" },
            })
          }
        />
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto pb-6">
        <div className="flex flex-col gap-5">
          {parsedInvoiceId === null ? (
            <div className="rounded-[1rem] border border-[#f3d4d4] bg-[#fff5f5] px-6 py-8 text-sm font-medium text-[#b42318] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
              Invalid invoice id.
            </div>
          ) : isLoading ? (
            <div className="rounded-[1rem] border border-[#e8edf2] bg-white px-6 py-8 text-sm font-medium text-[#667483] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
              Loading invoice...
            </div>
          ) : isError ? (
            <div className="rounded-[1rem] border border-[#f3d4d4] bg-[#fff5f5] px-6 py-8 text-sm font-medium text-[#b42318] shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
              {getApiErrorMessage(error)}
            </div>
          ) : (
            <>
              <div className="grid w-full grid-cols-1 gap-5 lg:grid-cols-[1fr_24.75rem]">
                <InvoiceSummaryCard invoice={invoice} />
                <CustomerDetailsCard
                  invoice={invoice}
                  onDownloadPdf={() => void handleDownloadInvoice()}
                  isDownloadingPdf={isDownloadingPdf}
                />
              </div>

              <LineItemsCard invoice={invoice} />
              <AdjustmentsCard items={invoice.adjustments} />
              <DisputesCard items={invoice.disputes} />
              <PaymentActivityCard items={invoice.activity} />
            </>
          )}
        </div>
      </div>

      <SendReminderModal
        open={reminderOpen}
        invoice={invoice}
        isSending={isSendingReminder}
        onClose={handleCloseReminder}
        onSend={() => void handleSendReminder()}
      />
    </div>
  );
}

export default InvoiceDetails;
