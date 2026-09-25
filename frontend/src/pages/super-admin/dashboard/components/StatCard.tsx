import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface StatCardProps {
  label: string;
  value: string;
  icon: ReactNode;
  subtext?: string;
  className?: string;
}

function StatCard(props: StatCardProps) {
  return (
    <div
      className={cn(
        "flex min-h-[4.5rem] flex-col justify-between rounded-[0.75rem] border border-[#e3ebf3]",
        "bg-white px-4 py-3 shadow-[0_1px_2px_rgba(15,23,42,0.04)]",
        props.className
      )}
    >
      <div className="flex items-center justify-between gap-3">
        <div>
          <div className="text-[#7c8a97] text-[0.75rem] font-medium leading-4">
            {props.label}
          </div>
          <div className="mt-2 flex items-end gap-1.5">
            <div className="text-[#1f2d38] text-[1rem] font-semibold leading-6">
              {props.value}
            </div>
            {props.subtext ? (
              <div className="pb-0.5 text-[#a0acb8] text-[0.6875rem] font-medium leading-4">
                {props.subtext}
              </div>
            ) : null}
          </div>
        </div>
        <div className="shrink-0">{props.icon}</div>
      </div>
    </div>
  );
}

export default StatCard;
