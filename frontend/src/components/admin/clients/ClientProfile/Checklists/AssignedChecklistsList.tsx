import type { ClientChecklistResponse } from "@/store/api/admin/checklists.api";
import { CalendarIcon } from "@/components/icons/commonIcons";
import { Eye } from "lucide-react";
import { useMemo, useState } from "react";
import { cn } from "@/lib/utils";
import ChecklistItemsModal from "./ChecklistItemsModal";
import Toast from "@/components/shared/Toast";
import {
  checklistsApi,
  useUpdateClientChecklistItemMutation,
} from "@/store/api/admin/checklists.api";
import { useAppDispatch } from "@/store/hooks";
import { getApiErrorMessage } from "@/utils/apiError";

type AssignedChecklist = ClientChecklistResponse;

interface AssignedChecklistsListProps {
  checklists: AssignedChecklist[];
  clientId: string;
  readOnly?: boolean;
}

const AssignedChecklistsList = ({
  checklists,
  clientId,
  readOnly = false,
}: AssignedChecklistsListProps) => {
  const dispatch = useAppDispatch();
  const [modalOpen, setModalOpen] = useState<number | null>(null);
  const [openChecklistSnapshot, setOpenChecklistSnapshot] =
    useState<AssignedChecklist | null>(null);
  const [draftSelectedItems, setDraftSelectedItems] = useState<string[]>([]);
  const [savingChecklistId, setSavingChecklistId] = useState<number | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [updateClientChecklistItem] = useUpdateClientChecklistItemMutation();

  const activeChecklist = useMemo(() => {
    if (modalOpen == null) return null;
    return (
      checklists.find((entry) => entry.id === modalOpen) ?? openChecklistSnapshot
    );
  }, [checklists, modalOpen, openChecklistSnapshot]);

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return "N/A";
    const date = new Date(dateStr);
    return date.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  const openChecklistModal = (checklistId: number) => {
    const checklist = checklists.find((entry) => entry.id === checklistId);
    if (!checklist) return;
    const completedIds = (checklist.items || [])
      .filter((item) => item.isCompleted)
      .map((item) => String(item.id));
    setOpenChecklistSnapshot(checklist);
    setDraftSelectedItems(completedIds);
    setModalOpen(checklistId);
  };

  const closeChecklistModal = () => {
    if (savingChecklistId != null) return;
    setModalOpen(null);
    setOpenChecklistSnapshot(null);
    setDraftSelectedItems([]);
  };

  const handleSaveChecklistItems = async () => {
    if (modalOpen == null || !activeChecklist) return;

    const selectedSet = new Set(draftSelectedItems);
    const changedItems = (activeChecklist.items || []).filter((item) => {
      const shouldBeCompleted = selectedSet.has(String(item.id));
      return Boolean(item.isCompleted) !== shouldBeCompleted;
    });

    if (changedItems.length === 0) {
      closeChecklistModal();
      return;
    }

    // Freeze current checks for the whole save — never rehydrate from server mid-request.
    const selectionAtSave = [...draftSelectedItems];
    setSavingChecklistId(modalOpen);
    try {
      for (const item of changedItems) {
        await updateClientChecklistItem({
          id: item.id,
          clientId,
          body: {
            isCompleted: selectedSet.has(String(item.id)),
            notes: "",
          },
        }).unwrap();
      }

      dispatch(
        checklistsApi.util.invalidateTags([{ type: "ClientChecklists", id: clientId }]),
      );
      setToastType("success");
      setToastMessage("Checklist items updated successfully.");
      setModalOpen(null);
      setOpenChecklistSnapshot(null);
      setDraftSelectedItems([]);
    } catch (error) {
      // Keep the user's checks if save fails.
      setDraftSelectedItems(selectionAtSave);
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setSavingChecklistId(null);
    }
  };

  return (
    <>
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      {activeChecklist && modalOpen != null ? (
        <ChecklistItemsModal
          isOpen
          onClose={closeChecklistModal}
          onSave={() => {
            void handleSaveChecklistItems();
          }}
          readOnly={readOnly}
          items={(activeChecklist.items || []).map((item) => ({
            id: String(item.id),
            label: item.checklistItemTitle ?? "Checklist item",
          }))}
          selectedItems={draftSelectedItems}
          onSelectedItemsChange={(next) => {
            if (savingChecklistId != null) return;
            setDraftSelectedItems(next);
          }}
          isSaving={savingChecklistId === activeChecklist.id}
        />
      ) : null}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {checklists.map((checklist) => {
          const totalItems = checklist.items?.length || 0;
          const completedCount =
            checklist.items?.filter((i) => i.isCompleted).length || 0;
          const progressPercent =
            totalItems > 0 ? Math.round((completedCount / totalItems) * 100) : 0;

          return (
            <div
              key={checklist.id}
              className="group flex flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white transition-all hover:shadow-sm"
            >
              <div className="flex-1 p-6">
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div className="flex min-w-0 flex-1 items-center gap-2">
                    <h3
                      className="min-w-0 truncate text-base font-semibold text-gray-900"
                      title={checklist.templateName}
                    >
                      {checklist.templateName}
                    </h3>
                    <span
                      className={cn(
                        "shrink-0 rounded px-2 py-0.5 text-[0.625rem] font-medium tracking-wide uppercase",
                        checklist.isCompleted
                          ? "bg-emerald-50 text-emerald-700"
                          : "bg-blue-50 text-blue-700",
                      )}
                    >
                      {checklist.isCompleted ? "Completed" : "In progress"}
                    </span>
                  </div>
                  <span className="shrink-0 rounded-md border border-gray-100 bg-gray-50 px-2 py-1 text-xs font-medium text-gray-500">
                    {completedCount}/{totalItems} ({progressPercent}%)
                  </span>
                </div>

                <p className="mb-1 text-sm leading-relaxed text-gray-500">
                  {checklist.description || "No description provided."}
                </p>
              </div>

              <div className="mt-auto flex items-center justify-between border-t border-gray-100 bg-[#F8FAFC] px-6 py-4">
                <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm leading-[1.375rem] text-[#1B1C20]">
                  <div className="flex items-center gap-1.5">
                    <CalendarIcon size={14} className="text-gray-400" />
                    <span>Assigned: {formatDate(checklist.createdAt)}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <CalendarIcon size={14} className="text-gray-400" />
                    <span>Due: {formatDate(checklist.dueDate)}</span>
                  </div>
                </div>

                <button
                  onClick={() => openChecklistModal(checklist.id)}
                  className="cursor-pointer rounded-lg border border-gray-200 bg-white p-2 text-gray-700 shadow-sm transition-all hover:border-gray-300 hover:text-gray-900"
                >
                  <Eye size={18} />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
};

export default AssignedChecklistsList;
