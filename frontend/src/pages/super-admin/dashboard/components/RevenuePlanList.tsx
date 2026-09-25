import type { SuperAdminRevenuePlanItem } from "../dashboard.types";
import { cn } from "@/lib/utils";

interface RevenuePlanListProps {
  totalLabel: string;
  items: SuperAdminRevenuePlanItem[];
}

function getProgressWidth(percent: string): string {
  const numeric = Number.parseFloat(percent.replace("%", ""));

  if (Number.isNaN(numeric)) {
    return "0%";
  }

  return Math.max(0, Math.min(100, numeric)) + "%";
}

function RevenuePlanList(props: RevenuePlanListProps) {
  return (
    <div className="overflow-hidden rounded-[0.75rem] border border-[#e3ebf3] bg-white">
      <div className="border-b border-[#edf1f5] px-4 py-3.5">
        <div className="text-[0.75rem] font-medium text-[#1f2b36]">
          Revenue by Plan (Last 30 days)
        </div>
      </div>

      <div className="space-y-3 px-4 py-3.5">
        {props.items.map(function (row) {
          return (
            <div key={row.label}>
              <div className="mb-1.5 flex items-center justify-between gap-4">
                <span className="text-[0.6875rem] font-medium text-[#7c8a97]">
                  {row.label}
                </span>
                <span className="text-[0.6875rem] font-medium text-[#465563]">
                  {row.value}
                </span>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-[#edf2f7]">
                <div
                  className={cn("h-full rounded-full", row.dotClassName)}
                  style={{ width: getProgressWidth(row.percent) }}
                  aria-hidden="true"
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default RevenuePlanList;
