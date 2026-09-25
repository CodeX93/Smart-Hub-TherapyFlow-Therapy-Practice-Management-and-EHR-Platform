import { useEffect, useState } from "react";

/** Keep typing responsive and send a search only after a short pause. */
export function useDebouncedSearch(value: string, delay = 300) {
  const normalized = value.trim();
  const [search, setSearch] = useState(normalized);
  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(normalized), delay);
    return () => window.clearTimeout(timer);
  }, [normalized, delay]);
  return { search, isPending: normalized !== search };
}
