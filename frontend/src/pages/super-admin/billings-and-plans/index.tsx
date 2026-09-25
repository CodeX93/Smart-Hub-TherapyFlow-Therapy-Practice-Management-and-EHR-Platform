import { useEffect, useMemo, useState, type ReactNode } from "react";
import {
  Activity,
  ArrowUp,
  Building2,
  Download,
  Plus,
  Rocket,
  DollarSign,
  TrendingUp,
  UsersRound,
} from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import Toast from "@/components/shared/Toast";
import BillingPlansTabs, {
  type BillingPlansTab,
} from "./components/BillingPlansTabs";
import MetricCard from "./components/MetricCard";
import PastDueInvoicesTable from "./components/PastDueInvoicesTable";
import PlanCard from "./components/PlanCard";
import AllInvoicesPanel from "./components/AllInvoicesPanel";
import PlansPricingPanel from "./components/PlansPricingPanel";
import BillingSettingsPanel from "./components/BillingSettingsPanel";
import AddOnsPanel from "./components/AddOnsPanel";
import CreateAddOnModal from "./components/CreateAddOnModal";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import { clearBillingToast, getBillingToast, stashBillingToast } from "./billingToast.utils";
import {
  useArchivePlanMutation,
  useDeleteBillingPlanMutation,
  useGetBillingPlansQuery,
  useGetRevenueAnalyticsQuery,
  useUnarchivePlanMutation,
  type BillingPlan,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

interface OverviewMetric {
  label: string;
  value: string;
  subtext?: string;
  icon: ReactNode;
}

interface RevenueSummaryMetrics {
  mrr: string;
  arr: string;
  churn: string;
  churnChangeText: string;
  activeSubscriptions: string;
  activeSubscriptionsSubtext: string;
}

interface OverviewPlan {
  title: string;
  statusLabel: string;
  statusVariant: "green" | "gray";
  price: string;
  priceSuffix: string;
  icon: ReactNode;
  metrics: Array<{
    label: string;
    value: string;
  }>;
  rawPlan: BillingPlan;
}

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0,
});

function formatCurrency(value: number): string {
  return currencyFormatter.format(value);
}

function formatCompactCurrency(value: number): string {
  if (Math.abs(value) >= 1_000_000) {
    return `${currencyFormatter.format(value / 1_000_000).replace(".00", "")}M`;
  }

  if (Math.abs(value) >= 1_000) {
    return `${currencyFormatter.format(value / 1_000).replace(".00", "")}K`;
  }

  return formatCurrency(value);
}

function formatPercent(value: number): string {
  return `${value.toFixed(1)}%`;
}

function getOverviewMetrics(metrics: RevenueSummaryMetrics): OverviewMetric[] {
  return [
    {
      label: "Monthly Recurring Revenue",
      value: metrics.mrr,
      icon: <DollarSign size={18} strokeWidth={1.8} aria-hidden="true" />,
    },
    {
      label: "Annual Recurring Revenue",
      value: metrics.arr,
      icon: <TrendingUp size={16} strokeWidth={1.8} aria-hidden="true" />,
    },
    {
      label: "Churn Rate (30D)",
      value: metrics.churn,
      subtext: metrics.churnChangeText,
      icon: <Activity size={16} strokeWidth={1.8} aria-hidden="true" />,
    },
    {
      label: "Active Subs",
      value: metrics.activeSubscriptions,
      subtext: metrics.activeSubscriptionsSubtext,
      icon: <ArrowUp size={16} strokeWidth={1.8} aria-hidden="true" />,
    },
  ];
}

function formatPlanPrice(plan: BillingPlan): { price: string; priceSuffix: string } {
  const cycleSuffix = plan.billingCycle?.toLowerCase().includes("year") ? "/yr" : "/mo";

  if (typeof plan.basePrice === "number" && Number.isFinite(plan.basePrice) && plan.basePrice > 0) {
    return {
      price: formatCurrency(plan.basePrice),
      priceSuffix: cycleSuffix,
    };
  }

  const firstTier = plan.pricingTiers[0];
  if (firstTier && firstTier.pricePerTherapistUsd > 0) {
    return {
      price: formatCurrency(firstTier.pricePerTherapistUsd),
      priceSuffix: "/user / mo",
    };
  }

  return { price: "-", priceSuffix: "" };
}

