import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { Archive, PencilLine } from "lucide-react";
import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import Toast from "@/components/shared/Toast";
import EditAddOnModal from "./EditAddOnModal";
import {
  type AddOnCatalogItem,
  useActivateAddOnCatalogItemMutation,
  useArchiveAddOnCatalogItemMutation,
  useDeactivateAddOnCatalogItemMutation,
  useGetAddOnsCatalogQuery,
  useLazyGetOrganisationsQuery,
  useUnassignOrganisationAddOnByFeatureMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

type AddOnStatus = "Active" | "Archived";

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0,
});

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

function getStatusClassName(status: AddOnStatus): string {
  if (status === "Archived") {
    return "bg-(--neutral-100) text-(--badge-gray-text)";
  }

  return "bg-(--status-completed-light) text-(--badge-green-text)";
}

function StatusPill(props: { status: AddOnStatus }) {
  return (
    <span
      className={cn(
        "inline-flex h-5 items-center rounded-full px-2",
        "text-[0.625rem] font-medium leading-4",
        getStatusClassName(props.status),
      )}
    >
      {props.status}
    </span>
  );
}

function getMenuContentClassName(): string {
  return cn(
    "w-[9.125rem] rounded-[0.75rem] border border-(--neutral-100) bg-(--surface-white) p-1.5",
    "shadow-[0px_12px_24px_var(--shadow)]",
  );
}

function getMenuItemClassName(isDestructive?: boolean): string {
  return cn(
    "cursor-pointer whitespace-nowrap rounded-[0.5rem] px-2 py-1.5 text-[0.6875rem] font-medium leading-4",
    isDestructive
      ? "text-(--status-denied) focus:bg-(--light-red) focus:text-(--status-denied)"
      : "text-(--text-gray-900) focus:bg-(--bg-primary-50) focus:text-(--text-gray-900)",
  );
}

function getApiErrorCode(error: unknown): string | null {
  if (!error || typeof error !== "object") return null;
  const data = (error as FetchBaseQueryError).data;
  if (data && typeof data === "object" && "code" in data) {
    const code = (data as { code?: unknown }).code;
    return typeof code === "string" ? code : null;
  }
  return null;
}

function getErrorStatus(error: unknown): number | null {
  if (!error || typeof error !== "object") return null;
  const status = (error as FetchBaseQueryError).status;
  return typeof status === "number" ? status : null;
}

