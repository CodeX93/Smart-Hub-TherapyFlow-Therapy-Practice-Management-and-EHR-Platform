import { useRef, useEffect } from "react";

interface UseInfiniteScrollProps extends IntersectionObserverInit {
  onLoadMore: () => void;
  hasMore: boolean;
  isLoading?: boolean;
  scrollRootRef?: React.RefObject<Element | null>;
}

function parseRootMarginPx(rootMargin: string): number {
  const parsed = Number.parseInt(rootMargin, 10);
  return Number.isFinite(parsed) ? parsed : 0;
}

function isTargetNearViewport(
  target: Element,
  root: Element | null,
  rootMargin: string,
): boolean {
  const margin = parseRootMarginPx(rootMargin);
  const targetRect = target.getBoundingClientRect();

  if (root) {
    const rootRect = root.getBoundingClientRect();
    return targetRect.top <= rootRect.bottom + margin;
  }

  return targetRect.top <= window.innerHeight + margin;
}

export const useInfiniteScroll = ({
  onLoadMore,
  hasMore,
  isLoading = false,
  threshold = 0.1,
  rootMargin = "500px",
  scrollRootRef,
  ...options
}: UseInfiniteScrollProps) => {
  const observerTarget = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        const first = entries[0];
        if (first.isIntersecting && hasMore && !isLoading) {
          onLoadMore();
        }
      },
      {
        threshold,
        rootMargin,
        root: scrollRootRef?.current ?? options.root ?? null,
        ...options,
      },
    );

    const target = observerTarget.current;
    if (target) {
      observer.observe(target);
    }

    return () => {
      if (target) {
        observer.unobserve(target);
      }
    };
  }, [
    onLoadMore,
    hasMore,
    isLoading,
    threshold,
    rootMargin,
    scrollRootRef,
    options,
  ]);

  // If hasMore becomes true while the sentinel is already visible (e.g. after
  // cache hydration), IntersectionObserver won't fire again — check manually.
  const prevHasMoreRef = useRef<boolean | null>(null);

  useEffect(() => {
    if (prevHasMoreRef.current === null) {
      prevHasMoreRef.current = hasMore;
      return;
    }

    const becameTrue = !prevHasMoreRef.current && hasMore;
    prevHasMoreRef.current = hasMore;

    if (!becameTrue || isLoading) return;

    const target = observerTarget.current;
    if (!target) return;

    const root = scrollRootRef?.current ?? null;
    if (!isTargetNearViewport(target, root, rootMargin)) return;

    onLoadMore();
  }, [hasMore, isLoading, onLoadMore, rootMargin, scrollRootRef]);

  return { observerTarget };
};
