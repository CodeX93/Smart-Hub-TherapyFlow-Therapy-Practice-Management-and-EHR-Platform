import type { OrganisationRow } from "../../organisations.data";
import GeneralInformationCard from "./overview/GeneralInformationCard";
import RecentInvoicesCard from "./overview/RecentInvoicesCard";
import SubscriptionCard from "./overview/SubscriptionCard";
import UsageLimitsCard from "./overview/UsageLimitsCard";

interface OverviewTabProps {
  org: OrganisationRow;
  organisationId: number | null;
  onViewAllBilling: () => void;
  onManageSubscription(): void;
  onUpdateGeneralInfo(values: {
    organisationName: string;
    supportEmail: string;
    slug: string;
    region: string;
    dataResidency: string;
    timezone: string;
  }): Promise<void>;
  isUpdatingGeneralInfo: boolean;
}

function OverviewTab(props: OverviewTabProps) {
  return (
    <div className="flex w-full flex-col gap-4">
      <div className="grid w-full grid-cols-1 items-stretch gap-4 xl:grid-cols-[minmax(0,1.7fr)_minmax(22.5rem,0.95fr)]">
        <GeneralInformationCard
          org={props.org}
          className="h-full"
          onSave={props.onUpdateGeneralInfo}
          isSaving={props.isUpdatingGeneralInfo}
        />
        <SubscriptionCard
          org={props.org}
          className="h-full"
          onManageSubscription={props.onManageSubscription}
        />
      </div>

      <div className="grid w-full grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1.7fr)_minmax(22.5rem,0.95fr)]">
        <RecentInvoicesCard
          organisationId={props.organisationId}
          slug={props.org.slug}
          onViewAllBilling={props.onViewAllBilling}
        />
        <UsageLimitsCard org={props.org} />
      </div>
    </div>
  );
}

export default OverviewTab;
