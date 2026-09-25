import { MenuDotsIcon } from "@/components/icons/commonIcons";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import ActionDropdown from "../shared/ActionDropdown";
import type { AccessLevel, AssessmentSection } from "../../types/create-assessment";

import type { DraggableAttributes } from "@dnd-kit/core";
import type { SyntheticListenerMap } from "@dnd-kit/core/dist/hooks/utilities";

function formatAccessLevelLabel(value: string): AccessLevel {
  const normalized = value.trim().toLowerCase().replace(/[\s-]+/g, "_");
  if (normalized === "therapist_only") return "Therapist Only";
  if (normalized === "client_only") return "Client Only";
  if (normalized === "shared") return "Shared";
  if (value === "Therapist Only" || value === "Client Only" || value === "Shared") {
    return value;
  }
  return "Shared";
}

interface SectionHeaderProps {
  section: AssessmentSection;
  index: number;
  isExpanded: boolean;
  onToggleExpand: () => void;
  onDuplicate: () => void;
  onDelete: () => void;
  onOpenReorder: () => void;
  dragAttributes?: DraggableAttributes;
  dragListeners?: SyntheticListenerMap;
  isMultipleSections: boolean;
}

const SectionHeader = ({
  section,
  index,
  isExpanded,
  onToggleExpand,
  onDuplicate,
  onDelete,
  onOpenReorder,
  isMultipleSections,
  // dragAttributes,
  // dragListeners,
}: SectionHeaderProps) => {
  const accessLevelLabel = formatAccessLevelLabel(section.accessLevel);

  return (
    <div
      className="px-6 py-4 flex items-center justify-between cursor-pointer hover:bg-(--bg-primary-light)/30 border-b border-(--neutral-50)"
      onClick={onToggleExpand}
    >
      <div className="flex items-center gap-4 min-w-0 flex-1">
        {/* <div
          {...dragAttributes}
          {...dragListeners}
          className="p-1 hover:bg-(--neutral-100) rounded-md cursor-grab active:cursor-grabbing transition-colors text-(--text-neutral-300) hover:text-(--text-primary-dark)"
          onClick={(e) => e.stopPropagation()}
        >
          <GripVertical size={20} />
        </div> */}
        <ChevronDown
          size={20}
          className={cn(
            "text-(--text-primary-dark) transition-transform duration-300",
            !isExpanded && "-rotate-90",
          )}
        />
        <h2
          className="text-lg font-bold text-(--text-primary-dark) truncate"
          title={section.title || `Section ${index + 1}`}
        >
          {section.title || `Section ${index + 1}`}
        </h2>
      </div>
      <div className="flex items-center gap-4">
        <span
          className={cn(
            "px-3 py-1 text-xs font-bold rounded-full border border-(--neutral-100)",
            accessLevelLabel === "Therapist Only" &&
              "bg-(--light-blue) text-(--status-billed)",
            accessLevelLabel === "Client Only" &&
              "bg-(--status-completed-light) text-(--dark-green)",
            accessLevelLabel === "Shared" &&
              "bg-(--neutral-100) text-(--text-primary-dark)",
          )}
        >
          {accessLevelLabel}
        </span>
        <span className="text-xs text-(--text-neutral-400) border-l border-(--neutral-100) pl-4">
          {section.questions.length} Question
        </span>
        <ActionDropdown
          contentClassName="w-40"
          actions={[
            {
              label: "Duplicate section",
              onClick: onDuplicate,
            },
            {
              label: "Delete section",
              onClick: onDelete,
            },
            ...(isMultipleSections
              ? [
                  {
                    label: "Move section",
                    onClick: onOpenReorder,
                  },
                ]
              : []),
          ]}
          trigger={
            <button
              className="p-1 hover:bg-(--neutral-100) rounded-full transition-colors cursor-pointer"
              onClick={(e) => e.stopPropagation()}
            >
              <MenuDotsIcon
                size={18}
                className="text-(--text-secondary-light)"
              />
            </button>
          }
        />
      </div>
    </div>
  );
};

export default SectionHeader;
