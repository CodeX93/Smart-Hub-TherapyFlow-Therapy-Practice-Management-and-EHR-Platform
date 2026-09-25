import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { Archive, ShieldCheck, PencilLine } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";
import { useNavigate } from "react-router-dom";
import type { BillingPlan } from "@/store/api/superAdminApi";

type PlanStatus = "Active" | "System Default" | "Archived";

interface PlanPricingRow {
  planName: string;
  code: string;
  basePrice: string;
  billingCycle: string;
  trialDays: string;
  status: PlanStatus;
  description: string;
}

function getPlanRows(): PlanPricingRow[] {
  return [
    {
      planName: "Enterprise",
      code: "Enterprise",
      basePrice: "$1200.00",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Active",
      description:
        "Comprehensive suite of tools for large organisation with advanced needs and custom integration",
    },
    {
      planName: "Professional",
      code: "Pro",
      basePrice: "$1200.00",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Active",
      description:
        "Flexible growth plan for mid-sized teams with strong reporting and workflow support",
    },
    {
      planName: "Enterprise",
      code: "Enterprise",
      basePrice: "$40.00/user",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "System Default",
      description:
        "Per-user enterprise pricing structure for high-volume account customization",
    },
    {
      planName: "Enterprise",
      code: "Trial",
      basePrice: "$1200.00",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Active",
      description:
        "Trial plan configuration for temporary onboarding and short-term access evaluation",
    },
    {
      planName: "Enterprise",
      code: "Enterprise",
      basePrice: "$1200.00",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Archived",
      description:
        "Archived enterprise configuration retained for reporting and historical plan references",
    },
    {
      planName: "Enterprise",
      code: "Pro-v1",
      basePrice: "$1200.00",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Active",
      description:
        "Legacy enterprise package with pro-level features and v1 migration support",
    },
    {
      planName: "Enterprise",
      code: "Pro-v1",
      basePrice: "$1200.00",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Active",
      description:
        "Legacy enterprise package with pro-level features and v1 migration support",
    },
    {
      planName: "Enterprise",
      code: "Pro-v1",
      basePrice: "$1200.00",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Active",
      description:
        "Legacy enterprise package with pro-level features and v1 migration support",
    },
    {
      planName: "Legacy Pro (V1)",
      code: "Trial",
      basePrice: "$0.00",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Active",
      description:
        "Legacy trial package preserved for backwards compatibility across older organizations",
    },
    {
      planName: "Enterprise",
      code: "Enterprise",
      basePrice: "$30.00/ user",
      billingCycle: "Monthly",
      trialDays: "14",
      status: "Active",
      description:
        "Scaled enterprise pricing with lower per-user cost for larger therapy teams",
    },
  ];
}

function formatPlanPrice(plan: BillingPlan): string {
  if (plan.basePrice > 0) {
    return `$${plan.basePrice.toFixed(2)}`;
  }

  const tier = plan.pricingTiers[0];
  if (tier && tier.pricePerTherapistUsd > 0) {
    return `$${tier.pricePerTherapistUsd.toFixed(2)}/user`;
  }

  return "$0.00";
}

function mapPlanRowsFromApi(plans: BillingPlan[]): PlanPricingRow[] {
  return plans.map((plan) => ({
    planName: plan.planName || plan.planCode,
    code: plan.planCode,
    basePrice: formatPlanPrice(plan),
    billingCycle: plan.billingCycle || "Monthly",
    trialDays: String(plan.trialDays || 0),
    status: plan.status?.toLowerCase().includes("default")
      ? "System Default"
      : plan.status?.toLowerCase().includes("archive")
      ? "Archived"
      : "Active",
    description: plan.description || "",
  }));
}

function getHeaderItems(): string[] {
  return [
    "Plan Name",
    "Code",
    "Base Price",
    "Billing Cycle",
    "Trial Days",
    "Status",
    "Actions",
  ];
}

function getStatusClassName(status: PlanStatus): string {
  if (status === "System Default") {
    return "bg-(--bg-primary-50) text-(--text-primary-500)";
  }

  if (status === "Archived") {
    return "bg-(--neutral-100) text-(--badge-gray-text)";
  }

  return "bg-(--status-completed-light) text-(--badge-green-text)";
}

function StatusPill(props: { status: PlanStatus }) {
  return (
    <span
      className={cn(
        "inline-flex h-5 items-center rounded-full px-2",
        "text-[0.625rem] font-medium leading-4",
        getStatusClassName(props.status)
      )}
    >
      {props.status}
    </span>
  );
}

function getMenuContentClassName(): string {
  return cn(
    "w-[11.5rem] rounded-[0.875rem] border border-[#e6edf3] bg-white p-1.5",
    "shadow-[0px_12px_24px_rgba(15,23,42,0.08)]"
  );
}

function getMenuItemClassName(isDestructive?: boolean): string {
  return cn(
    "cursor-pointer whitespace-nowrap rounded-[0.625rem] px-3 py-2 text-[0.875rem] font-medium leading-[1.375rem]",
    "[&_svg]:size-[0.9375rem] [&_svg]:text-current",
    isDestructive
      ? "text-[#dc2626] focus:bg-[#fef2f2] focus:text-[#dc2626]"
      : "text-[#1b1c20] focus:bg-[#f5f8fb] focus:text-[#1b1c20]"
  );
}

