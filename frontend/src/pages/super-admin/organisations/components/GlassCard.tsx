import { cn } from "@/lib/utils";

const GlassCard = ({
  className,
  ...props
}: React.ComponentProps<"div">) => {
  return (
    <div
      className={cn(
        "rounded-xl border-[0.040813rem] border-(--neutral-100) bg-(--surface-white)",
        "shadow-[0px_2px_2px_0px_var(--shadow)]",
        className
      )}
      {...props}
    />
  );
};

export default GlassCard;
