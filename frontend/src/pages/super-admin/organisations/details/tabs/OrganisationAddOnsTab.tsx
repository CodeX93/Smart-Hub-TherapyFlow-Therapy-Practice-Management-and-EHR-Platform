import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { useMemo, useState } from "react";
import { Settings2, UserMinus, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import SemanticStatusBadge from "@/components/shared/SemanticStatusBadge";
import Toast from "@/components/shared/Toast";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  type OrganisationAddOnListItem,
  useAssignOrganisationAddOnMutation,
  useGetAddOnsCatalogQuery,
  useGetOrganisationAddOnsQuery,
  useUnassignOrganisationAddOnByFeatureMutation,
  useUnassignOrganisationAddOnByPurchaseMutation,
} from "@/store/api/superAdminApi";
import {
  ASSIGN_ADD_ON_LIMITS,
  ASSIGN_ADD_ON_QUANTITY_MAX_DIGITS,
  sanitizeAssignAddOnQuantity,
  validateAssignAddOnInput,
} from "./assignAddOn.utils";

function getHeaderItems(): string[] {
  return [
    "Add-on Name",
    "Code",
    "Price",
    "Billing Cycle",
    "Status",
    "Actions",
  ];
}

function getAddOnTableGridClassName(): string {
  return "grid min-w-[45rem] grid-cols-[minmax(7.5rem,1.5fr)_minmax(6rem,1.2fr)_minmax(4.5rem,1fr)_minmax(6rem,1.2fr)_minmax(5rem,0.9fr)_3rem] items-center";
}

function getMenuContentClassName(): string {
  return cn(
    "z-[10030] w-[min(10.375rem,calc(100vw-1.5rem))] rounded-[0.75rem] border border-(--neutral-100) bg-(--surface-white) p-1.5",
    "shadow-[0px_12px_24px_var(--shadow)]",
  );
}

function getMenuItemClassName(isDestructive?: boolean): string {
  return cn(
    "flex min-w-0 cursor-pointer items-center gap-2 rounded-[0.5rem] px-2 py-1.5",
    "text-[0.6875rem] font-medium leading-4",
    isDestructive
      ? "text-(--status-denied) focus:bg-(--light-red) focus:text-(--status-denied)"
      : "text-(--text-gray-900) focus:bg-(--bg-primary-50) focus:text-(--text-gray-900)",
  );
}

function toDisplayBillingCycle(value: string): string {
  const normalized = value.trim().toLowerCase();
  if (normalized.includes("annual") || normalized.includes("year")) return "Annual";
  if (normalized.includes("month")) return "Monthly";
  return value || "-";
}

function toDisplayStatus(row: OrganisationAddOnListItem): string {
  if (!row.isActive || row.endAt) return "Ended";
  return "Active";
}

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0,
});

