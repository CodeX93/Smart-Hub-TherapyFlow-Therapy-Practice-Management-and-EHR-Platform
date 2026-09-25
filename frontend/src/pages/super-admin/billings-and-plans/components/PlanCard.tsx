import { Button } from "@/components/ui/button";
import StatusBadge from "@/pages/super-admin/organisations/components/StatusBadge";
import { cn } from "@/lib/utils";

interface PlanMetric {
  label: string;
  value: string;
}

interface PlanCardProps {
  title: string;
  statusLabel: string;
  statusVariant: "green" | "gray";
  price: string;
  priceSuffix: string;
  metrics: PlanMetric[];
  icon?: React.ReactNode;
  onEditTiers?: () => void;
  onEditEntitlements?: () => void;
}

function PlanCard(props: PlanCardProps) {
  return (
    <div className="w-full rounded-[1rem] border border-[#e6edf3] bg-white px-5 py-5 shadow-[0_2px_2px_0_var(--shadow)]">
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-[0.75rem] bg-[#f5f8fb] text-[#98a4b3]">
          <div className="flex h-[1.125rem] w-[1.125rem] items-center justify-center">{props.icon}</div>
        </div>

        <div className="min-w-0">
          <div className="truncate text-[1rem] font-semibold leading-6 text-[#1E282E]" title={props.title}>
            {props.title}
          </div>
          <div className="mt-0.5">
            {props.statusVariant === "green" ? (
              <StatusBadge
                variant="green"
                className="h-[1.375rem] rounded-full border-transparent px-2.5 text-[0.75rem] leading-4"
              >
                {props.statusLabel}
              </StatusBadge>
            ) : (
              <div className="inline-flex h-[1.375rem] items-center rounded-full bg-[#f4f8fb] px-2.5 text-[0.75rem] font-medium leading-4 text-[#8da1b0]">
                {props.statusLabel}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="mt-5 flex items-end gap-1">
        <div className="text-[2rem] font-semibold leading-10 text-[#1E282E]">
          {props.price}
        </div>
        <div className="pb-1 text-[0.875rem] font-normal leading-5 text-[#a0acb8]">
          {props.priceSuffix}
        </div>
      </div>

      <div className="mt-4 flex flex-col gap-2.5">
        {props.metrics.map(function (metric) {
          const isSubscriptionMetric = metric.label === "Active Subscriptions";

          return (
            <div key={metric.label} className="flex items-center justify-between gap-4">
              <div className="text-[0.875rem] font-normal leading-6 text-[#a0acb8]">
                {metric.label}
              </div>
              <div
                className={cn(
                  "text-[0.875rem] leading-6 text-[#2f3a44]",
                  isSubscriptionMetric ? "font-semibold" : "font-medium"
                )}
              >
                {metric.value}
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3">
        <Button variant="secondary" size="md" onClick={props.onEditTiers}>
          Edit Tiers
        </Button>
        <Button variant="secondary" size="md" onClick={props.onEditEntitlements}>
          Entitlements
        </Button>
      </div>
    </div>
  );
}

export default PlanCard;
