import { Info } from "lucide-react";

const GlobalScopeCallout = () => {
  return (
    <div className="w-full rounded-[1rem] border border-[#e3f2ef] bg-[#eef8f6] px-4 py-4 md:px-5">
      <div className="flex items-start gap-3">
        <div className="mt-0.5 text-[#6f8e8a]">
          <Info size={15} aria-hidden="true" />
        </div>
        <div>
          <div className="text-[0.875rem] font-semibold leading-5 text-[#3a4e5d]">
            Global Feature Scope
          </div>
          <div style={{ lineHeight: '1.6' }} className="mt-0.5 max-w-[97.5rem] text-[0.875rem] font-normal leading-5 text-[#72808d]">
            Toggling the default state here affects all organizations unless
            explicitly overridden by a plan entitlement or a tenant-level rollout.
            Ensure features are stable before enabling globally.
          </div>
        </div>
      </div>
    </div>
  );
};

export default GlobalScopeCallout;
