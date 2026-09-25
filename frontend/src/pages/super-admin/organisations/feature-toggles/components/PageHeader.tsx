import { Button } from "@/components/ui/button";
import InfoCallout from "../../components/InfoCallout";

interface PageHeaderProps {
  onDiscard(): void;
  onSave(): void;
  isSaving?: boolean;
}

function PageHeader(props: PageHeaderProps) {
  return (
    <div className="w-full">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
        <div className="min-w-0">
          <h1 className="text-[#1f2d38] text-[1.5rem] font-semibold leading-7">
            Feature Toggles
          </h1>
          <p className="mt-2 max-w-[47.5rem] text-[#667483] text-[0.8125rem] font-normal leading-5">
            Manage and override feature access and usage limits for this specific
            tenant. Overrides take precedence over the base plan (Enterprise)
            entitlements.
          </p>
        </div>

        <div className="flex shrink-0 items-center gap-3">
          <Button
            variant="secondary"
            size="md"
            onClick={props.onDiscard}
            disabled={props.isSaving}
          >
            Discard
          </Button>
          <Button
            variant="primary"
            size="md"
            onClick={props.onSave}
            disabled={props.isSaving}
            loading={props.isSaving}
            loadingLabel="Saving..."
          >
            Save Changes
          </Button>
        </div>
      </div>

      <InfoCallout className="mt-6 px-4 py-3.5">
        <div className="text-[#405261] text-[0.75rem] leading-5">
          <span className="font-semibold">
            Evaluation Order: Plan Entitlements + Purchased Add-ons -&gt; Tenant
            Overrides.
          </span>{" "}
          <span className="font-normal">
            Any settings explicitly overridden here will bypass standard
            limitations regardless of the organization&apos;s plan.
          </span>
        </div>
      </InfoCallout>
    </div>
  );
}

export default PageHeader;
