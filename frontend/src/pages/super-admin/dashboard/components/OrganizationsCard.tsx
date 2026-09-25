import { Building2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import OrganizationsStatusTable from "./OrganizationsStatusTable";
import TopTenantsList from "./TopTenantsList";
import type {
  SuperAdminOrgStatusItem,
  SuperAdminTopTenantItem,
} from "../dashboard.types";
import { cn } from "@/lib/utils";

interface OrganizationsCardProps {
  total: number;
  statuses: SuperAdminOrgStatusItem[];
  tenants: SuperAdminTopTenantItem[];
  className?: string;
}

function OrganizationsCard(props: OrganizationsCardProps) {
  const navigate = useNavigate();
  const statusTotal = props.statuses.reduce(function (sum, item) {
    return sum + item.count;
  }, 0);

  return (
    <section
      className={cn(
        "flex w-full flex-1 flex-col self-stretch rounded-[0.875rem] border border-[#e3ebf3] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]",
        props.className
      )}
    >
      <div className="flex w-full items-center justify-between px-4 py-3.5">
        <div className="flex items-center gap-2">
          <Building2 size={15} className="text-[#667483]" />
          <h3 className="text-[0.8125rem] font-semibold text-[#1e2934]">
            Organisations
          </h3>
        </div>

        <Button
          type="button"
          variant="tertiary"
          size="sm"
          onClick={() => navigate("/super-admin/organisations")}
        >
          View all organisations
        </Button>
      </div>

      <div className="w-full px-4 pb-4">
        <div className="flex items-end justify-between">
          <div className="text-[0.75rem] font-medium text-[#7c8a97]">
            Total Organisations
          </div>
          <div className="text-[1.5rem] font-semibold text-[#17212b]">
            {props.total}
          </div>
        </div>

        <div className="mt-3.5">
          <div className="flex h-1.5 w-full items-center gap-1 overflow-hidden">
            {props.statuses.map(function (status, index) {
              return (
                <span
                  key={status.label}
                  className={cn(
                    "h-full min-w-[0.375rem]",
                    status.dotClassName,
                    index === 0 ? "rounded-l-full" : "",
                    index === props.statuses.length - 1 ? "rounded-r-full" : ""
                  )}
                  style={{
                    flexGrow: statusTotal > 0 ? status.count : 1,
                    flexBasis: 0,
                  }}
                  aria-hidden="true"
                />
              );
            })}
          </div>
        </div>

        <OrganizationsStatusTable items={props.statuses} />
        <TopTenantsList tenants={props.tenants} />
      </div>
    </section>
  );
}

export default OrganizationsCard;
