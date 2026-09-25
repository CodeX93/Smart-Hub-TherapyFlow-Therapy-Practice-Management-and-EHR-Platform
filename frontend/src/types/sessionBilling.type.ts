export type BillingStatus =
  | "pending"
  | "billed"
  | "paid"
  | "denied"
  | "follow_up"
  | "cancelled";

export type DiscountType = "percentage" | "fixed" | "none";

export type PaymentMethod =
  | "cash"
  | "check"
  | "credit_card"
  | "debit_card"
  | "insurance"
  | "bank_transfer"
  | "online_payment"
  | "credit_balance";

export type InvoicePolicyPriceType = "FIXED" | "PERCENTAGE";

export interface SessionBillingResponse {
  id: number;
  sessionId: number;
  serviceId?: number | null;
  serviceCode?: string | null;
  serviceName?: string | null;
  unitRate: number;
  originalRatePerUnit?: number | null;
  units: number;
  originalSubtotalAmount?: number | null;
  totalAmount: number;
  insuranceCovered: boolean;
  billingStatus: BillingStatus | string;
  billingDate?: string | null;
  copayAmount?: number | null;
  discountType?: DiscountType | string | null;
  discountValue?: number | null;
  discountAmount?: number | null;
  amountDue: number;
  remainingDue: number;
  creditAmount?: number;
  clientPaidAmount?: number;
  insurancePaidAmount?: number;
  invoicePolicyId?: number | null;
  policyName?: string | null;
  stripeCheckoutSessionId?: string | null;
  stripePaymentIntentId?: string | null;
  clientId?: number;
  /** Client MRN / client number (e.g. CL-2026-0001). */
  clientReferenceNumber?: string | null;
  clientMrn?: string | null;
  clientName?: string;
  therapistId?: number;
  therapistName?: string;
  sessionDate?: string;
  /** Session/schedule status key from the linked session (e.g. no-show, completed). */
  sessionStatus?: string | null;
  paymentStatus?: string;
  paymentMethod?: string | null;
  paymentAmount?: number;
  paymentDate?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateSessionBillingRequest {
  sessionId?: number;
  serviceId?: number;
  serviceCode?: string;
  unitRate?: number;
  units?: number;
  insuranceCovered?: boolean;
  billingDate?: string;
  copayAmount?: number | null;
  discountType?: DiscountType | null;
  discountValue?: number | null;
  discountAmount?: number | null;
}

export interface BillingRecordsQueryArgs {
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
  clientId?: number;
  clientSearch?: string;
  therapistId?: number;
  status?: string;
  paymentStatus?: string;
  serviceCode?: string;
  clientType?: string;
  sessionType?: string;
  paymentMethod?: string;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
}

export interface SpringPageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface BillingTransaction {
  id: number;
  provider: string;
  transactionType: string;
  amount: number;
  providerIntentId?: string | null;
  providerChargeId?: string | null;
  providerCustomerId?: string | null;
  providerPaymentMethodId?: string | null;
  status: string;
  failureReason?: string | null;
  voided: boolean;
  voidReason?: string | null;
  voidedAt?: string | null;
  createdAt: string;
}

export interface PaymentGuidanceResponse {
  billingId: number;
  originalSubtotalAmount?: number | null;
  amountAfterPolicy?: number | null;
  amountAfterDiscount: number;
  expectedClientPortion: number;
  expectedInsurancePortion: number;
  clientAlreadyPaid: number;
  insuranceAlreadyPaid: number;
  clientRemaining: number;
  insuranceRemaining: number;
  totalAlreadyPaid: number;
  totalRemainingDue: number;
  overpayDelta: number;
  insuranceCovered: boolean;
}

export interface SplitPaymentLeg {
  amount: number;
  paymentMethod: string;
  paymentDate?: string;
  referenceNumber?: string;
}

export interface RecordSplitPaymentRequest {
  billingId: string | number;
  clientLeg: SplitPaymentLeg;
  insuranceLeg: SplitPaymentLeg;
  notes?: string;
  allowZeroBillOverpayment?: boolean;
  overrideReason?: string | null;
}

export interface RefundPaymentRequest {
  billingId: string | number;
  refundAmount: number;
  referenceNumber?: string;
  paymentMethod?: string;
  notes?: string;
}

export interface BillingRefundRecord {
  id: number;
  sessionBillingId: number;
  amount: number;
  paymentMethod: string;
  paymentSource: string;
  status: string;
  paymentDate?: string | null;
  reference?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
  transactions?: BillingTransaction[];
}

export interface BillingRefundsResponse {
  billingId: number;
  refundCount: number;
  totalRefunded: number;
  refunds: BillingRefundRecord[];
}

export interface VoidTransactionRequest {
  billingId: string | number;
  transactionId: number;
  voidReason: string;
}

export interface ClientBillingStatsResponse {
  clientId: number;
  totalInvoices: number;
  pendingInvoices: number;
  billedInvoices: number;
  paidInvoices: number;
  deniedInvoices: number;
  followUpInvoices: number;
  cancelledInvoices: number;
  totalBilledAmount: number;
  totalDiscountAmount?: number;
  totalPaidAmount: number;
  dueAmount: number;
  creditAmount: number;
}

export type SubscriptionBillingMode = "provider_managed" | "manual_or_unconfigured";

export type SubscriptionNextAction =
  | "none"
  | "subscribe"
  | "pay_invoice"
  | "update_payment_method"
  | "contact_support";

export interface TenantSubscriptionResponse {
  organisationId: number;
  subscriptionId: number;
  planCode: string;
  planName: string;
  planStatus: string;
  subscriptionStatus: string;
  billingCycle: string;
  priceAtTime: number;
  startAt?: string | null;
  endAt?: string | null;
  currentPeriodEnd?: string | null;
  trialEndAt?: string | null;
  trialing: boolean;
  usagePeriod?: string | null;
  billingMode?: SubscriptionBillingMode | null;
  nextAction?: SubscriptionNextAction | null;
  pendingInvoiceId?: number | null;
  accessRestricted?: boolean;
  canSelfServeRenew?: boolean;
  providerBillingConfigured?: boolean;
  features?: Array<{
    featureCode: string;
    featureName: string;
    description?: string;
    enabled: boolean;
  }>;
}

export interface TenantSubscriptionCheckoutResponse {
  checkoutUrl: string;
}

export interface TenantSubscriptionPortalResponse {
  portalUrl: string;
}

export interface TenantSubscriptionInvoice {
  invoiceId: number;
  subscriptionId: number;
  status: string;
  amount: number;
  outstandingBalance: number;
  totalPaid: number;
  refundedAmount: number;
  dueDate?: string | null;
  billingPeriodStart?: string | null;
  billingPeriodEnd?: string | null;
  paidAt?: string | null;
  providerInvoiceId?: string | null;
  createdAt: string;
}

export interface PaySubscriptionInvoiceResponse {
  invoiceId: number;
  providerInvoiceId?: string | null;
  providerStatus?: string | null;
  paymentUrl: string;
}
