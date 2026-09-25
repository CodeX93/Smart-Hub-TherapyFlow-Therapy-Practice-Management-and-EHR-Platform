import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import StatusBadge from "../../../components/StatusBadge";
import type { OrganisationRow } from "../../../organisations.data";
import { getStatusVariant } from "../../details.utils";

interface SubscriptionCardProps {
  org: OrganisationRow;
  className?: string;
  onManageSubscription?(): void;
}

interface SubscriptionItemProps {
  label: string;
  value: string;
  valueClassName?: string;
}

function SubscriptionItem(props: SubscriptionItemProps) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div className="text-(--text-neutral-400) text-sm font-normal leading-5.5">
        {props.label}
      </div>
      <div
        className={cn(
          "text-(--text-gray-900) text-sm font-medium leading-5.5 text-right",
          props.valueClassName
        )}
      >
        {props.value}
      </div>
    </div>
  );
}

function SubscriptionCard(props: SubscriptionCardProps) {
  const subscription = props.org.subscription;

  return (
    <div
      className={cn(
        "w-full rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) px-5 py-5 shadow-[0_1px_2px_0px_var(--shadow)]",
        "flex h-full flex-col",
        props.className
      )}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="text-(--text-gray-900) text-[1rem] font-semibold leading-6">
          Subscription
        </div>
        <StatusBadge variant={getStatusVariant(props.org.status)}>
          {props.org.status}
        </StatusBadge>
      </div>

      <div className="mt-5 flex flex-1 flex-col gap-5">
        <SubscriptionItem label="Plan" value={subscription.plan} />
        <SubscriptionItem label="Billing Cycle" value={subscription.billingCycle} />
        <SubscriptionItem
          label="Current Period End"
          value={
            subscription.currentPeriodEnd && subscription.currentPeriodEnd !== "-"
              ? new Date(subscription.currentPeriodEnd).toLocaleDateString(undefined, {
                  year: "numeric",
                  month: "short",
                  day: "numeric",
                })
              : "-"
          }
        />
        <SubscriptionItem label="Price" value={subscription.basePrice} />
      </div>

      <Button
        variant="secondary"
        size="md"
        className="mt-6 w-full"
        onClick={props.onManageSubscription}
      >
        Manage Subscription
      </Button>
    </div>
  );
}

export default SubscriptionCard;
