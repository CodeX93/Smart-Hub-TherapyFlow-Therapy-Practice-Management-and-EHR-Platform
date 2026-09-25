import React from "react";
import { X } from "lucide-react";
import { Button } from "../ui/button";
import { cn } from "@/lib/utils";

interface ConfirmationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isBooking?: boolean;
  data: {
    name: string;
    date: string; // e.g., "Thursday, November 13"
    time: string; // e.g., "09:15 AM"
    type: string; // e.g., "In Person"
    serviceName: string; // e.g., "Psychotherapy session 60 minutes ( Reg)"
    price: string; // e.g., "$100.00"
  };
}

const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  isBooking = false,
  data,
}) => {
  if (!isOpen) return null;

  const details = [
    { label: "Name", value: data.name },
    {
      label: "Date & Time",
      value: `${data.date} - ${data.time}`,
    },
    { label: "Meeting Type", value: data.type },
    { label: "Service", value: data.serviceName },
    { label: "Price", value: data.price },
  ];

  return (
    <div className="app-modal-overlay fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm p-2 md:p-0">
      <div className="app-modal-surface w-181.25 bg-(--bg-primary-light) rounded-2xl border border-(--neutral-100) overflow-hidden relative">
        {/* Header */}
        <div className="flex justify-between items-center md:px-6 px-3 py-4 border-b border-(--neutral-100) h-15">
          <h2 className="font-medium text-[1.125rem] leading-6.5 text-(--neutral-950)">
            Confirm Your Appointment
          </h2>
          <button
            type="button"
            onClick={onClose}
            disabled={isBooking}
            className="text-(--text-neutral-600) transition-colors hover:text-(--neutral-950) cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
          >
            <X size={24} />
          </button>
        </div>

        {/* Body */}
        <div className="p-3 md:p-6">
          <div className="bg-white rounded-3xl border border-(--neutral-100) md:p-5 p-3 flex flex-col gap-5">
            {details.map((detail, index) => (
              <div
                key={index}
                className="flex justify-between gap-3 items-start"
              >
                <span
                  className={cn(
                    "font-regular leading-6 text-(--text-neutral-600) min-w-30 shrink-0",
                  )}
                >
                  {detail.label}
                </span>
                <span
                  className={cn(
                    "min-w-0 max-w-[60%] truncate text-right font-medium leading-6 text-(--neutral-950) capitalize",
                  )}
                  title={detail.value}
                >
                  {detail.value}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="flex md:justify-end justify-between items-center gap-3 md:px-6 px-3 pb-6 pt-3">
          <Button
            type="button"
            variant="secondary"
            size="lg"
            className="px-8"
            onClick={onClose}
            disabled={isBooking}
          >
            Cancel
          </Button>
          <Button
            type="button"
            variant="primary"
            size="lg"
            className="px-10"
            onClick={onConfirm}
            disabled={isBooking}
            loading={isBooking}
            loadingLabel="Booking..."
          >
            Confirm Booking
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmationModal;
