import { Search, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "@/components/form/CustomInput";
import CustomSelect from "../form/CustomSelect";

interface ClinicalFormsToolbarProps {
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  category: string;
  setCategory: (category: string) => void;
  onNewTemplate?: () => void;
  showNewTemplate?: boolean;
}

const ClinicalFormsToolbar = ({
  searchQuery,
  setSearchQuery,
  category,
  setCategory,
  onNewTemplate,
  showNewTemplate = true,
}: ClinicalFormsToolbarProps) => {
  return (
    <div className="flex items-center justify-between mb-6 gap-4">
      <div className="flex items-center gap-4 flex-1">
        <div className="w-xs">
          <CustomInput
            placeholder="Search..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="rounded-full min-h-10 pt-1.75 pb-0 shadow-xs"
            icon={<Search className="size-4.5 text-(--text-neutral-400)" />}
            stopFloating
          />
        </div>
        <div>
          <CustomSelect
            value={category}
            onChange={setCategory}
            options={[
              { value: "all", label: "All Categories" },
              { value: "consent", label: "Informed Consent" },
              { value: "intake", label: "Intake" },
            ]}
            className="rounded-full max-h-10 pb-0 pt-0 bg-white shadow-xs"
            isSearch={false}
          />
        </div>
      </div>
      {showNewTemplate ? (
        <Button
          onClick={onNewTemplate}
          className="bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full px-6 h-10 flex items-center gap-2 cursor-pointer"
        >
          <Plus size={20} />
          New Form Template
        </Button>
      ) : null}
    </div>
  );
};

export default ClinicalFormsToolbar;
