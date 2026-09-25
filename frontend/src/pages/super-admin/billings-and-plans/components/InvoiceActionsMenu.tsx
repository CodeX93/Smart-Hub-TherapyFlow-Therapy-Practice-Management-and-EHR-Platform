import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { BadgeDollarSign, Eye, RotateCcw } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";

interface InvoiceActionsMenuProps {
  onActionStart?(): void;
  onViewInvoice(): void;
  onApplyCredit(): void;
  onProcessRefund(): void;
}

function getMenuContentClassName(): string {
  return cn(
    "z-[10030] w-[min(11.75rem,calc(100vw-1.5rem))] rounded-[0.75rem] border border-[#e6edf3] bg-white p-1.5",
    "shadow-[0_16px_32px_rgba(15,23,42,0.14)]",
  );
}

function getMenuItemClassName(): string {
  return cn(
    "flex min-w-0 cursor-pointer items-center gap-2 rounded-[0.5rem] px-3 py-2",
    "text-[0.875rem] font-medium leading-[1.375rem] text-[#1b1c20]",
    "focus:bg-[#f4f7fa] focus:text-[#1b1c20]",
  );
}

function InvoiceActionsMenu(props: InvoiceActionsMenuProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-(--text-primary-dark) transition-colors hover:bg-(--bg-primary-50)"
          aria-label="Open invoice actions"
          onPointerDown={function (event) {
            event.stopPropagation();
            props.onActionStart?.();
          }}
          onClick={function (event) {
            event.stopPropagation();
            props.onActionStart?.();
          }}
        >
          <MenuDotsIcon size={16} aria-hidden="true" />
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent
        align="end"
        side="bottom"
        sideOffset={8}
        collisionPadding={12}
        className={getMenuContentClassName()}
      >
        <DropdownMenuItem
          className={getMenuItemClassName()}
          onSelect={function (event) {
            event.stopPropagation();
            props.onActionStart?.();
            props.onViewInvoice();
          }}
        >
          <Eye size={16} className="shrink-0" aria-hidden="true" />
          <span className="min-w-0 truncate">View Invoice</span>
        </DropdownMenuItem>

        <DropdownMenuItem
          className={getMenuItemClassName()}
          onSelect={function (event) {
            event.stopPropagation();
            props.onActionStart?.();
            props.onApplyCredit();
          }}
        >
          <BadgeDollarSign size={16} className="shrink-0" aria-hidden="true" />
          <span className="min-w-0 truncate">Apply Credit</span>
        </DropdownMenuItem>

        <DropdownMenuItem
          className={getMenuItemClassName()}
          onSelect={function (event) {
            event.stopPropagation();
            props.onActionStart?.();
            props.onProcessRefund();
          }}
        >
          <RotateCcw size={16} className="shrink-0" aria-hidden="true" />
          <span className="min-w-0 truncate">Process Refund</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export default InvoiceActionsMenu;
