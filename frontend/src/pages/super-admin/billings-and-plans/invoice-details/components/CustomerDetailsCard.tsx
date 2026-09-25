import type { InvoiceDetailsData } from "../invoiceDetails.data";

interface CustomerDetailsCardProps {
  invoice: InvoiceDetailsData;
  onDownloadPdf(): void;
  isDownloadingPdf?: boolean;
}

interface DetailRowProps {
  label: string;
  value: string;
}

function DetailRow(props: DetailRowProps) {
  return (
    <div className="flex items-start justify-between gap-3">
      <div className="shrink-0 text-[0.875rem] font-normal leading-5 text-[#a0acb8]">
        {props.label}
      </div>
      <div
        className="min-w-0 text-right text-[0.875rem] font-medium leading-6 break-all text-[#2f3a44]"
        title={props.value}
      >
        {props.value}
      </div>
    </div>
  );
}

function CustomerDetailsCard(props: CustomerDetailsCardProps) {
  const customer = props.invoice.customer;

  return (
    <div className="w-full rounded-[1rem] border border-[#e6edf3] bg-white px-5 py-5 shadow-[0_2px_2px_0_var(--shadow)]">
      <div className="flex items-center justify-between gap-3">
        <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
          Customer Details
        </div>
        <button
          type="button"
          className="text-[0.875rem] font-medium leading-5 text-[#5b88a5] hover:opacity-90 disabled:opacity-50"
          onClick={props.onDownloadPdf}
          disabled={props.isDownloadingPdf}
        >
          {props.isDownloadingPdf ? "Downloading..." : "Download PDF"}
        </button>
      </div>

      <div className="mt-5 flex flex-col gap-4">
        <DetailRow label="Organization" value={customer.organisation} />
        <DetailRow label="Plan" value={customer.plan} />
        <DetailRow label="Plan Code" value={customer.planCode} />
        <DetailRow label="Billing Cycle" value={customer.billingCycle} />
        <DetailRow label="Provider Invoice" value={customer.providerInvoiceId} />
        <DetailRow label="Provider Charge" value={customer.providerChargeId} />
        <DetailRow
          label="Payment Intent"
          value={customer.providerPaymentIntentId}
        />
      </div>
    </div>
  );
}

export default CustomerDetailsCard;
