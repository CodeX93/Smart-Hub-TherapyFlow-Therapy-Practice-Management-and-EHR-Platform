import { useEffect, useRef, useState } from "react";

interface ModalPosition {
  left?: string;
  right?: string;
  top?: string;
}

const MODAL_WIDTH = 448;
const ANCHOR_SPACING = 12;
const VIEWPORT_PADDING = 16;

function resolveVerticalTop(
  anchorRect: DOMRect,
  modalHeight: number,
  spacing = ANCHOR_SPACING,
  viewportPadding = VIEWPORT_PADDING,
): number {
  const maxBottom = window.innerHeight - viewportPadding;
  const defaultTop = anchorRect.top;
  const fitsBelow = defaultTop + modalHeight <= maxBottom;

  if (fitsBelow) {
    return defaultTop;
  }

  const aboveTop = anchorRect.top - modalHeight - spacing;
  if (aboveTop >= viewportPadding) {
    return aboveTop;
  }

  return Math.max(viewportPadding, maxBottom - modalHeight);
}

export const useModalPositioning = (
  isOpen: boolean,
  anchorElementId?: string,
) => {
  const modalRef = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState<ModalPosition>({
    left: "100%",
  });

  useEffect(() => {
    if (!isOpen) return;

    const updatePosition = () => {
      const modal = modalRef.current;
      if (!modal) return;

      if (anchorElementId) {
        const anchorElement = document.getElementById(anchorElementId);
        if (!anchorElement) return;

        const anchorRect = anchorElement.getBoundingClientRect();
        const modalHeight = modal.getBoundingClientRect().height || modal.offsetHeight;
        const top = resolveVerticalTop(anchorRect, modalHeight);

        const spaceOnRight = window.innerWidth - anchorRect.right;
        const wouldOverflowRight = spaceOnRight < MODAL_WIDTH + ANCHOR_SPACING;

        if (wouldOverflowRight) {
          setPosition({
            right: `${window.innerWidth - anchorRect.left + ANCHOR_SPACING}px`,
            top: `${top}px`,
          });
        } else {
          setPosition({
            left: `${anchorRect.right + ANCHOR_SPACING}px`,
            top: `${top}px`,
          });
        }
        return;
      }

      const parent = modal.parentElement;
      if (!parent) return;

      const parentRect = parent.getBoundingClientRect();
      const spaceOnRight = window.innerWidth - parentRect.right;
      const wouldOverflowRight = spaceOnRight < MODAL_WIDTH + ANCHOR_SPACING;

      if (wouldOverflowRight) {
        setPosition({
          right: "-15%",
        });
      } else {
        setPosition({
          left: "-15%",
        });
      }
    };

    updatePosition();
    const rafId = window.requestAnimationFrame(updatePosition);

    const modal = modalRef.current;
    const resizeObserver =
      modal && typeof ResizeObserver !== "undefined"
        ? new ResizeObserver(() => updatePosition())
        : null;
    if (modal && resizeObserver) {
      resizeObserver.observe(modal);
    }

    window.addEventListener("resize", updatePosition);
    window.addEventListener("scroll", updatePosition, true);
    return () => {
      window.cancelAnimationFrame(rafId);
      resizeObserver?.disconnect();
      window.removeEventListener("resize", updatePosition);
      window.removeEventListener("scroll", updatePosition, true);
    };
  }, [isOpen, anchorElementId]);

  return { modalRef, position };
};
