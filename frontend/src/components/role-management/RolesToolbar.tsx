import { Search, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomInput from "@/components/form/CustomInput";

interface RolesToolbarProps {
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  onCreateRole: () => void;
}

const RolesToolbar = ({
  searchQuery,
  setSearchQuery,
  onCreateRole,
}: RolesToolbarProps) => {
  return (
    <div className="flex items-center justify-between gap-4">
      <div className="max-w-xs w-full shrink-0">
        <CustomInput
          placeholder="Search role..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="rounded-full min-h-10 pt-1.75 pb-0 shadow-xs w-full"
          icon={<Search className="size-4.5 text-(--text-neutral-400)" />}
          stopFloating
        />
      </div>
      <Button
        onClick={onCreateRole}
        className="shrink-0 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full px-6 h-10 flex items-center gap-2 cursor-pointer font-semibold"
      >
        <Plus size={20} />
        Create Role
      </Button>
    </div>
  );
};

export default RolesToolbar;
