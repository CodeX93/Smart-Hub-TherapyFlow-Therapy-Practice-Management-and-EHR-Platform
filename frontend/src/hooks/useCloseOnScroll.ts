import { useEffect, type RefObject } from "react";

const SCROLLABLE_OVERFLOW = /(auto|scroll|overlay)/;

function getScrollableAncestors(element: HTMLElement | null): HTMLElement[] {
  const ancestors: HTMLElement[] = [];
  let current = element?.parentElement ?? null;

  while (current) {
    const style = window.getComputedStyle(current);
    const isScrollable = [style.overflow, style.overflowY, style.overflowX].some(
      (value) => SCROLLABLE_OVERFLOW.test(value)
    );

    if (isScrollable) {
      ancestors.push(current);
    }

    current = current.parentElement;
  }

  return ancestors;
}

export function useCloseOnScroll(
  isOpen: boolean,
  onClose: () => void,
  anchorRef?: RefObject<HTMLElement | null>,
  extraScrollRefs?: RefObject<HTMLElement | null>[],
) {
  useEffect(() => {
    if (!isOpen) return;

    const handleScroll = () => {
      onClose();
    };

    const scrollTargets = getScrollableAncestors(anchorRef?.current ?? null);
    const extraTargets =
      extraScrollRefs
        ?.map((ref) => ref.current)
        .filter((element): element is HTMLElement => element !== null) ?? [];
    const uniqueTargets = Array.from(new Set([...scrollTargets, ...extraTargets]));

    window.addEventListener("scroll", handleScroll, { passive: true });
    uniqueTargets.forEach((target) => {
      target.addEventListener("scroll", handleScroll, { passive: true });
    });

    return () => {
      window.removeEventListener("scroll", handleScroll);
      uniqueTargets.forEach((target) => {
        target.removeEventListener("scroll", handleScroll);
      });
    };
  }, [isOpen, onClose, anchorRef, extraScrollRefs]);
}
