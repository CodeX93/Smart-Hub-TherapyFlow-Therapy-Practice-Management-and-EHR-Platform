import { useEffect, useState } from "react";

export function useAnimatedPresence(open: boolean, duration = 300) {
  const [present, setPresent] = useState(open);
  if (open && !present) setPresent(true);
  useEffect(() => {
    if (open) return;
    const timer = window.setTimeout(() => setPresent(false), duration);
    return () => window.clearTimeout(timer);
  }, [open, duration]);
  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = previousOverflow; };
  }, [open]);
  return open || present;
}
