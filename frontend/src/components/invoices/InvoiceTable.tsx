
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useRef, useMemo, useCallback } from "react";

import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import { Search } from "lucide-react";
import InvoiceTableComponent, {
  type InvoiceTableColumn,
  TruncatedCellText,
} from "../shared/InvoiceTable";
import CustomInput from "../form/CustomInput";
import ClientInvoicesFilterPanel from "./ClientInvoicesFilterPanel";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import type { ClientInvoiceRow } from "@/utils/portalInvoiceDisplay";
import {
  getPortalInvoiceStatusLabel,
  getPortalInvoiceStatusStyles,
} from "@/utils/portalInvoiceDisplay";
import { formatPaymentMethodLabel } from "@/utils/paymentMethodDisplay";
import {
  DEFAULT_CLIENT_INVOICE_FILTERS,
  type ClientInvoiceFilters,
} from "@/utils/clientInvoiceFilters";
import AppliedFiltersBar from "@/components/shared/AppliedFiltersBar";
import EmptyBillingState from "@/components/billing-sections/EmptyBillingState";
import {
  buildClientInvoiceFilterChips,
  removeClientInvoiceFilterChip,
} from "@/utils/appliedFilterChips";

interface InvoiceTableProps {
  invoices: ClientInvoiceRow[];
  isLoading?: boolean;
  isLoadingMore?: boolean;
  hasMore?: boolean;
  onLoadMore: () => void;
  searchQuery: string;
  onSearchChange: (value: string) => void;
  filters: ClientInvoiceFilters;
  setFilters: (filters: ClientInvoiceFilters) => void;
  payingInvoiceId: string | null;
  downloadingInvoiceId: string | null;
  onPayNow: (invoiceId: string) => void;
  onDownloadReceipt: (invoiceId: string) => void;
  scrollContainerRef?: React.RefObject<HTMLDivElement | null>;
}

