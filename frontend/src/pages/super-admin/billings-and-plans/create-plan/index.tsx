import { ChevronLeft } from "lucide-react";
import { useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import SuperAdminHeaderActions from "@/components/shared/SuperAdminHeaderActions";
import { Button } from "@/components/ui/button";
import CreatePlanForm from "./components/CreatePlanForm";
import type { CreatePlanValues } from "./createPlan.schema";
import { useGetBillingPlanDetailsQuery } from "@/store/api/superAdminApi";
import type { BillingPlansTab } from "../components/BillingPlansTabs";
import { stashBillingToast } from "../billingToast.utils";

interface CreatePlanLocationState extends Partial<CreatePlanValues> {
  mode?: "create" | "edit";
  initialTab?: BillingPlansTab;
}

function getDefaultValues(): CreatePlanValues {
  return {
    planName: "",
    planCode: "",
    description: "",
    billingCycle: "Monthly",
    basePriceUsd: "0.00",
    trialDays: "14",
    status: "Active",
  };
}

function CreatePlan() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = (location.state ?? {}) as CreatePlanLocationState;
  const returnTab: BillingPlansTab = state.initialTab ?? "plans";

  function handleGoBack() {
    navigate("/super-admin/billings-and-plans", {
      state: { initialTab: returnTab },
    });
  }

  const mode = state.mode === "edit" ? "edit" : "create";
  const planCode = state.planCode ?? "";
  const {
    data: planDetails,
    isLoading: isPlanLoading,
    isError: isPlanError,
  } = useGetBillingPlanDetailsQuery(planCode, {
    skip: mode !== "edit" || !planCode,
    refetchOnMountOrArgChange: true,
  });

  const initialValues = useMemo<CreatePlanValues>(function () {
    if (mode === "edit" && planDetails) {
      const billingCycle = planDetails.billingCycle?.toLowerCase().includes("year")
        ? "Yearly"
        : "Monthly";
      const trialDaysValue =
        typeof planDetails.trialDays === "number" && Number.isFinite(planDetails.trialDays)
          ? String(planDetails.trialDays)
          : "0";

      return {
        ...getDefaultValues(),
        planName: planDetails.planName || state.planName || "",
        planCode: planDetails.planCode || state.planCode || "",
        description: planDetails.description || "",
        billingCycle,
        basePriceUsd: (
          billingCycle === "Yearly" ? planDetails.annualPrice : planDetails.basePrice
        ).toString(),
        trialDays: trialDaysValue,
        status: planDetails.status?.toLowerCase().includes("draft") ? "Draft" : "Active",
      };
    }

    return {
      ...getDefaultValues(),
      planName: state.planName ?? "",
      planCode: state.planCode ?? "",
      description: state.description ?? "",
      billingCycle: state.billingCycle === "Yearly" ? "Yearly" : "Monthly",
      basePriceUsd: state.basePriceUsd ?? "0.00",
      trialDays: state.trialDays ?? "14",
      status: state.status === "Draft" ? "Draft" : "Active",
    };
  }, [mode, planDetails, state]);
  const pageTitle =
    mode === "edit"
      ? "Edit Plan : " + (initialValues.planName || "Enterprise")
      : "Create New Plan";
  const pageDescription =
    mode === "edit"
      ? "Modify the basic settings and pricing structure for this plan."
      : "";

  return (
    <div className="h-full min-h-full w-full overflow-auto bg-[#FAFAFB] pb-6">
      <div className="flex w-full min-h-full flex-col gap-4 bg-[#FAFAFB]">
        <div className="sticky top-0 z-20 flex shrink-0 items-start justify-between gap-4 bg-[#FAFAFB] pb-4 pt-1">
          <div className="min-w-0">
            <Button
              type="button"
              variant="tertiary"
              size="sm"
              onClick={handleGoBack}
            >
              <ChevronLeft size={14} aria-hidden="true" />
              Back
            </Button>
            <h1 className="mt-2 text-[1.125rem] font-semibold leading-8 text-[#1f2d38]">
              {pageTitle}
            </h1>
            {pageDescription ? (
              <p className="mt-1 text-[0.8125rem] font-normal leading-5 text-[#667483]">
                {pageDescription}
              </p>
            ) : null}
          </div>

          <SuperAdminHeaderActions />
        </div>

        <CreatePlanForm
          mode={mode}
          initialValues={initialValues}
          onCancel={handleGoBack}
          onSuccess={(message) => {
            stashBillingToast(message, "success");
            navigate("/super-admin/billings-and-plans", {
              state: {
                initialTab: returnTab,
                toastMessage: message,
                toastType: "success",
              },
            });
          }}
          isLoadingInitialValues={isPlanLoading}
          initialLoadError={isPlanError ? "Unable to load plan details." : null}
        />
      </div>
    </div>
  );
}

export default CreatePlan;
