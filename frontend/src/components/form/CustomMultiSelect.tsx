import { useState, useRef, useEffect } from "react";
import { X, ChevronDown, Search } from "lucide-react";
import { cn } from "@/lib/utils";
import { Checkbox } from "@/components/ui/checkbox";

interface MultiSelectOption {
  id: string;
  label: string;
  category?: string;
  required?: boolean;
}

interface MultiSelectProps {
  options: MultiSelectOption[];
  value: string[];
  onChange: (value: string[]) => void;
  placeholder?: string;
  searchPlaceholder?: string;
  label?: string;
  required?: boolean;
  isSearch?: boolean;
  className?: string;
}

const categoryStyles = {
  Intake: "bg-(--light-blue) text-(--status-billed)",
  Assessment: "bg-(--neutral-100) text-(--text-primary-dark)",
  Ongoing: "bg-(--status-completed-light) text-(--dark-green)",
  Discharge: "bg-(--status-completed-light) text-(--dark-green)",
};

const CustomMultiSelect = ({
  options,
  value,
  onChange,
  isSearch = true,
  placeholder = "Select items",
  searchPlaceholder = "Search items",
  label,
  required,
  className,
}: MultiSelectProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [openUpward, setOpenUpward] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    if (!isOpen || !dropdownRef.current) return;

    const checkPosition = () => {
      const triggerRect = dropdownRef.current?.querySelector(
        "[class*='rounded-xl border']",
      ) as HTMLElement;
      if (!triggerRect) return;

      const triggerBottom = triggerRect.getBoundingClientRect().bottom;
      const viewportHeight = window.innerHeight;
      const dropdownHeight = 350;

      const shouldOpenUpward =
        viewportHeight - triggerBottom < dropdownHeight + 20;
      setOpenUpward(shouldOpenUpward);
    };

    setTimeout(checkPosition, 0);

    window.addEventListener("scroll", checkPosition);
    window.addEventListener("resize", checkPosition);

    return () => {
      window.removeEventListener("scroll", checkPosition);
      window.removeEventListener("resize", checkPosition);
    };
  }, [isOpen]);

  const selectedOptions = options.filter((opt) => value.includes(opt.id));
  const unselectedOptions = options.filter((opt) => !value.includes(opt.id));

  // Sort: selected items first, then unselected
  const sortedOptions = [...selectedOptions, ...unselectedOptions];

  const filteredOptions = sortedOptions.filter((opt) =>
    opt.label.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  const handleToggle = (optionId: string) => {
    if (value.includes(optionId)) {
      onChange(value.filter((id) => id !== optionId));
    } else {
      onChange([...value, optionId]);
    }
  };

  const handleRemove = (optionId: string) => {
    onChange(value.filter((id) => id !== optionId));
  };

  const selectedCount = value.length;
  const maxVisibleTags = 2;
  const visibleTags = selectedOptions.slice(0, maxVisibleTags);
  const remainingCount = selectedCount - maxVisibleTags;

  const hasValue = selectedCount > 0;

  return (
    <div className="space-y-1.5" ref={dropdownRef}>
      <div className="relative">
        <div
          onClick={() => setIsOpen(!isOpen)}
          className={cn(
            "group w-full border border-(--neutral-100) hover:border-(--neutral-600) shadow-xs px-3 cursor-pointer transition-colors flex items-center gap-2 bg-white",
            label ? "min-h-15 pt-7 pb-2 rounded-xl" : "rounded-full min-h-11 py-2",
            isOpen && "border-(--neutral-600)",
            className,
          )}
        >
          {label && (
            <span
              className={cn(
                "absolute left-3 transition-all duration-200 pointer-events-none",
                hasValue || isOpen
                  ? "top-3.5 -translate-y-1/2 text-xs text-(--text-secondary-light)"
                  : "top-1/2 -translate-y-1/2 text-(--text-secondary-light)",
              )}
            >
              {label} {required && <span className="text-red-500">*</span>}
            </span>
          )}

          {selectedCount === 0 ? (
            <span
              className={cn(
                "text-sm w-full text-(--text-neutral-400)",
                label ? "opacity-0" : "opacity-100 text-(--text-primary-dark)",
              )}
            >
              {placeholder}
            </span>
          ) : (
            <div className="flex items-center gap-2 flex-1 overflow-hidden">
              {visibleTags.map((option) => (
                <span
                  key={option.id}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-(--neutral-100) text-(--text-primary-dark) rounded-full text-xs font-medium whitespace-nowrap"
                >
                  {option.label}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleRemove(option.id);
                    }}
                    className="hover:bg-(--neutral-200) rounded-full p-0.5 transition-colors cursor-pointer"
                  >
                    <X size={12} />
                  </button>
                </span>
              ))}
              {remainingCount > 0 && (
                <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-(--text-primary-dark) text-white rounded-full text-xs font-medium whitespace-nowrap shrink-0">
                  +{remainingCount}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onChange([]);
                    }}
                    className="cursor-pointer"
                  >
                    <X size={12} />
                  </button>
                </span>
              )}
            </div>
          )}
          <ChevronDown
            size={18}
            className={cn(
              "text-(--text-neutral-400) transition-transform shrink-0",
              isOpen && "rotate-180",
            )}
          />
        </div>

        {isOpen && (
          <div
            className={cn(
              "absolute z-50 w-full bg-white border border-(--neutral-100) rounded-xl shadow-lg max-h-80 overflow-hidden",
              openUpward ? "bottom-full mb-2" : "top-full mt-2",
            )}
          >
            {isSearch &&<div className="p-3 border-b border-(--neutral-100)">
              <div className="relative">
                <Search
 className="size-4.5 absolute left-3 top-1/2 -translate-y-1/2 text-(--text-neutral-400)" />
                <input
                  type="text"
                  placeholder={searchPlaceholder}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-(--neutral-100) rounded-full text-sm shadow-xs focus:outline-none focus:border-(--neutral-400)"
                  onClick={(e) => e.stopPropagation()}
                />
              </div>
            </div>}

            <div className="max-h-64 overflow-y-auto custom-scrollbar">
              {filteredOptions.map((option) => (
                <div
                  key={option.id}
                  onClick={() => handleToggle(option.id)}
                  className="flex items-center justify-between px-4 py-3 hover:bg-(--neutral-50) cursor-pointer transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <Checkbox
                      id={`checkbox-${option.id}`}
                      checked={value.includes(option.id)}
                      onCheckedChange={() => handleToggle(option.id)}
                      className="rounded border-(--neutral-200) data-[state=checked]:bg-(--bg-primary-dark) data-[state=checked]:border-(--bg-primary-dark)"
                    />
                    <span className="text-sm text-(--text-primary-dark) font-medium">
                      {option.label}{" "}
                      {option.required && (
                        <span className="text-red-500">*</span>
                      )}
                    </span>
                  </div>
                  {option.category && (
                    <span
                      className={cn(
                        "px-3 py-1 rounded-full text-xs font-medium",
                        categoryStyles[
                          option.category as keyof typeof categoryStyles
                        ],
                      )}
                    >
                      {option.category}
                    </span>
                  )}
                </div>
              ))}
              {filteredOptions.length === 0 && (
                <div className="px-4 py-8 text-center text-sm text-(--text-neutral-500)">
                  No items found
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CustomMultiSelect;
