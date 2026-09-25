import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { SearchIcon } from "../../dashboard/dashboard.utils";
import ProfileDropdown from "@/components/shared/ProfileDropdown";

const SuperAdminCrumbHeader = ({
  crumbs,
  userInitials,
  userFullName = "Super Admin",
  searchPlaceholder = "Search tenants, users, or settings...",
  rightMode = "search",
  supportText = "Support",
}: {
  crumbs: Array<{ label: string; isActive?: boolean }>;
  userInitials: string;
  userFullName?: string;
  searchPlaceholder?: string;
  rightMode?: "search" | "support";
  supportText?: string;
}) => {
  return (
    <div className="flex items-start justify-between gap-4">
      <div className="text-sm text-(--text-neutral-600) font-medium flex items-center gap-1.5">
        {crumbs.map((c, idx) => (
          <span key={`${c.label}-${idx}`} className="flex items-center gap-1.5">
            <span
              className={
                c.isActive
                  ? "text-(--text-primary-dark)"
                  : "opacity-80 text-(--text-neutral-600)"
              }
            >
              {c.label}
            </span>
            {idx < crumbs.length - 1 && (
              <span className="opacity-60">/</span>
            )}
          </span>
        ))}
      </div>

      <div className="flex items-center gap-3">
        {rightMode === "search" ? (
          <div className="relative w-90 max-w-[55vw]">
            <div className="absolute left-3 top-1/2 -translate-y-1/2">
              <SearchIcon />
            </div>
            <Input
              placeholder={searchPlaceholder}
              className={cn(
                "h-10 rounded-full pl-9 pr-4 bg-white border-(--neutral-100) shadow-xs",
                "placeholder:text-(--text-neutral-400) text-(--text-primary-dark)"
              )}
            />
          </div>
        ) : (
          <div className="text-(--text-neutral-600) text-sm font-medium leading-5">
            {supportText}
          </div>
        )}

        <ProfileDropdown
          initials={userInitials}
          fullName={userFullName}
          onLogout={() => {
            window.location.href = "/super-admin/login";
          }}
        />
      </div>
    </div>
  );
};

export default SuperAdminCrumbHeader;
