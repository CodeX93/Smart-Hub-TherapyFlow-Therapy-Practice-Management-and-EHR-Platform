import { cn } from "@/lib/utils";

interface MetricCardProps {
  label: string;
  value: string;
  subtext?: string;
  icon?: React.ReactNode;
  className?: string;
}

function MetricCard(props: MetricCardProps) {
  return (
    <div
      className={cn(
        "w-full rounded-[0.875rem] border border-[#e6edf3] bg-white",
        "min-h-[5.25rem] px-4 py-3 shadow-[0_2px_2px_0_var(--shadow)]",
        props.className
      )}
    >
      <div className="flex items-center justify-between gap-4">
        <div className="min-w-0 flex-1">
          <div className="text-[0.75rem] font-medium leading-4 text-[#7c8a97]">
            {props.label}
          </div>
          <div className="mt-2 flex flex-wrap items-end gap-1.5">
            <div className="text-[1rem] font-semibold leading-6 text-[#1f2d38]">
              {props.value}
            </div>
            {props.subtext ? (
              <div className="pb-0.5 text-[0.6875rem] font-medium leading-4 text-[#a0acb8]">
                {props.subtext}
              </div>
            ) : null}
          </div>
        </div>

        {props.icon ? (
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[0.75rem] bg-[#f5f8fb] text-[#8ea0ae]">
            {props.icon}
          </div>
        ) : null}
      </div>
    </div>
  );
}

export default MetricCard;