function getPlanIcon(plan: BillingPlan): ReactNode {
  const normalizedName = `${plan.planCode} ${plan.planName}`.toLowerCase();
  if (normalizedName.includes("trial")) return <Rocket size={18} aria-hidden="true" />;
  if (normalizedName.includes("pro")) return <UsersRound size={18} aria-hidden="true" />;
  return <Building2 size={18} aria-hidden="true" />;
}

function getPlanStatusVariant(status: string): "green" | "gray" {
  return status.toLowerCase().includes("default") ? "gray" : "green";
}

function getOverviewPlans(plans: BillingPlan[]): OverviewPlan[] {
  function formatLimitValue(value: number | null): string {
    return value === null ? "-" : value.toLocaleString("en-US");
  }

  const visiblePlans = plans.filter((plan) => !plan.status?.toLowerCase().includes("archive"));
  return visiblePlans.slice(0, 3).map(function (plan) {
    const formattedPrice = formatPlanPrice(plan);

    return {
      title: plan.planName || plan.planCode,
      statusLabel: plan.status || "Active",
      statusVariant: getPlanStatusVariant(plan.status || "Active"),
      price: formattedPrice.price,
      priceSuffix: formattedPrice.priceSuffix,
      icon: getPlanIcon(plan),
      metrics: [
        {
          label: "Included Therapists",
          value: formatLimitValue(plan.therapistLimit),
        },
        {
          label: "Included Supervisors",
          value: formatLimitValue(plan.supervisorLimit),
        },
        {
          label: "Included Clients",
          value: formatLimitValue(plan.clientLimit),
        },
        { label: "Trial Period", value: plan.trialDays > 0 ? `${plan.trialDays} Days` : "-" },
        {
          label: "Billing Cycle",
          value: plan.billingCycle?.trim()
            ? plan.billingCycle.trim().charAt(0).toUpperCase() +
              plan.billingCycle.trim().slice(1).toLowerCase()
            : "-",
        },
      ],
      rawPlan: plan,
    };
  });
}

