import { useState } from "react";
import { Button } from "../../ui/button";
import ConfirmationModal from "../../shared/ConfirmationModal";
import type { Client } from "../../../types/client.type";

interface PortalAccessSectionProps {
  client: Client;
  isEnabled: boolean;
}

const PortalAccessSection = ({ client, isEnabled }: PortalAccessSectionProps) => {
  const [isDisableModalOpen, setIsDisableModalOpen] = useState(false);

  const portalFeatures = [
    "View upcoming appointments",
    "Book new appointments online",
    "View and pay invoices",
    "Upload documents securely",
  ];

  const handleDisablePortal = () => {
    console.log("Disable portal access for:", client.id);
    // TODO: Implement API call to disable portal access
    setIsDisableModalOpen(false);
  };

  return (
    <>
      <div className="space-y-4 pt-4">
        {/* Portal Email and Action Buttons - Same Row */}
        {client.email && (
          <div className="flex items-center justify-between gap-4">
            {/* Left: Portal Email */}
            <div className="flex-shrink-0">
              <span className="text-xs text-gray-500 block mb-1">Portal Email</span>
              <p className="text-sm text-gray-900 font-medium">{client.email}</p>
            </div>

            {/* Right: Action Buttons */}
            <div className="flex gap-3">
              <Button
                variant="outline"
                className="h-10 px-4 border-gray-200 bg-white hover:bg-gray-50 text-gray-700 rounded-full cursor-pointer text-sm font-normal whitespace-nowrap"
              >
                Resend Activation Email
              </Button>
              {isEnabled && (
                <Button
                  variant="outline"
                  onClick={() => setIsDisableModalOpen(true)}
                  className="h-10 px-4 border-red-200 bg-white hover:bg-red-50 text-red-700 rounded-full cursor-pointer text-sm font-normal whitespace-nowrap"
                >
                  Disable Portal Access
                </Button>
              )}
            </div>
          </div>
        )}

        {/* Portal Features */}
        {isEnabled && (
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
            <p className="text-xs text-gray-500 mb-2">Portal Access Features:</p>
            <ul className="space-y-2">
              {portalFeatures.map((feature) => (
                <li key={feature} className="flex items-start gap-2 text-sm text-gray-900 font-medium">
                  <span className="text-gray-400 mt-0.5">•</span>
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
      />
    </>
  );
};

export default PortalAccessSection;

