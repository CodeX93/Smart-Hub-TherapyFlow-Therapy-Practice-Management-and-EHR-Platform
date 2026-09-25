import type { ReactNode } from "react";
import StatusBadge from "../../../components/StatusBadge";
import type { OrganisationRow, OrganisationStatus } from "../../../organisations.data";
import { getStatusVariant } from "../../details.utils";

interface SubscriptionDetailsCardProps {
  org: OrganisationRow;
  onUpdatePlan?: () => void;
}

interface SubscriptionRowProps {
  label: string;
  value: ReactNode;
}

function SubscriptionRow(props: SubscriptionRowProps) {
  return (
    <div className="flex items-center justify-between gap-6">
      <div className="text-(--text-neutral-400) text-sm font-normal leading-5.5">
        {props.label}
      </div>
      <div className="text-(--text-gray-900) text-sm font-medium leading-5.5 text-right">
        {props.value}
      </div>
    </div>
  );
}

function SubscriptionDetailsCard(props: SubscriptionDetailsCardProps) {
  const subscription = props.org.subscription;
  const rawSubscriptionStatus = subscription.status?.trim().toLowerCase() || "";
  const subscriptionStatus: OrganisationStatus =
    rawSubscriptionStatus.includes("pending")
      ? "Pending Activation"
      : rawSubscriptionStatus.includes("suspend")
        ? "Suspended"
        : rawSubscriptionStatus
          ? "Active"
          : props.org.status;

  return (
    <div className="flex h-full w-full flex-col rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) px-5 py-5 shadow-[0_1px_2px_0px_var(--shadow)]">
      <div className="text-(--text-gray-900) text-[1rem] font-semibold leading-6">
        Subscription Details
      </div>

      <div className="mt-5 flex flex-1 flex-col gap-4">
        <SubscriptionRow
          label="Plan"
          value={
            <div className="flex items-center justify-end gap-2">
              <span>{subscription.plan}</span>
              <button
                type="button"
                className="text-(--text-primary-500) text-sm font-medium leading-5 hover:opacity-90"
                onClick={props.onUpdatePlan}
              >
                Change
              </button>
            </div>
          }
        />
        <SubscriptionRow
          label="Status"
          value={
            <StatusBadge variant={getStatusVariant(subscriptionStatus)}>
              {subscription.status || props.org.status}
            </StatusBadge>
          }
        />
        <SubscriptionRow label="Billing Cycle" value={subscription.billingCycle} />
        <SubscriptionRow
          label="Current Period Ends"
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
        <SubscriptionRow label="Base Price" value={subscription.basePrice} />
      </div>
    </div>
  );
}

export default SubscriptionDetailsCard;
