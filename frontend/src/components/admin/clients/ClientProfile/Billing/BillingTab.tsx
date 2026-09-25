import { useScopedPage } from "@/hooks/useScopedPage";
import { useState, useCallback, useEffect, useMemo, useRef } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import BillingTable from "@/components/billing-sections/BillingTable";
import RecordPaymentModal from "@/components/billing-sections/RecordPaymentModal";
import ApplyDiscountModal from "@/components/billing-sections/ApplyDiscountModal";
import PreviewInvoiceModal from "@/components/billing-sections/PreviewInvoiceModal";
import ChangeStatusModal from "@/components/billing-sections/ChangeStatusModal";
import BillingTransactionsModal from "@/components/billing-sections/BillingTransactionsModal";
import BillingModuleGate from "@/components/billing-sections/BillingModuleGate";
import { BILLING_STATUS_OPTIONS } from "@/pages/therapist/therapist.static";
import { getApiErrorMessage } from "@/utils/apiError";
import { isBillingModuleForbidden, isStalePaymentStateError } from "@/utils/billingErrors";
import { useAccumulatedBillingRecords } from "@/utils/billingHistory";
import type { Invoice as CommonInvoice } from "@/types/invoice.type";
import type { Client } from "@/types/client.type";
import { useClientOverviewLabels } from "@/hooks/useSystemOptionCatalog";

import {
  useGetBillingRecordsQuery,
  useGetClientBillingStatsQuery,
  useRecordPaymentMutation,
  useApplyDiscountMutation,
  useChangeBillingStatusMutation,
  useSendInvoiceEmailMutation,
  useLazyDownloadInvoiceQuery,
  useLazyPreviewInvoiceQuery,
} from "@/store/api/admin/billing.api";
import { openInvoiceHtmlForPrint } from "@/utils/openInvoiceForPrint";
import { resolveInvoiceHtml } from "@/utils/resolveInvoiceHtml";

const PAGE_SIZE = 10;

