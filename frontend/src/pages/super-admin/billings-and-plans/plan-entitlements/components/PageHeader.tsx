import { Button } from "@/components/ui/button";

interface PageHeaderProps {
  planName: string;
  onDiscard: () => void;
  onSave: () => void;
  isSaving: boolean;
}

function PageHeader({ planName, onDiscard, onSave, isSaving }: PageHeaderProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-[#e3ebf3] pb-5">
      <div>
        <h1 className="min-w-0 text-[1.25rem] font-semibold leading-8 text-[#1f2d38]">
          <span className="mr-1">Edit Plan Entitlements:</span>
          <span className="inline-block max-w-full align-bottom truncate" title={planName}>
            {planName}
          </span>
        </h1>
        <p className="mt-1 text-[0.875rem] leading-5 text-[#667483]">
          <span>Configure exactly which modules and limits apply to the </span>
          <span className="inline-block max-w-full align-bottom truncate" title={planName}>
            {planName}
          </span>
          <span> plan.</span>
        </p>
      </div>

      <div className="flex items-center gap-3">
        <Button
          variant="outline"
          onClick={onDiscard}
          disabled={isSaving}
          className="h-10 rounded-full border-[#dce5ee] bg-white px-5 text-[0.8125rem] font-medium text-[#667483]"
        >
          Discard
        </Button>
        <Button
          onClick={onSave}
          disabled={isSaving}
          className="h-10 rounded-full bg-[#435564] px-5 text-[0.8125rem] font-semibold text-white hover:bg-[#394957]"
          loading={isSaving}
          loadingLabel="Saving..."
        >
          Save Changes
        </Button>
      </div>
    </div>
  );
}
export default PageHeader;
