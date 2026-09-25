import { MenuDotsIcon, TrashIcon } from "@/components/icons/commonIcons";
import { useCallback, useRef, useState } from "react";
import { User, Settings, Pencil } from "lucide-react";
import { cn } from "@/lib/utils";
import type { AssessmentTemplate } from "../../pages/admin/content/content.static";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useCloseOnScroll } from "@/hooks/useCloseOnScroll";

import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useNavigate } from "react-router-dom";

interface TemplateCardProps {
  template: AssessmentTemplate;
  onAssign?: (template: AssessmentTemplate) => void;
  onEdit?: (template: AssessmentTemplate) => void;
  onDelete?: (template: AssessmentTemplate) => void;
  scrollContainerRef?: React.RefObject<HTMLElement | null>;
  readOnly?: boolean;
  buildAssessmentPath?: string;
}

const TemplateCard = ({
  template,
  onAssign,
  onEdit,
  onDelete,
  scrollContainerRef,
  readOnly = false,
  buildAssessmentPath = "/admin/content/assessment/create-assessment",
}: TemplateCardProps) => {
  const navigate = useNavigate();
  const [isPopoverOpen, setIsPopoverOpen] = useState(false);
  const menuTriggerRef = useRef<HTMLButtonElement>(null);

  const closeMenu = useCallback(() => {
    setIsPopoverOpen(false);
  }, []);

  useCloseOnScroll(
    isPopoverOpen,
    closeMenu,
    menuTriggerRef,
    scrollContainerRef ? [scrollContainerRef] : undefined,
  );

  const handleAction = (action: "Editing" | "Deleting") => {
    if (action === "Deleting") {
      onDelete?.(template);
    } else {
      onEdit?.(template);
    }
    setIsPopoverOpen(false);
  };

  const actionButtonClassName = cn(
    "p-2 border border-(--neutral-100) rounded-lg transition-colors bg-white shadow-xs",
    readOnly
      ? "cursor-not-allowed text-(--text-neutral-400) opacity-50"
      : "text-(--text-neutral-600) hover:text-(--neutral-950) cursor-pointer hover:bg-white",
  );

  const menuButtonClassName = cn(
    "p-2 transition-colors rounded-full",
    readOnly
      ? "cursor-not-allowed text-(--text-neutral-300) opacity-50"
      : "text-(--text-neutral-400) hover:text-(--neutral-950) cursor-pointer hover:bg-(--neutral-50)",
  );

  return (
    <TooltipProvider>
      <div className="bg-white border border-(--neutral-100) rounded-xl shadow-xs hover:shadow-xl transition-shadow duration-300 flex flex-col gap-4">
        <div className="flex items-start justify-between pt-4 px-4 pb-1">
          <h3
            className="text-base font-bold text-(--neutral-950) leading-tight max-w-[70%] truncate"
            title={template.title}
          >
            {template.title}
          </h3>
          <span
            className={cn(
              "text-[0.625rem] font-bold px-3 py-1 rounded-full",
              template.type === "Standard"
                ? "bg-(--text-primary-500) text-white"
                : "bg-(--neutral-100) text-(--text-neutral-600)",
            )}
          >
            {template.type}
          </span>
        </div>

        <div className="space-y-3 px-4">
          <div className="flex justify-between text-xs">
            <span className="text-(--text-neutral-600)">Category</span>
            <span className="text-(--neutral-950) font-medium capitalize">
              {template.category}
            </span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-(--text-neutral-600)">Sections</span>
            <span className="text-(--neutral-950) font-medium">
              {template.sections}
            </span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-(--text-neutral-600)">Created by</span>
            <span className="text-(--neutral-950) font-medium text-right">
              {template.createdBy}
            </span>
          </div>
          <div className="flex justify-between text-xs">
            <span className="text-(--text-neutral-600)">Version</span>
            <span className="text-(--neutral-950) font-medium">
              {template.version}
            </span>
          </div>
        </div>

        <div className="px-4 py-3 pr-2 border-t border-(--neutral-100) bg-(--bg-primary-50) rounded-b-lg flex items-center justify-between">
          <div className="flex gap-2">
            <Tooltip>
              <TooltipTrigger asChild>
                <button
                  type="button"
                  disabled={readOnly}
                  onClick={() => onAssign?.(template)}
                  className={actionButtonClassName}
                >
                  <User size={18} />
                </button>
              </TooltipTrigger>
              <TooltipContent>Assign to Client</TooltipContent>
            </Tooltip>

            <Tooltip>
              <TooltipTrigger asChild>
                <button
                  type="button"
                  disabled={readOnly}
                  onClick={() => {
                    if (readOnly) return;
                    navigate(buildAssessmentPath, {
                      state: {
                        templateId: template.id,
                      },
                    });
                  }}
                  className={actionButtonClassName}
                >
                  <Settings size={18} />
                </button>
              </TooltipTrigger>
              <TooltipContent>Build Assessment</TooltipContent>
            </Tooltip>
          </div>

          <Popover
            open={readOnly ? false : isPopoverOpen}
            onOpenChange={readOnly ? undefined : setIsPopoverOpen}
          >
            <PopoverTrigger asChild>
              <button
                ref={menuTriggerRef}
                type="button"
                disabled={readOnly}
                className={menuButtonClassName}
              >
                <MenuDotsIcon size={20} />
              </button>
            </PopoverTrigger>
            <PopoverContent
              align="end"
              className="w-32 p-1 rounded-xl bg-white"
            >
              <button
                onClick={() => handleAction("Editing")}
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-(--neutral-950) hover:bg-(--neutral-50) transition-colors cursor-pointer border-b border-(--neutral-100)"
              >
                <Pencil size={16} />
                <span>Edit</span>
              </button>
              <button
                onClick={() => handleAction("Deleting")}
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-destructive hover:bg-(--neutral-50) transition-colors cursor-pointer"
              >
                <TrashIcon size={16} />
                <span>Delete</span>
              </button>
            </PopoverContent>
          </Popover>
        </div>
      </div>
    </TooltipProvider>
  );
};

export default TemplateCard;
