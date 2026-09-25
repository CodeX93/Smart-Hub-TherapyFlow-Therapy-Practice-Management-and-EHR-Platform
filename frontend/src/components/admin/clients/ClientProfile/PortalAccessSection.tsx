import { useState } from "react";
import { Button } from "../../../ui/button";
import ConfirmationModal from "../../../shared/ConfirmationModal";
import type { Client } from "@/types/client.type.ts";
import { ForbiddenCircle } from "@solar-icons/react-perf/category/ui/Linear/ForbiddenCircle";
import { Letter } from "@solar-icons/react-perf/category/messages/Linear/Letter";

interface PortalAccessSectionProps {
  client: Client;
  isEnabled: boolean;
  readOnly?: boolean;
  isSubmitPending?: boolean;
  isResendPending?: boolean;
  onUpdatePortalAccess?: (enable: boolean) => void;
  onResendActivationEmail?: () => void;
}

const PortalAccessSection = ({
  client,
  isEnabled,
  readOnly = false,
  isSubmitPending = false,
  isResendPending = false,
  onUpdatePortalAccess,
  onResendActivationEmail,
}: PortalAccessSectionProps) => {
  const [isDisableModalOpen, setIsDisableModalOpen] = useState(false);

  const portalFeatures = [
    "View upcoming appointments",
    "Book new appointments online",
    "View and pay invoices",
    "Upload documents securely",
  ];

  const handleDisablePortal = () => {
    onUpdatePortalAccess?.(false);
    setIsDisableModalOpen(false);
  };

  const handleEnablePortal = () => {
    onUpdatePortalAccess?.(true);
  };

  return (
    <>
      <div className="space-y-4 pt-4">
        {/* Portal Email and Action Buttons - Same Row */}
        {(client.portalEmail || client.email) && (
          <div className="flex min-w-0 flex-1 items-center justify-between gap-4">
            {/* Left: Portal Email */}
            <div className="min-w-0 flex-1">
              <span className="mb-1 block text-sm leading-[1.375rem] text-(--text-neutral-600)">
                Portal Email
              </span>
              <p
                className="truncate text-base font-medium leading-6 text-(--text-primary-dark)"
                title={client.portalEmail || client.email}
              >
                {client.portalEmail || client.email}
              </p>
            </div>

            {/* Right: Action Buttons */}
            {!readOnly ? (
            <div className="flex gap-3">
              {isEnabled ? (
                <Button
                  variant="outline"
                  onClick={onResendActivationEmail}
                  disabled={isSubmitPending || isResendPending}
                  className="h-10 cursor-pointer whitespace-nowrap rounded-full border-(--neutral-200) bg-white px-4 text-sm font-medium text-(--bg-primary-dark) hover:bg-(--neutral-50)"
                  loading={isResendPending}
                  loadingLabel="Sending..."
                >
                  <Letter size={20} color="#3C4D58" />
                  Resend Activation Email
                </Button>
              ) : null}
              {isEnabled ? (
                <Button
                  variant="outline"
                  onClick={() => setIsDisableModalOpen(true)}
                  disabled={isSubmitPending}
                  className="h-10 cursor-pointer whitespace-nowrap rounded-full border-(--neutral-200) bg-white px-4 text-sm font-medium text-red-500 hover:bg-red-50"
                >
                  <ForbiddenCircle
                    size={20}
                    color="#EF4444"
                  />
                  Disable Portal Access
                </Button>
              ) : (
                <Button
                  variant="outline"
                  onClick={handleEnablePortal}
                  disabled={isSubmitPending}
                  className="h-10 cursor-pointer whitespace-nowrap rounded-full border-(--neutral-200) bg-white px-4 text-sm font-medium text-(--success-green) hover:bg-(--bg-success-light)"
                  loading={isSubmitPending}
                  loadingLabel="Enabling portal access..."
                >
                  Enable Portal Access
                </Button>
              )}
            </div>
            ) : null}
          </div>
        )}

        {/* Portal Features */}
        {isEnabled && (
          <div className="rounded-xl border border-(--neutral-100) bg-(--bg-primary-light) p-3">
            <p className="mb-2 text-sm font-semibold leading-[1.375rem] text-(--text-primary-dark)">
              Portal Access Features:
            </p>
            <ul>
              {portalFeatures.map((feature) => (
                <li
                  key={feature}
                  className="flex items-start gap-2 text-sm font-normal leading-[1.375rem] text-(--text-neutral-600)"
                >
                  <span className="mt-0.5 text-(--text-neutral-600)">•</span>
                  <span>{feature}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* Disable Portal Access Modal */}
      <ConfirmationModal
        type="disable"
        isOpen={isDisableModalOpen}
        onClose={() => setIsDisableModalOpen(false)}
        onConfirm={handleDisablePortal}
        clientName={client.name}
        confirmButtonLoading={isSubmitPending}
        confirmButtonLoadingText="Disabling..."
      />
    </>
  );
};

export default PortalAccessSection;
