import { Button } from "@/components/ui/button";
import { buttonVariants } from "@/components/ui/button-variants";
import { cn } from "@/lib/utils";
import type { ResolveTenantOrganisation } from "@/store/api/authApi";

type OrgPickerModalProps = {
  open: boolean;
  organisations: ResolveTenantOrganisation[];
  onSelect: (organisation: ResolveTenantOrganisation) => void;
  onClose: () => void;
  title?: string;
  description?: string;
  isLoading?: boolean;
  /** Email / identifier used for login-context lookup — used to decide when to surface org username. */
  lookupIdentifier?: string;
};

function formatRoleLabel(role: string): string {
  return role
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

function formatOrgRoleHint(
  roles: string[] | undefined,
  orgName: string,
): string | undefined {
  if (!roles?.length) {
    return undefined;
  }

  const primaryRole = formatRoleLabel(roles[0]);
  return `${primaryRole} at ${orgName}`;
}

export default function OrgPickerModal({
  open,
  organisations,
  onSelect,
  onClose,
  title = "Select Organization",
  description = "Choose the tenant you want to access.",
  isLoading = false,
  lookupIdentifier,
}: OrgPickerModalProps) {
  if (!open) {
    return null;
  }

  const normalizedLookup = lookupIdentifier?.trim().toLowerCase();

  return (
    <div className="fixed inset-0 z-50">
      <button
        type="button"
        className="absolute inset-0 bg-black/30"
        aria-label="Close organization selector"
        onClick={onClose}
      />
      <div className="absolute left-1/2 top-1/2 w-full max-w-[32.5rem] -translate-x-1/2 -translate-y-1/2 rounded-[1rem] border border-[#e3ebf3] bg-white p-6 shadow-[0_20px_40px_rgba(15,23,42,0.2)]">
        <div className="text-[1.125rem] font-semibold text-[#1f2d38]">{title}</div>
        <p className="mt-1 text-[0.8125rem] text-[#6b7a88]">{description}</p>

        <div className="mt-4 flex flex-col gap-3">
          {organisations.length === 0 ? (
            <div className="text-sm text-[#8a96a3]">
              No organizations found for this account.
            </div>
          ) : null}

          {organisations.map((org) => {
            const roleHint = formatOrgRoleHint(org.roles, org.name);
            const orgUsername = org.username?.trim();
            const showUsername =
              Boolean(orgUsername) &&
              (!normalizedLookup ||
                orgUsername!.toLowerCase() !== normalizedLookup);

            return (
                <div
                  key={`${org.organisationId}-${org.slug}`}
                  role="button"
                  tabIndex={isLoading ? -1 : 0}
                  aria-disabled={isLoading}
                  onClick={() => {
                    if (!isLoading) onSelect(org);
                  }}
                  onKeyDown={(event) => {
                    if (isLoading) return;
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      onSelect(org);
                    }
                  }}
                  className={cn(
                    "flex cursor-pointer items-center justify-between rounded-[0.75rem] border border-[#e4ecf3] px-4 py-3 text-left transition",
                    "hover:border-[#c9d6e3] hover:bg-[#f7fafc]",
                    isLoading && "pointer-events-none cursor-not-allowed opacity-60",
                  )}
                >
                  <div>
                    <div className="text-[0.875rem] font-semibold text-[#1f2d38]">
                      {org.name}
                    </div>
                    <div className="text-[0.75rem] text-[#6b7a88]">{org.slug}</div>
                    {roleHint ? (
                      <div className="mt-1 text-[0.75rem] text-[#8a96a3]">{roleHint}</div>
                    ) : null}
                    {showUsername ? (
                      <div className="mt-1 text-[0.75rem] text-[#6b7a88]">
                        Username: {orgUsername}
                      </div>
                    ) : null}
                  </div>
                  <span
                    className={cn(
                      buttonVariants({ variant: "outline" }),
                      "pointer-events-none h-9 rounded-full px-4 text-[0.75rem]",
                    )}
                  >
                    Continue
                  </span>
                </div>
            );
          })}
        </div>

        <div className="mt-5 flex justify-end">
          <Button
            variant="outline"
            className="h-9 rounded-full px-4 text-[0.75rem]"
            onClick={onClose}
            disabled={isLoading}
          >
            Cancel
          </Button>
        </div>
      </div>
    </div>
  );
}
