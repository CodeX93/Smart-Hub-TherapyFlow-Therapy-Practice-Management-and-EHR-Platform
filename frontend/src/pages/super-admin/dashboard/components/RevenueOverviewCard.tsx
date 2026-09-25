import { LineChart } from "lucide-react";
import RevenueMiniStats from "./RevenueMiniStats";
import RevenuePlanList from "./RevenuePlanList";
import RevenueMovements from "./RevenueMovements";
import type {
  SuperAdminRevenueMiniMetricItem,
  SuperAdminRevenuePlanItem,
} from "../dashboard.types";
import { cn } from "@/lib/utils";

interface RevenueOverviewCardProps {
  miniMetrics: SuperAdminRevenueMiniMetricItem[];
  totalLabel: string;
  plans: SuperAdminRevenuePlanItem[];
  upgradesText: string;
  downgradesText: string;
  className?: string;
}

function RevenueOverviewCard(props: RevenueOverviewCardProps) {
  return (
    <section
      className={cn(
        "flex w-full flex-1 flex-col gap-4 self-stretch rounded-[0.875rem] border border-[#e3ebf3] bg-white px-4 py-3.5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]",
        props.className
      )}
    >
      <div className="flex w-full items-center justify-between">
        <div className="flex items-center gap-2">
          <LineChart size={15} className="text-[#667483]" />
          <h3 className="text-[0.8125rem] font-semibold text-[#1e2934]">
            Revenue Overview
          </h3>
        </div>
        <div className="text-[0.6875rem] text-[#8a96a3]">
          Monthly and annual recurring revenue and movements
        </div>
      </div>

      <div className="w-full">
        <RevenueMiniStats items={props.miniMetrics} />
      </div>

      <div className="w-full">
        <RevenuePlanList totalLabel={props.totalLabel} items={props.plans} />
      </div>

      <div className="w-full">
        <RevenueMovements
          upgradesText={props.upgradesText}
          downgradesText={props.downgradesText}
        />
      </div>
    </section>
  );
}

export default RevenueOverviewCard;
