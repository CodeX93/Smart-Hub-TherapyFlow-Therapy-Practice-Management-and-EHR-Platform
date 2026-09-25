import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export const StatusBadge = ({ status }: { status: string }) => {
  const normalizedStatus = status.toLowerCase();

  const getStyles = () => {
    if (normalizedStatus === "granted" || normalizedStatus === "enabled") {
      return "bg-(--bg-success-light) text-(--dark-green) border-none";
    }
    if (normalizedStatus === "denied") {
      return "bg-(--light-red) text-(--dark-red) border-none";
    }
    // "Disabled", "Not Set", "Not set"
    return "bg-transparent text-(--text-primary-dark) border-(--neutral-100)";
  };

  return (
    <Badge
      variant="outline"
      className={cn(
        "w-fit h-6 px-3 text-[0.75rem] font-medium rounded-full shadow-none",
        getStyles(),
      )}
    >
      {status}
    </Badge>
  );
};
