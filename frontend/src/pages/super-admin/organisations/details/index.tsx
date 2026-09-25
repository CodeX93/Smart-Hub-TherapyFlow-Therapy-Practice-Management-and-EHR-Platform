import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, Plus } from "lucide-react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import SuperAdminHeaderActions from "@/components/shared/SuperAdminHeaderActions";
import Toast from "@/components/shared/Toast";
import {
  ORGANISATIONS,
  type OrganisationPlan,
  type OrganisationRow,
  type OrganisationStatus,
} from "../organisations.data";
import OrganisationHeaderCard from "./components/OrganisationHeaderCard";
import OrganisationTabs, {
  type OrganisationDetailsTab,
} from "./components/OrganisationTabs";
import OverviewTab from "./tabs/OverviewTab";
import BillingSubscriptionTab from "./tabs/BillingSubscriptionTab";
import OrganisationAddOnsTab from "./tabs/OrganisationAddOnsTab";
import OrganisationAuditLogsTab from "./tabs/OrganisationAuditLogsTab";
import ProvisionTab from "./tabs/ProvisionTab";
import PageHeader from "../feature-toggles/components/PageHeader";
import SectionHeading from "../feature-toggles/components/SectionHeading";
import ModuleAccessCard, {
  type ModuleToggleRow,
} from "../feature-toggles/components/ModuleAccessCard";
import UsageLimitCard, {
  type UsageLimitRow,
} from "../feature-toggles/components/UsageLimitCard";
import {
  buildModuleRowsFromFeatures,
  buildFeaturesPayloadFromRows,
  buildUsageRowsFromFeatures,
  isUsageKey,
} from "../feature-toggles/featureRows";
import {
  sanitizeUsageLimitOverride,
  validateEnabledUsageLimits,
} from "../feature-toggles/featureOverrides.utils";
import {
  useGetOrganisationSubscriptionQuery,
  useGetPlansCatalogQuery,
  useGetOrganisationByIdQuery,
  useGetOrganisationFeaturesQuery,
  useReplaceOrganisationFeaturesMutation,
  useUpdateOrganisationSubscriptionMutation,
  useUpdateOrganisationByIdMutation,
  type OrganisationDetailsResult,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  PLAN_MANAGE_TRIAL_DAYS_MAX_DIGITS,
  applyPlanCatalogDefaults,
  validatePlanManageInput,
} from "../planManage.utils";

function updateModuleRows(
  rows: ModuleToggleRow[],
  keyName: string,
  next: boolean
): ModuleToggleRow[] {
  return rows.map(function (row) {
    if (row.keyName === keyName) {
      return { ...row, overrideEnabled: next };
    }
    return row;
  });
}

function updateUsageLimitRows(
  rows: UsageLimitRow[],
  keyName: string,
  next: string
): UsageLimitRow[] {
  return rows.map(function (row) {
    if (row.keyName === keyName) {
      return { ...row, overrideValue: next };
    }
    return row;
  });
}

