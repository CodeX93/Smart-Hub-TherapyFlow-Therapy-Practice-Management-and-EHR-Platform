import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import { SearchIcon } from "@/pages/super-admin/dashboard/dashboard.utils";

const CatalogFilters = ({
  search,
  onSearchChange,
  scope,
  onScopeChange,
  type,
  onTypeChange,
}: {
  search: string;
  onSearchChange: (next: string) => void;
  scope: string;
  onScopeChange: (next: string) => void;
  type: string;
  onTypeChange: (next: string) => void;
}) => {
  return (
    <div className="w-full flex items-center justify-between gap-4">
      <div className="relative w-full">
        <div className="absolute left-3 top-1/2 -translate-y-1/2">
          <SearchIcon />
        </div>
        <Input
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Search by feature name or key..."
          className={cn(
            "h-10 rounded-lg bg-(--surface-white) border-(--neutral-100) pl-9 pr-4 shadow-xs",
            "placeholder:text-(--text-neutral-400) text-(--text-primary-dark)"
          )}
        />
      </div>

      <div className="flex items-center gap-3 shrink-0">
        <Select value={scope} onValueChange={onScopeChange}>
          <SelectTrigger
            className={cn(
              "h-10 w-34 rounded-lg bg-(--surface-white) border-(--neutral-100) shadow-xs",
              "text-(--text-primary-dark) [&_svg]:text-(--text-neutral-400)"
            )}
          >
            <SelectValue placeholder="Scope: All" />
          </SelectTrigger>
          <SelectContent className="bg-(--surface-white) border-(--neutral-100) shadow-[0px_12px_24px_var(--shadow)]">
            {["Scope: All", "Scope: Tenant", "Scope: Global"].map((v) => (
              <SelectItem
                key={v}
                value={v}
                className="text-(--text-primary-dark) focus:bg-(--bg-primary-50) focus:text-(--text-primary-dark)"
              >
                {v}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={type} onValueChange={onTypeChange}>
          <SelectTrigger
            className={cn(
              "h-10 w-34 rounded-lg bg-(--surface-white) border-(--neutral-100) shadow-xs",
              "text-(--text-primary-dark) [&_svg]:text-(--text-neutral-400)"
            )}
          >
            <SelectValue placeholder="Type: All" />
          </SelectTrigger>
          <SelectContent className="bg-(--surface-white) border-(--neutral-100) shadow-[0px_12px_24px_var(--shadow)]">
            {["Type: All", "Type: Toggle", "Type: Limit"].map((v) => (
              <SelectItem
                key={v}
                value={v}
                className="text-(--text-primary-dark) focus:bg-(--bg-primary-50) focus:text-(--text-primary-dark)"
              >
                {v}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
};

export default CatalogFilters;

