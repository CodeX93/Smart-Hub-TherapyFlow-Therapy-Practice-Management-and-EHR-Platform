import {
  CheckCircle2,
  Eye,
  Mail,
  Download,
  MoveDown,
  GitPullRequestDraft,
  SquarePercent,
  X,
} from "lucide-react";
import { Badge } from "../ui/badge";
import {
  getBillingAmountDisplay,
  shouldShowCopayBadge,
} from "@/utils/sessionBillingUi";
import { Button } from "../ui/button";
import type { Invoice } from "../../types/invoice.type";
import type { InvoiceTableColumn } from "../shared/InvoiceTable";
import ActionDropdown, { type DropdownAction } from "../shared/ActionDropdown";
import {
  getPortalInvoiceStatusLabel,
  getPortalInvoiceStatusStyles,
} from "@/utils/portalInvoiceDisplay";
import { formatPaymentMethodLabel } from "@/utils/paymentMethodDisplay";
import { normalizeSessionStatus } from "@/utils/sessionStatusPresentation";

interface useBillingsTableProps {
  onRecordPayment: (invoice: Invoice) => void;
  /** @deprecated Use onRecordPayment */
  onPayNow?: (invoice: Invoice) => void;
  onApplyDiscount: (invoice: Invoice) => void;
}

export const useBillingsTable = ({
  onRecordPayment,
  onPayNow,
  onApplyDiscount,
}: useBillingsTableProps) => {
  const handleRecordPayment = onRecordPayment ?? onPayNow;
  const getStatusSubMenu = (invoice: Invoice): DropdownAction[] => [
    {
      label: "Mark as Paid",
      icon: <CheckCircle2 />,
      iconClassName: "text-(--status-paid)",
      onClick: () => console.log("Mark as Paid", invoice.id),
    },
    {
      label: "Mark as Denied",
      icon: <X />,
      iconClassName: "text-(--status-denied)",
      onClick: () => console.log("Mark as Denied", invoice.id),
    },
  ];

  const getPreviewActions = (invoice: Invoice): DropdownAction[] => [
    {
      label: "Email Invoice",
      icon: <Mail />,
      onClick: () => console.log("Email", invoice.id),
    },
    {
      label: "Download Invoice",
      icon: <Download />,
      onClick: () => console.log("Download", invoice.id),
    },
    {
      label: "Change Status",
      icon: <GitPullRequestDraft />,
      subMenu: getStatusSubMenu(invoice),
    },
  ];

  const getInvoiceActions = (invoice: Invoice): DropdownAction[] => [
    {
      label: "Email Invoice",
      icon: <Mail />,
      onClick: () => console.log("Email", invoice.id),
    },
    {
      label: "Preview Invoice",
      icon: <Eye />,
      onClick: () => console.log("Preview", invoice.id),
    },
    {
      label: "Download Invoice",
      icon: <Download />,
      onClick: () => console.log("Download", invoice.id),
    },
    {
      label: "Apply Discount",
      icon: <SquarePercent />,
      onClick: () => onApplyDiscount(invoice),
    },
    {
      label: "Change Status",
      icon: <GitPullRequestDraft />,
      subMenu: getStatusSubMenu(invoice),
    },
  ];

  const columns: InvoiceTableColumn<Invoice>[] = [
    {
      key: "client",
      label: "Client",
      width: "10rem",
      headerContent: (
        <div className="flex items-center gap-1">
          Client
          <MoveDown
            size={16}
            strokeWidth={1.5}
            className="cursor-pointer"
            onClick={() => console.log("Sort by client")}
          />
        </div>
      ),
      render: (value: string, invoice: Invoice) => (
        <div className="flex min-w-0 flex-col gap-0.5">
          <span
            className="block truncate font-medium text-(--text-primary-dark) underline cursor-pointer"
            title={value}
          >
            {value}
          </span>
          {normalizeSessionStatus(invoice.sessionStatus) === "noshow" ? (
            <Badge
              variant="outline"
              className="mt-0.5 w-fit border-transparent bg-(--dashboard-status-pending-light) px-1.5 py-0 text-[0.6875rem] font-medium text-(--dashboard-status-pending-dark)"
            >
              No Show
            </Badge>
          ) : null}
        </div>
      ),
    },
    {
      key: "service",
      label: "Service",
      width: "18.75rem",
      render: (_: string, invoice: Invoice) => (
        <div className="flex min-w-0 flex-col gap-0.5">
          <span
            className="block truncate text-sm font-medium text-(--text-primary-dark)"
            title={
              invoice.service.includes("PSY-")
                ? invoice.service.split(" PSY-")[0]
                : invoice.service
            }
          >
            {invoice.service.includes("PSY-")
              ? invoice.service.split(" PSY-")[0]
              : invoice.service}
          </span>
          {invoice.service.includes("PSY-") && (
            <span
              className="block truncate text-xs text-(--text-neutral-400)"
              title={`PSY-${invoice.service.split(" PSY-")[1]}`}
            >
              PSY-{invoice.service.split(" PSY-")[1]}
            </span>
          )}
        </div>
      ),
    },
    { key: "date", label: "Date", width: "7.5rem" },
    {
      key: "amount",
      label: "Amount",
      width: "7.5rem",
      className: "font-medium",
      render: (_value: string, invoice: Invoice) => {
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
      },
    },
    {
      key: "paid",
      label: "Paid",
      width: "6.25rem",
      render: (value: string) => `$${value}`,
    },
    {
      key: "status",
      label: "Status",
      width: "10.625rem",
      render: (_value: string, invoice: Invoice) => {
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
              className={`w-fit max-w-full h-6 px-2.5 text-xs font-medium border rounded-full ${getPortalInvoiceStatusStyles(invoice.status)}`}
              title={getPortalInvoiceStatusLabel(invoice.status)}
            >
              <span className="block min-w-0 truncate">
                {getPortalInvoiceStatusLabel(invoice.status)}
              </span>
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
      },
    },
    {
      key: "actions",
      label: "Actions",
      width: "11.25rem",
      className: "text-right",
      headerClassName: "text-center!",
      render: (_: string, invoice: Invoice) => (
        <div className="flex items-center justify-end gap-2">
          {invoice.status === "paid" ? (
            <ActionDropdown
              trigger={
                <div className="flex gap-2 items-center border border-(--neutral-100) rounded-full px-4 py-2 cursor-pointer min-w-30 justify-center hover:bg-(--bg-primary-50) hover:text-(--text-primary-500) transition-colors duration-300">
                  <Eye size={20} strokeWidth={1.5} />
                  Preview
                </div>
              }
              actions={getPreviewActions(invoice)}
            />
          ) : (
            <Button
              className="h-9 px-4 font-semibold text-sm bg-(--bg-primary-dark) text-white rounded-full cursor-pointer"
              onClick={() => handleRecordPayment?.(invoice)}
            >
              <img
                src="/invoice/receipt.svg"
                alt="receipt"
                className="w-5 h-5"
              />{" "}
              Pay now
            </Button>
          )}
          <ActionDropdown actions={getInvoiceActions(invoice)} />
        </div>
      ),
    },
  ];

  return { columns };
};
