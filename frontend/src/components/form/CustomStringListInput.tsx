import { useState } from "react";
import { Plus, X } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Control, FieldValues, Path } from "react-hook-form";
import {
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";

interface CustomStringListInputProps<T extends FieldValues> {
  label: string;
  placeholder?: string;
  hint?: string;
  maxItems?: number;
  maxItemLength?: number;
  control?: Control<T>;
  name?: Path<T>;
  value?: string[];
  onChange?: (value: string[]) => void;
  className?: string;
}

const CustomStringListInputInner = <T extends FieldValues>({
  label,
  placeholder = "Type and press Add",
  hint,
  maxItems = 20,
  maxItemLength = 50,
  value = [],
  onChange,
  className,
}: CustomStringListInputProps<T>) => {
  const [draft, setDraft] = useState("");

  const addItem = () => {
    const nextValue = draft.trim();
    if (!nextValue || !onChange) return;
    if (value.some((item) => item.toLowerCase() === nextValue.toLowerCase())) {
      setDraft("");
      return;
    }
    if (value.length >= maxItems) return;
    onChange([...value, nextValue]);
    setDraft("");
  };

  return (
    <div className={cn("space-y-2", className)}>
      <label className="text-sm font-medium text-(--text-primary-dark)">
        {label}
      </label>
      <div className="flex items-center gap-2">
        <input
          type="text"
          value={draft}
          maxLength={maxItemLength}
          placeholder={placeholder}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              addItem();
            }
          }}
          className="flex-1 h-11 rounded-xl border border-(--neutral-100) px-3 text-sm text-(--neutral-950) outline-none focus:border-(--neutral-600)"
        />
        <button
          type="button"
          onClick={addItem}
          disabled={!draft.trim() || value.length >= maxItems}
          className="inline-flex items-center gap-1 h-11 px-4 rounded-full border border-(--neutral-100) text-sm font-medium text-(--text-primary-500) hover:bg-(--neutral-50) disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          <Plus size={16} />
          Add
        </button>
      </div>
      {value.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {value.map((item) => (
            <span
              key={item}
              className="inline-flex items-center gap-1 rounded-full bg-(--bg-primary-50) px-3 py-1 text-sm text-(--text-primary-dark)"
            >
              {item}
              <button
                type="button"
                onClick={() => onChange?.(value.filter((entry) => entry !== item))}
                className="text-(--text-neutral-500) hover:text-(--text-primary-dark) cursor-pointer"
                aria-label={`Remove ${item}`}
              >
                <X size={14} />
              </button>
            </span>
          ))}
        </div>
      ) : null}
      {hint ? (
        <p className="text-xs text-(--text-neutral-600) px-1">{hint}</p>
      ) : null}
    </div>
  );
};

const CustomStringListInput = <T extends FieldValues>({
  control,
  name,
  ...props
}: CustomStringListInputProps<T>) => {
  if (control && name) {
    return (
      <FormField
        control={control}
        name={name}
        render={({ field }) => (
          <FormItem>
            <FormControl>
              <CustomStringListInputInner
                {...props}
                value={Array.isArray(field.value) ? field.value : []}
                onChange={field.onChange}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    );
  }

  return (
    <CustomStringListInputInner
      {...props}
      value={props.value ?? []}
      onChange={props.onChange}
    />
  );
};

export default CustomStringListInput;
