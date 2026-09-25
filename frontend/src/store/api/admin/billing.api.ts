import { baseApi } from "../baseApi";
import type {
  BillingRecordsQueryArgs,
  BillingRefundsResponse,
  BillingTransaction,
  ClientBillingStatsResponse,
  CreateSessionBillingRequest,
  PaymentGuidanceResponse,
  PaySubscriptionInvoiceResponse,
  RecordSplitPaymentRequest,
  RefundPaymentRequest,
  SessionBillingResponse,
  SpringPageResponse,
  TenantSubscriptionCheckoutResponse,
  TenantSubscriptionInvoice,
  TenantSubscriptionPortalResponse,
  TenantSubscriptionResponse,
  VoidTransactionRequest,
} from "@/types/sessionBilling.type";

export interface BillingStatistics {
  outstandingBalance: number;
  creditBalance?: number;
  totalCollected: number;
  activeClients: number;
  totalBillingRecords: number;
  pendingRecords: number;
  paidRecords: number;
  partialRecords?: number;
  deniedRecords: number;
  followUpRecords: number;
}

export interface BillingHistoryRecord {
  billingId: number;
  clientId: number;
  clientName: string;
  sessionId: number;
  sessionDate: string;
  serviceCode: string;
  serviceName: string;
  totalAmount: number;
  originalSubtotalAmount?: number | null;
  discountAmount: number;
  amountDue: number;
  remainingDue?: number;
  paymentStatus: string;
  billingStatus: string;
  paymentMethod: string;
  paymentAmount: number;
  paymentDate: string;
  billingDate: string;
  invoicePolicyId?: number | null;
  createdAt: string;
}

export interface BillingHistoryQueryArgs {
  clientId?: number;
  therapistId?: number;
  paymentStatus?: string;
  billingStatus?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  limit?: number;
}

export interface RecordPaymentRequest {
  billingId: string | number;
  clientId?: number;
  paymentAmount: number;
  paymentMethod: string;
  referenceNumber?: string;
  notes?: string;
  paymentDate?: string;
  paymentSide?: "client" | "insurance";
  expectedPreviousForSource?: number;
}

export interface ApplyDiscountRequest {
  billingId: string | number;
  discountType: string;
  discountValue?: number;
  discountAmount?: number;
}

export interface ChangeBillingStatusRequest {
  billingId: string | number;
  billingStatus: string;
  notes?: string;
}

export type BillingStatisticsQueryArgs = Partial<ReturnType<typeof import("@/utils/billingFilters").buildBillingQueryFilters>>;

