import { ChevronDown } from "lucide-react";
import { Button } from "../ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import { cn } from "@/lib/utils";
import { truncateSessionNoteAiTemplateName } from "@/utils/sessionNoteAiTemplateName";

interface SessionNoteAiTemplateSelectProps {
  templates: { id: number; name: string }[];
  value: number | null;
  onChange: (templateId: number | null) => void;
  disabled?: boolean;
}

const SessionNoteAiTemplateSelect = ({
  templates,
  value,
  onChange,
  disabled = false,
}: SessionNoteAiTemplateSelectProps) => {
  const selectedTemplate = templates.find((template) => template.id === value);
  const triggerLabel = selectedTemplate
    ? truncateSessionNoteAiTemplateName(selectedTemplate.name)
    : "Select template";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="outline"
          disabled={disabled}
          className="h-10 w-full max-w-72 justify-between gap-2 overflow-hidden rounded-full border border-(--neutral-100) bg-white px-4 text-sm font-normal shadow-(--shadow) hover:bg-(--neutral-50) disabled:opacity-50"
        >
          <span
            className={cn(
              "min-w-0 flex-1 truncate text-left",
              selectedTemplate
                ? "text-(--text-primary-dark)"
                : "text-(--text-neutral-500)",
            )}
            title={selectedTemplate?.name ?? "Select template"}
          >
            {triggerLabel}
          </span>
          <ChevronDown
            size={16}
            className="shrink-0 text-(--text-neutral-600)"
            strokeWidth={1.5}
          />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="start"
        className="max-h-60 w-[var(--radix-dropdown-menu-trigger-width)] min-w-48 max-w-72 overflow-y-auto rounded-xl p-1"
      >
        <DropdownMenuItem
          className={cn(
            "cursor-pointer rounded-lg px-3 py-2 text-sm",
            value === null && "bg-(--bg-primary-50) font-medium",
          )}
          onSelect={() => onChange(null)}
        >
          No template
        </DropdownMenuItem>
        {templates.map((template) => (
          <DropdownMenuItem
            key={template.id}
            className={cn(
              "cursor-pointer rounded-lg px-3 py-2 text-sm",
              value === template.id && "bg-(--bg-primary-50) font-medium",
            )}
            onSelect={() => onChange(template.id)}
            title={template.name}
          >
            <span className="block min-w-0 max-w-full truncate">
              {truncateSessionNoteAiTemplateName(template.name)}
            </span>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default SessionNoteAiTemplateSelect;
