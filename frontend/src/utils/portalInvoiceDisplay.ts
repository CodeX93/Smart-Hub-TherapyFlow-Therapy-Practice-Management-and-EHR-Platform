import type {
  PortalInvoice,
  PortalInvoiceStats,
  PortalPaymentConfig,
} from "@/store/api/portalApi";
import type { Invoice, InvoiceOverview, InvoiceStatus } from "@/types/invoice.type";
import { formatPaymentMethodLabel } from "@/utils/paymentMethodDisplay";

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(amount);
}

function formatInvoiceDate(value: string | null | undefined): string {
  if (!value) return "-";

  const dateOnlyMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  const parsed = dateOnlyMatch
    ? new Date(
        Number.parseInt(dateOnlyMatch[1], 10),
        Number.parseInt(dateOnlyMatch[2], 10) - 1,
        Number.parseInt(dateOnlyMatch[3], 10),
      )
    : new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleDateString("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  });
}

function formatPaidDate(value: string | null | undefined): string | undefined {
  if (!value) return undefined;
  const formatted = formatInvoiceDate(value);
  return formatted === "-" ? undefined : formatted;
}

export function resolvePortalInvoiceStatus(invoice: PortalInvoice): InvoiceStatus {
  const paymentStatus = (invoice.paymentStatus || "").trim().toLowerCase();

  if (paymentStatus === "paid") return "paid";
  if (paymentStatus === "unpaid") return "pending";
  if (paymentStatus === "partial") return "partial";
  if (paymentStatus === "denied") return "denied";
  if (paymentStatus === "cancelled" || paymentStatus === "canceled") {
    return "cancelled";
  }

  const billingStatus = (invoice.billingStatus || "").trim().toLowerCase();
  if (billingStatus === "paid") return "paid";
  if (billingStatus === "denied") return "denied";
  if (billingStatus === "follow_up" || billingStatus === "follow-up") {
    return "follow_up";
  }
  if (billingStatus === "cancelled" || billingStatus === "canceled") {
    return "cancelled";
  }
  if (billingStatus === "billed" || billingStatus === "pending") {
    return "pending";
  }

  const amountDue =
    invoice.amountDue ??
    Math.max(0, (invoice.totalAmount ?? 0) - (invoice.discountAmount ?? 0));
  const paymentAmount = invoice.paymentAmount ?? 0;

  if (amountDue > 0 && paymentAmount >= amountDue) {
    return "paid";
  }

  if (paymentAmount > 0) {
    return "partial";
  }

  return "pending";
}

export function isPortalInvoiceBalancePayable(invoice: PortalInvoice): boolean {
  const paymentStatus = (invoice.paymentStatus || "").trim().toLowerCase();
  if (
    paymentStatus === "paid" ||
    paymentStatus === "denied" ||
    paymentStatus === "cancelled" ||
    paymentStatus === "canceled"
  ) {
    return false;
  }

  const status = resolvePortalInvoiceStatus(invoice);
  if (status === "paid" || status === "denied" || status === "cancelled") {
    return false;
  }

  const outstandingAmount = invoice.outstandingAmount;
  if (outstandingAmount !== null && outstandingAmount !== undefined) {
    if (outstandingAmount <= 0) return false;
    return status === "pending" || status === "partial";
  }

  const amountDue =
    invoice.amountDue ??
    Math.max(0, (invoice.totalAmount ?? 0) - (invoice.discountAmount ?? 0));
  const paymentAmount = invoice.paymentAmount ?? 0;
  if (amountDue > 0 && paymentAmount >= amountDue) {
    return false;
  }

  return status === "pending" || status === "partial";
}

export function canPayPortalInvoice(
  invoice: PortalInvoice,
  paymentConfig?: PortalPaymentConfig | null,
): boolean {
  if (!isPortalInvoiceBalancePayable(invoice)) return false;
  if (paymentConfig && !paymentConfig.onlinePaymentsEnabled) return false;
  return true;
}

export function getPortalPayDisabledReason(
  paymentConfig?: PortalPaymentConfig | null,
): string | null {
  if (!paymentConfig || paymentConfig.onlinePaymentsEnabled) return null;
  return (
    paymentConfig.disabledReason?.trim() ||
    "Online payments are not available for this organisation."
  );
}

export function canDownloadPortalInvoiceReceipt(invoice: PortalInvoice): boolean {
  const paymentStatus = (invoice.paymentStatus || "").trim().toLowerCase();
  if (paymentStatus === "paid" || paymentStatus === "partial") {
    return true;
  }

  const status = resolvePortalInvoiceStatus(invoice);
  return status === "paid" || status === "partial";
}

export function getPortalInvoiceStatusLabel(status: InvoiceStatus): string {
  switch (status) {
    case "paid":
      return "Paid";
    case "partial":
      return "Partial";
    case "denied":
      return "Denied";
    case "cancelled":
      return "Cancelled";
    case "follow_up":
      return "Follow-up";
    case "refunded":
      return "Refunded";
    case "billed":
      return "Billed";
    case "pending":
    default:
      return "Pending";
  }
}

export function getPortalInvoiceStatusStyles(status: InvoiceStatus): string {
  switch (status) {
    case "paid":
      return "bg-[#EBFEF4] text-[#007C54] border-[#EBFEF4]";
    case "partial":
      return "bg-[#EEF4FF] text-[#1D4ED8] border-[#EEF4FF]";
    case "denied":
    case "cancelled":
      return "bg-[#FEF2F2] text-[#EF4444] border-[#FEF2F2]";
    case "follow_up":
      return "bg-[#FFF7ED] text-[#FBAC00] border-[#FFF7ED]";
    case "refunded":
      return "bg-[#EDEEF1] text-(--text-neutral-600) border-[#EDEEF1]";
    case "billed":
    case "pending":
    default:
      return "bg-[#EDEEF1] text-(--text-neutral-600) border-[#EDEEF1]";
  }
}

export function formatInsuranceLabel(insuranceCovered: boolean): string {
  return insuranceCovered ? "Covered" : "Self-pay";
}

export function formatCopayLabel(
  copayAmount: number | null | undefined,
): string {
  if (copayAmount === null || copayAmount === undefined) {
    return "--";
  }

  return formatCurrency(copayAmount);
}

export interface ClientInvoiceRow extends Invoice {
  canPay: boolean;
  canDownloadReceipt: boolean;
  payDisabledReason?: string | null;
  serviceCode?: string;
  billingDateRaw: string;
  amountTrail?: string | null;
}

function resolvePortalAmountDue(invoice: PortalInvoice): number {
  if (invoice.amountDue !== null && invoice.amountDue !== undefined) {
    return invoice.amountDue;
  }
  return Math.max(0, (invoice.totalAmount ?? 0) - (invoice.discountAmount ?? 0));
}

export function mapPortalInvoiceToRow(
  invoice: PortalInvoice,
  paymentConfig?: PortalPaymentConfig | null,
): ClientInvoiceRow {
  const status = resolvePortalInvoiceStatus(invoice);
  const paymentMethod = formatPaymentMethodLabel(invoice.paymentMethod);
  const canPay = canPayPortalInvoice(invoice, paymentConfig);
  const balancePayable = isPortalInvoiceBalancePayable(invoice);
  const amountDue = resolvePortalAmountDue(invoice);
  const paidAmount = invoice.paymentAmount ?? 0;
  const remainingDue =
    invoice.outstandingAmount !== null && invoice.outstandingAmount !== undefined
      ? Math.max(0, invoice.outstandingAmount)
      : Math.max(0, amountDue - paidAmount);

  return {
    id: String(invoice.id),
    date: formatInvoiceDate(invoice.billingDate || invoice.sessionDate),
    service: invoice.serviceName || "Session",
    amount: formatCurrency(remainingDue),
    remainingDue: formatCurrency(remainingDue),
    amountDue: formatCurrency(amountDue),
    originalSubtotal:
      invoice.originalSubtotalAmount !== null &&
      invoice.originalSubtotalAmount !== undefined
        ? formatCurrency(invoice.originalSubtotalAmount)
        : undefined,
    amountTrail: null,
    paid: formatCurrency(paidAmount),
    insurance: formatInsuranceLabel(Boolean(invoice.insuranceCovered)),
    status,
    paidDate: formatPaidDate(invoice.paymentDate),
    paymentMethod,
    canPay,
    canDownloadReceipt: canDownloadPortalInvoiceReceipt(invoice),
    payDisabledReason:
      balancePayable && !canPay ? getPortalPayDisabledReason(paymentConfig) : null,
    serviceCode: invoice.serviceCode || undefined,
    billingDateRaw: invoice.billingDate || invoice.sessionDate || "",
  };
}

export function mapPortalInvoiceStatsToOverview(
  stats: PortalInvoiceStats,
): InvoiceOverview {
  return {
    totalInvoices: stats.totalInvoices,
    totalBilled: formatCurrency(stats.totalBilled),
    totalPaid: formatCurrency(stats.totalPaid),
  };
}

export function downloadPortalInvoiceReceiptBlob(
  blob: Blob,
  invoiceId: number,
): void {
  const fileUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = fileUrl;
  link.download = `invoice-${invoiceId}-receipt.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(fileUrl);
}
