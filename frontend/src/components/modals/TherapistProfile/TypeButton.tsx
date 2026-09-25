import React from "react";
import { cn } from "@/lib/utils";

interface TypeButtonProps {
  active: boolean;
  onClick: () => void;
  label: string;
}

const TypeButton: React.FC<TypeButtonProps> = ({ active, onClick, label }) => {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex items-center gap-2 px-3 py-2 rounded-2xl text-(--neutral-950) bg-white border transition-all cursor-pointer text-[0.875rem] font-medium",
        active
          ? "border-(--neutral-950)"
          : "border-(--neutral-100) hover:border-(--neutral-200)",
      )}
    >
      <div
        className={`w-4 h-4 rounded-full flex items-center justify-center ${
          active
            ? "border-(--bg-primary-dark) border-5"
            : "border-(--neutral-200) border-2"
        }`}
      />
      {label}
    </button>
  );
};

export default TypeButton;
