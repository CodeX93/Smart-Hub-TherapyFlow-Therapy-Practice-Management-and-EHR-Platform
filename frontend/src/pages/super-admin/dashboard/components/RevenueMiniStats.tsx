import { ArrowDownRight, ArrowUpRight } from "lucide-react";
import type { SuperAdminRevenueMiniMetricItem } from "../dashboard.types";
import { cn } from "@/lib/utils";

interface RevenueMiniStatsProps {
  items: SuperAdminRevenueMiniMetricItem[];
}

function RevenueMiniStats(props: RevenueMiniStatsProps) {
  return (
    <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
      {props.items.map(function (item) {
        return (
          <div
            key={item.key}
            className="flex min-h-[4rem] flex-col justify-between rounded-[0.75rem] border border-[#edf2f7] bg-[#f5f8fb] px-3.5 py-3"
          >
            <div className="text-[0.625rem] font-medium text-[#8a96a3]">
              {item.label}
            </div>
            <div
              className={cn(
                "flex items-center gap-1 text-[0.875rem] font-semibold text-[#17212b]",
                item.valueClassName
              )}
            >
              <span>{item.value}</span>
              {item.trendIcon === "down" ? (
                <ArrowDownRight
                  size={14}
                  className="text-(--status-denied)"
                  aria-hidden="true"
                />
              ) : null}
              {item.trendIcon === "up" ? (
                <ArrowUpRight
                  size={14}
                  className="text-(--status-paid)"
                  aria-hidden="true"
                />
              ) : null}
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default RevenueMiniStats;