function humanizeFeatureKey(keyName: string): string {
  return keyName
    .split("_")
    .filter(Boolean)
    .map((part) => part[0]?.toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

function renderTabContent(
  tab: Exclude<OrganisationDetailsTab, "overview">,
  org: OrganisationRow,
  organisationId: number | null,
  addOnsModalState: {
    isOpen: boolean;
    onOpenChange(next: boolean): void;
  },
  featureOverrides: {
    moduleRows: ModuleToggleRow[];
    limitRows: UsageLimitRow[];
    onDiscard(): void;
    onSave(): void;
    isSaving: boolean;
    onModuleRowChange(keyName: string, next: boolean): void;
    onUsageLimitRowChange(keyName: string, next: string): void;
  },
  onUpdatePlan: () => void
) {
  if (tab === "billing") {
    return (
      <BillingSubscriptionTab
        org={org}
        organisationId={organisationId}
        onUpdatePlan={onUpdatePlan}
      />
    );
  }

  if (tab === "addons") {
    return (
      <OrganisationAddOnsTab
        organisationId={organisationId}
        isAssignModalOpen={addOnsModalState.isOpen}
        onAssignModalOpenChange={addOnsModalState.onOpenChange}
      />
    );
  }

  if (tab === "provision") {
    return <ProvisionTab organisationId={organisationId} />;
  }

  if (tab === "audit-logs") {
    return <OrganisationAuditLogsTab organisationId={organisationId} />;
  }

  if (tab === "feature-overrides") {
    return (
      <div className="flex flex-col gap-7">
        <div className="w-full">
          <SectionHeading
            title="Module & Access Controls"
            subtitle="Enable or disable entire modules or core capabilities."
          />
          <div className="mt-4">
            <ModuleAccessCard
              rows={featureOverrides.moduleRows}
              onChange={featureOverrides.onModuleRowChange}
            />
          </div>
        </div>

        <div className="w-full">
          <SectionHeading
            title="Usage Limitations"
            subtitle="Set strict numeric limits on features. Leave empty or set to unlimited where applicable."
          />
          <div className="mt-4">
            <UsageLimitCard
              rows={featureOverrides.limitRows}
              onChange={featureOverrides.onUsageLimitRowChange}
            />
          </div>
        </div>
      </div>
    );
  }

  return null;
}

function normalizeStatus(value: string | null | undefined): OrganisationStatus {
  const normalized = (value ?? "").trim().toLowerCase();
  if (normalized.includes("active")) return "Active";
  if (normalized.includes("pending")) return "Pending Activation";
  return "Suspended";
}

function normalizePlan(value: string | null | undefined): OrganisationPlan {
  const normalized = (value ?? "").trim().toLowerCase();
  if (!normalized) return "-";
  if (normalized.includes("enterprise")) return "Enterprise";
  if (normalized.includes("trial")) return "Trial";
  return "Pro";
}

function resolveCustomDomain(details: OrganisationDetailsResult): string {
  if (details.subdomain) {
    return `${details.subdomain}.localhost`;
  }
  return "-";
}

function mapOrganisationDetailsToRow(
  details: OrganisationDetailsResult,
  fallbackOrg: OrganisationRow | null
): OrganisationRow {
  const subscriptionDetails = details.subscriptionDetails;
  const extraPlan = (details.extra.plan ?? details.extra.planName ?? "") as string;
  const extraBillingCycle = (details.extra.billingCycle ?? "") as string;
  const extraPeriodEnd = (details.extra.currentPeriodEnd ?? details.extra.subscriptionEnd ?? "") as string;
  const extraAddOns = (details.extra.addOns ?? details.extra.entitlements ?? "") as string;
  const extraBasePrice = (details.extra.basePrice ?? details.extra.price ?? "") as string;

  const plan = normalizePlan(subscriptionDetails?.plan || extraPlan);
  const billingCycle =
    subscriptionDetails?.billingCycle || extraBillingCycle || "-";
  const basePrice = subscriptionDetails?.priceAtTime
    ? `$${subscriptionDetails.priceAtTime.toFixed(2)}`
    : extraBasePrice || fallbackOrg?.subscription.basePrice || "-";
  const currentPeriodEnd =
    subscriptionDetails?.trialEndsAt ||
    subscriptionDetails?.endAt ||
    extraPeriodEnd ||
    fallbackOrg?.subscription.currentPeriodEnd ||
    "-";

  return {
    name: details.name || fallbackOrg?.name || "-",
    slug: details.slug || fallbackOrg?.slug || "-",
    status: normalizeStatus(details.status || fallbackOrg?.status),
    plan,
    users: details.totalUserCount ?? fallbackOrg?.users ?? 0,
    createdAt: details.createdAt || fallbackOrg?.createdAt || "-",
    rrCode: details.id ? `RR: org_${details.id}` : fallbackOrg?.rrCode || "-",
    primaryAdmin: {
      initials: (details.name || fallbackOrg?.name || "O")
        .split(" ")
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase() ?? "")
        .join(""),
      email: details.primaryAdminEmail || fallbackOrg?.primaryAdmin.email || "-",
    },
    supportEmail: details.supportEmail || fallbackOrg?.supportEmail || "",
    region: details.region || fallbackOrg?.region || "-",
    timezone: details.timezone || fallbackOrg?.timezone || "-",
    dataResidency: details.dataResidency || fallbackOrg?.dataResidency || "-",
    customDomain: resolveCustomDomain(details),
    subscription: {
      plan,
      status: subscriptionDetails?.status || fallbackOrg?.subscription.status,
      billingCycle,
      currentPeriodEnd,
      addOns: extraAddOns || fallbackOrg?.subscription.addOns || "-",
      basePrice,
      userLimits: subscriptionDetails
        ? {
            therapist: subscriptionDetails.userLimits.therapistLimit,
            supervisor: subscriptionDetails.userLimits.supervisorLimit,
            client: subscriptionDetails.userLimits.clientLimit,
          }
        : fallbackOrg?.subscription.userLimits,
      userUsage: subscriptionDetails
        ? {
            therapist: subscriptionDetails.userUsage.therapistUsers,
            supervisor: subscriptionDetails.userUsage.supervisorUsers,
            client: subscriptionDetails.userUsage.clientUsers,
            total: subscriptionDetails.userUsage.totalUsers,
          }
        : fallbackOrg?.subscription.userUsage,
    },
  };
}

function OrganisationDetails() {
  const { slug } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [tab, setTab] = useState<OrganisationDetailsTab>("overview");
  const [isAssignAddOnModalOpen, setIsAssignAddOnModalOpen] = useState(false);
  const [isPlanManageModalOpen, setIsPlanManageModalOpen] = useState(false);
  const [planValue, setPlanValue] = useState("");
  const [billingCycleValue, setBillingCycleValue] = useState("monthly");
  const [trialDaysValue, setTrialDaysValue] = useState("");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  useEffect(() => {
    const requestedTab = (
      location.state as { initialTab?: OrganisationDetailsTab } | null
    )?.initialTab;
    if (requestedTab) {
      setTab(requestedTab);
    }
  }, [location.state]);

  const [moduleRows, setModuleRows] = useState<ModuleToggleRow[]>(
    buildModuleRowsFromFeatures()
  );
  const [limitRows, setLimitRows] = useState<UsageLimitRow[]>(
    buildUsageRowsFromFeatures()
  );
  const organisationId = (location.state as { organisationId?: number | null } | null)?.organisationId ?? null;

  const fallbackOrg = useMemo(
    function () {
      return ORGANISATIONS.find(function (item) {
        return item.slug === slug;
      });
    },
    [slug]
  );
  const {
    data: organisationDetails,
    isLoading: isOrganisationDetailsLoading,
    isError: isOrganisationDetailsError,
    error: organisationDetailsError,
    refetch: refetchOrganisationDetails,
  } = useGetOrganisationByIdQuery(organisationId ?? 0, {
    skip: organisationId === null,
  });
  const [updateOrganisationById, { isLoading: isUpdatingGeneralInfo }] =
    useUpdateOrganisationByIdMutation();
  const [replaceOrganisationFeatures, { isLoading: isSavingFeatures }] =
    useReplaceOrganisationFeaturesMutation();
  const { data: plansCatalog = [] } = useGetPlansCatalogQuery();
  const {
    data: organisationSubscription,
    isLoading: isSubscriptionLoading,
  } = useGetOrganisationSubscriptionQuery(organisationId ?? 0, {
    skip: !organisationId || !isPlanManageModalOpen,
  });
  const [updateOrganisationSubscription, { isLoading: isUpdatingSubscription }] =
    useUpdateOrganisationSubscriptionMutation();
  const {
    data: organisationFeatures,
    isLoading: isFeaturesLoading,
    isError: isFeaturesError,
    error: featuresError,
    refetch: refetchOrganisationFeatures,
  } = useGetOrganisationFeaturesQuery(organisationId ?? 0, {
    skip: organisationId === null,
  });

  useEffect(() => {
    setModuleRows(buildModuleRowsFromFeatures(organisationFeatures));
    setLimitRows(buildUsageRowsFromFeatures(organisationFeatures));
  }, [organisationFeatures]);

  useEffect(() => {
    if (!organisationSubscription) return;
    setPlanValue("");
    setBillingCycleValue("");
    setTrialDaysValue("");
  }, [organisationSubscription]);

  function handleTargetPlanChange(nextPlanCode: string) {
    setPlanValue(nextPlanCode);
    const plan = plansCatalog.find((item) => item.planCode === nextPlanCode);
    const defaults = applyPlanCatalogDefaults(plan);
    setBillingCycleValue(defaults.billingCycle);
    setTrialDaysValue(defaults.trialDays);
  }

  const org = useMemo(() => {
    if (organisationDetails) {
      return mapOrganisationDetailsToRow(organisationDetails, fallbackOrg ?? null);
    }
    return fallbackOrg ?? null;
  }, [fallbackOrg, organisationDetails]);

  const currentPlanLabel = useMemo(() => {
    const planCode = organisationSubscription?.plan?.trim();
    if (!planCode) return "";
    const match = plansCatalog.find((plan) => plan.planCode === planCode);
    return match?.planName || planCode;
  }, [organisationSubscription?.plan, plansCatalog]);

  const selectedTargetPlanLabel = useMemo(() => {
    if (!planValue) return "";
    const match = plansCatalog.find((plan) => plan.planCode === planValue);
    return match?.planName || planValue;
  }, [planValue, plansCatalog]);

  function handleBack() {
    navigate("/super-admin/organisations");
  }

  function handleManageSubscriptionFromOverview() {
    setIsPlanManageModalOpen(true);
  }

  async function handleUpdatePlan() {
    if (!organisationId) {
      setToastType("error");
      setToastMessage("Organisation id is missing.");
      return;
    }

    const validationError = validatePlanManageInput({
      plan: planValue,
      billingCycle: billingCycleValue,
      trialDays: trialDaysValue,
    });
    if (validationError) {
      setToastType("error");
      setToastMessage(validationError);
      return;
    }

    try {
      await updateOrganisationSubscription({
        id: organisationId,
        body: {
          plan: planValue || undefined,
          billingCycle: billingCycleValue ? billingCycleValue.toLowerCase() : undefined,
          trialDays: trialDaysValue ? Number.parseInt(trialDaysValue, 10) : undefined,
          prorate: false,
        },
      }).unwrap();
      setIsPlanManageModalOpen(false);
      await refetchOrganisationDetails();
      setToastType("success");
      setToastMessage("Plan updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  }

  function handleDiscardFeatureOverrides() {
    setModuleRows(buildModuleRowsFromFeatures(organisationFeatures));
    setLimitRows(buildUsageRowsFromFeatures(organisationFeatures));
  }

  async function handleSaveFeatureOverrides() {
    if (!organisationId) {
      setToastType("error");
      setToastMessage("Organisation id is missing.");
      return;
    }

    const validationError = validateEnabledUsageLimits(moduleRows, limitRows);
    if (validationError) {
      setToastType("error");
      setToastMessage(validationError);
      return;
    }

    try {
      const payload = buildFeaturesPayloadFromRows(
        moduleRows,
        limitRows,
        organisationFeatures
      );

      await replaceOrganisationFeatures({
        id: organisationId,
        body: payload,
      }).unwrap();

      await refetchOrganisationFeatures();
      setToastType("success");
      setToastMessage("Feature overrides saved successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  }

  function handleModuleRowChange(keyName: string, next: boolean) {
    setModuleRows((previousRows) => updateModuleRows(previousRows, keyName, next));
    if (!isUsageKey(keyName)) {
      return;
    }
    setLimitRows((previousRows) => {
      const existing = previousRows.find((row) => row.keyName === keyName);
      if (next) {
        if (existing) return previousRows;
        const fallbackLimit = organisationFeatures?.[keyName]?.usageLimit;
        return [
          ...previousRows,
          {
            title: humanizeFeatureKey(keyName),
            keyName,
            planDefault: "",
            overrideValue:
              typeof fallbackLimit === "number" && Number.isFinite(fallbackLimit)
                ? sanitizeUsageLimitOverride(String(fallbackLimit))
                : "",
          },
        ];
      }
      return previousRows.filter((row) => row.keyName !== keyName);
    });
  }

  function handleUsageLimitRowChange(keyName: string, next: string) {
    setLimitRows((previousRows) =>
      updateUsageLimitRows(previousRows, keyName, sanitizeUsageLimitOverride(next)),
    );
  }

  async function handleUpdateGeneralInfo(values: {
    organisationName: string;
    supportEmail: string;
    slug: string;
    region: string;
    dataResidency: string;
    timezone: string;
  }) {
    if (!organisationId) {
      throw new Error("Organisation id is missing");
    }

    const payload: Record<string, unknown> = {};
    const nextName = values.organisationName.trim();
    const nextSupportEmail = values.supportEmail.trim();
    const nextSlug = values.slug.trim();

    if (nextName !== (org?.name ?? "").trim()) payload.name = nextName;
    if (nextSupportEmail !== (org?.supportEmail ?? "").trim()) {
      payload.supportEmail = nextSupportEmail || null;
    }
    if (nextSlug !== (org?.slug ?? "").trim()) payload.slug = nextSlug;

    if (Object.keys(payload).length === 0) {
      return;
    }

    await updateOrganisationById({
      id: organisationId,
      body: payload,
    }).unwrap();
    await refetchOrganisationDetails();
  }

  if (organisationId !== null && isOrganisationDetailsLoading && !org) {
    return (
      <div className="h-full w-full overflow-auto pb-6">
        <div className="text-[#667483] text-sm font-medium">
          Loading organisation details...
        </div>
      </div>
    );
  }

  if (!org) {
    return (
      <div className="h-full w-full overflow-auto pb-6">
        <div className="text-(--text-gray-900) text-sm font-medium">
          {isOrganisationDetailsError
            ? getApiErrorMessage(organisationDetailsError)
            : "Organisation not found."}
        </div>
        <Button
          variant="outline"
          className="mt-4 border-(--neutral-100)"
          onClick={handleBack}
        >
          Back to Organisations
        </Button>
      </div>
    );
  }

  return (
    <div className="flex h-full min-h-0 w-full flex-col bg-[#FAFAFB]">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <div className="sticky top-0 z-20 flex shrink-0 flex-col gap-4 bg-[#FAFAFB] pb-4">
        <div className="flex items-center justify-between gap-4 pt-1">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              className="shrink-0"
              onClick={handleBack}
            >
              <ArrowLeft size={14} aria-hidden="true" />
              Back
            </Button>

            <div className="min-w-0 flex flex-1 items-center gap-1.5 overflow-hidden text-[0.8125rem] font-medium text-[#7c8a97]">
            <button
              type="button"
              onClick={() => navigate("/super-admin/dashboard")}
              className="shrink-0 cursor-pointer transition-colors hover:text-[#2b3946] hover:underline"
            >
              Super Admin
            </button>
            <span className="shrink-0">/</span>
            <button
              type="button"
              onClick={() => navigate("/super-admin/organisations")}
              className="shrink-0 cursor-pointer transition-colors hover:text-[#2b3946] hover:underline"
            >
              Organisations
            </button>
            <span className="shrink-0">/</span>
            <span className="min-w-0 truncate text-[#2b3946]" title={org.name}>
              {org.name}
            </span>
          </div>
          </div>

          <SuperAdminHeaderActions />
        </div>

        <OrganisationHeaderCard 
          org={org} 
          organisationId={organisationId} 
          onManageBilling={() => setTab("billing")}
        />
        <div className="flex min-w-0 flex-col gap-2 lg:flex-row lg:items-center lg:justify-between">
          <OrganisationTabs
            value={tab}
            onChange={setTab}
          />
          {tab === "addons" ? (
            <Button
              variant="primary"
              size="md"
              className="w-fit shrink-0 self-start"
              onClick={() => setIsAssignAddOnModalOpen(true)}
              disabled={!organisationId}
            >
              <Plus size={16} aria-hidden="true" />
              Create Add-on
            </Button>
          ) : null}
        </div>
      </div>

      <div
        className={cn(
          "min-h-0 min-w-0 flex-1",
          tab === "feature-overrides"
            ? "flex flex-col overflow-hidden"
            : "overflow-y-auto pb-6",
        )}
        style={
          tab === "feature-overrides"
            ? undefined
            : {
                scrollBehavior: "smooth",
                overscrollBehavior: "contain",
              }
        }
      >
        <div
          className={cn(
            "flex flex-col gap-4",
            tab === "feature-overrides" ? "min-h-0 flex-1" : undefined,
          )}
        >
          {isOrganisationDetailsLoading ? (
            <div className="w-full rounded-[1rem] border border-[#e3ebf3] bg-white px-4 py-4 text-[#667483] text-sm font-medium leading-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
              Loading organisation details...
            </div>
          ) : null}

          {isOrganisationDetailsError ? (
            <div className="w-full rounded-[1rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-4 text-(--status-denied) text-sm font-medium leading-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
              {getApiErrorMessage(organisationDetailsError)}
            </div>
          ) : null}

          {tab === "overview" ? (
            <OverviewTab
              org={org}
              organisationId={organisationId}
              onViewAllBilling={() => setTab("billing")}
              onManageSubscription={handleManageSubscriptionFromOverview}
              onUpdateGeneralInfo={handleUpdateGeneralInfo}
              isUpdatingGeneralInfo={isUpdatingGeneralInfo}
            />
          ) : tab === "feature-overrides" ? (
            <>
              {isFeaturesLoading ? (
                <div className="shrink-0 rounded-[0.75rem] border border-[#e3ebf3] bg-white px-4 py-3 text-sm text-[#667483]">
                  Loading feature state...
                </div>
              ) : null}
              {isFeaturesError ? (
                <div className="shrink-0 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-sm text-(--status-denied)">
                  {getApiErrorMessage(featuresError)}
                </div>
              ) : null}

              <div className="shrink-0">
                <PageHeader
                  onDiscard={handleDiscardFeatureOverrides}
                  onSave={handleSaveFeatureOverrides}
                  isSaving={isSavingFeatures}
                />
              </div>

              <div
                className="min-h-0 flex-1 overflow-y-auto pb-6"
                style={{
                  scrollBehavior: "smooth",
                  overscrollBehavior: "contain",
                }}
              >
                {renderTabContent(tab, org, organisationId, {
                  isOpen: isAssignAddOnModalOpen,
                  onOpenChange: setIsAssignAddOnModalOpen,
                }, {
                  moduleRows,
                  limitRows,
                  onDiscard: handleDiscardFeatureOverrides,
                  onSave: handleSaveFeatureOverrides,
                  isSaving: isSavingFeatures,
                  onModuleRowChange: handleModuleRowChange,
                  onUsageLimitRowChange: handleUsageLimitRowChange,
                }, handleManageSubscriptionFromOverview)}
              </div>
            </>
          ) : (
            <>
              {renderTabContent(tab, org, organisationId, {
                isOpen: isAssignAddOnModalOpen,
                onOpenChange: setIsAssignAddOnModalOpen,
              }, {
                moduleRows,
                limitRows,
                onDiscard: handleDiscardFeatureOverrides,
                onSave: handleSaveFeatureOverrides,
                isSaving: isSavingFeatures,
                onModuleRowChange: handleModuleRowChange,
                onUsageLimitRowChange: handleUsageLimitRowChange,
              }, handleManageSubscriptionFromOverview)}
            </>
          )}
        </div>
      </div>
      {isPlanManageModalOpen ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-[35rem] overflow-hidden rounded-[1rem] border border-[#e3ebf3] bg-white p-5 shadow-[0_20px_60px_rgba(15,23,42,0.22)]">
            <h3 className="text-[#1f2d38] text-[1.125rem] font-semibold leading-6">Update Plan</h3>
            <p
              className="mt-2 truncate text-[#667483] text-sm leading-5"
              title={org.name}
            >
              {org.name}
            </p>

            {isSubscriptionLoading ? (
              <div className="mt-4 text-sm text-[#667483]">Loading subscription...</div>
            ) : null}

            <div className="mt-6 space-y-5">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="min-w-0 space-y-1.5">
                  <Label className="text-[0.8125rem] font-semibold text-[#475467]">Current Plan</Label>
                  <Input
                    value={currentPlanLabel}
                    disabled
                    title={currentPlanLabel}
                    placeholder="Current Plan"
                    className="h-10 truncate rounded-[0.75rem] border-[#dce5ee] bg-[#f8fafc] text-[#667483]"
                  />
                </div>
                <div className="min-w-0 space-y-1.5">
                  <Label className="text-[0.8125rem] font-semibold text-[#475467]">Target Plan</Label>
                  <Select value={planValue || undefined} onValueChange={handleTargetPlanChange}>
                    <SelectTrigger
                      className="h-10 min-w-0 rounded-[0.75rem] border-[#dce5ee] transition-all hover:border-[#c9d6e3] [&>span]:truncate"
                      title={selectedTargetPlanLabel}
                    >
                      <SelectValue placeholder="Target Plan" />
                    </SelectTrigger>
                    <SelectContent>
                      {plansCatalog.map((plan) => (
                        <SelectItem key={plan.planCode} value={plan.planCode}>
                          <span className="block max-w-[16.25rem] truncate" title={plan.planName}>
                            {plan.planName}
                          </span>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="min-w-0 space-y-1.5">
                  <Label className="text-[0.8125rem] font-semibold text-[#475467]">Billing Cycle</Label>
                  <Select value={billingCycleValue || undefined} disabled>
                    <SelectTrigger className="h-10 min-w-0 rounded-[0.75rem] border-[#dce5ee] bg-[#f8fafc] [&>span]:truncate">
                      <SelectValue placeholder="Select plan first" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="monthly">Monthly</SelectItem>
                      <SelectItem value="annual">Annual</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="min-w-0 space-y-1.5">
                  <Label className="text-[0.8125rem] font-semibold text-[#475467]">Trial Days</Label>
                  <Input
                    value={trialDaysValue}
                    readOnly
                    placeholder="Select plan first"
                    inputMode="numeric"
                    maxLength={PLAN_MANAGE_TRIAL_DAYS_MAX_DIGITS}
                    disabled={!planValue}
                    className="h-10 rounded-[0.75rem] border-[#dce5ee] bg-[#f8fafc]"
                  />
                </div>
              </div>
            </div>

            <div className="mt-5 flex items-center justify-end gap-3">
              <Button
                variant="secondary"
                size="lg"
                onClick={() => setIsPlanManageModalOpen(false)}
                disabled={isUpdatingSubscription}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                size="lg"
                onClick={handleUpdatePlan}
                disabled={isUpdatingSubscription}
                loading={isUpdatingSubscription}
                loadingLabel="Saving..."
              >
                Save
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

export default OrganisationDetails;
