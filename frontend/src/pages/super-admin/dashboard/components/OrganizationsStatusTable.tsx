import type { SuperAdminOrgStatusItem } from "../dashboard.types";
import { cn } from "@/lib/utils";

interface OrganizationsStatusTableProps {
  items: SuperAdminOrgStatusItem[];
}

function OrganizationsStatusTable(props: OrganizationsStatusTableProps) {
  return (
    <div className="mt-4 w-full">
      <div className="flex items-center justify-between border-b border-[#e9eef3] pb-2 text-[0.6875rem] font-medium text-[#8a96a3]">
        <span>Status</span>
        <span>Count</span>
      </div>

      <div className="divide-y divide-[#edf1f5]">
        {props.items.map(function (row) {
          return (
            <div
              key={row.label}
              className="flex items-center justify-between bg-white py-3"
            >
              <div className="flex items-center gap-2">
                <span
                  className={cn("h-2 w-2 rounded-full", row.dotClassName)}
                  aria-hidden="true"
                />
                <span className="text-[0.75rem] font-medium text-[#2c3947]">
                  {row.label}
                </span>
              </div>
              <span className="text-[0.75rem] font-medium text-[#2c3947]">
                {row.count}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default OrganizationsStatusTable;
