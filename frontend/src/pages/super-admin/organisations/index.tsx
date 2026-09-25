import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { CreditCard, Eye, Flag, RotateCcw, Search, UserRoundCog, UserX } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import SemanticStatusBadge from "@/components/shared/SemanticStatusBadge";
import { cn } from "@/lib/utils";
import {
  useGetOrganisationsQuery,
  useGetPlansCatalogQuery,
  useGetOrganisationSubscriptionQuery,
  useReactivateOrganisationMutation,
  useSuspendOrganisationMutation,
  useTerminateOrganisationMutation,
  useUpdateOrganisationSubscriptionMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import Toast from "@/components/shared/Toast";
import { applyPlanCatalogDefaults } from "./planManage.utils";
import {
  RETENTION_DAYS_DEFAULT,
  RETENTION_DAYS_MAX,
  RETENTION_DAYS_MIN,
  sanitizeRetentionDaysInput,
  validateRetentionDaysInput,
} from "./terminate.utils";

const STATUS_OPTIONS: Array<string> = [
  "All",
  "Active",
  "Pending Activation",
  "Suspended",
];

const PLAN_OPTIONS: Array<string> = [
  "All",
  "Enterprise",
  "Pro",
  "Trial",
];

function matchesSearch(row: { name: string; slug: string }, needle: string): boolean {
  if (!needle) {
    return true;
  }

  return (
    row.name.toLowerCase().includes(needle) ||
    row.slug.toLowerCase().includes(needle)
  );
}

function matchesStatus(
  row: { status: string },
  statusFilter: string
): boolean {
  return statusFilter === "All" || row.status === statusFilter;
}

function matchesPlan(
  row: { plan: string },
  planFilter: string
): boolean {
  return planFilter === "All" || row.plan === planFilter;
}

function getRowMenuItemClassName(isDanger?: boolean): string {
  if (isDanger) {
    return cn(
      "cursor-pointer rounded-lg px-3 py-2.5 text-sm font-medium",
      "text-(--status-denied) focus:bg-(--light-red) focus:text-(--status-denied)"
    );
  }

  return cn(
    "cursor-pointer rounded-lg px-3 py-2.5 text-sm font-medium",
    "text-[#1f2d38] focus:bg-[#f4f7fa] focus:text-[#1f2d38]"
  );
}

function getRowMenuIconClassName(isDanger?: boolean): string {
  return cn(
    "h-4 w-4 shrink-0",
    isDanger ? "text-(--status-denied)" : "text-[#465563]"
  );
}

type OrganisationActionType = "suspend" | "terminate" | "reactivate";
type OrganisationSubscriptionModalType = "user-limits" | "plan-manage";

interface OrganisationActionDialogState {
  type: OrganisationActionType;
  rowId: string;
  rowName: string;
}

interface OrganisationSubscriptionDialogState {
  type: OrganisationSubscriptionModalType;
  rowId: string;
  rowName: string;
}

function shouldShowReactivate(status: string): boolean {
  const normalized = status.toLowerCase();
  return normalized.includes("suspend") || normalized.includes("terminate");
}

function isTerminationScheduled(status: string): boolean {
  return status.toLowerCase().includes("termination_scheduled");
}

const ORGANISATION_USER_LIMIT_MAX = 999_999;
const ORGANISATION_USER_LIMIT_MAX_DIGITS = String(ORGANISATION_USER_LIMIT_MAX).length;
const ORGANISATION_TRIAL_DAYS_MAX = 365;
const ORGANISATION_TRIAL_DAYS_MAX_DIGITS = String(ORGANISATION_TRIAL_DAYS_MAX).length;

function sanitizeUserLimitInput(value: string): string {
  return value.replace(/\D/g, "").slice(0, ORGANISATION_USER_LIMIT_MAX_DIGITS);
}

function validateUserLimitsInput(values: {
  therapists: string;
  supervisors: string;
  clients: string;
}): string | null {
  const fields = [
    { label: "Therapist limit", value: values.therapists },
    { label: "Supervisor limit", value: values.supervisors },
    { label: "Client limit", value: values.clients },
  ];

  for (const field of fields) {
    const trimmed = field.value.trim();
    if (!trimmed) {
      return `${field.label} is required.`;
    }

    if (!/^\d+$/.test(trimmed)) {
      return `Enter a valid number for ${field.label.toLowerCase()}.`;
    }

    const parsed = Number.parseInt(trimmed, 10);
    if (
      !Number.isFinite(parsed) ||
      parsed < 0 ||
      parsed > ORGANISATION_USER_LIMIT_MAX
    ) {
      return `${field.label} must be between 0 and ${ORGANISATION_USER_LIMIT_MAX.toLocaleString()}.`;
    }
  }

  return null;
}

function validatePlanManageInput(values: {
  plan: string;
  billingCycle: string;
  trialDays: string;
}): string | null {
  const trimmedPlan = values.plan.trim();
  if (!trimmedPlan) {
    return "Target plan is required.";
  }

  const cycle = values.billingCycle.trim().toLowerCase();
  if (!cycle || (cycle !== "monthly" && cycle !== "annual")) {
    return "Billing cycle must be Monthly or Annual.";
  }

  const trimmedTrialDays = values.trialDays.trim();
  if (trimmedTrialDays) {
    if (!/^\d+$/.test(trimmedTrialDays)) {
      return "Trial days must be a whole number.";
    }
    const parsedTrial = Number.parseInt(trimmedTrialDays, 10);
    if (
      !Number.isFinite(parsedTrial) ||
      parsedTrial < 0 ||
      parsedTrial > ORGANISATION_TRIAL_DAYS_MAX
    ) {
      return `Trial days must be between 0 and ${ORGANISATION_TRIAL_DAYS_MAX}.`;
    }
  }

  return null;
}

function Organisations() {
  const navigate = useNavigate();
  const location = useLocation();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("All");
  const [planFilter, setPlanFilter] = useState("All");
  const [statusOverrides, setStatusOverrides] = useState<Record<string, string>>({});
  const [actionDialog, setActionDialog] = useState<OrganisationActionDialogState | null>(null);
  const [actionReason, setActionReason] = useState("");
  const [retentionDays, setRetentionDays] = useState(
    RETENTION_DAYS_DEFAULT,
  );
  const [actionErrorMessage, setActionErrorMessage] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [subscriptionDialog, setSubscriptionDialog] =
    useState<OrganisationSubscriptionDialogState | null>(null);
  const [limitTherapists, setLimitTherapists] = useState("");
  const [limitSupervisors, setLimitSupervisors] = useState("");
  const [limitClients, setLimitClients] = useState("");
  const [planValue, setPlanValue] = useState("");
  const [billingCycleValue, setBillingCycleValue] = useState("monthly");
  const [trialDaysValue, setTrialDaysValue] = useState("");

  const filters = useMemo(
    () => ({
      search,
      status: statusFilter === "All" ? "" : statusFilter,
      plan: planFilter === "All" ? "" : planFilter,
      createdFrom: "",
      createdTo: "",
      region: "",
      dataResidency: "",
      page: 1,
      pageSize: 25,
      sort: "createdAt",
      order: "desc" as const,
      exportData: false,
    }),
    [planFilter, search, statusFilter]
  );

  const { data, isLoading, isError, error, refetch } = useGetOrganisationsQuery(filters);
  const [suspendOrganisation, { isLoading: isSuspending }] = useSuspendOrganisationMutation();
  const [terminateOrganisation, { isLoading: isTerminating }] = useTerminateOrganisationMutation();
  const [reactivateOrganisation, { isLoading: isReactivating }] =
    useReactivateOrganisationMutation();
  const { data: plansCatalog = [] } = useGetPlansCatalogQuery();
  const [updateOrganisationSubscription, { isLoading: isUpdatingSubscription }] =
    useUpdateOrganisationSubscriptionMutation();
  const isActionSubmitting = isSuspending || isTerminating || isReactivating;
  const subscriptionOrganisationId = subscriptionDialog
    ? Number.parseInt(subscriptionDialog.rowId, 10)
    : NaN;
  const {
    currentData: organisationSubscription,
    isLoading: isSubscriptionLoading,
  } = useGetOrganisationSubscriptionQuery(subscriptionOrganisationId, {
    skip: !subscriptionDialog || Number.isNaN(subscriptionOrganisationId),
  });

  const draftKey = subscriptionDialog ? `${subscriptionOrganisationId}:${subscriptionDialog.type}` : null;
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (!subscriptionDialog && seededDraftKey !== null) setSeededDraftKey(null);
  if (organisationSubscription && subscriptionDialog && seededDraftKey !== draftKey) {
    setSeededDraftKey(draftKey);
    setLimitTherapists(
      sanitizeUserLimitInput(
        String(organisationSubscription.userLimits.therapistLimit ?? ""),
      ),
    );
    setLimitSupervisors(
      sanitizeUserLimitInput(
        String(organisationSubscription.userLimits.supervisorLimit ?? ""),
      ),
    );
    setLimitClients(
      sanitizeUserLimitInput(String(organisationSubscription.userLimits.clientLimit ?? "")),
    );
    setPlanValue("");
    setBillingCycleValue("");
    setTrialDaysValue("");
  }

  function handleTargetPlanChange(nextPlanCode: string) {
    setPlanValue(nextPlanCode);
    const plan = plansCatalog.find((item) => item.planCode === nextPlanCode);
    const defaults = applyPlanCatalogDefaults(plan);
    setBillingCycleValue(defaults.billingCycle);
    setTrialDaysValue(defaults.trialDays);
  }

  const filteredRows = useMemo(() => {
    const rows = (data?.rows ?? []).map(function (row) {
      return {
        ...row,
        status: statusOverrides[row.id] ?? row.status,
      };
    });
    const needle = search.trim().toLowerCase();
    return rows.filter(
      (row) =>
        matchesSearch(row, needle) &&
        matchesStatus(row, statusFilter) &&
        matchesPlan(row, planFilter)
    );
  }, [data?.rows, planFilter, search, statusFilter, statusOverrides]);

  function handleViewDetails(row: { id: string; slug: string }) {
    const id = Number.parseInt(row.id, 10);
    navigate("/super-admin/organisations/" + row.slug, {
      state: {
        organisationId: Number.isNaN(id) ? null : id,
        organisationSlug: row.slug,
      },
    });
  }

  function handleFeatureToggles(row: { id: string; slug: string }) {
    const id = Number.parseInt(row.id, 10);
    navigate("/super-admin/organisations/" + row.slug + "/feature-toggles", {
      state: {
        organisationId: Number.isNaN(id) ? null : id,
        organisationSlug: row.slug,
      },
    });
  }

  function handleManagePlanAndBilling(row: { id: string; slug: string }) {
    const id = Number.parseInt(row.id, 10);
    navigate("/super-admin/organisations/" + row.slug, {
      state: {
        organisationId: Number.isNaN(id) ? null : id,
        organisationSlug: row.slug,
        initialTab: "billing",
      },
    });
  }

  function openSubscriptionDialog(
    type: OrganisationSubscriptionModalType,
    row: { id: string; name: string }
  ) {
    setSubscriptionDialog({ type, rowId: row.id, rowName: row.name });
  }

    const navState = location.state as
      | {
          openSubscriptionModal?: {
            type?: OrganisationSubscriptionModalType;
            organisationId?: number | null;
            organisationName?: string;
          };
          shouldRefetch?: boolean;
          toastMessage?: string;
          toastType?: "success" | "error" | "info";
        }
      | null;

  const [handledNavigation, setHandledNavigation] = useState<unknown>(null);
  if (navState && navState !== handledNavigation) {
    setHandledNavigation(navState);
    const request = navState.openSubscriptionModal;
    if (request?.type && typeof request.organisationId === "number") {
      setSubscriptionDialog({ type: request.type, rowId: String(request.organisationId), rowName: request.organisationName || "Organisation" });
    } else if (navState.toastMessage) {
      setToastMessage(navState.toastMessage);
      setToastType(navState.toastType ?? "success");
    }
  }
  useEffect(() => {
    if (!navState) return;
    if (navState.shouldRefetch) void refetch();
    if (navState.openSubscriptionModal || navState.shouldRefetch || navState.toastMessage) navigate(location.pathname, { replace: true, state: null });
  }, [navState, location.pathname, navigate, refetch]);

  function closeSubscriptionDialog() {
    setSubscriptionDialog(null);
  }

  async function handleConfirmSubscriptionUpdate() {
    if (!subscriptionDialog) return;
    const isUserLimits = subscriptionDialog.type === "user-limits";
    const parsedId = Number.parseInt(subscriptionDialog.rowId, 10);
    if (Number.isNaN(parsedId)) {
      const message = "Invalid organisation id.";
      if (isUserLimits) {
        setToastType("error");
        setToastMessage(message);
      }
      return;
    }

    try {
      if (isUserLimits) {
        const validationError = validateUserLimitsInput({
          therapists: limitTherapists,
          supervisors: limitSupervisors,
          clients: limitClients,
        });
        if (validationError) {
          setToastType("error");
          setToastMessage(validationError);
          return;
        }

        await updateOrganisationSubscription({
          id: parsedId,
          body: {
            userLimits: {
              therapistLimit: Number.parseInt(limitTherapists, 10),
              supervisorLimit: Number.parseInt(limitSupervisors, 10),
              clientLimit: Number.parseInt(limitClients, 10),
            },
          },
        }).unwrap();

        closeSubscriptionDialog();
        await refetch();
        setToastType("success");
        setToastMessage(`User limits updated for ${subscriptionDialog.rowName}.`);
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

      await updateOrganisationSubscription({
        id: parsedId,
        body: {
          plan: planValue || undefined,
          billingCycle: billingCycleValue ? billingCycleValue.toLowerCase() : undefined,
          trialDays: trialDaysValue ? Number.parseInt(trialDaysValue, 10) : undefined,
          prorate: false,
        },
      }).unwrap();
      closeSubscriptionDialog();
      await refetch();
      setToastType("success");
      setToastMessage(`Plan updated for ${subscriptionDialog.rowName}.`);
    } catch (error) {
      const message = getApiErrorMessage(error);
      setToastType("error");
      setToastMessage(message);
    }
  }

  function handleCreateOrganisation() {
    navigate("/super-admin/organisations/new");
  }

  function openActionDialog(
    type: OrganisationActionType,
    row: { id: string; name: string }
  ) {
    setActionErrorMessage(null);
    setActionReason("");
    setRetentionDays(RETENTION_DAYS_DEFAULT);
    setActionDialog({ type, rowId: row.id, rowName: row.name });
  }

  function closeActionDialog() {
    setActionDialog(null);
    setActionReason("");
    setRetentionDays(RETENTION_DAYS_DEFAULT);
  }

  async function handleConfirmAction() {
    if (!actionDialog) return;

    const parsedId = Number.parseInt(actionDialog.rowId, 10);
    if (Number.isNaN(parsedId)) {
      setActionErrorMessage("Invalid organisation id.");
      return;
    }

    if (actionDialog.type === "terminate") {
      const retentionDaysError = validateRetentionDaysInput(retentionDays);
      if (retentionDaysError) {
        setActionErrorMessage(retentionDaysError);
        return;
      }
    }

    try {
      setActionErrorMessage(null);

      if (actionDialog.type === "suspend") {
        await suspendOrganisation({
          id: parsedId,
          body: { reason: actionReason.trim() || "Suspended by super admin" },
        }).unwrap();
        setStatusOverrides((previous) => ({
          ...previous,
          [actionDialog.rowId]: "Suspended",
        }));
      }

      if (actionDialog.type === "terminate") {
        const parsedRetentionDays = Number.parseInt(retentionDays, 10);
        await terminateOrganisation({
          id: parsedId,
          body: {
            reason: actionReason.trim() || "Terminated by super admin",
            retentionDays: Number.isNaN(parsedRetentionDays)
              ? RETENTION_DAYS_MIN
              : parsedRetentionDays,
          },
        }).unwrap();
        setStatusOverrides((previous) => ({
          ...previous,
          [actionDialog.rowId]: "termination_scheduled",
        }));
      }

      if (actionDialog.type === "reactivate") {
        await reactivateOrganisation({
          id: parsedId,
          body: { reason: actionReason.trim() || "Reactivated by super admin" },
        }).unwrap();
        setStatusOverrides((previous) => ({
          ...previous,
          [actionDialog.rowId]: "Active",
        }));
      }

      closeActionDialog();
      await refetch();
      setStatusOverrides((previous) => {
        const next = { ...previous };
        delete next[actionDialog.rowId];
        return next;
      });
      setToastType("success");
      setToastMessage(
        actionDialog.type === "suspend"
          ? `${actionDialog.rowName} suspended successfully.`
          : actionDialog.type === "terminate"
            ? `Termination scheduled for ${actionDialog.rowName}.`
            : `${actionDialog.rowName} reactivated successfully.`,
      );
    } catch (actionError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(actionError));
      setActionErrorMessage(getApiErrorMessage(actionError));
    }
  }

  const actionDialogTitle = actionDialog
    ? actionDialog.type === "suspend"
      ? "Suspend Tenant"
      : actionDialog.type === "terminate"
        ? "Terminate Tenant"
        : "Reactivate Tenant"
    : "";

  const retentionDaysError =
    actionDialog?.type === "terminate"
      ? validateRetentionDaysInput(retentionDays)
      : null;

  const actionDialogConfirmLabel = actionDialog
    ? actionDialog.type === "suspend"
      ? "Suspend"
      : actionDialog.type === "terminate"
        ? "Terminate"
        : "Reactivate"
    : "";

  const organisationsToolbar = (
    <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
        <div className="relative w-full lg:w-[21.875rem]">
          <Search
 className="size-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-[#667483]"
 aria-hidden="true" />
          <Input
            value={search}
            onChange={function (event) {
              setSearch(event.target.value);
            }}
            placeholder="Search by name or slug..."
            className={cn(
              "h-11 rounded-full border-[#dce5ee] bg-white pl-10 pr-4 shadow-none",
              "placeholder:text-[#97a4b0] text-[#21303d]"
            )}
          />
        </div>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <Select
            value={statusFilter}
            onValueChange={function (value) {
              setStatusFilter(value);
            }}
          >
            <SelectTrigger
              className={cn(
                "h-11 w-full rounded-full border-[#dce5ee] bg-white px-4 shadow-none sm:w-[8.125rem]",
                "text-[#52606d] [&_svg]:text-[#97a4b0]"
              )}
            >
              <SelectValue placeholder="Status: All" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {STATUS_OPTIONS.map(function (status) {
                return (
                  <SelectItem
                    key={status}
                    value={status}
                    className="text-[#1f2d38] focus:bg-[#f4f7fa] focus:text-[#1f2d38]"
                  >
                    {"Status: " + status}
                  </SelectItem>
                );
              })}
            </SelectContent>
          </Select>

          <Select
            value={planFilter}
            onValueChange={function (value) {
              setPlanFilter(value);
            }}
          >
            <SelectTrigger
              className={cn(
                "h-11 w-full rounded-full border-[#dce5ee] bg-white px-4 shadow-none sm:w-[6.875rem]",
                "text-[#52606d] [&_svg]:text-[#97a4b0]"
              )}
            >
              <SelectValue placeholder="Plan: All" />
            </SelectTrigger>
            <SelectContent className="rounded-[0.875rem] border border-[#dce5ee] bg-white shadow-[0_12px_28px_rgba(15,23,42,0.08)]">
              {PLAN_OPTIONS.map(function (plan) {
                return (
                  <SelectItem
                    key={plan}
                    value={plan}
                    className="text-[#1f2d38] focus:bg-[#f4f7fa] focus:text-[#1f2d38]"
                  >
                    {"Plan: " + plan}
                  </SelectItem>
                );
              })}
            </SelectContent>
          </Select>
        </div>
      </div>

      <Button
        className={cn(
          "h-11 rounded-full bg-[#435564] px-5 text-sm font-semibold leading-5 text-white shadow-none",
          "hover:bg-[#394957]"
        )}
        onClick={handleCreateOrganisation}
      >
        Create Organization
      </Button>
    </div>
  );

  return (
    <SuperAdminPageShell
      title="Organisations"
      description="Manage all tenants, view their status, and control plans."
      toolbar={organisationsToolbar}
    >
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      {actionErrorMessage && !actionDialog ? (
        <div className="rounded-[0.875rem] border border-[#f7d3d7] bg-[#fff4f5] px-4 py-3 text-[0.8125rem] font-medium text-[#b42318]">
          {actionErrorMessage}
        </div>
      ) : null}

      <div className="overflow-x-auto overflow-hidden rounded-[1rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        <div className="grid min-w-[47.5rem] grid-cols-[1.7fr_1.1fr_0.9fr_0.7fr_1fr_4.5rem] items-center bg-[#f5f8fb] px-5 py-4">
          <div className="text-[#23313d] text-[0.75rem] font-semibold leading-4.5">
            Name
          </div>
          <div className="text-[#23313d] text-[0.75rem] font-semibold leading-4.5">
            Status
          </div>
          <div className="text-[#23313d] text-[0.75rem] font-semibold leading-4.5">
            Plan
          </div>
          <div className="text-[#23313d] text-[0.75rem] font-semibold leading-4.5">
            Users
          </div>
          <div className="text-[#23313d] text-[0.75rem] font-semibold leading-4.5">
            Created at
          </div>
          <div className="text-right text-[#23313d] text-[0.75rem] font-semibold leading-4.5">
            Actions
          </div>
        </div>

        <div className="w-full divide-y divide-[#edf2f7]">
          {isLoading ? (
            <div className="px-5 py-8 text-center text-sm text-[#667483]">
              Loading organisations...
            </div>
          ) : null}

          {isError ? (
            <div className="px-5 py-8 text-center text-sm text-(--status-denied)">
              {getApiErrorMessage(error)}
            </div>
          ) : null}

          {!isLoading && !isError && filteredRows.length === 0 ? (
            <div className="px-5 py-8 text-center text-sm text-[#667483]">
              No organisations found for the current filters.
            </div>
          ) : null}

          {!isLoading && !isError
            ? filteredRows.map(function (row) {
            return (
              <div
                key={row.slug}
                className="grid min-w-[47.5rem] grid-cols-[1.7fr_1.1fr_0.9fr_0.7fr_1fr_4.5rem] items-center px-5 py-3.5"
              >
                <div className="min-w-0 flex flex-col gap-1">
                  <div
                    className="truncate text-[#1f2d38] text-sm font-medium leading-5"
                    title={row.name}
                  >
                    {row.name}
                  </div>
                  <div
                    className="truncate text-[#8a96a3] text-xs font-normal leading-4"
                    title={row.slug}
                  >
                    {row.slug}
                  </div>
                </div>

                <div>
                  <SemanticStatusBadge
                    status={row.status}
                    className={cn(
                      "inline-flex h-5 w-fit items-center justify-center rounded-full border px-2.5",
                      "text-[0.6875rem] font-medium leading-4",
                    )}
                  >
                    {row.status}
                  </SemanticStatusBadge>
                </div>

                <div
                  className="min-w-0 truncate text-[#465563] text-sm font-normal leading-5"
                  title={row.plan}
                >
                  {row.plan}
                </div>

                <div className="text-[#465563] text-sm font-normal leading-5">
                  {row.users}
                </div>

                <div className="text-[#465563] text-sm font-normal leading-5">
                  {row.createdAt}
                </div>

                <div className="flex items-center justify-end">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <button
                        type="button"
                        className="grid h-8 w-8 place-items-center rounded-full text-[#1f2d38] transition-colors hover:bg-[#f4f7fa]"
                        aria-label={"Open actions for " + row.name}
                      >
                        <MenuDotsIcon size={16} aria-hidden="true" />
                      </button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent
                      align="end"
                      sideOffset={10}
                      className={cn(
                        "w-56 rounded-xl border border-[#dce5ee] bg-white p-2",
                        "shadow-[0_12px_28px_rgba(15,23,42,0.08)]"
                      )}
                    >
                      <DropdownMenuItem
                        className={getRowMenuItemClassName()}
                        onSelect={function () {
                          handleViewDetails(row);
                        }}
                      >
                        <Eye
                          className={getRowMenuIconClassName()}
                          aria-hidden="true"
                        />
                        View Details
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className={getRowMenuItemClassName()}
                        onSelect={function () {
                          openSubscriptionDialog("user-limits", row);
                        }}
                      >
                        <UserRoundCog
                          className={getRowMenuIconClassName()}
                          aria-hidden="true"
                        />
                        Manage User Limits
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className={getRowMenuItemClassName()}
                        onSelect={function () {
                          openSubscriptionDialog("plan-manage", row);
                        }}
                      >
                        <CreditCard
                          className={getRowMenuIconClassName()}
                          aria-hidden="true"
                        />
                        Plan Manage
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className={getRowMenuItemClassName()}
                        onSelect={function () {
                          handleManagePlanAndBilling(row);
                        }}
                      >
                        <CreditCard
                          className={getRowMenuIconClassName()}
                          aria-hidden="true"
                        />
                        Manage Plan & Billing
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className={getRowMenuItemClassName()}
                        onSelect={function () {
                          handleFeatureToggles(row);
                        }}
                      >
                        <Flag
                          className={getRowMenuIconClassName()}
                          aria-hidden="true"
                        />
                        Feature Toggles
                      </DropdownMenuItem>
                      <DropdownMenuSeparator className="-mx-2 my-2 bg-[#edf2f7]" />

                      {isTerminationScheduled(row.status) ? (
                        <DropdownMenuItem
                          className={getRowMenuItemClassName()}
                          onSelect={function () {
                            openActionDialog("suspend", row);
                          }}
                        >
                          <UserX
                            className={getRowMenuIconClassName()}
                            aria-hidden="true"
                          />
                          Suspend Tenant
                        </DropdownMenuItem>
                      ) : shouldShowReactivate(row.status) ? (
                        <DropdownMenuItem
                          className={getRowMenuItemClassName()}
                          onSelect={function () {
                            openActionDialog("reactivate", row);
                          }}
                        >
                          <RotateCcw
                            className={getRowMenuIconClassName()}
                            aria-hidden="true"
                          />
                          Reactivate Tenant
                        </DropdownMenuItem>
                      ) : (
                        <>
                          <DropdownMenuItem
                            className={getRowMenuItemClassName()}
                            onSelect={function () {
                              openActionDialog("suspend", row);
                            }}
                          >
                            <UserX
                              className={getRowMenuIconClassName()}
                              aria-hidden="true"
                            />
                            Suspend Tenant
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            className={getRowMenuItemClassName(true)}
                            onSelect={function () {
                              openActionDialog("terminate", row);
                            }}
                          >
                            <TrashIcon
                              className={getRowMenuIconClassName(true)}
                              aria-hidden="true"
                            />
                            Terminate Tenant
                          </DropdownMenuItem>
                        </>
                      )}
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </div>
            );
          })
            : null}
        </div>
      </div>

      {actionDialog ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4 py-6">
          <div className="max-h-[90vh] w-full min-w-0 max-w-[32.5rem] overflow-y-auto rounded-[1rem] border border-[#e3ebf3] bg-white p-5 shadow-[0_20px_60px_rgba(15,23,42,0.22)]">
            <h3
              className="text-[#1f2d38] text-[1.125rem] font-semibold leading-6 break-words [overflow-wrap:anywhere]"
              title={actionDialogTitle}
            >
              {actionDialogTitle}
            </h3>
            <p className="mt-2 text-[#667483] text-sm leading-5 break-words [overflow-wrap:anywhere]">
              {actionDialogTitle} for:
            </p>
            <p
              className="mt-1 text-[#2b3946] text-sm font-medium leading-5 break-words [overflow-wrap:anywhere]"
              title={actionDialog.rowName}
            >
              {actionDialog.rowName}
            </p>

            <div className="mt-4 space-y-3">
              <div>
                <label className="mb-1 block text-[0.75rem] font-medium text-[#52606d]">
                  Reason
                </label>
                <Input
                  value={actionReason}
                  onChange={(event) => setActionReason(event.target.value)}
                  placeholder="Enter a reason"
                  className="h-10 rounded-[0.75rem] border-[#dce5ee]"
                />
              </div>

              {actionDialog.type === "terminate" ? (
                <div>
                  <label className="mb-1 block text-[0.75rem] font-medium text-[#52606d]">
                    {`Retention Days (${RETENTION_DAYS_MIN}-${RETENTION_DAYS_MAX})`}
                  </label>
                  <Input
                    value={retentionDays}
                    onChange={(event) =>
                      setRetentionDays(sanitizeRetentionDaysInput(event.target.value))
                    }
                    inputMode="numeric"
                    placeholder={RETENTION_DAYS_DEFAULT}
                    aria-invalid={retentionDaysError ? true : undefined}
                    className="h-10 rounded-[0.75rem] border-[#dce5ee]"
                  />
                  <p
                    className={cn(
                      "mt-1 text-[0.75rem] leading-4 break-words [overflow-wrap:anywhere]",
                      retentionDaysError ? "text-[#b42318]" : "text-[#667483]",
                    )}
                  >
                    {retentionDaysError ??
                      `Data is kept for this many days before purge. Must be between ${RETENTION_DAYS_MIN} and ${RETENTION_DAYS_MAX} days.`}
                  </p>
                </div>
              ) : null}
            </div>

            {actionErrorMessage ? (
              <div className="mt-4 rounded-[0.75rem] border border-[#f7d3d7] bg-[#fff4f5] px-3 py-2 text-[0.75rem] font-medium text-[#b42318] break-words [overflow-wrap:anywhere]">
                {actionErrorMessage}
              </div>
            ) : null}

            <div className="mt-5 flex flex-wrap items-center justify-end gap-3">
              <Button
                variant="outline"
                className="h-10 shrink-0 rounded-full border-[#dce5ee] px-4 text-sm"
                onClick={closeActionDialog}
                disabled={isActionSubmitting}
              >
                Cancel
              </Button>
              <Button
                className="h-10 shrink-0 rounded-full bg-[#435564] px-5 text-sm font-semibold text-white hover:bg-[#394957]"
                onClick={handleConfirmAction}
                disabled={isActionSubmitting || retentionDaysError !== null}
                loading={isActionSubmitting}
                loadingLabel="Processing..."
              >
                {actionDialogConfirmLabel}
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {subscriptionDialog ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4 py-6">
          <div
            className={cn(
              "max-h-[90vh] w-full min-w-0 overflow-y-auto rounded-[1rem] border border-[#e3ebf3] bg-white p-5 shadow-[0_20px_60px_rgba(15,23,42,0.22)]",
              subscriptionDialog.type === "plan-manage" ? "max-w-[40rem]" : "max-w-[35rem]",
            )}
          >
            <h3 className="text-[#1f2d38] text-[1.125rem] font-semibold leading-6 break-words [overflow-wrap:anywhere]">
              {subscriptionDialog.type === "user-limits"
                ? "Adjust User Limits"
                : "Update Plan"}
            </h3>
            <p
              className="mt-2 text-[#667483] text-sm leading-5 break-words [overflow-wrap:anywhere]"
              title={subscriptionDialog.rowName}
            >
              {subscriptionDialog.rowName}
            </p>

            {isSubscriptionLoading ? (
              <div className="mt-4 text-sm text-[#667483]">Loading subscription...</div>
            ) : null}

            <div className="mt-4 space-y-3">
              {subscriptionDialog.type === "user-limits" ? (
                <>
                  <div>
                    <Input
                      value={limitTherapists}
                      onChange={(event) =>
                        setLimitTherapists(sanitizeUserLimitInput(event.target.value))
                      }
                      placeholder="Therapist limit"
                      inputMode="numeric"
                      maxLength={ORGANISATION_USER_LIMIT_MAX_DIGITS}
                      className="h-10 rounded-[0.75rem] border-[#dce5ee]"
                    />
                    <p className="mt-1 text-xs text-[#667483]">
                      Digits only, 0–{ORGANISATION_USER_LIMIT_MAX.toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <Input
                      value={limitSupervisors}
                      onChange={(event) =>
                        setLimitSupervisors(sanitizeUserLimitInput(event.target.value))
                      }
                      placeholder="Supervisor limit"
                      inputMode="numeric"
                      maxLength={ORGANISATION_USER_LIMIT_MAX_DIGITS}
                      className="h-10 rounded-[0.75rem] border-[#dce5ee]"
                    />
                    <p className="mt-1 text-xs text-[#667483]">
                      Digits only, 0–{ORGANISATION_USER_LIMIT_MAX.toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <Input
                      value={limitClients}
                      onChange={(event) =>
                        setLimitClients(sanitizeUserLimitInput(event.target.value))
                      }
                      placeholder="Client limit"
                      inputMode="numeric"
                      maxLength={ORGANISATION_USER_LIMIT_MAX_DIGITS}
                      className="h-10 rounded-[0.75rem] border-[#dce5ee]"
                    />
                    <p className="mt-1 text-xs text-[#667483]">
                      Digits only, 0–{ORGANISATION_USER_LIMIT_MAX.toLocaleString()}
                    </p>
                  </div>
                  <div className="text-xs text-[#667483]">
                    Active users: Therapists {organisationSubscription?.userUsage.therapistUsers ?? 0},
                    Supervisors {organisationSubscription?.userUsage.supervisorUsers ?? 0},
                    Clients {organisationSubscription?.userUsage.clientUsers ?? 0}
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <label className="mb-1 block text-[0.75rem] font-medium text-[#52606d]">
                      Current Plan
                    </label>
                    <Input
                      value={organisationSubscription?.plan || ""}
                      disabled
                      placeholder="Current Plan"
                      className="h-10 rounded-[0.75rem] border-[#dce5ee] bg-[#f8fafc]"
                    />
                  </div>

                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <div className="min-w-0">
                      <label className="mb-1 block text-[0.75rem] font-medium text-[#52606d]">
                        Target Plan
                      </label>
                      <Select value={planValue} onValueChange={handleTargetPlanChange}>
                        <SelectTrigger className="h-10 w-full rounded-[0.75rem] border-[#dce5ee]">
                          <SelectValue placeholder="Select plan" />
                        </SelectTrigger>
                        <SelectContent>
                          {plansCatalog.map((plan) => (
                            <SelectItem key={plan.planCode} value={plan.planCode}>
                              {plan.planName}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="min-w-0">
                      <label className="mb-1 block text-[0.75rem] font-medium text-[#52606d]">
                        Billing Cycle
                      </label>
                      <Select value={billingCycleValue || undefined} disabled>
                        <SelectTrigger className="h-10 w-full rounded-[0.75rem] border-[#dce5ee] bg-[#f8fafc]">
                          <SelectValue placeholder="Select plan first" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="monthly">Monthly</SelectItem>
                          <SelectItem value="annual">Annual</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="min-w-0">
                    <label className="mb-1 block text-[0.75rem] font-medium text-[#52606d]">
                      Trial Days
                    </label>
                    <Input
                      value={trialDaysValue}
                      readOnly
                      placeholder="Select plan first"
                      inputMode="numeric"
                      maxLength={ORGANISATION_TRIAL_DAYS_MAX_DIGITS}
                      disabled={!planValue}
                      className="h-10 rounded-[0.75rem] border-[#dce5ee] bg-[#f8fafc]"
                    />
                    <p className="mt-1 text-xs text-[#667483]">
                      Set from the selected plan catalog entry.
                    </p>
                  </div>
                </>
              )}
            </div>

            <div className="mt-5 flex flex-wrap items-center justify-end gap-3">
              <Button
                variant="outline"
                className="h-10 shrink-0 rounded-full border-[#dce5ee] px-4 text-sm"
                onClick={closeSubscriptionDialog}
                disabled={isUpdatingSubscription}
              >
                Cancel
              </Button>
              <Button
                className="h-10 shrink-0 rounded-full bg-[#435564] px-5 text-sm font-semibold text-white hover:bg-[#394957]"
                onClick={handleConfirmSubscriptionUpdate}
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
    </SuperAdminPageShell>
  );
}

export default Organisations;
