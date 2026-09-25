import { useState } from "react";
import { Button } from "../../../ui/button";
import ChecklistAssignmentDropdown from "./ChecklistAssignmentDropdown";
import AssignedChecklistsList from "./AssignedChecklistsList";
import EmptyChecklistsState from "./EmptyChecklistsState";
import { CHECKLISTS_STATIC_CONTENT } from "@/pages/therapist/therapist.static";

const ChecklistsTab = () => {
  const [selectedChecklists, setSelectedChecklists] = useState<string[]>([]);
  const [assignedChecklists, setAssignedChecklists] = useState<string[]>([]);

  const handleAssign = () => {
    // Mock assignment logic
    if (selectedChecklists.length > 0) {
      setAssignedChecklists((prev) => [...prev, ...selectedChecklists]);
      setSelectedChecklists([]);
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-300 p-6 pt-4">
      {/* Assignment Area */}
      <div className="flex gap-4 items-center">
        <div className="flex-1">
          <ChecklistAssignmentDropdown
            options={CHECKLISTS_STATIC_CONTENT.templates}
            selectedValues={selectedChecklists}
            onChange={setSelectedChecklists}
          />
        </div>
        <Button
          onClick={handleAssign}
          disabled={selectedChecklists.length === 0}
          className="h-[3rem] px-8 rounded-full font-semibold disabled:opacity-50 disabled:cursor-not-allowed text-sm shadow-sm transition-all"
        >
          Assign checklist template
        </Button>
      </div>

      {/* List Area */}
      <div>
        <h3 className="text-sm font-semibold text-gray-900 mb-4">
          Assigned Checklists
        </h3>

        {assignedChecklists.length > 0 ? (
          <AssignedChecklistsList checklists={assignedChecklists} />
        ) : (
          <EmptyChecklistsState />
        )}
      </div>
    </div>
  );
};

export default ChecklistsTab;
