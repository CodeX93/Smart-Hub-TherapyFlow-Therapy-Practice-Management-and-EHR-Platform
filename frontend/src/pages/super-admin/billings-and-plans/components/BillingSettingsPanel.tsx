import { TrashIcon } from "@/components/icons/commonIcons";
import { useMemo, useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import {
  useGetDunningPolicyQuery,
  useUpdateDunningPolicyMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

interface DunningStep {
  id: string;
  stepNumber: string;
  days: string;
  action: DunningActionValue;
}

type DunningActionValue = "email_reminder" | "auto_suspend" | "cancel";

const DUNNING_ACTION_OPTIONS: DunningActionValue[] = [
  "email_reminder",
  "auto_suspend",
  "cancel",
];

function getInitialDunningSteps(): DunningStep[] {
  return [{ id: "step-1", stepNumber: "1", days: "1", action: "email_reminder" }];
}

function mapPolicyToSteps(
  steps: Array<{ day?: number | null; action?: string | null }> | undefined
): DunningStep[] {
  const source = Array.isArray(steps) ? steps : [];
  const mapped = source
    .map(function (step, index) {
      const day = Number(step?.day ?? 0);
      const action = step?.action;
      const safeAction: DunningActionValue = DUNNING_ACTION_OPTIONS.includes(
        action as DunningActionValue
      )
        ? (action as DunningActionValue)
        : "email_reminder";
      return {
        id: `step-${index + 1}`,
        stepNumber: String(index + 1),
        days: String(day > 0 ? day : index + 1),
        action: safeAction,
      };
    })
    .filter((step) => step.days.trim().length > 0);

  return mapped.length > 0 ? mapped : getInitialDunningSteps();
}

function getDayOptions(steps: DunningStep[]): string[] {
  const baseOptions = ["1", "3", "5", "7", "10", "14"];
  const merged = new Set<string>(baseOptions);
  steps.forEach(function (step) {
    if (step.days.trim()) {
      merged.add(step.days.trim());
    }
  });
  return Array.from(merged).sort((a, b) => Number(a) - Number(b));
}

function getTrialDayOptions(values: string[]): string[] {
  const merged = new Set<string>(["3", "5", "7", "10", "14", ...values]);
  return Array.from(merged).sort((a, b) => Number(a) - Number(b));
}

function getSectionCardClassName(): string {
  return cn(
    "overflow-hidden rounded-[1rem] border border-[#e7edf3] bg-white",
    "shadow-[0_1px_2px_rgba(15,23,42,0.04)]"
  );
}

function getInnerPanelClassName(): string {
  return "rounded-[1.25rem] border border-[#edf2f7] bg-[#fbfcfd] px-5 py-5";
}

function getStepBadgeClassName(): string {
  return cn(
    "flex h-10 w-10 items-center justify-center rounded-[0.625rem] border border-[#e3eaf2]",
    "bg-white text-[#1f2d38] text-[1rem] font-medium leading-6 shadow-[0_1px_2px_rgba(15,23,42,0.04)]"
  );
}

function getSelectContentClassName(): string {
  return "rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]";
}

function getDayFieldTriggerClassName(): string {
  return cn(
    "h-[2.25rem] w-full rounded-[0.625rem] border-[#dce5ee] bg-white px-3 shadow-none",
    "focus-visible:border-[#dce5ee] focus-visible:ring-0"
  );
}

function getActionFieldTriggerClassName(): string {
  return cn(
    "h-[2.25rem] w-full justify-between rounded-[0.625rem] border-[#dce5ee] bg-white px-3 shadow-none",
    "text-[0.875rem] font-normal leading-5 text-[#2f3a44] [&_svg]:text-[#97a4b0]",
    "focus-visible:border-[#dce5ee] focus-visible:ring-0"
  );
}

function getFloatingSelectShellClassName(): string {
  return "relative h-[3.5rem] overflow-hidden rounded-[1rem] border border-[#dce5ee] bg-white";
}

function getFloatingSelectTriggerClassName(): string {
  return cn(
    "h-full w-full rounded-[1rem] border-0 bg-transparent px-4 py-3 shadow-none",
    "items-start text-[0.875rem] font-normal text-[#2b3946] [&_svg]:mt-[0.625rem] [&_svg]:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0"
  );
}

function getFloatingSelectContentClassName(): string {
  return "flex min-w-0 flex-1 flex-col items-start justify-start gap-[0.125rem] text-left";
}

function getFloatingSelectLabelClassName(): string {
  return "text-[0.6875rem] font-medium leading-4 text-[#8a96a3]";
}

function getSelectOptions(values: string[]) {
  return values.map(function (value) {
    return (
      <SelectItem
        key={value}
        value={value}
        className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
      >
        {value}
      </SelectItem>
    );
  });
}

function updateStepDays(
  steps: DunningStep[],
  stepId: string,
  nextDays: string
): DunningStep[] {
  return steps.map(function (step) {
    if (step.id === stepId) {
      return {
        ...step,
        days: nextDays,
      };
    }

    return step;
  });
}

function updateStepAction(
  steps: DunningStep[],
  stepId: string,
  nextAction: DunningActionValue
): DunningStep[] {
  return steps.map(function (step) {
    if (step.id === stepId) {
      return {
        ...step,
        action: nextAction,
      };
    }

    return step;
  });
}

function getNextStepNumber(steps: DunningStep[]): string {
  return String(steps.length + 1);
}

function createNextStep(steps: DunningStep[]): DunningStep {
  const nextStepNumber = getNextStepNumber(steps);

  return {
    id: "step-" + nextStepNumber,
    stepNumber: nextStepNumber,
    days: nextStepNumber,
    action: "email_reminder",
  };
}

function DaySelect(props: {
  value: string;
  options: string[];
  onChange(nextValue: string): void;
}) {
  return (
    <Select value={props.value} onValueChange={props.onChange}>
      <SelectTrigger className={getDayFieldTriggerClassName()}>
        <div className="flex min-w-0 flex-1 items-center gap-2 text-left">
          <span className="text-[0.875rem] font-normal leading-5 text-[#97a4b0]">
            Days
          </span>
          <SelectValue
            placeholder="03"
            className="text-[0.875rem] font-normal leading-5 text-[#2f3a44]"
          />
        </div>
      </SelectTrigger>
      <SelectContent className={getSelectContentClassName()}>
        {getSelectOptions(props.options)}
      </SelectContent>
    </Select>
  );
}

function ActionSelect(props: {
  value: DunningActionValue;
  onChange(nextValue: DunningActionValue): void;
}) {
  return (
    <Select
      value={props.value}
      onValueChange={function (value) {
        props.onChange(value as DunningActionValue);
      }}
    >
      <SelectTrigger className={getActionFieldTriggerClassName()}>
        <SelectValue
          placeholder="Action"
          className="text-[0.875rem] font-normal leading-5 text-[#2f3a44]"
        />
      </SelectTrigger>
      <SelectContent className={getSelectContentClassName()}>
        {getSelectOptions(DUNNING_ACTION_OPTIONS)}
      </SelectContent>
    </Select>
  );
}

function FloatingSelectField(props: {
  label: string;
  value: string;
  options: string[];
  onChange(nextValue: string): void;
}) {
  return (
    <Select value={props.value} onValueChange={props.onChange}>
      <div className={getFloatingSelectShellClassName()}>
        <SelectTrigger className={getFloatingSelectTriggerClassName()}>
          <div className={getFloatingSelectContentClassName()}>
            <span className={getFloatingSelectLabelClassName()}>{props.label}</span>
            <SelectValue
              placeholder={props.value}
              className="text-[0.875rem] font-normal leading-5 text-[#2b3946]"
            />
          </div>
        </SelectTrigger>
      </div>
      <SelectContent className={getSelectContentClassName()}>
        {getSelectOptions(props.options)}
      </SelectContent>
    </Select>
  );
}

function DunningPolicySection(props: {
  steps: DunningStep[];
  dayOptions: string[];
  isLoading: boolean;
  isSaving: boolean;
  onChangeStepDays(stepId: string, nextDays: string): void;
  onChangeStepAction(stepId: string, nextAction: DunningActionValue): void;
  onAddStep(): void;
  onRemoveStep(stepId: string): void;
  onSave(): void;
}) {
  const [isEnabled, setIsEnabled] = useState(true);

  return (
    <section className={getSectionCardClassName()}>
      <div className="px-7 py-7">
        <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
          Automated dunning policy
        </div>
        <div className="mt-2 text-[0.875rem] font-normal leading-5 text-[#7f8b98]">
          Configure automated steps for handling past-due invoices.
        </div>
      </div>

      <div className="border-t border-[#edf2f7] px-7 py-5">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
              Enabled Automated Dunning
            </div>
            <div className="mt-2 text-[0.875rem] font-normal leading-5 text-[#a0acb8]">
              Automatically execute steps when an invoice becomes past due.
            </div>
          </div>

          <Switch
            checked={isEnabled}
            onCheckedChange={setIsEnabled}
            className="mt-1 h-5 w-8"
            onClassName="bg-[#445461]"
            offClassName="bg-[#dfe5ec]"
          />
        </div>

        <div className="mt-4 text-[1rem] font-semibold leading-6 text-[#1f2d38]">
          Dunning Steps Timeline
        </div>

        <div className={cn("mt-3", getInnerPanelClassName())}>
          {props.isLoading ? (
            <div className="mb-3 text-[0.8125rem] font-medium text-[#7f8b98]">
              Loading dunning policy...
            </div>
          ) : null}
          <div className="flex flex-col gap-3">
            {props.steps.map(function (step) {
              return (
                <div
                  key={step.id}
                  className="grid grid-cols-[3rem_8rem_minmax(0,1fr)_2.5rem] items-center gap-3"
                >
                  <div className={getStepBadgeClassName()}>{step.stepNumber}</div>

                  <DaySelect
                    value={step.days}
                    options={props.dayOptions}
                    onChange={function (value) {
                      props.onChangeStepDays(step.id, value);
                    }}
                  />

                  <ActionSelect
                    value={step.action}
                    onChange={function (value) {
                      props.onChangeStepAction(step.id, value);
                    }}
                  />

                  <button
                    type="button"
                    onClick={() => props.onRemoveStep(step.id)}
                    disabled={props.steps.length === 1}
                    className="inline-flex h-9 w-9 items-center justify-center rounded-[0.625rem] border border-[#dce5ee] text-[#8a96a3] transition-colors hover:bg-[#f6f9fc] hover:text-[#5b6977] disabled:cursor-not-allowed disabled:opacity-50"
                    aria-label={`Delete step ${step.stepNumber}`}
                  >
                    <TrashIcon size={15} aria-hidden="true" />
                  </button>
                </div>
              );
            })}
          </div>

          <button
            type="button"
            className="mt-5 inline-flex items-center gap-2 text-[0.875rem] font-medium leading-5 text-[#5b87a4] hover:opacity-90"
            onClick={props.onAddStep}
          >
            <Plus size={16} aria-hidden="true" />
            Add another Step
          </button>

          <div className="mt-4 flex justify-end">
            <Button variant="primary" size="md" onClick={props.onSave} disabled={props.isSaving}
              loading={props.isSaving}
              loadingLabel="Saving..."
            >
              Save Policy
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}

function GeneralSettingsSection(props: {
  gracePeriodDays: string;
  trialNoticeDays: string;
  trialOptions: string[];
  isSaving: boolean;
  onChangeGracePeriodDays(next: string): void;
  onChangeTrialNoticeDays(next: string): void;
  onSave(): void;
}) {
  return (
    <section className={getSectionCardClassName()}>
      <div className="px-7 py-7">
        <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
          General Setting
        </div>
        <div className="mt-2 text-[0.875rem] font-normal leading-5 text-[#7f8b98]">
          Global Configuration for invoicing and trials
        </div>
      </div>

      <div className="border-t border-[#edf2f7] px-7 py-5">
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
          <div>
            <FloatingSelectField
              label="Default Trial Case period (Days)"
              value={props.gracePeriodDays}
              options={props.trialOptions}
              onChange={props.onChangeGracePeriodDays}
            />
          </div>

          <div>
            <FloatingSelectField
              label="Trial Expiry Notice lead time ( Days)"
              value={props.trialNoticeDays}
              options={props.trialOptions}
              onChange={props.onChangeTrialNoticeDays}
            />
          </div>
        </div>

        <div className="mt-6 flex justify-end">
          <Button variant="primary" size="md" onClick={props.onSave} disabled={props.isSaving}
            loading={props.isSaving}
            loadingLabel="Saving..."
          >
            Save Settings
          </Button>
        </div>
      </div>
    </section>
  );
}

function BillingSettingsPanel() {
  const [steps, setSteps] = useState<DunningStep[]>(getInitialDunningSteps);
  const [gracePeriodDays, setGracePeriodDays] = useState("3");
  const [trialNoticeDays, setTrialNoticeDays] = useState("7");
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState<string | null>(null);
  const { data, isLoading } = useGetDunningPolicyQuery();
  const [updateDunningPolicy, { isLoading: isSaving }] = useUpdateDunningPolicyMutation();

  const dayOptions = useMemo(() => getDayOptions(steps), [steps]);
  const trialOptions = useMemo(
    () => getTrialDayOptions([gracePeriodDays, trialNoticeDays]),
    [gracePeriodDays, trialNoticeDays]
  );

  const draftKey = "policy";
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (data && seededDraftKey !== draftKey) {
    setSeededDraftKey(draftKey);
    setSteps(mapPolicyToSteps(data.steps));
    setGracePeriodDays(String(data.gracePeriodDays || 0));
    setTrialNoticeDays(String(data.trialNoticeDays || 0));
  }

  function handleChangeStepDays(stepId: string, nextDays: string) {
    setSteps(function (previousSteps) {
      return updateStepDays(previousSteps, stepId, nextDays);
    });
  }

  function handleChangeStepAction(stepId: string, nextAction: DunningActionValue) {
    setSteps(function (previousSteps) {
      return updateStepAction(previousSteps, stepId, nextAction);
    });
  }

  function handleAddStep() {
    setSteps(function (previousSteps) {
      return previousSteps.concat(createNextStep(previousSteps));
    });
  }

  function handleRemoveStep(stepId: string) {
    setSteps(function (previousSteps) {
      if (previousSteps.length <= 1) return previousSteps;
      const filtered = previousSteps.filter((step) => step.id !== stepId);
      return filtered.map((step, index) => ({
        ...step,
        id: `step-${index + 1}`,
        stepNumber: String(index + 1),
      }));
    });
  }

  async function handleSave() {
    const payload = {
      steps: steps.map(function (step, index) {
        const parsedDay = Number.parseInt(step.days || "0", 10);
        return {
          day: Number.isFinite(parsedDay) && parsedDay >= 0 ? parsedDay : index + 1,
          action: step.action,
        };
      }),
      gracePeriodDays:
        Number.parseInt(gracePeriodDays || "0", 10) >= 0
          ? Number.parseInt(gracePeriodDays || "0", 10)
          : 0,
      trialNoticeDays:
        Number.parseInt(trialNoticeDays || "0", 10) >= 0
          ? Number.parseInt(trialNoticeDays || "0", 10)
          : 0,
    };

    try {
      setSaveError(null);
      setSaveSuccess(null);
      await updateDunningPolicy(payload).unwrap();
      setSaveSuccess("Dunning policy updated successfully.");
    } catch (error) {
      setSaveError(getApiErrorMessage(error));
    }
  }

  return (
    <div className="flex w-full flex-col gap-4">
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
      <DunningPolicySection
        steps={steps}
        dayOptions={dayOptions}
        isLoading={isLoading}
        isSaving={isSaving}
        onChangeStepDays={handleChangeStepDays}
        onChangeStepAction={handleChangeStepAction}
        onAddStep={handleAddStep}
        onRemoveStep={handleRemoveStep}
        onSave={handleSave}
      />
      <GeneralSettingsSection
        gracePeriodDays={gracePeriodDays}
        trialNoticeDays={trialNoticeDays}
        trialOptions={trialOptions}
        isSaving={isSaving}
        onChangeGracePeriodDays={setGracePeriodDays}
        onChangeTrialNoticeDays={setTrialNoticeDays}
        onSave={handleSave}
      />
    </div>
  );
}

export default BillingSettingsPanel;