const InvoiceTable = ({
  invoices,
  isLoading = false,
  isLoadingMore = false,
  hasMore = false,
  onLoadMore,
  searchQuery,
  onSearchChange,
  filters,
  setFilters,
  payingInvoiceId,
  downloadingInvoiceId,
  onPayNow,
  onDownloadReceipt,
  scrollContainerRef,
}: InvoiceTableProps) => {
  const internalScrollRef = useRef<HTMLDivElement>(null);
  const listScrollRef = scrollContainerRef ?? internalScrollRef;

  const appliedFilterChips = useMemo(
    () => buildClientInvoiceFilterChips(filters),
    [filters],
  );

  const handleRemoveFilterChip = useCallback(
    (chipId: string) => {
      setFilters(removeClientInvoiceFilterChip(filters, chipId));
    },
    [filters, setFilters],
  );

  const handleClearAllFilters = useCallback(() => {
    setFilters(DEFAULT_CLIENT_INVOICE_FILTERS);
  }, [setFilters]);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore,
    hasMore,
    isLoading: isLoadingMore,
    scrollRootRef: listScrollRef,
  });

  const columns: InvoiceTableColumn<ClientInvoiceRow>[] = [
    {
      key: "date",
      label: "Date",
      width: "8.125rem",
      padding: "1.25rem 0.75rem",
      headerPadding: "0.75rem",
    },
    {
      key: "service",
      label: "Service",
      width: "21.25rem",
      padding: "1rem 0.5rem",
      headerPadding: "0.75rem",
      render: (_, invoice) => (
        <div className="flex min-w-0 flex-col gap-1.5 overflow-hidden">
          <TruncatedCellText>{invoice.service}</TruncatedCellText>
          {invoice.serviceCode ? (
            <TruncatedCellText className="text-xs text-(--text-neutral-600)">
              {invoice.serviceCode}
            </TruncatedCellText>
          ) : null}
        </div>
      ),
    },
    {
      key: "amount",
      label: "Amount",
      width: "7.5rem",
      padding: "1.25rem 0.5rem",
      className: "font-medium",
      render: (_, invoice) => (
        <div className="flex min-w-0 flex-col gap-0.5 overflow-hidden">
          <span>{invoice.amount}</span>
          {invoice.originalSubtotal ? (
            <TruncatedCellText className="text-[0.6875rem] font-normal text-(--text-neutral-500)">
              {invoice.originalSubtotal}
            </TruncatedCellText>
          ) : null}
        </div>
      ),
    },
    {
      key: "paid",
      label: "Paid",
      width: "6.875rem",
      padding: "1.25rem 0.5rem",
      className: "font-medium",
      render: (_, invoice) => <span>{invoice.paid ?? "$0.00"}</span>,
    },
    {
      key: "status",
      label: "Status",
      width: "10.625rem",
      padding: "1.25rem 0.5rem",
      render: (_, invoice) => {
        const paymentMethodLabel = formatPaymentMethodLabel(invoice.paymentMethod);
        const paidMeta =
          (invoice.status === "paid" || invoice.status === "partial") &&
          invoice.paidDate
            ? `${invoice.paidDate}${paymentMethodLabel ? ` | via ${paymentMethodLabel}` : ""}`
            : null;

        return (
          <div className="flex min-w-0 flex-col gap-1 overflow-hidden">
            <Badge
              variant="outline"
              className={`max-w-full text-xs font-normal ${getPortalInvoiceStatusStyles(invoice.status)}`}
              title={getPortalInvoiceStatusLabel(invoice.status)}
            >
              <span className="block min-w-0 truncate">
                {getPortalInvoiceStatusLabel(invoice.status)}
              </span>
            </Badge>
            {paidMeta ? (
              <TruncatedCellText className="text-xs text-(--text-neutral-600)">
                {paidMeta}
              </TruncatedCellText>
            ) : null}
          </div>
        );
      },
    },
    {
      key: "actions",
      label: "Actions",
      width: "10.125rem",
      padding: "1.25rem 0.5rem",
      truncate: false,
      render: (_, invoice) => {
        if (invoice.canPay) {
          return (
            <Button
              type="button"
              className="h-9 min-w-27.75 cursor-pointer rounded-full px-4 text-[0.875rem] font-semibold"
              disabled={payingInvoiceId === invoice.id}
              loading={payingInvoiceId === invoice.id}
              loadingLabel="Processing..."
              onClick={() => onPayNow(invoice.id)}
            >
              <img
                src="/invoice/receipt.svg"
                alt=""
                className="h-5 w-5"
                aria-hidden="true"
              />
              Pay now
            </Button>
          );
        }

        if (invoice.payDisabledReason) {
          return (
            <Button
              type="button"
              className="h-9 min-w-27.75 rounded-full px-4 text-[0.875rem] font-semibold"
              disabled
              title={invoice.payDisabledReason}
            >
              <img
                src="/invoice/receipt.svg"
                alt=""
                className="h-5 w-5 opacity-50"
                aria-hidden="true"
              />
              Pay now
            </Button>
          );
        }

        if (invoice.canDownloadReceipt) {
          return (
            <Button
              type="button"
              variant="outline"
              className="h-9 min-w-27.75 cursor-pointer rounded-full px-3 text-[0.875rem] font-semibold"
              disabled={downloadingInvoiceId === invoice.id}
              loading={downloadingInvoiceId === invoice.id}
              loadingLabel="Downloading receipt..."
              onClick={() => onDownloadReceipt(invoice.id)}
            >
              <img
                src="/invoice/paynow.svg"
                alt=""
                className="h-5 w-5"
                aria-hidden="true"
              />
              Receipt
            </Button>
          );
        }

        return <span className="text-xs text-(--text-neutral-500)">—</span>;
      },
    },
  ];

  return (
    <div className="flex min-h-0 w-full flex-1 flex-col overflow-hidden">
      <div className="mb-4 flex shrink-0 flex-col justify-between gap-4 md:flex-row md:items-center">
        <h3 className="text-xl leading-6 font-semibold tracking-0 text-(--text-primary-dark)">
          My invoices
        </h3>
        <div className="flex items-center gap-2">
          <div className="flex w-full items-center gap-2">
            <CustomInput
              placeholder="Search..."
              value={searchQuery}
              onChange={(event) => onSearchChange(event.target.value)}
              icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
              className="min-h-10 rounded-full pt-1.75 pb-0 md:w-79"
            />
            <ClientInvoicesFilterPanel
              filters={filters}
              setFilters={setFilters}
            />
          </div>
        </div>
      </div>

      <AppliedFiltersBar
        chips={appliedFilterChips}
        onRemove={handleRemoveFilterChip}
        onClearAll={handleClearAllFilters}
        className="mb-4 shrink-0"
      />

      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        {isLoading && invoices.length === 0 ? (
          <div className="flex flex-1 items-center justify-center gap-2">
            <ContentLoader variant="inline" size="md" />
            <span className="text-sm text-(--text-neutral-600)">
              Loading invoices...
            </span>
          </div>
        ) : invoices.length === 0 ? (
          <div className="flex flex-1 items-center justify-center rounded-lg border border-(--neutral-100) bg-white px-4">
            <EmptyBillingState
              title={
                searchQuery.trim() ||
                appliedFilterChips.length > 0
                  ? "No invoices found"
                  : "No invoices available"
              }
              description={
                searchQuery.trim() || appliedFilterChips.length > 0
                  ? "Try adjusting your search or filters"
                  : "Invoices will appear here"
              }
            />
          </div>
        ) : (
          <div
            ref={listScrollRef}
            className="min-h-0 flex-1 overflow-y-auto overscroll-contain rounded-xl border border-[#EDEEF1] bg-white shadow-[0px_2px_2px_0px_#1E282E0A]"
          >
            <InvoiceTableComponent
              columns={columns}
              data={invoices}
              rowKey={(invoice) => invoice.id}
              stickyHeader
              containerClassName="overflow-visible border-0 shadow-none"
              containerStyle={{
                border: "none",
                boxShadow: "none",
                borderRadius: 0,
                background: "transparent",
              }}
            />
            <div
              ref={observerTarget}
              className="flex h-10 w-full items-center justify-center"
            >
              {isLoadingMore ? (
                <ContentLoader variant="inline" size="md" />
              ) : null}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default InvoiceTable;
