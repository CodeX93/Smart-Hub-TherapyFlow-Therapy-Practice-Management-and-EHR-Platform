import { TrashIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import {
  type DunningPolicy,
  useGetDunningPolicyQuery,
  useUpdateDunningPolicyMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

function getEmptyPolicy(): DunningPolicy {
  return {
    steps: [{ day: 1, action: "email_reminder" }],
    gracePeriodDays: 0,
    trialNoticeDays: 0,
  };
}

function DunningPolicyPage() {
  const [policy, setPolicy] = useState<DunningPolicy>(getEmptyPolicy);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState<string | null>(null);
  const { data, isLoading, isError, error, refetch } = useGetDunningPolicyQuery();
  const [updatePolicy, { isLoading: isSaving }] = useUpdateDunningPolicyMutation();

  const draftKey = "policy";
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (data && seededDraftKey !== draftKey) {
    setSeededDraftKey(draftKey);
    setPolicy({
      steps: data.steps?.length ? data.steps : [{ day: 1, action: "email_reminder" }],
      gracePeriodDays: data.gracePeriodDays ?? 0,
      trialNoticeDays: data.trialNoticeDays ?? 0,
    });
  }

  function setStep(index: number, next: { day?: number; action?: string }) {
    setPolicy((previous) => ({
      ...previous,
      steps: previous.steps.map((step, i) =>
        i === index ? { ...step, ...next } : step
      ),
    }));
  }

  function handleAddStep() {
    setPolicy((previous) => ({
      ...previous,
      steps: [...previous.steps, { day: previous.steps.length + 1, action: "email_reminder" }],
    }));
  }

  function handleRemoveStep(index: number) {
    setPolicy((previous) => ({
      ...previous,
      steps: previous.steps.filter((_, i) => i !== index),
    }));
  }

  async function handleSave() {
    try {
      setSaveError(null);
      setSaveSuccess(null);
      await updatePolicy(policy).unwrap();
      setSaveSuccess("Dunning policy updated successfully.");
      await refetch();
    } catch (err) {
      setSaveError(getApiErrorMessage(err));
    }
  }

  return (
    <SuperAdminPageShell
      title="Dunning Policy"
      description="Configure global billing recovery reminders and actions."
    >
      <section className="rounded-[1rem] border border-[#e3ebf3] bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        {isLoading ? (
          <div className="rounded-[0.75rem] border border-[#e3ebf3] bg-white px-4 py-3 text-sm text-[#667483]">
            Loading dunning policy...
          </div>
        ) : null}
        {isError ? (
          <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
            {getApiErrorMessage(error)}
          </div>
        ) : null}
        {saveError ? (
          <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
            {saveError}
          </div>
        ) : null}
        {saveSuccess ? (
          <div className="rounded-[0.75rem] border border-[#d8ead7] bg-[#f4fbf3] px-4 py-3 text-sm text-[#166534]">
            {saveSuccess}
          </div>
        ) : null}

        <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-[0.75rem] font-medium text-[#667483]">
              Grace Period Days
            </label>
            <Input
              type="number"
              min={0}
              value={policy.gracePeriodDays}
              onChange={(e) =>
                setPolicy((previous) => ({
                  ...previous,
                  gracePeriodDays: Number.parseInt(e.target.value || "0", 10) || 0,
                }))
              }
            />
          </div>
          <div>
            <label className="mb-1 block text-[0.75rem] font-medium text-[#667483]">
              Trial Notice Days
            </label>
            <Input
              type="number"
              min={0}
              value={policy.trialNoticeDays}
              onChange={(e) =>
                setPolicy((previous) => ({
                  ...previous,
                  trialNoticeDays: Number.parseInt(e.target.value || "0", 10) || 0,
                }))
              }
            />
          </div>
        </div>

        <div className="mt-5">
          <div className="mb-2 text-[0.8125rem] font-semibold text-[#1f2d38]">Dunning Steps</div>
          <div className="space-y-3">
            {policy.steps.map((step, index) => (
              <div
                key={`${index}-${step.day}-${step.action}`}
                className="grid grid-cols-[6.875rem_minmax(0,1fr)_2.75rem] items-end gap-2"
              >
                <div>
                  <label className="mb-1 block text-[0.75rem] font-medium text-[#667483]">
                    Day
                  </label>
                  <Input
                    type="number"
                    min={0}
                    value={step.day}
                    onChange={(e) =>
                      setStep(index, {
                        day: Number.parseInt(e.target.value || "0", 10) || 0,
                      })
                    }
                  />
                </div>
                <div>
                  <label className="mb-1 block text-[0.75rem] font-medium text-[#667483]">
                    Action
                  </label>
                  <Input
                    value={step.action}
                    onChange={(e) => setStep(index, { action: e.target.value })}
                    placeholder="email_reminder"
                  />
                </div>
                <Button
                  type="button"
                  variant="destructiveOutline"
                  size="icon"
                  onClick={() => handleRemoveStep(index)}
                  disabled={policy.steps.length === 1}
                  aria-label={`Remove dunning step ${index + 1}`}
                >
                  <TrashIcon size={16} aria-hidden="true" />
                </Button>
              </div>
            ))}
          </div>

          <Button
            type="button"
            variant="secondary"
            size="md"
            className="mt-3"
            onClick={handleAddStep}
          >
            <Plus size={16} className="mr-1" />
            Add Step
          </Button>
        </div>

        <div className="mt-6">
          <Button
            variant="primary"
            size="md"
            onClick={handleSave}
            disabled={isSaving}
            loading={isSaving}
            loadingLabel="Saving..."
          >
            Save Policy
          </Button>
        </div>
      </section>
    </SuperAdminPageShell>
  );
}

export default DunningPolicyPage;
