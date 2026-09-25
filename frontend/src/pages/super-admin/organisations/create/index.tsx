import { ChevronLeft } from "lucide-react";
import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import SuperAdminHeaderActions from "@/components/shared/SuperAdminHeaderActions";
import Toast from "@/components/shared/Toast";
import CreateOrganisationForm from "./components/CreateOrganisationForm";

const CreateOrganisation = () => {
  const navigate = useNavigate();
  const [isCreating, setIsCreating] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const showToast = useCallback((type: "success" | "error", message: string) => {
    setToastType(type);
    setToastMessage(message);
  }, []);

  return (
    <div className="flex h-full min-h-0 w-full flex-col bg-[#FAFAFB]">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <div className="flex shrink-0 flex-col gap-6 pb-4">
        <div className="flex items-center justify-between gap-4">
          <Button
            type="button"
            variant="tertiary"
            size="sm"
            onClick={function () {
              navigate("/super-admin/organisations");
            }}
          >
            <ChevronLeft size={15} aria-hidden="true" />
            Back
          </Button>

          <SuperAdminHeaderActions />
        </div>

        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-[#1E293B] text-[1.25rem] font-semibold leading-8">
              New Organization
            </h1>
            <p className="mt-1 text-[#667483] text-[0.8125rem] font-normal leading-5">
              Onboard a new tenant and configure their initial plan and regional
              settings.
            </p>
          </div>

          <div className="flex shrink-0 items-center gap-3">
            <Button
              variant="secondary"
              size="md"
              onClick={function () {
                navigate("/super-admin/organisations");
              }}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="md"
              disabled={isCreating}
              onClick={function () {
                const form = document.querySelector("#create-organisation-form");
                (form as HTMLFormElement | null)?.requestSubmit?.();
              }}
              loading={isCreating}
              loadingLabel="Creating..."
            >
              Create Organization
            </Button>
          </div>
        </div>
      </div>

      <div
        className="min-h-0 flex-1 overflow-y-auto pb-6 pr-1"
        style={{
          scrollBehavior: "smooth",
          scrollbarGutter: "stable",
          overscrollBehavior: "contain",
        }}
      >
        <CreateOrganisationForm
          onSubmittingChange={setIsCreating}
          onToast={showToast}
        />
      </div>
    </div>
  );
};

export default CreateOrganisation;
