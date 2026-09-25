import { useEffect } from "react";
import { useLocation } from "react-router-dom";

export const useScrollToTop = () => {
  const { pathname } = useLocation();

  useEffect(() => {
    if ("scrollRestoration" in history) {
      history.scrollRestoration = "manual";
    }

    window.scrollTo(0, 0);

    const timeout = setTimeout(() => {
      window.scrollTo(0, 0);
    }, 50);

    return () => {
      if ("scrollRestoration" in history) {
        history.scrollRestoration = "auto";
      }
      clearTimeout(timeout);
    };
  }, [pathname]);
};
