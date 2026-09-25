import { CalendarIcon, TrashIcon } from "@/components/icons/commonIcons";
import { CheckCheck, Pencil, Plus } from "lucide-react";
import type { ActionItem } from "./ActionsDropdown";

// Predefined action items for common use cases
export const createClientActions = (
  onChecklist: () => void,
  onScheduledSession: () => void,
  onEdit: () => void,
  onCreateTask: () => void,
  onDelete: () => void,
  options?: {
    disableCreateTask?: boolean;
    disableEdit?: boolean;
    hideScheduledSession?: boolean;
    hideCreateTask?: boolean;
    hideDelete?: boolean;
    hideEdit?: boolean;
  },
): ActionItem[] => [
    {
      id: "checklist",
      label: "Checklist",
      icon: <CheckCheck size={18} className="text-(--text-neutral-600)" />,
      onClick: onChecklist,
    },
    {
      id: "scheduled-session",
      label: "Scheduled Session",
      icon: <CalendarIcon size={18} className="text-(--text-neutral-600)" />,
      onClick: onScheduledSession,
      hidden: options?.hideScheduledSession,
    },
    {
      id: "edit",
      label: "Edit Client",
      icon: <Pencil size={18} className="text-(--text-neutral-600)" />,
      onClick: onEdit,
      disabled: options?.disableEdit,
      hidden: options?.hideEdit,
    },
    {
      id: "create-task",
      label: "Create Task",
      icon: <Plus size={18} className="text-(--text-neutral-600)" />,
      onClick: onCreateTask,
      disabled: options?.disableCreateTask,
      hidden: options?.hideCreateTask,
    },
    {
      id: "delete",
      label: "Delete Client",
      icon: <TrashIcon size={18} className="text-red-500" />,
      onClick: onDelete,
      variant: "destructive",
      hidden: options?.hideDelete,
    },
  ];
