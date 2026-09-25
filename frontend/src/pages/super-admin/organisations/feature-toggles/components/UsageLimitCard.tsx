import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import {
  FEATURE_OVERRIDE_LIMITS,
  FEATURE_USAGE_LIMIT_MAX_DIGITS,
  sanitizeUsageLimitOverride,
} from "../featureOverrides.utils";

interface UsageLimitRow {
  title: string;
  keyName: string;
  planDefault: string;
  overrideValue: string;
}

interface UsageRowProps {
  row: UsageLimitRow;
  onChange(next: string): void;
  className?: string;
}

interface UsageLimitCardProps {
  rows: UsageLimitRow[];
  onChange(keyName: string, next: string): void;
}

function UsageRow(props: UsageRowProps) {
  return (
    <div
      className={cn(
        "grid grid-cols-1 items-center gap-3 border-t border-[#edf2f7] px-4 py-3 sm:grid-cols-[minmax(0,1fr)_auto]",
        props.className
      )}
    >
      <div className="min-w-0">
        <div className="truncate text-[#1f2d38] text-[0.8125rem] font-semibold leading-5">
          {props.row.title}
        </div>
        <div className="mt-0.5 truncate text-[#a0acb8] text-[0.625rem] font-medium leading-4 uppercase tracking-[0.015rem]">
          {props.row.keyName}
        </div>
      </div>

      <div className="flex min-w-0 flex-wrap items-center gap-4 sm:justify-end sm:gap-6">
        <div className="whitespace-nowrap text-[#8a96a3] text-[0.6875rem] font-normal leading-4">
          <span className="mr-2">Plan Default:</span>
          <span className="text-[#1f2d38] font-semibold">
            {props.row.planDefault}
          </span>
        </div>

        <div className="flex shrink-0 items-center gap-3 whitespace-nowrap">
          <span className="text-[#1f2d38] text-[0.6875rem] font-semibold leading-4">
            Override:
          </span>
          <Input
            value={props.row.overrideValue}
            inputMode="numeric"
            placeholder={`0–${FEATURE_OVERRIDE_LIMITS.usageLimitMax.toLocaleString()}`}
            maxLength={FEATURE_USAGE_LIMIT_MAX_DIGITS}
            onChange={function (event) {
              props.onChange(sanitizeUsageLimitOverride(event.target.value));
            }}
            className={cn(
              "h-[2.125rem] w-[6.5rem] rounded-full border-[#e3ebf3] bg-white px-3.5 shadow-none",
              "text-[#1f2d38] text-[0.6875rem] placeholder:text-[#8a96a3]",
              "focus-visible:border-[#dce5ee] focus-visible:ring-0"
            )}
          />
        </div>
      </div>
    </div>
  );
}

function UsageLimitCard(props: UsageLimitCardProps) {
  return (
    <div className="w-full overflow-hidden rounded-[0.75rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
      {props.rows.map(function (row, index) {
        return (
          <UsageRow
            key={row.keyName}
            row={row}
            onChange={function (next) {
              props.onChange(row.keyName, next);
            }}
            className={index === 0 ? "border-t-0" : undefined}
          />
        );
      })}
    </div>
  );
}

export type { UsageLimitRow };
export default UsageLimitCard;
