import SemanticStatusBadge from "@/components/shared/SemanticStatusBadge";
import { cn } from "@/lib/utils";

type StatusBadgeVariant = "green" | "yellow" | "gray";

interface StatusBadgeProps {
  children: React.ReactNode;
  variant: StatusBadgeVariant;
  className?: string;
}

function getVariantStatus(variant: StatusBadgeVariant): string {
  if (variant === "green") return "active";
  if (variant === "yellow") return "pending";
  return "inactive";
}

function StatusBadge(props: StatusBadgeProps) {
  return (
    <SemanticStatusBadge
      status={getVariantStatus(props.variant)}
      className={cn(
        "inline-flex h-5 w-fit items-center justify-center rounded-full border border-(--badge-border) px-2.5",
        "text-[0.6875rem] font-medium leading-4",
        props.className
      )}
    >
      {props.children}
    </SemanticStatusBadge>
  );
}

export default StatusBadge;
