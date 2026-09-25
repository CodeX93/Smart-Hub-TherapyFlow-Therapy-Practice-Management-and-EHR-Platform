import type { SuperAdminTopTenantItem } from "../dashboard.types";

interface TopTenantsListProps {
  tenants: SuperAdminTopTenantItem[];
}

function getInitials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map(function (part) {
      return part[0]?.toUpperCase() ?? "";
    })
    .join("");
}

function TopTenantsList(props: TopTenantsListProps) {
  return (
    <div className="mt-4 w-full">
      <div className="text-[0.6875rem] font-medium text-[#8a96a3]">
        Top tenants this month:
      </div>

      <div className="mt-3 space-y-3">
        {props.tenants.map(function (tenant) {
          return (
            <div
              key={tenant.name}
              className="flex items-center justify-between gap-3"
            >
              <div className="flex items-center gap-2.5">
                <div className="flex h-6.5 w-6.5 items-center justify-center rounded-[0.5rem] bg-[#f1f4f7] text-[0.625rem] font-semibold text-[#7d8b98]">
                  {getInitials(tenant.name)}
                </div>
                <div className="text-[0.75rem] font-medium text-[#2c3947]">
                  {tenant.name}
                </div>
              </div>

              <div className="flex items-center gap-2 text-[0.6875rem] text-[#8a96a3]">
                <span>{tenant.planLabel}</span>
                <span className="opacity-40">&bull;</span>
                <span>{tenant.usersLabel}</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default TopTenantsList;
