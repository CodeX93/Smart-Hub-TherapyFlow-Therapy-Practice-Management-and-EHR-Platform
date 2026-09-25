import React from "react";
import { Button } from "../ui/button";

interface SuccessModalProps {
  isOpen: boolean;
  onClose: () => void;
  data: {
    date: string; // e.g., "Nov 15"
    time: string; // e.g., "10:00 AM"
    type: string; // e.g., "In-person"
  };
}

const SuccessModal: React.FC<SuccessModalProps> = ({
  isOpen,
  onClose,
  data,
}) => {
  if (!isOpen) return null;

  return (
    <div className="app-modal-overlay fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm p-2 md:p-0">
      <div className="app-modal-surface w-125 rounded-xl p-10 flex flex-col items-center text-center">
        {/* Success Icon */}
        <img
          src="/assets/check-email.png"
          alt="check"
          className="w-16 h-16 mb-6"
        />

        <h2 className="font-semibold md:text-2xl text-xl text-(--neutral-950) mb-4">
          Appointment Confirmed
        </h2>

        <div className="flex items-center gap-3 mb-2">
          <span className="font-bold md:text-2xl text-xl text-(--text-primary-dark)">
            {data.date}
          </span>
          <span className="text-(--text-primary-light) text-[1.5rem]">|</span>
          <span className="font-bold md:text-xl text-xl text-(--text-primary-dark)">
            {data.time}
          </span>
        </div>

        <p className="font-medium md:text-lg text-base text-(--text-neutral-600) mb-2 capitalize">
          {data.type}
        </p>

        <p className="font-regular md:text-base text-sm text-(--text-neutral-400) mb-8">
          You're all set!
        </p>

        <Button
          variant="primary"
          size="lg"
          className="px-12"
          onClick={onClose}
        >
          Done
        </Button>
      </div>
    </div>
  );
};

export default SuccessModal;
