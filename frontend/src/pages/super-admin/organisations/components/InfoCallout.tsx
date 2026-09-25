import { cn } from "@/lib/utils";

const InfoCallout = ({
  className,
  children,
}: {
  className?: string;
  children: React.ReactNode;
}) => {
  return (
    <div
      className={cn(
        "w-full rounded-[0.75rem] border border-[#a7f3d0] bg-[#ecfdf5]",
        "px-4 py-3",
        className
      )}
    >
      {children}
    </div>
  );
};

export default InfoCallout;

