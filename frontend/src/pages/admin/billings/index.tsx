import { useScopedPage } from "@/hooks/useScopedPage";
import { useState, useCallback, useEffect, useLayoutEffect, useMemo, useRef } from "react";
import { Magnifer } from "@solar-icons/react-perf/category/search/Linear/Magnifer";
import OverviewCard from "../../../components/shared/OverviewCard";
import FilterDropdown from "../../../components/billing-sections/FilterDropdown";
import ApplyDiscountModal from "../../../components/billing-sections/ApplyDiscountModal";
import type { Invoice } from "../../../types/invoice.type";
import RecordPaymentModal from "../../../components/billing-sections/RecordPaymentModal";
import CustomInput from "@/components/form/CustomInput";
import BillingTable from "@/components/billing-sections/BillingTable";
import { getIcon } from "@/utils/functions/billings";
import type { BillingFilters } from "@/types/billing.type";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useGetBillingStatisticsQuery,
  useGetBillingRecordsQuery,
  useRecordPaymentMutation,
  useSendInvoiceEmailMutation,
  useLazyDownloadInvoiceQuery,
  useLazyPreviewInvoiceQuery,
  useApplyDiscountMutation,
  useChangeBillingStatusMutation,
} from "@/store/api/admin/billing.api";
import PreviewInvoiceModal from "@/components/billing-sections/PreviewInvoiceModal";
import ChangeStatusModal from "@/components/billing-sections/ChangeStatusModal";
import BillingTransactionsModal from "@/components/billing-sections/BillingTransactionsModal";
import { openInvoiceHtmlForPrint } from "@/utils/openInvoiceForPrint";
import { resolveInvoiceHtml } from "@/utils/resolveInvoiceHtml";
import BillingModuleGate from "@/components/billing-sections/BillingModuleGate";
import { getApiErrorMessage } from "@/utils/apiError";
import { isBillingModuleForbidden, isStalePaymentStateError } from "@/utils/billingErrors";
import { useAccumulatedBillingRecords, formatTotalCollectedSubtext } from "@/utils/billingHistory";
import AppliedFiltersBar from "@/components/shared/AppliedFiltersBar";
import {
  buildBillingFilterChips,
  removeBillingFilterChip,
} from "@/utils/appliedFilterChips";
import {
  buildBillingQueryFilters,
  createCurrentMonthBillingFilters,
  createEmptyBillingFilters,
} from "@/utils/billingFilters";

const PAGE_SIZE = 10;

