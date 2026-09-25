import { cn } from "@/lib/utils";

const FieldBlock = ({
  label,
  required,
  helper,
  error,
  children,
  className,
}: {
  label: string;
  required?: boolean;
  helper?: string;
  error?: string;
  children: React.ReactNode;
  className?: string;
}) => {
  return (
    <div className={cn("w-full", className)}>
      <div className="text-[#7c8a97] text-[0.6875rem] font-medium leading-4">
        {label}
        {required ? <span className="text-[#ef4444]"> *</span> : null}
        {helper ? <span className="font-normal"> {helper}</span> : null}
      </div>
      <div className="mt-2">{children}</div>
      {error ? (
        <div className="mt-1.5 text-[#ef4444] text-[0.6875rem] font-normal leading-4">
          {error}
        </div>
      ) : null}
    </div>
  );
};

export default FieldBlock;
