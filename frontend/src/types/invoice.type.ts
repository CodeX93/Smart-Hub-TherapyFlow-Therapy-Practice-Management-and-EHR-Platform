export type InvoiceStatus =
  | "paid"
  | "pending"
  | "partial"
  | "billed"
  | "denied"
  | "cancelled"
  | "refunded"
  | "follow_up";

export interface Invoice {
  id: string;
  clientId?: string;
  /** Client MRN / client number for display and search. */
  clientReferenceNumber?: string | null;
  clientMrn?: string;
  client?: string;
  clientType?: string;
  therapist?: string;
  /** Formatted billing date (date-only) for the Billing date column. */
  date: string;
  /** Billing date ISO instant (date-only at UTC midnight). */
  billingDate?: string | null;
  /** Actual session start (ISO instant) for display under the service name / time. */
  sessionDate?: string | null;
  /** Session/schedule status key (e.g. no-show) for badges under the client name. */
  sessionStatus?: string | null;
  service: string;
  amount: string;
  remainingDue?: string;
  amountDue?: string;
  /** Service list price before invoice policy. */
  originalSubtotal?: string;
  /** Amount after invoice policy, before manual discount. */
  afterPolicyAmount?: string;
  paid?: string;
  insurance?: string;
  /** True when the bill is insurance-covered (shows Copay badge on billing table). */
  insuranceCovered?: boolean;
  copay?: string;
  status: InvoiceStatus;
  paidDate?: string;
  paymentMethod?: string;
  invoicePolicyId?: number | null;
  policyName?: string | null;
  sessionId?: number;
  amountTrail?: string | null;
  /** Present when a manual discount has already been applied (one-time). */
  discountType?: string | null;
  discountAmount?: string | null;
}

export interface InvoiceOverviewItem {
  label: string;
  value: string | number;
  subtext?: string;
  icon?: string;
}

export interface InvoiceOverview {
  totalInvoices: number;
  totalBilled: string;
  totalPaid: string;
  items?: InvoiceOverviewItem[];
}

export interface InvoicesPageProps {
  invoices: Invoice[];
  itemsPerPage: number;
}
