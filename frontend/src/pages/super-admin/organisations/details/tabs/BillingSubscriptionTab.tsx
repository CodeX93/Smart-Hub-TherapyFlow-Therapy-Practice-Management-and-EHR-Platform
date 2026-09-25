import type { OrganisationRow } from "../../organisations.data";
import InvoicesPaymentHistoryCard from "./billing/InvoicesPaymentHistoryCard";
import SubscriptionDetailsCard from "./billing/SubscriptionDetailsCard";
import UsageLimitsBillingCard from "./billing/UsageLimitsBillingCard";

interface BillingSubscriptionTabProps {
  org: OrganisationRow;
  organisationId: number | null;
  onUpdatePlan?: () => void;
}

function BillingSubscriptionTab(props: BillingSubscriptionTabProps) {
  return (
    <div className="flex w-full flex-col gap-4">
      <div className="grid w-full grid-cols-1 items-stretch gap-4 xl:grid-cols-[minmax(0,1.55fr)_minmax(25rem,0.9fr)]">
        <SubscriptionDetailsCard org={props.org} onUpdatePlan={props.onUpdatePlan} />
        <UsageLimitsBillingCard org={props.org} />
      </div>
      <InvoicesPaymentHistoryCard organisationId={props.organisationId} slug={props.org.slug} />
    </div>
  );
}

export default BillingSubscriptionTab;
