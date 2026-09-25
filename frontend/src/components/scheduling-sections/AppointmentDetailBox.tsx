import React, { useEffect, useState } from "react";
import { X } from "lucide-react";
import type { Appointment, AppointmentStatus } from "../../types/scheduling";
import { useModalPositioning } from "../../hooks/useModalPositioning";
import { AppointmentDetails } from "./AppointmentDetails";
import SessionStatusBadge from "@/components/shared/SessionStatusBadge";
import CreateSessionBillingModal from "@/components/billing-sections/CreateSessionBillingModal";
import Toast from "@/components/shared/Toast";

function formatAppointmentDateLabel(date?: string): string {
  if (!date) return "—";
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return "—";

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
  }).format(parsed);
}

interface AppointmentDetailBoxProps {
  isOpen: boolean;
  onClose: () => void;
  appointment: Appointment | null;
  onStatusChange?: (id: string, statusKey: string) => void;
  isStatusUpdating?: boolean;
  handleEditScheduleClick?: (appointment: Appointment) => void;
  hideEditActions?: boolean;
  showTherapistName?: boolean;
  anchorElementId?: string;
}

const AppointmentDetailBox: React.FC<AppointmentDetailBoxProps> = ({
  isOpen,
  onClose,
  appointment,
  onStatusChange,
  isStatusUpdating = false,
  handleEditScheduleClick,
  hideEditActions = false,
  showTherapistName = false,
  anchorElementId,
}) => {
  const { modalRef, position } = useModalPositioning(isOpen, anchorElementId);

  const [isMobile, setIsMobile] = React.useState(false);
  const [mobileStyle, setMobileStyle] = React.useState<React.CSSProperties>({});
  const [createBillingSessionId, setCreateBillingSessionId] = useState<number | null>(
    null,
  );
  const [invoiceCreated, setInvoiceCreated] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");

  const resetKey = appointment?.id;
  const [previousResetKey, setPreviousResetKey] = useState(resetKey);
  if (previousResetKey !== resetKey) {
    setPreviousResetKey(resetKey);
    setInvoiceCreated(false);
    setCreateBillingSessionId(null);
  }

  useEffect(() => {
    const calculateMobilePos = () => {
      const mobile = window.innerWidth < 768;
      setIsMobile(mobile);

      if (mobile && anchorElementId) {
        const anchor = document.getElementById(anchorElementId);
        if (anchor) {
          const rect = anchor.getBoundingClientRect();
          const modalHeight =
            modalRef.current?.getBoundingClientRect().height ||
            modalRef.current?.offsetHeight ||
            400;

          const newStyle: React.CSSProperties = {
            position: "fixed",
            left: "50%",
            transform: "translateX(-50%)",
            marginLeft: undefined,
            marginRight: undefined,
          };

          const maxBottom = window.innerHeight - 16;
          const fitsBelow = rect.bottom + 10 + modalHeight <= maxBottom;
          const aboveTop = rect.top - modalHeight - 10;

          if (!fitsBelow && aboveTop >= 16) {
            newStyle.top = `${aboveTop}px`;
            newStyle.bottom = undefined;
          } else if (!fitsBelow) {
            newStyle.top = `${Math.max(16, maxBottom - modalHeight)}px`;
            newStyle.bottom = undefined;
          } else {
            newStyle.top = `${rect.bottom + 10}px`;
            newStyle.bottom = undefined;
          }
          setMobileStyle(newStyle);
        }
      }
    };

    calculateMobilePos();
    const rafId = window.requestAnimationFrame(calculateMobilePos);
    window.addEventListener("resize", calculateMobilePos);
    window.addEventListener("scroll", calculateMobilePos, true);
    return () => {
      window.cancelAnimationFrame(rafId);
      window.removeEventListener("resize", calculateMobilePos);
      window.removeEventListener("scroll", calculateMobilePos, true);
    };
  }, [anchorElementId, isOpen, modalRef]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement | null;
      if (
        target?.closest('[data-slot="dropdown-menu-content"]') ||
        target?.closest('[data-slot="dropdown-menu-trigger"]') ||
        target?.closest("[data-create-invoice-modal]") ||
        target?.closest("[data-scheduling-nested-modal]")
      ) {
        return;
      }
      if (
        isOpen &&
        modalRef.current &&
        !modalRef.current.contains(event.target as Node)
      ) {
        onClose();
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen, onClose, modalRef]);

  if (!isOpen || !appointment) return null;

  const currentStatus =
    (appointment.status as AppointmentStatus) || "scheduled";
  const handleStatusUpdate = (statusKey: string) => {
    if (onStatusChange) {
      onStatusChange(appointment.id, statusKey);
    }
  };

  const parsedSessionId = Number.parseInt(appointment.id, 10);

  return (
    <>
      <div
        ref={modalRef}
        style={
          isMobile
            ? mobileStyle
            : {
                position: anchorElementId ? "fixed" : "absolute",
                ...position,
                marginLeft:
                  position.left && !anchorElementId ? "0.75rem" : undefined,
                marginRight:
                  position.right && !anchorElementId ? "0.75rem" : undefined,
              }
        }
        className="z-50 pointer-events-auto w-[95vw] max-w-md cursor-default overflow-visible rounded-2xl bg-white shadow-xl animate-in fade-in zoom-in duration-200 md:w-md"
      >
        <div className="relative min-w-0 p-4">
          <div className="flex min-w-0 gap-4">
            <div className="flex min-w-25 shrink-0 flex-col items-center justify-center rounded-xl border border-(--neutral-100) bg-(--neutral-50) p-4">
              <span className="text-lg font-bold text-(--text-primary-dark)">
                {formatAppointmentDateLabel(appointment.date)}
              </span>
              <span className="text-sm font-medium text-(--text-neutral-600)">
                {appointment.time}
              </span>
            </div>

            <div className="flex min-w-0 flex-1 flex-col gap-1.5">
              <div className="flex min-w-0 items-center gap-2 pr-10">
                <a
                  href="#"
                  className="min-w-0 truncate text-lg font-bold text-(--text-primary-dark) underline decoration-(--neutral-200) underline-offset-4 transition-colors hover:text-(--status-billed)"
                  title={appointment.name}
                >
                  {appointment.name}
                </a>
                <SessionStatusBadge status={currentStatus} />
              </div>
              {showTherapistName && appointment.therapistName ? (
                <span
                  className="min-w-0 truncate text-xs font-medium text-(--text-neutral-600)"
                  title={appointment.therapistName}
                >
                  {appointment.therapistName}
                </span>
              ) : null}

              <AppointmentDetails
                appointment={appointment}
                onEditClick={
                  hideEditActions
                    ? undefined
                    : () => handleEditScheduleClick?.(appointment)
                }
                onStatusChange={handleStatusUpdate}
                currentStatus={currentStatus}
                modalPosition={position}
                isMobile={isMobile}
                isStatusUpdating={isStatusUpdating}
                hideEditActions={hideEditActions}
                invoiceAlreadyCreated={invoiceCreated}
                onCreateInvoice={() => {
                  if (!Number.isFinite(parsedSessionId)) return;
                  setCreateBillingSessionId(parsedSessionId);
                }}
                onToast={(message, type) => {
                  setToastType(type);
                  setToastMessage(message);
                }}
              />
            </div>
          </div>

          <button
            onClick={onClose}
            className="absolute top-4 right-4 z-10 shrink-0 cursor-pointer rounded-full p-1 transition-colors hover:bg-(--neutral-50)"
          >
            <X className="h-5 w-5 text-(--text-neutral-400)" />
          </button>
        </div>
      </div>

      {createBillingSessionId ? (
        <div data-create-invoice-modal>
          <CreateSessionBillingModal
            isOpen={Boolean(createBillingSessionId)}
            onClose={() => setCreateBillingSessionId(null)}
            sessionId={createBillingSessionId}
            onCreated={() => {
              setInvoiceCreated(true);
              setToastType("success");
              setToastMessage("Invoice created. View it in Billings.");
              setCreateBillingSessionId(null);
            }}
          />
        </div>
      ) : null}

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </>
  );
};

export default AppointmentDetailBox;
