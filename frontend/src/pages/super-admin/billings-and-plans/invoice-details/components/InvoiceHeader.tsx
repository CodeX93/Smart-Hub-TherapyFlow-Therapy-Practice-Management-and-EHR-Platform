import { Button } from "@/components/ui/button";
import { ArrowLeft, Download } from "lucide-react";
import InvoiceStatusBadge from "./InvoiceStatusBadge";
import type { InvoiceDetailsData } from "../invoiceDetails.data";

interface InvoiceHeaderProps {
  invoice: InvoiceDetailsData;
  onSendReminder(): void;
  onDownload(): void;
  onBack(): void;
  isDownloadingPdf?: boolean;
  canSendReminder?: boolean;
}

function InvoiceHeader(props: InvoiceHeaderProps) {
  return (
    <div className="flex w-full items-start justify-between gap-4">
      <div className="flex min-w-0 items-start gap-3">
        <button
          type="button"
          onClick={props.onBack}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-[#5b88a5] transition-colors hover:bg-[#f0f7fb]"
          aria-label="Back to Billing and Plans"
        >
          <ArrowLeft size={18} aria-hidden="true" />
        </button>

        <div className="min-w-0">
          <div className="flex items-center gap-3">
            <h1 className="truncate text-[1.5rem] font-semibold leading-8 text-[#1f2d38]">
              Invoice {props.invoice.invoiceId}
            </h1>
            <InvoiceStatusBadge status={props.invoice.status} />
          </div>

          <div className="mt-1 flex items-center gap-1 text-[0.875rem] font-normal leading-5 text-[#7f8c99]">
            <span>{props.invoice.organisation}</span>
            <span>&bull;</span>
            <span>{props.invoice.billedOn}</span>
          </div>
        </div>
      </div>

      <div className="flex shrink-0 items-center gap-3">
        <Button
          variant="secondary"
          size="md"
          onClick={props.onDownload}
          disabled={props.isDownloadingPdf}
          loading={props.isDownloadingPdf}
          loadingLabel="Downloading..."
        >
          <Download size={16} aria-hidden="true" />
          Download PDF
        </Button>

        <Button
          variant="primary"
          size="md"
          onClick={props.onSendReminder}
          disabled={props.canSendReminder === false}
          title={
            props.canSendReminder === false
              ? "Reminders are only available for pending or past due invoices"
              : undefined
          }
        >
          Send Reminder
        </Button>
      </div>
    </div>
  );
}

export default InvoiceHeader;