function AssignAddOnModal(props: {
  open: boolean;
  onClose(): void;
  options: Array<{ code: string; name: string }>;
  isSubmitting?: boolean;
  onSubmit(featureCode: string, quantity: string): void;
}) {
  const [featureCode, setFeatureCode] = useState("");
  const [quantity, setQuantity] = useState("");

  const [previousOpen, setPreviousOpen] = useState(props.open);
  if (previousOpen !== props.open) {
    setPreviousOpen(props.open);
    setFeatureCode("");
    setQuantity("");
  }

  if (!props.open) return null;

  const selectedOption = props.options.find((item) => item.code === featureCode);

  function formatAddOnLabel(item: { code: string; name: string }): string {
    return `${item.name} (${item.code})`;
  }

  return (
    <div className="fixed inset-0 z-[10040]">
      <button
        type="button"
        className="absolute inset-0 bg-[rgba(15,23,42,0.26)] backdrop-blur-[0.125rem]"
        aria-label="Close modal"
        onClick={props.onClose}
      />

      <div className="absolute inset-0 flex items-center justify-center px-4 py-10">
        <div
          className="w-full max-w-[33.75rem] overflow-hidden rounded-[0.875rem] border border-[#e3eaf1] bg-white shadow-[0_24px_60px_rgba(15,23,42,0.16)]"
          role="dialog"
          aria-modal="true"
          aria-label="Assign add-on to organisation"
        >
          <div className="flex items-start justify-between gap-4 px-[1.125rem] pt-[0.875rem]">
            <div className="text-[1.25rem] font-semibold leading-7 text-[#1f2d38]">
              Assign Add-on
            </div>
            <button
              type="button"
              onClick={props.onClose}
              className="flex h-7 w-7 items-center justify-center rounded-full text-[#1f2d38] transition-colors hover:bg-[#f3f6f9]"
              aria-label="Close"
            >
              <X size={18} strokeWidth={1.8} aria-hidden="true" />
            </button>
          </div>

          <div className="px-[1.125rem] pb-[0.9375rem] pt-[1.125rem]">
            <div className="flex flex-col gap-[0.75rem]">
              <div className="min-w-0">
                <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                  Feature Code <span className="text-[#ef4444]">*</span>
                </div>
                <Select
                  value={featureCode || undefined}
                  onValueChange={setFeatureCode}
                >
                  <SelectTrigger
                    className="h-[2.75rem] w-full min-w-0 rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem] text-[0.8125rem] font-normal leading-5 text-[#2b3946] shadow-none [&>span]:min-w-0 [&>span]:truncate"
                    title={selectedOption ? formatAddOnLabel(selectedOption) : undefined}
                  >
                    <SelectValue placeholder="Select add-on">
                      {selectedOption ? (
                        <span className="block truncate">
                          {formatAddOnLabel(selectedOption)}
                        </span>
                      ) : null}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent
                    position="popper"
                    side="bottom"
                    align="start"
                    collisionPadding={12}
                    className={cn(
                      "z-[10050] max-h-[min(15rem,var(--radix-select-content-available-height))]",
                      "w-[var(--radix-select-trigger-width)] min-w-[var(--radix-select-trigger-width)]",
                      "rounded-[0.875rem] border border-[#dce5ee] bg-white p-1 shadow-[0_12px_28px_rgba(15,23,42,0.08)]",
                    )}
                  >
                    {props.options.map((item) => {
                      const label = formatAddOnLabel(item);
                      return (
                        <SelectItem
                          key={item.code}
                          value={item.code}
                          className="text-[#2b3946] focus:bg-[#f4f7fa] focus:text-[#2b3946]"
                          title={label}
                        >
                          <span className="block truncate">{label}</span>
                        </SelectItem>
                      );
                    })}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <div className="mb-1 text-[0.6875rem] font-medium leading-4 text-[#667483]">
                  Quantity <span className="text-[#ef4444]">*</span>
                </div>
                <Input
                  value={quantity}
                  inputMode="numeric"
                  maxLength={ASSIGN_ADD_ON_QUANTITY_MAX_DIGITS}
                  placeholder="1"
                  onChange={(event) =>
                    setQuantity(sanitizeAssignAddOnQuantity(event.target.value))
                  }
                  className="h-[2.75rem] rounded-[0.875rem] border border-[#dbe4ec] bg-white px-[0.8125rem] text-[0.8125rem] shadow-none"
                />
                <p className="mt-1 text-[0.6875rem] text-[#8a96a3]">
                  Max {ASSIGN_ADD_ON_LIMITS.quantityMax.toLocaleString()}
                </p>
              </div>
            </div>

            <div className="mt-5 flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                className="h-10 rounded-full px-5"
                onClick={props.onClose}
                disabled={props.isSubmitting}
              >
                Cancel
              </Button>
              <Button
                type="button"
                className="h-10 rounded-full bg-[#435564] px-5 text-white hover:bg-[#394957]"
                disabled={props.isSubmitting}
                onClick={() => props.onSubmit(featureCode, quantity)}
                loading={props.isSubmitting}
                loadingLabel="Assigning..."
              >
                Assign Add-on
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function OrganisationAddOnsTab(props: {
  organisationId: number | null;
  isAssignModalOpen: boolean;
  onAssignModalOpenChange(next: boolean): void;
}) {
  const navigate = useNavigate();
  const {
    data: addOns = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useGetOrganisationAddOnsQuery(props.organisationId ?? 0, {
    skip: !props.organisationId,
  });
  const { data: addOnsCatalog = [] } = useGetAddOnsCatalogQuery(undefined, {
    skip: !props.isAssignModalOpen,
  });
  const [assignOrganisationAddOn, { isLoading: isAssigning }] =
    useAssignOrganisationAddOnMutation();
  const [unassignByFeature, { isLoading: isUnassigningByFeature }] =
    useUnassignOrganisationAddOnByFeatureMutation();
  const [unassignByPurchase, { isLoading: isUnassigningByPurchase }] =
    useUnassignOrganisationAddOnByPurchaseMutation();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [addOnToUnassign, setAddOnToUnassign] =
    useState<OrganisationAddOnListItem | null>(null);

  const isUnassigning = isUnassigningByFeature || isUnassigningByPurchase;

  const options = useMemo(
    () =>
      addOnsCatalog.map((item) => ({
        code: item.code,
        name: item.name,
      })),
    [addOnsCatalog],
  );

  function showToast(type: "success" | "error", message: string) {
    setToastType(type);
    setToastMessage(message);
  }

  async function handleAssignAddOn(featureCode: string, quantity: string) {
    if (!props.organisationId) {
      showToast("error", "Organisation id is missing.");
      return;
    }

    const validationError = validateAssignAddOnInput({ featureCode, quantity });
    if (validationError) {
      showToast("error", validationError);
      return;
    }

    try {
      await assignOrganisationAddOn({
        id: props.organisationId,
        body: {
          featureCode: featureCode.trim(),
          quantity: Number.parseInt(quantity.trim(), 10),
        },
      }).unwrap();
      props.onAssignModalOpenChange(false);
      await refetch();
      showToast("success", "Add-on assigned successfully.");
    } catch (assignError) {
      showToast("error", getApiErrorMessage(assignError));
    }
  }

  async function confirmUnassign() {
    if (!props.organisationId || !addOnToUnassign) return;

    try {
      if (addOnToUnassign.purchaseId != null) {
        await unassignByPurchase({
          organisationId: props.organisationId,
          purchaseId: addOnToUnassign.purchaseId,
        }).unwrap();
      } else {
        await unassignByFeature({
          organisationId: props.organisationId,
          featureCode: addOnToUnassign.featureCode || addOnToUnassign.code,
        }).unwrap();
      }
      setAddOnToUnassign(null);
      await refetch();
      showToast("success", "Add-on unassigned successfully.");
    } catch (unassignError) {
      showToast("error", getApiErrorMessage(unassignError));
    }
  }

  return (
    <div className="flex w-full min-w-0 flex-col gap-4">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="w-full min-w-0 overflow-hidden rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) shadow-[0_2px_2px_0_var(--shadow)]">
        <div className="overflow-x-auto" style={{ scrollbarGutter: "auto" }}>
          <div className={getAddOnTableGridClassName() + " bg-(--bg-primary-50) px-4 py-3"}>
            {getHeaderItems().map(function (header) {
              return (
                <div
                  key={header}
                  className={cn(
                    "min-w-0 text-(--text-gray-900) text-xs font-medium leading-4",
                    header === "Actions" ? "text-center" : "",
                  )}
                >
                  {header}
                </div>
              );
            })}
          </div>

          {isLoading ? (
            <div className="px-4 py-4 text-sm text-[#667483]">Loading add-ons...</div>
          ) : null}
          {isError ? (
            <div className="px-4 py-4 text-sm text-red-600">
              {getApiErrorMessage(error)}
            </div>
          ) : null}

          {!isLoading && !isError && addOns.length === 0 ? (
            <div className="px-4 py-10 text-center text-sm text-[#667483]">
              No add-ons found.
            </div>
          ) : null}

          {addOns.map(function (row, index) {
            const status = toDisplayStatus(row);
            return (
              <div
                key={row.id || row.featureCode || `${index}`}
                className={cn(
                  getAddOnTableGridClassName() + " px-4 py-4",
                  index === 0 ? "" : "border-t border-(--neutral-100)",
                )}
              >
                <div
                  className="min-w-0 truncate pr-2 text-(--text-gray-900) text-sm font-normal leading-5.5"
                  title={row.featureName || row.name}
                >
                  {row.featureName || row.name}
                </div>
                <div
                  className="min-w-0 truncate pr-2 text-(--text-gray-900) text-sm font-normal leading-5.5"
                  title={row.featureCode || row.code}
                >
                  {row.featureCode || row.code}
                </div>
                <div className="min-w-0 truncate text-(--text-gray-900) text-sm font-normal leading-5.5">
                  {currencyFormatter.format(row.priceUsd || row.pricePerUnitAtTime || 0)}
                </div>
                <div className="min-w-0 truncate text-(--text-gray-900) text-sm font-normal leading-5.5">
                  {toDisplayBillingCycle(row.billingCycle)}
                </div>
                <div className="min-w-0">
                  <SemanticStatusBadge
                    status={status}
                    className="inline-flex h-5 items-center rounded-full px-2 text-[0.625rem] font-medium leading-4"
                  />
                </div>
                <div className="flex shrink-0 justify-center">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <button
                        type="button"
                        className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-(--text-primary-dark) transition-colors hover:bg-(--bg-primary-50)"
                        aria-label="Open add-on actions"
                        disabled={isUnassigning}
                      >
                        <MenuDotsIcon size={16} aria-hidden="true" />
                      </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                      align="end"
                      sideOffset={6}
                      collisionPadding={12}
                      className={getMenuContentClassName()}
                    >
                      {row.isActive ? (
                        <>
                          <DropdownMenuItem
                            className={getMenuItemClassName(true)}
                            onClick={() => setAddOnToUnassign(row)}
                          >
                            <UserMinus size={14} className="shrink-0" aria-hidden="true" />
                            <span className="min-w-0 truncate">Unassign</span>
                          </DropdownMenuItem>
                          <DropdownMenuSeparator className="my-1 bg-(--neutral-100)" />
                        </>
                      ) : null}
                      <DropdownMenuItem
                        className={getMenuItemClassName()}
                        onClick={function () {
                          navigate("/super-admin/billings-and-plans", {
                            state: { initialTab: "add-ons" },
                          });
                        }}
                      >
                        <Settings2 size={14} className="shrink-0" aria-hidden="true" />
                        <span className="min-w-0 truncate">Manage in Billing</span>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <AssignAddOnModal
        open={props.isAssignModalOpen}
        onClose={() => props.onAssignModalOpenChange(false)}
        options={options}
        isSubmitting={isAssigning}
        onSubmit={handleAssignAddOn}
      />

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(addOnToUnassign)}
        onClose={() => setAddOnToUnassign(null)}
        onConfirm={() => void confirmUnassign()}
        title={`Unassign "${addOnToUnassign?.featureName || addOnToUnassign?.name || ""}"?`}
        description="This ends the organisation's active purchase. History is kept."
        confirmButtonText={isUnassigning ? "Unassigning..." : "Unassign add-on"}
        confirmButtonDisabled={isUnassigning}
        items={[
          "Active purchase will end immediately",
          "Purchase history remains available",
        ]}
      />
    </div>
  );
}

export default OrganisationAddOnsTab;
