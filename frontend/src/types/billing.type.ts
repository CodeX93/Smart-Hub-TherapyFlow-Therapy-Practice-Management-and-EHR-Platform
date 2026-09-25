import type {
  ChangeBillingStatusFormValues,
  RecordPaymentSubmitValues,
  ApplyDiscountFormValues,
} from "@/schemas/billings.schema";
import type { Invoice } from "./invoice.type";

export interface BillingFilters {
  startDate: Date | null;
  endDate: Date | null;
  paymentStatus: string | null;
  billingStatus: string | null;
  clientId?: number | null;
  therapistId?: number | null;
  serviceCode?: string | null;
  clientType?: string | null;
  sessionType?: string | null;
  paymentMethod?: string | null;
  minAmount?: number | null;
  maxAmount?: number | null;
}

export interface RecordPaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoice: Invoice | null;
  onRecord: (data: RecordPaymentSubmitValues) => void | Promise<void>;
  isLoading?: boolean;
}

export interface ApplyDiscountModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoice: Invoice | null;
  onApply: (data: ApplyDiscountFormValues) => void;
  isLoading?: boolean;
}

export interface PreviewInvoiceModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoice: Invoice | null;
  htmlContent: string | null;
  isLoading?: boolean;
}

export interface ChangeStatusModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoice: Invoice | null;
  initialStatus: string;
  onApply: (data: ChangeBillingStatusFormValues) => void;
  isLoading?: boolean;
}