function PlanActionsMenu(props: {
  row: PlanPricingRow;
  isProcessing?: boolean;
  onEditEntitlements?: () => void;
  onArchive?: () => void;
  onUnarchive?: () => void;
  onDelete?: () => void;
}) {
  const navigate = useNavigate();

  function handleEditPlan() {
    navigate("/super-admin/billings-and-plans/create-plan", {
      state: {
        mode: "edit",
        initialTab: "plans",
        planName: props.row.planName,
        planCode: props.row.code,
        description: props.row.description,
        billingCycle: props.row.billingCycle,
        basePriceUsd: props.row.basePrice
          .replace("$", "")
          .replace("/user", "")
          .replace("/ user", "")
          .trim(),
        trialDays: props.row.trialDays,
        status: props.row.status === "Archived" ? "Draft" : "Active",
      },
    });
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex h-7 w-7 items-center justify-center rounded-full text-(--text-primary-dark) transition-colors hover:bg-(--bg-primary-50)"
          aria-label="Open plan actions"
        >
          <MenuDotsIcon size={16} aria-hidden="true" />
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="end"
        sideOffset={6}
        className={getMenuContentClassName()}
      >
        <DropdownMenuItem
          className={getMenuItemClassName()}
          onSelect={handleEditPlan}
          disabled={props.isProcessing}
        >
          <PencilLine size={14} aria-hidden="true" />
          Edit Plan Basics
        </DropdownMenuItem>
        <DropdownMenuItem
          className={getMenuItemClassName()}
          onSelect={props.onEditEntitlements}
          disabled={props.isProcessing}
        >
          <ShieldCheck size={14} aria-hidden="true" />
          Edit Entitlements
        </DropdownMenuItem>
        {props.row.status === "Archived" ? (
          <DropdownMenuItem
            className={getMenuItemClassName()}
            onSelect={props.onUnarchive}
            disabled={props.isProcessing}
          >
            <Archive size={14} aria-hidden="true" />
            {props.isProcessing ? "Unarchiving..." : "Unarchive Plan"}
          </DropdownMenuItem>
        ) : (
          <DropdownMenuItem
            className={getMenuItemClassName()}
            onSelect={props.onArchive}
            disabled={props.isProcessing}
          >
            <Archive size={14} aria-hidden="true" />
            {props.isProcessing ? "Archiving..." : "Archive Plan"}
          </DropdownMenuItem>
        )}
        <DropdownMenuSeparator className="my-1 bg-[#eef2f6]" />
        <DropdownMenuItem
          className={getMenuItemClassName(true)}
          onSelect={props.onDelete}
          disabled={props.isProcessing}
        >
          <TrashIcon size={14} aria-hidden="true" />
          {props.isProcessing ? "Deleting..." : "Delete"}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function PlansPricingPanel(props: {
  plans?: BillingPlan[];
  isLoading?: boolean;
  processingPlanCode?: string | null;
  onEditEntitlements?(plan: BillingPlan): void;
  onArchivePlan?(plan: BillingPlan): void;
  onUnarchivePlan?(plan: BillingPlan): void;
  onDeletePlan?(plan: BillingPlan): void;
  errorMessage?: string | null;
}) {
  const rows = props.plans?.length ? mapPlanRowsFromApi(props.plans) : getPlanRows();
  return (
    <div className="w-full overflow-hidden rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) shadow-[0_2px_2px_0_var(--shadow)]">
        <div className="grid grid-cols-[1.2fr_1.1fr_1.5fr_1.4fr_1fr_1.3fr_0.55fr] items-center bg-(--bg-primary-50) px-4 py-3">
          {getHeaderItems().map(function (header) {
            return (
              <div
                key={header}
                className={cn(
                  "text-(--text-gray-900) text-xs font-medium leading-4",
                  header === "Actions" ? "text-center" : ""
                )}
              >
                {header}
              </div>
            );
          })}
        </div>

        {props.isLoading ? (
          <div className="px-4 py-6 text-sm text-[#667483]">Loading plans...</div>
        ) : null}
        {props.errorMessage ? (
          <div className="px-4 py-6 text-sm text-red-600">{props.errorMessage}</div>
        ) : null}
        {rows.map(function (row, index) {
          return (
            <div
              key={row.planName + row.code + index}
              className={cn(
                "grid grid-cols-[1.2fr_1.1fr_1.5fr_1.4fr_1fr_1.3fr_0.55fr] items-center px-4 py-4",
                index === 0 ? "" : "border-t border-(--neutral-100)"
              )}
            >
              <div
                className="min-w-0 truncate text-(--text-gray-900) text-sm font-normal leading-5.5"
                title={row.planName}
              >
                {row.planName}
              </div>
              <div
                className="min-w-0 truncate text-(--text-gray-900) text-sm font-normal leading-5.5"
                title={row.code}
              >
                {row.code}
              </div>
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.basePrice}
              </div>
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.billingCycle}
              </div>
              <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
                {row.trialDays}
              </div>
              <div>
                <StatusPill status={row.status} />
              </div>
              <div className="flex justify-center">
                <PlanActionsMenu
                  row={row}
                  isProcessing={Boolean(props.processingPlanCode && props.processingPlanCode === row.code)}
                  onEditEntitlements={
                    props.onEditEntitlements && props.plans
                      ? () => {
                          const plan = props.plans?.[index];
                          if (plan) props.onEditEntitlements?.(plan);
                        }
                      : undefined
                  }
                  onArchive={
                    props.onArchivePlan && props.plans
                      ? () => {
                          const plan = props.plans?.[index];
                          if (plan) props.onArchivePlan?.(plan);
                        }
                      : undefined
                  }
                  onUnarchive={
                    props.onUnarchivePlan && props.plans
                      ? () => {
                          const plan = props.plans?.[index];
                          if (plan) props.onUnarchivePlan?.(plan);
                        }
                      : undefined
                  }
                  onDelete={
                    props.onDeletePlan && props.plans
                      ? () => {
                          const plan = props.plans?.[index];
                          if (plan) props.onDeletePlan?.(plan);
                        }
                      : undefined
                  }
                />
              </div>
            </div>
          );
        })}
    </div>
  );
}

export default PlansPricingPanel;