const AdminBillings = () => {
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [filters, setFilters] = useState<BillingFilters>(createCurrentMonthBillingFilters);

  const [searchQuery, setSearchQuery] = useState("");
  const statisticsArgs = useMemo(
    () => buildBillingQueryFilters(filters, searchQuery),
    [filters, searchQuery],
  );

  const { data: billingStats, isLoading: isLoadingStats, isError: isStatsError, error: statsError } =
    useGetBillingStatisticsQuery(statisticsArgs, { refetchOnMountOrArgChange: true });
  const [recordPayment, { isLoading: isRecordingPayment }] = useRecordPaymentMutation();
  const [applyDiscount, { isLoading: isApplyingDiscount }] = useApplyDiscountMutation();
  const [changeBillingStatus, { isLoading: isChangingStatus }] = useChangeBillingStatusMutation();
  const [sendInvoiceEmail] = useSendInvoiceEmailMutation();
  const [triggerDownloadInvoice] = useLazyDownloadInvoiceQuery();
  const [triggerPreview, { isFetching: isPreviewing }] = useLazyPreviewInvoiceQuery();

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

  useEffect(() => {
    if (isStatsError && statsError) {
      setToastMessage(getApiErrorMessage(statsError));
    }
  }, [isStatsError, statsError]);



  const [isRecordModalOpen, setIsRecordModalOpen] = useState(false);
  const [isDiscountModalOpen, setIsDiscountModalOpen] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [isChangeStatusModalOpen, setIsChangeStatusModalOpen] = useState(false);
  const [statusToChange, setStatusToChange] = useState("pending");
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);
  const [isTransactionsModalOpen, setIsTransactionsModalOpen] = useState(false);

  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [selectedInvoiceForDiscount, setSelectedInvoiceForDiscount] =
    useState<Invoice | null>(null);

  const appliedFilterChips = useMemo(
    () => buildBillingFilterChips(filters),
    [filters],
  );

  const handleRemoveFilterChip = useCallback((chipId: string) => {
    setFilters((currentFilters) => removeBillingFilterChip(currentFilters, chipId));
  }, []);

  const handleClearAllFilters = useCallback(() => {
    setFilters((currentFilters) => ({
      ...createEmptyBillingFilters(),
      clientId: currentFilters.clientId ?? null,
      therapistId: currentFilters.therapistId ?? null,
    }));
  }, []);

  const billingScope = JSON.stringify({ filters, search: searchQuery.trim() });
  const [page, setPage] = useScopedPage(billingScope);
  const scrollRootRef = useRef<HTMLDivElement>(null);

  const handleRecordPayment = (invoice: Invoice) => {
    setSelectedInvoice(invoice);
    setIsRecordModalOpen(true);
  };

  const handleApplyDiscountAction = (invoice: Invoice) => {
    setSelectedInvoiceForDiscount(invoice);
    setIsDiscountModalOpen(true);
  };

  const handleInvoiceAction = async (action: string, invoice: Invoice) => {
    try {
      if (action === "email_invoice" || action === "send_invoice") {
        setToastMessage("Sending invoice...");
        await sendInvoiceEmail(invoice.id).unwrap();
        setToastMessage("Invoice sent successfully!");
      } 
      else if (action === "download_invoice" || action === "Download") {
        const printWindow = window.open("about:blank", "_blank");
        setToastMessage("Preparing invoice...");
        try {
          const html = resolveInvoiceHtml(await triggerDownloadInvoice(invoice.id).unwrap());
          openInvoiceHtmlForPrint(html, printWindow);
          setToastMessage("Invoice opened for printing");
        } catch (downloadError) {
          printWindow?.close();
          throw downloadError;
        }
      }
      else if (action === "preview_invoice") {
        setSelectedInvoice(invoice);
        setIsPreviewModalOpen(true);
        const previewPayload = await triggerPreview(invoice.id).unwrap();
        setPreviewHtml(resolveInvoiceHtml(previewPayload));
      }
      else if (action === "view_transactions") {
        setSelectedInvoice(invoice);
        setIsTransactionsModalOpen(true);
      }
      else if (action.startsWith("mark_as_")) {
        const statusMap: Record<string, string> = {
          mark_as_billed: "billed",
          mark_as_paid: "paid",
          mark_as_denied: "denied",
          mark_as_followup: "follow_up",
          mark_as_pending: "pending"
        };
        const targetStatus = statusMap[action];
        if (targetStatus) {
          setSelectedInvoice(invoice);
          setStatusToChange(targetStatus);
          setIsChangeStatusModalOpen(true);
        }
      }
    } catch (err) {
      setToastMessage(getApiErrorMessage(err));
    }
  };


  const payloadArgs = useMemo(
    () => ({
      ...statisticsArgs,
      page: page - 1,
      size: PAGE_SIZE,
      sort: "billingDate",
      direction: "desc" as const,
    }),
    [statisticsArgs, page],
  );

  const {
    currentData: recordsResponse,
    isLoading: isLoadingHistory,
    error: historyError,
    isFetching,
    isUninitialized,
  } = useGetBillingRecordsQuery(payloadArgs, { refetchOnMountOrArgChange: true });

  const moduleForbidden = useMemo(
    () =>
      [statsError, historyError].some((error) => isBillingModuleForbidden(error)),
    [historyError, statsError],
  );

  const { displayedInvoices, clearInvoices, isReadyToLoadMore } =
    useAccumulatedBillingRecords(recordsResponse, page, billingScope);

  const totalRecords =
    recordsResponse?.totalElements ?? billingStats?.totalBillingRecords ?? 0;

  useLayoutEffect(() => {
    scrollRootRef.current?.scrollTo({ top: 0 });
  }, []);

  const resetBillingList = useCallback(() => {
    setPage(1);
    clearInvoices();
  }, [clearInvoices, setPage]);

  useEffect(() => {
    if (historyError && !isBillingModuleForbidden(historyError)) {
      setToastMessage(getApiErrorMessage(historyError));
    }
  }, [historyError]);

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
    scrollRootRef,
    rootMargin: "200px",
  });

  const filteredInvoices = useMemo(
    () => displayedInvoices,
    [displayedInvoices],
  );

  const isInitialBillingLoad =
    filteredInvoices.length === 0 &&
    (isUninitialized || isLoadingHistory || isFetching);

  const overviewItems = [
    {
      label: "Outstanding Balance",
      value: `$${(billingStats?.outstandingBalance ?? 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
      subtext: `${billingStats?.pendingRecords ?? 0} pending payments`,
      icon: "dollar",
    },
    {
      label: "Total Collected",
      value: `$${(billingStats?.totalCollected ?? 0).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
      subtext: formatTotalCollectedSubtext(
        billingStats?.paidRecords,
        billingStats?.partialRecords,
      ),
      icon: "check",
    },
    {
      label: "Active Clients",
      value: billingStats?.activeClients ?? 0,
      subtext: "With billing records",
      icon: "users",
    },
    {
      label: "Total Records",
      value: totalRecords,
      subtext: "Billing records",
      icon: "list",
    },
  ];

  if (moduleForbidden) {
    return (
      <div className="flex flex-1 min-h-0 flex-col overflow-hidden">
        <BillingModuleGate />
      </div>
    );
  }

  return (
    <div className="flex flex-1 min-h-0 flex-col overflow-hidden">
      <ScrollToTopButton />
      <div className="grid shrink-0 grid-cols-2 md:grid-cols-4 md:gap-4 gap-2">
        {(isLoadingStats ? Array(4).fill(null) : overviewItems).map((item, index) => (
          <OverviewCard
            key={index}
            label={item?.label ?? ""}
            value={isLoadingStats ? "" : item?.value}
            icon={getIcon(item?.icon ?? "dollar")}
            subtext={item?.subtext}
            labelFirst={true}
            className="h-28"
            labelClassName="!text-base !leading-6"
            valueClassName="!mt-2 !text-xl !font-bold !leading-7"
            iconClassName="!w-auto"
          />
        ))}
      </div>
      <div className="mb-4 mt-5 flex shrink-0 items-center justify-between gap-2">
        <div className="text-(--text-primary-dark) font-semibold">
          Billing Records
        </div>
        <div className="flex items-center gap-2 w-full md:w-auto">
          <CustomInput
            placeholder="Search by client name or client number..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            icon={<Magnifer className="size-5 text-(--text-neutral-600)" />}
            className="min-h-10 rounded-full pb-0 pt-1.75 md:w-[21rem]"
          />
          <FilterDropdown filters={filters} setFilters={setFilters} />
        </div>
      </div>
      <AppliedFiltersBar
        chips={appliedFilterChips}
        onRemove={handleRemoveFilterChip}
        onClearAll={handleClearAllFilters}
        className="mb-3 shrink-0"
      />
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        <BillingTable
          data={filteredInvoices}
          isLoading={isInitialBillingLoad}
          isLoadingMore={isFetching && page > 1}
          loadMoreRef={observerTarget}
          scrollRootRef={scrollRootRef}
          onRecordPayment={handleRecordPayment}
          onApplyDiscount={handleApplyDiscountAction}
          onAction={handleInvoiceAction}
          isAdmin={true}
          clientsBasePath="/admin/clients"
        />
      </div>

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

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
            resetBillingList();
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
              discountValue: data.discountValue ? Number(data.discountValue) : undefined,
            }).unwrap();
            setToastMessage("Discount applied successfully");
            setIsDiscountModalOpen(false);
            resetBillingList();
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
            resetBillingList();
          } catch (error) {
            setToastMessage(getApiErrorMessage(error));
          }
        }}
      />

      <BillingTransactionsModal
        isOpen={isTransactionsModalOpen}
        onClose={() => setIsTransactionsModalOpen(false)}
        invoice={selectedInvoice}
      />
    </div>
  );
};

export default AdminBillings;
