import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { isPlanLimitFeature } from "../planEntitlement.utils";

export interface PlanFeatureRow {
  key: string;
  name: string;
  description: string;
  type: string;
  enabled: boolean;
  usageLimit: number | null;
}

interface EntitlementCardProps {
  items: PlanFeatureRow[];
  onChange: (
    key: string,
    field: "enabled" | "usageLimit",
    value: boolean | number | null,
  ) => void;
  limitErrors?: Record<string, string>;
}

function EntitlementRow({
  row,
  onChange,
  limitError,
  className,
}: {
  row: PlanFeatureRow;
  onChange: (
    key: string,
    field: "enabled" | "usageLimit",
    value: boolean | number | null,
  ) => void;
  limitError?: string;
  className?: string;
}) {
  const isLimitFeature = isPlanLimitFeature(row.key, row.type);
  const showUsageLimit = row.enabled && isLimitFeature;

  return (
    <div
      className={cn(
        "flex min-h-[4.125rem] flex-col justify-center gap-4 border-t border-[#edf2f7] px-4 py-3 sm:flex-row sm:items-start sm:justify-between",
        className,
      )}
    >
      <div className="min-w-0 flex-1 overflow-hidden pr-2">
        <div
          className="break-words text-[0.8125rem] font-semibold leading-5 text-[#1f2d38] [overflow-wrap:anywhere]"
          title={row.name}
        >
          {row.name}
        </div>
        <div
          className="mt-0.5 break-all text-[0.6875rem] font-medium leading-4 text-[#8a96a3]"
          title={row.key}
        >
          {row.key}
        </div>
        {row.description ? (
          <div
            className="mt-1 line-clamp-3 break-words text-[0.75rem] leading-4 text-[#667483] [overflow-wrap:anywhere]"
            title={row.description}
          >
            {row.description}
          </div>
        ) : null}
      </div>

      <div className="flex w-full shrink-0 flex-col items-stretch gap-2 sm:w-auto sm:items-end">
        <div className="flex flex-wrap items-center justify-start gap-4 sm:justify-end sm:gap-6">
          {showUsageLimit ? (
            <div className="flex items-center gap-3">
              <span className="whitespace-nowrap text-[0.6875rem] font-semibold leading-4 text-[#1f2d38]">
                Usage Limit:
              </span>
              <Input
                value={row.usageLimit ?? ""}
                onChange={(event) => {
                  const rawValue = event.target.value.trim();
                  if (rawValue === "") {
                    onChange(row.key, "usageLimit", null);
                    return;
                  }
                  const parsedValue = Number.parseInt(rawValue, 10);
                  onChange(
                    row.key,
                    "usageLimit",
                    Number.isNaN(parsedValue) ? null : parsedValue,
                  );
                }}
                type="number"
                min={0}
                placeholder="0"
                aria-invalid={Boolean(limitError)}
                className={cn(
                  "h-[2.125rem] w-[6rem] rounded-full border-[#e3ebf3] bg-white px-3.5 shadow-none",
                  "text-[0.75rem] text-[#1f2d38] placeholder:text-[#8a96a3]",
                  "focus-visible:border-[#dce5ee] focus-visible:ring-0",
                  limitError && "border-[#f3d4d4]",
                )}
              />
            </div>
          ) : null}

          <div className="flex items-center gap-3">
            <span className="whitespace-nowrap text-[0.6875rem] font-semibold leading-4 text-[#1f2d38]">
              On plan:
            </span>
            <Switch
              checked={row.enabled}
              onCheckedChange={(value) => onChange(row.key, "enabled", value)}
              onClassName="bg-[#10b981]"
              offClassName="bg-[#d7e0e8]"
            />
          </div>
        </div>
        {limitError ? (
          <p className="text-right text-[0.6875rem] text-[#b42318]">{limitError}</p>
        ) : null}
      </div>
    </div>
  );
}

export default function EntitlementCard({
  items,
  onChange,
  limitErrors = {},
}: EntitlementCardProps) {
  if (!items || items.length === 0) {
    return (
      <div className="w-full rounded-[0.75rem] border border-[#e3ebf3] bg-white p-6 text-center text-[0.8125rem] text-[#8a96a3]">
        No features found in catalog.
      </div>
    );
  }

  return (
    <div className="w-full overflow-hidden rounded-[0.75rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
      {items.map((row, index) => (
        <EntitlementRow
          key={row.key}
          row={row}
          onChange={onChange}
          limitError={limitErrors[row.key]}
          className={index === 0 ? "border-t-0" : undefined}
        />
      ))}
    </div>
  );
}
