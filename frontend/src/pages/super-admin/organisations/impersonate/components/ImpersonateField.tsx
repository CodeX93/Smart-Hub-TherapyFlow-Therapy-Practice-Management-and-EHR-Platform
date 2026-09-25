import { cn } from "@/lib/utils";

interface ImpersonateFieldProps {
  label: string;
  helper: string;
  children: React.ReactNode;
  className?: string;
}

function ImpersonateField(props: ImpersonateFieldProps) {
  return (
    <div className={cn("w-full", props.className)}>
      <div className="rounded-[1rem] border border-[#dce5ee] bg-white px-4 py-3 shadow-none">
        <div className="text-[0.75rem] font-medium leading-4 text-[#7c8a97]">
          {props.label}
        </div>
        <div className="mt-1">{props.children}</div>
      </div>
      <div className="mt-2 text-[0.6875rem] font-normal leading-4 text-[#a0acb8]">
        {props.helper}
      </div>
    </div>
  );
}

export default ImpersonateField;
