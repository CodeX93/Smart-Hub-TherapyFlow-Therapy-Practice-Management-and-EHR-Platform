import React, { useState } from "react";
import { cn } from "@/lib/utils";
import type { Control, FieldValues, Path } from "react-hook-form";
import { useLocation } from "react-router-dom";
import {
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";

interface CustomTextareaProps<
  T extends FieldValues,
> extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLTextAreaElement>) => void;
  required?: boolean;
  hint?: string;
  control?: Control<T>;
  name?: Path<T>;
  textareaClassName?: string;
  characterLimit?: number;
  hasError?: boolean;
}

const CustomTextareaInner = <T extends FieldValues>({
  label,
  value = "",
  onChange,
  required = false,
  hint,
  className,
  textareaClassName,
  characterLimit,
  hasError = false,
  onFocus,
  onBlur,
  ...props
}: CustomTextareaProps<T>) => {
  const [isFocused, setIsFocused] = useState(false);
  const textareaRef = React.useRef<HTMLTextAreaElement>(null);
  const generatedId = React.useId();
  const fieldId = props.id ?? generatedId;
  const helpId = hint || characterLimit ? `${fieldId}-help` : undefined;
  const location = useLocation();
  const isSuperAdminRoute = location.pathname.startsWith("/super-admin");

  const handleFocus = (e: React.FocusEvent<HTMLTextAreaElement>) => {
    setIsFocused(true);
    if (onFocus) onFocus(e);
  };

  const handleBlur = (e: React.FocusEvent<HTMLTextAreaElement>) => {
    setIsFocused(false);
    if (onBlur) onBlur(e);
  };

  const safeValue = value ?? "";
  const isFloating = isFocused || safeValue.length > 0;

  return (
    <div className="flex flex-col gap-1.5 w-full">
      <div
        className="relative group w-full"
        onClick={() => textareaRef.current?.focus()}
      >
        <div
          className={cn(
            "w-full min-h-32 rounded-xl border border-(--neutral-100) shadow-(--shadow) pt-7 pb-2 px-3 transition-colors hover:border-(--neutral-600) bg-white cursor-text",
            className,
            isFocused && "border-(--neutral-600)",
            hasError && "border-red-500 hover:border-red-500",
            props.disabled && "cursor-not-allowed bg-(--neutral-50) hover:border-(--neutral-100)",
          )}
        >
          <textarea
            ref={textareaRef}
            {...props}
            id={fieldId}
            value={safeValue}
            onChange={(event) => {
              if (isSuperAdminRoute) {
                const startsWithSpace = /^\s/.test(event.target.value);
                event.target.setCustomValidity(
                  startsWithSpace ? "Value cannot start with a space" : "",
                );
              }
              onChange?.(event);
            }}
            onFocus={handleFocus}
            aria-invalid={hasError}
            aria-describedby={helpId}
            onBlur={(event) => {
              if (isSuperAdminRoute) {
                const startsWithSpace = /^\s/.test(event.target.value);
                event.target.setCustomValidity(
                  startsWithSpace ? "Value cannot start with a space" : "",
                );
                event.target.reportValidity();
              }
              handleBlur(event);
            }}
            placeholder={props.placeholder || " "}
            className={cn(
              "w-full bg-transparent border-none outline-none text-(--neutral-950) text-base placeholder:text-(--text-neutral-400) resize-none min-h-25 p-0.5 focus-visible:ring-0 peer",
              props.disabled && "cursor-not-allowed text-(--text-neutral-600)",
              textareaClassName,
            )}
          />
          <label
            htmlFor={fieldId}
            className={cn(
              "absolute left-3 right-3 transition-all duration-200 pointer-events-none whitespace-normal break-words [overflow-wrap:anywhere]",
              "top-6 -translate-y-1/2 text-base text-(--text-neutral-400)",
              "peer-focus:top-3.5 peer-focus:text-[0.6875rem]",
              "peer-[:not(:placeholder-shown)]:top-3.5 peer-[:not(:placeholder-shown)]:text-[0.6875rem]",
              "peer-autofill:top-3.5 peer-autofill:text-[0.6875rem]",
              (isFloating || isFocused) && "top-3.5 text-[0.6875rem]"
            )}
          >
            {label} {required && <span aria-hidden="true" className="text-red-500">*</span>}
          </label>
        </div>
      </div>
      {(hint || characterLimit) && (
        <div id={helpId} className="flex items-start justify-between gap-3 px-1 text-xs">
          {hint ? <p className="text-(--text-neutral-600)">{hint}</p> : <span />}
          {characterLimit ? (
            <p
              className={cn(
                "shrink-0 text-(--text-neutral-600)",
                safeValue.length > characterLimit && "font-medium text-red-500",
              )}
            >
              {safeValue.length}/{characterLimit}
            </p>
          ) : null}
        </div>
      )}
    </div>
  );
};

const CustomTextarea = <T extends FieldValues>({
  control,
  name,
  ...props
}: CustomTextareaProps<T>) => {
  if (control && name) {
    return (
      <FormField
        control={control}
        name={name}
        render={({ field, fieldState }) => (
          <FormItem>
            <FormControl>
              <CustomTextareaInner
                {...props}
                {...field}
                hasError={fieldState.invalid}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    );
  }

  return <CustomTextareaInner {...props} name={name} />;
};

export default CustomTextarea;
