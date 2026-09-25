import { useState, useEffect, useCallback } from "react";
import { ChevronUp } from "lucide-react";
import { cn } from "../../lib/utils";
import { useSidebar } from "@/contexts/sidebar";

interface ScrollToTopButtonProps {
  className?: string;
  centered?: boolean;
  containerRef?: React.RefObject<HTMLDivElement | null>;
}

const ScrollToTopButton = ({
  className,
  centered = true,
  containerRef,
}: ScrollToTopButtonProps) => {
  const [isVisible, setIsVisible] = useState(false);
  const { isCollapsed } = useSidebar();

  const toggleVisibility = useCallback(() => {
    // Check either the container ref or the window
    const scrollPos = containerRef?.current
      ? containerRef.current.scrollTop
      : window.scrollY;

    setIsVisible(scrollPos > 300);
  }, [containerRef]);

  const scrollToTop = () => {
    if (containerRef?.current) {
      containerRef.current.scrollTo({ top: 0, behavior: "smooth" });
    } else {
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  useEffect(() => {
    const target = containerRef?.current || window;
    target.addEventListener("scroll", toggleVisibility);
    return () => target.removeEventListener("scroll", toggleVisibility);
  }, [toggleVisibility, containerRef]);

  return (
    <button
      onClick={scrollToTop}
      style={
        centered && !isCollapsed
          ? { left: `calc(50% + ${140}px)` }
          : { left: `calc(50% + ${60}px)` }
      }
      className={cn(
        "fixed bottom-8 z-50 flex h-12 w-12 items-center justify-center rounded-full bg-(--bg-primary-dark) text-white shadow-lg transition-all duration-700 hover:scale-110 active:scale-95 cursor-pointer",
        centered ? "left-1/2 -translate-x-1/2" : "right-8",
        isVisible
          ? "translate-y-0 opacity-100"
          : "translate-y-20 opacity-0 pointer-events-none",
        className,
      )}
      aria-label="Scroll to top"
    >
      <ChevronUp size={24} strokeWidth={2.5} />
    </button>
  );
};

export default ScrollToTopButton;
