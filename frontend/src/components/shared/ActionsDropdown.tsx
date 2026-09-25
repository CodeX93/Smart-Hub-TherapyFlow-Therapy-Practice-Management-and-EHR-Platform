
import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { Button } from "../ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";

export interface ActionItem {
  id: string;
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
  variant?: "default" | "destructive";
  disabled?: boolean;
  hidden?: boolean;
}

interface ActionsDropdownProps {
  actions: ActionItem[];
  triggerClassName?: string;
  triggerIcon?: React.ReactNode;
  align?: "start" | "end" | "center";
}

const ActionsDropdown = ({
  actions,
  triggerClassName = "",
  triggerIcon,
  align = "end",
}: ActionsDropdownProps) => {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          size="icon"
          className={`h-9 w-9 rounded-xl border border-(--text-neutral-100) bg-white p-0 hover:bg-gray-50 cursor-pointer ${triggerClassName}`}
        >
          {triggerIcon ?? (
            <MenuDotsIcon size={18} color="#5B616E" />
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align={align} className="w-48 rounded-lg shadow-lg border border-(--text-neutral-100)">
        {actions.filter((action) => !action.hidden).map((action) => (
          <DropdownMenuItem
            key={action.id}
            onClick={action.onClick}
            variant={action.variant}
            disabled={action.disabled}
            className="cursor-pointer gap-2 py-2.5 px-3 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <span className="text-(--text-neutral-600)">{action.icon}</span>
            <span className="text-[0.875rem] leading-[1.375rem] text-(--text-primary-dark)">
              {action.label}
            </span>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default ActionsDropdown;

