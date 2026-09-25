import React from "react";
import { X } from "lucide-react";
import { Button } from "../ui/button";
import { cn } from "../../lib/utils";
import { type SchedulingSuccessData } from "@/schemas/scheduling.schema";
import { ZoomMeetingJoinBlock } from "@/components/shared/ZoomMeetingJoinBlock";

interface AddSessionSuccessModalProps {
  isOpen: boolean;
  onClose: () => void;
  data?: SchedulingSuccessData | null;
}

const AddSessionSuccessModal: React.FC<AddSessionSuccessModalProps> = ({
  isOpen,
  onClose,
  data = {
    sessionType: "Psychotherapy",
    client: "Arham (CL-2025-1485)",
    service: "Psychotherapy session 60 minutes ( Reg) $100.00",
    sessionMode: "In Person",
    room: "Room 101. Main Wing",
    date: new Date("2025-12-26"),
    selectedTimeSlot: "04:00 PM",
  },
}) => {
  if (!isOpen || !data) return null;

  const formatDate = (date: Date | null) => {
    if (!date) return "";
    return date.toLocaleDateString("en-US", { month: "short", day: "numeric" });
  };

  const getServiceDetails = () => {
    const service = data?.service || "";
    const priceIndex = service.lastIndexOf("$");
    const price = priceIndex !== -1 ? service.substring(priceIndex) : "";
    const serviceName =
      priceIndex !== -1 ? service.substring(0, priceIndex).trim() : service;
    return { price, serviceName };
  };

  const { price, serviceName } = getServiceDetails();

  const isInPersonMode = (() => {
    const mode = (data?.sessionMode || "").toLowerCase().replace(/[\s_-]+/g, "");
    return mode.includes("person") || mode === "inperson";
  })();

  const sessionModeLabel = isInPersonMode ? "In Person" : "Virtual";

  return (
    <div className="fixed inset-0 z-9999 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
      <div className="relative flex w-full max-w-125 max-h-[90vh] flex-col overflow-hidden rounded-2xl bg-white shadow-xl animate-in fade-in zoom-in-95 duration-200">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-10 text-(--text-neutral-400) hover:bg-(--neutral-100) rounded-full p-1.5 duration-300 transition-colors cursor-pointer"
        >
          <X size={18} />
        </button>

        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-5 md:px-6 md:pt-6 md:pb-4">
          {/* Success Icon */}
          <div className="mb-4 relative flex justify-center">
            <img src="/assets/check-email.png" alt="success" className="w-16 h-16" />
          </div>

          {/* Title & Subtitle */}
          <h2 className="text-xl font-semibold text-(--text-primary-dark) mb-1 text-center">
            {data?.recurringSummary
              ? "Recurring sessions scheduled!"
              : "Session scheduled successfully!"}
          </h2>
          <p className="text-(--text-neutral-600) text-sm text-center mb-3">
            {data?.recurringSummary
              ? `${data.recurringSummary.createdCount} session${data.recurringSummary.createdCount === 1 ? "" : "s"} added${data.recurringSummary.skippedCount > 0 ? ` · ${data.recurringSummary.skippedCount} skipped due to conflicts` : ""}`
              : "The session has been added to your schedule"}
          </p>

          {/* Date & Time */}
          <div className="text-lg font-bold text-(--text-primary-dark) mb-4 text-center">
            {formatDate(data?.date)} | {data?.selectedTimeSlot}
          </div>

          {/* Details Box */}
          <div className="w-full min-w-0 bg-(--bg-primary-light) border border-(--neutral-100) rounded-xl p-4 space-y-2.5">
            {[
              { label: "Session Type", value: data?.sessionType },
              { label: "Client", value: data?.client },
              { label: "Service", value: serviceName, isWide: true },
              { label: "Price", value: price },
              {
                label: "Session Mode",
                value: sessionModeLabel,
              },
              ...(isInPersonMode
                ? [{ label: "Room", value: data?.room }]
                : []),
            ].map((item, idx) => (
              <div key={idx} className="flex justify-between items-start gap-4 min-w-0">
                <span className="text-(--text-neutral-600) text-sm shrink-0">
                  {item.label}
                </span>
                <span
                  className={cn(
                    "text-(--text-primary-dark) text-sm font-medium text-right min-w-0 break-words [overflow-wrap:anywhere]",
                    item.isWide ? "flex-1" : "max-w-[60%]",
                  )}
                  title={item.value}
                >
                  {item.value}
                </span>
              </div>
            ))}
          </div>

          <ZoomMeetingJoinBlock
            joinUrl={data?.zoomJoinUrl}
            password={data?.zoomPassword}
            zoomEnabled={!isInPersonMode}
            sessionMode={data?.sessionMode}
            status="scheduled"
            className="mt-4"
          />
        </div>

        {/* Action Button */}
        <div className="shrink-0 border-t border-(--neutral-100) px-4 py-4 md:px-6">
          <Button
            onClick={onClose}
            className="w-full rounded-full px-8 py-2.5 h-auto bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white text-sm font-semibold cursor-pointer shadow-lg shadow-(--bg-primary-dark)/10"
          >
            Done
          </Button>
        </div>
      </div>
    </div>
  );
};

export default AddSessionSuccessModal;
