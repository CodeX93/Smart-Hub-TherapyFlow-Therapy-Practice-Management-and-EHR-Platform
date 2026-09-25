import React from "react";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

interface CheckboxProps extends Omit<
  React.InputHTMLAttributes<HTMLInputElement>,
  "onChange"
> {
  id: string;
  onCheckedChange?: (checked: boolean) => void;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

const Checkbox: React.FC<CheckboxProps> = ({
  id,
  className,
  onCheckedChange,
  onChange,
  ...props
}) => {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (onChange) {
      onChange(e);
    }
    if (onCheckedChange) {
      onCheckedChange(e.target.checked);
    }
  };

  return (
    <div className="relative flex items-center justify-center">
      <input
        type="checkbox"
        id={id}
        className={cn(
          "peer h-5 w-5 cursor-pointer appearance-none rounded-[0.375rem] border border-(--neutral-200) transition-all checked:border-(--bg-primary-dark) checked:bg-(--bg-primary-dark) focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-(--neutral-300) focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 checked:disabled:border-(--bg-primary-dark) checked:disabled:bg-(--bg-primary-dark)",
          className,
        )}
        onChange={handleChange}
        {...props}
      />
      <Check
        size={14}
        aria-hidden="true"
        className="pointer-events-none absolute scale-0 text-white transition-transform peer-checked:scale-100 peer-disabled:opacity-60"
      />
    </div>
  );
};

export { Checkbox };
