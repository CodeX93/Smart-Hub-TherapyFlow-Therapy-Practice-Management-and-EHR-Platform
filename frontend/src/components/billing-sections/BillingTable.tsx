import { ContentLoader } from "@/components/shared/ContentLoader";
import { MenuDotsIcon } from "@/components/icons/commonIcons";
import type { RefObject } from "react";
import { CheckCircle2, Mail, Download, FileText, GitPullRequestDraft, SquarePercent, X } from "lucide-react";
import { Eye } from "@solar-icons/react-perf/category/security/Linear/Eye";
import { ArrowDown } from "@solar-icons/react-perf/category/arrows/Linear/ArrowDown";
import { Card } from "@solar-icons/react-perf/category/money/Linear/Card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import type { Invoice } from "../../types/invoice.type";
import ActionDropdown, { type DropdownAction } from "../shared/ActionDropdown";
import { Link } from "react-router-dom";
import { cn } from "@/lib/utils";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../ui/tooltip";
import {
  canRecordBillingPayment,
  getBillingAmountDisplay,
  shouldShowCopayBadge,
} from "@/utils/sessionBillingUi";
import { formatPaymentMethodLabel } from "@/utils/paymentMethodDisplay";
import {
  getSessionStatusBadgeClass,
  normalizeSessionStatus,
} from "@/utils/sessionStatusPresentation";
import EmptyBillingState from "./EmptyBillingState";
import { useGetPracticeConfigurationQuery } from "@/store/api/admin/systemOptions.api";
import {
  formatDateInScheduleTimezone,
  formatTimeInScheduleTimezone,
} from "@/utils/scheduleTimezone";
import { DateTime } from "luxon";

interface BillingTableProps {
  data: Invoice[];
  isLoading?: boolean;
  isLoadingMore?: boolean;
  loadMoreRef?: RefObject<HTMLDivElement | null>;
  scrollRootRef?: RefObject<HTMLDivElement | null>;
  onRecordPayment: (invoice: Invoice) => void;
  /** @deprecated Use onRecordPayment */
  onPayNow?: (invoice: Invoice) => void;
  onApplyDiscount: (invoice: Invoice) => void;
  onAction: (action: string, invoice: Invoice) => void;
  /** Omit until sorting is implemented: the sort control only shows with it. */
  onSort?: (column: string) => void;
  isAdmin?: boolean;
  isTherapist?: boolean;
  clientsBasePath?: string;
  allowBillingMutations?: boolean;
}

