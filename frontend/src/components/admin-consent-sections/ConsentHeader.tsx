import { Plus, Eye, MoveLeft, Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";

interface ConsentHeaderProps {
  title: string;
  onAddField: () => void;
  onPreview: () => void;
  onSave: () => void;
  isSaving?: boolean;
}

const ConsentHeader = ({
  title,
  onAddField,
  onPreview,
  onSave,
  isSaving = false,
}: ConsentHeaderProps) => {
  const navigate = useNavigate();

  return (
    <div className="flex items-center justify-between py-5">
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate("/admin/content/clinical-forms")}
          className="p-2 hover:bg-(--neutral-50) rounded-full transition-colors cursor-pointer text-(--text-primary-dark)"
        >
          <MoveLeft size={20} />
        </button>
        <h1 className="text-xl font-semibold text-(--text-primary-dark)">
          {title}
        </h1>
      </div>
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          onClick={onSave}
          disabled={isSaving}
          className="flex items-center gap-2 rounded-full px-5 h-10 border border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--neutral-50) cursor-pointer disabled:opacity-60"
          loading={isSaving}
          loadingLabel="Saving..."
        >
          <Save size={18} />
          Save
        </Button>
        <Button
          variant="ghost"
          onClick={onPreview}
          className="flex items-center gap-2 rounded-full px-5 h-10 border border-(--neutral-200) text-(--text-primary-dark) hover:bg-(--neutral-50) cursor-pointer"
        >
          <Eye size={18} />
          Preview Form
        </Button>
        <Button
          onClick={onAddField}
          className="flex items-center gap-2 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full px-6 h-10 cursor-pointer"
        >
          <Plus size={18} />
          Add Field
        </Button>
      </div>
    </div>
  );
};

export default ConsentHeader;
