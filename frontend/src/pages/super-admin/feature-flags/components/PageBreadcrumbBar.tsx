import ProfileDropdown from "@/components/shared/ProfileDropdown";

interface Crumb {
  label: string;
  isActive?: boolean;
}

interface PageBreadcrumbBarProps {
  crumbs: Crumb[];
  avatarInitials: string;
  avatarFullName?: string;
}

const PageBreadcrumbBar = ({
  crumbs,
  avatarInitials,
  avatarFullName = "Super Admin",
}: PageBreadcrumbBarProps) => {
  return (
    <div className="w-full h-14 flex items-center justify-between px-6 bg-(--surface-white) border-b border-(--neutral-100)">
      <div className="text-sm font-medium text-(--text-neutral-600) flex items-center gap-2">
        {crumbs.map((c, idx) => (
          <span key={`${c.label}-${idx}`} className="flex items-center gap-2">
            <span
              className={
                c.isActive
                  ? "text-(--text-primary-dark) font-semibold"
                  : "text-(--text-neutral-600)"
              }
            >
              {c.label}
            </span>
            {idx < crumbs.length - 1 && (
              <span className="text-(--text-neutral-200)">{">"}</span>
            )}
          </span>
        ))}
      </div>

      <ProfileDropdown
        initials={avatarInitials}
        fullName={avatarFullName}
        onLogout={() => {
          window.location.href = "/super-admin/login";
        }}
      />
    </div>
  );
};

export default PageBreadcrumbBar;
