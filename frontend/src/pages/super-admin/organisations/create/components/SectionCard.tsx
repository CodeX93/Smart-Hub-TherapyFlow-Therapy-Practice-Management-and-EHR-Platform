import GlassCard from "../../components/GlassCard";
import { cn } from "@/lib/utils";

const SectionCard = ({
  title,
  subtitle,
  children,
  className,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  className?: string;
}) => {
  return (
    <GlassCard
      className={cn(
        "flex h-full w-full flex-col rounded-[1rem] border border-[#e3ebf3] bg-white px-4 py-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)] md:px-[1.125rem] md:py-[1.125rem]",
        className
      )}
    >
      <div className="text-[#1f2d38] text-[0.875rem] font-semibold leading-6">
        {title}
      </div>
      {subtitle ? (
        <div className="mt-1 text-[#a0acb8] text-[0.6875rem] font-normal leading-4.5">
          {subtitle}
        </div>
      ) : null}
      <div className="mt-4 flex-1">{children}</div>
    </GlassCard>
  );
};

export default SectionCard;

