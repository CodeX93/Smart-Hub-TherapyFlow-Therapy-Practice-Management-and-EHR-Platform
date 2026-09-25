import React, { useRef, useState, type RefObject } from "react";
import { ChevronDown, Search, Check } from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import CustomInput from "./CustomInput";
import type { Control, FieldValues, Path } from "react-hook-form";
import {
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";

import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { Info } from "lucide-react";
import { useCloseOnScroll } from "@/hooks/useCloseOnScroll";

export interface CustomSelectOption {
  value: string;
  label: string;
  disabled?: boolean;
  description?: string;
  helpText?: string[]; // For permissions overview
  group?: string;
  icon?: React.ReactNode;
}

interface CustomSelectProps<
  T extends FieldValues,
  O extends CustomSelectOption = CustomSelectOption,
> {
  label?: string;
  value?: string;
  onChange?: (value: string) => void;
  options: O[];
  placeholder?: string;
  required?: boolean;
  renderOption?: (option: O) => React.ReactNode;
  control?: Control<T>;
  name?: Path<T>;
  className?: string;
  contentClassName?: string;
  isSearch?: boolean;
  isIcon?: boolean;
  isGrouped?: boolean;
  disabled?: boolean;
  onOpenChange?: (open: boolean) => void;
  onSearchChange?: (query: string) => void;
  /** When true, options are already filtered by the parent (e.g. server search). */
  disableLocalFilter?: boolean;
  onMenuScrollToEnd?: () => void;
  hasMore?: boolean;
  isLoadingMore?: boolean;
  loadingMoreLabel?: string;
  closeOnScroll?: boolean;
  portalContainer?: HTMLElement | null;
  portalContainerRef?: RefObject<HTMLElement | null>;
  scrollContainerRefs?: RefObject<HTMLElement | null>[];
  avoidCollisions?: boolean;
  side?: "top" | "bottom" | "left" | "right";
  collisionPadding?: number | Partial<Record<"top" | "bottom" | "left" | "right", number>>;
  hasError?: boolean;
  /** When true, selected label wraps instead of truncating with ellipsis. */
  wrapSelectedLabel?: boolean;
}

function CustomSelectInner<
  T extends FieldValues,
  O extends CustomSelectOption = CustomSelectOption,
>({
  label,
  isSearch = true,
  isIcon = true,
  value,
  onChange,
  className,
  contentClassName,
  options,
  placeholder = "Search...",
  required = false,
  renderOption,
  isGrouped = false,
  disabled = false,
  onOpenChange,
  onSearchChange,
  disableLocalFilter = false,
  onMenuScrollToEnd,
  hasMore = false,
  isLoadingMore = false,
  loadingMoreLabel = "Loading more...",
  closeOnScroll = false,
  portalContainer = null,
  portalContainerRef,
  scrollContainerRefs,
  avoidCollisions = true,
  side = "bottom",
  collisionPadding = 12,
  hasError = false,
  wrapSelectedLabel = false,
}: CustomSelectProps<T, O>) {
  const [searchQuery, setSearchQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [portalContainerNode, setPortalContainerNode] =
    useState<HTMLElement | null>(portalContainer);
  const triggerRef = useRef<HTMLButtonElement>(null);

  const handleOpenChange = (nextOpen: boolean) => {
    if (nextOpen) {
      setPortalContainerNode(portalContainerRef?.current ?? portalContainer);
    } else {
      setSearchQuery("");
      onSearchChange?.("");
    }
    setOpen(nextOpen);
    onOpenChange?.(nextOpen);
  };

  useCloseOnScroll(
    closeOnScroll && open,
    () => handleOpenChange(false),
    triggerRef,
    scrollContainerRefs,
  );

  const filteredOptions = disableLocalFilter
    ? options
    : options.filter((option) =>
        option.label.toLowerCase().includes(searchQuery.toLowerCase()),
      );

  const groupedOptions = React.useMemo(() => {
    if (!isGrouped) return [{ group: null, items: filteredOptions }];

    const groups: { group: string | null; items: O[] }[] = [];
    filteredOptions.forEach((opt) => {
      const groupName = opt.group || null;
      let group = groups.find((g) => g.group === groupName);
      if (!group) {
        group = { group: groupName, items: [] };
        groups.push(group);
      }
      group.items.push(opt);
    });
    return groups;
  }, [filteredOptions, isGrouped]);

  const selectedOption = options.find((opt) => opt.value === value);
  const selectedLabel = selectedOption?.label || "";
  const selectedIcon = selectedOption?.icon;
  const resolvedPortalContainer =
    portalContainerNode ?? portalContainer ?? undefined;

  return (
    <Popover
      open={disabled ? false : open}
      onOpenChange={
        disabled
          ? undefined
          : handleOpenChange
      }
    >
      <PopoverTrigger asChild>
        <button
          ref={triggerRef}
          type="button"
          disabled={disabled}
          aria-label={label ?? placeholder}
          aria-invalid={hasError}
          className={cn(
            "group w-full rounded-xl flex justify-between gap-2 border border-(--neutral-100) hover:border-(--neutral-600) focus:border-(--neutral-600) transition-colors shadow-(--shadow) pt-7 px-3 pb-2 cursor-pointer relative text-left bg-transparent focus-visible:outline-none focus-visible:ring-0 overflow-hidden",
            wrapSelectedLabel ? "min-h-15 h-auto items-start" : "h-15 items-center",
            disabled && "cursor-not-allowed opacity-60 hover:border-(--neutral-100)",
            hasError && "border-destructive hover:border-destructive focus:border-destructive",
            className,
          )}
        >
          {label && (
            <span
              className={cn(
                "absolute left-3 transition-all duration-200 pointer-events-none",
                selectedLabel || open
                  ? "top-3.5 -translate-y-1/2 text-xs text-(--text-secondary-light)"
                  : "top-1/2 -translate-y-1/2 text-(--text-secondary-light)",
              )}
            >
              {label} {required && <span className="text-red-500">*</span>}
            </span>
          )}
          <div
            className={cn(
              "flex min-w-0 max-w-full gap-2",
              wrapSelectedLabel ? "items-start" : "items-center truncate",
            )}
          >
            {selectedIcon && (
              <span className="text-(--text-neutral-400) shrink-0 w-4.5 h-4.5">
                {selectedIcon}
              </span>
            )}
            <span
              className={cn(
                "block min-w-0 text-sm text-(--neutral-950)",
                wrapSelectedLabel
                  ? "whitespace-normal break-all [overflow-wrap:anywhere] leading-snug"
                  : "truncate",
              )}
              title={selectedLabel}
            >
              {selectedLabel}
            </span>
          </div>
          {isIcon && (
            <div className={cn(label && !wrapSelectedLabel && "-translate-y-1/2", "shrink-0")}>
              <ChevronDown
                className={cn(
                  "h-5 w-5 text-(--text-neutral-400) opacity-50 transition-transform",
                  open && "rotate-180",
                )}
              />
            </div>
          )}
        </button>
      </PopoverTrigger>

      <PopoverContent
        container={resolvedPortalContainer}
        className={cn(
          "z-9999 flex w-(--radix-popover-trigger-width) flex-col overflow-hidden rounded-xl border border-(--neutral-100) bg-white p-0 shadow-xl",
          resolvedPortalContainer
            ? "max-w-full"
            : "max-w-[calc(100vw-2rem)]",
          contentClassName,
        )}
        align="start"
        side={side}
        sideOffset={4}
        collisionPadding={collisionPadding}
        avoidCollisions={avoidCollisions}
        style={{
          maxHeight: "min(15rem, var(--radix-popover-content-available-height, 15rem))",
        }}
      >
        {isSearch && (
          <div className="shrink-0 border-b border-(--neutral-50) p-2">
            <CustomInput
              placeholder={placeholder}
              value={searchQuery}
              onChange={(e) => {
                const nextQuery = e.target.value;
                setSearchQuery(nextQuery);
                onSearchChange?.(nextQuery);
              }}
              icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
              className="min-h-10 rounded-full bg-white pb-0 pt-1.75"
            />
          </div>
        )}
        <div
          className="overflow-y-auto overscroll-contain"
          style={{ maxHeight: isSearch ? "11.5rem" : "15rem" }}
          onScroll={(event) => {
            if (!onMenuScrollToEnd || !hasMore || isLoadingMore) return;
            const target = event.currentTarget;
            const threshold = 24;
            const atBottom =
              target.scrollTop + target.clientHeight >= target.scrollHeight - threshold;
            if (atBottom) {
              onMenuScrollToEnd();
            }
          }}
        >
          {groupedOptions.some((group) => group.items.length > 0) ? (
            groupedOptions.map((group, groupIdx) => (
              <React.Fragment key={group.group || groupIdx}>
                {isGrouped && group.group && (
                  <div className="px-3 py-2 bg-(--bg-primary-light)">
                    <span className="text-xs font-medium uppercase tracking-wider text-(--text-neutral-600)">
                      {group.group}
                    </span>
                  </div>
                )}
                {group.items.map((opt) => (
                  <div
                    key={opt.value}
                    className={cn(
                      "group/item px-3 py-2.5 hover:bg-(--bg-primary-light)/50 cursor-pointer border-b border-(--neutral-50) last:border-0 transition-colors text-(--text-primary-dark) font-medium text-sm flex items-start justify-between gap-2",
                      opt.disabled && "opacity-50 cursor-not-allowed",
                      opt.value === value && "bg-(--bg-primary-light)/40",
                      isGrouped && "px-6",
                    )}
                    onClick={() => {
                      if (!opt.disabled) {
                        onChange?.(opt.value);
                        setOpen(false);
                        setSearchQuery("");
                      }
                    }}
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5 mb-0.5">
                        <div className="flex min-w-0 flex-1 items-center gap-2">
                          {opt.icon && (
                            <span className="shrink-0 text-(--text-neutral-400) transition-colors group-hover/item:text-(--text-primary-dark)">
                              {opt.icon}
                            </span>
                          )}
                          {renderOption ? (
                            <div className="min-w-0 flex-1">{renderOption(opt)}</div>
                          ) : (
                            <span
                              className={cn(
                                "min-w-0",
                                wrapSelectedLabel
                                  ? "break-all [overflow-wrap:anywhere] whitespace-normal"
                                  : "truncate",
                              )}
                              title={opt.label}
                            >
                              {opt.label}
                            </span>
                          )}
                        </div>
                        {opt.helpText && (
                          <TooltipProvider>
                            <Tooltip delayDuration={300}>
                              <TooltipTrigger
                                asChild
                                onClick={(e) => e.stopPropagation()}
                              >
                                <Info className="size-3.5 text-(--text-neutral-400) hover:text-(--text-primary-dark) cursor-help" />
                              </TooltipTrigger>
                              <TooltipContent className="p-4 rounded-xl border border-(--neutral-100) bg-white shadow-lg space-y-2">
                                <p className="font-semibold text-(--text-primary-dark) text-sm">
                                  Permissions Overview
                                </p>
                                <ul className="space-y-1">
                                  {opt.helpText.map((text, i) => (
                                    <li
                                      key={i}
                                      className="flex items-center gap-2 text-sm text-(--text-neutral-600)"
                                    >
                                      <Check className="size-3.5 text-(--text-neutral-600)" />
                                      {text}
                                    </li>
                                  ))}
                                </ul>
                              </TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        )}
                      </div>
                      {opt.description && (
                        <p className="truncate text-[0.6875rem] text-(--text-neutral-400) font-normal leading-tight" title={opt.description}>
                          {opt.description}
                        </p>
                      )}
                    </div>
                    {opt.value === value && (
                      <Check className="size-4.5 text-(--text-primary-dark) mt-0.5 shrink-0" />
                    )}
                  </div>
                ))}
              </React.Fragment>
            ))
          ) : !isLoadingMore ? (
            <div className="p-4 text-sm text-center text-(--text-neutral-400)">
              No results found
            </div>
          ) : null}
          {isLoadingMore ? (
            <div className="px-3 py-2.5 text-xs text-center text-(--text-neutral-500)">
              {loadingMoreLabel}
            </div>
          ) : null}
        </div>
      </PopoverContent>
    </Popover>
  );
}

function CustomSelect<
  T extends FieldValues,
  O extends CustomSelectOption = CustomSelectOption,
>(props: CustomSelectProps<T, O>) {
  const { control, name } = props;

  if (control && name) {
    return (
      <FormField
        control={control}
        name={name}
        render={({ field, fieldState }) => (
          <FormItem>
            <FormControl>
              <CustomSelectInner
                {...props}
                {...field}
                hasError={fieldState.invalid}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    );
  }

  return <CustomSelectInner {...props} />;
}

export default CustomSelect;
