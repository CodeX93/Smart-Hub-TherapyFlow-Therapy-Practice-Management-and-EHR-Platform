import type { ReactNode } from "react";

interface InfoRowProps {
  label: string;
  value: ReactNode;
}

function InfoRow(props: InfoRowProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <div className="text-(--text-neutral-400) text-[0.6875rem] font-medium leading-4 uppercase tracking-[0.025rem]">
        {props.label}
      </div>
      <div className="text-(--text-neutral-600) text-sm font-normal leading-5.5">
        {props.value}
      </div>
    </div>
  );
}

export default InfoRow;
