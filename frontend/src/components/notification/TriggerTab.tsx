import { ContentLoader } from "@/components/shared/ContentLoader";
import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { Pencil, Plus } from "lucide-react";
import { Button } from "../ui/button";
import { useState } from "react";
import CreateTriggerSidePanel from "./CreateTriggerSidePanel";
import ConfirmationModal from "../shared/ConfirmationModal";
import Toast from "../shared/Toast";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import {
  useDeleteSuperAdminNotificationTriggerMutation,
  useGetSuperAdminNotificationTriggersQuery,
  type SuperAdminNotificationTrigger,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

function formatDate(value: string): string {
  if (!value) return "—";
  try {
    return new Date(value).toLocaleString("en-US", {
      month: "short",
      day: "2-digit",
      year: "numeric",
    });
  } catch {
    return value;
  }
}

function truncateLabel(value: string, maxLength = 56): string {
  if (value.length <= maxLength) return value;
  return `${value.slice(0, maxLength - 1)}…`;
}

const TriggerTab = () => {
  const [isCreatePanelOpen, setIsCreatePanelOpen] = useState(false);
  const [selectedTrigger, setSelectedTrigger] =
    useState<SuperAdminNotificationTrigger | null>(null);
  const [triggerToDelete, setTriggerToDelete] =
    useState<SuperAdminNotificationTrigger | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("success");
  const { data: triggers = [], isLoading, isError, error, refetch } =
    useGetSuperAdminNotificationTriggersQuery();
  const [deleteTrigger, { isLoading: isDeleting }] =
    useDeleteSuperAdminNotificationTriggerMutation();

  async function handleDeleteTrigger() {
    if (!triggerToDelete) return;
    try {
      await deleteTrigger(triggerToDelete.id).unwrap();
      setToastType("success");
      setToastMessage("Trigger deleted successfully.");
      void refetch();
      setTriggerToDelete(null);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setTriggerToDelete(null);
    }
  }

  function handleOpenCreatePanel() {
    setSelectedTrigger(null);
    setIsCreatePanelOpen(true);
  }

  function handleOpenEditPanel(trigger: SuperAdminNotificationTrigger) {
    setSelectedTrigger(trigger);
    setIsCreatePanelOpen(true);
  }

  function handleSaved(message: string) {
    setToastType("success");
    setToastMessage(message);
    void refetch();
  }

  return (
    <>
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="px-6 py-5 h-full overflow-y-auto">
        {isLoading ? (
          <ContentLoader />
        ) : isError ? (
          <div className="rounded-xl border border-[#f3d4d4] bg-[#fff5f5] p-4 text-sm text-(--status-denied)">
            {getApiErrorMessage(error)}
          </div>
        ) : triggers.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-4 mt-20">
            <img src="/assets/setting.png" alt="setting" className="w-30 h-17" />
            <div className="flex flex-col items-center gap-2">
              <h1 className="text-(--text-primary-dark) font-semibold text-lg">
                No notification triggers configured
              </h1>
              <p className="text-(--text-neutral-600) max-w-100 text-center">
                Add triggers to automatically create notifications based on system
                events
              </p>
            </div>

            <Button
              onClick={handleOpenCreatePanel}
              className="min-w-36 h-10 px-4 font-semibold w-fit cursor-pointer text-sm bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full flex items-center gap-2 transition duration-300"
            >
              <Plus size={16} />
              Add Trigger
            </Button>
          </div>
        ) : (
          <div>
            <div className="mb-4 flex items-center justify-between">
              <h1 className="text-(--text-primary-dark) font-semibold">
                Notification Triggers
              </h1>
              <Button
                onClick={handleOpenCreatePanel}
                className="min-w-30 h-9 px-4 font-semibold w-fit cursor-pointer text-sm bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full flex items-center gap-2 transition duration-300"
              >
                <Plus size={16} />
                Add Trigger
              </Button>
            </div>
            <div className="space-y-3 pb-4">
              {triggers.map((trigger) => (
                <div
                  key={trigger.id}
                  className="rounded-xl border border-(--neutral-100) bg-white p-4"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <h3
                        className="truncate text-(--text-primary-dark) text-sm font-semibold"
                        title={trigger.name || "Untitled Trigger"}
                      >
                        {trigger.name || "Untitled Trigger"}
                      </h3>
                      <p
                        className="mt-1 line-clamp-2 break-words text-xs text-(--text-neutral-600)"
                        title={trigger.description || "—"}
                      >
                        {trigger.description || "—"}
                      </p>
                    </div>
                    <span className="rounded-full bg-(--neutral-100) px-3 py-1 text-[0.6875rem] font-medium text-(--text-neutral-600)">
                      {trigger.isActive ? "Active" : "Inactive"}
                    </span>
                  </div>
                  <div className="mt-3 flex flex-wrap items-center gap-2 text-[0.6875rem] text-(--text-neutral-600)">
                    <span className="rounded-full bg-(--neutral-50) px-2 py-1">
                      Event: {trigger.eventType || "—"}
                    </span>
                    <span className="rounded-full bg-(--neutral-50) px-2 py-1">
                      Entity: {trigger.entityType || "—"}
                    </span>
                    <span className="rounded-full bg-(--neutral-50) px-2 py-1">
                      Priority: {trigger.priority || "—"}
                    </span>
                    <span className="rounded-full bg-(--neutral-50) px-2 py-1">
                      Updated: {formatDate(trigger.updatedAt || trigger.createdAt)}
                    </span>
                  </div>
                  <div className="mt-3 flex justify-end">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button
                          type="button"
                          className="grid h-8 w-8 place-items-center rounded-full text-[#273540] transition-colors hover:bg-[#f4f7fa]"
                          aria-label={`Actions for ${trigger.name || "trigger"}`}
                        >
                          <MenuDotsIcon size={16} aria-hidden="true" />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-40">
                        <DropdownMenuItem onClick={() => handleOpenEditPanel(trigger)}>
                          <Pencil size={14} aria-hidden="true" />
                          Edit
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          variant="destructive"
                          onClick={() => setTriggerToDelete(trigger)}
                        >
                          <TrashIcon size={14} aria-hidden="true" />
                          Delete
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <CreateTriggerSidePanel
        isOpen={isCreatePanelOpen}
        onClose={() => {
          setIsCreatePanelOpen(false);
          setSelectedTrigger(null);
        }}
        trigger={selectedTrigger}
        onSaved={handleSaved}
      />
      <ConfirmationModal
        type="delete"
        isOpen={Boolean(triggerToDelete)}
        onClose={() => setTriggerToDelete(null)}
        onConfirm={() => {
          void handleDeleteTrigger();
        }}
        title={
          triggerToDelete
            ? `Delete Trigger "${truncateLabel(triggerToDelete.name, 36)}"`
            : "Delete Trigger"
        }
        description="Are you sure you want to delete this notification trigger? This action cannot be undone."
        items={[]}
        confirmButtonText={isDeleting ? "Deleting..." : "Delete trigger"}
        confirmButtonDisabled={isDeleting}
      />
    </>
  );
};

export default TriggerTab;
