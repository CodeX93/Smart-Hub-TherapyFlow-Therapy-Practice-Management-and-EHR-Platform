import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import RolloutCard from "../components/RolloutCard";

function FeatureFlagsRollout() {
  return (
    <SuperAdminPageShell
      title="Targeted Feature Rollout"
      description="Configure a targeted rollout strategy for specific organizations or therapists to test or selectively enable capabilities."
      className="gap-6"
    >
      <div className="flex w-full justify-center">
        <RolloutCard className="max-w-[45.8125rem]" />
      </div>
    </SuperAdminPageShell>
  );
}

export default FeatureFlagsRollout;
