import React, { useState } from "react";
import { sanitizePracticePhoneInput } from "@/utils/practiceConfigInput";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { Eye, EyeOff } from "lucide-react";
import type { Control, FieldValues, Path } from "react-hook-form";
import { useLocation } from "react-router-dom";
import {
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";

interface CustomInputProps<
  T extends FieldValues,
> extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  value?: string | number;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  required?: boolean;
  icon?: React.ReactNode;
  suffix?: React.ReactNode;
  stopFloating?: boolean;
  placeholder?: string;
  hint?: string;
  control?: Control<T>;
  name?: Path<T>;
  digitsOnly?: boolean;
  decimalOnly?: boolean;
  optionalPlusPhone?: boolean;
  hasError?: boolean;
}

const CustomInputInner = <T extends FieldValues>({
  label,
  placeholder,
  value = "",
  onChange,
  required = false,
  icon,
  suffix,
  hint,
  stopFloating: customIsFloating,
  className,
  onFocus,
  onBlur,
  type,
  hasError = false,
  digitsOnly,
  decimalOnly,
  optionalPlusPhone,
  ...props
}: CustomInputProps<T>) => {
  const [isFocused, setIsFocused] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const location = useLocation();
  const isSuperAdminRoute = location.pathname.startsWith("/super-admin");

  const normalizeInputValue = (rawValue: string): string => {
    if (decimalOnly) {
      return rawValue.replace(/[^0-9.]/g, "").replace(/(\..*)\./g, "$1");
    }

    if (optionalPlusPhone) {
      return sanitizePracticePhoneInput(rawValue);
    }

    if (digitsOnly) {
      return rawValue.replace(/\D/g, "");
    }

    return rawValue;
  };

  const handleFocus = (e: React.FocusEvent<HTMLInputElement>) => {
    setIsFocused(true);
    if (onFocus) onFocus(e);
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    setIsFocused(false);
    if (onBlur) onBlur(e);
  };

  const isLabelFloating = customIsFloating ?? (isFocused || value?.toString().length > 0);
  const isPassword = type === "password";
  const inputType = isPassword ? (showPassword ? "text" : "password") : type;
  const isDisabled = Boolean(props.disabled || props.readOnly);

  return (
    <label className="relative group flex flex-col w-full min-w-0">
      <div
        className={cn(
          "relative w-full min-h-15 min-w-0 rounded-xl border border-(--neutral-100) shadow-xs pt-7 pb-2 px-3 transition-colors bg-white",
          !isDisabled && "hover:border-(--neutral-600) cursor-text",
          isFocused && !isDisabled && "border-(--neutral-600)",
          hasError && "border-destructive hover:border-destructive",
          isDisabled && "cursor-not-allowed bg-gray-50 hover:border-(--neutral-100)",
          isPassword && "pr-10",
          className,
        )}
      >
        <div className="flex min-w-0 items-center gap-1 overflow-hidden">
          {icon && (
            <span className="text-base text-(--neutral-950)">{icon}</span>
          )}
          <Input
            {...props}
            type={inputType}
            value={value}
            onChange={(event) => {
              const rawValue = event.target.value;
              const normalizedValue = normalizeInputValue(rawValue);

              if (normalizedValue !== rawValue) {
                event.target.value = normalizedValue;
              }

              if (isSuperAdminRoute && type !== "password") {
                const startsWithSpace = /^\s/.test(event.target.value);
                event.target.setCustomValidity(
                  startsWithSpace ? "Value cannot start with a space" : "",
                );
              }

              onChange?.(event);
            }}
            onFocus={handleFocus}
            aria-invalid={hasError || props["aria-invalid"]}
            onBlur={(event) => {
              if (isSuperAdminRoute && type !== "password") {
                const startsWithSpace = /^\s/.test(event.target.value);
                event.target.setCustomValidity(
                  startsWithSpace ? "Value cannot start with a space" : "",
                );
                event.target.reportValidity();
              }
              handleBlur(event);
            }}
            title={
              isDisabled && value?.toString()
                ? value.toString()
                : props.title
            }
            className={cn(
              "h-6 min-w-0 w-full p-0 border-none focus-visible:ring-0 focus-visible:ring-offset-0 shadow-none text-base text-(--neutral-950) bg-transparent flex-1 peer truncate",
              isDisabled && "cursor-not-allowed text-(--text-neutral-600)",
            )}
            placeholder={placeholder || " "}
          />
          <span
            className={cn(
              "absolute left-3 right-3 transition-all duration-200 pointer-events-none whitespace-normal break-words [overflow-wrap:anywhere]",
              "top-1/2 -translate-y-1/2 text-base text-(--text-neutral-400)",
              "peer-focus:top-3.5 peer-focus:text-[0.6875rem]",
              "peer-[:not(:placeholder-shown)]:top-3.5 peer-[:not(:placeholder-shown)]:text-[0.6875rem]",
              "peer-autofill:top-3.5 peer-autofill:text-[0.6875rem]",
              (isLabelFloating || isFocused) && "top-3.5 text-[0.6875rem]"
            )}
          >
            {label} {required && <span className="text-red-500">*</span>}
          </span>
          {suffix && (
            <span className="text-base text-(--text-neutral-400)">
              {suffix}
            </span>
          )}
        </div>
        {isPassword && (
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              setShowPassword(!showPassword);
            }}
            aria-label={showPassword ? "Hide password" : "Show password"}
            className="absolute right-3 bottom-2.5 p-1 hover:bg-gray-100 rounded-full transition-colors cursor-pointer text-(--text-neutral-400) hover:text-(--neutral-950) z-10 shrink-0"
          >
            {showPassword ? <EyeOff className="size-4.5" /> : <Eye className="size-4.5" />}
          </button>
        )}
      </div>
      {hint && (
        <p className="text-(--text-neutral-600) text-xs px-1 mt-1.5">{hint}</p>
      )}
    </label>
  );
};

const CustomInput = <T extends FieldValues>({
  control,
  name,
  type,
  ...props
}: CustomInputProps<T>) => {
  const normalizeInputValue = (rawValue: string): string => {
    if (props.decimalOnly) {
      return rawValue.replace(/[^0-9.]/g, "").replace(/(\..*)\./g, "$1");
    }

    if (props.optionalPlusPhone) {
      return sanitizePracticePhoneInput(rawValue);
    }

    if (props.digitsOnly) {
      return rawValue.replace(/\D/g, "");
    }

    return rawValue;
  };

  if (control && name) {
    return (
      <FormField
        control={control}
        name={name}
        render={({ field, fieldState }) => (
          <FormItem>
            <FormControl>
              <CustomInputInner
                {...props}
                {...field}
                type={type}
                hasError={fieldState.invalid}
                onChange={(event) => {
                  const rawValue = event.target.value;
                  let normalizedValue = normalizeInputValue(rawValue);

                  if (normalizedValue !== rawValue) {
                    event.target.value = normalizedValue;
                  }

                  if (props.onChange) {
                    props.onChange(event);
                    normalizedValue = normalizeInputValue(event.target.value);
                  }

                  if (type === "number") {
                    field.onChange(
                      normalizedValue === "" ? "" : Number(normalizedValue),
                    );
                    return;
                  }

                  field.onChange(normalizedValue);
                }}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    );
  }

  return <CustomInputInner {...props} name={name} type={type} />;
};

export default CustomInput;
