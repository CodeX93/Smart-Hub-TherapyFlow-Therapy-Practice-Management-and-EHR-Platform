import type { InvoiceDetailsData, InvoiceLineItem } from "../invoiceDetails.data";

interface LineItemsCardProps {
  invoice: InvoiceDetailsData;
}

function getHeaderItems(): string[] {
  return ["Description", "Qty", "Unit Price", "Amount"];
}

function LineItemRow(props: { item: InvoiceLineItem; withBorder: boolean }) {
  return (
    <div
      className={
        "grid grid-cols-[minmax(0,2.8fr)_5.75rem_10rem_10rem] items-start gap-4 px-6 py-[1.0625rem]" +
        (props.withBorder ? " border-t border-[#edf1f5]" : "")
      }
    >
      <div className="min-w-0">
        <div className="text-[1rem] font-medium leading-6 text-[#2f3a44]">
          {props.item.description}
        </div>
        {props.item.subtext ? (
          <div className="mt-1 text-[0.875rem] font-normal leading-5 text-[#99a3b2]">
            {props.item.subtext}
          </div>
        ) : null}
      </div>
      <div className="pt-0.5 text-center text-[1rem] font-normal leading-6 text-[#2f3a44]">
        {props.item.qty}
      </div>
      <div className="pt-0.5 text-right text-[1rem] font-normal leading-6 text-[#3b4652]">
        {props.item.unitPrice}
      </div>
      <div className="pt-0.5 text-right text-[1rem] font-normal leading-6 text-[#2f3a44]">
        {props.item.amount}
      </div>
    </div>
  );
}

function LineItemsCard(props: LineItemsCardProps) {
  return (
    <div className="w-full overflow-hidden rounded-[1rem] border border-[#e8edf2] bg-white shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <div className="bg-[#f2f5f8] px-6 py-3">
        <div className="grid grid-cols-[minmax(0,2.8fr)_5.75rem_10rem_10rem] items-center gap-4">
          {getHeaderItems().map(function (header) {
            const alignmentClassName =
              header === "Qty"
                ? "text-center"
                : header === "Description"
                  ? "text-left"
                  : "text-right";

            return (
              <div
                key={header}
                className={
                  alignmentClassName +
                  " text-[0.75rem] font-medium uppercase leading-5 tracking-[0.03rem] text-[#98a4b3]"
                }
              >
                {header}
              </div>
            );
          })}
        </div>
      </div>

      {props.invoice.lineItems.map(function (item, index) {
        return (
          <LineItemRow
            key={item.description}
            item={item}
            withBorder={index > 0}
          />
        );
      })}

      <div className="border-t border-[#edf1f5]">
        <div className="px-6 py-4">
          <div className="flex items-center justify-between text-[0.9375rem] font-normal leading-6 text-[#95a1af]">
            <span>Subtotal</span>
            <span className="text-[#3b4652]">{props.invoice.totals.subtotal}</span>
          </div>
          <div className="mt-2 flex items-center justify-between text-[0.9375rem] font-normal leading-6 text-[#95a1af]">
            <span>Tax (0%)</span>
            <span className="text-[#3b4652]">{props.invoice.totals.tax}</span>
          </div>
        </div>

        <div className="border-t border-[#edf1f5] px-6 py-3">
          <div className="flex items-center justify-between text-[1.125rem] font-semibold leading-7 text-[#1f2d38]">
            <span>Total Due</span>
            <span>{props.invoice.totals.totalDue}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default LineItemsCard;
