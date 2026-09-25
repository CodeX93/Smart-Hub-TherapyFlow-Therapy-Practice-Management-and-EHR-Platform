import type {
  SuperAdminBillingInvoiceDetail,
  SuperAdminBillingInvoiceItem,
} from "@/store/api/superAdminApi";

export type InvoiceStatus = "Past Due" | "Overdue" | "Paid" | "Open" | "Void" | "Draft";

export interface InvoiceLineItem {
  description: string;
  subtext?: string;
  qty: number;
  unitPrice: string;
  amount: string;
}

export interface PaymentActivityItem {
  type: "error" | "success" | "neutral";
  title: string;
  description: string;
  timestamp: string;
}

export interface InvoiceAdjustmentRow {
  adjustmentId: number;
  type: string;
  amount: string;
  reason: string;
  status: string;
  createdAt: string;
}

export interface InvoiceDisputeRow {
  disputeId: number;
  externalCaseId: string;
  amount: string;
  reason: string;
  status: string;
  openedAt: string;
  resolvedAt: string;
}

export interface InvoiceDetailsData {
  invoiceId: string;
  invoiceNumericId: number | null;
  status: InvoiceStatus;
  rawStatus: string;
  organisation: string;
  billedOn: string;
  amountDue: string;
  amount: string;
  totalPaid: string;
  refundedAmount: string;
  issueDate: string;
  dueDate: string;
  billingPeriod: string;
  billingCycle: string;
  customer: {
    organisation: string;
    plan: string;
    planCode: string;
    billingCycle: string;
    providerInvoiceId: string;
    providerChargeId: string;
    providerPaymentIntentId: string;
  };
  lineItems: InvoiceLineItem[];
  totals: {
    subtotal: string;
    tax: string;
    totalDue: string;
  };
  activity: PaymentActivityItem[];
  adjustments: InvoiceAdjustmentRow[];
  disputes: InvoiceDisputeRow[];
}

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

function formatCurrency(value: number | undefined): string {
  return currencyFormatter.format(Number.isFinite(value) ? Number(value) : 0);
}

function formatDate(value: string | undefined | null): string {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
  }).format(parsed);
}

function formatDateTime(value: string | undefined | null): string {
  if (!value) return "-";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(parsed);
}

function formatBillingPeriod(start?: string | null, end?: string | null): string {
  const startLabel = formatDate(start || undefined);
  const endLabel = formatDate(end || undefined);
  if (startLabel === "-" && endLabel === "-") return "-";
  if (startLabel === "-") return endLabel;
  if (endLabel === "-") return startLabel;
  return `${startLabel} - ${endLabel}`;
}

export function normalizeStatus(status: string | undefined): InvoiceStatus {
  const normalized = (status || "").trim().toLowerCase();
  if (normalized.includes("paid")) return "Paid";
  if (normalized.includes("overdue")) return "Overdue";
  if (normalized.includes("void")) return "Void";
  if (normalized.includes("draft")) return "Draft";
  if (normalized.includes("open") || normalized.includes("pending")) return "Open";
  if (normalized.includes("past")) return "Past Due";
  return "Past Due";
}

export function canSendInvoiceReminder(status: string | undefined): boolean {
  const normalized = (status || "").trim().toUpperCase().replace(/[\s-]+/g, "_");
  return normalized === "PENDING" || normalized === "PAST_DUE";
}

function titleCaseCycle(value: string): string {
  const trimmed = value.trim();
  if (!trimmed || trimmed === "—") return "—";
  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1).toLowerCase();
}

export function mapInvoiceDetailsFromDetail(
  invoice: SuperAdminBillingInvoiceDetail,
): InvoiceDetailsData {
  const organizationName = invoice.organisationName || "-";
  const planName =
    invoice.planName ||
    invoice.planCode ||
    (invoice.subscriptionId ? `Subscription #${invoice.subscriptionId}` : "—");
  const issueDate = formatDate(invoice.createdAt);
  const dueDate = formatDate(invoice.dueDate);
  const billingPeriod = formatBillingPeriod(
    invoice.billingPeriodStart,
    invoice.billingPeriodEnd,
  );
  const amountDue = formatCurrency(invoice.outstandingBalance);
  const totalAmount = formatCurrency(invoice.amount);
  const totalPaid = formatCurrency(invoice.totalPaid);
  const refundedAmount = formatCurrency(invoice.refundedAmount);
  const billingCycle = titleCaseCycle(invoice.billingCycle);

  const activity: PaymentActivityItem[] = [];

  if (invoice.createdAt) {
    activity.push({
      type: "neutral",
      title: "Invoice created",
      description: `Invoice ${invoice.invoiceId} was generated.`,
      timestamp: formatDateTime(invoice.createdAt),
    });
  }

  if (invoice.paidAt) {
    activity.push({
      type: "success",
      title: "Payment received",
      description: `Total paid: ${totalPaid}`,
      timestamp: formatDateTime(invoice.paidAt),
    });
  }

  if ((invoice.refundedAmount ?? 0) > 0) {
    activity.push({
      type: "neutral",
      title: "Refund applied",
      description: `Refunded amount: ${refundedAmount}`,
      timestamp: formatDateTime(invoice.paidAt || invoice.createdAt),
    });
  }

  return {
    invoiceId: String(invoice.invoiceId),
    invoiceNumericId: invoice.invoiceId,
    status: normalizeStatus(invoice.status),
    rawStatus: invoice.status || "",
    organisation: organizationName,
    billedOn: issueDate === "-" ? "-" : `Billed on ${issueDate}`,
    amountDue,
    amount: totalAmount,
    totalPaid,
    refundedAmount,
    issueDate,
    dueDate,
    billingPeriod,
    billingCycle,
    customer: {
      organisation: organizationName,
      plan: planName,
      planCode: invoice.planCode || "—",
      billingCycle,
      providerInvoiceId: invoice.providerInvoiceId || "—",
      providerChargeId: invoice.providerChargeId || "—",
      providerPaymentIntentId: invoice.providerPaymentIntentId || "—",
    },
    lineItems: [
      {
        description: planName,
        subtext: billingPeriod,
        qty: 1,
        unitPrice: totalAmount,
        amount: totalAmount,
      },
    ],
    totals: {
      subtotal: totalAmount,
      tax: "$0.00",
      totalDue: amountDue,
    },
    activity,
    adjustments: invoice.adjustments.map((item) => ({
      adjustmentId: item.adjustmentId,
      type: item.type,
      amount: formatCurrency(item.amount),
      reason: item.reason,
      status: item.status,
      createdAt: formatDateTime(item.createdAt),
    })),
    disputes: invoice.disputes.map((item) => ({
      disputeId: item.disputeId,
      externalCaseId: item.externalCaseId,
      amount: formatCurrency(item.amountUsd),
      reason: item.reason,
      status: item.status,
      openedAt: formatDateTime(item.openedAt),
      resolvedAt: formatDateTime(item.resolvedAt),
    })),
  };
}

