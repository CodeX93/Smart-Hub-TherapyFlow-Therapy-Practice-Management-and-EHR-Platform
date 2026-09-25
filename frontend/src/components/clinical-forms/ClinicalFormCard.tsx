
import { CalendarIcon } from "@/components/icons/commonIcons";
import { useNavigate } from "react-router-dom";
import { Button } from "../ui/button";
import type { ClinicalForm } from "../../types/clinical-form.type";

interface ClinicalFormCardProps {
  form: ClinicalForm;
  onAction?: (formId: string, action: string) => void;
}

const ClinicalFormCard = ({ form, onAction }: ClinicalFormCardProps) => {
  const navigate = useNavigate();
  const getStatusBadgeClass = () => {
    switch (form.status) {
      case "pending":
        return "bg-[#EDEEF1] text-(--text-primary-dark)";
      case "in-progress":
        return "bg-[#EDEEF1] text-(--text-primary-dark)";
      case "completed":
        return "bg-[#D0FBE3] text-(--text-primary-dark)";
      default:
        return "bg-[#EDEEF1] text-(--text-primary-dark)";
    }
  };

  const getButtonConfig = () => {
    switch (form.status) {
      case "pending":
        return {
          label: "Start",
          className:
            "bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white border-none",
        };
      case "in-progress":
        return {
          label: "Continue",
          className:
            "bg-white hover:bg-gray-50 text-(--text-primary-dark) border border-(--text-neutral-200)",
        };
      case "completed":
        return {
          label: "View",
          className:
            "bg-white hover:bg-gray-50 text-(--text-primary-dark) border border-(--text-neutral-200)",
        };
      default:
        return {
          label: "Start",
          className:
            "bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white border-none",
        };
    }
  };

  const getStatusLabel = () => {
    switch (form.status) {
      case "pending":
        return "Pending";
      case "in-progress":
        return "In progress";
      case "completed":
        return "Completed";
      default:
        return "Pending";
    }
  };

  const buttonConfig = getButtonConfig();
  const dateText =
    form.status === "completed"
      ? `Completed date: ${form.completedDate}`
      : `Assigned date: ${form.assignedDate}`;

  return (
    <div className="w-full h-[11.25rem] border border-(--text-neutral-100) rounded-lg shadow-xs flex flex-col overflow-hidden">
      {/* Title and Badges - White Background */}
      <div className="flex-1 p-4 pb-0 bg-white">
        <h3
          className="text-(--text-primary-dark) mb-3 line-clamp-2"
          style={{
            fontFamily: "Manrope",
            fontWeight: 600,
            fontSize: "1rem",
            lineHeight: "1.5rem",
            letterSpacing: "0%",
          }}
        >
          {form.title}
        </h3>

        {/* Badges */}
        <div className="flex gap-2">
          <span
            className="inline-flex items-center justify-center rounded-full bg-[#EBEFFF] text-[#5878F8] px-2 py-0.5"
            style={{
              fontFamily: "Manrope",
              fontWeight: 400,
              fontSize: "0.75rem",
              lineHeight: "1.125rem",
              letterSpacing: "0%",
              height: "1.25rem",
            }}
          >
            {form.category}
          </span>
          <span
            className={`inline-flex items-center justify-center rounded-full px-2 py-0.5 ${getStatusBadgeClass()}`}
            style={{
              fontFamily: "Manrope",
              fontWeight: 400,
              fontSize: "0.75rem",
              lineHeight: "1.125rem",
              letterSpacing: "0%",
              height: "1.25rem",
            }}
          >
            {getStatusLabel()}
          </span>
        </div>
      </div>

      {/* Date and Action - #F3F7F8 Background */}
      <div
        className="flex items-center justify-between border-t border-(--text-neutral-100) bg-[#F3F7F8]"
        style={{
          height: "3.75rem",
          padding: "0.75rem 1rem",
        }}
      >
        <div className="flex items-center gap-2">
          <CalendarIcon size={16} className="text-(--text-neutral-600)" />
          <span
            className="text-(--text-neutral-600)"
            style={{
              fontFamily: "Manrope",
              fontWeight: 400,
              fontSize: "0.875rem",
              lineHeight: "1.375rem",
              letterSpacing: "0%",
            }}
          >
            {dateText}
          </span>
        </div>
        <Button
          onClick={() => {
            onAction?.(form.id, buttonConfig.label.toLowerCase());
            navigate(`/user/clinical-forms/${form.id}`);
          }}
          className={`${buttonConfig.className} w-20 h-9 rounded-full cursor-pointer`}
          style={{
            fontFamily: "Manrope",
            fontWeight: 600,
            fontSize: "0.875rem",
            lineHeight: "1.375rem",
            padding: "0.4375rem 1rem",
          }}
        >
          {buttonConfig.label}
        </Button>
      </div>
    </div>
  );
};

export default ClinicalFormCard;
