import { useMemo } from "react";
import { useAccumulatedPages } from "./useAccumulatedPages";

/** Replace refetched pages, retain earlier pages, and deduplicate by record id. */
export function usePagedItems<T extends { id: string | number }>(items: T[] | undefined, page: number, scopeKey: string) {
  const { pages, clearPages, updatePages, isReadyToLoadMore } = useAccumulatedPages(items, page, scopeKey);
  const accumulatedItems = useMemo(() => {
    const byId = new Map<string | number, T>();
    Object.values(pages).flat().forEach(item => byId.set(item.id, item));
    return Array.from(byId.values());
  }, [pages]);
  return { items: accumulatedItems, clearPages, updateItems: updatePages, isReadyToLoadMore };
}