export function mapInvoiceDetailsFromBillingInvoice(
  invoice: SuperAdminBillingInvoiceItem,
): InvoiceDetailsData {
  const organizationName = invoice.organisation || "-";
  const planName = invoice.plan || `Subscription #${invoice.subscriptionId ?? "-"}`;
  const issueDate = formatDate(invoice.createdAt);
  const dueDate = formatDate(invoice.dueDate);
  const amountDue = formatCurrency(invoice.currentBalance);
  const totalAmount = formatCurrency(invoice.amount);
  const totalPaid = formatCurrency(invoice.totalPaid);
  const refundedAmount = formatCurrency(invoice.refundedAmount);
  const billingPeriod =
    invoice.period ||
    formatBillingPeriod(invoice.billingPeriodStart, invoice.billingPeriodEnd);

  const activity: PaymentActivityItem[] = [];

  if (invoice.createdAt) {
    activity.push({
      type: "neutral",
      title: "Invoice created",
      description: `Invoice ${invoice.invoiceId} was generated.`,
      timestamp: formatDateTime(invoice.createdAt),
    });
  }

  if (invoice.paidAt) {
    activity.push({
      type: "success",
      title: "Payment received",
      description: `Total paid: ${totalPaid}`,
      timestamp: formatDateTime(invoice.paidAt),
    });
  }

  if ((invoice.refundedAmount ?? 0) > 0) {
    activity.push({
      type: "neutral",
      title: "Refund applied",
      description: `Refunded amount: ${refundedAmount}`,
      timestamp: formatDateTime(invoice.paidAt || invoice.createdAt),
    });
  }

  const numericId =
    invoice.invoiceNumericId ??
    (Number.parseInt(invoice.invoiceId, 10) || null);

  return {
    invoiceId: invoice.invoiceId,
    invoiceNumericId: numericId && Number.isFinite(numericId) ? numericId : null,
    status: normalizeStatus(invoice.status),
    rawStatus: invoice.status || "",
    organisation: organizationName,
    billedOn: issueDate === "-" ? "-" : `Billed on ${issueDate}`,
    amountDue,
    amount: totalAmount,
    totalPaid,
    refundedAmount,
    issueDate,
    dueDate,
    billingPeriod,
    billingCycle: "—",
    customer: {
      organisation: organizationName,
      plan: planName,
      planCode: "—",
      billingCycle: "—",
      providerInvoiceId: "—",
      providerChargeId: "—",
      providerPaymentIntentId: "—",
    },
    lineItems: [
      {
        description: planName,
        subtext: billingPeriod,
        qty: 1,
        unitPrice: totalAmount,
        amount: totalAmount,
      },
    ],
    totals: {
      subtotal: totalAmount,
      tax: "$0.00",
      totalDue: amountDue,
    },
    activity,
    adjustments: [],
    disputes: [],
  };
}

export const INVOICE_DETAILS_MOCK: InvoiceDetailsData = {
  invoiceId: "inv_9012",
  invoiceNumericId: null,
  status: "Past Due",
  rawStatus: "PAST_DUE",
  organisation: "East Ridge Counseling",
  billedOn: "Billed on Jan 15, 2026",
  amountDue: "$980.00",
  amount: "$980.00",
  totalPaid: "$0.00",
  refundedAmount: "$0.00",
  issueDate: "Jan 15, 2026",
  dueDate: "Feb 15, 2026",
  billingPeriod: "Jan 15, 2026 - Feb 14, 2026",
  billingCycle: "Monthly",
  customer: {
    organisation: "East Ridge Counseling",
    plan: "Professional",
    planCode: "PROFESSIONAL",
    billingCycle: "Monthly",
    providerInvoiceId: "—",
    providerChargeId: "—",
    providerPaymentIntentId: "—",
  },
  lineItems: [
    {
      description: "Professional Plan - Base Subscription",
      subtext: "Jan 15, 2026 - Feb 14, 2026",
      qty: 10,
      unitPrice: "$40.00",
      amount: "$400.00",
    },
  ],
  totals: {
    subtotal: "$980.00",
    tax: "$0.00",
    totalDue: "$980.00",
  },
  activity: [],
  adjustments: [],
  disputes: [],
};