export const billingApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getBillingStatistics: builder.query<BillingStatistics, BillingStatisticsQueryArgs | void>({
      query: (params) => ({
        url: "/api/v1/billing/statistics",
        method: "GET",
        params: params || undefined,
      }),
      providesTags: ["BillingStatistics"],
    }),
    getBillingHistory: builder.query<
      { items: BillingHistoryRecord[]; totalPages: number; totalCount: number },
      BillingHistoryQueryArgs
    >({
      query: (params) => ({
        url: "/api/v1/billing/history",
        method: "GET",
        params,
      }),
      providesTags: ["BillingInvoices"],
    }),
    getBillingRecords: builder.query<
      SpringPageResponse<SessionBillingResponse>,
      BillingRecordsQueryArgs
    >({
      query: (params) => ({
        url: "/api/v1/billing/billing",
        method: "GET",
        params,
      }),
      providesTags: ["BillingInvoices"],
    }),
    getSessionBilling: builder.query<SessionBillingResponse, number>({
      query: (sessionId) => ({
        url: `/api/v1/billing/sessions/${sessionId}/billing`,
        method: "GET",
      }),
      providesTags: (_result, _error, sessionId) => [
        { type: "BillingInvoices", id: `session-${sessionId}` },
      ],
    }),
    createSessionBilling: builder.mutation<
      SessionBillingResponse,
      { sessionId: number; body?: CreateSessionBillingRequest }
    >({
      query: ({ sessionId, body }) => ({
        url: `/api/v1/billing/sessions/${sessionId}/billing`,
        method: "POST",
        body: { sessionId, ...body },
      }),
      invalidatesTags: ["BillingInvoices", "BillingStatistics"],
    }),
    getBillingTransactions: builder.query<BillingTransaction[], number>({
      query: (billingId) => ({
        url: `/api/v1/billing/billing/${billingId}/transactions`,
        method: "GET",
      }),
      providesTags: (_result, _error, billingId) => [
        { type: "BillingTransactions", id: billingId },
      ],
    }),
    getPaymentGuidance: builder.query<PaymentGuidanceResponse, number>({
      query: (billingId) => ({
        url: `/api/v1/billing/billing/${billingId}/payment-guidance`,
        method: "GET",
      }),
    }),
    getBillingRefunds: builder.query<BillingRefundsResponse, number>({
      query: (billingId) => ({
        url: `/api/v1/billing/billing/${billingId}/refunds`,
        method: "GET",
      }),
    }),
    getClientBillingStats: builder.query<ClientBillingStatsResponse, number>({
      query: (clientId) => ({
        url: `/api/v1/billing/clients/${clientId}/stats`,
        method: "GET",
      }),
      providesTags: ["BillingStatistics"],
    }),
    recordSplitPayment: builder.mutation<SessionBillingResponse, RecordSplitPaymentRequest>({
      query: ({ billingId, ...body }) => ({
        url: `/api/v1/billing/billing/${billingId}/record-split-payment`,
        method: "POST",
        body,
      }),
      invalidatesTags: ["BillingInvoices", "BillingStatistics"],
    }),
    refundPayment: builder.mutation<SessionBillingResponse, RefundPaymentRequest>({
      query: ({ billingId, ...body }) => ({
        url: `/api/v1/billing/billing/${billingId}/refund-payment`,
        method: "POST",
        body,
      }),
      invalidatesTags: ["BillingInvoices", "BillingStatistics"],
    }),
    voidBillingTransaction: builder.mutation<
      SessionBillingResponse,
      VoidTransactionRequest
    >({
      query: ({ billingId, transactionId, voidReason }) => ({
        url: `/api/v1/billing/billing/${billingId}/transactions/${transactionId}/void`,
        method: "POST",
        body: { voidReason },
      }),
      invalidatesTags: ["BillingInvoices", "BillingStatistics"],
    }),
    getTenantSubscription: builder.query<TenantSubscriptionResponse, void>({
      query: () => ({
        url: "/api/v1/billing/subscription/me",
        method: "GET",
      }),
      providesTags: ["TenantSubscription"],
    }),
    getTenantSubscriptionInvoices: builder.query<
      SpringPageResponse<TenantSubscriptionInvoice>,
      { page?: number; size?: number; status?: string }
    >({
      query: (params) => ({
        url: "/api/v1/billing/subscription/invoices",
        method: "GET",
        params,
      }),
      providesTags: ["TenantSubscription"],
    }),
    payTenantSubscriptionInvoice: builder.mutation<
      PaySubscriptionInvoiceResponse,
      number
    >({
      query: (invoiceId) => ({
        url: `/api/v1/billing/subscription/invoices/${invoiceId}/pay`,
        method: "POST",
      }),
      invalidatesTags: ["TenantSubscription"],
    }),
    startTenantSubscriptionCheckout: builder.mutation<
      TenantSubscriptionCheckoutResponse,
      void
    >({
      query: () => ({
        url: "/api/v1/billing/subscription/checkout",
        method: "POST",
      }),
    }),
    openTenantSubscriptionPortal: builder.mutation<
      TenantSubscriptionPortalResponse,
      void
    >({
      query: () => ({
        url: "/api/v1/billing/subscription/portal",
        method: "POST",
      }),
    }),
    recordPayment: builder.mutation<SessionBillingResponse, RecordPaymentRequest>({
      query: ({ billingId, ...body }) => ({
        url: `/api/v1/billing/billing/${billingId}/record-payment`,
        method: "POST",
        body,
      }),
      invalidatesTags: (_result, _error, { billingId }) => [
        "BillingInvoices",
        "BillingStatistics",
        { type: "BillingTransactions", id: billingId },
      ],
    }),
    sendInvoiceEmail: builder.mutation<void, string | number>({
      query: (billingId) => ({
        url: `/api/v1/billing/billing/${billingId}/send-invoice-email`,
        method: "POST",
      }),
      invalidatesTags: ["BillingInvoices", "BillingStatistics"],
    }),
    downloadInvoice: builder.query<string, string | number>({
      query: (billingId) => ({
        url: `/api/v1/billing/billing/${billingId}/invoice-download`,
        method: "GET",
        responseHandler: "text",
      }),
    }),
    downloadInvoicePdf: builder.query<Blob, string | number>({
      query: (billingId) => ({
        url: `/api/v1/billing/billing/${billingId}/invoice`,
        method: "GET",
        responseHandler: (response) => response.blob(),
      }),
    }),
    previewInvoice: builder.query<{ html: string }, string | number>({
      query: (billingId) => ({
        url: `/api/v1/billing/billing/${billingId}/invoice-preview`,
        method: "GET",
      }),
    }),
    applyDiscount: builder.mutation<SessionBillingResponse, ApplyDiscountRequest>({
      query: ({ billingId, ...body }) => ({
        url: `/api/v1/billing/billing/${billingId}/discount`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: ["BillingInvoices", "BillingStatistics"],
    }),
    changeBillingStatus: builder.mutation<SessionBillingResponse, ChangeBillingStatusRequest>({
      query: ({ billingId, ...body }) => ({
        url: `/api/v1/billing/billing/${billingId}/status`,
        method: "PATCH",
        body,
      }),
      invalidatesTags: ["BillingInvoices", "BillingStatistics"],
    }),
  }),
});

export const {
  useGetBillingStatisticsQuery,
  useGetBillingHistoryQuery,
  useGetBillingRecordsQuery,
  useLazyGetBillingRecordsQuery,
  useGetSessionBillingQuery,
  useLazyGetSessionBillingQuery,
  useCreateSessionBillingMutation,
  useGetBillingTransactionsQuery,
  useLazyGetBillingTransactionsQuery,
  useGetPaymentGuidanceQuery,
  useLazyGetPaymentGuidanceQuery,
  useGetBillingRefundsQuery,
  useGetClientBillingStatsQuery,
  useRecordSplitPaymentMutation,
  useRefundPaymentMutation,
  useVoidBillingTransactionMutation,
  useGetTenantSubscriptionQuery,
  useLazyGetTenantSubscriptionQuery,
  useGetTenantSubscriptionInvoicesQuery,
  usePayTenantSubscriptionInvoiceMutation,
  useStartTenantSubscriptionCheckoutMutation,
  useOpenTenantSubscriptionPortalMutation,
  useRecordPaymentMutation,
  useSendInvoiceEmailMutation,
  useLazyDownloadInvoiceQuery,
  useLazyDownloadInvoicePdfQuery,
  useLazyPreviewInvoiceQuery,
  useApplyDiscountMutation,
  useChangeBillingStatusMutation,
} = billingApi;
