import { Switch } from "@/components/ui/switch";
import { cn } from "@/lib/utils";

interface ModuleToggleRow {
  title: string;
  keyName: string;
  planDefault: string;
  overrideEnabled: boolean;
}

interface ModuleAccessRowProps {
  row: ModuleToggleRow;
  onToggleOverride(next: boolean): void;
  className?: string;
}

interface ModuleAccessCardProps {
  rows: ModuleToggleRow[];
  onChange(keyName: string, next: boolean): void;
}

function ModuleAccessRow(props: ModuleAccessRowProps) {
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
          <Switch
            checked={props.row.overrideEnabled}
            onCheckedChange={props.onToggleOverride}
            onClassName="bg-[#435564]"
            offClassName="bg-[#d7e0e8]"
          />
        </div>
      </div>
    </div>
  );
}

function ModuleAccessCard(props: ModuleAccessCardProps) {
  return (
    <div className="w-full overflow-hidden rounded-[0.75rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
      {props.rows.map(function (row, index) {
        return (
          <ModuleAccessRow
            key={row.keyName}
            row={row}
            onToggleOverride={function (next) {
              props.onChange(row.keyName, next);
            }}
            className={index === 0 ? "border-t-0" : undefined}
          />
        );
      })}
    </div>
  );
}

export type { ModuleToggleRow };
export default ModuleAccessCard;
