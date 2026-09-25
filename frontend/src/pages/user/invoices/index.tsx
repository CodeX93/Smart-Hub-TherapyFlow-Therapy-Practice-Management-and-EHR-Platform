import { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import InvoiceOverviewCards from "../../../components/invoices/InvoiceOverviewCards";
import InvoiceTable from "../../../components/invoices/InvoiceTable";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useGetPortalInvoiceStatsQuery,
  useGetPortalInvoicesQuery,
  useGetPortalPaymentConfigQuery,
  useLazyGetPortalInvoiceReceiptHtmlQuery,
  usePayPortalInvoiceMutation,
  type PortalInvoice,
} from "@/store/api/portalApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  buildPortalInvoicesQueryArgs,
  DEFAULT_CLIENT_INVOICE_FILTERS,
  type ClientInvoiceFilters,
} from "@/utils/clientInvoiceFilters";
import {
  mapPortalInvoiceStatsToOverview,
  mapPortalInvoiceToRow,
} from "@/utils/portalInvoiceDisplay";
import { openInvoiceHtmlForPrint } from "@/utils/openInvoiceForPrint";
import { resolveInvoiceHtml } from "@/utils/resolveInvoiceHtml";

const SEARCH_DEBOUNCE_MS = 400;

const Invoices = () => {
  const listScrollRef = useRef<HTMLDivElement>(null);
  const [searchParams, setSearchParams] = useSearchParams();
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [invoiceFilters, setInvoiceFilters] = useState<ClientInvoiceFilters>(
    DEFAULT_CLIENT_INVOICE_FILTERS,
  );
  const [page, setPage] = useState(1);
  const [accumulatedInvoices, setAccumulatedInvoices] = useState<PortalInvoice[]>(
    [],
  );
  const [totalPages, setTotalPages] = useState(1);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("error");
  const [payingInvoiceId, setPayingInvoiceId] = useState<string | null>(null);
  const [downloadingInvoiceId, setDownloadingInvoiceId] = useState<string | null>(
    null,
  );
  const [isPaymentProcessing, setIsPaymentProcessing] = useState(false);

  const {
    data: statsData,
    isLoading: isStatsLoading,
    isError: isStatsError,
    error: statsError,
    refetch: refetchStats,
  } = useGetPortalInvoiceStatsQuery(undefined, {
    refetchOnMountOrArgChange: true,
  });

  const { data: paymentConfig } = useGetPortalPaymentConfigQuery(undefined, {
    refetchOnMountOrArgChange: true,
  });

  const listQueryArgs = useMemo(
    () => buildPortalInvoicesQueryArgs(page, invoiceFilters, debouncedSearch),
    [debouncedSearch, invoiceFilters, page],
  );

  const {
    data: invoicesPage,
    isLoading: isInvoicesLoading,
    isFetching: isInvoicesFetching,
    isError: isInvoicesError,
    error: invoicesError,
    refetch: refetchInvoices,
  } = useGetPortalInvoicesQuery(listQueryArgs, {
    refetchOnMountOrArgChange: true,
  });

  const [payInvoice] = usePayPortalInvoiceMutation();
  const [fetchReceiptHtml] = useLazyGetPortalInvoiceReceiptHtmlQuery();

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearch(searchQuery);
    }, SEARCH_DEBOUNCE_MS);

    return () => window.clearTimeout(timeoutId);
  }, [searchQuery]);

  useEffect(() => {
    setPage(1);
    setAccumulatedInvoices([]);
    setTotalPages(1);
  }, [debouncedSearch, invoiceFilters, setPage]);

  useEffect(() => {
    if (!invoicesPage) return;

    setTotalPages(invoicesPage.totalPages || 1);

    setAccumulatedInvoices((current) => {
      if (invoicesPage.page <= 1) {
        return invoicesPage.items;
      }

      const existingIds = new Set(current.map((invoice) => invoice.id));
      const nextItems = invoicesPage.items.filter(
        (invoice) => !existingIds.has(invoice.id),
      );
      return [...current, ...nextItems];
    });
  }, [invoicesPage]);

  const overview = useMemo(
    () =>
      statsData
        ? mapPortalInvoiceStatsToOverview(statsData)
        : {
            totalInvoices: 0,
            totalBilled: "$0.00",
            totalPaid: "$0.00",
          },
    [statsData],
  );

  const invoiceRows = useMemo(
    () => accumulatedInvoices.map((invoice) => mapPortalInvoiceToRow(invoice, paymentConfig)),
    [accumulatedInvoices, paymentConfig],
  );

  const hasMore = page < totalPages;
  const isInitialInvoicesLoading =
    (isInvoicesLoading || isInvoicesFetching) && accumulatedInvoices.length === 0;
  const isLoadingMore =
    isInvoicesFetching && page > 1 && accumulatedInvoices.length > 0;

  useEffect(() => {
    if (isStatsError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(statsError));
      return;
    }

    if (isInvoicesError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(invoicesError));
    }
  }, [invoicesError, isInvoicesError, isStatsError, statsError]);

  useEffect(() => {
    const paymentResult = searchParams.get("payment");
    if (!paymentResult) return;

    if (paymentResult === "success") {
      setToastType("success");
      setToastMessage("Payment completed successfully.");
      setIsPaymentProcessing(true);
      setPage(1);
      setAccumulatedInvoices([]);
      void Promise.all([refetchStats(), refetchInvoices()]);
    } else if (paymentResult === "cancelled") {
      setToastType("error");
      setToastMessage("Payment was cancelled.");
    }

    const nextParams = new URLSearchParams(searchParams);
    nextParams.delete("payment");
    nextParams.delete("session_id");
    setSearchParams(nextParams, { replace: true });
  }, [refetchInvoices, refetchStats, searchParams, setSearchParams, setPage]);

  useEffect(() => {
    if (!isPaymentProcessing) return;

    let attempts = 0;
    const maxAttempts = 15;
    const intervalId = window.setInterval(() => {
      attempts += 1;
      void Promise.all([refetchInvoices(), refetchStats()]);
      if (attempts >= maxAttempts) {
        setIsPaymentProcessing(false);
        window.clearInterval(intervalId);
      }
    }, 2000);

    return () => window.clearInterval(intervalId);
  }, [isPaymentProcessing, refetchInvoices, refetchStats]);

  useEffect(() => {
    if (!isPaymentProcessing || accumulatedInvoices.length === 0) return;

    const hasUpdatedPayment = accumulatedInvoices.some((invoice) => {
      const paymentStatus = (invoice.paymentStatus || "").trim().toLowerCase();
      return paymentStatus === "paid" || paymentStatus === "partial";
    });

    if (hasUpdatedPayment) {
      setIsPaymentProcessing(false);
    }
  }, [accumulatedInvoices, isPaymentProcessing]);

  const handleLoadMore = () => {
    if (!hasMore || isInvoicesFetching) return;
    setPage((currentPage) => currentPage + 1);
  };

  const handlePayNow = async (invoiceId: string) => {
    const numericId = Number.parseInt(invoiceId, 10);
    if (Number.isNaN(numericId)) return;

    try {
      setPayingInvoiceId(invoiceId);
      setToastMessage(null);
      const response = await payInvoice(numericId).unwrap();

      if (!response.checkoutUrl) {
        setToastType("error");
        setToastMessage("Unable to start checkout. Please try again.");
        return;
      }

      window.location.assign(response.checkoutUrl);
    } catch (payError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(payError));
    } finally {
      setPayingInvoiceId(null);
    }
  };

  const handleDownloadReceipt = async (invoiceId: string) => {
    const numericId = Number.parseInt(invoiceId, 10);
    if (Number.isNaN(numericId)) return;

    try {
      setDownloadingInvoiceId(invoiceId);
      setToastMessage(null);
      const html = resolveInvoiceHtml(await fetchReceiptHtml(numericId).unwrap());
      openInvoiceHtmlForPrint(html);
      setToastType("success");
      setToastMessage("Invoice opened for printing.");
    } catch (receiptError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(receiptError));
    } finally {
      setDownloadingInvoiceId(null);
    }
  };

  return (
    <div className="flex h-[calc(100vh-10rem)] min-h-0 flex-col gap-4 overflow-hidden">
      <ScrollToTopButton containerRef={listScrollRef} />
      {isPaymentProcessing ? (
        <div className="shrink-0 rounded-2xl border border-(--primary-100) bg-(--bg-primary-50) px-4 py-3 text-sm text-(--text-primary-dark)">
          Payment processing... invoice status will update shortly.
        </div>
      ) : null}
      {paymentConfig && !paymentConfig.onlinePaymentsEnabled ? (
        <div className="shrink-0 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          {paymentConfig.disabledReason ||
            "Online card payments are temporarily unavailable. Please contact your clinic for payment options."}
        </div>
      ) : null}
      <div className="shrink-0">
        <InvoiceOverviewCards overview={overview} isLoading={isStatsLoading} />
      </div>
      <InvoiceTable
        invoices={invoiceRows}
        isLoading={isInitialInvoicesLoading}
        isLoadingMore={isLoadingMore}
        hasMore={hasMore}
        onLoadMore={handleLoadMore}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        filters={invoiceFilters}
        setFilters={setInvoiceFilters}
        payingInvoiceId={payingInvoiceId}
        downloadingInvoiceId={downloadingInvoiceId}
        onPayNow={(invoiceId) => void handlePayNow(invoiceId)}
        onDownloadReceipt={(invoiceId) => void handleDownloadReceipt(invoiceId)}
        scrollContainerRef={listScrollRef}
      />
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

export default Invoices;
