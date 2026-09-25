import { cn } from "@/lib/utils";
import type { PaymentActivityItem } from "../invoiceDetails.data";

interface PaymentActivityCardProps {
  items: PaymentActivityItem[];
}

interface DotProps {
  type: PaymentActivityItem["type"];
}

interface ActivityRowProps {
  item: PaymentActivityItem;
}

function Dot(props: DotProps) {
  let className = "bg-[#98a4b3]";

  if (props.type === "success") {
    className = "bg-[#2f7d6d]";
  }

  return <span className={cn("mt-1 inline-block h-2 w-2 rounded-full", className)} />;
}

function ActivityRow(props: ActivityRowProps) {
  return (
    <div className="flex items-start justify-between gap-6">
      <div className="flex min-w-0 flex-1 items-start gap-4">
        <div className="flex min-h-[3.25rem] w-3 justify-center">
          <div className="flex flex-col items-center">
            <Dot type={props.item.type} />
            <span className="activity-line mt-1 h-[2.625rem] w-px bg-[#e5eaf0]" />
          </div>
        </div>
        <div className="min-w-0">
          <div className="text-[1rem] font-medium leading-6 text-[#2f3a44]">
            {props.item.title}
          </div>
          <div className="mt-2 text-[0.875rem] font-normal leading-5 text-[#99a3b2]">
            {props.item.description}
          </div>
        </div>
      </div>

      <div className="shrink-0 pt-0.5 text-right text-[0.875rem] font-normal leading-5 text-[#a0acb8]">
        {props.item.timestamp}
      </div>
    </div>
  );
}

function PaymentActivityCard(props: PaymentActivityCardProps) {
  return (
    <div className="w-full rounded-[1rem] border border-[#e8edf2] bg-white px-6 py-6 shadow-[0_1px_2px_rgba(16,24,40,0.04)]">
      <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
        Payment Activity
      </div>

      <div className="mt-6 flex flex-col gap-6">
        {props.items.map(function (item, index) {
          return (
            <div
              key={item.title + index}
              className={
                index === props.items.length - 1 ? "[&_.activity-line]:hidden" : ""
              }
            >
              <ActivityRow item={item} />
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default PaymentActivityCard;