function AddOnActionsMenu(props: {
  isArchived: boolean;
  onEdit(): void;
  onArchive(): void;
  onActivate(): void;
  onDelete(): void;
  disabled?: boolean;
}) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex h-7 w-7 items-center justify-center rounded-full text-(--text-primary-dark) transition-colors hover:bg-(--bg-primary-50)"
          aria-label="Open add-on actions"
          disabled={props.disabled}
        >
          <MenuDotsIcon size={16} aria-hidden="true" />
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="end"
        sideOffset={6}
        className={getMenuContentClassName()}
      >
        <DropdownMenuItem className={getMenuItemClassName()} onClick={props.onEdit}>
          <PencilLine size={14} aria-hidden="true" />
          Edit Add-on
        </DropdownMenuItem>
        {props.isArchived ? (
          <DropdownMenuItem className={getMenuItemClassName()} onClick={props.onActivate}>
            <Archive size={14} aria-hidden="true" />
            Activate Add-on
          </DropdownMenuItem>
        ) : (
          <DropdownMenuItem className={getMenuItemClassName()} onClick={props.onArchive}>
            <Archive size={14} aria-hidden="true" />
            Archive Add-on
          </DropdownMenuItem>
        )}
        <DropdownMenuSeparator className="my-1 bg-(--neutral-100)" />
        <DropdownMenuItem className={getMenuItemClassName(true)} onClick={props.onDelete}>
          <TrashIcon size={14} aria-hidden="true" />
          Delete
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function AddOnsPanel() {
  const {
    data: addOns,
    isLoading,
    isError,
    error,
    refetch,
  } = useGetAddOnsCatalogQuery();
  const [deactivateAddOn, { isLoading: isDeleting }] =
    useDeactivateAddOnCatalogItemMutation();
  const [archiveAddOn, { isLoading: isArchiving }] =
    useArchiveAddOnCatalogItemMutation();
  const [activateAddOn, { isLoading: isActivating }] =
    useActivateAddOnCatalogItemMutation();
  const [unassignByFeature, { isLoading: isUnassigning }] =
    useUnassignOrganisationAddOnByFeatureMutation();
  const [fetchOrganisations] = useLazyGetOrganisationsQuery();
  const [addOnToEdit, setAddOnToEdit] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("success");
  const [addOnToDelete, setAddOnToDelete] = useState<AddOnCatalogItem | null>(null);
  const [addOnInUse, setAddOnInUse] = useState<AddOnCatalogItem | null>(null);
  const [addOnToArchive, setAddOnToArchive] = useState<AddOnCatalogItem | null>(null);
  const [addOnToActivate, setAddOnToActivate] = useState<AddOnCatalogItem | null>(null);
  const [isForceDeleting, setIsForceDeleting] = useState(false);

  const errorMessage = isError ? getApiErrorMessage(error) : null;
  const isBusy = isDeleting || isArchiving || isActivating || isUnassigning || isForceDeleting;

  function formatBillingCycle(value: string): string {
    const normalized = value.trim().toLowerCase();
    if (normalized.includes("annual") || normalized.includes("year")) return "Annual";
    if (normalized.includes("month")) return "Monthly";
    return value || "-";
  }

  function toRowStatus(item: AddOnCatalogItem): AddOnStatus {
    return item.status === "ACTIVE" ? "Active" : "Archived";
  }

  async function confirmDelete() {
    if (!addOnToDelete) return;
    const target = addOnToDelete;
    try {
      await deactivateAddOn(target.code).unwrap();
      setAddOnToDelete(null);
      setToastType("success");
      setToastMessage("Add-on deleted successfully.");
      await refetch();
    } catch (deleteError) {
      setAddOnToDelete(null);
      const code = getApiErrorCode(deleteError);
      const status = getErrorStatus(deleteError);
      if (status === 409 || code === "ADDON_IN_USE") {
        setAddOnInUse(target);
        return;
      }
      if (status === 404 || code === "ADDON_NOT_FOUND") {
        setToastType("error");
        setToastMessage("Add-on was already deleted. Refreshing list.");
        await refetch();
        return;
      }
      setToastType("error");
      setToastMessage(getApiErrorMessage(deleteError));
    }
  }

  async function handleUnassignAndDelete() {
    if (!addOnInUse) return;
    const target = addOnInUse;
    setIsForceDeleting(true);
    try {
      const pageSize = 50;
      let page = 1;
      let totalPages = 1;

      while (page <= totalPages) {
        const organisations = await fetchOrganisations({
          search: "",
          status: "",
          plan: "",
          createdFrom: "",
          createdTo: "",
          region: "",
          dataResidency: "",
          page,
          pageSize,
          sort: "createdAt",
          order: "desc",
          exportData: false,
        }).unwrap();

        totalPages = Math.max(1, organisations.totalPages || 1);

        for (const org of organisations.rows) {
          const orgId = Number.parseInt(org.id, 10);
          if (!Number.isFinite(orgId) || orgId <= 0) continue;
          try {
            await unassignByFeature({
              organisationId: orgId,
              featureCode: target.code,
            }).unwrap();
          } catch {
            // Ignore orgs without an active purchase for this add-on.
          }
        }

        page += 1;
      }

      await deactivateAddOn(target.code).unwrap();
      setAddOnInUse(null);
      setToastType("success");
      setToastMessage("Add-on unassigned from organisations and deleted.");
      await refetch();
    } catch (forceDeleteError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(forceDeleteError));
    } finally {
      setIsForceDeleting(false);
    }
  }

  async function handleArchiveInstead() {
    if (!addOnInUse) return;
    try {
      await archiveAddOn(addOnInUse.code).unwrap();
      setAddOnInUse(null);
      setToastType("success");
      setToastMessage("Add-on archived successfully.");
      await refetch();
    } catch (archiveError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(archiveError));
    }
  }

  async function confirmArchive() {
    if (!addOnToArchive) return;
    try {
      await archiveAddOn(addOnToArchive.code).unwrap();
      setToastType("success");
      setToastMessage("Add-on archived successfully.");
      await refetch();
    } catch (archiveError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(archiveError));
    } finally {
      setAddOnToArchive(null);
    }
  }

  async function confirmActivate() {
    if (!addOnToActivate) return;
    try {
      await activateAddOn(addOnToActivate.code).unwrap();
      setToastType("success");
      setToastMessage("Add-on activated successfully.");
      await refetch();
    } catch (activateError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(activateError));
    } finally {
      setAddOnToActivate(null);
    }
  }

  return (
    <div className="w-full overflow-hidden rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) shadow-[0_2px_2px_0_var(--shadow)]">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <div className="grid grid-cols-[1.5fr_1.2fr_1fr_1.2fr_0.9fr_0.55fr] items-center bg-(--bg-primary-50) px-4 py-3">
        {getHeaderItems().map(function (header) {
          return (
            <div
              key={header}
              className={cn(
                "text-(--text-gray-900) text-xs font-medium leading-4",
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
      {errorMessage ? (
        <div className="px-4 py-4 text-sm text-red-600">{errorMessage}</div>
      ) : null}

      {!isLoading && !errorMessage && (addOns ?? []).length === 0 ? (
        <div className="px-4 py-10 text-center text-sm text-[#667483]">
          No add-ons found.
        </div>
      ) : null}

      {(addOns ?? []).map(function (row, index) {
        const status = toRowStatus(row);
        return (
          <div
            key={row.code || row.id || `${index}`}
            className={cn(
              "grid grid-cols-[1.5fr_1.2fr_1fr_1.2fr_0.9fr_0.55fr] items-center px-4 py-4",
              index === 0 ? "" : "border-t border-(--neutral-100)",
            )}
          >
            <div
              className="min-w-0 truncate pr-3 text-(--text-gray-900) text-sm font-normal leading-5.5"
              title={row.name}
            >
              {row.name}
            </div>
            <div
              className="min-w-0 truncate pr-3 text-(--text-gray-900) text-sm font-normal leading-5.5"
              title={row.code}
            >
              {row.code}
            </div>
            <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
              {currencyFormatter.format(row.priceUsd || 0)}
            </div>
            <div className="text-(--text-gray-900) text-sm font-normal leading-5.5">
              {formatBillingCycle(row.billingCycle)}
            </div>
            <div>
              <StatusPill status={status} />
            </div>
            <div className="flex justify-center">
              <AddOnActionsMenu
                disabled={isBusy}
                isArchived={status === "Archived"}
                onEdit={() => setAddOnToEdit(row.code)}
                onArchive={() => setAddOnToArchive(row)}
                onActivate={() => setAddOnToActivate(row)}
                onDelete={() => setAddOnToDelete(row)}
              />
            </div>
          </div>
        );
      })}

      <EditAddOnModal
        open={Boolean(addOnToEdit)}
        code={addOnToEdit}
        onClose={() => setAddOnToEdit(null)}
        onSaved={async () => {
          setToastType("success");
          setToastMessage("Add-on updated successfully.");
          await refetch();
        }}
        onSaveError={(message) => {
          setToastType("error");
          setToastMessage(message);
        }}
      />

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(addOnToDelete)}
        onClose={() => setAddOnToDelete(null)}
        onConfirm={() => void confirmDelete()}
        title={`Delete add-on "${addOnToDelete?.name || addOnToDelete?.code || ""}"?`}
        description="This permanently deletes the catalog entry. It fails if any organisation still has an active purchase."
        confirmButtonText={isDeleting ? "Deleting..." : "Delete add-on"}
        confirmButtonDisabled={isBusy}
        items={[
          "Catalog row will be removed",
          "Unassign from all organisations first if delete fails",
        ]}
      />

      {addOnInUse ? (
        <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-[32.5rem] rounded-[1rem] border border-[#e3ebf3] bg-white p-5 shadow-[0_20px_60px_rgba(15,23,42,0.22)]">
            <h3 className="text-[1.125rem] font-semibold text-[#1f2d38]">
              Still assigned to organisations
            </h3>
            <p className="mt-2 text-sm leading-5 text-[#667483]">
              Cannot permanently delete{" "}
              <span className="font-medium text-[#1f2d38]">
                {addOnInUse.name || addOnInUse.code}
              </span>{" "}
              while organisations still have active purchases. Unassign those
              purchases first, or archive instead.
            </p>
            <div className="mt-5 flex flex-wrap justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                className="h-10 rounded-full"
                disabled={isBusy}
                onClick={() => setAddOnInUse(null)}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="outline"
                className="h-10 rounded-full"
                disabled={isBusy}
                onClick={() => void handleArchiveInstead()}
                loading={isArchiving}
                loadingLabel="Archiving..."
              >
                Archive instead
              </Button>
              <Button
                type="button"
                className="h-10 rounded-full bg-[#435564] text-white hover:bg-[#394957]"
                disabled={isBusy}
                onClick={() => void handleUnassignAndDelete()}
                loading={isForceDeleting}
                loadingLabel="Deleting add-on..."
              >
                Unassign & delete
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(addOnToArchive)}
        onClose={() => setAddOnToArchive(null)}
        onConfirm={() => void confirmArchive()}
        title={`Archive add-on "${addOnToArchive?.name || addOnToArchive?.code || ""}"?`}
        description="Are you sure you want to archive this add-on? It will be set to inactive."
        confirmButtonText={isArchiving ? "Archiving..." : "Archive add-on"}
        confirmButtonDisabled={isBusy}
        items={[
          "Add-on status will change to inactive",
          "Existing subscriptions remain unchanged",
        ]}
      />

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(addOnToActivate)}
        onClose={() => setAddOnToActivate(null)}
        onConfirm={() => void confirmActivate()}
        title={`Activate add-on "${addOnToActivate?.name || addOnToActivate?.code || ""}"?`}
        description="Are you sure you want to activate this add-on?"
        confirmButtonText={isActivating ? "Activating..." : "Activate add-on"}
        confirmButtonDisabled={isBusy}
        items={[
          "Add-on will become available for purchase again",
          "This action can be reversed by archiving",
        ]}
      />
    </div>
  );
}

export default AddOnsPanel;