function formatBillingMoney(value: number | null | undefined): string {
  const amount = Number(value ?? 0);
  if (!Number.isFinite(amount)) return "$0.00";
  return `$${amount.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

interface BillingTabProps {
  scrollRef: React.RefObject<HTMLDivElement | null>;
  client: Client;
  isActive?: boolean;
  readOnly?: boolean;
  /** Same as /billings: enables Change Status + therapist column when true. */
  isAdmin?: boolean;
}

const BillingTab = ({
  client,
  isActive = false,
  readOnly = false,
  isAdmin = false,
}: BillingTabProps) => {
  const billingScrollRef = useRef<HTMLDivElement>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("All Statuses");
  const billingScope = JSON.stringify({ clientId: client.id, statusFilter });
  const [page, setPage] = useScopedPage(billingScope);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const labels = useClientOverviewLabels({
    insuranceProvider: client.insuranceProvider || client.insurance,
    insuranceType: client.insuranceType,
  });
  const insuranceLabel =
    (labels.insuranceProvider && labels.insuranceProvider !== "—"
      ? labels.insuranceProvider
      : null) ||
    (labels.insuranceType && labels.insuranceType !== "—"
      ? labels.insuranceType
      : null) ||
    client.insurance ||
    "N/A";

  const [isRecordModalOpen, setIsRecordModalOpen] = useState(false);
  const [isDiscountModalOpen, setIsDiscountModalOpen] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [isChangeStatusModalOpen, setIsChangeStatusModalOpen] = useState(false);
  const [isTransactionsModalOpen, setIsTransactionsModalOpen] = useState(false);
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);
  const [statusToChange, setStatusToChange] = useState("pending");

  const [selectedInvoice, setSelectedInvoice] = useState<CommonInvoice | null>(null);
  const [selectedInvoiceForDiscount, setSelectedInvoiceForDiscount] =
    useState<CommonInvoice | null>(null);

  const payloadArgs = useMemo(
    () => ({
      clientId: Number(client.id),
      status: statusFilter !== "All Statuses" ? statusFilter : undefined,
      page: page - 1,
      size: PAGE_SIZE,
    }),
    [client.id, statusFilter, page],
  );

  const {
    currentData: recordsResponse,
    isFetching,
    isLoading: isLoadingHistory,
    isUninitialized,
    error: historyError,
    refetch: refetchBillingRecords,
  } = useGetBillingRecordsQuery(payloadArgs, {
    skip: !client.id || !isActive,
    refetchOnMountOrArgChange: true,
  });

  const { data: clientBillingStats } = useGetClientBillingStatsQuery(Number(client.id), {
    skip: !client.id || !isActive,
    refetchOnMountOrArgChange: true,
  });

  const billingTotalsSummary = useMemo(() => {
    if (!clientBillingStats) return null;
    const total = Number(clientBillingStats.totalBilledAmount ?? 0);
    const discount = Number(clientBillingStats.totalDiscountAmount ?? 0);
    const paid = Number(clientBillingStats.totalPaidAmount ?? 0);
    const remaining = Number(clientBillingStats.dueAmount ?? 0);
    return `Total ${formatBillingMoney(total)} − Discount ${formatBillingMoney(discount)} − Paid ${formatBillingMoney(paid)} · Remaining ${formatBillingMoney(remaining)}`;
  }, [clientBillingStats]);

  const moduleForbidden = useMemo(
    () => isBillingModuleForbidden(historyError),
    [historyError],
  );

  const { displayedInvoices, clearInvoices, isReadyToLoadMore } =
    useAccumulatedBillingRecords(recordsResponse, page, billingScope);

  const [recordPayment, { isLoading: isRecordingPayment }] = useRecordPaymentMutation();
  const [applyDiscount, { isLoading: isApplyingDiscount }] = useApplyDiscountMutation();
  const [changeBillingStatus, { isLoading: isChangingStatus }] =
    useChangeBillingStatusMutation();
  const [sendInvoiceEmail] = useSendInvoiceEmailMutation();
  const [triggerDownloadInvoice] = useLazyDownloadInvoiceQuery();
  const [triggerPreview, { isFetching: isPreviewing }] = useLazyPreviewInvoiceQuery();

  useEffect(() => {
    if (historyError && !isBillingModuleForbidden(historyError)) {
      setToastMessage(getApiErrorMessage(historyError));
    }
  }, [historyError]);

  useEffect(() => {
    if (!isActive || !client.id) return;
    setPage(1);
    clearInvoices();
    void refetchBillingRecords();
  }, [isActive, client.id, refetchBillingRecords, clearInvoices, setPage]);

  useEffect(() => {
    if (!toastMessage) return;
    const lower = toastMessage.toLowerCase();
    if (
      lower.includes("sending") ||
      lower.includes("preparing") ||
      lower.includes("started")
    ) {
      setToastType("info");
    } else {
      setToastType(lower.includes("success") ? "success" : "error");
    }
    const timer = window.setTimeout(() => setToastMessage(null), 3000);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const totalRecords = recordsResponse?.totalElements ?? 0;

  const handleLoadMore = useCallback(() => {
    if (!isReadyToLoadMore || isFetching) return;
    if (page * PAGE_SIZE < totalRecords) {
      setPage((prev) => prev + 1);
    }
  }, [totalRecords, isFetching, page, isReadyToLoadMore, setPage]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: page * PAGE_SIZE < totalRecords,
    isLoading: isFetching,
    scrollRootRef: billingScrollRef,
    rootMargin: "200px",
  });

  const currentInvoices = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return displayedInvoices;
    return displayedInvoices.filter((inv) => {
      const service = (inv.service || "").toLowerCase();
      const clientName = (inv.client || "").toLowerCase();
      return service.includes(query) || clientName.includes(query);
    });
  }, [displayedInvoices, searchQuery]);

  const isInitialBillingLoad =
    currentInvoices.length === 0 &&
    (isUninitialized || isLoadingHistory || isFetching);

  const handleInvoiceAction = async (action: string, invoice: CommonInvoice) => {
    try {
      if (action === "record_payment" || action === "pay_now") {
        setSelectedInvoice(invoice);
        setIsRecordModalOpen(true);
      } else if (action === "email_invoice" || action === "send_invoice") {
        setToastMessage("Sending invoice...");
        await sendInvoiceEmail(invoice.id).unwrap();
        setToastMessage("Invoice sent successfully!");
      } else if (action === "download_invoice" || action === "Download") {
        // Open synchronously so the browser still treats this as a user gesture.
        const printWindow = window.open("about:blank", "_blank");
        setToastMessage("Preparing invoice...");
        try {
          const html = resolveInvoiceHtml(
            await triggerDownloadInvoice(invoice.id).unwrap(),
          );
          openInvoiceHtmlForPrint(html, printWindow);
          setToastMessage("Invoice opened for printing");
        } catch (error) {
          printWindow?.close();
          throw error;
        }
      } else if (action === "preview" || action === "preview_invoice") {
        setSelectedInvoice(invoice);
        setPreviewHtml(null);
        setIsPreviewModalOpen(true);
        const previewPayload = await triggerPreview(invoice.id).unwrap();
        setPreviewHtml(resolveInvoiceHtml(previewPayload));
      } else if (action === "view_transactions") {
        setSelectedInvoice(invoice);
        setIsTransactionsModalOpen(true);
      } else if (action === "apply_discount") {
        setSelectedInvoiceForDiscount(invoice);
        setIsDiscountModalOpen(true);
      } else if (action.startsWith("mark_as_")) {
        const statusMap: Record<string, string> = {
          mark_as_billed: "billed",
          mark_as_paid: "paid",
          mark_as_denied: "denied",
          mark_as_followup: "follow_up",
          mark_as_pending: "pending",
        };
        const targetStatus = statusMap[action] ?? action.replace("mark_as_", "");
        setSelectedInvoice(invoice);
        setStatusToChange(targetStatus);
        setIsChangeStatusModalOpen(true);
      }
    } catch (err) {
      setToastMessage(getApiErrorMessage(err));
    }
  };

  if (moduleForbidden) {
    return <BillingModuleGate />;
  }

  return (
    <>
      <ScrollToTopButton containerRef={billingScrollRef} centered={false} />

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <div className="flex h-full min-h-0 flex-col gap-6 overflow-hidden p-6 animate-in fade-in duration-300">
        <div className="flex shrink-0 items-end justify-between gap-4">
          <div>
            <h2 className="text-base font-semibold text-gray-900">
              Invoice History
            </h2>
            <p className="text-sm text-gray-500">
              Insurance: {insuranceLabel}
            </p>
            {billingTotalsSummary ? (
              <p
                className="mt-1 text-xs text-gray-500"
                title={billingTotalsSummary}
              >
                {billingTotalsSummary}
              </p>
            ) : null}
          </div>

          <div className="flex items-center gap-3">
            <div className="relative w-[17.5rem]">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Search..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 h-10 rounded-full border-gray-200 bg-white"
              />
            </div>
            <div className="w-[9.375rem]">
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="h-10 rounded-full border-gray-200 bg-white cursor-pointer">
                  <SelectValue placeholder="All Statuses" />
                </SelectTrigger>
                <SelectContent className="rounded-xl cursor-pointer">
                  <SelectItem value="All Statuses">All Statuses</SelectItem>
                  {BILLING_STATUS_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label.split(" -")[0]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>

        <div className="min-h-0 w-full flex-1 overflow-hidden rounded-3xl border border-gray-200 bg-white shadow-sm">
          <BillingTable
            data={currentInvoices}
            isLoading={isInitialBillingLoad}
            isLoadingMore={!isInitialBillingLoad && isFetching && page > 1}
            loadMoreRef={observerTarget}
            scrollRootRef={billingScrollRef}
            onRecordPayment={(invoice) => {
              void handleInvoiceAction("record_payment", invoice);
            }}
            onApplyDiscount={(invoice) => {
              void handleInvoiceAction("apply_discount", invoice);
            }}
            onAction={(action, invoice) => {
              void handleInvoiceAction(action, invoice);
            }}
            isAdmin={isAdmin}
            isTherapist={!isAdmin}
            allowBillingMutations={!readOnly}
          />
        </div>
      </div>

      <RecordPaymentModal
        key={selectedInvoice?.id}
        isOpen={isRecordModalOpen}
        onClose={() => setIsRecordModalOpen(false)}
        invoice={selectedInvoice}
        isLoading={isRecordingPayment}
        onRecord={async (data) => {
          if (!selectedInvoice) return;
          try {
            await recordPayment({
              billingId: selectedInvoice.id,
              paymentAmount: Number(data.paymentAmount),
              paymentMethod: data.paymentMethod,
              paymentSide: data.paymentSide,
              paymentDate: data.paymentDate,
              expectedPreviousForSource: data.expectedPreviousForSource,
              referenceNumber: data.referenceNumber,
              notes: data.notes,
            }).unwrap();
            setToastMessage("Payment recorded successfully");
            setIsRecordModalOpen(false);
            setPage(1);
            clearInvoices();
          } catch (error) {
            if (isStalePaymentStateError(error)) {
              setToastMessage(
                "Payment totals changed. Reload latest totals in the dialog and try again.",
              );
              return;
            }
            setToastMessage(getApiErrorMessage(error));
          }
        }}
      />

      <ApplyDiscountModal
        key={`${selectedInvoiceForDiscount?.id}-${isDiscountModalOpen}`}
        isOpen={isDiscountModalOpen}
        onClose={() => setIsDiscountModalOpen(false)}
        invoice={selectedInvoiceForDiscount}
        isLoading={isApplyingDiscount}
        onApply={async (data) => {
          if (!selectedInvoiceForDiscount) return;
          try {
            await applyDiscount({
              billingId: selectedInvoiceForDiscount.id,
              discountType: data.discountType,
              discountValue: data.discountValue
                ? Number(data.discountValue)
                : undefined,
            }).unwrap();
            setToastMessage("Discount applied successfully");
            setIsDiscountModalOpen(false);
            setPage(1);
            clearInvoices();
          } catch (error) {
            setToastMessage(getApiErrorMessage(error));
          }
        }}
      />

      <PreviewInvoiceModal
        isOpen={isPreviewModalOpen}
        onClose={() => {
          setIsPreviewModalOpen(false);
          setPreviewHtml(null);
        }}
        invoice={selectedInvoice}
        htmlContent={previewHtml}
        isLoading={isPreviewing}
      />

      <BillingTransactionsModal
        isOpen={isTransactionsModalOpen}
        onClose={() => setIsTransactionsModalOpen(false)}
        invoice={selectedInvoice}
      />

      <ChangeStatusModal
        key={`status-${selectedInvoice?.id}-${isChangeStatusModalOpen}`}
        isOpen={isChangeStatusModalOpen}
        onClose={() => setIsChangeStatusModalOpen(false)}
        invoice={selectedInvoice}
        initialStatus={statusToChange}
        isLoading={isChangingStatus}
        onApply={async (data) => {
          if (!selectedInvoice) return;
          try {
            await changeBillingStatus({
              billingId: selectedInvoice.id,
              billingStatus: data.billingStatus,
              notes: data.notes || undefined,
            }).unwrap();
            setToastMessage("Status changed successfully");
            setIsChangeStatusModalOpen(false);
            setPage(1);
            clearInvoices();
          } catch (error) {
            setToastMessage(getApiErrorMessage(error));
          }
        }}
      />
    </>
  );
};

export default BillingTab;
