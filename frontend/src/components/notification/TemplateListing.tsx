import { CalendarIcon, TrashIcon } from "@/components/icons/commonIcons";
import { Pencil, ChevronDown } from "lucide-react";
import { useState } from "react";
import type { NotificationTemplate } from "../../types/notification";

interface TemplateListingProps {
  template: NotificationTemplate;
  onEdit: (template: NotificationTemplate) => void;
  onDelete: (template: NotificationTemplate) => void;
}

const TemplateListing = ({ template, onEdit, onDelete }: TemplateListingProps) => {
  const [isExpanded, setIsExpanded] = useState(false);
  return (
    <div className="bg-white border border-(--neutral-100) rounded-xl p-5 hover:shadow-sm transition-shadow">
      <div className="flex justify-between items-start mb-2">
        <h3
          className="min-w-0 flex-1 pr-3 truncate text-(--text-primary-dark) font-semibold text-base"
          title={template.title}
        >
          {template.title}
        </h3>
        <div className="flex items-center gap-3">
          <span className="bg-(--neutral-100) text-(--text-neutral-600) px-3 py-1 rounded-full text-xs font-medium">
            {template.tag}
          </span>
          <button
            onClick={() => onEdit(template)}
            className="text-(--text-neutral-600) hover:text-(--text-primary-dark) hover:bg-(--neutral-100) p-1 rounded-full transition-colors cursor-pointer"
          >
            <Pencil size={16} />
          </button>
          <button
            onClick={() => onDelete(template)}
            className="text-(--status-denied) hover:bg-(--neutral-100) p-1 rounded-full transition-colors cursor-pointer"
          >
            <TrashIcon size={16} />
          </button>
        </div>
      </div>

      {template.subject && (
        <p
          className="text-(--text-neutral-600) text-sm mb-1 truncate"
          title={template.subject}
        >
          {template.subject}
        </p>
      )}

      <div className="mb-3 overflow-hidden">
        <div
          className={`transition-all duration-300 ease-in-out ${isExpanded ? "" : "line-clamp-2"}`}
        >
          <p className="text-(--text-neutral-600) text-sm">{template.description}</p>
        </div>
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="flex items-center gap-1 text-(--text-primary-500) text-sm font-medium mt-2 cursor-pointer transition-colors"
        >
          {isExpanded ? "Read Less" : "Read More"}
          <ChevronDown
            size={16}
            className={`transition-transform duration-300 ease-in-out ${isExpanded ? "rotate-180" : ""}`}
          />
        </button>
      </div>

      <div className="flex items-center gap-2 text-(--text-neutral-400) text-xs">
        <CalendarIcon size={14} />
        <span>Created {template.createdDate}</span>
      </div>
    </div>
  );
};

export default TemplateListing;
