import React from "react";
import licenseIcon from "@/assets/figma/professional-profile/license.png";
import specializationsIcon from "@/assets/figma/professional-profile/specializations.png";
import backgroundIcon from "@/assets/figma/professional-profile/background.png";
import scheduleIcon from "@/assets/figma/professional-profile/schedule.png";
import emergencyContactIcon from "@/assets/figma/professional-profile/emergency-contact.png";

interface SidebarItem {
  id: string;
  label: string;
  iconSrc: string;
}

const sidebarItems: SidebarItem[] = [
  { id: "license", label: "License", iconSrc: licenseIcon },
  {
    id: "specializations",
    label: "Specializations",
    iconSrc: specializationsIcon,
  },
  { id: "background", label: "Background", iconSrc: backgroundIcon },
  { id: "schedule", label: "Schedule", iconSrc: scheduleIcon },
  {
    id: "consultationSchedule",
    label: "Consultation Schedule",
    iconSrc: scheduleIcon,
  },
  {
    id: "emergencyContact",
    label: "Emergency Contact",
    iconSrc: emergencyContactIcon,
  },
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
    <div className="w-full md:w-55 flex flex-row md:flex-col gap-2 md:gap-1 p-2 py-5 bg-(--bg-primary-light) h-auto md:h-full overflow-x-auto md:overflow-hidden scrollbar-hide [&::-webkit-scrollbar]:hidden border-b border-(--neutral-100) shrink-0">
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
          <span className="inline-flex size-4.5 shrink-0 items-center justify-center overflow-hidden md:size-5">
            <img
              src={item.iconSrc}
              alt=""
              width={20}
              height={20}
              className="size-full object-contain"
              aria-hidden="true"
            />
          </span>
          <span className="text-sm font-medium text-(--neutral-950) leading-6 whitespace-nowrap">
            {item.label}
          </span>
        </button>
      ))}
    </div>
  );
};

export default ModalSidebar;
