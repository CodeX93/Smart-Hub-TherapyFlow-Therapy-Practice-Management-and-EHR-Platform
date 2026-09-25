import SemanticStatusBadge from "@/components/shared/SemanticStatusBadge";
import type { InvoiceStatus } from "../invoiceDetails.data";

interface InvoiceStatusBadgeProps {
  status: InvoiceStatus;
}

function InvoiceStatusBadge(props: InvoiceStatusBadgeProps) {
  return (
    <SemanticStatusBadge
      status={props.status}
      className="inline-flex h-8 items-center rounded-[0.625rem] px-3 text-[0.875rem] font-medium leading-5"
    >
      {props.status}
    </SemanticStatusBadge>
  );
}

export default InvoiceStatusBadge;
