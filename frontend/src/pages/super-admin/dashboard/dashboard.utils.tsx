import type { ReactNode } from "react";
import {
  Activity,
  Building2,
  DollarSign,
  Search,
  TrendingDown,
  Users,
} from "lucide-react";
import type { SuperAdminStatIconKey } from "./dashboard.types";

interface SuperAdminStatIconProps {
  iconKey: SuperAdminStatIconKey;
}

function StatIconFrame(props: { children: ReactNode }) {
  return (
    <div className="flex h-10 w-10 items-center justify-center rounded-[0.75rem] bg-[#f4f7fa] text-[#607080]">
      {props.children}
    </div>
  );
}

export function SuperAdminStatIcon(props: SuperAdminStatIconProps) {
  const iconClassName = "h-[1.125rem] w-[1.125rem]";

  switch (props.iconKey) {
    case "active-tenants":
      return (
        <StatIconFrame>
          <Building2 className={iconClassName} aria-hidden="true" />
        </StatIconFrame>
      );
    case "total-end-users":
      return (
        <StatIconFrame>
          <Users className={iconClassName} aria-hidden="true" />
        </StatIconFrame>
      );
    case "mrr":
      return (
        <StatIconFrame>
          <DollarSign className={iconClassName} aria-hidden="true" />
        </StatIconFrame>
      );
    case "churn-month":
      return (
        <StatIconFrame>
          <TrendingDown className={iconClassName} aria-hidden="true" />
        </StatIconFrame>
      );
    case "platform-uptime":
      return (
        <StatIconFrame>
          <Activity className={iconClassName} aria-hidden="true" />
        </StatIconFrame>
      );
    default:
      return null;
  }
}

export function SearchIcon() {
  return (
    <Search className="size-4 text-(--text-neutral-400)" aria-hidden="true" />
  );
}
