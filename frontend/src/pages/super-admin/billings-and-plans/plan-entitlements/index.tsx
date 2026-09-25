import { useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import SuperAdminHeaderActions from "@/components/shared/SuperAdminHeaderActions";
import PageHeader from "./components/PageHeader";
import EntitlementCard, { type PlanFeatureRow } from "./components/EntitlementCard";
import {
  useGetFeatureCatalogQuery,
  useGetPlanEntitlementsQuery,
  useReplacePlanEntitlementsMutation,
  type PlanEntitlementFeaturePayload,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  hasValidUsageLimit,
  isPlanLimitFeature,
} from "./planEntitlement.utils";
import { stashBillingToast } from "../billingToast.utils";

function renderBreadcrumb(
  planName: string,
  onGoDashboard: () => void,
  onGoBillingsAndPlans: () => void,
) {
  return (
    <div className="flex min-w-0 flex-1 items-center gap-1.5 overflow-hidden text-[0.6875rem] font-medium leading-4 text-[#8a96a3]">
      <button
        type="button"
        onClick={onGoDashboard}
        className="shrink-0 cursor-pointer transition-colors hover:text-[#2b3946]"
      >
        Super Admin
      </button>
      <ChevronRight size={12} aria-hidden="true" />
      <button
        type="button"
        onClick={onGoBillingsAndPlans}
        className="shrink-0 cursor-pointer transition-colors hover:text-[#2b3946]"
      >
        Billings &amp; Plans
      </button>
      <ChevronRight size={12} aria-hidden="true" />
      <span className="min-w-0 truncate" title={planName}>
        {planName}
      </span>
      <ChevronRight size={12} aria-hidden="true" />
      <span className="shrink-0 font-bold text-[#2b3946]">Edit Entitlements</span>
    </div>
  );
}

export default function PlanEntitlements() {
  const navigate = useNavigate();
  const location = useLocation();
  const state =
    (location.state as {
      planName?: string;
      planCode?: string;
      initialTab?: "overview" | "invoices" | "plans" | "add-ons" | "settings";
    }) || {};

  const planName = state.planName || state.planCode || "Plan";
  const planPathParam = state.planCode || state.planName || "";
  const returnTab = state.initialTab ?? "plans";

  const {
    data: catalog = [],
    isLoading: isLoadingCatalog,
    isError: isCatalogError,
    error: catalogError,
  } = useGetFeatureCatalogQuery({ includeDeprecated: false });

  const {
    currentData: entitlementsResponse,
    isLoading: isLoadingEntitlements,
    isError: isEntitlementsError,
    error: entitlementsError,
    refetch: refetchEntitlements,
  } = useGetPlanEntitlementsQuery(planPathParam, {
    skip: !planPathParam,
  });

  const [replacePlanEntitlements, { isLoading: isSaving }] =
    useReplacePlanEntitlementsMutation();

  const [rows, setRows] = useState<PlanFeatureRow[]>([]);
  const [saveErrorMessage, setSaveErrorMessage] = useState<string | null>(null);
  const [limitErrors, setLimitErrors] = useState<Record<string, string>>({});

  const entitlementByKey = useMemo(() => {
    const map = new Map<
      string,
      { enabled: boolean; usageLimit: number | null }
    >();
    const features = entitlementsResponse?.features ?? [];
    features.forEach((feature) => {
      map.set(feature.key, {
        enabled: feature.enabled,
        usageLimit: feature.usageLimit,
      });
    });
    return map;
  }, [entitlementsResponse]);

  const [planScope, setPlanScope] = useState(planPathParam);
  const [seededPlan, setSeededPlan] = useState<string | null>(null);
  if (planScope !== planPathParam) {
    setPlanScope(planPathParam);
    setSeededPlan(null);
    setRows([]);
    setLimitErrors({});
  }
  if (catalog.length > 0 && entitlementsResponse && (planScope !== planPathParam || seededPlan !== planPathParam)) {
    setSeededPlan(planPathParam);
    setRows(
      catalog.map((feature) => {
        const current = entitlementByKey.get(feature.keyName);
        return {
          key: feature.keyName,
          name: feature.name,
          description: feature.description,
          type: feature.type,
          enabled: current?.enabled ?? false,
          usageLimit: current?.usageLimit ?? null,
        };
      }),
    );
    setLimitErrors({});
  }

  const isLoading = isLoadingCatalog || isLoadingEntitlements;
  const loadError =
    (isCatalogError ? catalogError : null) ||
    (isEntitlementsError ? entitlementsError : null);

  const handleDiscard = () => {
    navigate("/super-admin/billings-and-plans", {
      state: { initialTab: returnTab },
    });
  };

  const handleGoDashboard = () => {
    navigate("/super-admin/dashboard");
  };

  const handleGoBillingsAndPlans = () => {
    navigate("/super-admin/billings-and-plans", {
      state: { initialTab: returnTab },
    });
  };

  const handleChange = (
    key: string,
    field: "enabled" | "usageLimit",
    value: boolean | number | null,
  ) => {
    setLimitErrors((previous) => {
      if (!previous[key]) return previous;
      const next = { ...previous };
      delete next[key];
      return next;
    });

    setRows((previous) =>
      previous.map((row) => {
        if (row.key !== key) return row;
        if (field === "enabled") {
          return {
            ...row,
            enabled: Boolean(value),
            usageLimit: value ? row.usageLimit : null,
          };
        }
        return {
          ...row,
          usageLimit: typeof value === "number" ? value : null,
        };
      }),
    );
  };

  const handleSave = async () => {
    if (!planPathParam) return;

    const nextLimitErrors: Record<string, string> = {};
    rows.forEach((row) => {
      if (!row.enabled) return;
      if (!isPlanLimitFeature(row.key, row.type)) return;
      if (!hasValidUsageLimit(row.usageLimit)) {
        nextLimitErrors[row.key] = "Usage limit is required for this feature.";
      }
    });

    if (Object.keys(nextLimitErrors).length > 0) {
      setLimitErrors(nextLimitErrors);
      setSaveErrorMessage(
        "Enter a usage limit for every enabled limit-type feature before saving.",
      );
      return;
    }

    const features: PlanEntitlementFeaturePayload[] = rows
      .filter((row) => row.enabled)
      .map((row) => {
        const payload: PlanEntitlementFeaturePayload = {
          key: row.key,
          enabled: true,
        };
        if (isPlanLimitFeature(row.key, row.type)) {
          payload.usageLimit = row.usageLimit;
        }
        return payload;
      });

    try {
      setSaveErrorMessage(null);
      setLimitErrors({});
      await replacePlanEntitlements({
        planName: planPathParam,
        body: { features },
      }).unwrap();
      await refetchEntitlements();
      stashBillingToast("Plan entitlements updated successfully.", "success");
      navigate("/super-admin/billings-and-plans", {
        state: {
          initialTab: returnTab,
          toastMessage: "Plan entitlements updated successfully.",
          toastType: "success",
        },
      });
    } catch (error) {
      setSaveErrorMessage(getApiErrorMessage(error));
    }
  };

  return (
    <div className="flex h-full min-h-0 w-full flex-col overflow-hidden bg-[#FAFAFB]">
      <div className="sticky top-0 z-20 flex shrink-0 flex-col gap-7 bg-[#FAFAFB] pb-4 pt-1">
        <div className="flex items-center justify-between gap-4">
          {renderBreadcrumb(planName, handleGoDashboard, handleGoBillingsAndPlans)}

          <div className="flex shrink-0 items-center gap-3">
            <SuperAdminHeaderActions />
          </div>
        </div>

        <PageHeader
          planName={planName}
          onDiscard={handleDiscard}
          onSave={() => void handleSave()}
          isSaving={isSaving}
        />
      </div>

      <div className="min-h-0 flex-1 overflow-x-hidden overflow-y-auto pb-6">
        <div className="flex w-full min-w-0 flex-col gap-7">
          {!planPathParam ? (
            <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-[#b42318]">
              Missing plan code. Open Edit Entitlements from the plans list.
            </div>
          ) : null}

          {saveErrorMessage ? (
            <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-[#b42318]">
              {saveErrorMessage}
            </div>
          ) : null}

          {loadError ? (
            <div className="rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-[#b42318]">
              {getApiErrorMessage(loadError)}
            </div>
          ) : null}

          {isLoading ? (
            <div className="rounded-[0.75rem] border border-[#e3ebf3] bg-white px-4 py-3 text-sm text-[#667483]">
              Loading catalog and plan entitlements...
            </div>
          ) : null}

          {!isLoading && !loadError && planPathParam ? (
            <div className="w-full min-w-0">
              <div className="mb-4">
                <h2 className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
                  Plan features
                </h2>
                <p className="mt-1 text-[0.8125rem] text-[#667483]">
                  Toggle features from the master catalog on or off for this plan.
                  Limit features require a usage limit before save.
                </p>
              </div>
              <EntitlementCard
                items={rows}
                onChange={handleChange}
                limitErrors={limitErrors}
              />
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}
