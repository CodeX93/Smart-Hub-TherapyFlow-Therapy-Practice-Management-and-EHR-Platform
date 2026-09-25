import type { InvoiceAdjustmentRow } from "../invoiceDetails.data";

interface AdjustmentsCardProps {
  items: InvoiceAdjustmentRow[];
}

function AdjustmentsCard(props: AdjustmentsCardProps) {
  return (
    <div className="w-full overflow-hidden rounded-[1rem] border border-[#e8edf2] bg-white shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <div className="border-b border-[#edf1f5] px-6 py-4">
        <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
          Adjustments
        </div>
      </div>

      {props.items.length === 0 ? (
        <div className="px-6 py-8 text-sm text-[#667483]">No adjustments on this invoice.</div>
      ) : (
        <>
          <div className="hidden grid-cols-[0.8fr_0.8fr_1.4fr_0.8fr_1fr] gap-3 bg-[#f2f5f8] px-6 py-3 text-[0.75rem] font-medium uppercase tracking-[0.03rem] text-[#98a4b3] md:grid">
            <div>Type</div>
            <div>Amount</div>
            <div>Reason</div>
            <div>Status</div>
            <div>Created</div>
          </div>
          <div className="divide-y divide-[#edf1f5]">
            {props.items.map((item) => (
              <div
                key={item.adjustmentId}
                className="grid grid-cols-1 gap-2 px-6 py-4 text-[0.875rem] text-[#2f3a44] md:grid-cols-[0.8fr_0.8fr_1.4fr_0.8fr_1fr] md:gap-3"
              >
                <div>
                  <span className="mr-2 text-[0.6875rem] font-semibold uppercase text-[#98a4b3] md:hidden">
                    Type
                  </span>
                  {item.type}
                </div>
                <div>
                  <span className="mr-2 text-[0.6875rem] font-semibold uppercase text-[#98a4b3] md:hidden">
                    Amount
                  </span>
                  {item.amount}
                </div>
                <div className="min-w-0 break-words">
                  <span className="mr-2 text-[0.6875rem] font-semibold uppercase text-[#98a4b3] md:hidden">
                    Reason
                  </span>
                  {item.reason}
                </div>
                <div>
                  <span className="mr-2 text-[0.6875rem] font-semibold uppercase text-[#98a4b3] md:hidden">
                    Status
                  </span>
                  {item.status}
                </div>
                <div>
                  <span className="mr-2 text-[0.6875rem] font-semibold uppercase text-[#98a4b3] md:hidden">
                    Created
                  </span>
                  {item.createdAt}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

export default AdjustmentsCard;
