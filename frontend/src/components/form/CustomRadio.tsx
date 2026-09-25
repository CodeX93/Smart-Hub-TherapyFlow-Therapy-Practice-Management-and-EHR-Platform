import { cn } from "@/lib/utils";
import { useId } from "react";
import type { Control, FieldValues, Path } from "react-hook-form";
import {
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";

interface CustomRadioProps<T extends FieldValues> {
  label: string;
  name?: Path<T>;
  options: string[];
  value: string;
  onChange: (value: string) => void;
  orientation?: "vertical" | "horizontal";
  control?: Control<T>;
  required?: boolean;
  className?: string;
  disabled?: boolean;
  hasError?: boolean;
}

const CustomRadioInner = <T extends FieldValues>({
  label,
  options,
  value,
  onChange,
  orientation = "vertical",
  required = false,
  className,
  disabled = false,
  hasError = false,
  name,
}: CustomRadioProps<T>) => {
  const getDisplayLabel = (option: string) => option.split("__opt_")[0];
  const groupId = useId();

  return (
    <fieldset
      disabled={disabled}
      aria-invalid={hasError}
      aria-required={required}
      className={cn("min-w-0 space-y-3", className)}
    >
      <legend className="block text-base font-medium text-(--text-primary-dark) break-words [overflow-wrap:anywhere]">
        {label} {required && <span aria-hidden="true" className="text-red-500">*</span>}
      </legend>
      <div
        className={
          orientation === "vertical" ? "space-y-3" : "flex flex-wrap gap-6"
        }
      >
        {options.map((option, index) => (
          <label
            key={option}
            htmlFor={`${groupId}-${index}`}
            className={cn(
              "group flex min-w-0 cursor-pointer gap-3",
              disabled && "cursor-not-allowed opacity-60",
              orientation === "vertical" ? "w-full items-start" : "max-w-full items-start",
            )}
          >
            <div className="relative mt-0.5 flex shrink-0 items-center justify-center">
              <input
                type="radio"
                id={`${groupId}-${index}`}
                name={name ?? groupId}
                value={option}
                checked={value === option}
                onChange={() => onChange(option)}
                aria-invalid={hasError}
                className="peer h-5 w-5 cursor-pointer appearance-none rounded-full border-2 border-(--neutral-400) bg-white transition-all group-hover:border-(--bg-primary-dark) checked:border-[0.3125rem] checked:border-(--bg-primary-dark) focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-(--neutral-300) focus-visible:ring-offset-2 disabled:cursor-not-allowed"
              />
            </div>
            <span className="min-w-0 flex-1 break-words text-base text-(--text-primary-dark) transition-colors [overflow-wrap:anywhere]">
              {getDisplayLabel(option)}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
};

const CustomRadio = <T extends FieldValues>({
  control,
  name,
  ...props
}: CustomRadioProps<T>) => {
  if (control && name) {
    return (
      <FormField
        control={control}
        name={name}
        render={({ field, fieldState }) => (
          <FormItem>
            <FormControl>
              <CustomRadioInner
                {...props}
                value={field.value}
                onChange={field.onChange}
                name={name}
                hasError={fieldState.invalid}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    );
  }

  return <CustomRadioInner {...props} />;
};

export default CustomRadio;
