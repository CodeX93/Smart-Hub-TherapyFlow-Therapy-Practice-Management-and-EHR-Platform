import { useRef, useState } from "react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import Calendar from "../appointment-sections/Calendar";
import { cn } from "@/lib/utils";
import { useCloseOnScroll } from "@/hooks/useCloseOnScroll";
import {
  DEFAULT_CALENDAR_PAST_MIN_YEAR,
  DEFAULT_FILTER_CALENDAR_MIN_YEAR,
  getDefaultCalendarMaxYear,
} from "@/utils/transformer/dates.transformer";
import type { Control, FieldValues, Path } from "react-hook-form";
import {
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";

interface CustomDatePickerProps<T extends FieldValues> {
  label: string;
  date?: Date | string | null;
  onDateChange?: (d: Date | null) => void;
  valueType?: "string" | "date";
  required?: boolean;
  control?: Control<T>;
  name?: Path<T>;
  className?: string;
  disabled?: boolean;
  disableFuture?: boolean;
  disablePast?: boolean;
  minYear?: number;
  maxYear?: number;
  minDate?: Date | string | null;
  maxDate?: Date | string | null;
  compact?: boolean;
}

const CustomDatePickerInner = <T extends FieldValues>({
  label,
  date,
  onDateChange,
  required = false,
  className,
  disabled = false,
  disableFuture,
  disablePast,
  minYear,
  maxYear,
  minDate,
  maxDate,
  compact = false,
}: CustomDatePickerProps<T>) => {
  const [open, setOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const parseInputDate = (value?: Date | string | null) =>
    value instanceof Date
      ? value
      : typeof value === "string" && value
        ? (() => {
            const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
            if (match) {
              const year = Number.parseInt(match[1], 10);
              const month = Number.parseInt(match[2], 10) - 1;
              const day = Number.parseInt(match[3], 10);
              return new Date(year, month, day);
            }
            return new Date(value);
          })()
        : null;
  const parsedDate = parseInputDate(date);
  const resolvedDate =
    parsedDate && !Number.isNaN(parsedDate.getTime()) ? parsedDate : null;
  const parsedMinDate = parseInputDate(minDate);
  const parsedMaxDate = parseInputDate(maxDate);
  const resolvedMinDate =
    parsedMinDate && !Number.isNaN(parsedMinDate.getTime()) ? parsedMinDate : null;
  const resolvedMaxDate =
    parsedMaxDate && !Number.isNaN(parsedMaxDate.getTime()) ? parsedMaxDate : null;
  const [currentMonth, setCurrentMonth] = useState<Date>(resolvedDate ?? new Date());

  const [previousDate, setPreviousDate] = useState(date);
  if (previousDate !== date) {
    setPreviousDate(date);
    const parsed = parseInputDate(date);
    if (parsed && !Number.isNaN(parsed.getTime())) setCurrentMonth(parsed);
  }

  const handleOpenChange = (nextOpen: boolean) => {
    if (disabled) return;
    if (nextOpen) {
      setCurrentMonth(resolvedDate ?? new Date());
    }
    setOpen(nextOpen);
  };

  useCloseOnScroll(open, () => setOpen(false), triggerRef);

  const handleDateSelect = (day: number) => {
    const newDate = new Date(
      currentMonth.getFullYear(),
      currentMonth.getMonth(),
      day,
    );
    onDateChange?.(newDate);
    setOpen(false);
  };

  const currentYear = new Date().getFullYear();
  const resolvedMinYear =
    minYear ?? (disableFuture ? DEFAULT_CALENDAR_PAST_MIN_YEAR : DEFAULT_FILTER_CALENDAR_MIN_YEAR);
  const resolvedMaxYear =
    maxYear ?? (disableFuture ? currentYear : getDefaultCalendarMaxYear());

  const formattedDate = resolvedDate
    ? resolvedDate.toLocaleDateString("en-US", {
        // weekday: "long",
        month: "short",
        day: "numeric",
        year: "numeric",
      })
    : "";

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <button
          ref={triggerRef}
          type="button"
          disabled={disabled}
          className={cn(
            compact
              ? "group relative flex h-12 w-full items-center rounded-xl border border-(--neutral-200) bg-white px-3 pr-10 text-left shadow-none transition-colors hover:border-(--neutral-600) focus-visible:outline-none focus-visible:ring-0 cursor-pointer"
              : "group w-full min-h-15 rounded-xl border border-(--neutral-100) shadow-(--shadow) pt-7 pb-2 cursor-pointer relative text-left bg-transparent items-start focus-visible:outline-none focus-visible:ring-0 transition-colors hover:border-(--neutral-600)",
            disabled &&
              "cursor-default opacity-60 hover:border-(--neutral-100)",
            className,
          )}
        >
          {compact ? (
            <span
              className={cn(
                "truncate text-sm",
                formattedDate
                  ? "text-(--text-primary-dark)"
                  : "text-(--text-neutral-400)",
              )}
            >
              {formattedDate || label}
              {required && !formattedDate ? (
                <span className="text-red-500"> *</span>
              ) : null}
            </span>
          ) : (
            <>
              <span
                className={cn(
                  "absolute left-3 transition-all duration-200 pointer-events-none",
                  date || open
                    ? "top-3.5 -translate-y-1/2 text-[0.6875rem] text-(--text-neutral-400)"
                    : "top-1/2 -translate-y-1/2 text-base text-(--text-neutral-400)",
                )}
              >
                {label} {required && <span className="text-red-500">*</span>}
              </span>
              <span className="block truncate text-base text-(--neutral-950) px-3">
                {formattedDate}
              </span>
            </>
          )}
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            <img src="/assets/calendar.png" alt="calendar" className="h-4 w-4" />
          </div>
        </button>
      </PopoverTrigger>

      <PopoverContent
        align="start"
        className="p-4 rounded-2xl bg-white shadow-xl border border-(--neutral-100) w-80 z-9999"
      >
        <Calendar
          currentMonth={currentMonth}
          setCurrentMonth={setCurrentMonth}
          selectedDate={
            resolvedDate &&
            resolvedDate.getMonth() === currentMonth.getMonth() &&
            resolvedDate.getFullYear() === currentMonth.getFullYear()
              ? resolvedDate.getDate()
              : 0
          }
          setSelectedDate={handleDateSelect}
          disableFuture={disableFuture}
          disablePast={disablePast}
          minYear={resolvedMinYear}
          maxYear={resolvedMaxYear}
          minDate={resolvedMinDate}
          maxDate={resolvedMaxDate}
          isDropdowns={false}
        />
      </PopoverContent>
    </Popover>
  );
};

const CustomDatePicker = <T extends FieldValues>(
  props: CustomDatePickerProps<T>,
) => {
  const { control, name } = props;

  if (control && name) {
    return (
      <FormField
        control={control}
        name={name}
        render={({ field }) => (
          <FormItem>
            <FormControl>
              <CustomDatePickerInner
                {...props}
                date={field.value}
                onDateChange={(selectedDate) => {
                  if (!selectedDate) {
                    field.onChange(props.valueType === "date" ? null : "");
                    return;
                  }
                  if (props.valueType === "date") {
                    field.onChange(selectedDate);
                    return;
                  }
                  const year = selectedDate.getFullYear();
                  const month = String(selectedDate.getMonth() + 1).padStart(2, "0");
                  const day = String(selectedDate.getDate()).padStart(2, "0");
                  field.onChange(`${year}-${month}-${day}`);
                }}
              />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    );
  }

  return <CustomDatePickerInner {...props} />;
};

export default CustomDatePicker;
