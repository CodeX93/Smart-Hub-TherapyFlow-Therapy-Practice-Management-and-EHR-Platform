import { CalendarIcon } from "@/components/icons/commonIcons";
import { Eye } from "lucide-react";
import { useState } from "react";
import ChecklistItemsModal from "../../../admin/clients/ClientProfile/Checklists/ChecklistItemsModal";
import { Button } from "@/components/ui/button";

// Mock data items mirroring the screenshots
// e.g. "Refugee clients", "0/11 (0%)" or "Created: Dec 18, 2025 Due: Jan 17, 2026"

interface AssignedChecklistsListProps {
  checklists: string[]; // IDs of assigned templates (mocking data)
}

const MOCK_CHECKLIST_ITEMS = [
  { id: "1-1", label: "Client Contacted" },
  { id: "1-2", label: "Support Letter Provided" },
  { id: "1-3", label: "Waiting for Referral" },
  { id: "1-4", label: "Referred by Doctor" },
  { id: "1-5", label: "Assessment Ongoing" },
  { id: "1-6", label: "Report Completed" },
  { id: "1-7", label: "Psychotherapy Extended" },
  { id: "1-8", label: "Psychotherapy Submitted for Approval" },
  { id: "1-9", label: "Psychotherapy Ongoing" },
  { id: "1-10", label: "Psychotherapy Completed" },
  { id: "1-11", label: "File Closed" },
];

const MOCK_CHECKLIST_DETAILS: Record<string, { title: string; desc: string }> =
  {
    "1": {
      title: "Refugee clients",
      desc: "This checklist will help manage client administration and track what is required to obtain their approval, as well as provide them with the support they need.",
    },
    "2": {
      title: "Initial Assessment",
      desc: "Standard protocol for initial client meeting and intake forms processing.",
    },
    "3": {
      title: "Discharge Planning",
      desc: "Steps required to properly close the case and ensure client has necessary resources.",
    },
    "4": {
      title: "Insurance Verification",
      desc: "Verify eligibility, benefits, and pre-authorization requirements for treatment.",
    },
    "5": {
      title: "Safety Plan",
      desc: "Create and review safety plan for clients identified as high risk.",
    },
  };

const AssignedChecklistsList = ({
  checklists,
}: AssignedChecklistsListProps) => {
  const [modalOpen, setModalOpen] = useState<string | null>(null);
  const [draftSelectedItems, setDraftSelectedItems] = useState<string[]>([]);
  const [completedItems, setCompletedItems] = useState<
    Record<string, string[]>
  >({});

  const getChecklistDetails = (id: string) => {
    // Mock data to match screenshot visuals
    // First item "Refugee clients" with "In progress"
    const base = MOCK_CHECKLIST_DETAILS[id] || MOCK_CHECKLIST_DETAILS["1"];

    return {
      ...base,
      status: "In progress",
      progressText: "0/11 (0%)",
      assignedDate: "Dec 18, 2025",
      dueDate: "Jan 17, 2026",
    };
  };

  const openChecklistModal = (checklistId: string) => {
    setDraftSelectedItems(completedItems[checklistId] || []);
    setModalOpen(checklistId);
  };

  const closeChecklistModal = () => {
    setModalOpen(null);
    setDraftSelectedItems([]);
  };

  const handleSaveItems = () => {
    if (modalOpen == null) return;
    setCompletedItems({
      ...completedItems,
      [modalOpen]: draftSelectedItems,
    });
    closeChecklistModal();
  };

  return (
    <>
      {modalOpen != null ? (
        <ChecklistItemsModal
          isOpen
          onClose={closeChecklistModal}
          onSave={handleSaveItems}
          items={MOCK_CHECKLIST_ITEMS}
          selectedItems={draftSelectedItems}
          onSelectedItemsChange={setDraftSelectedItems}
        />
      ) : null}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {checklists.map((checklistId, index) => {
          const details = getChecklistDetails(checklistId);

          return (
            <div
              key={`${checklistId}-${index}`}
              className="group bg-white border border-gray-200 rounded-2xl hover:shadow-sm transition-all overflow-hidden flex flex-col"
            >
              <div className="p-6 flex-1">
                {/* Header: Title + Badge + Progress */}
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex min-w-0 flex-1 items-center gap-2">
                    <h3
                      className="min-w-0 truncate text-base font-semibold text-gray-900"
                      title={details.title}
                    >
                      {details.title}
                    </h3>
                    <span className="shrink-0 bg-blue-50 text-blue-700 px-2 py-0.5 rounded text-[0.625rem] font-medium uppercase tracking-wide">
                      {details.status}
                    </span>
                  </div>
                  <span className="shrink-0 text-xs font-medium text-gray-500 bg-gray-50 px-2 py-1 rounded-md border border-gray-100">
                    {details.progressText}
                  </span>
                </div>

                {/* Description */}
                <p className="text-sm text-gray-500 leading-relaxed mb-1">
                  {details.desc}
                </p>
              </div>

              {/* Footer: Dates & Actions */}
              <div className="px-6 py-4 bg-[#F8FAFC] border-t border-gray-100 flex items-center justify-between mt-auto">
                <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-xs text-gray-500">
                  <div className="flex items-center gap-1.5">
                    <CalendarIcon size={14} className="text-gray-400" />
                    <span>Assigned: {details.assignedDate}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <CalendarIcon size={14} className="text-gray-400" />
                    <span>Due: {details.dueDate}</span>
                  </div>
                </div>

                {completedItems[checklistId]?.length > 0 ? (
                  <Button
                    variant="outline"
                    onClick={() => openChecklistModal(checklistId)}
                    className="px-8 h-11.5 rounded-full border border-(--neutral-200) text-(--text-neutral-800) hover:bg-(--neutral-50) cursor-pointer"
                  >
                    View Checklist Items
                  </Button>
                ) : (
                  <button
                    onClick={() => openChecklistModal(checklistId)}
                    className="p-2 bg-white border border-gray-200 text-gray-700 hover:text-gray-900 hover:border-gray-300 rounded-lg transition-all shadow-sm cursor-pointer"
                  >
                    <Eye size={18} />
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
};

export default AssignedChecklistsList;
