import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface DashboardCardProps {
  title: string;
  icon?: ReactNode;
  right?: ReactNode;
  children: ReactNode;
  className?: string;
  contentClassName?: string;
}

function DashboardCard(props: DashboardCardProps) {
  return (
    <section
      className={cn(
        "overflow-hidden rounded-xl border border-(--neutral-100) bg-white shadow-(--shadow)",
        props.className
      )}
    >
      <div className="flex items-center justify-between px-4 py-3.5">
        <div className="flex items-center gap-2">
          {props.icon}
          <h3 className="text-sm font-semibold text-(--text-primary-dark)">
            {props.title}
          </h3>
        </div>
        {props.right}
      </div>
      <div className={cn("px-4 pb-4", props.contentClassName)}>
        {props.children}
      </div>
    </section>
  );
}

export default DashboardCard;
