import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import {
  createPlanSchema,
  type CreatePlanValues,
} from "../createPlan.schema";
import {
  useCreateBillingPlanMutation,
  useUpdateBillingPlanMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

interface CreatePlanFormProps {
  mode: "create" | "edit";
  initialValues: CreatePlanValues;
  onCancel(): void;
  onSuccess?(message: string): void;
  isLoadingInitialValues?: boolean;
  initialLoadError?: string | null;
}

function getCardClassName(): string {
  return cn(
    "w-full rounded-[1rem] border border-[#e7edf3] bg-white",
    "px-5 py-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]"
  );
}

function getInputClassName(): string {
  return cn(
    "h-full rounded-[1rem] border-0 bg-transparent px-4 shadow-none",
    "text-[0.875rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0"
  );
}

function getTextareaClassName(): string {
  return cn(
    "min-h-[6.625rem] rounded-[1rem] border-0 bg-transparent px-4 py-3 shadow-none",
    "text-[0.875rem] font-normal leading-5 text-[#2b3946] placeholder:text-[#97a4b0]",
    "resize-none focus-visible:border-0 focus-visible:ring-0"
  );
}

function getSelectTriggerClassName(): string {
  return cn(
    "h-full w-full rounded-[1rem] border-0 bg-transparent px-4 py-3 shadow-none",
    "items-start text-[0.875rem] font-normal text-[#2b3946] [&_svg]:mt-[0.625rem] [&_svg]:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0"
  );
}

function getSelectContentClassName(): string {
  return "rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]";
}

function FieldHint(props: { children: React.ReactNode; error?: boolean }) {
  return (
    <div
      className={cn(
        "mt-1.5 text-[0.6875rem] font-normal leading-4",
        props.error ? "text-[#ef4444]" : "text-[#a0acb8]"
      )}
    >
      {props.children}
    </div>
  );
}

function FieldLabel(props: { children: React.ReactNode }) {
  return (
    <div className="pointer-events-none absolute left-4 top-3 text-[0.6875rem] font-medium leading-4 text-[#8a96a3]">
      {props.children}
    </div>
  );
}

function getFloatingInputPaddingClassName(hasValue: boolean): string {
  return hasValue ? "pb-2 pt-7" : "py-0";
}

function getFloatingTextareaPaddingClassName(hasValue: boolean): string {
  return hasValue ? "pb-3 pt-8" : "py-3";
}

function getFieldShellClassName(hasError?: boolean, isTextarea?: boolean): string {
  return cn(
    "relative overflow-hidden rounded-[1rem] border bg-white",
    isTextarea ? "min-h-[6.625rem]" : "h-[3.75rem]",
    hasError ? "border-[#ef4444]" : "border-[#dce5ee]"
  );
}

function getSelectFieldContentClassName(): string {
  return "flex min-w-0 flex-1 flex-col items-start justify-start gap-[0.125rem] text-left";
}

function getSelectFieldLabelClassName(): string {
  return "text-[0.6875rem] font-medium leading-4 text-[#8a96a3]";
}

function getTitle(mode: CreatePlanFormProps["mode"]): string {
  return mode === "edit" ? "Save Changes" : "Create Plan";
}

function CreatePlanForm(props: CreatePlanFormProps) {
  const [createPlan, { isLoading: isCreating }] = useCreateBillingPlanMutation();
  const [updatePlan, { isLoading: isUpdating }] = useUpdateBillingPlanMutation();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("success");
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<CreatePlanValues>({
    resolver: zodResolver(createPlanSchema),
    defaultValues: props.initialValues,
    mode: "onChange",
    reValidateMode: "onChange",
  });

  useEffect(
    function syncInitialValues() {
      reset(props.initialValues);
    },
    [props.initialValues, reset]
  );

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const submit = handleSubmit(async function (values) {
    try {
      const normalizedCycle = values.billingCycle.trim();
      const parsedBasePrice = Number.parseFloat(values.basePriceUsd);
      const requestBody = {
        name: values.planName.trim(),
        description: values.description?.trim() ?? "",
        billingCycle: normalizedCycle,
        basePrice: normalizedCycle.toLowerCase().includes("year") ? 0 : parsedBasePrice,
        annualPrice: normalizedCycle.toLowerCase().includes("year") ? parsedBasePrice : 0,
        trialDays: Number.parseInt(values.trialDays, 10),
        status: values.status.trim(),
        providerPriceIdMonthly: "",
        providerPriceIdAnnual: "",
      };

      if (props.mode === "edit") {
        await updatePlan({
          planName: values.planCode.trim(),
          body: requestBody,
        }).unwrap();
        props.onSuccess?.("Plan updated successfully.");
      } else {
        await createPlan({
          code: values.planCode.trim(),
          ...requestBody,
        }).unwrap();
        props.onSuccess?.("Plan created successfully.");
      }
    } catch (error) {
      const message = getApiErrorMessage(error);
      setToastType("error");
      setToastMessage(message);
    }
  });
  const planName = watch("planName");
  const planCode = watch("planCode");
  const description = watch("description");
  const basePriceUsd = watch("basePriceUsd");
  const trialDays = watch("trialDays");

  return (
    <form onSubmit={submit} className="w-full">
      {toastMessage ? (
        <div
          className={cn(
            "fixed right-6 top-6 z-[70] rounded-[0.75rem] px-4 py-3 text-sm font-medium shadow-[0_12px_24px_rgba(15,23,42,0.10)]",
            toastType === "success"
              ? "border border-[#d1fae5] bg-[#ecfdf5] text-[#047857]"
              : "border border-[#f3d4d4] bg-[#fff5f5] text-[#b42318]"
          )}
        >
          {toastMessage}
        </div>
      ) : null}
      <div className="flex w-full flex-col gap-4">
        {props.initialLoadError ? (
          <div className="rounded-[0.875rem] border border-[#f7d3d7] bg-[#fff4f5] px-4 py-3 text-[0.8125rem] font-medium text-[#b42318]">
            {props.initialLoadError}
          </div>
        ) : null}
        <section className={getCardClassName()}>
          <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
            Plan Details
          </div>

          <div className="mt-5 grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
            <div>
              <div className={getFieldShellClassName(Boolean(errors.planName?.message))}>
                {planName ? <FieldLabel>Plan Name</FieldLabel> : null}
                <Input
                  aria-label="Plan Name"
                  placeholder="Plan Name"
                  maxLength={30}
                  className={cn(
                    getInputClassName(),
                    getFloatingInputPaddingClassName(Boolean(planName))
                  )}
                  {...register("planName")}
                />
              </div>
              <FieldHint error={Boolean(errors.planName?.message)}>
                {errors.planName?.message ??
                  "e.g. Enterprise; The public display name for the plan. Max 30 characters."}
              </FieldHint>
            </div>

            <div>
              <div className={getFieldShellClassName(Boolean(errors.planCode?.message))}>
                {planCode ? <FieldLabel>Plan Code</FieldLabel> : null}
                <Input
                  aria-label="Plan Code"
                  placeholder="Plan Code"
                  maxLength={30}
                  readOnly={props.mode === "edit"}
                  className={cn(
                    getInputClassName(),
                    getFloatingInputPaddingClassName(Boolean(planCode))
                  )}
                  {...register("planCode")}
                />
              </div>
              <FieldHint error={Boolean(errors.planCode?.message)}>
                {errors.planCode?.message ??
                  "e.g. enterprise; A unique, URL-safe identifier. Cannot be changed later. Max 30 characters."}
              </FieldHint>
            </div>
          </div>

          <div className="mt-4">
            <div
              className={getFieldShellClassName(
                Boolean(errors.description?.message),
                true
              )}
            >
              {description ? (
                <FieldLabel>
                  {props.mode === "edit" ? "Description (Optional)" : "Description"}
                </FieldLabel>
              ) : null}
              <Textarea
                aria-label="Description"
                placeholder="Description"
                maxLength={500}
                className={cn(
                  getTextareaClassName(),
                  getFloatingTextareaPaddingClassName(Boolean(description))
                )}
                {...register("description")}
              />
            </div>
            <FieldHint error={Boolean(errors.description?.message)}>
              {errors.description?.message ??
                "Briefly describe what this feature controls..."}
            </FieldHint>
          </div>
        </section>

        <section className={getCardClassName()}>
          <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
            Pricing &amp; Billing
          </div>

          <div className="mt-5 grid grid-cols-1 gap-x-5 gap-y-4 md:grid-cols-2">
            <div>
              <Select
                value={watch("billingCycle")}
                onValueChange={function (value) {
                  setValue("billingCycle", value as CreatePlanValues["billingCycle"], {
                    shouldDirty: true,
                    shouldValidate: true,
                  });
                }}
              >
                <div className={getFieldShellClassName(Boolean(errors.billingCycle?.message))}>
                  <SelectTrigger className={getSelectTriggerClassName()}>
                    <div className={getSelectFieldContentClassName()}>
                      <span className={getSelectFieldLabelClassName()}>Billing Cycle</span>
                      <SelectValue
                        placeholder="Select billing cycle"
                        className="text-[0.875rem] font-normal leading-5 text-[#2b3946] data-[placeholder]:text-[#97a4b0]"
                      />
                    </div>
                  </SelectTrigger>
                </div>
                <SelectContent className={getSelectContentClassName()}>
                  {["Monthly", "Yearly"].map(function (value) {
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
              {errors.billingCycle?.message ? (
                <FieldHint error>{errors.billingCycle.message}</FieldHint>
              ) : null}
            </div>

            <div>
              <div
                className={getFieldShellClassName(Boolean(errors.basePriceUsd?.message))}
              >
                {basePriceUsd ? <FieldLabel>Base Price (USD)</FieldLabel> : null}
                <Input
                  aria-label="Base Price (USD)"
                  placeholder="Base Price (USD)"
                  maxLength={10}
                  className={cn(
                    getInputClassName(),
                    getFloatingInputPaddingClassName(Boolean(basePriceUsd))
                  )}
                  {...register("basePriceUsd")}
                />
              </div>
              <FieldHint error={Boolean(errors.basePriceUsd?.message)}>
                {errors.basePriceUsd?.message ??
                  "The starting price before per-user fees or add-ons. Max 10 characters."}
              </FieldHint>
            </div>

            <div>
              <div className={getFieldShellClassName(Boolean(errors.trialDays?.message))}>
                {trialDays ? <FieldLabel>Trial Days</FieldLabel> : null}
                <Input
                  aria-label="Trial Days"
                  placeholder="Trial Days"
                  maxLength={3}
                  className={cn(
                    getInputClassName(),
                    getFloatingInputPaddingClassName(Boolean(trialDays))
                  )}
                  {...register("trialDays")}
                />
              </div>
              <FieldHint error={Boolean(errors.trialDays?.message)}>
                {errors.trialDays?.message ?? "Leave 0 for no trial period. Max 3 digits."}
              </FieldHint>
            </div>

            <div>
              <Select
                value={watch("status")}
                onValueChange={function (value) {
                  setValue("status", value as CreatePlanValues["status"], {
                    shouldDirty: true,
                    shouldValidate: true,
                  });
                }}
              >
                <div className={getFieldShellClassName(Boolean(errors.status?.message))}>
                  <SelectTrigger className={getSelectTriggerClassName()}>
                    <div className={getSelectFieldContentClassName()}>
                      <span className={getSelectFieldLabelClassName()}>Status</span>
                      <SelectValue
                        placeholder="Select status"
                        className="text-[0.875rem] font-normal leading-5 text-[#2b3946] data-[placeholder]:text-[#97a4b0]"
                      />
                    </div>
                  </SelectTrigger>
                </div>
                <SelectContent className={getSelectContentClassName()}>
                  {["Active", "Draft"].map(function (value) {
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
              <FieldHint error={Boolean(errors.status?.message)}>
                {errors.status?.message ??
                  "Draft plans cannot be assigned to organizations."}
              </FieldHint>
            </div>
          </div>

          <div className="mt-6 flex items-center justify-end gap-3">
            <Button
              type="button"
              variant="secondary"
              size="md"
              onClick={props.onCancel}
              disabled={isCreating || isUpdating}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="md"
              disabled={props.isLoadingInitialValues || isCreating || isUpdating}
            >
              {getTitle(props.mode)}
            </Button>
          </div>
        </section>
      </div>
    </form>
  );
}

export default CreatePlanForm;
