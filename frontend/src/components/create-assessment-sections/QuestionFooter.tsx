
import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { Switch } from "@/components/ui/switch";
import ActionDropdown from "../shared/ActionDropdown";

interface QuestionFooterProps {
  required: boolean;
  onUpdateRequired: (required: boolean) => void;
  onDuplicate: () => void;
  onDelete: () => void;
}

const QuestionFooter = ({
  required,
  onUpdateRequired,
  onDuplicate,
  onDelete,
}: QuestionFooterProps) => {
  return (
    <div className="bg-(--bg-upload-container) px-4 py-3 mt-3 flex items-center justify-between border-t border-(--neutral-100)">
      <div className="flex items-center gap-3">
        <Switch checked={required} onCheckedChange={onUpdateRequired} />
        <span className="text-sm text-(--text-neutral-800) font-medium">
          Required
        </span>
      </div>
      <ActionDropdown
        contentClassName="w-40"
        actions={[
          {
            label: "Duplicate question",
            onClick: onDuplicate,
          },
          {
            label: "Delete question",
            onClick: onDelete,
          },
        ]}
        trigger={
          <button className="p-1 hover:bg-(--neutral-100) rounded-full transition-colors cursor-pointer">
            <MenuDotsIcon size={18} className="text-(--text-secondary-light)" />
          </button>
        }
      />
    </div>
  );
};

export default QuestionFooter;
