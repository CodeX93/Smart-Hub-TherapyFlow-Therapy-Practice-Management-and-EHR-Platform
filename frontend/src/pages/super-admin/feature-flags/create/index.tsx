import { Bell } from "lucide-react";
import { useNavigate } from "react-router-dom";
import ProfileDropdown from "@/components/shared/ProfileDropdown";
import CreateFeatureForm from "./components/CreateFeatureForm";

function CreateFeature() {
  const navigate = useNavigate();

  function handleLogout() {
    window.location.href = "/super-admin/login";
  }

  function handleCancel() {
    navigate(-1);
  }

  return (
    <div className="h-full min-h-full w-full overflow-auto pb-6">
      <div className="flex w-full min-h-full flex-col gap-6 bg-(--surface-white)">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h1 className="text-(--text-gray-900) text-[2rem] font-semibold leading-[1.08] tracking-[-0.03em]">
              Create Feature
            </h1>
            <p className="mt-2 text-(--text-neutral-600) text-sm font-normal leading-5.5">
              Add a new feature toggle or limit to the global catalog.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              className="relative flex h-9 w-9 items-center justify-center rounded-full text-(--text-primary-dark) transition-colors hover:bg-(--bg-primary-50)"
              aria-label="Notifications"
            >
              <Bell size={17} strokeWidth={1.9} aria-hidden="true" />
              <span className="absolute right-0.5 top-0.5 flex h-[1.125rem] min-w-[1.125rem] items-center justify-center rounded-full bg-(--status-denied) px-1 text-[0.625rem] font-semibold leading-none text-white">
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

        <CreateFeatureForm onCancel={handleCancel} />
      </div>
    </div>
  );
}

export default CreateFeature;
