import { MenuDotsIcon } from "@/components/icons/commonIcons";
import React from "react";

import { Popover, PopoverContent, PopoverTrigger } from "../ui/popover";
import { Button } from "../ui/button";
import { cn } from "../../lib/utils";
import { useCloseOnScroll } from "@/hooks/useCloseOnScroll";

export interface DropdownAction {
  label: string;
  icon?: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  variant?: "default" | "destructive";
  subMenu?: DropdownAction[];
  className?: string;
  iconClassName?: string;
}

interface ActionDropdownProps {
  actions: DropdownAction[];
  trigger?: React.ReactNode;
  align?: "start" | "center" | "end";
  contentClassName?: string;
}

interface ActionDropdownPopoverProps {
  actions: DropdownAction[];
  trigger?: React.ReactNode;
  align?: "start" | "center" | "end";
  contentClassName?: string;
  nested?: boolean;
  onAncestorClose?: () => void;
}

const ActionDropdownPopover = ({
  actions,
  trigger,
  align = "end",
  contentClassName,
  nested = false,
  onAncestorClose,
}: ActionDropdownPopoverProps) => {
  const [open, setOpen] = React.useState(false);
  const anchorRef = React.useRef<HTMLButtonElement | null>(null);

  const handleClose = React.useCallback(() => {
    setOpen(false);
    onAncestorClose?.();
  }, [onAncestorClose]);

  useCloseOnScroll(open, handleClose, anchorRef);

  const renderActions = (items: DropdownAction[], closePopover: () => void) => {
    return items.map((action, index) => {
      if (action.subMenu) {
        return (
          <ActionDropdownPopover
            key={index}
            nested
            align="start"
            contentClassName="w-45 p-2 rounded-xl border border-(--neutral-50) shadow-[0px_32px_16px_0px_#1E282E0D,0px_16px_8px_0px_#1E282E0D] bg-white ml-2"
            trigger={
              <button
                className={cn(
                  "w-full text-left py-2.5 px-2 cursor-pointer border-b border-(--neutral-100) last:border-0 hover:bg-(--bg-primary-light)/50 transition-colors flex items-center justify-between group",
                  action.disabled && "opacity-50 cursor-not-allowed pointer-events-none",
                  action.className,
                )}
                onClick={(e) => e.stopPropagation()}
              >
                <div className="flex items-center gap-2">
                  {action.icon && (
                    <span className="flex items-center justify-center">
                      {React.isValidElement(action.icon)
                        ? React.cloneElement(
                            action.icon as React.ReactElement<{
                              className?: string;
                            }>,
                            {
                              className: cn(
                                "size-5 text-(--text-primary-dark) font-medium",
                                action.iconClassName,
                              ),
                            },
                          )
                        : action.icon}
                    </span>
                  )}
                  <span className="text-(--text-primary-dark) text-sm font-medium">
                    {action.label}
                  </span>
                </div>
                <MenuDotsIcon className="size-3.5 text-(--text-neutral-400) group-hover:text-(--text-primary-dark)" />
              </button>
            }
            actions={action.subMenu}
            onAncestorClose={closePopover}
          />
        );
      }

      return (
        <button
          key={index}
          className={cn(
            "w-full text-left py-2.5 px-2 cursor-pointer border-b border-(--neutral-100) last:border-0 hover:bg-(--bg-primary-light)/50 transition-colors flex items-center gap-2",
            action.disabled && "opacity-50 cursor-not-allowed hover:bg-transparent",
            action.className,
          )}
          disabled={action.disabled}
          onClick={(e) => {
            if (action.disabled) return;
            e.stopPropagation();
            action.onClick?.();
            closePopover();
          }}
        >
          {action.icon && (
            <span className="flex items-center justify-center">
              {React.isValidElement(action.icon)
                ? React.cloneElement(
                    action.icon as React.ReactElement<{ className?: string }>,
                    {
                      className: cn(
                        "size-5 text-(--text-primary-dark) font-medium", // Default size and color
                        action.iconClassName,
                        (
                          action.icon as React.ReactElement<{
                            className?: string;
                          }>
                        ).props?.className,
                      ),
                    },
                  )
                : action.icon}
            </span>
          )}
          <span className="text-(--text-primary-dark) font-medium text-sm">
            {action.label}
          </span>
        </button>
      );
    });
  };

  const defaultTrigger = (
    <Button variant="ghost" size="icon" className="cursor-pointer">
      <MenuDotsIcon size={16} color="#1B1C20" />
    </Button>
  );


  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild ref={anchorRef}>
        {React.isValidElement(trigger) ? trigger : defaultTrigger}
      </PopoverTrigger>
      <PopoverContent
        align={align}
        side={nested ? "right" : undefined}
        className={cn(
          "w-50 p-2 rounded-xl border border-(--neutral-50) shadow-[0px_32px_16px_0px_#1E282E0D,0px_16px_8px_0px_#1E282E0D] bg-white",
          contentClassName,
        )}
      >
        <div className="flex flex-col">
          {renderActions(actions, handleClose)}
        </div>
      </PopoverContent>
    </Popover>
  );
};

const ActionDropdown = (props: ActionDropdownProps) => {
  return <ActionDropdownPopover {...props} />;
};

export default ActionDropdown;