const BillingTable = ({
  data,
  isLoading,
  isLoadingMore,
  loadMoreRef,
  scrollRootRef,
  onRecordPayment,
  onPayNow,
  onApplyDiscount,
  onAction,
  onSort,
  isAdmin,
  clientsBasePath,
  isTherapist = false,
  allowBillingMutations,
}: BillingTableProps) => {
  const { data: practiceConfig } = useGetPracticeConfigurationQuery();
  const practiceTimezone = practiceConfig?.timezone;
  const columnCount = isAdmin ? 8 : 7;
  const canMutateBilling = allowBillingMutations ?? Boolean(isAdmin);
  const handleRecordPayment = onRecordPayment ?? onPayNow;
  const resolvedClientsPath =
    clientsBasePath ??
    (isAdmin ? "/admin/clients" : isTherapist ? "/therapist/clients" : "/admin/clients");
  const isInvoiceActionLocked = (invoice: Invoice) =>
    invoice.status === "paid" || invoice.status === "denied";

  const formatSessionDateTime = (sessionDate?: string | null) => {
    if (!sessionDate) return null;
    const datePart = formatDateInScheduleTimezone(sessionDate, practiceTimezone);
    const timePart = formatTimeInScheduleTimezone(sessionDate, practiceTimezone);
    if (!datePart || datePart === "-") return null;
    return timePart ? `${datePart} · ${timePart}` : datePart;
  };

  /** Billing dates are date-only values sent as UTC midnight — format the calendar day, not local TZ. */
  const formatBillingDateDisplay = (invoice: Invoice) => {
    const raw = invoice.billingDate;
    if (raw) {
      const parsed = DateTime.fromISO(String(raw), { zone: "utc" });
      if (parsed.isValid) return parsed.toFormat("MMM dd, yyyy");
    }
    return invoice.date || "---";
  };

  const formatBillingTimeDisplay = (invoice: Invoice) => {
    // Prefer session start time under Billing date when available.
    if (!invoice.sessionDate) return null;
    return formatTimeInScheduleTimezone(invoice.sessionDate, practiceTimezone) ?? null;
  };

  const hasDiscountApplied = (invoice: Invoice) => {
    const type = (invoice.discountType ?? "").toLowerCase();
    const amount = Number.parseFloat(String(invoice.discountAmount ?? "0"));
    return Boolean(type && type !== "none" && type !== "no_discount" && amount > 0);
  };

  const getStatusSubMenu = (invoice: Invoice): DropdownAction[] => [
    {
      label: "Mark as Paid",
      icon: <CheckCircle2 />,
      iconClassName: "text-(--status-paid)",
      onClick: () => handleRecordPayment?.(invoice),
    },
    {
      label: "Mark as Denied",
      icon: <X />,
      iconClassName: "text-(--status-denied)",
      onClick: () => onAction("mark_as_denied", invoice),
    },
  ];

  const getInvoiceActions = (invoice: Invoice): DropdownAction[] => {
    const actions: DropdownAction[] = [
      {
        label: "Email Invoice",
        icon: <Mail />,
        onClick: () => onAction("email_invoice", invoice),
      },
      {
        label: "Preview Invoice",
        icon: <Eye />,
        onClick: () => onAction("preview_invoice", invoice),
      },
      {
        label: "Download Invoice",
        icon: <Download />,
        onClick: () => onAction("download_invoice", invoice),
      },
    ];

    if (canMutateBilling) {
      if (!isTherapist) {
        actions.push({
          label: "Apply Discount",
          icon: <SquarePercent />,
          disabled: isInvoiceActionLocked(invoice) || hasDiscountApplied(invoice),
          onClick: () => onApplyDiscount(invoice),
        });
      }
      actions.push({
        label: "View Transactions",
        icon: <FileText />,
        onClick: () => onAction("view_transactions", invoice),
      });
    }

    if (isAdmin) {
      const statusLocked = isInvoiceActionLocked(invoice);
      actions.push({
        label: "Change Status",
        icon: <GitPullRequestDraft />,
        disabled: statusLocked,
        subMenu: statusLocked ? undefined : getStatusSubMenu(invoice),
      });
    }

    return actions;
  };

  return (
    <div className="flex h-full min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-(--neutral-100) bg-white shadow-[0px_2px_2px_0px_var(--shadow)]">
      <div
        ref={scrollRootRef}
        className="min-h-0 flex-1 overflow-auto overscroll-contain"
      >
        <table
          className={cn(
            "w-full table-fixed",
            isAdmin ? "min-w-[70.75rem]" : "min-w-[62rem]",
          )}
        >
          <thead className="sticky top-0 z-10 bg-(--bg-primary-50)">
            <tr className="h-[2.875rem]">
            <th className="w-[9.375rem] px-3 py-3 text-left text-[0.875rem] font-semibold text-(--text-primary-dark)">
              <div className="flex items-center gap-1">
                Client
                {onSort ? (
                  <ArrowDown className="size-4 cursor-pointer" onClick={() => onSort("client")} />
                ) : null}
              </div>
            </th>
            <th className="text-left w-[18.75rem] px-2 py-3 text-[0.875rem] font-semibold text-(--text-primary-dark) ">
              Service
            </th>
            {isAdmin && (
              <th className="w-[8.75rem] px-2 py-3 text-left text-[0.875rem] font-semibold text-(--text-primary-dark)">
                Therapist
              </th>
            )}
            <th className="w-[7.5rem] px-2 py-3 text-left text-[0.875rem] font-semibold text-(--text-primary-dark)">
              Billing date
            </th>
            <th className="text-left w-[6.25rem] px-2 py-3 text-[0.875rem] font-semibold text-(--text-primary-dark) ">
              Amount
            </th>
            <th className="w-[5rem] px-2 py-3 text-left text-[0.875rem] font-semibold text-(--text-primary-dark)">
              Paid
            </th>
            <th className="w-[10.625rem] px-2 py-3 text-left text-[0.875rem] font-semibold text-(--text-primary-dark)">
              Status
            </th>
            <th className="w-[10.125rem] px-3 py-3 text-center text-[0.875rem] font-semibold text-(--text-primary-dark)">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          {isLoading ? (
            <tr className="h-[4.5rem] border-t border-(--neutral-100)">
              <td colSpan={columnCount} className="text-center py-8">
                <ContentLoader variant="inline" size="md" className="mx-auto" />
              </td>
            </tr>
          ) : data.length === 0 ? (
            <tr className="border-t border-(--neutral-100)">
              <td colSpan={columnCount} className="px-3 py-8">
                <EmptyBillingState
                  title="No billing records available"
                  description="Billing records will appear here"
                />
              </td>
            </tr>
          ) : (
            data.map((invoice, index) => {
            const serviceName = invoice.service.includes("PSY-")
              ? invoice.service.split(" PSY-")[0]
              : invoice.service;
            const actualSessionDateTime = formatSessionDateTime(invoice.sessionDate);
            const billingDateLabel = formatBillingDateDisplay(invoice);
            const billingTimeLabel = formatBillingTimeDisplay(invoice);
            const showRecordPayment =
              canMutateBilling && canRecordBillingPayment(invoice);

            return (
            <tr
              key={invoice.id || index}
              className="h-[4.25rem] border-t border-(--neutral-100)"
              style={{ height: "4.25rem" }}
            >
              <td
                className="w-[9.375rem] max-w-[9.375rem] px-3 text-sm text-(--text-primary-dark)"

              >
                <div className="flex min-w-0 flex-col gap-0.5">
                  {invoice.clientId ? (
                    <Link
                      to={resolvedClientsPath}
                      state={{ clientId: invoice.clientId, initialTab: "Billing" }}
                      className="block truncate underline font-medium text-(--text-primary-dark)"
                      title={invoice.client}
                    >
                      {invoice.client}
                    </Link>
                  ) : (
                    <span className="block truncate font-medium" title={invoice.client}>{invoice.client}</span>
                  )}
                  {invoice.clientReferenceNumber?.trim() && invoice.clientId ? (
                    <Link
                      to={resolvedClientsPath}
                      state={{ clientId: invoice.clientId, initialTab: "Overview" }}
                      className="block truncate text-xs underline text-(--text-neutral-400)"
                      title={`Ref: ${invoice.clientReferenceNumber.trim()}`}
                    >
                      Ref: {invoice.clientReferenceNumber.trim()}
                    </Link>
                  ) : null}
                  {normalizeSessionStatus(invoice.sessionStatus) === "noshow" ? (
                    <Badge
                      variant="outline"
                      className={cn(
                        "mt-0.5 w-fit border-transparent px-1.5 py-0 text-[0.6875rem] font-medium",
                        getSessionStatusBadgeClass("noshow"),
                      )}
                    >
                      No Show
                    </Badge>
                  ) : null}
                </div>
              </td>
              <td className="text-sm text-(--text-primary-dark) w-[18.75rem] max-w-[18.75rem] px-2">
                <div className="flex min-w-0 flex-col gap-0.5">
                  <span
                    className="block truncate text-sm font-normal leading-[1.375rem] text-(--text-primary-dark)"
                    title={serviceName}
                  >
                    {serviceName}
                  </span>
                  {actualSessionDateTime ? (
                    <span
                      className="block truncate text-xs leading-[1.125rem] text-(--text-neutral-400)"
                      title={`Actual session date: ${actualSessionDateTime}`}
                    >
                      Actual session date: {actualSessionDateTime}
                    </span>
                  ) : null}
                  {invoice.invoicePolicyId ? (
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Badge
                            variant="outline"
                            className="mt-0.5 w-fit border-(--primary-100) bg-(--bg-primary-50) text-[0.6875rem] text-(--text-primary-500)"
                          >
                            Policy
                          </Badge>
                        </TooltipTrigger>
                        <TooltipContent>
                          {invoice.policyName || `Policy #${invoice.invoicePolicyId}`}
                        </TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                  ) : null}
                </div>
              </td>
              {isAdmin && (
                <td className="w-[8.75rem] max-w-[8.75rem] px-2 text-sm text-(--text-primary-dark)">
                  <span className="block truncate" title={invoice.therapist}>
                    {invoice.therapist}
                  </span>
                </td>
              )}
              <td className="w-[7.5rem] px-2 text-sm text-(--text-primary-dark)">
                <div className="flex min-w-0 flex-col gap-0.5">
                  <span className="block truncate" title={billingDateLabel}>
                    {billingDateLabel}
                  </span>
                  {billingTimeLabel ? (
                    <span
                      className="block truncate text-xs text-(--text-neutral-400)"
                      title={billingTimeLabel}
                    >
                      {billingTimeLabel}
                    </span>
                  ) : null}
                </div>
              </td>
              <td className="w-[6.25rem] px-2 text-sm font-normal text-(--text-primary-dark)">
                {(() => {
                  const { primary, partialRemaining } = getBillingAmountDisplay(invoice);
                  return (
                    <div className="flex min-w-0 flex-col gap-0.5">
                      <span>${primary}</span>
                      {shouldShowCopayBadge(invoice) ? (
                        <Badge
                          variant="outline"
                          className="w-fit border-[#BFDBFE] bg-[#EFF6FF] px-1.5 py-0 text-[0.6875rem] font-medium text-[#1D4ED8]"
                          title={
                            invoice.copay != null
                              ? `Client copay $${invoice.copay}`
                              : "Insurance-covered bill (copay)"
                          }
                        >
                          Copay
                        </Badge>
                      ) : null}
                      {partialRemaining ? (
                        <span
                          className="truncate text-[0.6875rem] font-normal text-(--text-neutral-500)"
                          title={`Remaining - $${partialRemaining}`}
                        >
                          Remaining - ${partialRemaining}
                        </span>
                      ) : null}
                    </div>
                  );
                })()}
              </td>
              <td className="w-[5rem] px-2 text-sm text-(--text-primary-dark)">
                ${invoice.paid}
              </td>
              <td className="w-[10.625rem] px-2 text-sm text-(--text-primary-dark)">
                {(() => {
                  const paymentMethodLabel = formatPaymentMethodLabel(
                    invoice.paymentMethod,
                  );
                  const paidMeta =
                    (invoice.status === "paid" || invoice.status === "partial") &&
                    (invoice.paidDate || paymentMethodLabel)
                      ? [
                          invoice.paidDate,
                          paymentMethodLabel ? `via ${paymentMethodLabel}` : null,
                        ]
                          .filter(Boolean)
                          .join(" | ")
                      : null;

                  return (
                    <div className="flex min-w-0 flex-col gap-1 overflow-hidden">
                      <Badge
                        variant="outline"
                        className={cn(
                          "w-fit max-w-full h-6 px-2.5 text-xs font-medium border-0 rounded-full",
                          invoice.status === "paid" && "bg-[#D0FBE3] text-[#036243]",
                          invoice.status === "partial" && "bg-[#EEF4FF] text-[#1D4ED8]",
                          invoice.status === "billed" &&
                            "bg-(--bg-primary-50) text-(--text-primary-500)",
                          invoice.status === "denied" && "bg-[#fff5f5] text-[#b42318]",
                          invoice.status === "follow_up" && "bg-[#f9f5ff] text-[#6941c0]",
                          invoice.status === "pending" && "bg-[#FFF7C5] text-[#BB5D02]",
                        )}
                      >
                        {invoice.status === "paid" && "Paid"}
                        {invoice.status === "partial" && "Partial"}
                        {invoice.status === "billed" && "Billed"}
                        {invoice.status === "denied" && "Denied"}
                        {invoice.status === "follow_up" && "Follow-up"}
                        {invoice.status === "pending" && "Pending"}
                        {!["paid", "partial", "billed", "denied", "follow_up", "pending"].includes(
                          invoice.status,
                        ) &&
                          invoice.status.charAt(0).toUpperCase() +
                            invoice.status.slice(1).replace("_", " ")}
                      </Badge>
                      {paidMeta ? (
                        <span
                          className="truncate text-[0.6875rem] text-(--text-neutral-500)"
                          title={paidMeta}
                        >
                          {paidMeta}
                        </span>
                      ) : null}
                    </div>
                  );
                })()}
              </td>
              <td className="w-[10.125rem] px-2 text-right text-sm text-(--text-primary-dark)">
                <div className="flex items-center justify-end gap-2">
                  {isInvoiceActionLocked(invoice) ? (
                    <Button
                      variant="outline"
                      className="h-9 w-[6.8125rem] cursor-pointer gap-1 rounded-full border border-[#D8DBDF] bg-white px-4 text-sm font-semibold text-[#3C4D58] transition-colors duration-300 hover:bg-(--bg-primary-50)"
                      onClick={() => onAction("preview_invoice", invoice)}
                    >
                      <Eye className="size-5" />
                      Preview
                    </Button>
                  ) : showRecordPayment && handleRecordPayment ? (
                    <Button
                      className="h-9 min-w-[6.8125rem] cursor-pointer gap-1 rounded-full bg-[#3C4D58] px-3 text-sm font-semibold text-white"
                      onClick={() => handleRecordPayment(invoice)}
                    >
                      <Card size={18} />
                      Pay now
                    </Button>
                  ) : (
                    <Button
                      variant="outline"
                      className="h-9 w-[6.8125rem] cursor-pointer gap-1 rounded-full border border-[#D8DBDF] bg-white px-4 text-sm font-semibold text-[#3C4D58] transition-colors duration-300 hover:bg-(--bg-primary-50)"
                      onClick={() => onAction("preview_invoice", invoice)}
                    >
                      <Eye className="size-5" />
                      Preview
                    </Button>
                  )}
                  <ActionDropdown
                    actions={getInvoiceActions(invoice)}
                    trigger={
                      <Button variant="ghost" size="icon" className="h-8 w-8 cursor-pointer">
                        <MenuDotsIcon size={24} />
                      </Button>
                    }
                  />
                </div>
              </td>
            </tr>
            );
            })
          )}
          {!isLoading && data.length > 0 && loadMoreRef ? (
            <tr>
              <td colSpan={columnCount} className="p-0">
                <div
                  ref={loadMoreRef}
                  className="flex h-10 w-full items-center justify-center"
                >
                  {isLoadingMore ? (
                    <ContentLoader variant="inline" size="md" className="mx-auto" />
                  ) : null}
                </div>
              </td>
            </tr>
          ) : null}
        </tbody>
        </table>
      </div>
    </div>
  );
};

export default BillingTable;
