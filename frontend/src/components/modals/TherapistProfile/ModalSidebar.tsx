import { CalendarIcon } from "@/components/icons/commonIcons";
import React from "react";
import { User, Award, Video, Lock, BriefcaseBusiness, GraduationCap } from "lucide-react";

interface SidebarItem {
  id: string;
  label: string;
  icon: React.ReactNode;
}

const sidebarItems: SidebarItem[] = [
  { id: "basic", label: "Basic Info", icon: <User size={20} /> },
  { id: "license", label: "License", icon: <Award size={20} /> },
  {
    id: "specializations",
    label: "Specializations",
    icon: <BriefcaseBusiness size={20} />,
  },
  { id: "background", label: "Background", icon: <GraduationCap size={20} /> },
  { id: "schedule", label: "Schedule", icon: <CalendarIcon size={20} /> },
  {
    id: "consultation-schedule",
    label: "Consultation Schedule",
    icon: <CalendarIcon size={20} />,
  },
  { id: "zoom", label: "Zoom Integration", icon: <Video size={20} /> },
  { id: "password", label: "Password", icon: <Lock size={20} /> },
];

interface ModalSidebarProps {
  activeSection: string;
  onSectionChange: (id: string) => void;
}

const ModalSidebar: React.FC<ModalSidebarProps> = ({
  activeSection,
  onSectionChange,
}) => {
  return (
    <div className="w-full md:w-57.5 flex flex-row md:flex-col gap-2 md:gap-1 p-4 py-5 bg-(--bg-primary-light) h-auto md:h-full overflow-x-auto md:overflow-hidden scrollbar-hide [&::-webkit-scrollbar]:hidden border-b border-(--neutral-100) md:border-b-0 md:border-r shrink-0">
      {sidebarItems.map((item) => (
        <button
          key={item.id}
          onClick={() => onSectionChange(item.id)}
          className={`flex items-center gap-2 md:gap-3 px-3 py-2 md:px-4 md:py-3 rounded-full md:rounded-2xl transition-colors cursor-pointer w-auto md:w-full text-left shrink-0
            ${
              activeSection === item.id
                ? "bg-(--neutral-100) text-(--neutral-950) font-medium"
                : "text-(--text-secondary-light) hover:bg-(--neutral-50)"
            }`}
        >
          <span
            className={
              activeSection === item.id
                ? "text-(--neutral-950) [&>svg]:w-4.5 [&>svg]:h-4.5 md:[&>svg]:w-5 md:[&>svg]:h-5"
                : "text-(--text-secondary-light) [&>svg]:w-4.5 [&>svg]:h-4.5 md:[&>svg]:w-5 md:[&>svg]:h-5"
            }
          >
            {item.icon}
          </span>
          <span className="text-[1rem] font-medium text-(--neutral-950) leading-6 whitespace-nowrap">
            {item.label}
          </span>
        </button>
      ))}
    </div>
  );
};

export default ModalSidebar;
