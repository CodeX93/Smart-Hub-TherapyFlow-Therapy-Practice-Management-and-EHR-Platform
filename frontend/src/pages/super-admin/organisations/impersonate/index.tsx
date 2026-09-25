import { useMemo, useState } from "react";
import { Bell } from "lucide-react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import ProfileDropdown from "@/components/shared/ProfileDropdown";
import PolicyNotice from "./components/PolicyNotice";
import ImpersonateField from "./components/ImpersonateField";
import { useGetOrganisationByIdQuery } from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

function getInputClassName(): string {
  return cn(
    "h-auto border-0 bg-transparent p-0 shadow-none",
    "text-[0.875rem] font-normal text-[#2b3946] placeholder:text-[#97a4b0]",
    "focus-visible:ring-0 focus-visible:border-0"
  );
}

function getTargetUserName(slug?: string): string {
  if (slug === "harbor-wellness") {
    return "Ana Martin";
  }

  return "Admin User";
}

function getTargetUserValue(
  org?: { primaryAdminEmail?: string },
  slug?: string
): string {
  if (!org?.primaryAdminEmail) {
    return "";
  }

  return getTargetUserName(slug) + " (" + org.primaryAdminEmail + ")";
}

function ImpersonateAdmin() {
  const { slug } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const organisationId =
    (location.state as { organisationId?: number | null } | null)?.organisationId ?? null;

  const {
    data: organisationDetails,
    isLoading: isOrganisationLoading,
    isError: isOrganisationError,
    error: organisationError,
  } = useGetOrganisationByIdQuery(organisationId ?? 0, {
    skip: organisationId === null,
  });

  const organisationName = useMemo(() => {
    if (organisationDetails?.name) return organisationDetails.name;
    if (!slug) return "Organization";
    return slug
      .split("-")
      .filter(Boolean)
      .map((part) => part[0]?.toUpperCase() + part.slice(1))
      .join(" ");
  }, [organisationDetails?.name, slug]);

  const primaryAdminEmail = organisationDetails?.primaryAdminEmail ?? "";

  const [targetUser, setTargetUser] = useState(
    getTargetUserValue(
      { primaryAdminEmail },
      slug
    )
  );
  const [reason, setReason] = useState("Troubleshooting user-reported issues");
  const [duration, setDuration] = useState("30 minutes");

  function handleLogout() {
    window.location.href = "/super-admin/login";
  }

  function handleCancel() {
    navigate(-1);
  }

  function handleStartImpersonation() {}

  const currentRecordInput = JSON.stringify([primaryAdminEmail, slug]);
  const [previousRecordInput, setPreviousRecordInput] = useState(currentRecordInput);
  if (currentRecordInput !== previousRecordInput) {
    setPreviousRecordInput(currentRecordInput);
    setTargetUser(getTargetUserValue({ primaryAdminEmail }, slug));
  }

  if (organisationId !== null && isOrganisationLoading) {
    return (
      <div className="h-full w-full overflow-auto pb-6">
        <div className="text-[#667483] text-sm font-medium">
          Loading organisation...
        </div>
      </div>
    );
  }

  if (organisationId !== null && isOrganisationError) {
    return (
      <div className="h-full w-full overflow-auto pb-6">
        <div className="text-(--status-denied) text-sm font-medium">
          {getApiErrorMessage(organisationError)}
        </div>
        <Button
          variant="outline"
          className="mt-4 border-(--neutral-100)"
          onClick={handleCancel}
        >
          Back
        </Button>
      </div>
    );
  }

  return (
    <div className="h-full w-full overflow-auto pb-6">
      <div className="w-full">
        <div className="flex w-full flex-col gap-6">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <h1 className="text-[#1f2d38] text-[1.125rem] font-semibold leading-8">
                Impersonate Organization Admin
              </h1>
              <p className="mt-1 max-w-[47.5rem] text-[#667483] text-[0.8125rem] font-normal leading-5">
                Securely access {organisationName} as an existing administrator. All
                actions during this session will be logged in the global audit
                trail.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                className="relative flex h-9 w-9 items-center justify-center rounded-full text-[#24313f] transition-colors hover:bg-[#eff4f8]"
                aria-label="Notifications"
              >
                <Bell size={17} strokeWidth={1.9} aria-hidden="true" />
                <span className="absolute right-0.5 top-0.5 flex h-[1.125rem] min-w-[1.125rem] items-center justify-center rounded-full bg-[#ef4444] px-1 text-[0.625rem] font-semibold leading-none text-white">
                  4
                </span>
              </button>

              <ProfileDropdown
                initials="JS"
                fullName="Jordan Smith"
                onLogout={handleLogout}
              />
            </div>
          </div>

          <div className="flex w-full justify-center">
            <div className="w-full max-w-[45rem] rounded-[1rem] border border-[#e3ebf3] bg-white px-6 py-6 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
              <PolicyNotice className="mb-4" />

              <div className="flex flex-col gap-5">
                <ImpersonateField
                  label="Target User"
                  helper={
                    "Only users with the 'admin' role in " +
                    organisationName +
                    " are shown."
                  }
                >
                  <Input
                    value={targetUser}
                    onChange={function (event) {
                      setTargetUser(event.target.value);
                    }}
                    className={getInputClassName()}
                  />
                </ImpersonateField>

                <ImpersonateField
                  label="Reason for Impersonation"
                  helper="This reason will be recorded in the audit log. Minimum 10 characters."
                >
                  <Input
                    value={reason}
                    onChange={function (event) {
                      setReason(event.target.value);
                    }}
                    className={getInputClassName()}
                  />
                </ImpersonateField>

                <ImpersonateField
                  label="Session Duration"
                  helper="You will be automatically logged out of the impersonation session when the duration expires."
                >
                  <Input
                    value={duration}
                    onChange={function (event) {
                      setDuration(event.target.value);
                    }}
                    className={getInputClassName()}
                  />
                </ImpersonateField>
              </div>

              <div className="mt-7 flex items-center justify-end gap-3">
                <Button
                  variant="secondary"
                  size="md"
                  onClick={handleCancel}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  onClick={handleStartImpersonation}
                >
                  Start Impersonation
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ImpersonateAdmin;
