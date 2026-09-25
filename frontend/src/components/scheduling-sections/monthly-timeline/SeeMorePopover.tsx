import React, { useLayoutEffect, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import type { Appointment } from "../../../types/scheduling";
import { cn } from "@/lib/utils";
import {
  getSessionStatusBackgroundClass,
  getSessionStatusTextClass,
} from "@/utils/sessionStatusPresentation";

interface SeeMorePopoverProps {
  date: Date;
  appointments: Appointment[];
  anchorRef: React.RefObject<HTMLElement | null>;
  onClose: () => void;
  onSelectAppointment: (appointment: Appointment) => void;
  showTherapistName?: boolean;
}

const WEEKDAYS = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];

const POPOVER_WIDTH = 320;
const POPOVER_MAX_HEIGHT = 280;
const VIEWPORT_PADDING = 16;

const SeeMorePopover: React.FC<SeeMorePopoverProps> = ({
  date,
  appointments,
  anchorRef,
  onClose,
  onSelectAppointment,
  showTherapistName = false,
}) => {
  const [popoverStyle, setPopoverStyle] = useState<React.CSSProperties>({});

  useLayoutEffect(() => {
    const updatePosition = () => {
      const anchor = anchorRef.current;
      if (!anchor) return;

      const rect = anchor.getBoundingClientRect();
      const width = Math.min(POPOVER_WIDTH, window.innerWidth - VIEWPORT_PADDING * 2);

      let left = rect.left;
      if (left + width > window.innerWidth - VIEWPORT_PADDING) {
        left = window.innerWidth - width - VIEWPORT_PADDING;
      }
      left = Math.max(VIEWPORT_PADDING, left);

      const spaceBelow = window.innerHeight - rect.bottom - VIEWPORT_PADDING - 8;
      const spaceAbove = rect.top - VIEWPORT_PADDING - 8;
      const placeBelow = spaceBelow >= spaceAbove;
      const availableSpace = Math.max(160, placeBelow ? spaceBelow : spaceAbove);
      const maxHeight = Math.min(POPOVER_MAX_HEIGHT, availableSpace);

      const top = placeBelow
        ? rect.bottom + 8
        : Math.max(VIEWPORT_PADDING, rect.top - maxHeight - 8);

      setPopoverStyle({
        position: "fixed",
        top,
        left,
        width,
        maxHeight,
        zIndex: 10000,
      });
    };

    updatePosition();
    window.addEventListener("resize", updatePosition);
    window.addEventListener("scroll", updatePosition, true);
    return () => {
      window.removeEventListener("resize", updatePosition);
      window.removeEventListener("scroll", updatePosition, true);
    };
  }, [anchorRef, appointments.length]);

  return createPortal(
    <>
      <button
        type="button"
        className="fixed inset-0 z-[9998] bg-black/20"
        aria-label="Close day appointments"
        onClick={(e) => {
          e.stopPropagation();
          onClose();
        }}
      />
      <div
        data-popover-content
        style={popoverStyle}
        className="flex flex-col overflow-hidden rounded-xl border border-(--neutral-100) bg-white shadow-xl shadow-(--shadow) animate-in fade-in zoom-in-95 duration-100"
        onClick={(e) => e.stopPropagation()}
        onWheel={(e) => e.stopPropagation()}
      >
        <div className="relative shrink-0 border-b border-(--neutral-100) px-4 py-3">
          <button
            type="button"
            className="absolute top-2.5 right-3 cursor-pointer text-(--text-neutral-600)"
            aria-label="Close"
            onClick={(e) => {
              e.stopPropagation();
              onClose();
            }}
          >
            <X size={18} />
          </button>
          <div className="flex flex-col pr-6">
            <span className="text-xs text-(--text-neutral-600) font-medium uppercase">
              {WEEKDAYS[date.getDay()]}
            </span>
            <span className="text-base font-bold text-(--text-neutral-950)">
              {date.getDate()}
            </span>
          </div>
        </div>
        <div
          data-popover-content
          className="custom-scrollbar min-h-0 flex-1 overflow-y-auto overscroll-contain px-3 py-2.5"
          onClick={(e) => e.stopPropagation()}
          onWheel={(e) => e.stopPropagation()}
        >
          <div className="flex flex-col gap-2">
            {appointments.map((apt) => {
              const therapistLabel =
                showTherapistName && apt.therapistName
                  ? apt.therapistName
                  : undefined;
              return (
              <div
                key={apt.id}
                id={`popover-apt-${apt.id}`}
                className={cn(
                  "min-w-0 rounded-lg px-3 py-2 text-sm font-medium cursor-pointer hover:opacity-80 transition-opacity",
                  getSessionStatusBackgroundClass(apt.status),
                  getSessionStatusTextClass(apt.status),
                )}
                title={[`${apt.time} ${apt.name}`, therapistLabel]
                  .filter(Boolean)
                  .join(" · ")}
                onClick={(e) => {
                  e.stopPropagation();
                  onSelectAppointment(apt);
                }}
                onMouseDown={(e) => {
                  e.stopPropagation();
                }}
              >
                <div className="flex flex-col gap-0.5 min-w-0">
                  <span className="font-semibold text-xs">{apt.time}</span>
                  <span className="break-words [overflow-wrap:anywhere] text-sm leading-tight">
                    {apt.name}
                  </span>
                  {therapistLabel ? (
                    <span className="break-words [overflow-wrap:anywhere] text-[0.625rem] font-medium leading-tight opacity-80">
                      {therapistLabel}
                    </span>
                  ) : null}
                </div>
              </div>
              );
            })}
          </div>
        </div>
      </div>
    </>,
    document.body,
  );
};

export default SeeMorePopover;
