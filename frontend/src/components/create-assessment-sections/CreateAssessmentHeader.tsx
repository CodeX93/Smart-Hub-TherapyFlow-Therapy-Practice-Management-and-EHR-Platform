import { Eye, MoveLeft, Bookmark } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";

interface CreateAssessmentHeaderProps {
  title: string;
  onSaveTemplate: () => void;
  onPreview: () => void;
  isPreviewModalOpen: boolean;
  isSaving?: boolean;
}

const CreateAssessmentHeader = ({
  title,
  onSaveTemplate,
  onPreview,
  isPreviewModalOpen,
  isSaving,
}: CreateAssessmentHeaderProps) => {
  const navigate = useNavigate();

  return (
    <div className="flex items-center justify-between gap-4 min-w-0">
      <div className="flex min-w-0 flex-1 items-center gap-4">
        <button
          onClick={() => navigate("/admin/content/assessment")}
          className="shrink-0 p-2 hover:bg-(--neutral-50) rounded-full transition-colors cursor-pointer text-(--text-primary-dark)"
        >
          <MoveLeft size={20} />
        </button>
        <h1
          className="min-w-0 text-xl font-semibold text-(--text-primary-dark) break-words [overflow-wrap:anywhere]"
          title={title}
        >
          {title}
        </h1>
      </div>
      <div className="flex shrink-0 items-center gap-3">
        <Button
          variant="ghost"
          onClick={onPreview}
          className="border border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--bg-primary-dark) hover:text-white transition-all duration-300 ease-in-out cursor-pointer rounded-full px-4! font-semibold h-10"
        >
          <Eye size={18} />
          {isPreviewModalOpen ? "Exit Preview" : "Preview"}
        </Button>
        <Button
          onClick={onSaveTemplate}
          disabled={isSaving}
          loading={isSaving}
          loadingLabel="Saving..."
          className="border border-transparent bg-(--bg-primary-dark) text-white hover:bg-transparent hover:border-(--neutral-200) hover:text-(--bg-primary-dark) rounded-full font-semibold h-10 transition-all duration-300 ease-in-out cursor-pointer disabled:opacity-70 disabled:cursor-not-allowed"
        >
          <Bookmark size={18} />
          Save Template
        </Button>
      </div>
    </div>
  );
};

export default CreateAssessmentHeader;