function OverviewPanel(props: {
  onOpenInvoices(): void;
  onCreatePlan(): void;
  onEditPlan(plan: BillingPlan): void;
  onEditEntitlements(plan: BillingPlan): void;
  metrics: RevenueSummaryMetrics;
  isAnalyticsLoading?: boolean;
  plans: BillingPlan[];
  isPlansLoading?: boolean;
  errorMessage?: string | null;
  plansErrorMessage?: string | null;
}) {
  const overviewPlans = getOverviewPlans(props.plans);

  return (
    <div className="flex w-full flex-col gap-4">
      {props.errorMessage ? (
        <div className="rounded-[0.875rem] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {props.errorMessage}
        </div>
      ) : null}
      {props.isAnalyticsLoading ? (
        <div className="rounded-[0.875rem] border border-[#e3ebf3] bg-white px-4 py-3 text-sm text-[#667483]">
          Loading revenue analytics...
        </div>
      ) : null}

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
        {getOverviewMetrics(props.metrics).map(function (metric) {
          return (
            <MetricCard
              key={metric.label}
              label={metric.label}
              value={metric.value}
              subtext={metric.subtext}
              icon={metric.icon}
            />
          );
        })}
      </div>

      <PastDueInvoicesTable onViewAllInvoices={props.onOpenInvoices} />

      <section className="flex flex-col gap-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="text-base font-semibold leading-6 text-[#1f2d38]">
            Active Plans
          </div>

          <Button variant="primary" size="md" onClick={props.onCreatePlan}>
            <Plus size={16} aria-hidden="true" />
            Create Plan
          </Button>
        </div>

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
          {props.isPlansLoading ? (
            <div className="text-sm text-[#667483]">Loading plans...</div>
          ) : null}
          {props.plansErrorMessage ? (
            <div className="text-sm text-red-600">{props.plansErrorMessage}</div>
          ) : null}
          {overviewPlans.map(function (plan) {
            return (
              <PlanCard
                key={plan.title}
                title={plan.title}
                statusLabel={plan.statusLabel}
                statusVariant={plan.statusVariant}
                price={plan.price}
                priceSuffix={plan.priceSuffix}
                icon={plan.icon}
                metrics={plan.metrics}
                onEditTiers={() => props.onEditPlan(plan.rawPlan)}
                onEditEntitlements={() => props.onEditEntitlements(plan.rawPlan)}
              />
            );
          })}
        </div>
      </section>
    </div>
  );
}

function PlansPanel(props: {
  plans: BillingPlan[];
  isLoading?: boolean;
  processingPlanCode?: string | null;
  onEditEntitlements(plan: BillingPlan): void;
  onArchivePlan(plan: BillingPlan): void;
  onUnarchivePlan(plan: BillingPlan): void;
  onDeletePlan(plan: BillingPlan): void;
  errorMessage?: string | null;
}) {
  return (
    <div className="flex w-full flex-col gap-5">
      <div className="text-(--text-gray-900) text-base font-semibold leading-6">
        Subscription Plans
      </div>

      <PlansPricingPanel
        plans={props.plans}
        isLoading={props.isLoading}
        processingPlanCode={props.processingPlanCode}
        onEditEntitlements={props.onEditEntitlements}
        onArchivePlan={props.onArchivePlan}
        onUnarchivePlan={props.onUnarchivePlan}
        onDeletePlan={props.onDeletePlan}
        errorMessage={props.errorMessage}
      />
    </div>
  );
}

function BillingsAndPlans() {
  const navigate = useNavigate();
  const location = useLocation();
  const [activeTab, setActiveTab] = useState<BillingPlansTab>("overview");
  const [plansCatalog, setPlansCatalog] = useState<BillingPlan[]>([]);
  const [processingPlanCode, setProcessingPlanCode] = useState<string | null>(null);
  const [planToArchive, setPlanToArchive] = useState<BillingPlan | null>(null);
  const [planToDelete, setPlanToDelete] = useState<BillingPlan | null>(null);
  const [createAddOnOpen, setCreateAddOnOpen] = useState(false);
  const {
    data: plansData,
    isLoading: isPlansLoading,
    isError: isPlansError,
    error: plansError,
    refetch: refetchPlans,
  } = useGetBillingPlansQuery();
  const [archivePlan] = useArchivePlanMutation();
  const [unarchivePlan] = useUnarchivePlanMutation();
  const [deletePlan] = useDeleteBillingPlanMutation();
  const {
    data: analyticsData,
    isLoading: isAnalyticsLoading,
    isError: isAnalyticsError,
    error: analyticsError,
  } = useGetRevenueAnalyticsQuery({ months: 12 });
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("success");

  const overviewMetrics = useMemo<RevenueSummaryMetrics>(() => {
    if (!analyticsData?.rows?.length) {
      return {
        mrr: "—",
        arr: "—",
        churn: "—",
        churnChangeText: "",
        activeSubscriptions: "—",
        activeSubscriptionsSubtext: "",
      };
    }

    const rows = [...analyticsData.rows].sort((left, right) =>
      left.month.localeCompare(right.month)
    );
    const latestRow = rows.at(-1);
    const previousRow = rows.at(-2);

    if (!latestRow) {
      return {
        mrr: "$0",
        arr: "$0",
        churn: "0.0%",
        churnChangeText: "0.0% change",
        activeSubscriptions: "0",
        activeSubscriptionsSubtext: "0 ended this month",
      };
    }

    const churnChange =
      latestRow.churnRatePct - (previousRow?.churnRatePct ?? latestRow.churnRatePct);

    return {
      mrr: formatCurrency(latestRow.mrr),
      arr: formatCompactCurrency(latestRow.arr),
      churn: formatPercent(latestRow.churnRatePct),
      churnChangeText: `${churnChange >= 0 ? "+" : ""}${churnChange.toFixed(1)}% vs prev`,
      activeSubscriptions: latestRow.activeSubscriptions.toString(),
      activeSubscriptionsSubtext: `${latestRow.endedSubscriptions} ended this month`,
    };
  }, [analyticsData]);

  useEffect(() => {
    const storedToast = getBillingToast();
    if (storedToast) {
      setToastMessage(storedToast.message);
      setToastType(storedToast.type);
    }

    const navState = location.state as
      | {
          initialTab?: BillingPlansTab;
          toastMessage?: string;
          toastType?: "success" | "error" | "info";
        }
      | null;
    const requestedTab = navState?.initialTab;
    if (requestedTab) {
      setActiveTab(requestedTab);
    }
    if (!storedToast && navState?.toastMessage) {
      stashBillingToast(navState.toastMessage, navState.toastType ?? "success");
      setToastMessage(navState.toastMessage);
      setToastType(navState.toastType ?? "success");
    }
    if (navState?.initialTab || navState?.toastMessage) {
      navigate(location.pathname, { replace: true });
    }
  }, [location.pathname, location.state, navigate]);

  useEffect(() => {
    if (plansData) {
      setPlansCatalog(plansData);
    }
  }, [plansData]);

  function handleCreatePlan() {
    navigate("/super-admin/billings-and-plans/create-plan", {
      state: { initialTab: activeTab },
    });
  }

  function handleEditPlan(plan: BillingPlan) {
    navigate("/super-admin/billings-and-plans/create-plan", {
      state: {
        mode: "edit",
        initialTab: activeTab,
        planName: plan.planName || plan.planCode,
        planCode: plan.planCode,
        description: plan.description || "",
        billingCycle: plan.billingCycle || "Monthly",
        basePriceUsd: String(plan.basePrice || 0),
        trialDays: String(plan.trialDays || 0),
        status: plan.status?.toLowerCase().includes("archive") ? "Draft" : "Active",
      },
    });
  }

  function handleEditEntitlements(plan: BillingPlan) {
    const returnTab: BillingPlansTab = activeTab === "overview" ? "overview" : "plans";
    navigate("/super-admin/billings-and-plans/plan-entitlements", {
      state: {
        initialTab: returnTab,
        planName: plan.planName || plan.planCode,
        planCode: plan.planCode,
      },
    });
  }

  function handleArchivePlan(plan: BillingPlan) {
    setPlanToArchive(plan);
  }

  function handleUnarchivePlan(plan: BillingPlan) {
    setPlanToArchive(plan);
  }

  async function confirmArchivePlan() {
    if (!planToArchive) return;

    setProcessingPlanCode(planToArchive.planCode);

    try {
      const isArchived = planToArchive.status?.toLowerCase().includes("archive");
      if (isArchived) {
        await unarchivePlan(planToArchive.planCode).unwrap();
        setToastMessage("Plan unarchived successfully.");
      } else {
        await archivePlan(planToArchive.planCode).unwrap();
        setToastMessage("Plan archived successfully.");
      }
      await refetchPlans();
    } finally {
      setProcessingPlanCode(null);
      setPlanToArchive(null);
    }
  }

  async function handleDeletePlan(plan: BillingPlan) {
    setPlanToDelete(plan);
  }

  async function confirmDeletePlan() {
    if (!planToDelete) return;
    setProcessingPlanCode(planToDelete.planCode);
    try {
      await deletePlan(planToDelete.planCode).unwrap();
      setToastType("success");
      setToastMessage("Plan deleted successfully.");
      await refetchPlans();
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setProcessingPlanCode(null);
      setPlanToDelete(null);
    }
  }

  function renderTabContent() {
    if (activeTab === "overview") {
      return (
        <OverviewPanel
          onOpenInvoices={function () {
            setActiveTab("invoices");
          }}
          onCreatePlan={handleCreatePlan}
          onEditPlan={handleEditPlan}
          onEditEntitlements={handleEditEntitlements}
          metrics={overviewMetrics}
          isAnalyticsLoading={isAnalyticsLoading}
          plans={plansCatalog}
          isPlansLoading={isPlansLoading}
          errorMessage={isAnalyticsError ? getApiErrorMessage(analyticsError) : null}
          plansErrorMessage={isPlansError ? getApiErrorMessage(plansError) : null}
        />
      );
    }

    if (activeTab === "invoices") {
      return <AllInvoicesPanel />;
    }

    if (activeTab === "plans") {
      return (
        <PlansPanel
          plans={plansCatalog}
          isLoading={isPlansLoading}
          processingPlanCode={processingPlanCode}
          onEditEntitlements={handleEditEntitlements}
          onArchivePlan={handleArchivePlan}
          onUnarchivePlan={handleUnarchivePlan}
          onDeletePlan={handleDeletePlan}
          errorMessage={isPlansError ? getApiErrorMessage(plansError) : null}
        />
      );
    }

    if (activeTab === "add-ons") {
      return <AddOnsPanel />;
    }

    return <BillingSettingsPanel />;
  }

  return (
    <SuperAdminPageShell
      title="Billing & Plans"
      description="Manage platform revenue, active plans, and tenant invoices."
    >
      <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <BillingPlansTabs value={activeTab} onChange={setActiveTab} />

        {activeTab === "overview" ? (
          <Button variant="secondary" size="md">
            <Download size={15} aria-hidden="true" />
            Export Revenue Report
          </Button>
        ) : activeTab === "plans" ? (
          <Button variant="primary" size="md" onClick={handleCreatePlan}>
            <Plus size={16} aria-hidden="true" />
            Create Plan
          </Button>
        ) : activeTab === "add-ons" ? (
          <Button
            variant="primary"
            size="md"
            onClick={() => setCreateAddOnOpen(true)}
          >
            <Plus size={16} aria-hidden="true" />
            Create Add-on
          </Button>
        ) : null}
      </div>

      <div className="mt-6">{renderTabContent()}</div>

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => {
            clearBillingToast();
            setToastMessage(null);
          }}
        />
      ) : null}

      <ConfirmationModal
        type="delete"
        isOpen={!!planToArchive}
        onClose={() => setPlanToArchive(null)}
        onConfirm={confirmArchivePlan}
        title={`${planToArchive?.status?.toLowerCase().includes("archive") ? "Unarchive" : "Archive"} plan "${planToArchive?.planName || planToArchive?.planCode}"?`}
        description={`Are you sure you want to ${planToArchive?.status?.toLowerCase().includes("archive") ? "unarchive" : "archive"} this plan?`}
        confirmButtonText={planToArchive?.status?.toLowerCase().includes("archive") ? "Unarchive plan" : "Archive plan"}
        confirmButtonDisabled={Boolean(processingPlanCode)}
        items={[
          "New organisations cannot subscribe to it",
          "Existing subscriptions remain active",
          "Visible under historical archives",
        ]}
      />

      <ConfirmationModal
        type="delete"
        isOpen={!!planToDelete}
        onClose={() => setPlanToDelete(null)}
        onConfirm={confirmDeletePlan}
        title={`Delete plan "${planToDelete?.planName || planToDelete?.planCode}"?`}
        description="Are you sure you want to delete this plan? This action cannot be undone."
        confirmButtonText="Delete plan"
        confirmButtonDisabled={Boolean(processingPlanCode)}
        items={[
          "New organisations cannot subscribe to it",
          "Existing subscriptions may be impacted",
          "Plan configuration will be permanently removed",
        ]}
      />

      <CreateAddOnModal open={createAddOnOpen} onClose={() => setCreateAddOnOpen(false)} />
    </SuperAdminPageShell>
  );
}

export default BillingsAndPlans;
