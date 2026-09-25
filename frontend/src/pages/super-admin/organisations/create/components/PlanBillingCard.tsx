import type { UseFormReturn } from "react-hook-form";
import type { CreateOrganisationValues } from "../createOrganisation.schema";
import {
  CREATE_ORG_FIELD_LIMITS,
} from "../createOrganisation.utils";
import SectionCard from "./SectionCard";
import FieldBlock from "./FieldBlock";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import type { BillingPlan } from "@/store/api/superAdminApi";
import { applyPlanCatalogDefaults } from "../../planManage.utils";

const selectTriggerClassName = cn(
  "h-10 w-full rounded-[0.875rem] border-[#dce5ee] bg-white px-4 shadow-none",
  "text-[0.875rem] text-[#2b3946] data-[placeholder]:text-[#97a4b0] [&_svg]:text-[#97a4b0]"
);

const inputClassName = cn(
  "h-10 rounded-[0.875rem] border-[#dce5ee] bg-white px-4 shadow-none",
  "text-[0.875rem] text-[#2b3946] placeholder:text-[#97a4b0]",
  "focus-visible:border-[#dce5ee] focus-visible:ring-0",
  "disabled:pointer-events-auto disabled:cursor-not-allowed"
);

const PlanBillingCard = ({
  form,
  planOptions,
  isPlansLoading,
  plansErrorMessage,
}: {
  form: UseFormReturn<CreateOrganisationValues>;
  planOptions: BillingPlan[];
  isPlansLoading?: boolean;
  plansErrorMessage?: string | null;
}) => {
  const {
    setValue,
    watch,
    register,
    formState: { errors },
  } = form;
  const { ref: trialDaysRef, ...trialDaysField } = register("trialDays");
  const selectedPlanCode = watch("plan");
  const selectedPlan = planOptions.find((plan) => plan.planCode === selectedPlanCode);
  const billingCycleValue = watch("billingCycle");
  const billingCycleOptions = selectedPlan
    ? [
        selectedPlan.billingCycle?.toLowerCase().includes("year")
          ? "Yearly"
          : "Monthly",
      ]
    : [];
  const therapistLimitValue = watch("therapistsOverride") || "";
  const supervisorLimitValue = watch("supervisorsOverride") || "";
  const clientLimitValue = watch("clientsOverride") || "";

  return (
    <SectionCard
      title="Plan & Billing"
      subtitle="Select the initial subscription tier."
      className="min-h-[18.875rem]"
    >
      <div className="flex flex-col gap-4">
        <FieldBlock label="Plan" required error={errors.plan?.message}>
          <Select
            value={watch("plan")}
            onValueChange={function (value) {
              setValue("plan", value as CreateOrganisationValues["plan"], {
                shouldValidate: true,
              });
              const plan = planOptions.find((item) => item.planCode === value);
              if (!plan) return;
              const defaults = applyPlanCatalogDefaults(plan);
              const nextBillingCycle =
                defaults.billingCycle === "annual"
                  ? "Yearly"
                  : ("Monthly" as CreateOrganisationValues["billingCycle"]);
              setValue(
                "billingCycle",
                nextBillingCycle as CreateOrganisationValues["billingCycle"],
                { shouldValidate: true }
              );
              setValue("trialDays", defaults.trialDays, {
                shouldValidate: true,
              });
              setValue("therapistsOverride", plan.therapistLimit != null ? String(plan.therapistLimit) : "", {
                shouldValidate: true,
              });
              setValue("supervisorsOverride", plan.supervisorLimit != null ? String(plan.supervisorLimit) : "", {
                shouldValidate: true,
              });
              setValue("clientsOverride", plan.clientLimit != null ? String(plan.clientLimit) : "", {
                shouldValidate: true,
              });
            }}
          >
            <SelectTrigger className={selectTriggerClassName}>
              <SelectValue placeholder="Choose your plan" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {isPlansLoading ? (
                <SelectItem value="loading" className="text-[#2b3946]">
                  Loading plans...
                </SelectItem>
              ) : null}
              {plansErrorMessage ? (
                <SelectItem value="error" className="text-[#b42318]">
                  {plansErrorMessage}
                </SelectItem>
              ) : null}
              {planOptions.map(function (plan) {
                return (
                  <SelectItem
                    key={plan.planCode}
                    value={plan.planCode}
                    className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                  >
                    {plan.planName}
                  </SelectItem>
                );
              })}
            </SelectContent>
          </Select>
        </FieldBlock>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <FieldBlock
            label="Billing Cycle"
            required
            error={errors.billingCycle?.message}
          >
            <Select
              value={billingCycleValue || undefined}
              disabled
            >
              <SelectTrigger className={selectTriggerClassName}>
                <SelectValue placeholder="Select plan first" />
              </SelectTrigger>
              <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
                {billingCycleOptions.map(function (value) {
                  return (
                    <SelectItem
                      key={value}
                      value={value}
                      className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                    >
                      {value}
                    </SelectItem>
                  );
                })}
              </SelectContent>
            </Select>
          </FieldBlock>

          <FieldBlock
            label="Trial Days"
            required
            error={errors.trialDays?.message}
          >
            <Input
              className={cn(inputClassName, selectedPlan ? "bg-[#f8fafc]" : undefined)}
              placeholder="Select plan first"
              inputMode="numeric"
              disabled={!selectedPlan}
              readOnly={Boolean(selectedPlan)}
              maxLength={3}
              name={trialDaysField.name}
              ref={trialDaysRef}
              onBlur={trialDaysField.onBlur}
              value={watch("trialDays")}
            />
            {!errors.trialDays?.message ? (
              <p className="mt-1.5 text-[#a0acb8] text-[0.6875rem] font-normal leading-4">
                {selectedPlan
                  ? "Set from the selected plan catalog entry."
                  : `Digits only, 0–${CREATE_ORG_FIELD_LIMITS.trialDaysMax}`}
              </p>
            ) : null}
          </FieldBlock>
        </div>

        <div>
          <div className="text-[#1f2d38] text-[0.8125rem] font-medium leading-5">
            User Limits
          </div>

          <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2">
            <FieldBlock label="Therapists" error={errors.therapistsOverride?.message}>
              <Input
                className={inputClassName}
                disabled
                placeholder={selectedPlan ? "Plan default limit" : "Select a plan first"}
                value={therapistLimitValue}
                readOnly
              />
            </FieldBlock>
            <FieldBlock label="Supervisors" error={errors.supervisorsOverride?.message}>
              <Input
                className={inputClassName}
                disabled
                placeholder={selectedPlan ? "Plan default limit" : "Select a plan first"}
                value={supervisorLimitValue}
                readOnly
              />
            </FieldBlock>
            <FieldBlock
              label="Clients"
              error={errors.clientsOverride?.message}
              className="md:col-span-2"
            >
              <Input
                className={inputClassName}
                disabled
                placeholder={selectedPlan ? "Plan default limit" : "Select a plan first"}
                value={clientLimitValue}
                readOnly
              />
            </FieldBlock>
          </div>
        </div>
      </div>
    </SectionCard>
  );
};

export default PlanBillingCard;
