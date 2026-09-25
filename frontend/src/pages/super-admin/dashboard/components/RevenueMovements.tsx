import { ArrowDownRight, ArrowUpRight } from "lucide-react";

interface RevenueMovementsProps {
  upgradesText: string;
  downgradesText: string;
}

function RevenueMovements(props: RevenueMovementsProps) {
  return (
    <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
      <div className="flex min-h-[3.125rem] items-center gap-3 rounded-[0.75rem] border border-[#c6efdd] bg-[#ecfbf3] px-4 py-3">
        <div className="flex h-6.5 w-6.5 shrink-0 items-center justify-center rounded-full bg-[#baf2d4]">
          <ArrowUpRight size={14} className="text-(--success-green)" />
        </div>
        <div>
          <div className="text-[0.625rem] font-medium text-[#8a96a3]">
            Upgrades
          </div>
          <div className="text-[0.75rem] font-semibold text-(--success-green)">
            {props.upgradesText}
          </div>
        </div>
      </div>

      <div className="flex min-h-[3.125rem] items-center gap-3 rounded-[0.75rem] border border-[#f4c9c9] bg-[#fff1f1] px-4 py-3">
        <div className="flex h-6.5 w-6.5 shrink-0 items-center justify-center rounded-full bg-[#ffd6d6]">
          <ArrowDownRight size={14} className="text-(--dark-red)" />
        </div>
        <div>
          <div className="text-[0.625rem] font-medium text-[#8a96a3]">
            Downgrades
          </div>
          <div className="text-[0.75rem] font-semibold text-(--dark-red)">
            {props.downgradesText}
          </div>
        </div>
      </div>
    </div>
  );
}

export default RevenueMovements;
