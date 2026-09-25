import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, RefreshCw, X, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import Toast from "@/components/shared/Toast";
import GlobalScopeCallout from "./catalog/components/GlobalScopeCallout";
import FeatureCatalogTable, {
  type FeatureCatalogRow,
} from "./catalog/components/FeatureCatalogTable";
import EditFeatureModal from "./components/EditFeatureModal";
import {
  useDeleteFeatureCatalogEntryMutation,
  useGetFeatureCatalogQuery,
  useGetFeatureCatalogHistoryQuery,
  useToggleFeatureFlagDefaultMutation,
} from "@/store/api/superAdminApi";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import { getApiErrorMessage } from "@/utils/apiError";

const FeatureFlagsCatalog = () => {
  const { data: catalogData, isLoading, refetch } = useGetFeatureCatalogQuery({
    includeDeprecated: false,
  });
  const [deleteFeatureCatalogEntry, { isLoading: isDeletingFeature }] =
    useDeleteFeatureCatalogEntryMutation();
  const [toggleFeatureFlagDefault] = useToggleFeatureFlagDefaultMutation();
  const initialRows = useMemo<FeatureCatalogRow[]>(
    () => catalogData ?? [],
    [catalogData]
  );
  const [rows, setRows] = useState<FeatureCatalogRow[]>([]);
  const [historyRow, setHistoryRow] = useState<FeatureCatalogRow | null>(null);
  const [rowToDelete, setRowToDelete] = useState<FeatureCatalogRow | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("success");
  const [rowToEdit, setRowToEdit] = useState<FeatureCatalogRow | null>(null);
  const {
    data: historyRows = [],
    isLoading: isHistoryLoading,
  } = useGetFeatureCatalogHistoryQuery(
    { key: historyRow?.keyName ?? "", page: 0, size: 50 },
    { skip: !historyRow?.keyName }
  );

  useEffect(() => {
    setRows(initialRows);
  }, [initialRows]);

  function showToast(type: "success" | "error", message: string) {
    setToastType(type);
    setToastMessage(message);
  }

  function handleApplyDefaults() {
    setRows(initialRows);
  }

  async function handleConfirmDelete() {
    if (!rowToDelete) return;
    try {
      await deleteFeatureCatalogEntry(rowToDelete.keyName).unwrap();
      showToast("success", "Feature deleted successfully.");
      setRowToDelete(null);
      await refetch();
    } catch (error) {
      showToast("error", getApiErrorMessage(error));
      setRowToDelete(null);
    }
  }

  return (
    <SuperAdminPageShell
      title="Feature Catalog"
      description="Central registry for core and custom feature toggles, usage limits, and rollouts."
    >
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="w-full xl:max-w-[45rem] xl:flex-1">
          <GlobalScopeCallout />
        </div>

        <div className="hidden flex-wrap items-center gap-3 xl:justify-end">
          <Button
            type="button"
            variant="secondary"
            size="md"
            onClick={handleApplyDefaults}
          >
            <RefreshCw size={14} aria-hidden="true" />
            Apply Plan Defaults
          </Button>
          <Button type="button" variant="primary" size="md">
            <CheckCircle2 size={15} aria-hidden="true" />
            Save Entitlements
          </Button>
        </div>
      </div>

      <FeatureCatalogTable
        rows={rows}
        isLoading={isLoading}
        onToggleDefault={async function (keyName, next) {
          setRows(function (previousRows) {
            return previousRows.map(function (row) {
              if (row.keyName === keyName) {
                return { ...row, globalDefault: next };
              }

              return row;
            });
          });
          try {
            await toggleFeatureFlagDefault({ key: keyName, enabled: next }).unwrap();
            showToast("success", "Global default updated successfully.");
            await refetch();
          } catch (error) {
            setRows(function (previousRows) {
              return previousRows.map(function (row) {
                if (row.keyName === keyName) {
                  return { ...row, globalDefault: !next };
                }
                return row;
              });
            });
            showToast("error", getApiErrorMessage(error));
          }
        }}
        onViewHistory={setHistoryRow}
        onDelete={setRowToDelete}
        onEdit={setRowToEdit}
      />

      {historyRow ? (
        <div className="fixed inset-0 z-50">
          <button
            type="button"
            className="absolute inset-0 bg-black/20"
            aria-label="Close history"
            onClick={function () {
              setHistoryRow(null);
            }}
          />

          <div className="absolute right-0 top-0 h-full w-full max-w-[35rem] border-l border-[#e3ebf3] bg-white shadow-[-8px_0_24px_rgba(15,23,42,0.08)]">
            <div className="flex items-start justify-between border-b border-[#edf2f7] px-6 py-5">
              <div>
                <div className="text-[1.125rem] font-semibold leading-7 text-[#1f2d38]">
                  Feature History
                </div>
                <div className="mt-3 flex items-center gap-3">
                  <span className="text-[0.8125rem] font-semibold text-[#1f2d38]">
                    {historyRow.name}
                  </span>
                  <span className="text-[0.6875rem] text-[#8a96a3]">
                    {historyRow.keyName}
                  </span>
                </div>
              </div>

              <Button
                type="button"
                variant="ghost"
                size="icon-sm"
                aria-label="Close history panel"
                onClick={function () {
                  setHistoryRow(null);
                }}
              >
                <X size={18} aria-hidden="true" />
              </Button>
            </div>

            <div className="flex h-[calc(100%-6rem)] flex-col gap-8 overflow-auto px-6 py-6">
              {isHistoryLoading ? (
                <div className="text-sm text-[#667483]">Loading history...</div>
              ) : null}
              {!isHistoryLoading && historyRows.length === 0 ? (
                <div className="text-sm text-[#667483]">No history found.</div>
              ) : null}
              {historyRows.map(function (event, index) {
                return (
                  <div key={event.id || `${event.createdAt}-${index}`} className="relative pl-7">
                    {index !== historyRows.length - 1 ? (
                      <span className="absolute left-[0.1875rem] top-3 h-[calc(100%+1.5rem)] w-px bg-[#d8e2ec]" />
                    ) : (
                      <span className="absolute left-[0.1875rem] top-3 h-[calc(100%-1rem)] w-px bg-[#d8e2ec]" />
                    )}
                    <span className="absolute left-0 top-2 h-2.5 w-2.5 rounded-full bg-[#98a4b3]" />

                    <div className="text-[0.8125rem] font-medium leading-5 text-[#334155]">
                      {event.createdAt ? (
                        new Intl.DateTimeFormat("en-US", {
                          year: "numeric",
                          month: "short",
                          day: "2-digit",
                          hour: "numeric",
                          minute: "2-digit",
                        }).format(new Date(event.createdAt))
                      ) : "-"}
                    </div>

                    <div className="mt-4 flex items-center gap-3">
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[#efe6dd] text-[0.625rem] font-semibold text-[#7a6757]">
                        {event.authId ? <User size={14} /> : "SA"}
                      </div>
                      <div className="text-[0.8125rem] font-medium text-[#2b3946]">
                        {event.action
                          ? event.action
                              .split("_")
                              .map(
                                (word) =>
                                  word.charAt(0).toUpperCase() +
                                  word.slice(1).toLowerCase()
                              )
                              .join(" ")
                          : "Updated"}
                      </div>
                    </div>

                    <div className="mt-4 rounded-[1.25rem] border border-[#EDEEF1] bg-[#FAFAFB] px-5 py-5">
                      <div className="text-[0.8125rem] text-[#1f2d38] break-words">
                        {event.details || event.resourceType || "Feature catalog change"}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      ) : null}

      <ConfirmationModal
        type="delete"
        isOpen={Boolean(rowToDelete)}
        onClose={() => setRowToDelete(null)}
        onConfirm={handleConfirmDelete}
        title={`Delete feature "${rowToDelete?.name || rowToDelete?.keyName || ""}"?`}
        description="Are you sure you want to delete this feature catalog entry?"
        confirmButtonText={isDeletingFeature ? "Deleting..." : "Delete feature"}
        confirmButtonDisabled={isDeletingFeature}
      />

      <EditFeatureModal
        row={rowToEdit}
        onClose={() => setRowToEdit(null)}
        onUpdated={refetch}
      />
    </SuperAdminPageShell>
  );
};

export default FeatureFlagsCatalog;
