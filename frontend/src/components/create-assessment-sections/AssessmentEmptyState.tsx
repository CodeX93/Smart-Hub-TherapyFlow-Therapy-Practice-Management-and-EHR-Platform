import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";

interface AssessmentEmptyStateProps {
  onAddSection: () => void;
}

const AssessmentEmptyState = ({ onAddSection }: AssessmentEmptyStateProps) => {
  return (
    <div className="bg-white rounded-2xl p-8 shadow-xs flex flex-col items-center justify-center gap-1.5 mt-8">
      <img src="/assets/lab-empty.png" alt="lab-empty" />
      <h1 className="text-xl font-semibold text-(--text-primary-dark)">
        No section created yet
      </h1>
      <p className="text-(--text-neutral-600)">
        Add your first section to start building the assessment.
      </p>
      <Button
        onClick={onAddSection}
        className="flex items-center gap-2 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full h-10 cursor-pointer mt-1.5"
      >
        <Plus size={18} />
        Add First Section
      </Button>
    </div>
  );
};

export default AssessmentEmptyState;
