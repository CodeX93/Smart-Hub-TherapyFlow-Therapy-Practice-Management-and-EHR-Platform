
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState } from "react";
import { Button } from "../../../../ui/button";
import ChecklistAssignmentDropdown from "./ChecklistAssignmentDropdown";
import AssignedChecklistsList from "./AssignedChecklistsList";
import EmptyChecklistsState from "./EmptyChecklistsState";
import {
  useGetChecklistTemplatesQuery,
  useAssignChecklistToClientMutation,
  useGetClientChecklistsQuery,
} from "@/store/api/admin/checklists.api";

import Toast from "../../../../shared/Toast";
import { getApiErrorMessage } from "@/utils/apiError";
import type { Client } from "@/types/client.type";

interface ChecklistsTabProps {
  client: Client;
  readOnly?: boolean;
}

const ChecklistsTab = ({ client, readOnly = false }: ChecklistsTabProps) => {
  const [selectedChecklists, setSelectedChecklists] = useState<string[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const { data: templatesResponse, isLoading: isLoadingTemplates } =
    useGetChecklistTemplatesQuery({
      page: 1,
      pageSize: 100,
    });

  const { data: assignedChecklists = [], isLoading: isLoadingAssigned } =
    useGetClientChecklistsQuery(client.id);

  const [assignChecklist, { isLoading: isAssigning }] =
    useAssignChecklistToClientMutation();

  const handleAssign = async () => {
    if (selectedChecklists.length === 0) return;

    try {
      setToastType("info");
      setToastMessage("Assigning checklists...");

      const promises = selectedChecklists.map((templateId) =>
        assignChecklist({
          clientId: client.id,
          templateId: Number(templateId),
          dueDate: new Date().toISOString().split("T")[0],
        }).unwrap(),
      );

      await Promise.all(promises);

      setToastType("success");
      setToastMessage("Checklists assigned successfully.");
      setSelectedChecklists([]);
    } catch (err) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(err));
    }
  };

  const templates = (templatesResponse?.items || []).map((t) => ({
    id: String(t.id),
    title: t.name,
    totalItems: t.itemCount ?? t.items?.length ?? 0,
  }));

  return (
    <div className="space-y-8 animate-in fade-in duration-300 p-6">
      {/* Assignment Area */}
      {!readOnly ? (
      <div className="flex gap-4 items-center">
        <div className="flex-1">
          {isLoadingTemplates ? (
            <div className="h-[3rem] flex items-center px-4 border border-gray-200 rounded-[1.125rem] bg-gray-50">
              <ContentLoader variant="inline" size="md" className="mr-2" />
              <span className="text-sm text-gray-500">Loading templates...</span>
            </div>
          ) : (
            <ChecklistAssignmentDropdown
              options={templates}
              selectedValues={selectedChecklists}
              onChange={setSelectedChecklists}
            />
          )}
        </div>
        <Button
          onClick={handleAssign}
          disabled={selectedChecklists.length === 0 || isAssigning}
          className="h-[3rem] px-8 rounded-full font-semibold disabled:opacity-50 disabled:cursor-not-allowed text-sm shadow-sm transition-all bg-[#1E293B] hover:bg-[#1E293B]/90 text-white"
          loading={isAssigning}
          loadingLabel="Assigning checklist template..."
        >
          Assign checklist template
        </Button>
      </div>
      ) : null}

      {/* List Area */}
      <div>
        <h3 className="text-sm font-semibold text-gray-900 mb-4">
          Assigned Checklists
        </h3>

        {isLoadingAssigned ? (
          <div className="py-20 flex flex-col items-center justify-center">
            <ContentLoader size="xl" className="mb-2" />
            <span className="text-sm text-gray-500">Loading assigned checklists...</span>
          </div>
        ) : assignedChecklists.length > 0 ? (
          <AssignedChecklistsList
            checklists={assignedChecklists}
            clientId={client.id}
            readOnly={readOnly}
          />
        ) : (
          <EmptyChecklistsState />
        )}
      </div>

      {toastMessage && (
        <Toast
          message={toastMessage}
          onClose={() => setToastMessage(null)}
          type={toastType}
        />
      )}
    </div>
  );
};

export default ChecklistsTab;
