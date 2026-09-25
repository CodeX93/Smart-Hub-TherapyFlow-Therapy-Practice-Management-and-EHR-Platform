import type { InvoiceDetailsData } from "../invoiceDetails.data";

interface InvoiceSummaryCardProps {
  invoice: InvoiceDetailsData;
}

interface SummaryItemProps {
  label: string;
  value: string;
  emphasis?: boolean;
}

function SummaryItem(props: SummaryItemProps) {
  if (props.emphasis) {
    return (
      <div>
        <div className="text-[0.875rem] font-normal leading-5 text-[#a0acb8]">
          {props.label}
        </div>
        <div className="mt-1 text-[1rem] font-semibold leading-7 text-[#ef4444]">
          {props.value}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="text-[0.875rem] font-normal leading-5 text-[#a0acb8]">
        {props.label}
      </div>
      <div className="mt-1 text-[0.875rem] font-medium leading-6 text-[#2f3a44]">
        {props.value}
      </div>
    </div>
  );
}

function InvoiceSummaryCard(props: InvoiceSummaryCardProps) {
  return (
    <div className="w-full rounded-[1rem] border border-[#e6edf3] bg-white px-6 py-6 shadow-[0_2px_2px_0_var(--shadow)]">
      <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
        Invoice Summary
      </div>

      <div className="mt-6 grid grid-cols-2 gap-x-14 gap-y-6">
        <SummaryItem label="Amount Due" value={props.invoice.amountDue} emphasis />
        <SummaryItem label="Invoice Amount" value={props.invoice.amount} />
        <SummaryItem label="Total Paid" value={props.invoice.totalPaid} />
        <SummaryItem label="Refunded" value={props.invoice.refundedAmount} />
        <SummaryItem label="Issue Date" value={props.invoice.issueDate} />
        <SummaryItem label="Due Date" value={props.invoice.dueDate} />
        <SummaryItem label="Billing Period" value={props.invoice.billingPeriod} />
        <SummaryItem label="Billing Cycle" value={props.invoice.billingCycle} />
      </div>
    </div>
  );
}

export default InvoiceSummaryCard;
